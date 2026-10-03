package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufIntegerRange;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosEnumDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosEnumOptionsBuilder;
import com.google.protobuf.EnumDescriptorProtoDescriptorProtosEnumReservedRangeBuilder;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;
import com.github.auties00.daedalus.protobuf.model.ProtobufJsonCompatibility;

import java.util.ArrayList;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Represents an enum type declaration in the Protocol Buffer AST.
 * <p>
 * Enums define a type with a restricted set of named integer constants. Each enum value consists
 * of a name and an associated numeric value. Enums can be defined at file level or nested within
 * messages.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Simple enum
 * enum Status {
 *   UNKNOWN = 0;
 *   PENDING = 1;
 *   APPROVED = 2;
 *   REJECTED = 3;
 * }
 *
 * // Enum with aliases (allow_alias option)
 * enum Priority {
 *   option allow_alias = true;
 *   PRIORITY_UNSPECIFIED = 0;
 *   LOW = 1;
 *   MEDIUM = 2;
 *   HIGH = 3;
 *   CRITICAL = 3;  // Alias for HIGH
 * }
 *
 * // Enum with reserved values
 * enum Color {
 *   reserved 2, 15, 9 to 11;
 *   reserved "FOO", "BAR";
 *   COLOR_UNKNOWN = 0;
 *   RED = 1;
 * }
 * }</pre>
 * <p>
 * Enums can contain various child elements:
 * </p>
 * <ul>
 *   <li><strong>Enum constants:</strong> Named values with associated integer numbers</li>
 *   <li><strong>Options:</strong> Configuration options for the enum (e.g., allow_alias)</li>
 *   <li><strong>Reserved:</strong> Reserved field numbers or names to prevent reuse</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * Important enum requirements:
 * </p>
 * <ul>
 *   <li>In proto3, the first enum value must be zero</li>
 *   <li>By default, enum values must be unique; use {@code allow_alias = true} to allow aliases</li>
 *   <li>Enum value numbers must be within the 32-bit integer range</li>
 * </ul>
 * <p>
 * This class implements multiple child marker interfaces, allowing enums to appear at file level,
 * nested within messages, inside extend blocks, or as group field types.
 * </p>
 * <p>
 * Enums are attributed when they have a name and all their child statements are attributed.
 * During semantic analysis, enum values are validated for uniqueness and proto3 zero-value requirements.
 * </p>
 *
 * @see ProtobufEnumChild
 * @see ProtobufEnumConstantStatement
 * @see ProtobufReservedStatement
 */
public final class ProtobufEnumStatement
        extends ProtobufStatementWithBodyImpl<ProtobufEnumChild>
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithBody<ProtobufEnumChild>,
                   ProtobufDocumentChild, ProtobufMessageChild, ProtobufGroupChild, ProtobufTree.WithDescriptor {
    private String name;
    private ProtobufTreeVisibility visibility;

    /**
     * Constructs a new enum statement covering the specified source range.
     *
     * @param document the enclosing document
     * @param range the source range covering this statement
     */
    public ProtobufEnumStatement(ProtobufDocumentTree document, ProtobufTreeSourceRange range) {
        super(document, range);
        this.visibility = ProtobufTreeVisibility.NONE;
    }

    /**
     * Returns the visibility modifier for this enum.
     *
     * @return the visibility modifier
     */
    public ProtobufTreeVisibility visibility() {
        return visibility;
    }

    /**
     * Sets the visibility modifier for this enum.
     *
     * @param visibility the visibility modifier to set
     */
    public void setVisibility(ProtobufTreeVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the enum name.
     *
     * @return the enum name, or null if not yet set
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether this enum has a name assigned.
     *
     * @return true if a name is present, false otherwise
     */
    @Override
    public boolean hasName() {
        return name != null;
    }

    /**
     * Sets the name for this enum.
     * <p>
     * The name must be a valid Protocol Buffer identifier and unique within the parent scope.
     * </p>
     *
     * @param name the enum name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the resolved enum type for this enum by walking up the AST.
     *
     * @return the resolved enum type
     */
    public ProtobufEnumType enumType() {
        return ProtobufTreeFeatures.enumType(this);
    }

    /**
     * Returns the resolved JSON compatibility for this enum by walking up the AST.
     *
     * @return the resolved JSON compatibility
     */
    public ProtobufJsonCompatibility jsonCompatibility() {
        return ProtobufTreeFeatures.jsonCompatibility(this);
    }

    @Override
    public DescriptorProtos.EnumDescriptorProto toDescriptor() {
        var values = getDirectChildrenByType(ProtobufEnumConstantStatement.class)
                .map(ProtobufEnumConstantStatement::toDescriptor)
                .toList();

        var reservedRanges = new ArrayList<DescriptorProtos.EnumDescriptorProto.EnumReservedRange>();
        var reservedNames = new ArrayList<String>();
        getDirectChildrenByType(ProtobufReservedStatement.class)
                .flatMap(reserved -> reserved.expressions().stream())
                .forEachOrdered(reservedExpression -> {
                    switch (reservedExpression) {
                        case ProtobufIntegerExpression integerExpression -> integerExpression.value()
                                .toEnumConstantIndex()
                                .ifPresent(index -> {
                                    var range = new EnumDescriptorProtoDescriptorProtosEnumReservedRangeBuilder()
                                            .start(index)
                                            .end(index + 1)
                                            .build();
                                    reservedRanges.add(range);
                                });

                        case ProtobufIntegerRangeExpression rangeExpression -> {
                            var min = rangeExpression.value()
                                    .lowerBound()
                                    .toEnumConstantIndex();
                            var max = switch (rangeExpression.value()) {
                                case ProtobufIntegerRange.LowerBounded _ -> OptionalInt.of(ProtobufEnum.MAX_CONSTANT_INDEX);
                                case ProtobufIntegerRange.Bounded(var _, var upperBound) -> upperBound.toEnumConstantIndex();
                            };
                            if (min.isPresent() && max.isPresent()) {
                                var range = new EnumDescriptorProtoDescriptorProtosEnumReservedRangeBuilder()
                                        .start(min.getAsInt())
                                        .end(max.getAsInt() + 1)
                                        .build();
                                reservedRanges.add(range);
                            }
                        }

                        case ProtobufLiteralExpression literalExpression -> reservedNames.add(literalExpression.value());
                    }
                });

        var builder = new DescriptorProtosEnumDescriptorProtoBuilder()
                .name(name())
                .value(values)
                .reservedRange(reservedRanges)
                .reservedName(reservedNames);
        var enumOptions = buildEnumOptions();
        if (enumOptions != null) {
            builder.options(enumOptions);
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

    private DescriptorProtosEnumOptionsBuilder buildEnumOptions() {
        var optionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (optionStatements.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosEnumOptionsBuilder();
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
                case "allow_alias" -> {
                    if (value instanceof ProtobufBoolExpression(var allowAlias)) {
                        builder.allowAlias(allowAlias);
                    }
                }
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                    }
                }
                case "deprecated_legacy_json_field_conflicts" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecatedLegacyJsonFieldConflicts)) {
                        builder.deprecatedLegacyJsonFieldConflicts(deprecatedLegacyJsonFieldConflicts);
                    }
                }
                default -> {
                    // uninterpreted_option is not derivable from the AST; features is handled below
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

        builder.append("enum");
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
}
