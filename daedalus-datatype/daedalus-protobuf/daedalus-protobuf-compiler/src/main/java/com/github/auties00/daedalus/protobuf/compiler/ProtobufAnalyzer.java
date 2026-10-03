package com.github.auties00.daedalus.protobuf.compiler;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufParserException;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufSemanticErrors;
import com.github.auties00.daedalus.protobuf.compiler.expression.*;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufIntegerRange;
import com.github.auties00.daedalus.protobuf.compiler.tree.*;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.*;
import com.github.auties00.daedalus.protobuf.model.*;
import com.github.auties00.daedalus.protobuf.support.google.WellKnownProtos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Performs semantic analysis and validation of parsed Protocol Buffer documents.
 * <p>
 * The analyzer is responsible for the second phase of Protocol Buffer processing, after the
 * {@link ProtobufParser} has constructed the abstract syntax tree. It performs:
 * </p>
 * <ul>
 *   <li><strong>Type Resolution:</strong> Resolves unattributed type references to their declarations,
 *       converting {@link ProtobufUnresolvedTypeReference} to concrete type references</li>
 *   <li><strong>Import Attribution:</strong> Processes import statements and links imported documents
 *       to the importing document, enabling cross-file type resolution</li>
 *   <li><strong>Semantic Validation:</strong> Validates Protocol Buffer language rules including:
 *       <ul>
 *         <li>Field number uniqueness and range validity</li>
 *         <li>Reserved field number and name enforcement</li>
 *         <li>Enum constant uniqueness and first value zero requirement (proto3)</li>
 *         <li>Oneof field constraints</li>
 *         <li>Map key type restrictions</li>
 *         <li>Extension range validity</li>
 *         <li>Option type checking</li>
 *       </ul>
 *   </li>
 *   <li><strong>Built-in Type Integration:</strong> Automatically includes Google's well-known types
 *       and descriptor.proto for option validation</li>
 * </ul>
 * <p>
 * The analyzer reports every problem it finds instead of stopping at the first one. Declarations
 * that depend on a problematic declaration, such as a field whose type cannot be resolved, are
 * validated only as far as the problem allows, so that a single mistake does not produce a cascade
 * of unrelated diagnostics.
 * </p>
 * <p>
 * The analyzer is automatically invoked by {@link ProtobufParser} methods, so direct use is typically
 * not necessary unless implementing custom parsing workflows.
 * </p>
 *
 * @see ProtobufParser
 * @see ProtobufDocumentTree
 */
public final class ProtobufAnalyzer {
    private static final String TYPE_SELECTOR = ".";
    private static final String TYPE_SELECTOR_SPLITTER = "\\.";

    private static final int FIELD_NUMBER_MIN = ProtobufMessage.MIN_FIELD_INDEX;
    private static final int FIELD_NUMBER_MAX = ProtobufMessage.MAX_FIELD_INDEX;

    private static final long ENUM_CONSTANT_MIN = ProtobufEnum.MIN_CONSTANT_INDEX;
    private static final long ENUM_CONSTANT_MAX = ProtobufEnum.MAX_CONSTANT_INDEX;

    private static final int RESERVED_RANGE_MIN = 19_000;
    private static final int RESERVED_RANGE_MAX = 19_999;

    private static final long INT32_MIN = Integer.MIN_VALUE;
    private static final long INT32_MAX = Integer.MAX_VALUE;

    private static final long UINT32_MIN = 0L;
    private static final long UINT32_MAX = 4294967295L;

    private static final long UINT64_MIN = 0;
    private static final String UINT64_MAX = "18446744073709551615";

    private static final Map<Class<? extends ProtobufTree>, String> TYPE_OPTIONS_MAP = Map.ofEntries(
            Map.entry(ProtobufDocumentTree.class, "google.protobuf.FileOptions"),
            Map.entry(ProtobufMessageStatement.class, "google.protobuf.MessageOptions"),
            Map.entry(ProtobufEnumStatement.class, "google.protobuf.EnumOptions"),
            Map.entry(ProtobufOneofStatement.class, "google.protobuf.OneOfOptions"),
            Map.entry(ProtobufEnumConstantStatement.class, "google.protobuf.EnumValueOptions"),
            Map.entry(ProtobufFieldStatement.class, "google.protobuf.FieldOptions"),
            Map.entry(ProtobufServiceStatement.class, "google.protobuf.ServiceOptions"),
            Map.entry(ProtobufMethodStatement.class, "google.protobuf.MethodOptions"),
            Map.entry(ProtobufExtendStatement.class, "google.protobuf.ExtensionRangeOptions"),
            Map.entry(ProtobufExtensionsStatement.class, "google.protobuf.ExtensionRangeOptions")
    );

    private static final Map<String, ProtobufDocumentTree> BUILT_IN_TYPES;
    private static final Map<String, Map<String, ProtobufFieldStatement>> BUILT_IN_OPTIONS;

    static {
        var diagnostics = new ProtobufDiagnosticator();
        List<ProtobufDocumentTree> collected = new ArrayList<>(WellKnownProtos.NAMES.size());
        try {
            for (var name : WellKnownProtos.NAMES) {
                try (var stream = WellKnownProtos.open(name)) {
                    if (stream == null) {
                        throw new ExceptionInInitializerError("Missing built-in proto file: " + name);
                    }
                    try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        var tree = ProtobufParser.doParse(Path.of(name), reader, diagnostics);
                        collected.add(tree);
                    }
                }
            }
            attribute(collected, diagnostics);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }

        try {
            diagnostics.throwIfErrors();
        } catch (ProtobufParserException exception) {
            throw new ExceptionInInitializerError(exception);
        }

        Map<String, ProtobufDocumentTree> types = HashMap.newHashMap(collected.size());
        for (var tree : collected) {
            types.put(tree.qualifiedPath(), tree);
        }
        BUILT_IN_TYPES = Collections.unmodifiableMap(types);

        var descriptor = BUILT_IN_TYPES.get("google/protobuf/descriptor.proto");
        if (descriptor == null) {
            throw new ExceptionInInitializerError("Missing descriptor.proto");
        }

