package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import java.util.Map;

/**
 * Represents a JSON object.
 *
 * <p>An unordered key-value map, intending to perfectly capture the semantics of a
 * JSON object. This enables parsing any arbitrary JSON payload as a message
 * field in ProtoJSON format.
 *
 * <p>This follows RFC 8259 guidelines for interoperable JSON: notably this type
 * cannot represent large Int64 values or {@code NaN}/{@code Infinity} numbers,
 * since the JSON format generally does not support those values in its number
 * type.
 *
 * <p>If you do not intend to parse arbitrary JSON into your message, a custom
 * typed message should be preferred instead of using this type.
 *
 * <p>Protobuf type {@code google.protobuf.Struct}
 */
@ProtobufMessage
public final class Struct {

    /**
     * Unordered map of dynamically typed values.
     *
     * <p><code>map&lt;string, Value&gt; fields = 1;</code>
     */
    @ProtobufMessage.MapField(index = 1, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    Map<String, Value> fields;

    Struct(Map<String, Value> fields) {
        this.fields = fields;
    }

    public Map<String, Value> fields() {
        return fields;
    }
}
