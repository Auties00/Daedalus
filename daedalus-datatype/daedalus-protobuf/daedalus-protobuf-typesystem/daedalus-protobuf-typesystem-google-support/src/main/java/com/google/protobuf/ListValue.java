package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Represents a JSON array.
 *
 * <p>Protobuf type {@code com.google.protobuf.ListValue}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class ListValue implements Value.Kind {

    /**
     * Repeated field of dynamically typed values.
     *
     * <p><code>repeated Value values = 1;</code>
     */
    @ProtobufMessage.MessageField(index = 1)
    List<Value> values;

    /**
     * Constructs a new {@code ListValue} with the specified field values.
     *
     * @param values the values of the repeated {@code values} field
     */
    ListValue(List<Value> values) {
        this.values = values;
    }

    /**
     * Returns an unmodifiable view of the values of the repeated
     * {@code values} field.
     *
     * @return an unmodifiable view of the values of the {@code values} field,
     *         in encounter order
     */
    public SequencedCollection<Value> values() {
        return Collections.unmodifiableSequencedCollection(values);
    }
}