        BUILT_IN_OPTIONS = getOptionsForDescriptor(descriptor);
    }

    private static Map<String, Map<String, ProtobufFieldStatement>> getOptionsForDescriptor(ProtobufDocumentTree descriptorDocument) {
        return descriptorDocument.children()
                .stream()
                .filter(child -> child instanceof ProtobufMessageStatement messageStatement && messageStatement.name().endsWith("Options"))
                .map(child -> (ProtobufMessageStatement) child)
                .collect(Collectors.toUnmodifiableMap(ProtobufMessageStatement::qualifiedName, ProtobufAnalyzer::getOptionsForDescriptor));
    }

    private static Map<String, ProtobufFieldStatement> getOptionsForDescriptor(ProtobufMessageStatement statement) {
        return statement.children()
                .stream()
                .filter(child -> child instanceof ProtobufFieldStatement fieldStatement
                        && !Objects.equals(fieldStatement.name(), "uninterpreted_options"))
                .map(child -> (ProtobufFieldStatement) child)
                .collect(Collectors.toUnmodifiableMap(ProtobufFieldStatement::name, Function.identity()));
    }

    /**
     * The collector that receives the diagnostics reported during the analysis.
     */
    private final ProtobufDiagnosticator diagnostics;

    /**
     * The trees whose unresolved type has already been reported, so that it is not reported again
     * when the same tree is attributed from a different context.
     */
    private final Set<ProtobufTree> unresolvedTypes;

    /**
     * The file of the document being analyzed, or {@code null} if it was not read from a file.
     */
    private Path location;

    /**
     * A field of a message, recorded to report which field already uses a field number.
     *
     * @param name the name of the field
     * @param tree the field, which is either a regular field or a group
     */
    private record IndexedField(String name, ProtobufTree tree) {
    }

    /**
     * Constructs a new analyzer that reports to the given collector.
     *
     * @param diagnostics the collector that receives the diagnostics reported during the analysis
     */
    private ProtobufAnalyzer(ProtobufDiagnosticator diagnostics) {
        this.diagnostics = diagnostics;
        this.unresolvedTypes = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Performs semantic analysis on a collection of parsed Protocol Buffer documents.
     * <p>
     * This method orchestrates the complete semantic analysis process:
     * </p>
     * <ol>
     *   <li>Builds a map of all documents including built-in Google types</li>
     *   <li>Attributes (resolves) import statements for all documents</li>
     *   <li>Attributes type references and validates each document</li>
     * </ol>
     * <p>
     * Documents can reference types defined in other documents through imports or within the
     * same package. The analyzer ensures all type references are valid and properly resolved.
     * </p>
     *
     * @param documents the collection of parsed documents to analyze, must not be null
     * @throws ProtobufParserException if any semantic error is found
     */
    public static void attribute(Collection<ProtobufDocumentTree> documents) throws ProtobufParserException {
        var diagnostics = new ProtobufDiagnosticator();
        attribute(documents, diagnostics);
        diagnostics.throwIfErrors();
    }

    /**
     * Performs semantic analysis on a collection of parsed Protocol Buffer documents, reporting
     * every problem to the given collector instead of throwing.
     *
     * @param documents the collection of parsed documents to analyze
     * @param diagnostics the collector that receives the diagnostics reported during the analysis
     */
    static void attribute(Collection<ProtobufDocumentTree> documents, ProtobufDiagnosticator diagnostics) {
        var analyzer = new ProtobufAnalyzer(diagnostics);
        var canonicalPathToDocumentMap = buildImportsMap(documents);

        for (var document : documents) {
            analyzer.attributeImports(document, canonicalPathToDocumentMap);
        }

        for (var document : documents) {
            analyzer.attributeDocument(document);
        }
    }

    /**
     * Reports the given problem at the range of the given tree.
     *
     * @param tree the tree that contains the problem
     * @param message the problem to report
     */
    private void report(ProtobufTree tree, ProtobufDiagnosticMessage message) {
        diagnostics.report(location, tree.range(), message);
    }

    /**
     * Reports the given problem at the range of the given tree if {@code condition} is
     * {@code false}.
     *
     * @param condition the condition that must hold
     * @param tree the tree that contains the problem
     * @param message the problem to report if the condition does not hold
     * @return {@code condition}
     */
    private boolean check(boolean condition, ProtobufTree tree, ProtobufDiagnosticMessage message) {
        if (!condition) {
            report(tree, message);
        }
        return condition;
    }

    private static Map<String, ProtobufDocumentTree> buildImportsMap(Collection<ProtobufDocumentTree> documents) {
        var queue = new LinkedList<>(documents);
        var result = new HashMap<String, ProtobufDocumentTree>();
        while(!queue.isEmpty()) {
            var document = queue.poll();
            if (result.put(document.qualifiedPath(), document) == null) {
                document.getDirectChildrenByType(ProtobufImportStatement.class)
                        .filter(ProtobufImportStatement::hasDocument)
                        .forEachOrdered(importStatement -> queue.add(importStatement.document()));
            }
        }
        return result;
    }

    private void attributeImports(ProtobufDocumentTree document, Map<String, ProtobufDocumentTree> canonicalPathToDocumentMap) {
        location = document.location().orElse(null);
        var importStatements = document.getDirectChildrenByType(ProtobufImportStatement.class).toList();
        for (var importStatement : importStatements) {
            if (importStatement.isAttributed()) {
                continue;
            }

            var imported = canonicalPathToDocumentMap.get(importStatement.location());
            if (imported == null && BUILT_IN_TYPES != null) {
                imported = BUILT_IN_TYPES.get(importStatement.location());
            }
            if (imported == null) {
                report(importStatement, ProtobufSemanticErrors.unresolvedImport(importStatement.location()));
                continue;
            }
            importStatement.setDocument(imported);
        }
    }

    private void attributeDocument(ProtobufDocumentTree document) {
        location = document.location().orElse(null);
        var enforceNaming = document.namingStyle() == ProtobufNamingStyle.STYLE2024;
        if(enforceNaming) {
            validateNamingStyle(document);
        }

        var queue = new LinkedList<ProtobufTree>();
        queue.add(document);
        while (!queue.isEmpty()) {
            var tree = queue.removeFirst();

            if(tree instanceof ProtobufTree.WithBody<?> withBody) {
                queue.addAll(withBody.children());
            }

            if(enforceNaming) {
                validateNamingStyleForTree(tree);
            }

            switch (tree) {
                case ProtobufCommentStatement _ -> {} // Nothing to do

                case ProtobufEmptyStatement _ -> {} // Nothing to do

                case ProtobufExtendStatement extendStatement -> validateExtendDeclaration(document, extendStatement);

                case ProtobufExtensionsStatement extensionsStatement -> validateExtensionsStatementOptions(document, extensionsStatement);

                case ProtobufFieldStatement protobufField -> validateField(document, protobufField);

                case ProtobufOptionStatement protobufOption -> validateOption(document, protobufOption);

                case ProtobufPackageStatement _, ProtobufReservedStatement _ -> {} // Nothing to do

                case ProtobufEnumStatement enumStatement -> validateEnum(enumStatement);

                case ProtobufImportStatement _ -> {} // Nothing to do

                case ProtobufMessageStatement messageStatement -> validateMessage(document, messageStatement);

                case ProtobufMethodStatement methodStatement -> validateServiceMethod(document, methodStatement);

                case ProtobufServiceStatement serviceStatement -> validateService(serviceStatement);

                case ProtobufSyntaxStatement _, ProtobufEditionStatement _ -> {} // Nothing to do

                case ProtobufDocumentTree documentTree -> {
                    var names = new HashSet<String>();
                    for(var child : documentTree.children()) {
                        if(child instanceof ProtobufTree.WithName withName && withName.hasName() && !names.add(withName.name())) {
                            report(child, ProtobufSemanticErrors.duplicateTypeName(withName.name()));
                        }
                    }
                }

                case ProtobufEnumConstantStatement enumConstant -> validateEnumConstant(document, enumConstant);

                case ProtobufGroupStatement groupStatement -> validateGroupField(document, groupStatement);

                case ProtobufOneofStatement oneofStatement -> validateOneofField(oneofStatement);
            }
        }
    }

    private void validateMessage(ProtobufDocumentTree document, ProtobufMessageStatement messageStatement) {
        validateReserved(messageStatement, FIELD_NUMBER_MIN, FIELD_NUMBER_MAX);
        validateExtensions(document, messageStatement);

        var names = new LinkedHashMap<String, ProtobufTree>();
        var jsonNames = new HashMap<String, String>();
        var reservedNames = new HashSet<String>();
        var indexes = new LinkedHashMap<Long, IndexedField>();
        var reservedIndexes = new TreeMap<Long, Long>();
        var extensibleIndexes = new TreeMap<Long, Long>();
        for(var child : messageStatement.children()) {
            switch (child) {
                case ProtobufFieldStatement fieldStatement -> {
                    if (fieldStatement.hasName() && names.putIfAbsent(fieldStatement.name(), fieldStatement) != null) {
                        report(fieldStatement, ProtobufSemanticErrors.duplicateFieldName(fieldStatement.name()));
                    } else if(document.jsonCompatibility() == ProtobufJsonCompatibility.ENABLED) {
                        var jsonFieldName = fieldStatement.getOption("json_name")
                                .map(jsonName -> jsonName.value() instanceof ProtobufLiteralExpression(var value) ? value : null)
                                .orElseGet(() -> toJsonName(fieldStatement.name()));
                        var previousField = jsonFieldName == null ? null : jsonNames.putIfAbsent(jsonFieldName, fieldStatement.name());
                        if(previousField != null) {
                            report(fieldStatement, ProtobufSemanticErrors.duplicateJsonFieldName(fieldStatement.name(), jsonFieldName, previousField));
                        }
                    }

                    registerFieldIndex(indexes, fieldStatement);
                }

                case ProtobufGroupStatement groupStatement -> {
                    if (groupStatement.hasName() && names.putIfAbsent(groupStatement.name(), groupStatement) != null) {
                        report(groupStatement, ProtobufSemanticErrors.duplicateFieldName(groupStatement.name()));
                    }

                    registerFieldIndex(indexes, groupStatement);
                }

                case ProtobufOneofStatement oneofStatement -> {
                    if (oneofStatement.hasName() && names.putIfAbsent(oneofStatement.name(), oneofStatement) != null) {
                        report(oneofStatement, ProtobufSemanticErrors.duplicateName(oneofStatement.name()));
                    }

                    for(var oneofChild : oneofStatement.children()) {
                        switch (oneofChild) {
                            case ProtobufFieldStatement fieldStatement -> {
                                if (fieldStatement.hasName() && names.putIfAbsent(fieldStatement.name(), fieldStatement) != null) {
                                    report(fieldStatement, ProtobufSemanticErrors.duplicateFieldName(fieldStatement.name()));
                                }

                                registerFieldIndex(indexes, fieldStatement);
                            }

                            case ProtobufGroupStatement groupStatement -> {
                                if (groupStatement.hasName() && names.putIfAbsent(groupStatement.name(), groupStatement) != null) {
                                    report(groupStatement, ProtobufSemanticErrors.duplicateFieldName(groupStatement.name()));
                                }

                                registerFieldIndex(indexes, groupStatement);
                            }

                            case ProtobufOptionStatement _ -> {} // Do nothing
                        }
                    }
                }

                case ProtobufEmptyStatement _ -> {} // Do nothing

                case ProtobufEnumStatement enumStatement -> {
                    if (enumStatement.hasName() && names.putIfAbsent(enumStatement.name(), enumStatement) != null) {
                        report(enumStatement, ProtobufSemanticErrors.duplicateName(enumStatement.name()));
                    }

                    for(var enumChild : enumStatement.children()) {
                        switch (enumChild) {
                            case ProtobufEnumConstantStatement enumConstantStatement -> {
                                if(names.putIfAbsent(enumConstantStatement.name(), enumConstantStatement) != null) {
                                    report(enumConstantStatement, ProtobufSemanticErrors.duplicateFieldName(enumConstantStatement.name()));
                                }
                            }

                            case ProtobufEmptyStatement _,
                                 ProtobufOptionStatement _,
                                 ProtobufReservedStatement _ -> {} // Do nothing
                        }
                    }
                }

                case ProtobufExtendStatement extendStatement -> {
                    for(var extendChild : extendStatement.children()) {
                        switch (extendChild) {
                            case ProtobufFieldStatement fieldStatement -> {
                                if(names.putIfAbsent(fieldStatement.name(), fieldStatement) != null) {
                                    report(fieldStatement, ProtobufSemanticErrors.duplicateFieldName(fieldStatement.name()));
                                }
                            }


                            case ProtobufGroupStatement groupStatement -> {
                                if(names.putIfAbsent(groupStatement.name(), groupStatement) != null) {
                                    report(groupStatement, ProtobufSemanticErrors.duplicateFieldName(groupStatement.name()));
                                }
                            }
                        }
                    }
                }

                case ProtobufReservedStatement reservedStatement -> {
                    for(var expression : reservedStatement.expressions()) {
                        switch (expression) {
                            case ProtobufIntegerExpression integerExpression -> {
                                var value = integerExpression.value().value();

                                var overlap = findOverlap(extensibleIndexes, value, value);
                                if (overlap != null) {
                                    report(reservedStatement, ProtobufSemanticErrors.extensionRangeOverlapsReservedRange(
                                            formatRange(value, value, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                reservedIndexes.put(value, value);
                            }

                            case ProtobufIntegerRangeExpression rangeExpression -> {
                                var min = rangeExpression.value()
                                        .lowerBound()
                                        .value();
                                var max = rangeExpression.value()
                                        .upperBound()
                                        .map(ProtobufInteger::value)
                                        .orElse((long) FIELD_NUMBER_MAX);

                                var overlap = findOverlap(extensibleIndexes, min, max);
                                if (overlap != null) {
                                    report(reservedStatement, ProtobufSemanticErrors.extensionRangeOverlapsReservedRange(
                                            formatRange(min, max, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                reservedIndexes.put(min, max);
                            }

                            case ProtobufLiteralExpression literalExpression -> reservedNames.add(literalExpression.value());
                        }
                    }
                }

                case ProtobufExtensionsStatement extensionsStatement -> {
                    for(var expression : extensionsStatement.expressions()) {
                        switch (expression) {
                            case ProtobufIntegerExpression integerExpression -> {
                                var value = integerExpression.value().value();

                                var overlap = findOverlap(reservedIndexes, value, value);
                                if (overlap != null) {
                                    report(extensionsStatement, ProtobufSemanticErrors.reservedRangeOverlapsExtensionRange(
                                            formatRange(value, value, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                extensibleIndexes.put(value, value);
                            }

                            case ProtobufIntegerRangeExpression rangeExpression -> {
                                var min = rangeExpression.value()
                                        .lowerBound()
                                        .value();
                                var max = rangeExpression.value()
                                        .upperBound()
                                        .map(ProtobufInteger::value)
                                        .orElse((long) FIELD_NUMBER_MAX);

                                var overlap = findOverlap(reservedIndexes, min, max);
                                if (overlap != null) {
                                    report(extensionsStatement, ProtobufSemanticErrors.reservedRangeOverlapsExtensionRange(
                                            formatRange(min, max, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                extensibleIndexes.put(min, max);
                            }
                        }
                    }
                }


                case ProtobufMessageStatement protobufMessageStatement -> {
                    if (protobufMessageStatement.hasName() && names.putIfAbsent(protobufMessageStatement.name(), protobufMessageStatement) != null) {
                        report(protobufMessageStatement, ProtobufSemanticErrors.duplicateName(protobufMessageStatement.name()));
                    }
                }

                case ProtobufOptionStatement _ -> {} // Do nothing
            }
        }

        for (var entry : names.entrySet()) {
            if (reservedNames.contains(entry.getKey())) {
                report(entry.getValue(), ProtobufSemanticErrors.reservedFieldName(entry.getKey()));
            }
        }

        for(var entry : indexes.entrySet()) {
            var value = entry.getKey();
            var field = entry.getValue();
            if (findOverlap(reservedIndexes, value, value) != null) {
                report(field.tree(), ProtobufSemanticErrors.reservedFieldIndex(value, field.name()));
                continue;
            }

            var extensionRange = findOverlap(extensibleIndexes, value, value);
            if (extensionRange != null) {
                report(field.tree(), ProtobufSemanticErrors.extensibleFieldIndex(value, field.name(), formatRange(extensionRange.getKey(), extensionRange.getValue(), FIELD_NUMBER_MAX)));
            }
        }
    }

    /**
     * Records the number of the given field, reporting an error if another field of the same
     * message already uses it.
     *
     * @param <T> the type of the field, which is either a regular field or a group
     * @param indexes the fields of the message indexed by their number
     * @param field the field to record
     */
    private <T extends ProtobufTree.WithName & ProtobufTree.WithIndex> void registerFieldIndex(Map<Long, IndexedField> indexes, T field) {
        if (!field.hasIndex()) {
            return;
        }

        var index = field.index().value();
        var previous = indexes.putIfAbsent(index, new IndexedField(field.name(), field));
        if (previous != null) {
            report(field, ProtobufSemanticErrors.duplicateFieldIndex(field.name(), index, previous.name()));
        }
    }

    /**
     * Returns the range of the given set of ranges that overlaps with the range from {@code min} to
     * {@code max}, both inclusive.
     *
     * @param ranges the ranges, mapping the start of each range to its inclusive end
     * @param min the start of the range to check
     * @param max the inclusive end of the range to check
     * @return the overlapping range, or {@code null} if no range overlaps
     */
    private static Map.Entry<Long, Long> findOverlap(NavigableMap<Long, Long> ranges, long min, long max) {
        var floor = ranges.floorEntry(min);
        if (floor != null && floor.getValue() >= min) {
            return floor;
        }

        var ceiling = ranges.ceilingEntry(min);
        if (ceiling != null && ceiling.getKey() <= max) {
            return ceiling;
        }

        return null;
    }

    /**
     * Returns the given range written as it would be in a reserved or extensions declaration.
     *
     * @param min the start of the range
     * @param max the inclusive end of the range
     * @param limit the largest valid number, which is written as {@code max}
     * @return the range rendered as text
     */
    private static String formatRange(long min, long max, long limit) {
        if (min == max) {
            return String.valueOf(min);
        }

        return max == limit ? min + " to max" : min + " to " + max;
    }

    /**
     * Returns the given extension ranges written as they would be in an extensions declaration.
     *
     * @param ranges the extension ranges, mapping the start of each range to its inclusive end
     * @return the ranges rendered as text and separated by commas, or {@code null} if there are no
     *         ranges
     */
    private static String formatRanges(NavigableMap<ProtobufInteger, ProtobufInteger> ranges) {
        if (ranges.isEmpty()) {
            return null;
        }

        var result = new StringJoiner(", ");
        for (var entry : ranges.entrySet()) {
            result.add(formatRange(entry.getKey().value(), entry.getValue().value(), FIELD_NUMBER_MAX));
        }
        return result.toString();
    }

    private void validateExtendDeclaration(ProtobufDocumentTree document, ProtobufExtendStatement extendStatement) {
        // Resolve the declaration type
        var reference = extendStatement.declaration();
        if (reference instanceof ProtobufUnresolvedTypeReference(var name)) {
            var resolvedType = tryResolveType(document, name, extendStatement);
            if (resolvedType == null) {
                report(extendStatement, ProtobufSemanticErrors.unresolvedExtendTarget(name));
                return;
            }
            extendStatement.setDeclaration(resolvedType);
            reference = resolvedType;
        }

        var declaration = switch (reference) {
            case ProtobufMessageTypeReference(var messageDeclaration) -> messageDeclaration;
            case ProtobufGroupTypeReference(var groupDeclaration) -> groupDeclaration;
            default -> null;
        };
        if (declaration == null) {
            report(extendStatement, ProtobufSemanticErrors.invalidExtendTarget(reference.name()));
            return;
        }

        var ranges = buildExtensionsLookup(declaration);
        for(var statement : extendStatement.children()) {
            switch (statement) {
                case ProtobufFieldStatement field -> {
                    // Extension fields cannot be required
                    check(field.modifier() != ProtobufModifier.REQUIRED,
                            field, ProtobufSemanticErrors.requiredExtensionField(field.name()));

                    // Extension fields cannot be map types
                    check(!(field.type() instanceof ProtobufMapTypeReference),
                            field, ProtobufSemanticErrors.mapExtensionField(field.name()));

                    // Validate field number is within declared extension ranges
                    var entry = ranges.floorEntry(field.index());
                    var extensible = entry != null && field.index().compareTo(entry.getValue()) <= 0;
                    check(extensible,
                            field, ProtobufSemanticErrors.extensionFieldOutsideRanges(field.name(), field.index().value(), declaration.name(), formatRanges(ranges)));
                }

                case ProtobufGroupStatement field -> {
                    // Extension fields cannot be required
                    check(field.modifier() != ProtobufModifier.REQUIRED,
                            field, ProtobufSemanticErrors.requiredExtensionField(field.name()));

                    // Extension fields cannot be map types
                    check(!(field.type() instanceof ProtobufMapTypeReference),
                            field, ProtobufSemanticErrors.mapExtensionField(field.name()));

                    // Validate field number is within declared extension ranges
                    var entry = ranges.floorEntry(field.index());
                    var extensible = entry != null && field.index().compareTo(entry.getValue()) <= 0;
                    check(extensible,
                            field, ProtobufSemanticErrors.extensionFieldOutsideRanges(field.name(), field.index().value(), declaration.name(), formatRanges(ranges)));
                }
            }
        }
    }

    private static TreeMap<ProtobufInteger, ProtobufInteger> buildExtensionsLookup(ProtobufTree.WithBody<?> declaration) {
        var ranges = new TreeMap<ProtobufInteger, ProtobufInteger>();
        declaration.getDirectChildrenByType(ProtobufExtensionsStatement.class).forEachOrdered(extensionsStatement -> {
            for(var expression : extensionsStatement.expressions()) {
                switch (expression) {
                    case ProtobufIntegerExpression integerExpression -> ranges.put(integerExpression.value(), integerExpression.value());
                    case ProtobufIntegerRangeExpression rangeExpression -> {
                        switch (rangeExpression.value()) {
                            case ProtobufIntegerRange.Bounded bounded -> ranges.put(bounded.min(), bounded.max());
                            case ProtobufIntegerRange.LowerBounded lowerBounded -> ranges.put(lowerBounded.min(), new ProtobufInteger(FIELD_NUMBER_MAX));
                        }
                    }
                }
            }
        });
        return ranges;
    }

    /**
     * Resolves a type name to a message, enum or group visible from the given context.
     *
     * <p>A name that references a declaration which is not a type, such as a service or a oneof, is
     * treated as unresolved.
     *
     * @param document the document that contains {@code context}
     * @param originalName the name to resolve, optionally qualified
     * @param context the tree that references the type
     * @return the resolved type, or {@code null} if no type with that name is visible
     */
    private static ProtobufTypeReference tryResolveType(ProtobufDocumentTree document, String originalName, ProtobufTree context) {
        var isGlobalScope = originalName.startsWith(TYPE_SELECTOR);
        var name = isGlobalScope ? originalName.substring(1) : originalName;
        var types = name.split(TYPE_SELECTOR_SPLITTER);

        // Look for type in parent scope (if not global scope)
        ProtobufTree.WithBody<?> resolvedType = null;
        if(!isGlobalScope) {
            resolvedType = findTypeInParentScope(context.parent(), types[0]);
            if(resolvedType != null) {
                resolvedType = resolveNestedTypes(resolvedType, types, 1);
            }
        }

        // If not found in parent scope, try imports and current document
        if(resolvedType == null) {
            var visibleImports = collectVisibleImports(document);
            var documentsToCheck = new ArrayList<ProtobufDocumentTree>(visibleImports.size() + 1);
            documentsToCheck.add(document);
            documentsToCheck.addAll(visibleImports);
            resolvedType = findTypeInDocuments(documentsToCheck, name);
        }

        // TODO: Bug with ProtobufExtendStatement
        return switch (resolvedType) {
            case ProtobufEnumStatement enumeration -> new ProtobufEnumTypeReference(enumeration);
            case ProtobufMessageStatement message -> new ProtobufMessageTypeReference(message);
            case ProtobufGroupStatement protobufGroupStatement -> new ProtobufGroupTypeReference(protobufGroupStatement);
            case ProtobufOneofStatement _,
                 ProtobufServiceStatement _,
                 ProtobufDocumentTree _,
                 ProtobufExtendStatement _,
                 ProtobufMethodStatement _ -> null;
            case null -> null;
        };
    }

    private static ProtobufTree.WithBody<?> findTypeInParentScope(ProtobufTree parent, String typeName) {
        while (parent != null) {
            if (parent instanceof ProtobufTree.WithBody<?> withBody) {
                var found = withBody.getDirectChildByNameAndType(typeName, ProtobufTree.WithBody.class)
                        .orElse(null);
                if(found != null) {
                    return found;
                }
                parent = parent.parent() instanceof ProtobufTree.WithBody<?> validParent ? validParent : null;
            } else {
                parent = null;
            }
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private static ProtobufTree.WithBody<?> resolveNestedTypes(ProtobufTree.WithBody<?> current, String[] types, int startIndex) {
        for (var i = startIndex; i < types.length && current != null; i++) {
            current = current.getDirectChildByNameAndType(types[i], ProtobufTree.WithBody.class)
                    .orElse(null);
        }
        return current;
    }

    private static ProtobufTree.WithBody<?> findTypeInDocuments(List<ProtobufDocumentTree> documents, String name) {
        for (var documentToCheck : documents) {
            var typeName = documentToCheck.packageName()
                    .map(packageName -> name.startsWith(packageName + TYPE_SELECTOR) ? name.substring(packageName.length() + 1) : null)
                    .orElse(name)
                    .split(TYPE_SELECTOR_SPLITTER);

            if (typeName.length == 0) {
                continue;
            }

            var resolvedType = documentToCheck.getDirectChildByNameAndType(typeName[0], ProtobufTree.WithBody.class).orElse(null);
            resolvedType = resolveNestedTypes(resolvedType, typeName, 1);

            if (resolvedType != null) {
                return resolvedType;
            }
        }
        return null;
    }

    private void attributeFieldOption(ProtobufDocumentTree document, ProtobufFieldStatement protobufField, ProtobufOptionExpression option) {
        if(BUILT_IN_OPTIONS == null) {
            return; // Options validation cannot happen during the bootstrap phase
        }

        var segments = option.name().segments();
        if(segments.isEmpty()) {
            return;
        }

        var firstSegment = segments.getFirst();
        if(firstSegment.hasName("features") && !firstSegment.isExtension()) {
            check(document.version().isEdition(),
                    protobufField, ProtobufSemanticErrors.featuresRequireEditions());
            if(segments.size() >= 2 && segments.get(1).hasName("field_presence")) {
                check(protobufField.modifier() != ProtobufModifier.REPEATED,
                        protobufField, ProtobufSemanticErrors.repeatedFieldPresence(protobufField.name()));
            }
        }

        switch (firstSegment.name()) {
            case "default" -> validateDefaultOption(document, protobufField, option);
            case "packed" -> validatePackedOption(document, protobufField);
            case "json_name" -> validateJsonNameOption(protobufField, option);
            case "ctype" -> check(document.version() != ProtobufVersion.EDITION_2024,
                    protobufField, ProtobufSemanticErrors.ctypeInEdition2024(protobufField.name()));
        }

        attributeCompactOption(document, ProtobufFieldStatement.class, protobufField, option);
    }

    private void validateDefaultOption(ProtobufDocumentTree document, ProtobufFieldStatement protobufField, ProtobufOptionExpression option) {
        if (!check(document.version() != ProtobufVersion.PROTOBUF_3,
                protobufField, ProtobufSemanticErrors.defaultValueInProto3(protobufField.name()))) {
            return;
        }

        if (!check(protobufField.fieldPresence() != ProtobufFieldPresence.IMPLICIT,
                protobufField, ProtobufSemanticErrors.defaultValueWithImplicitPresence(protobufField.name()))) {
            return;
        }

        if (!check(protobufField.modifier() != ProtobufModifier.REPEATED,
                protobufField, ProtobufSemanticErrors.defaultValueOnRepeatedField(protobufField.name()))) {
            return;
        }

        // Do not validate unresolved types: the unresolved type was already reported
        if (!(protobufField.type() instanceof ProtobufValueTypeReference valueType)) {
            if (protobufField.type() instanceof ProtobufMapTypeReference) {
                report(protobufField, ProtobufSemanticErrors.defaultValueOnInvalidType(protobufField.name(), protobufField.type()));
            }
            return;
        }

        var fieldType = valueType.protobufType();
        var value = option.value();

        switch (fieldType) {
            case MESSAGE -> report(protobufField, ProtobufSemanticErrors.defaultValueOnInvalidType(protobufField.name(), protobufField.type()));

            case ENUM -> {
                if (!(value instanceof ProtobufEnumConstantExpression enumExpr)) {
                    report(protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)));
                } else if (protobufField.type() instanceof ProtobufEnumTypeReference(var declaration)) {
                    var constant = declaration.getDirectChildByNameAndType(enumExpr.name(), ProtobufEnumConstantStatement.class);
                    check(constant.isPresent(),
                            protobufField, ProtobufSemanticErrors.defaultValueUnknownEnumConstant(enumExpr.name(), declaration.name()));
                }
            }

            case FLOAT, DOUBLE -> {
                // Accepts any numeric value: integers, finite floats, inf, -inf, nan
                check(value instanceof ProtobufNumberExpression,
                        protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)));
            }

            case BOOL -> check(value instanceof ProtobufBoolExpression,
                    protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)));

            case STRING, BYTES -> check(value instanceof ProtobufLiteralExpression,
                    protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)));

            case INT32, SINT32, SFIXED32 -> {
                if (check(value instanceof ProtobufIntegerExpression,
                        protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)))) {
                    var intValue = ((ProtobufIntegerExpression) value).value().value();
                    check(intValue >= INT32_MIN && intValue <= INT32_MAX,
                            protobufField, ProtobufSemanticErrors.defaultValueOutOfRange(intValue, protobufField.name(), fieldType, String.valueOf(INT32_MIN), String.valueOf(INT32_MAX)));
                }
            }

            case UINT32, FIXED32 -> {
                if (check(value instanceof ProtobufIntegerExpression,
                        protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)))) {
                    var intValue = ((ProtobufIntegerExpression) value).value().value();
                    check(intValue >= UINT32_MIN && intValue <= UINT32_MAX,
                            protobufField, ProtobufSemanticErrors.defaultValueOutOfRange(intValue, protobufField.name(), fieldType, String.valueOf(UINT32_MIN), String.valueOf(UINT32_MAX)));
                }
            }

            case INT64, SINT64, SFIXED64 -> {
                // long is always a valid int64
                check(value instanceof ProtobufIntegerExpression,
                        protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)));
            }

            case UINT64, FIXED64 -> {
                if (check(value instanceof ProtobufIntegerExpression,
                        protobufField, ProtobufSemanticErrors.defaultValueTypeMismatch(protobufField.name(), fieldType, String.valueOf(value)))) {
                    var intValue = ((ProtobufIntegerExpression) value).value().value();
                    check(intValue >= UINT64_MIN,
                            protobufField, ProtobufSemanticErrors.defaultValueOutOfRange(intValue, protobufField.name(), fieldType, String.valueOf(UINT64_MIN), UINT64_MAX));
                }
            }
        }
    }

    private void validatePackedOption(ProtobufDocumentTree document, ProtobufFieldStatement protobufField) {
        if (!check(!document.version().isEdition(),
                protobufField, ProtobufSemanticErrors.packedInEditions(protobufField.name()))) {
            return;
        }

        if (!check(protobufField.modifier() == ProtobufModifier.REPEATED,
                protobufField, ProtobufSemanticErrors.packedOnNonRepeatedField(protobufField.name()))) {
            return;
        }

        // An unresolved type was already reported, and a map field is never repeated
        if (!(protobufField.type() instanceof ProtobufValueTypeReference valueType)) {
            return;
        }

        var type = valueType.protobufType();
        check(type.isPackable(),
                protobufField, ProtobufSemanticErrors.packedOnNonPackableField(protobufField.name(), type));
    }

    private void validateJsonNameOption(ProtobufFieldStatement protobufField, ProtobufOptionExpression option) {
        // json_name value must be a string
        var value = option.value();
        check(value instanceof ProtobufLiteralExpression,
                protobufField, ProtobufSemanticErrors.jsonNameNotString(protobufField.name(), String.valueOf(value)));
    }

    /**
     * Resolves the type of the given tree, reporting an error if it cannot be resolved.
     *
     * <p>An unresolved type is reported at most once per tree, and the tree keeps its
     * {@link ProtobufUnresolvedTypeReference} so that validations depending on the type can skip it.
     *
     * @param document the document that contains {@code typedFieldTree}
     * @param typedFieldTree the tree whose type should be resolved
     * @return {@code true} if the type is resolved, otherwise {@code false}
     */
    private boolean attributeType(ProtobufDocumentTree document, ProtobufTree.WithType typedFieldTree) {
        var typeReference = typedFieldTree.type();
        if(typeReference.isAttributed()) {
            return true;
        }

        // Handle map types specially - need to attribute key and value types
        if(typeReference instanceof ProtobufMapTypeReference mapType) {
            return attributeMapType(document, typedFieldTree, mapType);
        }

        var resolvedType = typeReference instanceof ProtobufUnresolvedTypeReference(var originalName)
                ? tryResolveType(document, originalName, typedFieldTree)
                : null;
        if(resolvedType == null) {
            if (unresolvedTypes.add(typedFieldTree)) {
                var parent = typedFieldTree.parent() instanceof ProtobufTree.WithName withName ? withName.name() : null;
                report(typedFieldTree, ProtobufSemanticErrors.unresolvedFieldType(typeReference.name(), getTreeName(typedFieldTree), parent));
            }
            return false;
        }

        typedFieldTree.setType(resolvedType);
        return true;
    }

    private static String getTreeName(ProtobufTree tree) {
        return tree instanceof ProtobufTree.WithName withName && withName.hasName() ? withName.name() : "<unknown>";
    }

    private boolean attributeMapType(ProtobufDocumentTree document, ProtobufTree.WithType typedFieldTree, ProtobufMapTypeReference mapType) {
        var hasUpdate = false;
        var resolved = true;

        var keyType = mapType.keyType();
        if(keyType instanceof ProtobufUnresolvedTypeReference(var keyName)) {
            var resolvedKeyType = tryResolveType(document, keyName, typedFieldTree);
            if (resolvedKeyType == null) {
                if (unresolvedTypes.add(typedFieldTree)) {
                    report(typedFieldTree, ProtobufSemanticErrors.unresolvedMapKeyType(keyName, getTreeName(typedFieldTree)));
                }
                resolved = false;
            } else {
                keyType = resolvedKeyType;
                hasUpdate = true;
            }
        }

        // Attribute value type
        var valueType = mapType.valueType();
        if(valueType instanceof ProtobufUnresolvedTypeReference(var valueName)) {
            var resolvedValueType = tryResolveType(document, valueName, typedFieldTree);
            if (resolvedValueType == null) {
                if (resolved && unresolvedTypes.add(typedFieldTree)) {
                    report(typedFieldTree, ProtobufSemanticErrors.unresolvedMapValueType(valueName, getTreeName(typedFieldTree)));
                }
                resolved = false;
            } else {
                valueType = resolvedValueType;
                hasUpdate = true;
            }
        }

        if(resolved && hasUpdate) {
            var updatedType = new ProtobufMapTypeReference(keyType, valueType);
            typedFieldTree.setType(updatedType);
        }

        return resolved;
    }

    private static List<ProtobufDocumentTree> collectVisibleImports(ProtobufDocumentTree document) {
        var result = new ArrayList<ProtobufDocumentTree>();
        var visited = new HashSet<ProtobufDocumentTree>();

        record Entry(ProtobufDocumentTree document, boolean includeNonPublic) { }
        var stack = new ArrayDeque<Entry>();
        stack.push(new Entry(document, true));

        while (!stack.isEmpty()) {
            var entry = stack.pop();
            if (visited.add(entry.document())) {
                entry.document().getDirectChildrenByType(ProtobufImportStatement.class)
                        .filter(ProtobufImportStatement::hasDocument)
                        .forEachOrdered(importStatement -> {
                            var imported = importStatement.document();
                            var isPublic = importStatement.modifier() == ProtobufImportStatement.Modifier.PUBLIC;

                            if (entry.includeNonPublic() || isPublic) {
                                result.add(imported);
                            }

                            stack.push(new Entry(imported, false));
                        });
            }
        }

        var builtInTypes = BUILT_IN_TYPES;
        if (builtInTypes != null) {
            result.addAll(builtInTypes.values());
        }

        return result;
    }

    private void validateOption(ProtobufDocumentTree document, ProtobufOptionStatement optionStatement) {
        if(BUILT_IN_OPTIONS == null) {
            return; // Options validation cannot happen during the bootstrap phase
        }

        var segments = optionStatement.name().segments();
        if(segments.isEmpty()) {
            return;
        }

        var firstSegment = segments.getFirst();
        if(firstSegment.hasName("features") && !firstSegment.isExtension()) {
            check(document.version().isEdition(),
                    optionStatement, ProtobufSemanticErrors.featuresRequireEditions());
        }

        var optionsMapKey = TYPE_OPTIONS_MAP.get(optionStatement.parent().getClass());
        if(optionsMapKey == null) {
            return;
        }

        var definition = resolveOptionDefinition(document, optionsMapKey, optionStatement.name(), optionStatement, true);
        if (definition == null) {
            return;
        }

        optionStatement.setDefinition(definition);
        validateOptionValueType(optionStatement);
    }

    private ProtobufOptionDefinition resolveOptionDefinition(ProtobufDocumentTree document, String optionsMapKey, ProtobufOptionName optionName, ProtobufTree source, boolean failOnMissing) {
        var optionsMap = BUILT_IN_OPTIONS.get(optionsMapKey);
        if(optionsMap == null) {
            return null;
        }

        ProtobufOptionDefinition definition = null;
        var contextTypeName = optionsMapKey;
        for(var segment : optionName.segments()) {
            if(segment.isExtension()) {
                definition = findExtensionField(document, contextTypeName, segment.name());
                if (definition == null) {
                    if (failOnMissing) {
                        report(source, ProtobufSemanticErrors.unknownCustomOption(segment.name(), contextTypeName));
                    }
                    return null;
                }
            }else if(definition == null) {
                definition = optionsMap.get(segment.name());
                if (definition == null) {
                    if (failOnMissing) {
                        report(source, ProtobufSemanticErrors.unknownOption(segment.name(), optionsMapKey));
                    }
                    return null;
                }
            }else {
                definition = findSimpleField(segment, definition);
                if (definition == null) {
                    if (failOnMissing) {
                        report(source, ProtobufSemanticErrors.unresolvedOptionField(segment.name(), optionName.toString()));
                    }
                    return null;
                }
            }

            if(definition.type() instanceof ProtobufUnresolvedTypeReference && !attributeType(document, definition)) {
                return null;
            }

            contextTypeName = switch(definition.type()) {
                case ProtobufMessageTypeReference(var decl) -> decl.qualifiedName();
                case ProtobufGroupTypeReference(var decl) -> decl.qualifiedName();
                default -> null;
            };
        }

        return definition;
    }

    private void attributeCompactOption(ProtobufDocumentTree document, Class<? extends ProtobufTree> parentClass, ProtobufTree source, ProtobufOptionExpression option) {
        if (BUILT_IN_OPTIONS == null) {
            return;
        }
        var optionsMapKey = TYPE_OPTIONS_MAP.get(parentClass);
        if (optionsMapKey == null) {
            return;
        }
        var definition = resolveOptionDefinition(document, optionsMapKey, option.name(), source, false);
        if (definition != null) {
            option.setDefinition(definition);
        }
    }

    private static ProtobufOptionDefinition findSimpleField(ProtobufOptionNameSegment segment, ProtobufOptionDefinition definition) {
        return switch (definition.type()) {
            case ProtobufMessageTypeReference(var decl) ->
                    decl.getDirectChildByNameAndType(segment.name(), ProtobufOptionDefinition.class)
                            .orElse(null);
            case ProtobufGroupTypeReference(var decl) ->
                    decl.getDirectChildByNameAndType(segment.name().toLowerCase(), ProtobufOptionDefinition.class)
                            .orElse(null);
            default -> null;
        };
    }

    private ProtobufOptionDefinition findExtensionField(ProtobufDocumentTree document, String extendedTypeName, String fieldName) {
        if(extendedTypeName == null) {
            return null;
        }

        var visited = new HashSet<ProtobufDocumentTree>();
        return findExtensionField(document, document, visited, true, extendedTypeName, fieldName);
    }

    private ProtobufOptionDefinition findExtensionField(
            ProtobufDocumentTree rootDocument,
            ProtobufDocumentTree searchDocument,
            Set<ProtobufDocumentTree> visited,
            boolean includeNonPublic,
            String extendedTypeName,
            String fieldName) {
        // Search in the current document
        var result = findExtensionFieldInDocument(rootDocument, searchDocument, extendedTypeName, fieldName);
        if(result != null) {
            return result;
        }

        // Search in imports
        var importStatements = searchDocument.getDirectChildrenByType(ProtobufImportStatement.class).toList();
        for (var importStatement : importStatements) {
            var imported = findExtensionFieldInImport(rootDocument, visited, includeNonPublic, extendedTypeName, fieldName, importStatement);
            if (imported != null) {
                return imported;
            }
        }
        return null;
    }

    private ProtobufOptionDefinition findExtensionFieldInImport(
            ProtobufDocumentTree rootDocument,
            Set<ProtobufDocumentTree> visited,
            boolean includeNonPublic,
            String extendedTypeName,
            String fieldName,
            ProtobufImportStatement importStatement
    ) {
        if (!importStatement.hasDocument()) {
            return null;
        }

        var isPublic = importStatement.modifier() == ProtobufImportStatement.Modifier.PUBLIC;
        if (!includeNonPublic && !isPublic) {
            return null;
        }

        var imported = importStatement.document();
        if (!visited.add(imported)) {
            return null;
        }

        return findExtensionField(rootDocument, imported, visited, false, extendedTypeName, fieldName);
    }

    private ProtobufOptionDefinition findExtensionFieldInDocument(
            ProtobufDocumentTree rootDocument,
            ProtobufDocumentTree searchDocument,
            String extendedTypeName,
            String fieldName) {
        for (var child : searchDocument.children()) {
            if (!(child instanceof ProtobufExtendStatement extendStatement)) {
                continue;
            }

            var declaration = extendStatement.declaration();
            if(declaration instanceof ProtobufUnresolvedTypeReference(var name)) {
                var resolvedType = tryResolveType(searchDocument, name, extendStatement);
                if (resolvedType == null) {
                    // The unresolved target is reported when the extend declaration is validated
                    continue;
                }
                extendStatement.setDeclaration(resolvedType);
                declaration = resolvedType;
            }
            if(!(declaration instanceof ProtobufMessageTypeReference(var messageDefinition)) || !extendedTypeName.equals(messageDefinition.qualifiedName())) {
                continue;
            }

            for (var extendChild : extendStatement.children()) {
                ProtobufOptionDefinition definition = switch (extendChild) {
                    case ProtobufFieldStatement field when fieldName.equals(field.name()) -> field;
                    case ProtobufGroupStatement group when fieldName.equals(group.name().toLowerCase()) -> group;
                    default -> null;
                };
                if(definition == null) {
                    continue;
                }

                if (definition.type() instanceof ProtobufUnresolvedTypeReference) {
                    attributeType(rootDocument, definition);
                }

                return definition;
            }
        }
        return null;
    }

    private void validateOptionValueType(ProtobufOptionStatement option) {
        var optionDefinition = option.definition();
        if(optionDefinition == null) {
            return;
        }

        var optionValue = option.value();
        if(optionValue == null) {
            return;
        }

        if (optionDefinition.type() instanceof ProtobufMapTypeReference) {
            report(option, ProtobufSemanticErrors.optionValueOnMapField(optionDefinition.name()));
            return;
        }

        // Do not validate unresolved types: the unresolved type was already reported
        if (!(optionDefinition.type() instanceof ProtobufValueTypeReference valueType)) {
            return;
        }

        var type = valueType.protobufType();
        switch (type) {
            case MESSAGE -> check(optionValue instanceof ProtobufJsonExpression,
                    option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));

            case ENUM -> {
                if (!(optionValue instanceof ProtobufEnumConstantExpression enumExpr)) {
                    report(option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));
                } else if (optionDefinition.type() instanceof ProtobufEnumTypeReference(var declaration)) {
                    var constant = declaration.getDirectChildByNameAndType(enumExpr.name(), ProtobufEnumConstantStatement.class);
                    check(constant.isPresent(),
                            option, ProtobufSemanticErrors.optionValueUnknownEnumConstant(enumExpr.name(), declaration.name()));
                }
            }

            case FLOAT, DOUBLE -> {
                // Accepts any numeric value: integers, finite floats, inf, -inf, nan
                check(optionValue instanceof ProtobufNumberExpression,
                        option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));
            }

            case BOOL -> check(optionValue instanceof ProtobufBoolExpression,
                    option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));

            case STRING, BYTES -> check(optionValue instanceof ProtobufLiteralExpression,
                    option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));

            case INT32, SINT32, SFIXED32 -> {
                if (check(optionValue instanceof ProtobufIntegerExpression,
                        option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)))) {
                    var intValue = ((ProtobufIntegerExpression) optionValue).value().value();
                    check(intValue >= INT32_MIN && intValue <= INT32_MAX,
                            option, ProtobufSemanticErrors.optionValueOutOfRange(intValue, optionDefinition.name(), type, String.valueOf(INT32_MIN), String.valueOf(INT32_MAX)));
                }
            }

            case UINT32, FIXED32 -> {
                if (check(optionValue instanceof ProtobufIntegerExpression,
                        option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)))) {
                    var intValue = ((ProtobufIntegerExpression) optionValue).value().value();
                    check(intValue >= UINT32_MIN && intValue <= UINT32_MAX,
                            option, ProtobufSemanticErrors.optionValueOutOfRange(intValue, optionDefinition.name(), type, String.valueOf(UINT32_MIN), String.valueOf(UINT32_MAX)));
                }
            }

            case INT64, SINT64, SFIXED64 -> {
                // long is always a valid int64
                check(optionValue instanceof ProtobufIntegerExpression,
                        option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)));
            }

            case UINT64, FIXED64 -> {
                if (check(optionValue instanceof ProtobufIntegerExpression,
                        option, ProtobufSemanticErrors.optionValueTypeMismatch(optionDefinition.name(), type, String.valueOf(optionValue)))) {
                    var intValue = ((ProtobufIntegerExpression) optionValue).value().value();
                    check(intValue >= UINT64_MIN,
                            option, ProtobufSemanticErrors.optionValueOutOfRange(intValue, optionDefinition.name(), type, String.valueOf(UINT64_MIN), UINT64_MAX));
                }
            }
        }
    }

    private void validateField(ProtobufDocumentTree document, ProtobufFieldStatement field) {
        var resolved = attributeType(document, field);

        validateFieldModifiers(document, field);

        if (resolved) {
            validateFieldType(field);
        }

        validateFieldIndex(field);

        for (var option : field.options()) {
            attributeFieldOption(document, field, option);
        }
    }

    private void validateFieldModifiers(ProtobufDocumentTree document, ProtobufFieldStatement field) {
        switch (document.version()) {
            case PROTOBUF_2 -> {
                // Proto2 fields inside messages (not oneofs) must have an explicit label
                if(!(field.type() instanceof ProtobufMapTypeReference) && !(field.parent() instanceof ProtobufOneofStatement)) {
                    check(field.modifier() != ProtobufModifier.NONE,
                            field, ProtobufSemanticErrors.missingLabelInProto2(field.name()));
                }
            }
            case PROTOBUF_3 -> check(field.modifier() != ProtobufModifier.REQUIRED,
                    field, ProtobufSemanticErrors.requiredInProto3(field.name()));
            case EDITION_2023, EDITION_2024 -> {
                check(field.modifier() != ProtobufModifier.REQUIRED,
                        field, ProtobufSemanticErrors.requiredInEditions(field.name()));
                check(field.modifier() != ProtobufModifier.OPTIONAL,
                        field, ProtobufSemanticErrors.optionalInEditions(field.name()));
            }
        }
    }

    private void validateFieldType(ProtobufFieldStatement field) {
        switch (field.type()) {
            case ProtobufMapTypeReference mapType -> validateMapField(field, mapType);
            case ProtobufEnumTypeReference _,
                 ProtobufGroupTypeReference _,
                 ProtobufMessageTypeReference _,
                 ProtobufPrimitiveTypeReference _,
                 ProtobufUnresolvedTypeReference _ -> {} // Nothing to do
        }
    }

    private void validateMapField(ProtobufFieldStatement field, ProtobufMapTypeReference mapType) {
        var parent = field.parent();
        if(parent instanceof ProtobufOneofStatement oneofStatement) {
            report(field, ProtobufSemanticErrors.mapFieldInOneof(field.name(), oneofStatement.name()));
        }

        check(field.modifier() == ProtobufModifier.NONE,
                field, ProtobufSemanticErrors.mapFieldWithModifier(field.name(), field.modifier().token()));

        var keyType = mapType.keyType();
        if(keyType != null) {
            check(keyType instanceof ProtobufValueTypeReference keyValueType && keyValueType.protobufType().isValidMapKeyType(),
                    field, ProtobufSemanticErrors.invalidMapKeyType(field.name(), keyType.name()));
        }

        var valueType = mapType.valueType();
        if(valueType != null) {
            check(!(valueType instanceof ProtobufMapTypeReference),
                    field, ProtobufSemanticErrors.nestedMapValue(field.name()));
        }
    }

    private void validateFieldIndex(ProtobufFieldStatement field) {
        if (!field.hasIndex()) {
            return;
        }
        var fieldNumber = field.index().value();

        var minAllowedIndex = field.parent() instanceof ProtobufEnumStatement
                ? ENUM_CONSTANT_MIN
                : FIELD_NUMBER_MIN;
        check(fieldNumber >= minAllowedIndex,
                field, ProtobufSemanticErrors.fieldNumberTooSmall(field.name(), fieldNumber));

        var maxAllowedIndex = field.parent() instanceof ProtobufEnumStatement
                ? ENUM_CONSTANT_MAX
                : FIELD_NUMBER_MAX;
        check(fieldNumber <= maxAllowedIndex,
                field, ProtobufSemanticErrors.fieldNumberTooLarge(field.name(), fieldNumber));

        check(fieldNumber < RESERVED_RANGE_MIN || fieldNumber > RESERVED_RANGE_MAX,
                field, ProtobufSemanticErrors.fieldNumberInImplementationRange(field.name(), fieldNumber));
    }

    private void validateEnumConstant(ProtobufDocumentTree document, ProtobufEnumConstantStatement constant) {
        if (constant.hasIndex()) {
            var constantIndex = constant.index().value();
            check(constantIndex >= ENUM_CONSTANT_MIN && constantIndex <= ENUM_CONSTANT_MAX,
                    constant, ProtobufSemanticErrors.enumValueOutOfRange(constantIndex, ENUM_CONSTANT_MIN, ENUM_CONSTANT_MAX, getParentName(constant), constant.name()));
        }
        for (var option : constant.options()) {
            attributeCompactOption(document, ProtobufEnumConstantStatement.class, constant, option);
        }
    }

    private void validateExtensionsStatementOptions(ProtobufDocumentTree document, ProtobufExtensionsStatement extensionsStatement) {
        for (var option : extensionsStatement.options()) {
            attributeCompactOption(document, ProtobufExtensionsStatement.class, extensionsStatement, option);
        }
    }

    private void validateGroupField(ProtobufDocumentTree document, ProtobufGroupStatement groupField) {
        switch (document.version()) {
            case PROTOBUF_2 -> {}
            case PROTOBUF_3 -> report(groupField, ProtobufSemanticErrors.groupInProto3(groupField.name()));
            case EDITION_2023, EDITION_2024 -> report(groupField, ProtobufSemanticErrors.groupInEditions(groupField.name()));
        }
    }

    private static String getParentName(ProtobufTree groupField) {
        return groupField.parent() instanceof ProtobufTree.WithName parentWithName ? parentWithName.name() : "unknown";
    }

    private void validateOneofField(ProtobufOneofStatement field) {
        // Oneof must contain at least one field
        var child = field.getDirectChildByType(ProtobufFieldStatement.class);
        check(child.isPresent(),
                field, ProtobufSemanticErrors.emptyOneof(field.name()));
    }

    private void validateEnum(ProtobufEnumStatement enumStmt) {
        validateReserved(enumStmt, ENUM_CONSTANT_MIN, ENUM_CONSTANT_MAX);

        ProtobufEnumConstantStatement firstConstant = null;
        var names = new LinkedHashMap<String, ProtobufEnumConstantStatement>();
        var reservedNames = new HashSet<String>();
        var indexes = new LinkedHashMap<Long, List<ProtobufEnumConstantStatement>>();
        var reservedIndexes = new TreeMap<Long, Long>();
        var allowsIndexDuplicates = false;
        for(var child : enumStmt.children()) {
            switch (child) {
                case ProtobufEnumConstantStatement enumConstant -> {
                    if(firstConstant == null) {
                        firstConstant = enumConstant;
                    }

                    if (enumConstant.hasName() && names.putIfAbsent(enumConstant.name(), enumConstant) != null) {
                        report(enumConstant, ProtobufSemanticErrors.duplicateEnumConstantName(enumConstant.name()));
                    }

                    if (enumConstant.hasIndex()) {
                        indexes.computeIfAbsent(enumConstant.index().value(), _ -> new ArrayList<>())
                                .add(enumConstant);
                    }
                }

                case ProtobufOptionStatement option -> {
                    var aliasSegments = option.name().segments();
                    if(!aliasSegments.isEmpty() && aliasSegments.getFirst().hasName("allow_alias") && option.value() instanceof ProtobufBoolExpression(
                            var value) && value) {
                        allowsIndexDuplicates = true;
                    }
                }

                case ProtobufReservedStatement reservedStatement -> {
                    for(var expression : reservedStatement.expressions()) {
                        switch (expression) {
                            case ProtobufIntegerExpression integerExpression -> reservedIndexes.put(integerExpression.value().value(), integerExpression.value().value());

                            case ProtobufIntegerRangeExpression rangeExpression -> {
                                switch (rangeExpression.value()) {
                                    case ProtobufIntegerRange.Bounded bounded -> reservedIndexes.put(bounded.min().value(), bounded.max().value());
                                    case ProtobufIntegerRange.LowerBounded lowerBounded -> reservedIndexes.put(lowerBounded.min().value(), ENUM_CONSTANT_MAX);
                                }
                            }

                            case ProtobufLiteralExpression literalExpression -> reservedNames.add(literalExpression.value());
                        }
                    }
                }

                case ProtobufEmptyStatement _ -> {}
            }
        }

        if (!check(firstConstant != null,
                enumStmt, ProtobufSemanticErrors.emptyEnum(enumStmt.name()))) {
            return;
        }

        if(enumStmt.enumType() == ProtobufEnumType.OPEN && firstConstant.hasIndex()) {
            check(firstConstant.index().value() == 0,
                    firstConstant, ProtobufSemanticErrors.openEnumFirstValueNotZero(enumStmt.name(), firstConstant.index().value(), firstConstant.name()));
        }

        for (var entry : names.entrySet()) {
            if (reservedNames.contains(entry.getKey())) {
                report(entry.getValue(), ProtobufSemanticErrors.reservedEnumConstantName(entry.getKey()));
            }
        }

        for(var entry : indexes.entrySet()) {
            var value = entry.getKey();
            var constants = entry.getValue();
            if (findOverlap(reservedIndexes, value, value) != null) {
                for (var constant : constants) {
                    report(constant, ProtobufSemanticErrors.reservedEnumConstantIndex(value, constant.name()));
                }
            }

            if (constants.size() > 1 && !allowsIndexDuplicates) {
                var constantNames = new StringJoiner(", ");
                for (var constant : constants) {
                    constantNames.add(constant.name());
                }
                report(constants.get(1), ProtobufSemanticErrors.duplicateEnumValue(enumStmt.name(), value, constantNames.toString()));
            }
        }
    }

    private void validateReserved(ProtobufTree.WithBody<?> treeWithBody, long minIndex, long maxIndex) {
        var reservedConstantsIndexes = new TreeMap<Long, Long>();
        var reservedConstantsNames = new HashSet<String>();
        var reservedStatements = treeWithBody.getDirectChildrenByType(ProtobufReservedStatement.class).toList();
        for (var reservedStatement : reservedStatements) {
            var reservesNames = false;
            var reservesIndexes = false;
            for(var expression : reservedStatement.expressions()) {
                switch (expression) {
                    case ProtobufLiteralExpression literalExpression -> {
                        if(!reservedConstantsNames.add(literalExpression.value())) {
                            report(reservedStatement, ProtobufSemanticErrors.duplicateReservedName(literalExpression.value()));
                        }
                        reservesNames = true;
                    }

                    case ProtobufIntegerExpression integerExpression -> {
                        var value = integerExpression.value().value();
                        reservesIndexes = true;

                        var inBounds = check(value >= minIndex,
                                reservedStatement, ProtobufSemanticErrors.reservedNumberTooSmall(value, minIndex));
                        inBounds &= check(value <= maxIndex,
                                reservedStatement, ProtobufSemanticErrors.reservedNumberTooLarge(value, maxIndex));
                        if (!inBounds) {
                            continue;
                        }

                        // Check if value falls within an existing range
                        var overlap = findOverlap(reservedConstantsIndexes, value, value);
                        if (overlap != null) {
                            report(reservedStatement, ProtobufSemanticErrors.reservedRangesOverlap(
                                    formatRange(value, value, maxIndex), formatRange(overlap.getKey(), overlap.getValue(), maxIndex)));
                        }

                        reservedConstantsIndexes.put(value, value);
                    }

                    case ProtobufIntegerRangeExpression rangeExpr -> {
                        var range = rangeExpr.value();
                        reservesIndexes = true;
                        switch (range) {
                            case ProtobufIntegerRange.Bounded(ProtobufInteger(var min), ProtobufInteger(var max)) -> {
                                var valid = check(min >= minIndex,
                                        reservedStatement, ProtobufSemanticErrors.reservedNumberTooSmall(min, minIndex));
                                valid &= check(max <= maxIndex,
                                        reservedStatement, ProtobufSemanticErrors.reservedNumberTooLarge(max, maxIndex));
                                valid &= check(min <= max,
                                        reservedStatement, ProtobufSemanticErrors.invalidReservedRange(min, max));
                                if (!valid) {
                                    continue;
                                }

                                // Check overlap with range starting before min, or with range starting within [min, max]
                                var overlap = findOverlap(reservedConstantsIndexes, min, max);
                                if (overlap != null) {
                                    report(reservedStatement, ProtobufSemanticErrors.reservedRangesOverlap(
                                            formatRange(min, max, maxIndex), formatRange(overlap.getKey(), overlap.getValue(), maxIndex)));
                                }

                                reservedConstantsIndexes.put(min, max);
                            }

                            case ProtobufIntegerRange.LowerBounded(ProtobufInteger(var min)) -> {
                                if (!check(min >= minIndex,
                                        reservedStatement, ProtobufSemanticErrors.reservedNumberTooSmall(min, minIndex))) {
                                    continue;
                                }

                                // Check overlap with range starting before min, and any range starting at or after min
                                var overlap = findOverlap(reservedConstantsIndexes, min, maxIndex);
                                if (overlap != null) {
                                    report(reservedStatement, ProtobufSemanticErrors.reservedRangesOverlap(
                                            formatRange(min, maxIndex, maxIndex), formatRange(overlap.getKey(), overlap.getValue(), maxIndex)));
                                }

                                reservedConstantsIndexes.put(min, maxIndex);
                            }
                        }
                    }

                    default -> {}
                }
            }
            check(!reservesIndexes || !reservesNames,
                    reservedStatement, ProtobufSemanticErrors.mixedReservedNumbersAndNames());
        }
    }

    private void validateExtensions(ProtobufDocumentTree document, ProtobufTree.WithBody<?> treeWithBody) {
        var extensibleIndexes = new TreeMap<Long, Long>();
        var extensionsStatements = treeWithBody.getDirectChildrenByType(ProtobufExtensionsStatement.class).toList();
        for (var extensionsStatement : extensionsStatements) {
            var syntax = document.version();
            if(syntax == ProtobufVersion.PROTOBUF_3) {
                var messageName = treeWithBody instanceof ProtobufTree.WithName withName ? withName.name() : "";
                report(extensionsStatement, ProtobufSemanticErrors.extensionsInProto3(messageName));
                continue;
            }

            for(var expression : extensionsStatement.expressions()) {
                switch (expression) {
                    case ProtobufIntegerExpression integerExpression -> {
                        var value = integerExpression.value().value();

                        var inBounds = check(value >= FIELD_NUMBER_MIN,
                                extensionsStatement, ProtobufSemanticErrors.extensionNumberTooSmall(value, FIELD_NUMBER_MIN));
                        inBounds &= check(value <= FIELD_NUMBER_MAX,
                                extensionsStatement, ProtobufSemanticErrors.extensionNumberTooLarge(value, FIELD_NUMBER_MAX));
                        if (!inBounds) {
                            continue;
                        }

                        // Check if value falls within an existing range
                        var overlap = findOverlap(extensibleIndexes, value, value);
                        if (overlap != null) {
                            report(extensionsStatement, ProtobufSemanticErrors.extensionRangesOverlap(
                                    formatRange(value, value, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                        }

                        extensibleIndexes.put(value, value);
                    }

                    case ProtobufIntegerRangeExpression rangeExpr -> {
                        var range = rangeExpr.value();
                        switch (range) {
                            case ProtobufIntegerRange.Bounded(ProtobufInteger(var min), ProtobufInteger(var max)) -> {
                                var valid = check(min >= FIELD_NUMBER_MIN,
                                        extensionsStatement, ProtobufSemanticErrors.extensionNumberTooSmall(min, FIELD_NUMBER_MIN));
                                valid &= check(max <= FIELD_NUMBER_MAX,
                                        extensionsStatement, ProtobufSemanticErrors.extensionNumberTooLarge(max, FIELD_NUMBER_MAX));
                                valid &= check(min <= max,
                                        extensionsStatement, ProtobufSemanticErrors.invalidExtensionRange(min, max));
                                if (!valid) {
                                    continue;
                                }

                                // Check overlap with range starting before min, or with range starting within [min, max]
                                var overlap = findOverlap(extensibleIndexes, min, max);
                                if (overlap != null) {
                                    report(extensionsStatement, ProtobufSemanticErrors.extensionRangesOverlap(
                                            formatRange(min, max, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                extensibleIndexes.put(min, max);
                            }

                            case ProtobufIntegerRange.LowerBounded(ProtobufInteger(var min)) -> {
                                if (!check(min >= FIELD_NUMBER_MIN,
                                        extensionsStatement, ProtobufSemanticErrors.extensionNumberTooSmall(min, FIELD_NUMBER_MIN))) {
                                    continue;
                                }

                                // Check overlap with range starting before min, and any range starting at or after min
                                var overlap = findOverlap(extensibleIndexes, min, FIELD_NUMBER_MAX);
                                if (overlap != null) {
                                    report(extensionsStatement, ProtobufSemanticErrors.extensionRangesOverlap(
                                            formatRange(min, FIELD_NUMBER_MAX, FIELD_NUMBER_MAX), formatRange(overlap.getKey(), overlap.getValue(), FIELD_NUMBER_MAX)));
                                }

                                extensibleIndexes.put(min, (long) FIELD_NUMBER_MAX);
                            }
                        }
                    }

                    default -> {}
                }
            }
        }
    }

    private void validateService(ProtobufServiceStatement service) {
        var methodNames = new HashSet<String>();
        var methods = service.getDirectChildrenByType(ProtobufMethodStatement.class).toList();
        for (var method : methods) {
            if (!methodNames.add(method.name())) {
                report(method, ProtobufSemanticErrors.duplicateMethodName(method.name(), service.name()));
            }
        }
    }

    private void validateServiceMethod(ProtobufDocumentTree document, ProtobufMethodStatement method) {
        var inputTypeWrapper = method.inputType();
        var outputTypeWrapper = method.outputType();

        if (inputTypeWrapper == null || outputTypeWrapper == null) {
            return; // Not fully parsed yet
        }

        var inputType = inputTypeWrapper.value();
        var outputType = outputTypeWrapper.value();

        // Resolve and validate input type
        if (inputType instanceof ProtobufUnresolvedTypeReference(var inputName)) {
            var resolvedInput = tryResolveType(document, inputName, method);
            if (resolvedInput != null) {
                method.setInputType(new ProtobufMethodStatement.Type(resolvedInput, inputTypeWrapper.isStream()));
                inputType = resolvedInput;
            } else {
                report(method, ProtobufSemanticErrors.unresolvedMethodType(inputName, method.name(), getParentName(method)));
            }
        }
        check(!(inputType instanceof ProtobufEnumTypeReference),
                method, ProtobufSemanticErrors.enumMethodInput(method.name(), getParentName(method)));

        // Resolve and validate output type
        if (outputType instanceof ProtobufUnresolvedTypeReference(var outputName)) {
            var resolvedOutput = tryResolveType(document, outputName, method);
            if (resolvedOutput != null) {
                method.setOutputType(new ProtobufMethodStatement.Type(resolvedOutput, outputTypeWrapper.isStream()));
                outputType = resolvedOutput;
            } else {
                report(method, ProtobufSemanticErrors.unresolvedMethodType(outputName, method.name(), getParentName(method)));
            }
        }
        check(!(outputType instanceof ProtobufEnumTypeReference),
                method, ProtobufSemanticErrors.enumMethodOutput(method.name(), getParentName(method)));
    }

    private void validateNamingStyle(ProtobufDocumentTree document) {
        var packageName = document.packageName();
        if (packageName.isEmpty()) {
            return;
        }

        for(var segment : packageName.get().split("\\.")) {
            check(isValidSnakeCase(segment, false),
                    document, ProtobufSemanticErrors.packageSegmentNotSnakeCase(segment));
        }
    }

    private void validateNamingStyleForTree(ProtobufTree tree) {
        switch (tree) {
            case ProtobufMessageStatement message ->
                    check(isValidTitleCase(message.name()),
                            message, ProtobufSemanticErrors.messageNameNotTitleCase(message.name()));
            case ProtobufEnumStatement enumStmt ->
                    check(isValidTitleCase(enumStmt.name()),
                            enumStmt, ProtobufSemanticErrors.enumNameNotTitleCase(enumStmt.name()));
            case ProtobufServiceStatement service ->
                    check(isValidTitleCase(service.name()),
                            service, ProtobufSemanticErrors.serviceNameNotTitleCase(service.name()));
            case ProtobufMethodStatement method ->
                    check(isValidTitleCase(method.name()),
                            method, ProtobufSemanticErrors.methodNameNotTitleCase(method.name()));
            case ProtobufFieldStatement field ->
                    check(isValidSnakeCase(field.name(), false),
                            field, ProtobufSemanticErrors.fieldNameNotSnakeCase(field.name()));
            case ProtobufOneofStatement oneof ->
                    check(isValidSnakeCase(oneof.name(), false),
                            oneof, ProtobufSemanticErrors.oneofNameNotSnakeCase(oneof.name()));
            case ProtobufEnumConstantStatement constant ->
                    check(isValidSnakeCase(constant.name(), true),
                            constant, ProtobufSemanticErrors.enumValueNotUpperSnakeCase(constant.name()));
            default -> {}
        }
    }

    private static boolean isValidTitleCase(String name) {
        if(name == null || name.isEmpty()) {
            return false;
        }

        if(!Character.isUpperCase(name.charAt(0))) {
            return false;
        }

        for(var i = 0; i < name.length(); i++) {
            if(!Character.isLetterOrDigit(name.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidSnakeCase(String name, boolean upperCase) {
        if(name == null || name.isEmpty()) {
            return false;
        }

        var firstChar = name.charAt(0);
        if(upperCase ? !Character.isUpperCase(firstChar) : !Character.isLowerCase(firstChar)) {
            return false;
        }

        for(var i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if(c != '_' && !Character.isDigit(c) && (upperCase ? !Character.isUpperCase(c) : !Character.isLowerCase(c))) {
                return false;
            }
        }

        return !containsBadUnderscores(name);
    }

    private static boolean containsBadUnderscores(String name) {
        if(name.charAt(0) == '_' || name.charAt(name.length() - 1) == '_') {
            return true;
        }

        for(var i = 0; i < name.length() - 1; i++) {
            if(name.charAt(i) == '_' && !Character.isLetter(name.charAt(i + 1))) {
                return true;
            }
        }

        return false;
    }

    // Converts a field name to its JSON representation (lowerCamelCase)
    private static String toJsonName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }

        var result = new StringBuilder(fieldName.length());
        var capitalizeNext = false;
        for (var i = 0; i < fieldName.length(); i++) {
            var c = fieldName.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
