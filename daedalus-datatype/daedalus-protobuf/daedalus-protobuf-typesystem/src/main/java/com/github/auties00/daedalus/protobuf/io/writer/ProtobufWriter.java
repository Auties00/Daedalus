package com.github.auties00.daedalus.protobuf.io.writer;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A sealed interface representing a Protocol Buffer writer.
 * <p>
 * Two families of writers are supported:
 * <ul>
 *     <li>{@link ProtobufBinaryWriter} — writes the binary wire format (byte arrays, ByteBuffers, MemorySegments, OutputStreams)</li>
 *     <li>{@link ProtobufTextWriter} — writes text-based formats (JSON, textproto)</li>
 * </ul>
 * <p>
 * Generated Spec classes provide separate overloaded {@code encode} methods for each writer type.
 */
public sealed interface ProtobufWriter<OUTPUT> extends AutoCloseable permits ProtobufBinaryWriter, ProtobufTextWriter {
    OUTPUT toOutput();

    static ProtobufBinaryWriter<byte[]> toBinaryBytes(int length) {
        return ProtobufBinaryWriter.toBytes(length);
    }

    static ProtobufBinaryWriter<byte[]> toBinaryBytes(byte[] bytes, int offset) {
        return ProtobufBinaryWriter.toBytes(bytes, offset);
    }

    static ProtobufBinaryWriter<ByteBuffer> toBinaryBuffer(int length) {
        return ProtobufBinaryWriter.toBuffer(length);
    }

    static ProtobufBinaryWriter<ByteBuffer> toBinaryBuffer(ByteBuffer buffer) {
        return ProtobufBinaryWriter.toBuffer(buffer);
    }

    static ProtobufBinaryWriter<ByteBuffer> toBinaryDirectBuffer(int length) {
        return ProtobufBinaryWriter.toDirectBuffer(length);
    }

    static ProtobufBinaryWriter<MemorySegment> toBinaryMemorySegment(MemorySegment segment) {
        return ProtobufBinaryWriter.toMemorySegment(segment);
    }

    static ProtobufBinaryWriter<OutputStream> toBinaryStream(OutputStream stream) {
        return ProtobufBinaryWriter.toStream(stream);
    }

    static ProtobufBinaryWriter<OutputStream> toBinaryStream(OutputStream stream, int tempBufferLength) {
        return ProtobufBinaryWriter.toStream(stream, tempBufferLength);
    }

    static ProtobufTextWriter<byte[]> toTextBytes(int length) {
        return ProtobufTextWriter.toBytes(length);
    }

    static ProtobufTextWriter<byte[]> toTextBytes(byte[] bytes, int offset) {
        return ProtobufTextWriter.toBytes(bytes, offset);
    }

    static ProtobufTextWriter<ByteBuffer> toTextHeapBuffer(int length) {
        return ProtobufTextWriter.toHeapBuffer(length);
    }

    static ProtobufTextWriter<ByteBuffer> toTextDirectBuffer(int length) {
        return ProtobufTextWriter.toDirectBuffer(length);
    }

    static ProtobufTextWriter<ByteBuffer> toTextBuffer(ByteBuffer buffer) {
        return ProtobufTextWriter.toBuffer(buffer);
    }

    static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream) {
        return ProtobufTextWriter.toStream(stream);
    }

    static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream, boolean autoclose) {
        return ProtobufTextWriter.toStream(stream, autoclose);
    }

    static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream, boolean autoclose, int bufferSize) {
        return ProtobufTextWriter.toStream(stream, autoclose, bufferSize);
    }

    static ProtobufTextWriter<MemorySegment> toTextMemorySegment(MemorySegment segment) {
        return ProtobufTextWriter.toMemorySegment(segment);
    }

    static ProtobufTextWriter<String> toTextString(int initialCapacity) {
        return ProtobufTextWriter.toString(initialCapacity);
    }

    static ProtobufTextWriter<? extends CharSequence> toTextCharSequence(int initialCapacity) {
        return ProtobufTextWriter.toCharSequence(initialCapacity);
    }

    static ProtobufTextWriter<CharBuffer> toTextCharBuffer(CharBuffer buffer) {
        return ProtobufTextWriter.toCharBuffer(buffer);
    }
}
