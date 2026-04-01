
package com.github.auties00.daedalus.protobuf;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.calculator.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for encoding and decoding Protocol Buffer messages into and
 * from a generic {@link Map Map&lt;Long, ProtobufUnknownValue&gt;} structure.
 *
 * <p>This class provides schema-less encoding and decoding: no generated Spec
 * classes, annotations, or reflection are required. The data model is a simple
 * map from field numbers to {@link ProtobufUnknownValue} instances.
 *
 * <p>This is particularly useful for:
 * <ul>
 * <li>Inspecting unknown or dynamic protobuf messages
 * <li>Debugging protobuf data without access to the original .proto definition
 * <li>Re-encoding previously decoded messages after modification
 * <li>Generic protobuf message processing
 * </ul>
 *
 * <p><span style="color: red;"><strong>Warning:</strong> This class is slow compared to the
 * generated Spec classes. Use it only when the generated Spec classes are not
 * available.</span></p>
 */
public final class ProtobufMessageSpec {
    private ProtobufMessageSpec() {
        throw new UnsupportedOperationException("ProtobufMessageSpec is a utility class and cannot be instantiated");
    }

    /**
     * Encodes a map of field numbers to {@link ProtobufUnknownValue} instances
     * into the given {@link ProtobufBinaryWriter}.
     *
     * <p><span style="color: red;"><strong>Warning:</strong> This method is slow compared to the
     * generated Spec classes. Use it only when the generated Spec classes are
     * not available.</span></p>
     *
     * @param fields the map of field numbers to values to encode
     * @param writer the writer to encode the fields into
     * @throws NullPointerException if the writer or the fields map is
     *         {@code null}
     */
    public static void encode(Map<Long, ProtobufUnknownValue> fields, ProtobufBinaryWriter<?> writer) {
        Objects.requireNonNull(fields, "The fields map cannot be null");
        Objects.requireNonNull(writer, "The writer cannot be null");
        for (var entry : fields.entrySet()) {
            writeUnknownProperty(entry.getKey(), entry.getValue(), writer);
        }
    }

    private static void writeUnknownProperty(long index, ProtobufUnknownValue value, ProtobufBinaryWriter<?> writer) {
        switch (value) {
            case ProtobufUnknownValue.VarInt(var varIntValue) -> {
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_VAR_INT);
                writer.writeRawVarInt64(varIntValue);
            }

            case ProtobufUnknownValue.Fixed64(var fixed64Value) -> {
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_FIXED64);
                writer.writeRawFixedInt64(fixed64Value);
            }

