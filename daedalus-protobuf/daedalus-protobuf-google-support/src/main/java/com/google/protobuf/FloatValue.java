package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code float}.
 *
 * <p>The JSON representation for {@code FloatValue} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.FloatValue}
 */
@ProtobufMessage
public final class FloatValue {

    /**
     * The float value.
     *
     * <p><code>float value = 1;</code>
     */
    @ProtobufMessage.FloatField(index = 1)
    float value;

    FloatValue(float value) {
        this.value = value;
    }

    public float value() {
        return value;
    }
}
