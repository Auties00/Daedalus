package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code uint32}.
 *
 * <p>The JSON representation for {@code UInt32Value} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.UInt32Value}
 */
@ProtobufMessage
public final class UInt32Value {

    /**
     * The uint32 value.
     *
     * <p><code>uint32 value = 1;</code>
     */
    @ProtobufMessage.Uint32Field(index = 1)
    int value;

    UInt32Value(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
