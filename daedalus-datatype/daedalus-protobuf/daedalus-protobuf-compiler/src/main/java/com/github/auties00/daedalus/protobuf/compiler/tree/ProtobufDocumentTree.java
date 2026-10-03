package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosFileDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosFileOptionsBuilder;
import com.google.protobuf.DescriptorProtosSourceCodeInfoBuilder;
import com.google.protobuf.SourceCodeInfoDescriptorProtosLocationBuilder;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufEnumConstantExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.model.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents the root node of a Protocol Buffer document AST.
 * <p>
 * The document tree is the top-level container for all statements in a .proto file. It holds
 * file-level declarations such as syntax, package, imports, options, and type definitions
 * (messages, enums, services). The document also tracks the file's location on disk.
 * </p>
 * <h2>Structure of a .proto file:</h2>
 * <pre>{@code
 * // example.proto
 * syntax = "proto3";
 *
 * package com.example;
 *
 * import "google/protobuf/timestamp.proto";
 * import public "other.proto";
 *
 * option java_package = "com.example.proto";
 * option java_outer_classname = "ExampleProtos";
 *
 * message Person {
 *   string name = 1;
 *   int32 age = 2;
 * }
 *
 * enum Status {
 *   UNKNOWN = 0;
 *   ACTIVE = 1;
 * }
 *
 * service UserService {
 *   rpc GetUser(Person) returns (Person);
 * }
 * }</pre>
 * <p>
 * The document tree can contain these child elements:
 * </p>
 * <ul>
 *   <li><strong>Syntax statement:</strong> Declares proto2 or proto3 (at most one)</li>
 *   <li><strong>Package statement:</strong> Declares the package name (at most one)</li>
 *   <li><strong>Import statements:</strong> Imports other .proto files</li>
 *   <li><strong>Option statements:</strong> File-level configuration options</li>
 *   <li><strong>Message statements:</strong> Message type definitions</li>
 *   <li><strong>Enum statements:</strong> Enum type definitions</li>
 *   <li><strong>Service statements:</strong> RPC service definitions</li>
 *   <li><strong>Extend statements:</strong> Extensions to existing messages</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * This class is the root of the AST and has no parent. It provides convenience methods to
 * access the syntax version and package name directly. The document is attributed when all
 * of its child statements are attributed.
 * </p>
 * <p>
 * The {@code qualifiedPath()} method returns the document's path in the Protocol Buffer
 * namespace, combining the package name and file name (e.g., "com/example/example.proto").
 * </p>
 *
 * @see ProtobufDocumentChild
 * @see ProtobufSyntaxStatement
 * @see ProtobufPackageStatement
 * @see ProtobufImportStatement
 */
