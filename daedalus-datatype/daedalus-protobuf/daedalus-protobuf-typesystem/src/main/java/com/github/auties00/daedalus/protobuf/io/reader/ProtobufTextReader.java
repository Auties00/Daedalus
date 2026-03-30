package com.github.auties00.daedalus.protobuf.io.reader;

import com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextStreamReader;
import com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextTokenizedReader;

import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * An abstract reader for text-based Protocol Buffer formats (JSON, textproto).
 * <p>
 * Text readers parse human-readable representations of Protocol Buffer messages
 * using field names instead of field numbers. Generated Spec classes provide separate
 * {@code decode(ProtobufTextReader)} overloads that switch on field names.
 *
 * @see ProtobufTextReader
 */
public abstract non-sealed class ProtobufTextReader extends ProtobufReader {
    public static ProtobufTextReader fromString(String sequence) {
        return new ProtobufTextTokenizedReader(sequence);
    }

    public static ProtobufTextReader fromCharSequence(CharSequence sequence) {
        return new ProtobufTextTokenizedReader(sequence);
    }

    public static ProtobufTextReader fromBytes(byte[] bytes) {
        return new ProtobufTextTokenizedReader(bytes);
    }

    public static ProtobufTextReader fromBytes(byte[] bytes, int offset, int length) {
        return new ProtobufTextTokenizedReader(bytes, offset, offset + length);
    }

    public static ProtobufTextReader fromBuffer(ByteBuffer buffer) {
        return new ProtobufTextTokenizedReader(buffer);
    }

    public static ProtobufTextReader fromStream(InputStream stream) {
        return fromStream(stream, true);
    }

    public static ProtobufTextReader fromStream(InputStream stream, boolean autoclose) {
        return new ProtobufTextStreamReader(stream, -1, autoclose);
    }

    public static ProtobufTextReader fromStream(InputStream stream, boolean autoclose, int bufferSize) {
        return new ProtobufTextStreamReader(stream, -1, autoclose, bufferSize);
    }

    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit) {
        return fromLimitedStream(stream, limit, true);
    }

    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return new ProtobufTextStreamReader(stream, limit, autoclose);
    }

    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return new ProtobufTextStreamReader(stream, limit, autoclose, bufferSize);
    }

    public static ProtobufTextReader fromMemorySegment(MemorySegment segment) {
        return new ProtobufTextTokenizedReader(segment);
    }

    protected String propertyName;

    public abstract void readStartObject();

    public abstract void readEndObject();

    public abstract void readStartArray();

    public abstract void readEndArray();

    /**
     * Reads the next property name in the current object.
     * Returns {@code true} if a property was read, {@code false} if the object is finished.
     * The name is accessible via {@link #propertyName()}.
     */
    public abstract boolean readPropertyName();

    /**
     * Returns the last property name read by {@link #readPropertyName()}.
     */
    public String propertyName() {
        return propertyName;
    }

    public abstract boolean isFinished();

    public abstract void skipUnknownProperty();

    public abstract float readFloatProperty();

    public abstract double readDoubleProperty();

    public abstract boolean readBoolProperty();

    public abstract int readInt32Property();

    public abstract int readUInt32Property();

    public abstract int readSInt32Property();

    public abstract int readFixed32Property();

    public abstract int readSFixed32Property();

    public abstract long readInt64Property();

    public abstract long readUInt64Property();

    public abstract long readSInt64Property();

    public abstract long readFixed64Property();

    public abstract long readSFixed64Property();

    public abstract String readStringProperty();

    public abstract byte[] readBytesProperty();
}
