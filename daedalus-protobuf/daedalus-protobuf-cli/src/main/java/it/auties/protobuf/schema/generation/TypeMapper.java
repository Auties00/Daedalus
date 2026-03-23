package it.auties.protobuf.schema.generation;

import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.parser.typeReference.*;
import it.auties.protobuf.schema.config.SchemaConfig;

/**
 * Maps proto type references to Java type names and {@link ProtobufType} enum values.
 */
public final class TypeMapper {
    private final SchemaConfig config;
    private final NamingStrategy naming;

    /**
     * Constructs a type mapper with the given configuration and naming strategy.
     *
     * @param config the schema configuration for package resolution
     * @param naming the naming strategy for type name conversion
     */
    public TypeMapper(SchemaConfig config, NamingStrategy naming) {
        this.config = config;
        this.naming = naming;
    }

    /**
     * Returns the Java type name for a proto type reference.
     *
     * <p>For primitive types, returns the Java primitive or boxed type name.
     * For message, enum, and group types, resolves the Java class name via
     * the configured package mappings. For map types, returns
     * {@code Map<KeyType, ValueType>}.
     *
     * @param typeRef the proto type reference
     * @param boxed whether to use boxed types for primitives
     * @return the Java type name
     */
    public String toJavaType(ProtobufTypeReference typeRef, boolean boxed) {
        return switch (typeRef) {
            case ProtobufPrimitiveTypeReference primitive -> primitiveToJava(primitive.protobufType(), boxed);
            case ProtobufMapTypeReference map -> "Map<"
                    + toJavaType(map.keyType(), true) + ", "
                    + toJavaType(map.valueType(), true) + ">";
            case ProtobufMessageTypeReference msg -> resolveObjectTypeName(msg.declaration().name());
            case ProtobufEnumTypeReference enumRef -> resolveObjectTypeName(enumRef.declaration().name());
            case ProtobufGroupTypeReference group -> resolveObjectTypeName(group.declaration().name());
            case ProtobufObjectTypeReference other -> other.name();
        };
    }

    /**
     * Returns the {@link ProtobufType} enum name for a proto type reference.
     *
     * @param typeRef the proto type reference
     * @return the ProtobufType enum constant name (e.g., {@code "STRING"}, {@code "INT32"})
     */
    public String toProtobufTypeName(ProtobufTypeReference typeRef) {
        return typeRef.protobufType().name();
    }

    /**
     * Returns the {@link ProtobufType} enum name for a map key type.
     *
     * @param map the map type reference
     * @return the ProtobufType enum constant name for the key type
     */
    public String toMapKeyTypeName(ProtobufMapTypeReference map) {
        return map.keyType().protobufType().name();
    }

    /**
     * Returns the {@link ProtobufType} enum name for a map value type.
     *
     * @param map the map type reference
     * @return the ProtobufType enum constant name for the value type
     */
    public String toMapValueTypeName(ProtobufMapTypeReference map) {
        return map.valueType().protobufType().name();
    }

    private String primitiveToJava(ProtobufType type, boolean boxed) {
        return switch (type) {
            case FLOAT -> boxed ? "Float" : "float";
            case DOUBLE -> boxed ? "Double" : "double";
            case BOOL -> boxed ? "Boolean" : "boolean";
            case STRING -> "String";
            case BYTES -> "byte[]";
            case INT32, SINT32, SFIXED32, UINT32, FIXED32 -> boxed ? "Integer" : "int";
            case INT64, SINT64, SFIXED64, UINT64, FIXED64 -> boxed ? "Long" : "long";
            default -> "Object";
        };
    }

    private String resolveObjectTypeName(String protoName) {
        if (protoName == null) {
            return "Object";
        }
        var lastDot = protoName.lastIndexOf('.');
        if (lastDot < 0) {
            return naming.toTypeName(protoName);
        }
        return naming.toTypeName(protoName.substring(lastDot + 1));
    }
}
