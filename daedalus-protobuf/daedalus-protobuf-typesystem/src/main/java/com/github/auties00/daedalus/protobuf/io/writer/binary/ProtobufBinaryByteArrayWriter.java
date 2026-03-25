package com.github.auties00.daedalus.protobuf.io.writer.binary;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.ProtobufIODataType;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public final class ProtobufBinaryByteArrayWriter extends ProtobufBinaryWriter<byte[]> {
    private final byte[] buffer;
    private int position;

    public ProtobufBinaryByteArrayWriter(byte[] buffer, int offset) {
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
        position += writeRawVarInt32(buffer, position, entry);
    }

    @Override
    public void writeRawVarInt64(long entry) {
        position += writeRawVarInt64(buffer, position, entry);
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) {
        for (var value : input) {
            writeRawFixedInt32(value);
        }
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) {
        for (var value : input) {
            writeRawFixedInt64(value);
        }
    }

    @Override
    public void writeRawPackedFloat(float[] input) {
        for (var value : input) {
            writeRawFloat(value);
        }
    }

    @Override
    public void writeRawPackedDouble(double[] input) {
        for (var value : input) {
            writeRawDouble(value);
        }
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) {
        for (var value : input) {
            position += writeRawVarInt32(buffer, position, value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        for (var value : input) {
            position += writeRawVarInt32(buffer, position, (value << 1) ^ (value >> 31));
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        for (var value : input) {
            position += writeRawVarInt64(buffer, position, value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        for (var value : input) {
            position += writeRawVarInt64(buffer, position, (value << 1) ^ (value >> 63));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
    }

    @Override
    public byte[] toOutput() {
        return buffer;
    }

    @Override
    public ProtobufIODataType rawDataTypePreference() {
        return ProtobufIODataType.BYTE_ARRAY;
    }

    @Override
    public void close() {

    }
}
