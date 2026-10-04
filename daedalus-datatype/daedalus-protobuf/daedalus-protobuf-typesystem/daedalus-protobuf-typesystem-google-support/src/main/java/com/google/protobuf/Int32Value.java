package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code int32}.
 *
 * <p>The JSON representation for {@code Int32Value} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.Int32Value}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Int32Value {

    /**
     * The int32 value.
     *
     * <p><code>int32 value = 1;</code>
     */
    @ProtobufMessage.Int32Field(index = 1)
    int value;

    /**
     * Constructs a new {@code Int32Value} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    Int32Value(int value) {
        this.value = value;
    }

    /**
     * Returns the value of the {@code value} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code value} field
     */
    public int value() {
        return value;
    }
}
