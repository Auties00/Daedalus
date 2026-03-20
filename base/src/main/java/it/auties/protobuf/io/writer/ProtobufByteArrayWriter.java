package it.auties.protobuf.io.writer;

import it.auties.protobuf.exception.ProtobufSerializationException;
import it.auties.protobuf.io.ProtobufDataType;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

final class ProtobufByteArrayWriter extends ProtobufWriter<byte[]> {
    private final byte[] buffer;
    private int position;

    ProtobufByteArrayWriter(byte[] buffer, int offset) {
        this.buffer = buffer;
        this.position = offset;
    }

    @Override
    public void writeRawByte(byte entry) {
        try {
            buffer[position++] = entry;
        } catch (ArrayIndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) {
        try {
            System.arraycopy(entry, offset, buffer, position, length);
            position += length;
        } catch (NegativeArraySizeException _) {
            throw ProtobufSerializationException.negativeLength();
        } catch (IndexOutOfBoundsException error) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) {
        var length = entry.remaining();
        entry.get(entry.position(), buffer, position, length);
        position += length;
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) {
        var byteSize = entry.byteSize();
        var safeByteSize = (int) byteSize;
        if (byteSize == safeByteSize) {
            try {
                MemorySegment.copy(
                        entry,
                        ValueLayout.JAVA_BYTE,
                        0,
                        buffer,
                        position,
                        safeByteSize
                );
                position += safeByteSize;
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufSerializationException.underflow();
            }
        }

        throw ProtobufSerializationException.underflow();
    }

    @Override
    public void writeRawFixedInt32(int value) {
        try {
            ARRAY_AS_INT32_LE.set(buffer, position, value);
            position += Integer.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long value) {
        try {
            ARRAY_AS_INT64_LE.set(buffer, position, value);
            position += Long.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) {
        try {
            ARRAY_AS_FLOAT_LE.set(buffer, position, entry);
            position += Float.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) {
        try {
            ARRAY_AS_DOUBLE_LE.set(buffer, position, entry);
            position += Double.BYTES;
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
    public byte[] toOutput() {
        return buffer;
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.BYTE_ARRAY;
    }

    @Override
    public void close() {

    }
}
