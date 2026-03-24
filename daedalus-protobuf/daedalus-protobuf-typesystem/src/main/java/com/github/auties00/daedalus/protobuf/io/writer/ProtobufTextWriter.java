package com.github.auties00.daedalus.protobuf.io.writer;

import com.github.auties00.daedalus.protobuf.io.writer.text.*;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * An abstract writer for text-based Protocol Buffer formats (JSON, textproto).
 * <p>
 * Text writers produce human-readable representations of Protocol Buffer messages
 * using field names instead of field numbers. Generated Spec classes provide separate
 * {@code encode(T, ProtobufTextWriter)} overloads that pass field names for each property.
 *
 * @see ProtobufBinaryWriter
 */
public abstract non-sealed class ProtobufTextWriter<OUTPUT> implements ProtobufWriter<OUTPUT> {
    public static ProtobufTextWriter<byte[]> toBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteArrayWriter(new byte[length], 0, length);
    }

    public static ProtobufTextWriter<byte[]> toBytes(byte[] bytes, int offset) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        Objects.checkIndex(offset, bytes.length);
        return new ProtobufTextByteArrayWriter(bytes, offset, bytes.length);
    }

    public static ProtobufTextWriter<ByteBuffer> toHeapBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteBufferWriter(ByteBuffer.allocate(length));
    }

    public static ProtobufTextWriter<ByteBuffer> toDirectBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteBufferWriter(ByteBuffer.allocateDirect(length));
    }

    public static ProtobufTextWriter<ByteBuffer> toBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer is read-only");
        }
        return new ProtobufTextByteBufferWriter(buffer);
    }

    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, true);
    }

    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream, boolean autoclose) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, autoclose);
    }

    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, autoclose, bufferSize);
    }

    public static ProtobufTextWriter<MemorySegment> toMemorySegment(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        return new ProtobufTextMemorySegmentWriter(segment);
    }

    public static ProtobufTextWriter<? extends CharSequence> toCharSequence(int initialCapacity) {
        return toString(initialCapacity);
    }

    public static ProtobufTextWriter<String> toString(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must not be negative");
        }
        return new ProtobufTextCharSequenceWriter(initialCapacity);
    }

    public static ProtobufTextWriter<CharBuffer> toCharBuffer(CharBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer is read-only");
        }
        return new ProtobufTextCharBufferWriter(buffer);
    }

    public abstract void writeStartObject();

    public abstract void writeStartObjectProperty(String fieldName);

    public abstract void writeStartObjectElement();

    public abstract void writeEndObject();

    public abstract void writeStartRepeatedProperty(String fieldName);

    public abstract void writeEndRepeatedProperty();

    public abstract void writeFloatProperty(String fieldName, float value);

    public abstract void writeDoubleProperty(String fieldName, double value);

    public abstract void writeBoolProperty(String fieldName, boolean value);

    public abstract void writeInt32Property(String fieldName, int value);

    public abstract void writeUInt32Property(String fieldName, int value);

    public abstract void writeSInt32Property(String fieldName, int value);

    public abstract void writeFixed32Property(String fieldName, int value);

    public abstract void writeSFixed32Property(String fieldName, int value);

    public abstract void writeInt64Property(String fieldName, long value);

    public abstract void writeUInt64Property(String fieldName, long value);

    public abstract void writeSInt64Property(String fieldName, long value);

    public abstract void writeFixed64Property(String fieldName, long value);

    public abstract void writeSFixed64Property(String fieldName, long value);

    public abstract void writeStringProperty(String fieldName, String value);

    public abstract void writeBytesProperty(String fieldName, byte[] value);

    public abstract void writeEnumProperty(String fieldName, int number, String enumName);

    public abstract void writeFloatElement(float value);

    public abstract void writeDoubleElement(double value);

    public abstract void writeBoolElement(boolean value);

    public abstract void writeInt32Element(int value);

    public abstract void writeUInt32Element(int value);

    public abstract void writeSInt32Element(int value);

    public abstract void writeFixed32Element(int value);

    public abstract void writeSFixed32Element(int value);

    public abstract void writeInt64Element(long value);

    public abstract void writeUInt64Element(long value);

    public abstract void writeSInt64Element(long value);

    public abstract void writeFixed64Element(long value);

    public abstract void writeSFixed64Element(long value);

    public abstract void writeStringElement(String value);

    public abstract void writeBytesElement(byte[] value);

    public abstract void writeEnumElement(int number, String enumName);

    public abstract void writeNullElement();
}
