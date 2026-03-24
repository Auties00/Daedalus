package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.auties00.daedalus.protobuf.compiler.ProtobufParser;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufDocumentTree;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.generation.NamingStrategy;
import com.github.auties00.daedalus.protobuf.cli.generation.PropertyGenerator;
import com.github.auties00.daedalus.protobuf.cli.generation.TypeMapper;
import com.github.auties00.daedalus.protobuf.cli.model.SourceChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates the update process: parsing protos, scanning Java sources,
 * matching types, detecting conflicts, computing patches, and generating diffs.
 *
 * <p>This is the main entry point for the update workflow. It coordinates
 * the {@link JavaSourceScanner}, {@link TypeMatcher}, {@link ConflictResolver},
 * {@link SourcePatcher}, and {@link DiffCalculator} to produce an {@link UpdatePlan}.
 */
public final class UpdateEngine {
    private final SchemaConfig config;
    private final JavaSourceScanner scanner;
    private final TypeMatcher matcher;
    private final ConflictResolver conflictResolver;
    private final SourcePatcher patcher;
    private final DiffCalculator diffCalculator;
    private final NamingStrategy naming;

    /**
     * Constructs an update engine with the given configuration.
     *
     * @param config the schema configuration
     */
    public UpdateEngine(SchemaConfig config) {
        this.config = config;
        this.naming = new NamingStrategy();
        var typeMapper = new TypeMapper(config, naming);
        var propertyGenerator = new PropertyGenerator(typeMapper, naming);
        var extractor = new AnnotationExtractor();

        this.scanner = new JavaSourceScanner(extractor);
        this.matcher = new TypeMatcher();
        this.conflictResolver = new ConflictResolver(naming);
        this.patcher = new SourcePatcher(propertyGenerator, naming);
        this.diffCalculator = new DiffCalculator();
    }

    /**
     * Runs the update process: parses protos, scans Java sources, matches types,
     * and produces an update plan.
     *
     * @return the update plan with proposed changes, conflicts, and unmatched types
     * @throws IOException if proto files or Java sources cannot be read
     */
    public UpdatePlan computeUpdatePlan() throws IOException {
        var protoDir = Path.of(config.protoDir());
        if (!Files.exists(protoDir)) {
            throw new IOException("Proto directory does not exist: " + protoDir);
        }

        var sourceDir = Path.of(config.javaSourceDir());
        if (!Files.exists(sourceDir)) {
            throw new IOException("Java source directory does not exist: " + sourceDir);
        }

        var documents = ProtobufParser.parse(protoDir);
        var scannedTypes = scanner.scan(sourceDir);

        return buildUpdatePlan(documents, scannedTypes);
    }

    /**
     * Runs the update process with pre-parsed documents and pre-scanned types.
     *
     * @param documents the parsed proto documents
     * @param scannedTypes the scanned Java types
     * @return the update plan
     */
    public UpdatePlan buildUpdatePlan(Map<String, ProtobufDocumentTree> documents,
                                       List<ScannedType> scannedTypes) {
        var plan = new UpdatePlan();
        var matchedTypes = new HashSet<ScannedType>();

        for (var entry : documents.entrySet()) {
            var document = entry.getValue();
            var protoPackage = document.packageName().orElse(null);

            document.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(message ->
                    processMessage(message, protoPackage, scannedTypes, matchedTypes, plan)
            );

            document.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(enumStmt ->
                    processEnum(enumStmt, protoPackage, scannedTypes, matchedTypes, plan)
            );
        }

        return plan;
    }

    /**
     * Applies the accepted changes from an update plan to disk.
     *
     * @param plan the update plan to apply
     * @return the list of file paths that were modified
     * @throws IOException if files cannot be written
     */
    public List<Path> applyPlan(UpdatePlan plan) throws IOException {
        var modifiedFiles = new ArrayList<Path>();
        for (var update : plan.updates()) {
            var change = update.sourceChange();
            if (change.hasChanges()) {
                Files.writeString(change.filePath(), change.updatedSource());
                modifiedFiles.add(change.filePath());
            }
        }
        return modifiedFiles;
    }

