package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;

import java.io.OutputStream;
import java.util.Objects;

public final class ProtobufTextStreamWriter extends ProtobufTextWriter<OutputStream> {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final OutputStream outputStream;
    private final boolean autoclose;

    private final byte[] buffer;
    private int bufferPosition;

    public ProtobufTextStreamWriter(OutputStream outputStream, boolean autoclose) {
        this(outputStream, autoclose, DEFAULT_BUFFER_SIZE);
    }

    public ProtobufTextStreamWriter(OutputStream outputStream, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(outputStream, "outputStream cannot be null");
        this.outputStream = outputStream;
        this.autoclose = autoclose;
        this.buffer = new byte[bufferSize];
    }

    @Override public void writeStartObject() { throw new UnsupportedOperationException(); }
    @Override public void writeStartObjectProperty(String fieldName) { throw new UnsupportedOperationException(); }
    @Override public void writeStartObjectElement() { throw new UnsupportedOperationException(); }
    @Override public void writeEndObject() { throw new UnsupportedOperationException(); }
    @Override public void writeStartRepeatedProperty(String fieldName) { throw new UnsupportedOperationException(); }
    @Override public void writeEndRepeatedProperty() { throw new UnsupportedOperationException(); }
    @Override public void writeFloatProperty(String fieldName, float value) { throw new UnsupportedOperationException(); }
    @Override public void writeDoubleProperty(String fieldName, double value) { throw new UnsupportedOperationException(); }
    @Override public void writeBoolProperty(String fieldName, boolean value) { throw new UnsupportedOperationException(); }
    @Override public void writeInt32Property(String fieldName, int value) { throw new UnsupportedOperationException(); }
    @Override public void writeUInt32Property(String fieldName, int value) { throw new UnsupportedOperationException(); }
    @Override public void writeSInt32Property(String fieldName, int value) { throw new UnsupportedOperationException(); }
    @Override public void writeFixed32Property(String fieldName, int value) { throw new UnsupportedOperationException(); }
    @Override public void writeSFixed32Property(String fieldName, int value) { throw new UnsupportedOperationException(); }
    @Override public void writeInt64Property(String fieldName, long value) { throw new UnsupportedOperationException(); }
    @Override public void writeUInt64Property(String fieldName, long value) { throw new UnsupportedOperationException(); }
    @Override public void writeSInt64Property(String fieldName, long value) { throw new UnsupportedOperationException(); }
    @Override public void writeFixed64Property(String fieldName, long value) { throw new UnsupportedOperationException(); }
    @Override public void writeSFixed64Property(String fieldName, long value) { throw new UnsupportedOperationException(); }
    @Override public void writeStringProperty(String fieldName, String value) { throw new UnsupportedOperationException(); }
    @Override public void writeBytesProperty(String fieldName, byte[] value) { throw new UnsupportedOperationException(); }
    @Override public void writeFloatElement(float value) { throw new UnsupportedOperationException(); }
    @Override public void writeDoubleElement(double value) { throw new UnsupportedOperationException(); }
    @Override public void writeBoolElement(boolean value) { throw new UnsupportedOperationException(); }
    @Override public void writeInt32Element(int value) { throw new UnsupportedOperationException(); }
    @Override public void writeUInt32Element(int value) { throw new UnsupportedOperationException(); }
    @Override public void writeSInt32Element(int value) { throw new UnsupportedOperationException(); }
    @Override public void writeFixed32Element(int value) { throw new UnsupportedOperationException(); }
    @Override public void writeSFixed32Element(int value) { throw new UnsupportedOperationException(); }
    @Override public void writeInt64Element(long value) { throw new UnsupportedOperationException(); }
    @Override public void writeUInt64Element(long value) { throw new UnsupportedOperationException(); }
    @Override public void writeSInt64Element(long value) { throw new UnsupportedOperationException(); }
    @Override public void writeFixed64Element(long value) { throw new UnsupportedOperationException(); }
    @Override public void writeSFixed64Element(long value) { throw new UnsupportedOperationException(); }
    @Override public void writeStringElement(String value) { throw new UnsupportedOperationException(); }
    @Override public void writeBytesElement(byte[] value) { throw new UnsupportedOperationException(); }
    @Override public void writeNullElement() { throw new UnsupportedOperationException(); }
    @Override public OutputStream toOutput() { throw new UnsupportedOperationException(); }
    @Override public void close() {}
}
