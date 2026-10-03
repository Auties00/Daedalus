package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufValueTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufUnresolvedTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosFieldDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosFieldOptionsBuilder;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufEnumConstantExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufFloatingPointExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufOptionExpression;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufEnumTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufGroupTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMessageTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a field declaration in the Protocol Buffer AST.
 * <p>
 * Fields are the primary data members of messages, specifying the structure and types of data
 * that can be stored. Each field has a name, type, field number (index), an optional modifier
 * (required/optional/repeated), and optional field options.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Simple fields
 * string name = 1;
 * int32 age = 2;
 *
 * // Fields with modifiers (proto2)
 * required string email = 3;
 * optional int32 phone = 4;
 * repeated string tags = 5;
 *
 * // Fields with options
 * string id = 1 [deprecated = true];
 * bytes data = 2 [ctype = CORD, packed = true];
 *
 * // Complex type fields
 * User user = 10;
 * map<string, int32> scores = 11;
 * }</pre>
 * <p>
 * Field modifiers (proto2 vs proto3):
 * </p>
 * <ul>
 *   <li><strong>required:</strong> (proto2 only) Field must be set</li>
 *   <li><strong>optional:</strong> Field may or may not be set</li>
 *   <li><strong>repeated:</strong> Field can appear zero or more times (array/list)</li>
 *   <li><strong>none:</strong> Default for proto3 fields (implicitly optional)</li>
 * </ul>
 * <p>
 * Field numbers (indices) must be:
 * </p>
 * <ul>
 *   <li>Unique within the message</li>
 *   <li>In the range 1 to 536,870,911 (2^29 - 1)</li>
 *   <li>Not in the reserved range 19,000 to 19,999 (used by Protocol Buffers implementation)</li>
 *   <li>Not previously reserved by the message</li>
 * </ul>
 * <p>
 * This sealed class permits three specialized subclasses:
 * </p>
 * <ul>
 *   <li>{@link ProtobufEnumConstantStatement}: Enum value declarations</li>
 *   <li>{@link ProtobufGroupStatement}: Deprecated group field syntax</li>
 *   <li>{@link ProtobufOneofStatement}: Oneof group declarations</li>
 * </ul>
 * <p>
 * This class implements multiple child marker interfaces, allowing fields to appear in messages,
 * oneofs, extend blocks, and groups. Fields are attributed when they have a name, type, index,
 * and modifier assigned.
 * </p>
 *
 * @see ProtobufMessageChild
 * @see ProtobufOneofChild
 * @see ProtobufTypeReference
 * @see ProtobufOptionExpression
 */
