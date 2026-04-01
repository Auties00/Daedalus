package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

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
@ProtobufMessage
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

    Option(String name, Any value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public Any value() {
        return value;
    }
}
