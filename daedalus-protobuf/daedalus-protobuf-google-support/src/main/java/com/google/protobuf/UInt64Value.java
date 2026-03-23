package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code uint64}.
 *
 * <p>The JSON representation for {@code UInt64Value} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.UInt64Value}
 */
@ProtobufMessage
public final class UInt64Value {

    /**
     * The uint64 value.
     *
     * <p><code>uint64 value = 1;</code>
     */
    @ProtobufMessage.Uint64Field(index = 1)
    long value;

    UInt64Value(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }
}