public final class ProtobufFieldStatement
        extends ProtobufStatementImpl
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithIndex, ProtobufTree.WithOptions, ProtobufTree.WithType, ProtobufTree.WithModifier, ProtobufOptionDefinition,
                   ProtobufOneofChild, ProtobufMessageChild, ProtobufGroupChild, ProtobufExtendChild, ProtobufTree.WithDescriptor {
    private ProtobufModifier modifier;
    private ProtobufTypeReference type;
    private String name;
    private ProtobufInteger index;
    private final SequencedMap<String, ProtobufOptionExpression> options;

    /**
     * Constructs a new field statement covering the specified source range.
     *
     * @param range the source range covering this statement
     */
    public ProtobufFieldStatement(ProtobufTreeSourceRange range) {
        super(range);
        this.options = new LinkedHashMap<>();
    }

    /**
     * Returns the field name.
     *
     * @return the field name, or null if not yet set
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether this field has a name assigned.
     *
     * @return true if a name is present, false otherwise
     */
    @Override
    public boolean hasName() {
        return name != null;
    }

    /**
     * Sets the name for this field.
     * <p>
     * The name must be a valid Protocol Buffer identifier and unique within the parent scope.
     * </p>
     *
     * @param name the field name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the field number (index).
     * <p>
     * Field numbers are used to identify fields in the binary encoding and must be unique
     * within the message.
     * </p>
     *
     * @return the field number, or null if not yet set
     */
    @Override
    public ProtobufInteger index() {
        return index;
    }

    /**
     * Checks whether this field has an index assigned.
     *
     * @return true if an index is present, false otherwise
     */
    @Override
    public boolean hasIndex() {
        return index != null;
    }

    /**
     * Sets the field number (index) for this field.
     * <p>
     * The index must be within valid ranges and not conflict with reserved numbers.
     * </p>
     *
     * @param index the field number to set
     */
    @Override
    public void setIndex(ProtobufInteger index) {
        this.index = index;
    }

    /**
     * Returns the field type reference.
     *
     * @return the type reference, or null if not yet set
     */
    public ProtobufTypeReference type() {
        return type;
    }

    /**
     * Checks whether this field has a type assigned.
     *
     * @return true if a type is present, false otherwise
     */
    public boolean hasType() {
        return type != null;
    }

    /**
     * Sets the type reference for this field.
     * <p>
     * The type can be a primitive type, message type, enum type, or map type.
     * </p>
     *
     * @param type the type reference to set
     */
    public void setType(ProtobufTypeReference type) {
        this.type = type;
    }

    /**
     * Returns the field modifier (required/optional/repeated/none).
     *
     * @return the modifier, or null if not yet set
     */
    public ProtobufModifier modifier() {
        return modifier;
    }

    /**
     * Checks whether this field has a modifier assigned.
     *
     * @return true if a modifier is present, false otherwise
     */
    public boolean hasModifier() {
        return modifier != null;
    }

    /**
     * Sets the modifier for this field.
     *
     * @param modifier the modifier to set
     */
    public void setModifier(ProtobufModifier modifier) {
        this.modifier = modifier;
    }

    @Override
    public SequencedCollection<ProtobufOptionExpression> options() {
        return options.sequencedValues();
    }

    @Override
    public void addOption(ProtobufOptionExpression value) {
        Objects.requireNonNull(value, "Cannot add null option");
        options.put(value.name().toString(), value);
    }

    @Override
    public boolean removeOption(String name) {
        return options.remove(name) != null;
    }

    @Override
    public Optional<ProtobufOptionExpression> getOption(String name) {
        return Optional.ofNullable(options.get(name));
    }

    @Override
    public boolean isAttributed() {
        return hasType() && hasName() && hasIndex();
    }

    /**
     * Returns the type of the descriptor of a field of the given type.
     *
     * <p>A map field is described as a repeated message field whose message is the entry of
     * the map, and a group as a field of type {@code TYPE_GROUP}.
     *
     * @param type the type of the field
     * @return the type of the descriptor, or {@code null} if the type is not resolved
     */
    private static DescriptorProtos.FieldDescriptorProto.Type toDescriptorType(ProtobufTypeReference type) {
        return switch (type) {
            case ProtobufGroupTypeReference _ -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP;
            case ProtobufMapTypeReference _ -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;
            case ProtobufUnresolvedTypeReference _ -> null;
            case ProtobufValueTypeReference valueType -> switch (valueType.protobufType()) {
                case MESSAGE -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;
                case ENUM -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM;
                case FLOAT -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
                case DOUBLE -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
                case BOOL -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
                case STRING -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
                case BYTES -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
                case INT32 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32;
                case SINT32 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;
                case UINT32 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32;
                case FIXED32 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32;
                case SFIXED32 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32;
                case INT64 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64;
                case SINT64 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64;
                case UINT64 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64;
                case FIXED64 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64;
                case SFIXED64 -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64;
            };
        };
    }

    @Override
    public DescriptorProtos.FieldDescriptorProto toDescriptor() {
        var builder = new DescriptorProtosFieldDescriptorProtoBuilder()
                .name(name())
                .number(hasIndex() ? (int) index().value() : 0);
        if (type() != null) {
            var descriptorType = toDescriptorType(type());
            if (descriptorType != null) {
                builder.type(descriptorType);
            }
            if (type() instanceof ProtobufMessageTypeReference
                    || type() instanceof ProtobufEnumTypeReference
                    || type() instanceof ProtobufGroupTypeReference) {
                builder.typeName(type().name());
            }
        }
        if (modifier() != null) {
            builder.label(switch (modifier()) {
                case REQUIRED -> DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED;
                case REPEATED -> DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED;
                case OPTIONAL, NONE -> DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL;
            });
        }

        if (parent() instanceof ProtobufExtendStatement extend && extend.hasDeclaration()) {
            builder.extendee(extend.declaration().name());
        }

        if (parent() instanceof ProtobufOneofStatement oneof && oneof.parent() instanceof ProtobufMessageStatement message) {
            var oneofIndex = 0;
            for (var sibling : message.children()) {
                if (sibling == oneof) {
                    builder.oneofIndex(oneofIndex);
                    break;
                }
                if (sibling instanceof ProtobufOneofStatement) {
                    oneofIndex++;
                }
            }
        }

        for (var option : options.values()) {
            if (option.name().segments().size() != 1 || option.name().segments().getFirst().isExtension()) {
                continue;
            }
            switch (option.name().segments().getFirst().name()) {
                case "default" -> {
                    var defaultValueExpression = option.value();
                    var defaultValue = switch (defaultValueExpression) {
                        case ProtobufLiteralExpression literal -> literal.value();
                        case ProtobufBoolExpression bool -> Boolean.toString(bool.value());
                        case ProtobufIntegerExpression integer -> Long.toString(integer.value().value());
                        case ProtobufFloatingPointExpression floating -> floating.value().toString();
                        case ProtobufEnumConstantExpression enumConstant -> enumConstant.name();
                        default -> defaultValueExpression.toString();
                    };
                    builder.defaultValue(defaultValue);
                }
                case "json_name" -> {
                    if (option.value() instanceof ProtobufLiteralExpression(var jsonName)) {
                        builder.jsonName(jsonName);
                    }
                }
                default -> {}
            }
        }

        if (modifier() == ProtobufModifier.OPTIONAL && parent().document().version() == ProtobufVersion.PROTOBUF_3) {
            builder.proto3Optional(true);
        }

        var fieldOptions = buildFieldOptions();
        if (fieldOptions != null) {
            builder.options(fieldOptions);
        }

        return builder.build();
    }

    private DescriptorProtosFieldOptionsBuilder buildFieldOptions() {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosFieldOptionsBuilder();
        for (var option : options.values()) {
            if (option.name().segments().size() != 1) {
                continue;
            }
            var segment = option.name().segments().getFirst();
            if (segment.isExtension()) {
                continue;
            }
            var value = option.value();
            switch (segment.name()) {
                case "ctype" -> {
                    if (value instanceof ProtobufEnumConstantExpression(var ctypeName)) {
                        try {
                            builder.ctype(DescriptorProtos.FieldOptions.CType.valueOf(ctypeName));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown ctype literal
                        }
                    }
                }
                case "packed" -> {
                    if (value instanceof ProtobufBoolExpression(var packed)) {
                        builder.packed(packed);
                    }
                }
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                    }
                }
                case "lazy" -> {
                    if (value instanceof ProtobufBoolExpression(var lazy)) {
                        builder.lazy(lazy);
                    }
                }
                case "unverified_lazy" -> {
                    if (value instanceof ProtobufBoolExpression(var unverifiedLazy)) {
                        builder.unverifiedLazy(unverifiedLazy);
                    }
                }
                case "jstype" -> {
                    if (value instanceof ProtobufEnumConstantExpression(var jsTypeName)) {
                        try {
                            builder.jstype(DescriptorProtos.FieldOptions.JSType.valueOf(jsTypeName));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown jstype literal
                        }
                    }
                }
                case "weak" -> {
                    if (value instanceof ProtobufBoolExpression(var weak)) {
                        builder.weak(weak);
                    }
                }
                case "debug_redact" -> {
                    if (value instanceof ProtobufBoolExpression(var debugRedact)) {
                        builder.debugRedact(debugRedact);
                    }
                }
                case "retention" -> {
                    if (value instanceof ProtobufEnumConstantExpression(var retentionName)) {
                        try {
                            builder.retention(DescriptorProtos.FieldOptions.OptionRetention.valueOf(retentionName));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown retention literal
                        }
                    }
                }
                case "targets" -> {
                    // The AST represents each [targets = VALUE] entry as a separate single value;
                    // repeated specification via a list literal is not yet modeled by the expression
                    // tree, so only the single value form is supported here.
                    if (value instanceof ProtobufEnumConstantExpression(var targetName)) {
                        try {
                            List<DescriptorProtos.FieldOptions.OptionTargetType> targetList = new ArrayList<>();
                            targetList.add(DescriptorProtos.FieldOptions.OptionTargetType.valueOf(targetName));
                            builder.targets(targetList);
                        } catch (IllegalArgumentException ignored) {
                            // Unknown target type literal
                        }
                    }
                }
                case "edition_defaults" -> {
                    // Requires building a typed EditionDefault aggregate from a ProtobufJsonExpression
                    // with enum-aware edition values, which the option AST does not currently model.
                }
                case "features" -> {
                    // Aggregate-form `option features = { ... }` is not modeled by the AST. The
                    // dotted form `[features.field_presence = EXPLICIT]` is multi-segment and is
                    // handled by ProtobufTreeFeatures.buildExplicitFeatureSet below.
                }
                case "feature_support" -> {
                    // Requires constructing a typed FeatureSupport from nested aggregate option values,
                    // which the option AST does not currently model with enough type information.
                }
                case "uninterpreted_option" -> {
                    // Uninterpreted options are produced by protoc for options whose definitions are
                    // unknown at parse time and cannot be reconstructed from the resolved AST here.
                }
                case "default", "json_name" -> {
                    // Already handled as their own descriptor fields
                }
                default -> {
                    // Unknown or unsupported field option
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
        if(modifier != null) {
            builder.append(modifier.token());
            builder.append(" ");
        }
        var type = Objects.requireNonNullElse(this.type.toString(), "[missing]");
        builder.append(type);
        builder.append(" ");
        var name = Objects.requireNonNullElse(this.name, "[missing]");
        builder.append(name);
        builder.append(" ");
        var index = Objects.requireNonNullElse(this.index, "[missing]");
        builder.append("=");
        builder.append(" ");
        builder.append(index);
        var options = this.options.sequencedEntrySet();
        if (!options.isEmpty()) {
            builder.append(" ");
            builder.append("[");
            var optionsToString = options.stream()
                    .map(entry -> entry.getValue().toString())
                    .collect(Collectors.joining(", "));
            builder.append(optionsToString);
            builder.append("]");
        }
        builder.append(";");
        return builder.toString();
    }
}
