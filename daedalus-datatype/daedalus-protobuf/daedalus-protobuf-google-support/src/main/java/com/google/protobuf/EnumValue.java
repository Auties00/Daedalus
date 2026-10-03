package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Enum value definition.
 *
 * <p>New usages of this message as an alternative to EnumValueDescriptorProto are
 * strongly discouraged. This message does not reliability preserve all
 * information necessary to model the schema and preserve semantics. Instead
 * make use of FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.EnumValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class EnumValue {

    /**
     * Enum value name.
     *
     * <p><code>string name = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String name;

    /**
     * Enum value number.
     *
     * <p><code>int32 number = 2;</code>
     */
    @ProtobufMessage.Int32Field(index = 2)
    int number;

    /**
     * Protocol buffer options.
     *
     * <p><code>repeated Option options = 3;</code>
     */
    @ProtobufMessage.MessageField(index = 3)
    List<Option> options;

    /**
     * Constructs a new {@code EnumValue} with the specified field values.
     *
     * @param name the value of the {@code name} field
     * @param number the value of the {@code number} field
     * @param options the values of the repeated {@code options} field
     */
    EnumValue(String name, int number, List<Option> options) {
        this.name = name;
        this.number = number;
        this.options = options;
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
     * Returns the value of the {@code number} field.
     *
     * <p>This field has implicit presence, so an unset value is
     * indistinguishable from the default value of its type.
     *
     * @return the value of the {@code number} field
     */
    public int number() {
        return number;
    }

    /**
     * Returns an unmodifiable view of the values of the repeated
     * {@code options} field.
     *
     * @return an unmodifiable view of the values of the {@code options} field,
     *         in encounter order
     */
    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }
}
