package it.auties.protobuf.io.writer;

import it.auties.protobuf.exception.ProtobufSerializationException;
import it.auties.protobuf.io.ProtobufDataType;

import java.lang.foreign.MemorySegment;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

final class ProtobufByteBufferWriter extends ProtobufWriter<ByteBuffer> {
    private final ByteBuffer buffer;

    ProtobufByteBufferWriter(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void writeRawByte(byte entry) {
        try {
            buffer.put(entry);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) {
        try {
            buffer.put(entry, offset, length);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) {
        try {
            buffer.put(entry.duplicate());
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) {
        try {
            buffer.put(entry.asByteBuffer());
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt32(int value) {
        try {
            var position = buffer.position();
            BUFFER_AS_INT32_LE.set(buffer, position, value);
            buffer.position(position + Integer.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long value) {
        try {
            var position = buffer.position();
            BUFFER_AS_INT64_LE.set(buffer, position, value);
            buffer.position(position + Long.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) {
        try {
            var position = buffer.position();
            BUFFER_AS_FLOAT_LE.set(buffer, position, entry);
            buffer.position(position + Float.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) {
        try {
            var position = buffer.position();
            BUFFER_AS_DOUBLE_LE.set(buffer, position, entry);
            buffer.position(position + Double.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt32(int entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawVarInt64(long entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFloat(float[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedDouble(double[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        throw new UnsupportedOperationException();
    }


    @Override
    public ByteBuffer toOutput() {
        return buffer;
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.BYTE_BUFFER;
    }

    @Override
    public void close() {

    }
}
