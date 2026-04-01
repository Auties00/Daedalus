package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code bytes}.
 *
 * <p>The JSON representation for {@code BytesValue} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.BytesValue}
 */
@ProtobufMessage
public final class BytesValue {

    /**
     * The bytes value.
     *
     * <p><code>bytes value = 1;</code>
     */
    @ProtobufMessage.BytesField(index = 1)
    byte[] value;

    BytesValue(byte[] value) {
        this.value = value;
    }

    public byte[] value() {
        return value;
    }
}
