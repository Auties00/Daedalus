package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code int32}.
 *
 * <p>The JSON representation for {@code Int32Value} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.Int32Value}
 */
@ProtobufMessage
public final class Int32Value {

    /**
     * The int32 value.
     *
     * <p><code>int32 value = 1;</code>
     */
    @ProtobufMessage.Int32Field(index = 1)
    int value;

    Int32Value(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
