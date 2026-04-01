package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;

/**
 * The syntax in which a protocol buffer element is defined.
 *
 * <p>Protobuf enum {@code com.google.protobuf.Syntax}
 */
@ProtobufEnum
public enum Syntax {
    /**
     * Syntax {@code proto2}.
     */
    @ProtobufEnum.Constant(index = 0)
    SYNTAX_PROTO2,

    /**
     * Syntax {@code proto3}.
     */
    @ProtobufEnum.Constant(index = 1)
    SYNTAX_PROTO3,

    /**
     * Syntax {@code editions}.
     */
    @ProtobufEnum.Constant(index = 2)
    SYNTAX_EDITIONS;
}
