package com.github.auties00.daedalus.protobuf.io.writer.binary;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
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
    public void writeRawByte(byte entry) throws ProtobufSerializationException {
        try {
            buffer[position++] = entry;
        } catch (ArrayIndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) throws ProtobufSerializationException {
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
    public void writeRawBuffer(ByteBuffer entry) throws ProtobufSerializationException {
        try {
            var length = entry.remaining();
            entry.get(entry.position(), buffer, position, length);
            position += length;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) throws ProtobufSerializationException {
        var byteSize = entry.byteSize();
        if (byteSize > buffer.length - position) {
            throw ProtobufSerializationException.underflow();
        }

        var length = (int) byteSize;
        MemorySegment.copy(
                entry,
                ValueLayout.JAVA_BYTE,
                0,
                buffer,
                position,
                length
        );
        position += length;
    }

    @Override
    public void writeRawFixedInt32(int value) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeIntLE(buffer, position, value);
            position += Integer.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long value) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeLongLE(buffer, position, value);
            position += Long.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeFloatLE(buffer, position, entry);
            position += Float.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeDoubleLE(buffer, position, entry);
            position += Double.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt32(int entry) throws ProtobufSerializationException {
        try {
            position += writeRawVarInt32(buffer, position, entry);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt64(long entry) throws ProtobufSerializationException {
        try {
            position += writeRawVarInt64(buffer, position, entry);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFixedInt32(value);
        }
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFixedInt64(value);
        }
    }

    @Override
    public void writeRawPackedFloat(float[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFloat(value);
        }
    }

    @Override
    public void writeRawPackedDouble(double[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawDouble(value);
        }
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt32(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawZigZagVarInt32(value);
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt64(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt64((value << 1) ^ (value >> 63));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
    }

    @Override
    public byte[] toOutput() {
        return buffer;
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.BYTE_ARRAY;
    }

    @Override
    public void close() {

    }
}
