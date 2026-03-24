package com.github.auties00.daedalus.protobuf.io.reader;

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
public sealed abstract class ProtobufReader implements AutoCloseable permits ProtobufBinaryReader, ProtobufTextReader {
    public static ProtobufBinaryReader fromBinaryBytes(byte[] bytes) {
        return ProtobufBinaryReader.fromBytes(bytes);
    }

    public static ProtobufBinaryReader fromBinaryBytes(byte[] bytes, int offset, int length) {
        return ProtobufBinaryReader.fromBytes(bytes, offset, length);
    }

    public static ProtobufBinaryReader fromBinaryBuffer(ByteBuffer buffer) {
        return ProtobufBinaryReader.fromBuffer(buffer);
    }

    public static ProtobufBinaryReader fromBinaryStream(InputStream stream) {
        return ProtobufBinaryReader.fromStream(stream);
    }

    public static ProtobufBinaryReader fromBinaryStream(InputStream stream, boolean autoclose) {
        return ProtobufBinaryReader.fromStream(stream, autoclose);
    }

    public static ProtobufBinaryReader fromBinaryStream(InputStream stream, boolean autoclose, int bufferSize) {
        return ProtobufBinaryReader.fromStream(stream, autoclose, bufferSize);
    }

    public static ProtobufBinaryReader fromBinaryLimitedStream(InputStream stream, long limit) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit);
    }

    public static ProtobufBinaryReader fromBinaryLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit, autoclose);
    }

    public static ProtobufBinaryReader fromBinaryLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return ProtobufBinaryReader.fromLimitedStream(stream, limit, autoclose, bufferSize);
    }

    public static ProtobufBinaryReader fromBinaryMemorySegment(MemorySegment segment) {
        return ProtobufBinaryReader.fromMemorySegment(segment);
    }

    public static ProtobufTextReader fromTextString(String sequence) {
        return ProtobufTextReader.fromString(sequence);
    }

    public static ProtobufTextReader fromTextCharSequence(CharSequence sequence) {
        return ProtobufTextReader.fromCharSequence(sequence);
    }

    public static ProtobufTextReader fromTextBytes(byte[] bytes) {
        return ProtobufTextReader.fromBytes(bytes);
    }

    public static ProtobufTextReader fromTextBytes(byte[] bytes, int offset, int length) {
        return ProtobufTextReader.fromBytes(bytes, offset, length);
    }

    public static ProtobufTextReader fromTextBuffer(ByteBuffer buffer) {
        return ProtobufTextReader.fromBuffer(buffer);
    }

    public static ProtobufTextReader fromTextStream(InputStream stream) {
        return ProtobufTextReader.fromStream(stream);
    }

    public static ProtobufTextReader fromTextStream(InputStream stream, boolean autoclose) {
        return ProtobufTextReader.fromStream(stream, autoclose);
    }

    public static ProtobufTextReader fromTextStream(InputStream stream, boolean autoclose, int bufferSize) {
        return ProtobufTextReader.fromStream(stream, autoclose, bufferSize);
    }

    public static ProtobufTextReader fromTextLimitedStream(InputStream stream, long limit) {
        return ProtobufTextReader.fromLimitedStream(stream, limit);
    }

    public static ProtobufTextReader fromTextLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return ProtobufTextReader.fromLimitedStream(stream, limit, autoclose);
    }

    public static ProtobufTextReader fromTextLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return ProtobufTextReader.fromLimitedStream(stream, limit, autoclose, bufferSize);
    }

    public static ProtobufTextReader fromTextMemorySegment(MemorySegment segment) {
        return ProtobufTextReader.fromMemorySegment(segment);
    }
}