public final class ProtobufDocumentTree
        implements ProtobufTree, ProtobufTree.WithBody<ProtobufDocumentChild>, ProtobufTree.WithComments, ProtobufTree.WithDescriptor {
    private static final int FILE_NAME = 1;
    private static final int FILE_PACKAGE = 2;
    private static final int FILE_DEPENDENCY = 3;
    private static final int FILE_MESSAGE_TYPE = 4;
    private static final int FILE_ENUM_TYPE = 5;
    private static final int FILE_SERVICE = 6;
    private static final int FILE_EXTENSION = 7;
    private static final int FILE_OPTIONS = 8;
    private static final int FILE_SYNTAX = 12;
    private static final int FILE_OPTION_DEPENDENCY = 15;
    private static final int MESSAGE_NAME = 1;
    private static final int MESSAGE_FIELD = 2;
    private static final int MESSAGE_NESTED_TYPE = 3;
    private static final int MESSAGE_ENUM_TYPE = 4;
    private static final int MESSAGE_EXTENSION = 6;
    private static final int MESSAGE_ONEOF_DECL = 8;
    private static final int FIELD_NAME = 1;
    private static final int ENUM_NAME = 1;
    private static final int ENUM_VALUE = 2;
    private static final int ENUM_VALUE_NAME = 1;
    private static final int SERVICE_NAME = 1;
    private static final int SERVICE_METHOD = 2;
    private static final int METHOD_NAME = 1;
    private static final int ONEOF_NAME = 1;
    
    private final Path location;
    private final List<ProtobufDocumentChild> children;
    private final List<ProtobufCommentStatement> comments;
    private ProtobufTreeSourceRange range;

    /**
     * Constructs a new document tree for a file at the specified location.
     *
     * @param location the file system path to the .proto file, or null if unknown
     */
    public ProtobufDocumentTree(Path location) {
        this.location = location;
        this.children = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.range = ProtobufTreeSourceRange.UNKNOWN;
    }

    /**
     * Constructs a new document tree with no associated file location.
     * <p>
     * This constructor is useful for programmatically created ASTs or testing.
     * </p>
     */
    public ProtobufDocumentTree() {
        this(null);
    }

    /**
     * Returns the source range covering the entire document.
     *
     * <p>The range starts at line 1, column 1 and ends at the position immediately after the last
     * character of the file. Synthetic documents that have not been populated by a parser return
     * {@link ProtobufTreeSourceRange#UNKNOWN}.
     *
     * @return the source range covering this document
     */
    @Override
    public ProtobufTreeSourceRange range() {
        return range;
    }

    /**
     * Updates the source range covering this document.
     *
     * <p>The parser uses this method to fill in the range once parsing reaches end-of-input.
     *
     * @param range the source range to set
     */
    public void setRange(ProtobufTreeSourceRange range) {
        this.range = range;
    }

    /**
     * Returns the file-level comments attached to this document, in source order.
     *
     * @return an unmodifiable view of the attached comments
     */
    @Override
    public SequencedCollection<ProtobufCommentStatement> comments() {
        return Collections.unmodifiableList(comments);
    }

    /**
     * Appends a comment to this document.
     *
     * <p>The parser uses this method to attach detached and leading comments captured by the
     * lexer to the document they belong to.
     *
     * @param comment the comment to append
     */
    public void addComment(ProtobufCommentStatement comment) {
        comments.add(comment);
    }

    /**
     * Checks whether this document is fully attributed.
     * <p>
     * A document is attributed when all of its child statements are attributed.
     * </p>
     *
     * @return true if all children are attributed, false otherwise
     */
    @Override
    public boolean isAttributed() {
        return children.stream()
                .allMatch(ProtobufTree::isAttributed);
    }

    /**
     * Returns the file system location of this .proto file.
     *
     * @return optional containing the file path, or empty if no location is set
     */
    public Optional<Path> location() {
        return Optional.ofNullable(location);
    }

    /**
     * Returns this document. Implements {@link ProtobufTree.WithBody#document()}; the document
     * itself is its own enclosing document.
     *
     * @return this
     */
    @Override
    public ProtobufDocumentTree document() {
        return this;
    }

    /**
     * Returns the parent tree node.
     * <p>
     * Documents are root nodes and have no parent.
     * </p>
     *
     * @return always returns null
     */
    @Override
    public ProtobufTree parent() {
        return null;
    }

    /**
     * Checks whether this document has a parent.
     * <p>
     * Documents are root nodes and never have parents.
     * </p>
     *
     * @return always returns false
     */
    @Override
    public boolean hasParent() {
        return false;
    }

    /**
     * Returns the fully qualified path for this document in the Protocol Buffer namespace.
     * <p>
     * Combines the package name (with dots converted to slashes) and the file name.
     * For example, package "com.example" with file "foo.proto" becomes "com/example/foo.proto".
     * </p>
     *
     * @return the qualified path string
     */
    public String qualifiedPath() {
        var result = new StringBuilder();
        packageName().ifPresent(value -> {
            result.append(value.replaceAll("\\.", "/"));
        });
        location().ifPresent(value -> {
            if(!result.isEmpty()) {
                result.append('/');
            }
            result.append(value.getFileName());
        });
        return result.toString();
    }

    /**
     * Returns the Protocol Buffer version declared in this document.
     *
     * @return the declared version, or {@link ProtobufVersion#DEFAULT_VERSION} if none is declared
     */
    public ProtobufVersion version() {
        return syntaxVersion()
                .or(this::editionVersion)
                .orElse(ProtobufVersion.DEFAULT_VERSION);
    }

    /**
     * Checks whether this document has an explicit version declaration (syntax or edition statement).
     *
     * @return true if a version is explicitly declared, false otherwise
     */
    public boolean hasVersion() {
        return syntaxVersion()
                .or(this::editionVersion)
                .isPresent();
    }

    private Optional<ProtobufVersion> syntaxVersion() {
        return getDirectChildByType(ProtobufSyntaxStatement.class)
                .map(ProtobufSyntaxStatement::version);
    }

    private Optional<ProtobufVersion> editionVersion() {
        return getDirectChildByType(ProtobufEditionStatement.class)
                .map(ProtobufEditionStatement::version);
    }

    /**
     * Returns the package name declared in this document.
     * <p>
     * Extracts the package name from the package statement if present.
     * </p>
     *
     * @return optional containing the package name, or empty if no package statement exists
     */
    public Optional<String> packageName() {
        return getDirectChildByType(ProtobufPackageStatement.class)
                .map(ProtobufPackageStatement::name);
    }

    /**
     * Returns the resolved field presence semantics for this document.
     *
     * @return the resolved field presence
     */
    public ProtobufFieldPresence fieldPresence() {
        return ProtobufTreeFeatures.fieldPresence(this);
    }

    /**
     * Returns the resolved enum type semantics for this document.
     *
     * @return the resolved enum type
     */
    public ProtobufEnumType enumType() {
        return ProtobufTreeFeatures.enumType(this);
    }

    /**
     * Returns the resolved repeated field encoding for this document.
     *
     * @return the resolved repeated field encoding
     */
    public ProtobufRepeatedFieldEncoding repeatedFieldEncoding() {
        return ProtobufTreeFeatures.repeatedFieldEncoding(this);
    }

    /**
     * Returns the resolved UTF8 validation mode for this document.
     *
     * @return the resolved UTF8 validation mode
     */
    public ProtobufUtf8Validation utf8Validation() {
        return ProtobufTreeFeatures.utf8Validation(this);
    }

    /**
     * Returns the resolved message encoding format for this document.
     *
     * @return the resolved message encoding
     */
    public ProtobufMessageEncoding messageEncoding() {
        return ProtobufTreeFeatures.messageEncoding(this);
    }

    /**
     * Returns the resolved JSON compatibility level for this document.
     *
     * @return the resolved JSON compatibility
     */
    public ProtobufJsonCompatibility jsonCompatibility() {
        return ProtobufTreeFeatures.jsonCompatibility(this);
    }

    /**
     * Returns the resolved naming style for this document.
     *
     * @return the resolved naming style
     */
    public ProtobufNamingStyle namingStyle() {
        return ProtobufTreeFeatures.namingStyle(this);
    }

    @Override
    public DescriptorProtos.FileDescriptorProto toDescriptor() {
        var messages = getDirectChildrenByType(ProtobufMessageStatement.class)
                .map(ProtobufMessageStatement::toDescriptor)
                .toList();
        var enums = getDirectChildrenByType(ProtobufEnumStatement.class)
                .map(ProtobufEnumStatement::toDescriptor)
                .toList();
        var services = getDirectChildrenByType(ProtobufServiceStatement.class)
                .map(ProtobufServiceStatement::toDescriptor)
                .toList();
        var extensions = getDirectChildrenByType(ProtobufExtendStatement.class)
                .flatMap(ext -> ext.getDirectChildrenByType(ProtobufFieldStatement.class))
                .map(ProtobufFieldStatement::toDescriptor)
                .toList();
        var imports = getDirectChildrenByType(ProtobufImportStatement.class).toList();
        List<String> regularDeps = new ArrayList<>();
        List<String> optionDeps = new ArrayList<>();
        List<Integer> publicDeps = new ArrayList<>();
        List<Integer> weakDeps = new ArrayList<>();
        for (var importStatement : imports) {
            var importLocation = importStatement.location();
            var modifier = importStatement.modifier();
            if (modifier == ProtobufImportStatement.Modifier.OPTION) {
                // Per descriptor.proto, "option" imports are excluded from the regular dependency
                // list and live in option_dependency instead.
                optionDeps.add(importLocation);
                continue;
            }
            var regularIndex = regularDeps.size();
            regularDeps.add(importLocation);
            switch (modifier) {
                case PUBLIC -> publicDeps.add(regularIndex);
                case WEAK -> weakDeps.add(regularIndex);
                case NONE -> {}
                case OPTION -> throw new AssertionError("OPTION modifier handled above");
            }
        }
        var builder = new DescriptorProtosFileDescriptorProtoBuilder()
                .messageType(messages)
                .enumType(enums)
                .service(services)
                .extension(extensions)
                .dependency(regularDeps)
                .publicDependency(publicDeps)
                .weakDependency(weakDeps)
                .optionDependency(optionDeps);
        var qualifiedName = qualifiedPath();
        if (!qualifiedName.isEmpty()) {
            builder.name(qualifiedName);
        } else {
            location().ifPresent(path -> builder.name(path.getFileName().toString()));
        }
        packageName().ifPresent(builder::package_);
        var fileOptions = buildFileOptions();
        if (fileOptions != null) {
            builder.options(fileOptions);
        }
        var sourceCodeInfo = buildSourceCodeInfo();
        if (sourceCodeInfo != null) {
            builder.sourceCodeInfo(sourceCodeInfo);
        }
        var version = version();
        if (version.isEdition()) {
            builder.syntax("editions");
            builder.edition(switch (version) {
                case EDITION_2023 -> DescriptorProtos.Edition.EDITION_2023;
                case EDITION_2024 -> DescriptorProtos.Edition.EDITION_2024;
                default -> null;
            });
        } else {
            builder.syntax(version.versionCode());
        }
        return builder.build();
    }

    private DescriptorProtosFileOptionsBuilder buildFileOptions() {
        // Note: FileOptions.features (field 50) and FileOptions.uninterpreted_option (field 999)
        // are not derivable from the AST option statements alone and are intentionally left unset.
        var optionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (optionStatements.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosFileOptionsBuilder();
        for (var option : optionStatements) {
            if (!option.hasName() || !option.hasValue() || option.name().segments().size() != 1) {
                continue;
            }
            var segment = option.name().segments().getFirst();
            if (segment.isExtension()) {
                continue;
            }
            var value = option.value();
            switch (segment.name()) {
                case "java_package" -> {
                    if (value instanceof ProtobufLiteralExpression(var javaPackage)) {
                        builder.javaPackage(javaPackage);
                    }
                }
                case "java_outer_classname" -> {
                    if (value instanceof ProtobufLiteralExpression(var javaOuterClassname)) {
                        builder.javaOuterClassname(javaOuterClassname);
                    }
                }
                case "java_multiple_files" -> {
                    if (value instanceof ProtobufBoolExpression(var javaMultipleFiles)) {
                        builder.javaMultipleFiles(javaMultipleFiles);
                    }
                }
                case "java_generate_equals_and_hash" -> {
                    if (value instanceof ProtobufBoolExpression(var javaGenerateEqualsAndHash)) {
                        builder.javaGenerateEqualsAndHash(javaGenerateEqualsAndHash);
                    }
                }
                case "java_string_check_utf8" -> {
                    if (value instanceof ProtobufBoolExpression(var javaStringCheckUtf8)) {
                        builder.javaStringCheckUtf8(javaStringCheckUtf8);
                    }
                }
                case "optimize_for" -> {
                    if (value instanceof ProtobufEnumConstantExpression(var optimizeFor)) {
                        try {
                            builder.optimizeFor(DescriptorProtos.FileOptions.OptimizeMode.valueOf(optimizeFor));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown optimize mode literal; skip.
                        }
                    }
                }
                case "go_package" -> {
                    if (value instanceof ProtobufLiteralExpression(var goPackage)) {
                        builder.goPackage(goPackage);
                    }
                }
                case "cc_generic_services" -> {
                    if (value instanceof ProtobufBoolExpression(var ccGenericServices)) {
                        builder.ccGenericServices(ccGenericServices);
                    }
                }
                case "java_generic_services" -> {
                    if (value instanceof ProtobufBoolExpression(var javaGenericServices)) {
                        builder.javaGenericServices(javaGenericServices);
                    }
                }
                case "py_generic_services" -> {
                    if (value instanceof ProtobufBoolExpression(var pyGenericServices)) {
                        builder.pyGenericServices(pyGenericServices);
                    }
                }
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                    }
                }
                case "cc_enable_arenas" -> {
                    if (value instanceof ProtobufBoolExpression(var ccEnableArenas)) {
                        builder.ccEnableArenas(ccEnableArenas);
                    }
                }
                case "objc_class_prefix" -> {
                    if (value instanceof ProtobufLiteralExpression(var objcClassPrefix)) {
                        builder.objcClassPrefix(objcClassPrefix);
                    }
                }
                case "csharp_namespace" -> {
                    if (value instanceof ProtobufLiteralExpression(var csharpNamespace)) {
                        builder.csharpNamespace(csharpNamespace);
                    }
                }
                case "swift_prefix" -> {
                    if (value instanceof ProtobufLiteralExpression(var swiftPrefix)) {
                        builder.swiftPrefix(swiftPrefix);
                    }
                }
                case "php_class_prefix" -> {
                    if (value instanceof ProtobufLiteralExpression(var phpClassPrefix)) {
                        builder.phpClassPrefix(phpClassPrefix);
                    }
                }
                case "php_namespace" -> {
                    if (value instanceof ProtobufLiteralExpression(var phpNamespace)) {
                        builder.phpNamespace(phpNamespace);
                    }
                }
                case "php_metadata_namespace" -> {
                    if (value instanceof ProtobufLiteralExpression(var phpMetadataNamespace)) {
                        builder.phpMetadataNamespace(phpMetadataNamespace);
                    }
                }
                case "ruby_package" -> {
                    if (value instanceof ProtobufLiteralExpression(var rubyPackage)) {
                        builder.rubyPackage(rubyPackage);
                    }
                }
                default -> {
                    // Unknown or unsupported file option
                }
            }
        }
        var features = ProtobufTreeFeatures.featureSet(this);
        if (features != null) {
            builder.features(features);
        }
        var unknownExtensions = ProtobufTreeFeatures.collectUnknownExtensions(this);
        if (!unknownExtensions.isEmpty()) {
            builder.unknownFields(unknownExtensions);
        }
        return builder;
    }

    private DescriptorProtosSourceCodeInfoBuilder buildSourceCodeInfo() {
        var locations = new ArrayList<DescriptorProtos.SourceCodeInfo.Location>();

        // Top-level location with an empty path — represents the file as a whole and carries any
        // document-level header comments captured by the lexer.
        if (range != ProtobufTreeSourceRange.UNKNOWN || !comments.isEmpty()) {
            locations.add(makeLocation(List.of(), this));
        }

        // File-level singletons
        getDirectChildByType(ProtobufPackageStatement.class)
                .ifPresent(stmt -> locations.add(makeLocation(List.of(FILE_PACKAGE), stmt)));
        var syntax = (ProtobufTree.WithComments) getDirectChildByType(ProtobufSyntaxStatement.class).orElse(null);
        if (syntax == null) {
            syntax = getDirectChildByType(ProtobufEditionStatement.class).orElse(null);
        }
        if (syntax != null) {
            locations.add(makeLocation(List.of(FILE_SYNTAX), syntax));
        }

        var imports = getDirectChildrenByType(ProtobufImportStatement.class).toList();
        var regularImportIndex = 0;
        var optionImportIndex = 0;
        for (var importStatement : imports) {
            if (importStatement.modifier() == ProtobufImportStatement.Modifier.OPTION) {
                locations.add(makeLocation(List.of(FILE_OPTION_DEPENDENCY, optionImportIndex), importStatement));
                optionImportIndex++;
            } else {
                locations.add(makeLocation(List.of(FILE_DEPENDENCY, regularImportIndex), importStatement));
                regularImportIndex++;
            }
        }

        var fileOptionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (!fileOptionStatements.isEmpty()) {
            locations.add(makeLocation(List.of(FILE_OPTIONS), fileOptionStatements.getFirst()));
        }

        var messages = getDirectChildrenByType(ProtobufMessageStatement.class).toList();
        for (var i = 0; i < messages.size(); i++) {
            collectMessageLocations(locations, messages.get(i), List.of(FILE_MESSAGE_TYPE, i));
        }

        var enums = getDirectChildrenByType(ProtobufEnumStatement.class).toList();
        for (var i = 0; i < enums.size(); i++) {
            collectEnumLocations(locations, enums.get(i), List.of(FILE_ENUM_TYPE, i));
        }

        var services = getDirectChildrenByType(ProtobufServiceStatement.class).toList();
        for (var i = 0; i < services.size(); i++) {
            collectServiceLocations(locations, services.get(i), List.of(FILE_SERVICE, i));
        }

        var extendStatements = getDirectChildrenByType(ProtobufExtendStatement.class).toList();
        var extensionIndex = 0;
        for (var extendStatement : extendStatements) {
            for (var field : (Iterable<ProtobufFieldStatement>) extendStatement.getDirectChildrenByType(ProtobufFieldStatement.class)::iterator) {
                collectFieldLocations(locations, field, List.of(FILE_EXTENSION, extensionIndex));
                extensionIndex++;
            }
        }

        if (locations.isEmpty()) {
            return null;
        }
        return new DescriptorProtosSourceCodeInfoBuilder()
                .location(locations);
    }

    private static void collectMessageLocations(
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            ProtobufMessageStatement message,
            List<Integer> path
    ) {
        locations.add(makeLocation(path, message));
        locations.add(makeLocation(append(path, MESSAGE_NAME), message));

        var fields = message.getDirectChildrenByType(ProtobufFieldStatement.class).toList();
        for (var i = 0; i < fields.size(); i++) {
            collectFieldLocations(locations, fields.get(i), append(path, MESSAGE_FIELD, i));
        }

        var nested = message.getDirectChildrenByType(ProtobufMessageStatement.class).toList();
        for (var i = 0; i < nested.size(); i++) {
            collectMessageLocations(locations, nested.get(i), append(path, MESSAGE_NESTED_TYPE, i));
        }

        var nestedEnums = message.getDirectChildrenByType(ProtobufEnumStatement.class).toList();
        for (var i = 0; i < nestedEnums.size(); i++) {
            collectEnumLocations(locations, nestedEnums.get(i), append(path, MESSAGE_ENUM_TYPE, i));
        }

        var oneofs = message.getDirectChildrenByType(ProtobufOneofStatement.class).toList();
        for (var i = 0; i < oneofs.size(); i++) {
            collectOneofLocations(locations, oneofs.get(i), append(path, MESSAGE_ONEOF_DECL, i));
        }

        var extendStatements = message.getDirectChildrenByType(ProtobufExtendStatement.class).toList();
        var extensionIndex = 0;
        for (var extendStatement : extendStatements) {
            for (var field : (Iterable<ProtobufFieldStatement>) extendStatement.getDirectChildrenByType(ProtobufFieldStatement.class)::iterator) {
                collectFieldLocations(locations, field, append(path, MESSAGE_EXTENSION, extensionIndex));
                extensionIndex++;
            }
        }
    }

    private static void collectFieldLocations(
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            ProtobufFieldStatement field,
            List<Integer> path
    ) {
        locations.add(makeLocation(path, field));
        locations.add(makeLocation(append(path, FIELD_NAME), field));
    }

    private static void collectEnumLocations(
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            ProtobufEnumStatement enumStatement,
            List<Integer> path
    ) {
        locations.add(makeLocation(path, enumStatement));
        locations.add(makeLocation(append(path, ENUM_NAME), enumStatement));

        var values = enumStatement.getDirectChildrenByType(ProtobufEnumConstantStatement.class).toList();
        for (var i = 0; i < values.size(); i++) {
            var constant = values.get(i);
            var constantPath = append(path, ENUM_VALUE, i);
            locations.add(makeLocation(constantPath, constant));
            locations.add(makeLocation(append(constantPath, ENUM_VALUE_NAME), constant));
        }
    }

    private static void collectServiceLocations(
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            ProtobufServiceStatement service,
            List<Integer> path
    ) {
        locations.add(makeLocation(path, service));
        locations.add(makeLocation(append(path, SERVICE_NAME), service));

        var methods = service.getDirectChildrenByType(ProtobufMethodStatement.class).toList();
        for (var i = 0; i < methods.size(); i++) {
            var method = methods.get(i);
            var methodPath = append(path, SERVICE_METHOD, i);
            locations.add(makeLocation(methodPath, method));
            locations.add(makeLocation(append(methodPath, METHOD_NAME), method));
        }
    }

    private static void collectOneofLocations(
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            ProtobufOneofStatement oneof,
            List<Integer> path
    ) {
        locations.add(makeLocation(path, oneof));
        locations.add(makeLocation(append(path, ONEOF_NAME), oneof));
    }

    private static DescriptorProtos.SourceCodeInfo.Location makeLocation(List<Integer> path, ProtobufTree.WithComments node) {
        var range = node.range();
        // descriptor.proto's SourceCodeInfo.span uses 0-indexed line and column values.
        var startLine = Math.max(range.start().line() - 1, 0);
        var startCol = Math.max(range.start().column() - 1, 0);
        var endLine = Math.max(range.end().line() - 1, startLine);
        var endCol = Math.max(range.end().column() - 1, startCol);
        var span = (startLine == endLine)
                ? List.of(startLine, startCol, endCol)
                : List.of(startLine, startCol, endLine, endCol);
        var builder = new SourceCodeInfoDescriptorProtosLocationBuilder()
                .path(new ArrayList<>(path))
                .span(span);
        String leading = null;
        String trailing = null;
        var leadingDetached = new ArrayList<String>();
        for (var comment : node.comments()) {
            switch (comment) {
                case ProtobufCommentStatement.Leading leadingComment -> leading = leadingComment.text();
                case ProtobufCommentStatement.Trailing trailingComment -> trailing = trailingComment.text();
                case ProtobufCommentStatement.LeadingDetached detachedComment -> leadingDetached.add(detachedComment.text());
            }
        }
        if (leading != null) {
            builder.leadingComments(leading);
        }
        if (trailing != null) {
            builder.trailingComments(trailing);
        }
        if (!leadingDetached.isEmpty()) {
            builder.leadingDetachedComments(leadingDetached);
        }
        return builder.build();
    }

    private static List<Integer> append(List<Integer> base, int... values) {
        var result = new ArrayList<Integer>(base.size() + values.length);
        result.addAll(base);
        for (var value : values) {
            result.add(value);
        }
        return result;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        children().forEach(statement -> {
            builder.append(statement);
            builder.append("\n");
        });
        return builder.toString();
    }

    @Override
    public SequencedCollection<ProtobufDocumentChild> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void addChild(ProtobufDocumentChild statement) {
        children.add(statement);
        if(statement instanceof ProtobufStatementImpl mutableTree) {
            mutableTree.setParent(this);
        }
    }

    @Override
    public boolean removeChild(ProtobufDocumentChild statement) {
        var result = children.remove(statement);
        if(result && statement instanceof ProtobufStatementImpl mutableTree) {
            mutableTree.setParent(null);
        }
        return result;
    }

    @Override
    public <V extends ProtobufTree> Optional<V> getDirectChildByType(Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getDirectChildrenByType(children, clazz)
                .findFirst();
    }

    @Override
    public <V extends ProtobufTree> Stream<V> getDirectChildrenByType(Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getDirectChildrenByType(children, clazz);
    }

    @Override
    public Optional<WithName> getDirectChildByName(String name) {
        return ProtobufStatementWithBodyImpl.getDirectChildByName(children, name);
    }

    /**
     * Returns a direct child with the specified index.
     * <p>
     * Documents do not have indexed children at the file level, so this always returns empty.
     * </p>
     *
     * @param index the index to search for
     * @return always returns empty
     */
    @Override
    public Optional<WithIndex> getDirectChildByIndex(long index) {
        return Optional.empty(); // No direct child with an index exists in a document
    }

    @Override
    public <V extends ProtobufTree> Optional<V> getDirectChildByNameAndType(String name, Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getDirectChildByNameAndType(children, name, clazz);
    }

    /**
     * Returns a direct child with the specified index and type.
     * <p>
     * Documents do not have indexed children at the file level, so this always returns empty.
     * </p>
     *
     * @param index the index to search for
     * @param clazz the type of child to search for
     * @param <V>   the type parameter
     * @return always returns empty
     */
    @Override
    public <V extends ProtobufTree> Optional<V> getDirectChildByIndexAndType(long index, Class<V> clazz) {
        return Optional.empty(); // No direct child with an index exists in a document
    }

    @Override
    public <V extends ProtobufTree> Optional<V> getAnyChildByType(Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getAnyChildrenByType(children, clazz)
                .findFirst();
    }

    @Override
    public <V extends WithName> Optional<V> getAnyChildByNameAndType(String name, Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getAnyChildrenByNameAndType(children, name, clazz)
                .findFirst();
    }

    @Override
    public <V extends ProtobufTree> Stream<V> getAnyChildrenByType(Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getAnyChildrenByType(children, clazz);
    }

    @Override
    public <V extends WithName> Stream<V> getAnyChildrenByNameAndType(String name, Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getAnyChildrenByNameAndType(children, name, clazz);
    }
}
