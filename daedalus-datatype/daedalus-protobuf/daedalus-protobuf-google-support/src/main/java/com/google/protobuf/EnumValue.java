package com.google.protobuf;

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
@ProtobufMessage
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

    EnumValue(String name, int number, List<Option> options) {
        this.name = name;
        this.number = number;
        this.options = options;
    }

    public String name() {
        return name;
    }

    public int number() {
        return number;
    }

    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }
}
