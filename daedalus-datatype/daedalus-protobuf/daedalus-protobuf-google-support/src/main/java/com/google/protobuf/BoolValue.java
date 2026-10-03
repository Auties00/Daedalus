package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code bool}.
 *
 * <p>The JSON representation for {@code BoolValue} is JSON {@code true} and {@code false}.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.BoolValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class BoolValue {

    /**
     * The bool value.
     *
     * <p><code>bool value = 1;</code>
     */
    @ProtobufMessage.BoolField(index = 1)
    boolean value;

    /**
     * Constructs a new {@code BoolValue} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    BoolValue(boolean value) {
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
    public boolean value() {
        return value;
    }
}
