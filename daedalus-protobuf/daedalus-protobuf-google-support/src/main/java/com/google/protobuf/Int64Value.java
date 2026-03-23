package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code int64}.
 *
 * <p>The JSON representation for {@code Int64Value} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.Int64Value}
 */
@ProtobufMessage
public final class Int64Value {

    /**
     * The int64 value.
     *
     * <p><code>int64 value = 1;</code>
     */
    @ProtobufMessage.Int64Field(index = 1)
    long value;

    Int64Value(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }
}
