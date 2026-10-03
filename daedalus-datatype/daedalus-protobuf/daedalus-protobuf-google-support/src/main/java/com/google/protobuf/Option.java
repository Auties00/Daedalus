package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import java.util.Optional;

/**
 * A protocol buffer option, which can be attached to a message, field,
 * enumeration, etc.
 *
 * <p>New usages of this message as an alternative to FileOptions, MessageOptions,
 * FieldOptions, EnumOptions, EnumValueOptions, ServiceOptions, or MethodOptions
 * are strongly discouraged.
 *
 * <p>Protobuf type {@code com.google.protobuf.Option}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Option {

    /**
     * The option's name. For protobuf built-in options (options defined in
     * descriptor.proto), this is the short name. For example, {@code "map_entry"}.
     * For custom options, it should be the fully-qualified name. For example,
     * {@code "google.api.http"}.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * The option's value packed in an Any message. If the value is a primitive,
     * the corresponding wrapper type defined in google/protobuf/wrappers.proto
     * should be used. If the value is an enum, it should be stored as an int32
     * value using the com.google.protobuf.Int32Value type.
     *
     * <p><code>Any value = 2;</code>
     */
    @ProtobufMessage.MessageField(index = 2)
    Any value;

    /**
     * Constructs a new {@code Option} with the specified field values.
     *
     * @param name the value of the {@code name} field
     * @param value the value of the {@code value} field, or {@code null} if the
     *              field is not set
     */
    Option(String name, Any value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the value of the {@code name} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code name} field
     */
    public String name() {
        return name;
    }

    /**
     * Returns the value of the {@code value} field, if present.
     *
     * @return an {@code Optional} containing the value of the {@code value}
     *         field, or an empty {@code Optional} if the field is not set
     */
    public Optional<Any> value() {
        return Optional.ofNullable(value);
    }
}
