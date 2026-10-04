package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Wrapper message for {@code string}.
 *
 * <p>The JSON representation for {@code StringValue} is JSON string.
 *
 * <p>Not recommended for use in new APIs, but still useful for legacy APIs and
 * has no plan to be removed.
 *
 * <p>Protobuf type {@code com.google.protobuf.StringValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class StringValue {

    /**
     * The string value.
     *
     * <p><code>string value = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String value;

    /**
     * Constructs a new {@code StringValue} with the specified field values.
     *
     * @param value the value of the {@code value} field
     */
    StringValue(String value) {
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
    public String value() {
        return value;
    }
}
