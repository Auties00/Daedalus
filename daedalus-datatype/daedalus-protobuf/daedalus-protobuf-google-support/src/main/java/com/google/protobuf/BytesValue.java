package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
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
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class BytesValue {

    /**
     * The bytes value.
     *
     * <p><code>bytes value = 1;</code>
     */
    @ProtobufMessage.BytesField(index = 1)
    byte[] value;

    /**
     * Constructs a new {@code BytesValue} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    BytesValue(byte[] value) {
        this.value = value;
    }

    /**
     * Returns a copy of the value of the {@code value} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return a copy of the value of the {@code value} field
     */
    public byte[] value() {
        return value.clone();
    }
}
