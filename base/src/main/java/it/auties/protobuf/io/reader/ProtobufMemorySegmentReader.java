package it.auties.protobuf.io.reader;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.io.ProtobufDataType;
import it.auties.protobuf.platform.BMI2;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

final class ProtobufMemorySegmentReader extends ProtobufReader {
    private static final ValueLayout.OfLong VARINT64_FAST_PATH_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED
            .withOrder(ByteOrder.LITTLE_ENDIAN);

    private final MemorySegment segment;
    private long position;

    ProtobufMemorySegmentReader(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment cannot be null");
        this.segment = segment;
        this.position = 0;
    }

    @Override
    public byte readRawByte() {
        try {
            var result = segment.get(ValueLayout.OfByte.JAVA_BYTE, position);
            position++;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte[] readRawBytes(int size) {
        try {
            var result = segment.asSlice(position, position + size)
                    .toArray(ValueLayout.OfByte.JAVA_BYTE);
            position += size;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        } catch (NegativeArraySizeException _) {
            throw new IllegalArgumentException("size cannot be negative");
        }
    }

    @Override
    public ByteBuffer readRawBuffer(int size) {
        try {
            var result = segment.asSlice(position, position + size);
            position += size;
            return result.asByteBuffer();
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) {
        try {
            var result = segment.asSlice(position, position + size);
            position += size;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.MEMORY_SEGMENT;
    }

    @Override
    public boolean isFinished() {
        return position >= segment.byteSize();
    }

    @Override
    public void skipRawBytes(int size) {
        position += size;
        if (position > size) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public int readRawFixedInt32() {
        try {
            var result = segment.getAtIndex(ValueLayout.OfInt.JAVA_INT_UNALIGNED, position);
            position += Integer.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawFixedInt64() {
        try {
            var result = segment.getAtIndex(ValueLayout.OfLong.JAVA_LONG_UNALIGNED, position);
            position += Long.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public float readRawFloat() {
        try {
            var result = segment.getAtIndex(ValueLayout.OfInt.JAVA_FLOAT_UNALIGNED, position);
            position += Float.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public double readRawDouble() {
        try {
            var result = segment.getAtIndex(ValueLayout.OfInt.JAVA_DOUBLE_UNALIGNED, position);
            position += Double.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ProtobufMemorySegmentReader readRawLengthDelimited(long size) {
        try {
            var result = new ProtobufMemorySegmentReader(segment.asSlice(position, size));
            position += size;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public int readRawVarInt32() {
        if (BMI2.isSupported() && segment.byteSize() - position >= VARINT32_FAST_PATH_BYTES) {
            var word = segment.get(VARINT64_FAST_PATH_LAYOUT, position);
            var cont = ~word & VARINT32_CONT_BITS;
            var spread = cont ^ (cont - 1);
            var mask = spread & VARINT32_PAYLOAD_BITS;
            position += Long.bitCount(spread) >>> 3;
            return (int) Long.compress(word, mask);
        }

        return getVarInt32Slow();
    }

    private int getVarInt32Slow() {
        try {
            int x;
            if ((x = segment.get(ValueLayout.JAVA_BYTE, position++)) >= 0) {
                return x;
            } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 7)) < 0) {
                return x ^ (~0 << 7);
            } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 14)) >= 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 21)) < 0) {
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else {
                x ^= segment.get(ValueLayout.JAVA_BYTE, position++) << 28;
                return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
            }
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() {
        if (BMI2.isSupported() && segment.byteSize() - position >= VARINT64_FAST_PATH_BYTES) {
            var lo = segment.get(VARINT64_FAST_PATH_LAYOUT, position);
            var hi = segment.get(VARINT64_FAST_PATH_LAYOUT, position + Long.BYTES);
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
            position += (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
            return loResult | (hiResult << 56);
        }

        return getVarInt64Slow();
    }

    private long getVarInt64Slow() {
        try {
            long x;
            int y;
            if ((y = segment.get(ValueLayout.JAVA_BYTE, position++)) >= 0) {
                return y;
            } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, position++) << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 49)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
            } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 56)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
            } else {
                x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, position++) << 63);
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
            var x = segment.get(ValueLayout.OfByte.JAVA_BYTE, position);
            if (x >= 0) {
                position++;
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
        var segment = readRawMemorySegment(length);
        var resultsCount = countVarInts(segment);
        var results = new boolean[resultsCount];
        var ptr = 0;
        var end = segment.byteSize();
        var dst = 0;
        while (end - ptr >= 8) {
            var word = (long) BUFFER_AS_INT64_LE.get(segment, ptr);
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
            ptr = readRawPackedBoolSlow(segment, ptr, results, dst++);
        }
        while (ptr < end) {
            ptr = readRawPackedBoolSlow(segment, ptr, results, dst++);
        }
        return results;
    }

    private static int readRawPackedBoolSlow(MemorySegment segment, int ptr, boolean[] out, int dst) {
        var b = segment.get(ValueLayout.JAVA_BYTE, ptr++);
        if (b >= 0) {
            out[dst] = b != 0;
            return ptr;
        }
        var nonZero = (b & 0x7F) != 0;
        while ((b = segment.get(ValueLayout.JAVA_BYTE, ptr++)) < 0) {
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

    private static int countVarInts(MemorySegment seg) {
        var base = seg.heapBase();
        return base.isPresent() && base.get() instanceof byte[] arr
                ? countVarInts(arr, 0, arr.length)
                : countVarInts(seg.asByteBuffer());
    }
}
