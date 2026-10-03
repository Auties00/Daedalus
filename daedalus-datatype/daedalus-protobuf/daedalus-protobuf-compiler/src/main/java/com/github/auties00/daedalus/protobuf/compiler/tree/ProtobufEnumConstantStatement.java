package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosEnumValueDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosEnumValueOptionsBuilder;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufOptionExpression;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an enum constant (value) declaration in the Protocol Buffer AST.
 * <p>
 * Enum constants are the named integer values that comprise an enum type. Each constant
 * has a name and an associated numeric value. Multiple constants can have the same numeric
 * value when the {@code allow_alias} option is enabled.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * enum Status {
 *   UNKNOWN = 0;
 *   PENDING = 1;
 *   APPROVED = 2;
 *   REJECTED = 3;
 * }
 *
 * // With options
 * enum Priority {
 *   LOW = 1 [deprecated = true];
 *   MEDIUM = 2;
 *   HIGH = 3;
 * }
 *
 * // With aliases
 * enum Result {
 *   option allow_alias = true;
 *   SUCCESS = 0;
 *   OK = 0;  // Alias for SUCCESS
 * }
 * }</pre>
 * <p>
 * Important requirements for enum constants:
 * </p>
 * <ul>
 *   <li>In proto3, the first enum value must be zero for default value semantics</li>
 *   <li>Constant names must be unique within the enum</li>
 *   <li>Numeric values must be unique unless {@code allow_alias = true}</li>
 *   <li>Values must fit within the 32-bit integer range</li>
 * </ul>
 * <p>
 * This class extends {@link ProtobufFieldStatement} but reuses only the name, index (numeric value),
 * and options fields. The type and modifier fields are not used for enum constants.
 * </p>
 *
 * @see ProtobufEnumChild
 * @see ProtobufEnumStatement
 * @see ProtobufFieldStatement
 */
public final class ProtobufEnumConstantStatement
        extends ProtobufStatementImpl
        implements ProtobufTree.WithName, ProtobufTree.WithIndex, ProtobufTree.WithOptions,
                   ProtobufEnumChild, ProtobufTree.WithDescriptor {
    private String name;
    private ProtobufInteger index;
    private final SequencedMap<String, ProtobufOptionExpression> options;

    /**
     * Constructs a new enum constant statement covering the specified source range.
     *
     * @param range the source range covering this statement
     */
    public ProtobufEnumConstantStatement(ProtobufTreeSourceRange range) {
        super(range);
        this.options = new LinkedHashMap<>();
    }

    @Override
    public DescriptorProtos.EnumValueDescriptorProto toDescriptor() {
        var builder = new DescriptorProtosEnumValueDescriptorProtoBuilder()
                .name(name());

        index.toEnumConstantIndex()
                .ifPresent(builder::number);

        var enumValueOptions = buildEnumValueOptions();
        if (enumValueOptions != null) {
            builder.options(enumValueOptions);
        }

        return builder.build();
    }

    private DescriptorProtosEnumValueOptionsBuilder buildEnumValueOptions() {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosEnumValueOptionsBuilder();
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
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                    }
                }
                case "debug_redact" -> {
                    if (value instanceof ProtobufBoolExpression(var debugRedact)) {
                        builder.debugRedact(debugRedact);
                    }
                }
                default -> {
                    // Unknown or unsupported enum value option.
                    // The following EnumValueOptions fields cannot be derived from the AST:
                    //   feature_support (4): FieldOptions.FeatureSupport aggregate with typed sub fields
                    //   uninterpreted_option (999): reserved for the descriptor pipeline, not set here
                    // The features field (2) is populated below from multi segment options.
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
        var name = Objects.requireNonNullElse(this.name, "[missing]");
        builder.append(name);
        builder.append(" = ");
        var index = Objects.requireNonNull(this.index, "[missing]");
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

    @Override
    public ProtobufInteger index() {
        return index;
    }

    @Override
    public boolean hasIndex() {
        return index != null;
    }

    @Override
    public void setIndex(ProtobufInteger index) {
        this.index = index;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean hasName() {
        return name != null;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public SequencedCollection<ProtobufOptionExpression> options() {
        return Collections.unmodifiableSequencedCollection(options.sequencedValues());
    }

    @Override
    public Optional<ProtobufOptionExpression> getOption(String name) {
        return Optional.ofNullable(options.get(name));
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
    public boolean isAttributed() {
        return hasName() && hasIndex();
    }
}
