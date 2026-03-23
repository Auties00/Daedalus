package it.auties.protobuf.io.writer;

import it.auties.protobuf.exception.ProtobufSerializationException;
import it.auties.protobuf.io.ProtobufDataType;

import java.lang.foreign.MemorySegment;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

final class ProtobufBinaryByteBufferWriter extends ProtobufBinaryWriter<ByteBuffer> {
    private final ByteBuffer buffer;

    ProtobufBinaryByteBufferWriter(ByteBuffer buffer) {
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
        var pos = buffer.position();
        buffer.position(pos + putVarInt32LE(buffer, pos, entry));
    }

    @Override
    public void writeRawVarInt64(long entry) {
        var pos = buffer.position();
        buffer.position(pos + putVarInt64LE(buffer, pos, entry));
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
            var pos = buffer.position();
            buffer.position(pos + putVarInt32LE(buffer, pos, value));
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        for (var value : input) {
            var pos = buffer.position();
            buffer.position(pos + putVarInt32LE(buffer, pos, (value << 1) ^ (value >> 31)));
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        for (var value : input) {
            var pos = buffer.position();
            buffer.position(pos + putVarInt64LE(buffer, pos, value));
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        for (var value : input) {
            var pos = buffer.position();
            buffer.position(pos + putVarInt64LE(buffer, pos, (value << 1) ^ (value >> 63)));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
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

    private static int putVarInt32LE(ByteBuffer buffer, int offset, int value) {
        if (value >= 0) {
            if (buffer.limit() - offset >= 8) {
                var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
                var size = VARINT32_SIZE_TABLE[nlz];
                var loCont = VARINT32_LO_CONT_TABLE[nlz];
                var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
                BUFFER_AS_INT64_LE.set(buffer, offset, scattered | loCont);
                return size;
            }

            return putVarInt32Slow(buffer, offset, value);
        }

        return putVarInt64LE(buffer, offset, value);
    }

    private static int putVarInt32Slow(ByteBuffer buffer, int offset, int value) {
        int pos = offset;
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
            BUFFER_AS_INT64_LE.set(buffer, offset, lo | loCont);
            BUFFER_AS_INT64_LE.set(buffer, offset + 8, hi | hiCont);
            return size;
        }

        return putVarInt64Slow(buffer, offset, value);
    }

    private static int putVarInt64Slow(ByteBuffer buffer, int offset, long value) {
        int pos = offset;
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
