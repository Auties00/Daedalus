package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code float}.
 *
 * <p>The JSON representation for {@code FloatValue} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.FloatValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class FloatValue {

    /**
     * The float value.
     *
     * <p><code>float value = 1;</code>
     */
    @ProtobufMessage.FloatField(index = 1)
    float value;

    /**
     * Constructs a new {@code FloatValue} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    FloatValue(float value) {
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
    public float value() {
        return value;
    }
}
