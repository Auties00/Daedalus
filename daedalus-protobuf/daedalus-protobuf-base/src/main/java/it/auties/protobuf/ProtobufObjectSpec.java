
package it.auties.protobuf;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.model.ProtobufUnknownValue;
import it.auties.protobuf.io.reader.ProtobufBinaryReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for decoding Protocol Buffer messages into a generic map structure.
 * <p>
 * This class provides methods to decode Protocol Buffer encoded data without requiring
 * a specific message schema or compiled protobuf classes. The decoded data is returned
 * as a map where keys are field numbers and values are the decoded field values.
 * <p>
 * This is particularly useful for:
 * <ul>
 *   <li>Inspecting unknown or dynamic protobuf messages</li>
 *   <li>Debugging protobuf data without access to the original .proto definition</li>
 *   <li>Generic protobuf message processing</li>
 * </ul>
 * <p>
 *     <font color="red">
 *         This class is extremely slow: use it only when necessary.
 *     </font>
 * <p>
 */
public final class ProtobufObjectSpec {
    // TODO: Add an encoder

    /**
     * Decodes a Protocol Buffer message from a ProtobufInputStream.
     * This method is extremely slow: use it only when deserializing dynamic messages.
     * <p>
     * This method reads from the provided reader and decodes all available
     * protobuf fields into a map structure. Each field is read according to its
     * wire type and stored with its field number as the key.
     * <p>
     *     <font color="red">
     *         This class is extremely slow: use it only when necessary.
     *     </font>
     *
     * @param reader the reader containing the encoded Protocol Buffer data
     * @return a map where keys are field numbers (Integer) and values are the decoded field values (Object).
     * @throws NullPointerException if the reader is null
     * @throws ProtobufDeserializationException if the data cannot be decoded
     *
     * @see ProtobufReader#readUnknownProperty()
     */
    public static Map<Long, ProtobufUnknownValue> decode(ProtobufBinaryReader reader) {
        Objects.requireNonNull(reader, "The reader cannot be null");
        var result = new HashMap<Long, ProtobufUnknownValue>();
        while (reader.readPropertyTag()) {
            var key = reader.propertyIndex();
            var value = decodeValue(reader);
            result.put(key, value);
        }
        return result;
    }

    private static ProtobufUnknownValue decodeValue(ProtobufBinaryReader reader) {
        var value = reader.readUnknownProperty();
        return switch (value) {
            case ProtobufUnknownValue.LengthDelimited.ByteArrayBacked(var bytes) -> {
                try { // Maybe it's an embedded message
                    yield decodeValue(ProtobufBinaryReader.fromBytes(bytes));
                }catch (ProtobufDeserializationException _) { // It wasn't an embedded message
                    yield value;
                }
            }

            case ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(var buffer) -> {
                var position = buffer.position();
                try { // Maybe it's an embedded message
                    yield decodeValue(ProtobufBinaryReader.fromBuffer(buffer));
                }catch (ProtobufDeserializationException _) { // It wasn't an embedded message
                    buffer.position(position); // Reset the position
                    yield value;
                }
            }

            case ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(var segment, var _) -> {
                try { // Maybe it's an embedded message
                    yield decodeValue(ProtobufBinaryReader.fromMemorySegment(segment));
                }catch (ProtobufDeserializationException _) { // It wasn't an embedded message
                    // No position to reset
                    yield value;
                }
            }

            case ProtobufUnknownValue.Fixed32 fixed32 -> fixed32;

            case ProtobufUnknownValue.Fixed64 fixed64 -> fixed64;

            case ProtobufUnknownValue.Group group -> group;

            case ProtobufUnknownValue.VarInt varInt -> varInt;
        };
    }
}