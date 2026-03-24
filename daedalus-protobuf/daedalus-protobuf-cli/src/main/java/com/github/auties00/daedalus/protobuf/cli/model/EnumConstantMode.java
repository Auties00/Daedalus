package com.github.auties00.daedalus.protobuf.cli.model;

/**
 * Specifies how protobuf enum constants are mapped to their integer index in Java.
 */
public enum EnumConstantMode {
    /**
     * Each constant is annotated with {@code @ProtobufEnum.Constant(index = N)}.
     */
    CONSTANT_ANNOTATION,

    /**
     * The type defines {@code @ProtobufSerializer} and {@code @ProtobufDeserializer}
     * methods for custom conversion between the type and its integer index.
     */
    SERIALIZER_DESERIALIZER
}
