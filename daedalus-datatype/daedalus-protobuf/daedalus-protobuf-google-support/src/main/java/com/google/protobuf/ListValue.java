package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Represents a JSON array.
 *
 * <p>Protobuf type {@code com.google.protobuf.ListValue}
 */
@ProtobufMessage
public final class ListValue {

    /**
     * Repeated field of dynamically typed values.
     *
     * <p><code>repeated Value values = 1;</code>
     */
    @ProtobufMessage.MessageField(index = 1)
    List<Value> values;

    ListValue(List<Value> values) {
        this.values = values;
    }

    public SequencedCollection<Value> values() {
        return Collections.unmodifiableSequencedCollection(values);
    }
}
