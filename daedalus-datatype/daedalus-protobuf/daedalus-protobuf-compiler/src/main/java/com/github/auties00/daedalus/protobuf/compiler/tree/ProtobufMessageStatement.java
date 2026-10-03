package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufIntegerRange;
import com.google.protobuf.DescriptorProtoDescriptorProtosExtensionRangeBuilder;
import com.google.protobuf.DescriptorProtoDescriptorProtosReservedRangeBuilder;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosExtensionRangeOptionsBuilder;
import com.google.protobuf.DescriptorProtosMessageOptionsBuilder;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufEnumConstantExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.model.ProtobufJsonCompatibility;

import java.util.ArrayList;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Represents a message type declaration in the Protocol Buffer AST.
 * <p>
 * Messages are the fundamental structured data types in Protocol Buffers, similar to classes in
 * object-oriented programming or structs in C. A message contains fields that hold data and can
 * be nested within other messages or defined at the file level.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Simple message
 * message Person {
 *   string name = 1;
 *   int32 age = 2;
 * }
 *
 * // Message with nested types
 * message SearchResponse {
 *   message Result {
 *     string url = 1;
 *     string title = 2;
 *   }
 *   repeated Result results = 1;
 * }
 *
 * // Message with options and reserved fields
 * message Example {
 *   option deprecated = true;
 *   reserved 2, 15, 9 to 11;
 *   reserved "foo", "bar";
 *   string field = 1;
 * }
 * }</pre>
 * <p>
 * Messages can contain various child elements:
 * </p>
 * <ul>
 *   <li><strong>Fields:</strong> Data members with types and field numbers</li>
 *   <li><strong>Nested messages:</strong> Message definitions within the message body</li>
 *   <li><strong>Nested enums:</strong> Enum definitions scoped to the message</li>
 *   <li><strong>Oneofs:</strong> Groups of fields where at most one can be set</li>
 *   <li><strong>Reserved:</strong> Reserved field numbers or names</li>
 *   <li><strong>Options:</strong> Configuration options for the message</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * This class implements multiple child marker interfaces, allowing messages to appear at file level,
 * nested within other messages, inside extend blocks, or as group field types.
 * </p>
 * <p>
 * Messages are attributed when they have a name and all their child statements are attributed.
 * The name is resolved during semantic analysis to ensure uniqueness within the parent scope.
 * </p>
 *
 * @see ProtobufMessageChild
 * @see ProtobufFieldStatement
 * @see ProtobufOneofStatement
 * @see ProtobufGroupStatement
 */
