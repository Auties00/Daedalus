package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtosServiceDescriptorProtoBuilder;
import com.google.protobuf.DescriptorProtosServiceOptionsBuilder;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;

import java.util.Objects;

/**
 * Represents a service declaration in the Protocol Buffer AST.
 * <p>
 * Services define RPC (Remote Procedure Call) interfaces in Protocol Buffers. A service contains
 * a collection of method declarations that specify the request and response message types. Services
 * are used by RPC frameworks like gRPC to generate client and server code.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Simple service with unary RPC
 * service UserService {
 *   rpc GetUser(GetUserRequest) returns (GetUserResponse);
 *   rpc CreateUser(CreateUserRequest) returns (CreateUserResponse);
 * }
 *
 * // Service with streaming RPCs
 * service ChatService {
 *   // Server streaming
 *   rpc StreamMessages(StreamRequest) returns (stream Message);
 *
 *   // Client streaming
 *   rpc UploadFile(stream Chunk) returns (UploadResponse);
 *
 *   // Bidirectional streaming
 *   rpc Chat(stream ChatMessage) returns (stream ChatMessage);
 * }
 *
 * // Service with options
 * service SearchService {
 *   option deprecated = true;
 *   rpc Search(SearchRequest) returns (SearchResponse) {
 *     option (google.api.http) = {
 *       post: "/v1/search"
 *       body: "*"
 *     };
 *   }
 * }
 * }</pre>
 * <p>
 * Services can contain various child elements:
 * </p>
 * <ul>
 *   <li><strong>Methods (RPCs):</strong> RPC method declarations with request/response types</li>
 *   <li><strong>Options:</strong> Configuration options for the service</li>
 *   <li><strong>Empty statements:</strong> Standalone semicolons</li>
 * </ul>
 * <p>
 * This class only implements {@link ProtobufDocumentChild}, meaning services can only appear at
 * the file level, not nested within other structures.
 * </p>
 * <p>
 * Services are attributed when they have a name and all their child statements (methods and options)
 * are attributed. During semantic analysis, method request and response types are resolved to their
 * message definitions.
 * </p>
 *
 * @see ProtobufServiceChild
 * @see ProtobufMethodStatement
 * @see ProtobufOptionStatement
 */
public final class ProtobufServiceStatement
        extends ProtobufStatementWithBodyImpl<ProtobufServiceChild>
        implements ProtobufStatement, ProtobufTree.WithName, ProtobufTree.WithBody<ProtobufServiceChild>,
                   ProtobufDocumentChild, ProtobufTree.WithDescriptor {
    private String name;

    /**
     * Constructs a new service statement covering the specified source range.
     *
     * @param document the enclosing document
     * @param range the source range covering this statement
     */
    public ProtobufServiceStatement(ProtobufDocumentTree document, ProtobufTreeSourceRange range) {
        super(document, range);
    }

    @Override
    public DescriptorProtos.ServiceDescriptorProto toDescriptor() {
        var methods = getDirectChildrenByType(ProtobufMethodStatement.class)
                .map(ProtobufMethodStatement::toDescriptor)
                .toList();
        var builder = new DescriptorProtosServiceDescriptorProtoBuilder()
                .name(name())
                .method(methods);
        var serviceOptions = buildServiceOptions();
        if (serviceOptions != null) {
            builder.options(serviceOptions);
        }
        return builder.build();
    }

    private DescriptorProtosServiceOptionsBuilder buildServiceOptions() {
        var optionStatements = getDirectChildrenByType(ProtobufOptionStatement.class).toList();
        if (optionStatements.isEmpty()) {
            return null;
        }
        var builder = new DescriptorProtosServiceOptionsBuilder();
        var applied = false;
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
                case "deprecated" -> {
                    if (value instanceof ProtobufBoolExpression(var deprecated)) {
                        builder.deprecated(deprecated);
                        applied = true;
                    }
                }
                default -> {
                    // Unknown or unsupported service option
                }
            }
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
        // uninterpreted_option (999) is not derivable from the AST
        return applied ? builder : null;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        builder.append("service");
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
     * Returns the service name.
     *
     * @return the service name, or null if not yet set
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether this service has a name assigned.
     *
     * @return true if a name is present, false otherwise
     */
    @Override
    public boolean hasName() {
        return name != null;
    }

    /**
     * Sets the name for this service.
     * <p>
     * The name must be a valid Protocol Buffer identifier and unique within the file.
     * </p>
     *
     * @param name the service name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
}
