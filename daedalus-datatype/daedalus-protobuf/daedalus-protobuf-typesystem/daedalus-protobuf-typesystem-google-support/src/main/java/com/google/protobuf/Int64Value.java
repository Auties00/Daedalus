package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code int64}.
 *
 * <p>The JSON representation for {@code Int64Value} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.Int64Value}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Int64Value {

    /**
     * The int64 value.
     *
     * <p><code>int64 value = 1;</code>
     */
    @ProtobufMessage.Int64Field(index = 1)
    long value;

    /**
     * Constructs a new {@code Int64Value} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    Int64Value(long value) {
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
    public long value() {
        return value;
    }
}
