package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtoBuilder;
import com.github.auties00.daedalus.protobuf.model.ProtobufJsonCompatibility;

import java.util.Objects;

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
     * Constructs a new message statement at the specified line number.
     *
     * @param line the line number in the source file
     */
    public ProtobufMessageStatement(int line) {
        super(line);
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
        return new DescriptorProtoBuilder()
                .name(name())
                .field(fields)
                .nestedType(nestedMessages)
                .enumType(nestedEnums)
                .oneofDecl(oneofs)
                .build();
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
