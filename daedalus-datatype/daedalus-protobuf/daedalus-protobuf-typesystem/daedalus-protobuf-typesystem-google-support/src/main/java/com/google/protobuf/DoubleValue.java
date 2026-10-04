package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code double}.
 *
 * <p>The JSON representation for {@code DoubleValue} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.DoubleValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class DoubleValue {

    /**
     * The double value.
     *
     * <p><code>double value = 1;</code>
     */
    @ProtobufMessage.DoubleField(index = 1)
    double value;

    /**
     * Constructs a new {@code DoubleValue} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    DoubleValue(double value) {
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
    public double value() {
        return value;
    }
}
