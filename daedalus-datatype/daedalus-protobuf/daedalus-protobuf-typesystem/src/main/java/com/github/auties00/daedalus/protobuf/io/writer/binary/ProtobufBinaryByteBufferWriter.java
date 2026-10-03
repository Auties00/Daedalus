package com.github.auties00.daedalus.protobuf.io.writer.binary;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;

import java.lang.foreign.MemorySegment;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public final class ProtobufBinaryByteBufferWriter extends ProtobufBinaryWriter<ByteBuffer> {
    private final ByteBuffer buffer;

    public ProtobufBinaryByteBufferWriter(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void writeRawByte(byte entry) throws ProtobufSerializationException {
        try {
            buffer.put(entry);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) throws ProtobufSerializationException {
        try {
            buffer.put(entry, offset, length);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) throws ProtobufSerializationException {
        try {
            buffer.put(entry.duplicate());
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) throws ProtobufSerializationException {
        try {
            buffer.put(entry.asByteBuffer());
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt32(int value) throws ProtobufSerializationException {
        try {
            var position = buffer.position();
            BinaryUtils.writeIntLE(buffer, position, value);
            buffer.position(position + Integer.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long value) throws ProtobufSerializationException {
        try {
            var position = buffer.position();
            BinaryUtils.writeLongLE(buffer, position, value);
            buffer.position(position + Long.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) throws ProtobufSerializationException {
        try {
            var position = buffer.position();
            BinaryUtils.writeFloatLE(buffer, position, entry);
            buffer.position(position + Float.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) throws ProtobufSerializationException {
        try {
            var position = buffer.position();
            BinaryUtils.writeDoubleLE(buffer, position, entry);
            buffer.position(position + Double.BYTES);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt32(int entry) throws ProtobufSerializationException {
        try {
            var pos = buffer.position();
            buffer.position(pos + putVarInt32LE(buffer, pos, entry));
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt64(long entry) throws ProtobufSerializationException {
        try {
            var pos = buffer.position();
            buffer.position(pos + putVarInt64LE(buffer, pos, entry));
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
    public ByteBuffer toOutput() {
        return buffer;
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.BYTE_BUFFER;
    }

    @Override
    public void close() {

    }

    private static int putVarInt32LE(ByteBuffer buffer, int offset, int value) {
        if (value >= 0) {
            if (buffer.limit() - offset >= 8) {
                var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
                var size = VARINT32_SIZE_TABLE[nlz];
                var loCont = VARINT32_LO_CONT_TABLE[nlz];
                var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
                BinaryUtils.writeLongLE(buffer, offset, scattered | loCont);
                return size;
            }

            return putVarInt32Slow(buffer, offset, value);
        }

        return putVarInt64LE(buffer, offset, value);
    }

    private static int putVarInt32Slow(ByteBuffer buffer, int offset, int value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                buffer.put(pos++, (byte) value);
                return pos - offset;
            }
            buffer.put(pos++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    private static int putVarInt64LE(ByteBuffer buffer, int offset, long value) {
        if (buffer.limit() - offset >= 16) {
            var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
            var size = VARINT64_SIZE_TABLE[nlz];
            var loCont = VARINT64_LO_CONT_TABLE[nlz];
            var hiCont = VARINT64_HI_CONT_TABLE[nlz];
            var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
            var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
            BinaryUtils.writeLongLE(buffer, offset, lo | loCont);
            BinaryUtils.writeLongLE(buffer, offset + 8, hi | hiCont);
            return size;
        }

        return putVarInt64Slow(buffer, offset, value);
    }

    private static int putVarInt64Slow(ByteBuffer buffer, int offset, long value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buffer.put(pos++, (byte) value);
                return pos - offset;
            }
            buffer.put(pos++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }
}
