package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosOneofDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosOneofOptionsBuilder;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a oneof declaration in the Protocol Buffer AST.
 * <p>
 * A oneof is a group of fields where at most one field can be set at any given time. Setting
 * any member of the oneof automatically clears all other members. Oneofs are useful for
 * representing variants or union types.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * message SampleMessage {
 *   oneof test_oneof {
 *     string name = 4;
 *     int32 age = 9;
 *   }
 * }
 *
 * message Result {
 *   oneof result {
 *     string success = 1;
 *     string error = 2;
 *   }
 * }
 *
 * message SearchQuery {
 *   oneof query_type {
 *     string text = 1;
 *     int32 id = 2;
 *     Message nested = 3;
 *   }
 * }
 * }</pre>
 * <p>
 * Oneof restrictions and requirements:
 * </p>
 * <ul>
 *   <li>Oneof fields cannot use the {@code required}, {@code optional}, or {@code repeated} modifiers</li>
 *   <li>Oneof fields cannot be maps</li>
 *   <li>Field numbers must still be unique within the containing message</li>
 *   <li>Only regular fields and groups can appear within a oneof (no nested messages/enums)</li>
 * </ul>
 * <p>
 * Oneofs can contain:
 * </p>
 * <ul>
 *   <li><strong>Fields:</strong> Regular field declarations (the oneof members)</li>
 *   <li><strong>Options:</strong> Configuration options for the oneof</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * This class extends {@link ProtobufFieldStatement} but does not use the type, index, or modifier
 * fields from the parent. It maintains its own list of child statements representing the oneof members.
 * </p>
 *
 * @see ProtobufOneofChild
 * @see ProtobufFieldStatement
 * @see ProtobufMessageChild
 */

public final class ProtobufOneofStatement
        extends ProtobufStatementImpl
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithBody<ProtobufOneofChild>,
                   ProtobufMessageChild, ProtobufGroupChild, ProtobufTree.WithDescriptor {
    private final ProtobufDocumentTree document;
    private String name;
    private final List<ProtobufOneofChild> children;

    /**
     * Constructs a new oneof statement covering the specified source range.
     *
     * @param document the enclosing document
     * @param range the source range covering this statement
     */
    public ProtobufOneofStatement(ProtobufDocumentTree document, ProtobufTreeSourceRange range) {
        super(range);
        this.document = document;
        this.children = new ArrayList<>();
    }

    /**
     * Returns the {@link ProtobufDocumentTree} that encloses this oneof.
     *
     * @return the enclosing document
     */
    @Override
    public ProtobufDocumentTree document() {
        return document;
    }

    /**
     * Returns the generated class name for this oneof.
     * <p>
     * Generates a class name by capitalizing the first letter of the oneof name
     * and appending "Seal" (e.g., "result" becomes "ResultSeal").
     * </p>
     *
     * @return the generated class name
     */
    public String className() {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1) + "Seal";
    }

    /**
     * Returns the oneof name.
     *
     * @return the oneof name, or null if not yet set
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether this oneof has a name assigned.
     *
     * @return true if a name is present, false otherwise
     */
    @Override
    public boolean hasName() {
        return name != null;
    }

    @Override
    public boolean isAttributed() {
        return hasName() && children.stream().allMatch(ProtobufOneofChild::isAttributed);
    }

    /**
     * Sets the name for this oneof.
     * <p>
     * The name must be a valid Protocol Buffer identifier and unique within the parent scope.
     * </p>
     *
     * @param name the oneof name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public DescriptorProtos.OneofDescriptorProto toDescriptor() {
        var builder = new DescriptorProtosOneofDescriptorProtoBuilder()
                .name(name());
        var oneofOptions = buildOneofOptions();
        if (oneofOptions != null) {
            builder.options(oneofOptions);
        }
        return builder.build();
    }

    /**
     * Builds the {@link DescriptorProtos.OneofOptions} sub message for this oneof.
     *
     * <p>The {@code OneofOptions} message defined in {@code descriptor.proto} contains
     * only three kinds of fields, none of which can be derived from a typical
     * {@link ProtobufOptionStatement}:
     * <ul>
     * <li>{@code features} (field 1, {@code FeatureSet}): a structured sub message
     *     that cannot be populated from a flat single segment option.
     * <li>{@code uninterpreted_option} (field 999): reserved for unrecognized options
     *     kept by the parser; this compiler does not propagate uninterpreted entries.
     * <li>Custom user extensions (fields 1000 to max): user defined extensions that
     *     are out of scope for the standard descriptor mapping.
     * </ul>
     *
     * <p>As a result, there are no scalar or enum fields on {@code OneofOptions} that
     * map cleanly from the AST, so this helper never produces a populated options
     * instance. It returns {@code null} whenever no recognized option could be
     * applied, which mirrors the {@code applied} flag pattern used by sibling
     * descriptor builders, ensuring the enclosing descriptor does not attach an
     * empty options sub message.
     *
     * @return the builder of a populated {@link DescriptorProtos.OneofOptions}, or
     *         {@code null} if no recognized options were applied
     */
    private DescriptorProtosOneofOptionsBuilder buildOneofOptions() {
        var optionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (optionStatements.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosOneofOptionsBuilder();
        var applied = false;
        for (var option : optionStatements) {
            if (!option.hasName() || !option.hasValue() || option.name().segments().size() != 1) {
                continue;
            }
            var segment = option.name().segments().getFirst();
            if (segment.isExtension()) {
                continue;
            }
            // OneofOptions has no scalar or enum fields that are directly mappable
            // from a single segment AST option. The only declared fields are
            // features (FeatureSet), uninterpreted_option, and custom extensions.
            // The features field is populated below from multi segment options.
        }
        var features = ProtobufTreeFeatures.featureSet(this);
        if (features != null) {
            builder.features(features);
            applied = true;
        }
        var unknownExtensions = ProtobufTreeFeatures.collectUnknownExtensions(this);
        if (!unknownExtensions.isEmpty()) {
            builder.unknownFields(unknownExtensions);
            applied = true;
        }
        return applied ? builder : null;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        builder.append("oneof");
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

    @Override
    public SequencedCollection<ProtobufOneofChild> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void addChild(ProtobufOneofChild statement) {
        children.add(statement);
        if(statement instanceof ProtobufStatementImpl mutableTree) {
            mutableTree.setParent(this);
        }
    }

    @Override
    public boolean removeChild(ProtobufOneofChild statement) {
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
}
