package it.auties.protobuf.io.reader;

import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * A sealed interface representing a Protocol Buffer reader.
 * <p>
 * Two families of readers are supported:
 * <ul>
 *     <li>{@link ProtobufBinaryReader} — reads from the binary wire format (byte arrays, ByteBuffers, MemorySegments, InputStreams)</li>
 *     <li>{@link ProtobufTextReader} — reads from text-based formats (JSON, textproto)</li>
 * </ul>
 * <p>
 * Generated Spec classes provide separate overloaded {@code decode} methods for each reader type.
 */
public sealed interface ProtobufReader extends AutoCloseable permits ProtobufBinaryReader, ProtobufTextReader {
    static ProtobufBinaryReader fromBytes(byte[] bytes) {
        return ProtobufBinaryReader.fromBytes(bytes);
    }

    static ProtobufBinaryReader fromBytes(byte[] bytes, int offset, int length) {
        return ProtobufBinaryReader.fromBytes(bytes, offset, length);
    }

    static ProtobufBinaryReader fromBuffer(ByteBuffer buffer) {
        return ProtobufBinaryReader.fromBuffer(buffer);
    }

    static ProtobufBinaryReader fromStream(InputStream stream) {
        return ProtobufBinaryReader.fromStream(stream);
    }

    static ProtobufBinaryReader fromStream(InputStream stream, boolean autoclose) {
        return ProtobufBinaryReader.fromStream(stream, autoclose);
    }

    static ProtobufBinaryReader fromStream(InputStream stream, boolean autoclose, int bufferSize) {
        return ProtobufBinaryReader.fromStream(stream, autoclose, bufferSize);
    }

    static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit);
    }

    static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit, autoclose);
    }

    static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit, autoclose, bufferSize);
    }

    static ProtobufBinaryReader fromMemorySegment(MemorySegment segment) {
        return ProtobufBinaryReader.fromMemorySegment(segment);
    }

    static ProtobufJsonStringReader fromJson(String json) {
        return new ProtobufJsonStringReader(json);
    }

    static ProtobufJsonByteArrayReader fromJson(byte[] json) {
        return new ProtobufJsonByteArrayReader(json);
    }

    static ProtobufTextFormatStringReader fromTextFormat(String textproto) {
        return new ProtobufTextFormatStringReader(textproto);
    }

    static ProtobufTextFormatByteArrayReader fromTextFormat(byte[] textproto) {
        return new ProtobufTextFormatByteArrayReader(textproto);
    }
}
