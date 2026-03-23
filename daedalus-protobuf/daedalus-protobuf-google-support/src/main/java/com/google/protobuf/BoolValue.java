package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code bool}.
 *
 * <p>The JSON representation for {@code BoolValue} is JSON {@code true} and {@code false}.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.BoolValue}
 */
@ProtobufMessage
public final class BoolValue {

    /**
     * The bool value.
     *
     * <p><code>bool value = 1;</code>
     */
    @ProtobufMessage.BoolField(index = 1)
    boolean value;

    BoolValue(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }
}