public final class ProtobufMessageStatement
        extends ProtobufStatementWithBodyImpl<ProtobufMessageChild>
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithBody<ProtobufMessageChild>,
                   ProtobufDocumentChild, ProtobufGroupChild, ProtobufMessageChild, ProtobufTree.WithDescriptor {
    private String name;
    private ProtobufTreeVisibility visibility;

    /**
     * Constructs a new message statement covering the specified source range.
     *
     * @param document the enclosing document
     * @param range the source range covering this statement
     */
    public ProtobufMessageStatement(ProtobufDocumentTree document, ProtobufTreeSourceRange range) {
        super(document, range);
        this.visibility = ProtobufTreeVisibility.NONE;
    }

    /**
     * Returns the visibility modifier for this message.
     *
     * @return the visibility modifier
     */
    public ProtobufTreeVisibility visibility() {
        return visibility;
    }

    /**
     * Sets the visibility modifier for this message.
     *
     * @param visibility the visibility modifier to set
     */
    public void setVisibility(ProtobufTreeVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the resolved JSON compatibility for this message by walking up the AST.
     *
     * @return the resolved JSON compatibility
     */
    public ProtobufJsonCompatibility jsonCompatibility() {
        return ProtobufTreeFeatures.jsonCompatibility(this);
    }

    @Override
    public DescriptorProtos.DescriptorProto toDescriptor() {
        var fields = getDirectChildrenByType(ProtobufFieldStatement.class)
                .map(ProtobufFieldStatement::toDescriptor)
                .toList();
        var nestedMessages = getDirectChildrenByType(ProtobufMessageStatement.class)
                .map(ProtobufMessageStatement::toDescriptor)
                .toList();
        var nestedEnums = getDirectChildrenByType(ProtobufEnumStatement.class)
                .map(ProtobufEnumStatement::toDescriptor)
                .toList();
        var oneofs = getDirectChildrenByType(ProtobufOneofStatement.class)
                .map(ProtobufOneofStatement::toDescriptor)
                .toList();
        var extensionFields = getDirectChildrenByType(ProtobufExtendStatement.class)
                .flatMap(ext -> ext.getDirectChildrenByType(ProtobufFieldStatement.class))
                .map(ProtobufFieldStatement::toDescriptor)
                .toList();

        var reservedRanges = new ArrayList<DescriptorProtos.DescriptorProto.ReservedRange>();
        var reservedNames = new ArrayList<String>();
        getDirectChildrenByType(ProtobufReservedStatement.class)
                .flatMap(reserved -> reserved.expressions().stream())
                .forEachOrdered(reservedExpression -> {
                    switch (reservedExpression) {
                        case ProtobufIntegerExpression integerExpression -> integerExpression.value()
                                .toMessageFieldIndex()
                                .ifPresent(index -> {
                                    var range = new DescriptorProtoDescriptorProtosReservedRangeBuilder()
                                            .start(index)
                                            .end(index + 1)
                                            .build();
                                    reservedRanges.add(range);
                                });

                        case ProtobufIntegerRangeExpression rangeExpression -> {
                            var min = rangeExpression.value()
                                    .lowerBound()
                                    .toMessageFieldIndex();
                            var max = switch (rangeExpression.value()) {
                                case ProtobufIntegerRange.LowerBounded _ -> OptionalInt.of(ProtobufMessage.MAX_FIELD_INDEX);
                                case ProtobufIntegerRange.Bounded(var _, var upperBound) -> upperBound.toMessageFieldIndex();
                            };
                            if (min.isPresent() && max.isPresent()) {
                                var range = new DescriptorProtoDescriptorProtosReservedRangeBuilder()
                                        .start(min.getAsInt())
                                        .end(max.getAsInt() + 1)
                                        .build();
                                reservedRanges.add(range);
                            }
                        }

                        case ProtobufLiteralExpression literalExpression -> reservedNames.add(literalExpression.value());
                    }
                });

        var extensionRanges = new ArrayList<DescriptorProtos.DescriptorProto.ExtensionRange>();
        getDirectChildrenByType(ProtobufExtensionsStatement.class)
                .forEachOrdered(extensions -> {
                    var extensionRangeOptions = buildExtensionRangeOptions(extensions);
                    for (var extensionsExpression : extensions.expressions()) {
                        switch (extensionsExpression) {
                            case ProtobufIntegerExpression integerExpression -> integerExpression.value()
                                    .toMessageFieldIndex()
                                    .ifPresent(index -> {
                                        var rangeBuilder = new DescriptorProtoDescriptorProtosExtensionRangeBuilder()
                                                .start(index)
                                                .end(index + 1);
                                        if (extensionRangeOptions != null) {
                                            rangeBuilder.options(extensionRangeOptions);
                                        }
                                        extensionRanges.add(rangeBuilder.build());
                                    });

                            case ProtobufIntegerRangeExpression rangeExpression -> {
                                var min = rangeExpression.value()
                                        .lowerBound()
                                        .toMessageFieldIndex();
                                var max = switch (rangeExpression.value()) {
                                    case ProtobufIntegerRange.LowerBounded _ -> OptionalInt.of(ProtobufMessage.MAX_FIELD_INDEX);
                                    case ProtobufIntegerRange.Bounded(var _, var upperBound) -> upperBound.toMessageFieldIndex();
                                };
                                if (min.isPresent() && max.isPresent()) {
                                    var rangeBuilder = new DescriptorProtoDescriptorProtosExtensionRangeBuilder()
                                            .start(min.getAsInt())
                                            .end(max.getAsInt() + 1);
                                    if (extensionRangeOptions != null) {
                                        rangeBuilder.options(extensionRangeOptions);
                                    }
                                    extensionRanges.add(rangeBuilder.build());
                                }
                            }
                        }
                    }
                });


        var builder = new DescriptorProtosDescriptorProtoBuilder()
                .name(name())
                .field(fields)
                .nestedType(nestedMessages)
                .enumType(nestedEnums)
                .oneofDecl(oneofs)
                .extension(extensionFields)
                .reservedRange(reservedRanges)
                .reservedName(reservedNames)
                .extensionRange(extensionRanges);
        var messageOptions = buildMessageOptions();
        if (messageOptions != null) {
            builder.options(messageOptions);
        }
        switch (visibility) {
            case EXPORT -> builder.visibility(DescriptorProtos.SymbolVisibility.VISIBILITY_EXPORT);
            case LOCAL -> builder.visibility(DescriptorProtos.SymbolVisibility.VISIBILITY_LOCAL);
            case NONE -> {
                // Leave unset so the descriptor reflects no explicit visibility
            }
        }
        return builder.build();
    }

    /**
     * Builds an {@link DescriptorProtos.ExtensionRangeOptions} descriptor from the
     * options declared on the given {@link ProtobufExtensionsStatement}.
     *
     * <p>Only the {@code verification} enum field is currently mapped. The
     * {@code declaration}, {@code features}, and {@code uninterpreted_option}
     * fields have no AST representation and are therefore ignored.
     *
     * @param extensions the extensions statement whose options should be mapped
     * @return the builder of the populated {@code ExtensionRangeOptions}, or
     *         {@code null} when the statement has no recognised options
     */
    private DescriptorProtosExtensionRangeOptionsBuilder buildExtensionRangeOptions(ProtobufExtensionsStatement extensions) {
        if (extensions.options().isEmpty()) {
            return null;
        }
        var optionsBuilder = new DescriptorProtosExtensionRangeOptionsBuilder();
        var populated = false;
        for (var option : extensions.options()) {
            if (option.name().segments().size() != 1) {
                continue;
            }
            var segment = option.name().segments().getFirst();
            if (segment.isExtension()) {
                continue;
            }
            var value = option.value();
            if ("verification".equals(segment.name()) && value instanceof ProtobufEnumConstantExpression(var constantName)) {
                switch (constantName) {
                    case "DECLARATION" -> {
                        optionsBuilder.verification(DescriptorProtos.ExtensionRangeOptions.VerificationState.DECLARATION);
                        populated = true;
                    }
                    case "UNVERIFIED" -> {
                        optionsBuilder.verification(DescriptorProtos.ExtensionRangeOptions.VerificationState.UNVERIFIED);
                        populated = true;
                    }
                    default -> {
                        // Unknown verification state
                    }
                }
            }
        }
        var features = ProtobufTreeFeatures.featureSet(extensions);
        if (features != null) {
            optionsBuilder.features(features);
            populated = true;
        }
        var unknownExtensions = ProtobufTreeFeatures.collectUnknownExtensions(extensions);
        if (!unknownExtensions.isEmpty()) {
            optionsBuilder.unknownFields(unknownExtensions);
            populated = true;
        }
        if (!populated) {
            return null;
        }
        return optionsBuilder;
    }

    private DescriptorProtosMessageOptionsBuilder buildMessageOptions() {
        var optionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (optionStatements.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosMessageOptionsBuilder();
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
                case "message_set_wire_format" -> {
                    if (value instanceof ProtobufBoolExpression(var messageSetWireFormat)) {
                        builder.messageSetWireFormat(messageSetWireFormat);
                    }
                }
                case "no_standard_descriptor_accessor" -> {
                    if (value instanceof ProtobufBoolExpression(var noStandardDescriptorAccessor)) {
                        builder.noStandardDescriptorAccessor(noStandardDescriptorAccessor);
                    }
                }
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                    }
                }
                case "map_entry" -> {
                    if (value instanceof ProtobufBoolExpression(var mapEntry)) {
                        builder.mapEntry(mapEntry);
                    }
                }
                case "deprecated_legacy_json_field_conflicts" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecatedLegacyJsonFieldConflicts)) {
                        builder.deprecatedLegacyJsonFieldConflicts(deprecatedLegacyJsonFieldConflicts);
                    }
                }
                default -> {
                    // Unknown or unsupported message option
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

    @Override
    public String toString() {
        var builder = new StringBuilder();

        if(visibility != ProtobufTreeVisibility.NONE) {
            builder.append(visibility.token());
            builder.append(" ");
        }

        builder.append("message");
        builder.append(" ");

        var name = Objects.requireNonNullElse(this.name, "[missing]");
        builder.append(name);
        builder.append(" ");

        builder.append("{");
        builder.append("\n");

        if(children.isEmpty()) {
            builder.append("\n");
        } else {
            children.forEach(statement -> {
                builder.append("    ");
                builder.append(statement);
                builder.append("\n");
            });
        }

        builder.append("}");

        return builder.toString();
    }

    /**
     * Returns the message name.
     *
     * @return the message name, or null if not yet set
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether this message has a name assigned.
     *
     * @return true if a name is present, false otherwise
     */
    @Override
    public boolean hasName() {
        return name != null;
    }

    /**
     * Sets the name for this message.
     * <p>
     * The name must be a valid Protocol Buffer identifier and unique within the parent scope.
     * </p>
     *
     * @param name the message name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
}
