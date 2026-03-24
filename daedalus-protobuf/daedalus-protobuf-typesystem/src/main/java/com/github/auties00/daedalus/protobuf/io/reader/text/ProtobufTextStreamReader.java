package com.github.auties00.daedalus.protobuf.io.reader.text;

import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;

import java.io.InputStream;
import java.util.Objects;

public final class ProtobufTextStreamReader extends ProtobufTextReader {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream inputStream;
    private final boolean autoclose;
    private long position;
    private final long limit;
    private boolean finished;

    private final byte[] buffer;
    private int bufferPosition;
    private int bufferLimit;

    private ProtobufTextStreamReader lengthDelimitedReader;

    public ProtobufTextStreamReader(InputStream inputStream, long limit, boolean autoclose) {
        this(inputStream, limit, autoclose, DEFAULT_BUFFER_SIZE);
    }

    public ProtobufTextStreamReader(InputStream inputStream, long limit, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        this.inputStream = inputStream;
        this.autoclose = autoclose;
        this.limit = limit;
        this.buffer = new byte[bufferSize];
    }

    private ProtobufTextStreamReader(InputStream inputStream, long limit, byte[] buffer, int bufferPosition, int bufferLimit, long position) {
        this.inputStream = inputStream;
        this.autoclose = false;
        this.limit = limit;
        this.buffer = buffer;
        this.bufferPosition = bufferPosition;
        this.bufferLimit = bufferLimit;
        this.position = position;
    }

    @Override public void readStartObject() { throw new UnsupportedOperationException(); }
    @Override public void readEndObject() { throw new UnsupportedOperationException(); }
    @Override public void readStartArray() { throw new UnsupportedOperationException(); }
    @Override public void readEndArray() { throw new UnsupportedOperationException(); }
    @Override public boolean readPropertyName() { throw new UnsupportedOperationException(); }
    @Override public boolean isFinished() { throw new UnsupportedOperationException(); }
    @Override public void skipUnknownProperty() { throw new UnsupportedOperationException(); }
    @Override public float readFloatProperty() { throw new UnsupportedOperationException(); }
    @Override public double readDoubleProperty() { throw new UnsupportedOperationException(); }
    @Override public boolean readBoolProperty() { throw new UnsupportedOperationException(); }
    @Override public int readInt32Property() { throw new UnsupportedOperationException(); }
    @Override public int readUInt32Property() { throw new UnsupportedOperationException(); }
    @Override public int readSInt32Property() { throw new UnsupportedOperationException(); }
    @Override public int readFixed32Property() { throw new UnsupportedOperationException(); }
    @Override public int readSFixed32Property() { throw new UnsupportedOperationException(); }
    @Override public long readInt64Property() { throw new UnsupportedOperationException(); }
    @Override public long readUInt64Property() { throw new UnsupportedOperationException(); }
    @Override public long readSInt64Property() { throw new UnsupportedOperationException(); }
    @Override public long readFixed64Property() { throw new UnsupportedOperationException(); }
    @Override public long readSFixed64Property() { throw new UnsupportedOperationException(); }
    @Override public String readStringProperty() { throw new UnsupportedOperationException(); }
    @Override public byte[] readBytesProperty() { throw new UnsupportedOperationException(); }
    @Override public String readEnumNameProperty() { throw new UnsupportedOperationException(); }
    @Override public int readEnumNumberProperty() { throw new UnsupportedOperationException(); }
    @Override public void close() {}
}