package it.auties.protobuf.io.reader.text;

import it.auties.protobuf.io.reader.ProtobufTextReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class ProtobufTextByteBufferReader extends ProtobufTextReader {
    private final ByteBuffer buffer;

    public ProtobufTextByteBufferReader(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.buffer = buffer.duplicate();
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