    private void processMessage(ProtobufMessageStatement message, String protoPackage,
                                 List<ScannedType> scannedTypes, Set<ScannedType> matchedTypes,
                                 UpdatePlan plan) {
        var protoFqn = buildProtoFqn(protoPackage, message.name());
        var protoSimpleName = naming.toTypeName(message.name());
        var protoFieldIndexes = collectFieldIndexes(message);

        var bestMatch = matcher.findBestMatch(protoFqn, protoSimpleName, protoFieldIndexes,
                protoPackage, scannedTypes, ScannedType.AnnotationType.MESSAGE);

        if (bestMatch == null) {
            plan.addUnmatchedType(protoFqn);
            return;
        }

        if (matchedTypes.contains(bestMatch.scannedType())) {
            return;
        }
        matchedTypes.add(bestMatch.scannedType());

        var conflicts = conflictResolver.detectMessageConflicts(
                bestMatch.scannedType(), message, protoFqn);

        var originalSource = bestMatch.scannedType().compilationUnit().toString();
        var changeDescriptions = patcher.patchMessage(bestMatch.scannedType(), message);
        var updatedSource = patcher.getUpdatedSource(bestMatch.scannedType());

        var diff = diffCalculator.computeDiff(
                bestMatch.scannedType().filePath(), originalSource, updatedSource);

        var description = changeDescriptions.isEmpty()
                ? "No changes needed for " + protoFqn
                : String.join("; ", changeDescriptions);

        var sourceChange = new SourceChange(
                bestMatch.scannedType().filePath(),
                originalSource,
                updatedSource,
                description,
                diff
        );

        plan.addUpdate(bestMatch, sourceChange, conflicts);

        // Process nested messages and enums recursively
        message.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(nested ->
                processMessage(nested, protoPackage, scannedTypes, matchedTypes, plan)
        );
        message.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(nested ->
                processEnum(nested, protoPackage, scannedTypes, matchedTypes, plan)
        );
    }

    private void processEnum(ProtobufEnumStatement enumStmt, String protoPackage,
                              List<ScannedType> scannedTypes, Set<ScannedType> matchedTypes,
                              UpdatePlan plan) {
        var protoFqn = buildProtoFqn(protoPackage, enumStmt.name());
        var protoSimpleName = naming.toTypeName(enumStmt.name());

        var bestMatch = matcher.findBestMatch(protoFqn, protoSimpleName, Set.of(),
                protoPackage, scannedTypes, ScannedType.AnnotationType.ENUM);

        if (bestMatch == null) {
            plan.addUnmatchedType(protoFqn);
            return;
        }

        if (matchedTypes.contains(bestMatch.scannedType())) {
            return;
        }
        matchedTypes.add(bestMatch.scannedType());

        var conflicts = conflictResolver.detectEnumConflicts(
                bestMatch.scannedType(), enumStmt, protoFqn);

        var originalSource = bestMatch.scannedType().compilationUnit().toString();
        var changeDescriptions = patcher.patchEnum(bestMatch.scannedType(), enumStmt);
        var updatedSource = patcher.getUpdatedSource(bestMatch.scannedType());

        var diff = diffCalculator.computeDiff(
                bestMatch.scannedType().filePath(), originalSource, updatedSource);

        var description = changeDescriptions.isEmpty()
                ? "No changes needed for " + protoFqn
                : String.join("; ", changeDescriptions);

        var sourceChange = new SourceChange(
                bestMatch.scannedType().filePath(),
                originalSource,
                updatedSource,
                description,
                diff
        );

        plan.addUpdate(bestMatch, sourceChange, conflicts);
    }

    private Set<Long> collectFieldIndexes(ProtobufMessageStatement message) {
        var indexes = new LinkedHashSet<Long>();
        message.getDirectChildrenByType(ProtobufFieldStatement.class).forEach(field ->
                indexes.add(field.index().value().longValueExact())
        );
        return indexes;
    }

    private String buildProtoFqn(String protoPackage, String typeName) {
        if (protoPackage == null || protoPackage.isEmpty()) {
            return typeName;
        }
        return protoPackage + "." + typeName;
    }
}
