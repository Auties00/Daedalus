package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code double}.
 *
 * <p>The JSON representation for {@code DoubleValue} is JSON number.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.DoubleValue}
 */
@ProtobufMessage
public final class DoubleValue {

    /**
     * The double value.
     *
     * <p><code>double value = 1;</code>
     */
    @ProtobufMessage.DoubleField(index = 1)
    double value;

    DoubleValue(double value) {
        this.value = value;
    }

    public double value() {
        return value;
    }
}
