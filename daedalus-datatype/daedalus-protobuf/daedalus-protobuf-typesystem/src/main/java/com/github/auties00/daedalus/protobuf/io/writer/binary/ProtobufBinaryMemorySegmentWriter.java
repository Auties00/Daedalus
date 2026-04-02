package com.github.auties00.daedalus.protobuf.io.writer.binary;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.ProtobufIODataType;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public final class ProtobufBinaryMemorySegmentWriter extends ProtobufBinaryWriter<MemorySegment> {
    private final MemorySegment memorySegment;
    private long position;

    public ProtobufBinaryMemorySegmentWriter(MemorySegment memorySegment) {
        this.memorySegment = memorySegment;
    }

    @Override
    public void writeRawByte(byte entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_BYTE, position, entry);
            position++;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) {
        try {
            MemorySegment.copy(
                    entry,
                    offset,
                    memorySegment,
                    ValueLayout.JAVA_BYTE,
                    position,
                    length
            );
            position += length;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) {
        writeRawMemorySegment(MemorySegment.ofBuffer(entry));
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) {
        try {
            var length = entry.byteSize();
            MemorySegment.copy(
                    entry,
                    0,
                    memorySegment,
                    position,
                    length
            );
            position += length;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt32(int entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_INT_UNALIGNED, position, entry);
            position += Integer.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, position, entry);
            position += Long.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, position, entry);
            position += Float.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, position, entry);
            position += Double.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt32(int entry) {
        position += putVarInt32LE(memorySegment, position, entry);
    }

    @Override
    public void writeRawVarInt64(long entry) {
        position += putVarInt64LE(memorySegment, position, entry);
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
            position += putVarInt32LE(memorySegment, position, value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        for (var value : input) {
            position += putVarInt32LE(memorySegment, position, (value << 1) ^ (value >> 31));
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        for (var value : input) {
            position += putVarInt64LE(memorySegment, position, value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        for (var value : input) {
            position += putVarInt64LE(memorySegment, position, (value << 1) ^ (value >> 63));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
    }

    @Override
    public MemorySegment toOutput() {
        return memorySegment;
    }

    @Override
    public ProtobufIODataType rawDataTypePreference() {
        return ProtobufIODataType.MEMORY_SEGMENT;
    }

    @Override
    public void close() {

    }

    private static int putVarInt32LE(MemorySegment segment, long offset, int value) {
        if (value >= 0) {
            if (segment.byteSize() - offset >= 8) {
                var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
                var size = VARINT32_SIZE_TABLE[nlz];
                var loCont = VARINT32_LO_CONT_TABLE[nlz];
                var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
                segment.set(INT64_LE_LAYOUT, offset, scattered | loCont);
                return size;
            }

            return putVarInt32Slow(segment, offset, value);
        }

        return putVarInt64LE(segment, offset, value);
    }

    private static int putVarInt32Slow(MemorySegment segment, long offset, int value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) value);
                return (int) (pos - offset);
            }
            segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    private static int putVarInt64LE(MemorySegment segment, long offset, long value) {
        if (segment.byteSize() - offset >= 16) {
            var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
            var size = VARINT64_SIZE_TABLE[nlz];
            var loCont = VARINT64_LO_CONT_TABLE[nlz];
            var hiCont = VARINT64_HI_CONT_TABLE[nlz];
            var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
            var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
            segment.set(INT64_LE_LAYOUT, offset, lo | loCont);
            segment.set(INT64_LE_LAYOUT, offset + 8, hi | hiCont);
            return size;
        }

        return putVarInt64Slow(segment, offset, value);
    }

    private static int putVarInt64Slow(MemorySegment segment, long offset, long value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) value);
                return (int) (pos - offset);
            }
            segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }
}
