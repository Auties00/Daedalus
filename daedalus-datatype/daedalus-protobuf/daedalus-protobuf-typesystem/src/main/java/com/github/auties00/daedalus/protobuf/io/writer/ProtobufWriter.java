package com.github.auties00.daedalus.protobuf.io.writer;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A sealed abstract class representing a Protocol Buffer writer.
 * <p>
 * Two families of writers are supported:
 * <ul>
 *     <li>{@link ProtobufBinaryWriter} — writes the binary wire format (byte arrays, ByteBuffers, MemorySegments, OutputStreams)</li>
 *     <li>{@link ProtobufTextWriter} — writes text-based formats (JSON, textproto)</li>
 * </ul>
 * <p>
 * Generated Spec classes provide separate overloaded {@code encode} methods for each writer type.
 *
 * @param <OUTPUT> the type of output this writer produces (byte[], ByteBuffer, OutputStream, ...)
 */
public sealed abstract class ProtobufWriter<OUTPUT> implements AutoCloseable permits ProtobufBinaryWriter, ProtobufTextWriter {
    /**
     * Returns the output produced by this writer.
     *
     * @return the output produced by this writer
     */
    public abstract OUTPUT toOutput();

    public static ProtobufBinaryWriter<byte[]> toBinaryBytes(int length) {
        return ProtobufBinaryWriter.toBytes(length);
    }

    public static ProtobufBinaryWriter<byte[]> toBinaryBytes(byte[] bytes, int offset) {
        return ProtobufBinaryWriter.toBytes(bytes, offset);
    }

    public static ProtobufBinaryWriter<ByteBuffer> toBinaryBuffer(int length) {
        return ProtobufBinaryWriter.toBuffer(length);
    }

    public static ProtobufBinaryWriter<ByteBuffer> toBinaryBuffer(ByteBuffer buffer) {
        return ProtobufBinaryWriter.toBuffer(buffer);
    }

    public static ProtobufBinaryWriter<ByteBuffer> toBinaryDirectBuffer(int length) {
        return ProtobufBinaryWriter.toDirectBuffer(length);
    }

    public static ProtobufBinaryWriter<MemorySegment> toBinaryMemorySegment(MemorySegment segment) {
        return ProtobufBinaryWriter.toMemorySegment(segment);
    }

    public static ProtobufBinaryWriter<OutputStream> toBinaryStream(OutputStream stream) {
        return ProtobufBinaryWriter.toStream(stream);
    }

    public static ProtobufBinaryWriter<OutputStream> toBinaryStream(OutputStream stream, int tempBufferLength) {
        return ProtobufBinaryWriter.toStream(stream, tempBufferLength);
    }

    public static ProtobufTextWriter<byte[]> toTextBytes(int length) {
        return ProtobufTextWriter.toBytes(length);
    }

    public static ProtobufTextWriter<byte[]> toTextBytes(byte[] bytes, int offset) {
        return ProtobufTextWriter.toBytes(bytes, offset);
    }

    public static ProtobufTextWriter<ByteBuffer> toTextHeapBuffer(int length) {
        return ProtobufTextWriter.toHeapBuffer(length);
    }

    public static ProtobufTextWriter<ByteBuffer> toTextDirectBuffer(int length) {
        return ProtobufTextWriter.toDirectBuffer(length);
    }

    public static ProtobufTextWriter<ByteBuffer> toTextBuffer(ByteBuffer buffer) {
        return ProtobufTextWriter.toBuffer(buffer);
    }

    public static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream) {
        return ProtobufTextWriter.toStream(stream);
    }

    public static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream, boolean autoclose) {
        return ProtobufTextWriter.toStream(stream, autoclose);
    }

    public static ProtobufTextWriter<OutputStream> toTextStream(OutputStream stream, boolean autoclose, int bufferSize) {
        return ProtobufTextWriter.toStream(stream, autoclose, bufferSize);
    }

    public static ProtobufTextWriter<MemorySegment> toTextMemorySegment(MemorySegment segment) {
        return ProtobufTextWriter.toMemorySegment(segment);
    }

    public static ProtobufTextWriter<String> toTextString(int initialCapacity) {
        return ProtobufTextWriter.toString(initialCapacity);
    }

    public static ProtobufTextWriter<? extends CharSequence> toTextCharSequence(int initialCapacity) {
        return ProtobufTextWriter.toCharSequence(initialCapacity);
    }

    public static ProtobufTextWriter<CharBuffer> toTextCharBuffer(CharBuffer buffer) {
        return ProtobufTextWriter.toCharBuffer(buffer);
    }
}