            case ProtobufUnknownValue.Fixed32(var fixed32Value) -> {
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_FIXED32);
                writer.writeRawFixedInt32(fixed32Value);
            }

            case ProtobufUnknownValue.LengthDelimited lengthDelimited -> {
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
                switch (lengthDelimited) {
                    case ProtobufUnknownValue.LengthDelimited.ByteArrayBacked(var array) -> {
                        writer.writeLengthDelimitedPropertyLength(array.length);
                        writer.writeRawBytes(array);
                    }
                    case ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(var buffer) -> {
                        writer.writeLengthDelimitedPropertyLength(buffer.remaining());
                        writer.writeRawBuffer(buffer);
                    }
                    case ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(var segment, var length) -> {
                        writer.writeLengthDelimitedPropertyLength(length);
                        writer.writeRawMemorySegment(segment);
                    }
                }
            }

            case ProtobufUnknownValue.Group(var groupValue) -> {
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_START_OBJECT);
                for (var groupEntry : groupValue.entrySet()) {
                    writeUnknownProperty(groupEntry.getKey(), groupEntry.getValue(), writer);
                }
                writer.writePropertyTag(index, ProtobufWireType.WIRE_TYPE_END_OBJECT);
            }

            case null -> { /* skip null values */ }
        }
    }

    /**
     * Computes the serialized size, in bytes, of a map of field numbers to
     * {@link ProtobufUnknownValue} instances.
     *
     * <p>The returned size accounts for field tags, wire-type overhead, and
     * length prefixes for length-delimited values.
     *
     * <p><span style="color: red;"><strong>Warning:</strong> This method is slow compared to the
     * generated Spec classes. Use it only when the generated Spec classes are
     * not available.</span></p>
     *
     * @param fields the map of field numbers to values to measure
     * @return the total serialized size in bytes
     * @throws NullPointerException if the fields map is {@code null}
     */
    public static long sizeOf(Map<Long, ProtobufUnknownValue> fields) {
        Objects.requireNonNull(fields, "The fields map cannot be null");
        var size = 0L;
        for (var entry : fields.entrySet()) {
            size += sizeOfProperty(entry.getKey(), entry.getValue());
        }
        return size;
    }

    private static long sizeOfProperty(long index, ProtobufUnknownValue value) {
        return switch (value) {
            case ProtobufUnknownValue.VarInt(var varInt) ->
                    ProtobufBinarySizeCalculator.getVarIntPropertySize(index, varInt);

            case ProtobufUnknownValue.Fixed64(var fixed64) ->
                    ProtobufBinarySizeCalculator.getFixed64PropertySize(index, fixed64);

            case ProtobufUnknownValue.Fixed32(var fixed32) ->
                    ProtobufBinarySizeCalculator.getFixed32PropertySize(index, fixed32);

            case ProtobufUnknownValue.LengthDelimited lengthDelimited -> {
                var contentLength = switch (lengthDelimited) {
                    case ProtobufUnknownValue.LengthDelimited.ByteArrayBacked(var array) -> array.length;
                    case ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(var buffer) -> buffer.remaining();
                    case ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(_, var length) -> length;
                };
                yield ProtobufBinarySizeCalculator.getLengthDelimitedPropertySize(index, contentLength);
            }

            case ProtobufUnknownValue.Group(var groupValue) -> {
                var tagSize = ProtobufBinarySizeCalculator.getPropertyWireTagSize(index, ProtobufWireType.WIRE_TYPE_START_OBJECT);
                var endTagSize = ProtobufBinarySizeCalculator.getPropertyWireTagSize(index, ProtobufWireType.WIRE_TYPE_END_OBJECT);
                var contentSize = 0L;
                for (var groupEntry : groupValue.entrySet()) {
                    contentSize += sizeOfProperty(groupEntry.getKey(), groupEntry.getValue());
                }
                yield tagSize + contentSize + endTagSize;
            }

            case null -> 0L;
        };
    }

    /**
     * Decodes a Protocol Buffer message from a {@link ProtobufBinaryReader}.
     *
     * <p>This method reads from the provided reader and decodes all available
     * protobuf fields into a map structure. Each field is read according to its
     * wire type and stored with its field number as the key.
     *
     * <p><span style="color: red;"><strong>Warning:</strong> This method is slow compared to the
     * generated Spec classes. Use it only when the generated Spec classes are
     * not available.</span></p>
     *
     * @param reader the reader containing the encoded Protocol Buffer data
     * @return a map where keys are field numbers and values are the decoded
     *         field values
     * @throws NullPointerException if the reader is {@code null}
     * @throws ProtobufDeserializationException if the data cannot be decoded
     * @see ProtobufBinaryReader#readUnknownProperty()
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
                } catch (ProtobufDeserializationException _) { // It wasn't an embedded message
                    yield value;
                }
            }

            case ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(var buffer) -> {
                var position = buffer.position();
                try { // Maybe it's an embedded message
                    yield decodeValue(ProtobufBinaryReader.fromBuffer(buffer));
                } catch (ProtobufDeserializationException _) { // It wasn't an embedded message
                    buffer.position(position); // Reset the position
                    yield value;
                }
            }

            case ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(var segment, var _) -> {
                try { // Maybe it's an embedded message
                    yield decodeValue(ProtobufBinaryReader.fromMemorySegment(segment));
                } catch (ProtobufDeserializationException _) { // It wasn't an embedded message
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