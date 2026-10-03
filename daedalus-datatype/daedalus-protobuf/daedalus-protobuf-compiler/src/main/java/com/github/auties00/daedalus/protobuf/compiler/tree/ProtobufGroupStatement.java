package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufIntegerRange;
import com.google.protobuf.*;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufOptionExpression;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufGroupTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a group field declaration in the Protocol Buffer AST.
 * <p>
 * Groups are a deprecated proto2 feature that combines a nested message type definition with
 * a field declaration. The group's name defines both the field name (lowercased) and the
 * message type name (capitalized). Groups were deprecated in favor of nested message types.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * message SearchResponse {
 *   // Deprecated group syntax
 *   repeated group Result = 1 {
 *     required string url = 2;
 *     optional string title = 3;
 *     repeated string snippets = 4;
 *   }
 * }
 *
 * // The above is equivalent to the modern nested message approach:
 * message SearchResponse {
 *   message Result {
 *     required string url = 2;
 *     optional string title = 3;
 *     repeated string snippets = 4;
 *   }
 *   repeated Result result = 1;
 * }
 * }</pre>
 * <p>
 * Group characteristics:
 * </p>
 * <ul>
 *   <li>Groups define both a message type and a field</li>
 *   <li>The group name must start with a capital letter (message type convention)</li>
 *   <li>The field name is the lowercased version of the group name</li>
 *   <li>Groups can have modifiers (optional, required, repeated)</li>
 *   <li>Groups have a field number like regular fields</li>
 *   <li>Groups use a different wire format encoding than nested messages</li>
 * </ul>
 * <p>
 * Groups can contain the same child elements as messages:
 * </p>
 * <ul>
 *   <li><strong>Fields:</strong> Data members of the group</li>
 *   <li><strong>Nested messages:</strong> Additional message definitions</li>
 *   <li><strong>Nested enums:</strong> Enum definitions</li>
 *   <li><strong>Options:</strong> Configuration options</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * This class extends {@link ProtobufFieldStatement} and maintains its own body of child statements.
 * The type is automatically set to a {@link ProtobufGroupTypeReference} and cannot be changed.
 * </p>
 * <p>
 * <strong>Note:</strong> Groups are deprecated and should not be used in new .proto files.
 * Use nested message types instead.
 * </p>
 *
 * @see ProtobufGroupChild
 * @see ProtobufFieldStatement
 * @see ProtobufGroupTypeReference
 */
public final class ProtobufGroupStatement
        extends ProtobufStatementImpl
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithIndex, ProtobufTree.WithOptions, ProtobufTree.WithBody<ProtobufGroupChild>, ProtobufTree.WithType, ProtobufTree.WithModifier, ProtobufOptionDefinition,
                   ProtobufMessageChild, ProtobufOneofChild, ProtobufGroupChild, ProtobufExtendChild, ProtobufTree.WithDescriptor {
    private final ProtobufDocumentTree document;
    private ProtobufModifier modifier;
    private ProtobufTypeReference type;
    private String name;
    private ProtobufInteger index;
    private final List<ProtobufGroupChild> children;
    private final ProtobufGroupTypeReference reference;
    private final SequencedMap<String, ProtobufOptionExpression> options;

    /**
     * Constructs a new group field statement covering the specified source range.
     *
     * @param document the enclosing document
     * @param range the source range covering this statement
     */
    public ProtobufGroupStatement(ProtobufDocumentTree document, ProtobufTreeSourceRange range) {
        super(range);
        this.document = document;
        this.children = new ArrayList<>();
        this.reference = new ProtobufGroupTypeReference(this);
        this.options = new LinkedHashMap<>();
    }

    /**
     * Returns the {@link ProtobufDocumentTree} that encloses this group.
     *
     * @return the enclosing document
     */
    @Override
    public ProtobufDocumentTree document() {
        return document;
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
                .flatMap(extensions -> extensions.expressions().stream())
                .forEachOrdered(extensionsExpression -> {
                    switch (extensionsExpression) {
                        case ProtobufIntegerExpression integerExpression -> integerExpression.value()
                                .toMessageFieldIndex()
                                .ifPresent(index -> {
                                    var range = new DescriptorProtoDescriptorProtosExtensionRangeBuilder()
                                            .start(index)
                                            .end(index + 1)
                                            .build();
                                    extensionRanges.add(range);
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
                                var range = new DescriptorProtoDescriptorProtosExtensionRangeBuilder()
                                        .start(min.getAsInt())
                                        .end(max.getAsInt() + 1)
                                        .build();
                                extensionRanges.add(range);
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
        var messageOptions = buildGroupMessageOptions();
        if (messageOptions != null) {
            builder.options(messageOptions);
        }
        return builder.build();
    }

    private DescriptorProtosMessageOptionsBuilder buildGroupMessageOptions() {
        if (options.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosMessageOptionsBuilder();
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
                case "message_set_wire_format" -> {
                    if (value instanceof ProtobufBoolExpression(var messageSetWireFormat)) {
                        builder.messageSetWireFormat(messageSetWireFormat);
                    }
                }
                case "no_standard_descriptor_accessor" -> {
                    if (value instanceof ProtobufBoolExpression(var noStandaredDescriptorAccessor)) {
                        builder.noStandardDescriptorAccessor(noStandaredDescriptorAccessor);
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
                    // Unknown or unsupported group option
                }
            }
        }
        return builder;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        if(modifier != null) {
            builder.append(modifier);
            builder.append(" ");
        }

        builder.append("group");
        builder.append(" ");

        var name = Objects.requireNonNullElse(this.name, "[missing]");
        builder.append(name);
        builder.append(" ");

        var index = Objects.requireNonNullElse(this.index, "[missing]");
        builder.append("=");
        builder.append(" ");
        builder.append(index);
        builder.append(" ");

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

    @Override
    public ProtobufModifier modifier() {
        return modifier;
    }

    @Override
    public boolean hasModifier() {
        return modifier != null;
    }

    @Override
    public void setModifier(ProtobufModifier modifier) {
        this.modifier = modifier;
    }

    /**
     * Returns the type reference for this group field.
     * <p>
     * Group fields have an implicit type that references the group itself as a message type.
     * </p>
     *
     * @return a {@link ProtobufGroupTypeReference} referencing this group
     */
    @Override
    public ProtobufTypeReference type() {
        return reference;
    }

    @Override
    public boolean hasType() {
        return true;
    }

    /**
     * Setting the type of a group field is not supported.
     * <p>
     * Groups have an implicit type that cannot be changed.
     * </p>
     *
     * @param type the type reference (ignored)
     */
    @Override
    public void setType(ProtobufTypeReference type) {

    }

    @Override
    public SequencedCollection<ProtobufGroupChild> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void addChild(ProtobufGroupChild statement) {
        children.add(statement);
        if(statement instanceof ProtobufStatementImpl mutableTree) {
            mutableTree.setParent(this);
        }
    }

    @Override
    public boolean removeChild(ProtobufGroupChild statement) {
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

    @Override
    public Optional<WithIndex> getDirectChildByIndex(long index) {
        return ProtobufStatementWithBodyImpl.getDirectChildByIndex(children, index);
    }

    @Override
    public <V extends ProtobufTree> Optional<V> getDirectChildByNameAndType(String name, Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getDirectChildByNameAndType(children, name, clazz);
    }

    @Override
    public <V extends ProtobufTree> Optional<V> getDirectChildByIndexAndType(long index, Class<V> clazz) {
        return ProtobufStatementWithBodyImpl.getDirectChildByIndexAndType(children, index, clazz);
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
        return hasType() && hasName() && hasIndex();
    }
}
