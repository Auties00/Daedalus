package it.auties.protobuf.io.reader;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.io.ProtobufDataType;
import it.auties.protobuf.platform.BMI2;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class ProtobufByteArrayReader extends ProtobufReader {
    private static final long MSB8 = 0x80808080_80808080L;

    private final byte[] buffer;
    private final int limit;
    private int offset;

    ProtobufByteArrayReader(byte[] buffer, int offset, int limit) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.checkFromToIndex(offset, limit, buffer.length);
        this.buffer = buffer;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public byte readRawByte() {
        if (offset < limit) {
            return buffer[offset++];
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public byte[] readRawBytes(int size) {
        try {
            var result = new byte[size];
            System.arraycopy(buffer, offset, result, 0, size);
            offset += size;
            return result;
        } catch (NegativeArraySizeException _) {
            throw ProtobufDeserializationException.negativeLength(size);
        } catch (IndexOutOfBoundsException error) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ByteBuffer readRawBuffer(int size) {
        try {
            var result = ByteBuffer.wrap(buffer, offset, size);
            offset += size;
            return result;
        } catch (IndexOutOfBoundsException error) {
            if (size < 0) {
                throw ProtobufDeserializationException.negativeLength(size);
            } else {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) {
        try {
            var result = MemorySegment.ofBuffer(ByteBuffer.wrap(buffer, offset, offset + size));
            offset += size;
            return result;
        } catch (IndexOutOfBoundsException error) {
            if (size < 0) {
                throw ProtobufDeserializationException.negativeLength(size);
            } else {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.BYTE_ARRAY;
    }

    @Override
    public boolean isFinished() {
        return offset >= limit;
    }

    @Override
    public void skipRawBytes(int size) {
        if (size >= 0) {
            offset += size;
            if (offset > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        throw ProtobufDeserializationException.negativeLength(size);
    }

    @Override
    public int readRawFixedInt32() {
        if (offset + Integer.BYTES <= limit) {
            var result = (int) ARRAY_AS_INT32_LE.get(buffer, offset);
            offset += Integer.BYTES;
            return result;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public long readRawFixedInt64() {
        if (offset + Long.BYTES <= limit) {
            var result = (long) ARRAY_AS_INT64_LE.get(buffer, offset);
            offset += Long.BYTES;
            return result;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public float readRawFloat() {
        if (offset + Float.BYTES <= limit) {
            var result = (int) ARRAY_AS_FLOAT_LE.get(buffer, offset);
            offset += Float.BYTES;
            return result;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public double readRawDouble() {
        if (offset + Double.BYTES <= limit) {
            var result = (long) ARRAY_AS_DOUBLE_LE.get(buffer, offset);
            offset += Double.BYTES;
            return result;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public ProtobufByteArrayReader readRawLengthDelimited(long size) {
        int bufferSize;
        try {
            bufferSize = Math.toIntExact(size);
        } catch (ArithmeticException _) {
            throw ProtobufDeserializationException.lengthDelimitedPropertyOverflow(size);
        }

        try {
            var result = new ProtobufByteArrayReader(buffer, offset, offset + bufferSize);
            offset += bufferSize;
            return result;
        } catch (IndexOutOfBoundsException _) {
            if (size < 0) {
                throw ProtobufDeserializationException.negativeLength(bufferSize);
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
        if (BMI2.isSupported() && buffer.length - offset >= VARINT32_FAST_PATH_BYTES) {
            var word = (long) ARRAY_AS_INT64_LE.get(buffer, offset);
            var cont = ~word & VARINT32_CONT_BITS;
            var spread = cont ^ (cont - 1);
            var mask = spread & VARINT32_PAYLOAD_BITS;
            offset += Long.bitCount(spread) >>> 3;
            return (int) Long.compress(word, mask);
        }

        return readRawVarInt32Slow();
    }

    private int readRawVarInt32Slow() {
        var unsafeResult = getVarInt32SlowUnsafe();
        if (offset <= limit) {
            return unsafeResult;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    private int getVarInt32SlowUnsafe() {
        try {
            int x;
            if ((x = buffer[offset++]) >= 0) {
                return x;
            } else if ((x ^= (buffer[offset++] << 7)) < 0) {
                return x ^ (~0 << 7);
            } else if ((x ^= (buffer[offset++] << 14)) >= 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((x ^= (buffer[offset++] << 21)) < 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else {
                x ^= buffer[offset++] << 28;
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
            }
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() {
        if (BMI2.isSupported() && buffer.length - offset >= VARINT64_FAST_PATH_BYTES) {
            var lo = (long) ARRAY_AS_INT64_LE.get(buffer, offset);
            var hi = (long) ARRAY_AS_INT64_LE.get(buffer, offset + Long.BYTES);
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
            offset += (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
            return loResult | (hiResult << 56);
        }

        return readRawVarInt64Slow();
    }

    private long readRawVarInt64Slow() {
        var unsafeResult = getVarInt64SlowUnsafe();
        if (offset <= limit) {
            return unsafeResult;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    private long getVarInt64SlowUnsafe() {
        try {
            long x;
            int y;
            if ((y = buffer[offset++]) >= 0) {
                return y;
            } else if ((y ^= (buffer[offset++] << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (buffer[offset++] << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (buffer[offset++] << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) buffer[offset++] << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) buffer[offset++] << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) buffer[offset++] << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) buffer[offset++] << 49)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
            } else if ((x ^= ((long) buffer[offset++] << 56)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
            } else {
                x ^= ((long) buffer[offset++] << 63);
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
            }
            return x;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public boolean readRawBool() {
        try {
            var x = buffer[offset];
            if (x >= 0) {
                offset++;
                return x != 0;
            }
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        return readRawVarInt64() != 0;
    }

    @Override
    public float[] readRawPackedFloat() {
        var length = readLengthDelimitedPropertyLength();
        var result = new float[length / Float.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(result);
        offset += length;
        return result;
    }

    @Override
    public double[] readRawPackedDouble() {
        var length = readLengthDelimitedPropertyLength();
        var result = new double[length / Double.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer()
                .get(result);
        offset += length;
        return result;
    }

    @Override
    public int[] readRawPackedVarInt32() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] readRawPackedZigZagVarInt32() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] readRawPackedVarInt64() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] readRawPackedBool() {
        var length = readLengthDelimitedPropertyLength();
        var resultsCount = countVarInts(buffer, offset, length);
        var results = new boolean[resultsCount];
        var ptr = offset;
        var end = offset + length;
        var dst = 0;
        while (end - ptr >= 8) {
            var word = (long) ARRAY_AS_INT64_LE.get(buffer, ptr);
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

    private static int readRawPackedBoolSlow(byte[] buf, int ptr, boolean[] out, int dst) {
        var b = buf[ptr++];
        if (b >= 0) {
            out[dst] = b != 0;
            return ptr;
        }
        var nonZero = (b & 0x7F) != 0;
        while ((b = buf[ptr++]) < 0) {
            nonZero |= (b & 0x7F) != 0;
        }
        out[dst] = nonZero | (b != 0);
        return ptr;
    }

    @Override
    public int[] readRawPackedFixedInt32() {
        var length = readLengthDelimitedPropertyLength();
        var result = new int[length / Integer.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer()
                .get(result);
        offset += length;
        return result;
    }

    @Override
    public long[] readRawPackedFixedInt64() {
        var length = readLengthDelimitedPropertyLength();
        var result = new long[length / Long.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer()
                .get(result);
        offset += length;
        return result;
    }
}
