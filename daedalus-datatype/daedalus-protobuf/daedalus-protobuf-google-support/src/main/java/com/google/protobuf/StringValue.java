package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code string}.
 *
 * <p>The JSON representation for {@code StringValue} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code google.protobuf.StringValue}
 */
@ProtobufMessage
public final class StringValue {

    /**
     * The string value.
     *
     * <p><code>string value = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String value;

    StringValue(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
