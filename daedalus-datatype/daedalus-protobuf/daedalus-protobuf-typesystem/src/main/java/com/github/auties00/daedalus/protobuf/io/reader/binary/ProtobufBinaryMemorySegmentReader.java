package com.github.auties00.daedalus.protobuf.io.reader.binary;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.typesystem.util.CpuUtils;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ProtobufBinaryMemorySegmentReader extends ProtobufBinaryReader {

    private static final int MIN_SCRATCH_LENGTH = 64;
    private static final int MAX_SCRATCH_LENGTH = 65536;

    private final MemorySegment segment;
    private long position;
    // Off heap bytes are copied into this array before decoding a string: this was measured to be the fastest
    // correct strategy. It is inherited by length delimited readers, and never grows past MAX_SCRATCH_LENGTH.
    private byte[] scratch;

    public ProtobufBinaryMemorySegmentReader(MemorySegment segment) {
        this(segment, null);
    }

    private ProtobufBinaryMemorySegmentReader(MemorySegment segment, byte[] scratch) {
        Objects.requireNonNull(segment, "segment cannot be null");
        this.segment = segment;
        this.position = 0;
        this.scratch = scratch;
    }

    private void checkAvailable(long size) throws ProtobufDeserializationException {
        if (size < 0) {
            throw ProtobufDeserializationException.negativeLength(size);
        } else if (size > segment.byteSize() - position) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte readRawByte() throws ProtobufDeserializationException {
        try {
            var result = segment.get(ValueLayout.JAVA_BYTE, position);
            position++;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte[] readRawBytes(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = new byte[size];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position, result, 0, size);
        position += size;
        return result;
    }

    private byte[] scratch(int size) {
        if (size > MAX_SCRATCH_LENGTH) {
            return new byte[size];
        }
        var scratch = this.scratch;
        if (scratch == null || scratch.length < size) {
            scratch = this.scratch = new byte[Math.min(Math.max(size, MIN_SCRATCH_LENGTH), MAX_SCRATCH_LENGTH)];
        }
        return scratch;
    }

    @Override
    public String readRawString(int size, boolean validateUtf8) throws ProtobufDeserializationException {
        checkAvailable(size);
        var scratch = scratch(size);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position, scratch, 0, size);
        if (validateUtf8 && !CharsetUtils.isValidUtf8(scratch, 0, size)) {
            throw ProtobufDeserializationException.invalidUtf8();
        }
        position += size;
        return new String(scratch, 0, size, StandardCharsets.UTF_8);
    }

    @Override
    public ByteBuffer readRawBuffer(int size) throws ProtobufDeserializationException {
        return readRawMemorySegment(size)
                .asByteBuffer();
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = segment.asSlice(position, size);
        position += size;
        return result;
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.MEMORY_SEGMENT;
    }

    @Override
    public boolean isFinished() {
        return position >= segment.byteSize();
    }

    @Override
    public void skipRawBytes(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        position += size;
    }

    @Override
    public int readRawFixedInt32() throws ProtobufDeserializationException {
        try {
            var result = BinaryUtils.readIntLE(segment, position);
            position += Integer.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawFixedInt64() throws ProtobufDeserializationException {
        try {
            var result = BinaryUtils.readLongLE(segment, position);
            position += Long.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public float readRawFloat() throws ProtobufDeserializationException {
        try {
            var result = BinaryUtils.readFloatLE(segment, position);
            position += Float.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public double readRawDouble() throws ProtobufDeserializationException {
        try {
            var result = BinaryUtils.readDoubleLE(segment, position);
            position += Double.BYTES;
            return result;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public ProtobufBinaryMemorySegmentReader readRawLengthDelimited(long size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = new ProtobufBinaryMemorySegmentReader(segment.asSlice(position, size), scratch);
        position += size;
        return result;
    }

    @Override
    public void close() {

    }

    @Override
    public int readRawVarInt32() throws ProtobufDeserializationException {
        if (CpuUtils.isFastBmi2Supported() && segment.byteSize() - position >= VARINT32_FAST_PATH_BYTES) {
            var word = BinaryUtils.readLongLE(segment, position);
            var cont = ~word & VARINT32_CONT_BITS;
            // A varint32 longer than five bytes is a sign extended negative int32: decode it as a varint64
            if (cont != 0) {
                var spread = cont ^ (cont - 1);
                position += Long.bitCount(spread) >>> 3;
                return (int) Long.compress(word, spread & VARINT32_PAYLOAD_BITS);
            }
        }

        return getVarInt32Slow();
    }

    private int getVarInt32Slow() throws ProtobufDeserializationException {
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
                var y = segment.get(ValueLayout.JAVA_BYTE, position++);
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                // Negative int32 values are sign extended to ten bytes: the upper bytes are discarded
                if (y < 0
                        && segment.get(ValueLayout.JAVA_BYTE, position++) < 0
                        && segment.get(ValueLayout.JAVA_BYTE, position++) < 0
                        && segment.get(ValueLayout.JAVA_BYTE, position++) < 0
                        && segment.get(ValueLayout.JAVA_BYTE, position++) < 0
                        && segment.get(ValueLayout.JAVA_BYTE, position++) < 0) {
                    throw ProtobufDeserializationException.malformedVarInt();
                }
                return x;
            }
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() throws ProtobufDeserializationException {
        if (CpuUtils.isFastBmi2Supported() && segment.byteSize() - position >= VARINT64_FAST_PATH_BYTES) {
            var lo = BinaryUtils.readLongLE(segment, position);
            var hi = BinaryUtils.readLongLE(segment, position + Long.BYTES);
            var loCont = ~lo & VARINT64_LO_CONT_BITS;
            var hiCont = ~hi & VARINT64_HI_CONT_BITS;
            if (loCont == 0 && hiCont == 0) {
                throw ProtobufDeserializationException.malformedVarInt();
            }
            var loContM1 = loCont - 1;
            var loSpread = loCont ^ loContM1;
            var loMask = loSpread & INT64_PEXT_MASK_LOW;
            var loResult = Long.compress(lo, loMask);
            // The high word only continues the varint if the low word has no terminator: loCont is then zero, while its sign bit alone is not enough
            var hiEnable = ~((loCont | -loCont) >> 63);
            var hiSpread = (hiCont ^ (hiCont - 1)) & hiEnable;
            var hiMask = hiSpread & VARINT64_HI_PAYLOAD_BITS;
            var hiResult = Long.compress(hi, hiMask);
            position += (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
            return loResult | (hiResult << 56);
        }

        return getVarInt64Slow();
    }

    private long getVarInt64Slow() throws ProtobufDeserializationException {
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
                var last = segment.get(ValueLayout.JAVA_BYTE, position++);
                if (last < 0) {
                    throw ProtobufDeserializationException.malformedVarInt();
                }
                x ^= ((long) last << 63);
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
            }
            return x;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public boolean readRawBool() throws ProtobufDeserializationException {
        try {
            var x = segment.get(ValueLayout.JAVA_BYTE, position);
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
    public float[] readRawPackedFloat() throws ProtobufDeserializationException {
        var length = readPackedFixedLength(Float.BYTES);
        var result = new float[length / Float.BYTES];
        BinaryUtils.readFloatsLE(segment, position, result, 0, result.length);
        position += length;
        return result;
    }

    @Override
    public double[] readRawPackedDouble() throws ProtobufDeserializationException {
        var length = readPackedFixedLength(Double.BYTES);
        var result = new double[length / Double.BYTES];
        BinaryUtils.readDoublesLE(segment, position, result, 0, result.length);
        position += length;
        return result;
    }

    private int readPackedFixedLength(int elementSize) throws ProtobufDeserializationException {
        var length = readLengthDelimitedFieldLength();
        checkAvailable(length);
        if (length % elementSize != 0) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        return length;
    }

    private int readPackedVarIntLength() throws ProtobufDeserializationException {
        var length = readLengthDelimitedFieldLength();
        checkAvailable(length);
        return length;
    }

    // Every varint in a packed field must end exactly at the end of the field
    private void checkPackedEnd(long end) throws ProtobufDeserializationException {
        if (position != end) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public int[] readRawPackedVarInt32() throws ProtobufDeserializationException {
        var length = readPackedVarIntLength();
        var count = countVarInts(segment, position, length);
        var end = position + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (CpuUtils.isFastBmi2Supported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - position < 16) break;
                var bv = ByteVector.fromMemorySegment(B128, segment, position, ByteOrder.LITTLE_ENDIAN);
                var bitmask = (int) bv.compare(VectorOperators.LT, (byte) 0).toLong() & 0x3FF;
                var entry = VARINT32_2X_LOOKUP_STEP1[bitmask];
                var shuffleIdx = entry & 0xFF;
                var firstLen = (entry >>> 8) & 0xFF;
                var secondLen = (entry >>> 16) & 0xFF;
                // The bitmask only covers ten bytes: longer pairs are decoded by the scalar path
                if (firstLen + secondLen > 10) break;
                var indices = ByteVector.fromArray(B128, VARINT32_2X_SHUFFLE_TABLE, shuffleIdx * 16);
                var shuffled = indices.selectFrom(bv);
                var lv = shuffled.reinterpretAsLongs();
                result[dst] = (int) Long.compress(lv.lane(0), VARINT32_PEXT_MASKS[firstLen]);
                result[dst + 1] = (int) Long.compress(lv.lane(1), VARINT32_PEXT_MASKS[secondLen]);
                position += firstLen + secondLen;
                dst += 2;
            }
        }

        while (dst < count) {
            result[dst++] = readRawVarInt32();
        }

        checkPackedEnd(end);
        return result;
    }

    @Override
    public int[] readRawPackedZigZagVarInt32() throws ProtobufDeserializationException {
        var length = readPackedVarIntLength();
        var count = countVarInts(segment, position, length);
        var end = position + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (CpuUtils.isFastBmi2Supported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - position < 16) break;
                var bv = ByteVector.fromMemorySegment(B128, segment, position, ByteOrder.LITTLE_ENDIAN);
                var bitmask = (int) bv.compare(VectorOperators.LT, (byte) 0).toLong() & 0x3FF;
                var entry = VARINT32_2X_LOOKUP_STEP1[bitmask];
                var shuffleIdx = entry & 0xFF;
                var firstLen = (entry >>> 8) & 0xFF;
                var secondLen = (entry >>> 16) & 0xFF;
                // The bitmask only covers ten bytes: longer pairs are decoded by the scalar path
                if (firstLen + secondLen > 10) break;
                var indices = ByteVector.fromArray(B128, VARINT32_2X_SHUFFLE_TABLE, shuffleIdx * 16);
                var shuffled = indices.selectFrom(bv);
                var lv = shuffled.reinterpretAsLongs();
                var v0 = (int) Long.compress(lv.lane(0), VARINT32_PEXT_MASKS[firstLen]);
                var v1 = (int) Long.compress(lv.lane(1), VARINT32_PEXT_MASKS[secondLen]);
                result[dst] = (v0 >>> 1) ^ -(v0 & 1);
                result[dst + 1] = (v1 >>> 1) ^ -(v1 & 1);
                position += firstLen + secondLen;
                dst += 2;
            }
        }

        while (dst < count) {
            result[dst++] = readRawZigZagVarInt32();
        }

        checkPackedEnd(end);
        return result;
    }

    @Override
    public long[] readRawPackedVarInt64() throws ProtobufDeserializationException {
        var length = readPackedVarIntLength();
        var count = countVarInts(segment, position, length);
        var end = position + length;
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawVarInt64();
        }
        checkPackedEnd(end);
        return result;
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() throws ProtobufDeserializationException {
        var length = readPackedVarIntLength();
        var count = countVarInts(segment, position, length);
        var end = position + length;
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawZigZagVarInt64();
        }
        checkPackedEnd(end);
        return result;
    }

    @Override
    public boolean[] readRawPackedBool() throws ProtobufDeserializationException {
        var length = readPackedVarIntLength();
        var segment = readRawMemorySegment(length);
        var resultsCount = countVarInts(segment, 0, length);
        var results = new boolean[resultsCount];
        var ptr = 0L;
        var dst = 0;
        try {
            while (length - ptr >= 8) {
                var word = BinaryUtils.readLongLE(segment, ptr);
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
            while (ptr < length) {
                ptr = readRawPackedBoolSlow(segment, ptr, results, dst++);
            }
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        return results;
    }

    private static long readRawPackedBoolSlow(MemorySegment segment, long ptr, boolean[] out, int dst) {
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
    public int[] readRawPackedFixedInt32() throws ProtobufDeserializationException {
        var length = readPackedFixedLength(Integer.BYTES);
        var result = new int[length / Integer.BYTES];
        BinaryUtils.readIntsLE(segment, position, result, 0, result.length);
        position += length;
        return result;
    }

    @Override
    public long[] readRawPackedFixedInt64() throws ProtobufDeserializationException {
        var length = readPackedFixedLength(Long.BYTES);
        var result = new long[length / Long.BYTES];
        BinaryUtils.readLongsLE(segment, position, result, 0, result.length);
        position += length;
        return result;
    }

    private static int countVarInts(MemorySegment segment, long offset, long length) {
        var count = 0;
        long ptr = offset, end = offset + length;

        while (end - ptr >= Long.BYTES) {
            count += Long.bitCount(~BinaryUtils.readLongLE(segment, ptr) & MSB8);
            ptr += Long.BYTES;
        }

        while (ptr < end) {
            if ((segment.get(ValueLayout.JAVA_BYTE, ptr++) & 0x80) == 0) {
                count++;
            }
        }

        return count;
    }
}
