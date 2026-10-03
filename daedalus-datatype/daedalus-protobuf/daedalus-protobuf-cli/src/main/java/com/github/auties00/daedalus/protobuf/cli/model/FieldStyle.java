package com.github.auties00.daedalus.protobuf.cli.model;

/**
 * Specifies how protobuf fields are declared in the generated Java type.
 */
public enum FieldStyle {
    /**
     * Private final fields with explicit accessor methods
     * annotated with {@code @ProtobufAccessor}.
     */
    PRIVATE_WITH_ACCESSORS,

    /**
     * Record components annotated directly with a field annotation, such as
     * {@code @ProtobufMessage.Int32Field}.
     */
    RECORD_COMPONENTS,

    /**
     * Abstract interface methods annotated with a field annotation, such as
     * {@code @ProtobufMessage.Int32Field}.
     */
    INTERFACE_METHODS
}
