package com.github.auties00.daedalus.protobuf.io.reader.binary;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.ProtobufDataType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.platform.BMI2;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

import java.lang.foreign.MemorySegment;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class ProtobufBinaryByteBufferReader extends ProtobufBinaryReader {
    private final ByteBuffer buffer;

    public ProtobufBinaryByteBufferReader(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.buffer = buffer.duplicate()
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public byte readRawByte() {
        try {
            return buffer.get();
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte[] readRawBytes(int size) {
        try {
            var result = new byte[size];
            buffer.get(result);
            return result;
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        } catch (NegativeArraySizeException _) {
            throw new IllegalArgumentException("size cannot be negative");
        }
    }

    @Override
    public ByteBuffer readRawBuffer(int size) {
        try {
            var position = buffer.position();
            var result = buffer.slice(position, size);
            buffer.position(position + size);
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) {
        try {
            var position = buffer.position();
            var result = buffer.slice(position, size);
            buffer.position(position + size);
            return MemorySegment.ofBuffer(result);
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.BYTE_BUFFER;
    }

    @Override
    public boolean isFinished() {
        return !buffer.hasRemaining();
    }

    @Override
    public void skipRawBytes(int size) {
        if (size >= 0) {
            try {
                buffer.position(buffer.position() + size);
            } catch (IllegalArgumentException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }
        throw ProtobufDeserializationException.negativeLength(size);
    }

    @Override
    public int readRawFixedInt32() {
        try {
            return buffer.getInt();
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawFixedInt64() {
        try {
            return buffer.getLong();
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public float readRawFloat() {
        try {
            return buffer.getFloat();
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public double readRawDouble() {
        try {
            return buffer.getDouble();
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ProtobufBinaryByteBufferReader readRawLengthDelimited(long size) {
        int bufferSize;
        try {
            bufferSize = Math.toIntExact(size);
        } catch (ArithmeticException _) {
            throw ProtobufDeserializationException.lengthDelimitedPropertyOverflow(size);
        }

        try {
            var position = buffer.position();
            var result = new ProtobufBinaryByteBufferReader(buffer.slice(position, bufferSize));
            buffer.position(position + bufferSize);
            return result;
        } catch (IndexOutOfBoundsException _) {
            if (size < 0) {
                throw ProtobufDeserializationException.negativeLength(size);
            } else {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }
    }

    @Override
    public void close() {

    }

    @Override
    public int readRawVarInt32() {
        if (BMI2.isHardwareSupported() && buffer.remaining() >= VARINT32_FAST_PATH_BYTES) {
            var position = buffer.position();
            var word = (long) BUFFER_AS_INT64_LE.get(buffer, position);
            var cont = ~word & VARINT32_CONT_BITS;
            var spread = cont ^ (cont - 1);
            var mask = spread & VARINT32_PAYLOAD_BITS;
            buffer.position(position + (Long.bitCount(spread) >>> 3));
            return (int) Long.compress(word, mask);
        }

        return getVarInt32Slow();
    }

    private int getVarInt32Slow() {
        try {
            int x;
            if ((x = buffer.get()) >= 0) {
                return x;
            } else if ((x ^= (buffer.get() << 7)) < 0) {
                return x ^ (~0 << 7);
            } else if ((x ^= (buffer.get() << 14)) >= 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((x ^= (buffer.get() << 21)) < 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else {
                x ^= buffer.get() << 28;
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
            }
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() {
        if (BMI2.isHardwareSupported() && buffer.remaining() >= VARINT64_FAST_PATH_BYTES) {
            var offset = buffer.position();
            var lo = (long) BUFFER_AS_INT64_LE.get(buffer, offset);
            var hi = (long) BUFFER_AS_INT64_LE.get(buffer, offset + 8);
            var loCont = ~lo & VARINT64_LO_CONT_BITS;
            var loContM1 = loCont - 1;
            var loSpread = loCont ^ loContM1;
            var loMask = loSpread & INT64_PEXT_MASK_LOW;
            var loResult = Long.compress(lo, loMask);
            var hiEnable = loContM1 >> 63;
            var hiCont = ~hi & VARINT64_HI_CONT_BITS;
            var hiSpread = (hiCont ^ (hiCont - 1)) & hiEnable;
            var hiMask = hiSpread & VARINT64_HI_PAYLOAD_BITS;
            var hiResult = Long.compress(hi, hiMask);
            buffer.position(offset + ((Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3));
            return loResult | (hiResult << 56);
        }

        return getVarInt64Slow();
    }

    private long getVarInt64Slow() {
        try {
            long x;
            int y;
            if ((y = buffer.get()) >= 0) {
                return y;
            } else if ((y ^= (buffer.get() << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (buffer.get() << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (buffer.get() << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) buffer.get() << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) buffer.get() << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) buffer.get() << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) buffer.get() << 49)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
            } else if ((x ^= ((long) buffer.get() << 56)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
            } else {
                x ^= ((long) buffer.get() << 63);
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
            }
            return x;
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public boolean readRawBool() {
        try {
            var pos = buffer.position();
            var x = buffer.get(pos);
            if (x >= 0) {
                buffer.position(pos + 1);
                return x != 0;
            }
        } catch (BufferUnderflowException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        return readRawVarInt64() != 0;
    }

    @Override
    public float[] readRawPackedFloat() {
        var length = readLengthDelimitedPropertyLength();
        var result = new float[length / Float.BYTES];
        readRawBuffer(length) // Zero copy
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(result);
        return result;
    }

    @Override
    public double[] readRawPackedDouble() {
        var length = readLengthDelimitedPropertyLength();
        var result = new double[length / Double.BYTES];
        readRawBuffer(length) // Zero copy
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer()
                .get(result);
        return result;
    }

    @Override
    public int[] readRawPackedVarInt32() {
        var length = readLengthDelimitedPropertyLength();
        var count = countVarInts(buffer.slice(buffer.position(), length));
        var end = buffer.position() + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (BMI2.isHardwareSupported() && buffer.hasArray()) {
            var arr = buffer.array();
            var off = buffer.arrayOffset() + buffer.position();
            var arrEnd = buffer.arrayOffset() + end;
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (arrEnd - off < 16) break;
                var bv = ByteVector.fromArray(B128, arr, off);
                var bitmask = (int) bv.compare(VectorOperators.LT, (byte) 0).toLong() & 0x3FF;
                var entry = VARINT32_2X_LOOKUP_STEP1[bitmask];
                var shuffleIdx = entry & 0xFF;
                var firstLen = (entry >>> 8) & 0xFF;
                var secondLen = (entry >>> 16) & 0xFF;
                var indices = ByteVector.fromArray(B128, VARINT32_2X_SHUFFLE_TABLE, shuffleIdx * 16);
                var shuffled = indices.selectFrom(bv);
                var lv = shuffled.reinterpretAsLongs();
                result[dst] = (int) Long.compress(lv.lane(0), VARINT32_PEXT_MASKS[firstLen]);
                result[dst + 1] = (int) Long.compress(lv.lane(1), VARINT32_PEXT_MASKS[secondLen]);
                off += firstLen + secondLen;
                dst += 2;
            }
            buffer.position(off - buffer.arrayOffset());
        }

        while (dst < count) {
            result[dst++] = readRawVarInt32();
        }

        buffer.position(end);
        return result;
    }

    @Override
    public int[] readRawPackedZigZagVarInt32() {
        var length = readLengthDelimitedPropertyLength();
        var count = countVarInts(buffer.slice(buffer.position(), length));
        var end = buffer.position() + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (BMI2.isHardwareSupported() && buffer.hasArray()) {
            var arr = buffer.array();
            var off = buffer.arrayOffset() + buffer.position();
            var arrEnd = buffer.arrayOffset() + end;
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (arrEnd - off < 16) break;
                var bv = ByteVector.fromArray(B128, arr, off);
                var bitmask = (int) bv.compare(VectorOperators.LT, (byte) 0).toLong() & 0x3FF;
                var entry = VARINT32_2X_LOOKUP_STEP1[bitmask];
                var shuffleIdx = entry & 0xFF;
                var firstLen = (entry >>> 8) & 0xFF;
                var secondLen = (entry >>> 16) & 0xFF;
                var indices = ByteVector.fromArray(B128, VARINT32_2X_SHUFFLE_TABLE, shuffleIdx * 16);
                var shuffled = indices.selectFrom(bv);
                var lv = shuffled.reinterpretAsLongs();
                var v0 = (int) Long.compress(lv.lane(0), VARINT32_PEXT_MASKS[firstLen]);
                var v1 = (int) Long.compress(lv.lane(1), VARINT32_PEXT_MASKS[secondLen]);
                result[dst] = (v0 >>> 1) ^ -(v0 & 1);
                result[dst + 1] = (v1 >>> 1) ^ -(v1 & 1);
                off += firstLen + secondLen;
                dst += 2;
            }
            buffer.position(off - buffer.arrayOffset());
        }

        while (dst < count) {
            result[dst++] = readRawZigZagVarInt32();
        }

        buffer.position(end);
        return result;
    }

    @Override
    public long[] readRawPackedVarInt64() {
        var length = readLengthDelimitedPropertyLength();
        var count = countVarInts(buffer.slice(buffer.position(), length));
        var end = buffer.position() + length;
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawVarInt64();
        }
        buffer.position(end);
        return result;
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() {
        var length = readLengthDelimitedPropertyLength();
        var count = countVarInts(buffer.slice(buffer.position(), length));
        var end = buffer.position() + length;
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawZigZagVarInt64();
        }
        buffer.position(end);
        return result;
    }

    @Override
    public boolean[] readRawPackedBool() {
        var length = readLengthDelimitedPropertyLength();
        var buffer = readRawBuffer(length);
        var resultsCount = countVarInts(buffer);
        var results = new boolean[resultsCount];
        var ptr = buffer.position();
        var end = buffer.limit();
        var dst = 0;
        while (end - ptr >= 8) {
            var word = (long) BUFFER_AS_INT64_LE.get(buffer, ptr);
            if ((word & MSB8) == 0) {
                // FIXME: Could be done in a single OP if the JDK had a replacement for Unsafe.putLong
                results[dst] = (byte) word != 0;
                results[dst + 1] = (byte) (word >>> 8) != 0;
                results[dst + 2] = (byte) (word >>> 16) != 0;
                results[dst + 3] = (byte) (word >>> 24) != 0;
                results[dst + 4] = (byte) (word >>> 32) != 0;
                results[dst + 5] = (byte) (word >>> 40) != 0;
                results[dst + 6] = (byte) (word >>> 48) != 0;
                results[dst + 7] = (byte) (word >>> 56) != 0;
                dst += 8;
                ptr += 8;
                continue;
            }
            ptr = readRawPackedBoolSlow(buffer, ptr, results, dst++);
        }
        while (ptr < end) {
            ptr = readRawPackedBoolSlow(buffer, ptr, results, dst++);
        }
        return results;
    }

    private static int readRawPackedBoolSlow(ByteBuffer buf, int ptr, boolean[] out, int dst) {
        var b = buf.get(ptr++);
        if (b >= 0) {
            out[dst] = b != 0;
            return ptr;
        }
        var nonZero = (b & 0x7F) != 0;
        while ((b = buf.get(ptr++)) < 0) {
            nonZero |= (b & 0x7F) != 0;
        }
        out[dst] = nonZero | (b != 0);
        return ptr;
    }

    @Override
    public int[] readRawPackedFixedInt32() {
        var length = readLengthDelimitedPropertyLength();
        var result = new int[length / Integer.BYTES];
        readRawBuffer(length) // Zero copy
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer()
                .get(result);
        return result;
    }

    @Override
    public long[] readRawPackedFixedInt64() {
        var length = readLengthDelimitedPropertyLength();
        var result = new long[length / Long.BYTES];
        readRawBuffer(length) // Zero copy
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer()
                .get(result);
        return result;
    }
}
