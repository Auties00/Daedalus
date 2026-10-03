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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class ProtobufBinaryByteArrayReader extends ProtobufBinaryReader {
    private final byte[] buffer;
    private final int limit;
    private int offset;

    public ProtobufBinaryByteArrayReader(byte[] buffer, int offset, int limit) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.checkFromToIndex(offset, limit, buffer.length);
        this.buffer = buffer;
        this.offset = offset;
        this.limit = limit;
    }

    private void checkAvailable(long size) throws ProtobufDeserializationException {
        if (size < 0) {
            throw ProtobufDeserializationException.negativeLength(size);
        } else if (size > limit - offset) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte readRawByte() throws ProtobufDeserializationException {
        if (offset < limit) {
            return buffer[offset++];
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    @Override
    public byte[] readRawBytes(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = Arrays.copyOfRange(buffer, offset, offset + size);
        offset += size;
        return result;
    }

    @Override
    public String readRawString(int size, boolean validateUtf8) throws ProtobufDeserializationException {
        checkAvailable(size);
        if (validateUtf8 && !CharsetUtils.isValidUtf8(buffer, offset, size)) {
            throw ProtobufDeserializationException.invalidUtf8();
        }
        var result = new String(buffer, offset, size, StandardCharsets.UTF_8);
        offset += size;
        return result;
    }

    @Override
    public ByteBuffer readRawBuffer(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = ByteBuffer.wrap(buffer, offset, size)
                .slice();
        offset += size;
        return result;
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var result = MemorySegment.ofArray(buffer)
                .asSlice(offset, size);
        offset += size;
        return result;
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.BYTE_ARRAY;
    }

    @Override
    public boolean isFinished() {
        return offset >= limit;
    }

    int remaining() {
        return limit - offset;
    }

    @Override
    public void skipRawBytes(int size) throws ProtobufDeserializationException {
        checkAvailable(size);
        offset += size;
    }

    @Override
    public int readRawFixedInt32() throws ProtobufDeserializationException {
        checkAvailable(Integer.BYTES);
        var result = BinaryUtils.readIntLE(buffer, offset);
        offset += Integer.BYTES;
        return result;
    }

    @Override
    public long readRawFixedInt64() throws ProtobufDeserializationException {
        checkAvailable(Long.BYTES);
        var result = BinaryUtils.readLongLE(buffer, offset);
        offset += Long.BYTES;
        return result;
    }

    @Override
    public float readRawFloat() throws ProtobufDeserializationException {
        checkAvailable(Float.BYTES);
        var result = BinaryUtils.readFloatLE(buffer, offset);
        offset += Float.BYTES;
        return result;
    }

    @Override
    public double readRawDouble() throws ProtobufDeserializationException {
        checkAvailable(Double.BYTES);
        var result = BinaryUtils.readDoubleLE(buffer, offset);
        offset += Double.BYTES;
        return result;
    }

    @Override
    public ProtobufBinaryByteArrayReader readRawLengthDelimited(long size) throws ProtobufDeserializationException {
        checkAvailable(size);
        var bufferSize = (int) size;
        var result = new ProtobufBinaryByteArrayReader(buffer, offset, offset + bufferSize);
        offset += bufferSize;
        return result;
    }

    @Override
    public void close() {

    }

    @Override
    public int readRawVarInt32() throws ProtobufDeserializationException {
        // The fast path may read past the limit, but never past the array: the limit is checked after decoding
        if (CpuUtils.isFastBmi2Supported() && buffer.length - offset >= VARINT32_FAST_PATH_BYTES) {
            var word = BinaryUtils.readLongLE(buffer, offset);
            var cont = ~word & VARINT32_CONT_BITS;
            // A varint32 longer than five bytes is a sign extended negative int32: decode it as a varint64
            if (cont != 0) {
                var spread = cont ^ (cont - 1);
                var end = offset + (Long.bitCount(spread) >>> 3);
                if (end > limit) {
                    throw ProtobufDeserializationException.truncatedMessage();
                }
                offset = end;
                return (int) Long.compress(word, spread & VARINT32_PAYLOAD_BITS);
            }
        }

        return readRawVarInt32Slow();
    }

    private int readRawVarInt32Slow() throws ProtobufDeserializationException {
        var unsafeResult = getVarInt32SlowUnsafe();
        if (offset <= limit) {
            return unsafeResult;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    private int getVarInt32SlowUnsafe() throws ProtobufDeserializationException {
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
                var y = buffer[offset++];
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                // Negative int32 values are sign extended to ten bytes: the upper bytes are discarded
                if (y < 0
                        && buffer[offset++] < 0
                        && buffer[offset++] < 0
                        && buffer[offset++] < 0
                        && buffer[offset++] < 0
                        && buffer[offset++] < 0) {
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
        // The fast path may read past the limit, but never past the array: the limit is checked after decoding
        if (CpuUtils.isFastBmi2Supported() && buffer.length - offset >= VARINT64_FAST_PATH_BYTES) {
            var lo = BinaryUtils.readLongLE(buffer, offset);
            var hi = BinaryUtils.readLongLE(buffer, offset + Long.BYTES);
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
            var end = offset + ((Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3);
            if (end > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
            offset = end;
            return loResult | (hiResult << 56);
        }

        return readRawVarInt64Slow();
    }

    private long readRawVarInt64Slow() throws ProtobufDeserializationException {
        var unsafeResult = getVarInt64SlowUnsafe();
        if (offset <= limit) {
            return unsafeResult;
        }

        throw ProtobufDeserializationException.truncatedMessage();
    }

    private long getVarInt64SlowUnsafe() throws ProtobufDeserializationException {
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
                var last = buffer[offset++];
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
        if (offset >= limit) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        var x = buffer[offset];
        if (x >= 0) {
            offset++;
            return x != 0;
        }

        return readRawVarInt64() != 0;
    }

    @Override
    public float[] readRawPackedFloat() throws ProtobufDeserializationException {
        return readRawPackedFloat(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    float[] readRawPackedFloat(int length) throws ProtobufDeserializationException {
        checkPackedFixedLength(length, Float.BYTES);
        var result = new float[length / Float.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(result);
        offset += length;
        return result;
    }

    @Override
    public double[] readRawPackedDouble() throws ProtobufDeserializationException {
        return readRawPackedDouble(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    double[] readRawPackedDouble(int length) throws ProtobufDeserializationException {
        checkPackedFixedLength(length, Double.BYTES);
        var result = new double[length / Double.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer()
                .get(result);
        offset += length;
        return result;
    }

    private void checkPackedFixedLength(int length, int elementSize) throws ProtobufDeserializationException {
        checkAvailable(length);
        if (length % elementSize != 0) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    // Every varint in a packed field must end exactly at the end of the field
    private void checkPackedEnd(int end) throws ProtobufDeserializationException {
        if (offset != end) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public int[] readRawPackedVarInt32() throws ProtobufDeserializationException {
        return readRawPackedVarInt32(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    int[] readRawPackedVarInt32(int length) throws ProtobufDeserializationException {
        checkAvailable(length);
        var count = countVarInts(buffer, offset, length);
        var result = new int[count];
        var end = offset + length;
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (CpuUtils.isFastBmi2Supported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - offset < 16) break;
                var bv = ByteVector.fromArray(B128, buffer, offset);
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
                offset += firstLen + secondLen;
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
        return readRawPackedZigZagVarInt32(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    int[] readRawPackedZigZagVarInt32(int length) throws ProtobufDeserializationException {
        checkAvailable(length);
        var count = countVarInts(buffer, offset, length);
        var result = new int[count];
        var end = offset + length;
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (CpuUtils.isFastBmi2Supported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - offset < 16) break;
                var bv = ByteVector.fromArray(B128, buffer, offset);
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
                offset += firstLen + secondLen;
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
        return readRawPackedVarInt64(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    long[] readRawPackedVarInt64(int length) throws ProtobufDeserializationException {
        checkAvailable(length);
        var count = countVarInts(buffer, offset, length);
        var result = new long[count];
        var end = offset + length;
        for (var i = 0; i < count; i++) {
            result[i] = readRawVarInt64();
        }
        checkPackedEnd(end);
        return result;
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() throws ProtobufDeserializationException {
        return readRawPackedZigZagVarInt64(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    long[] readRawPackedZigZagVarInt64(int length) throws ProtobufDeserializationException {
        checkAvailable(length);
        var count = countVarInts(buffer, offset, length);
        var result = new long[count];
        var end = offset + length;
        for (var i = 0; i < count; i++) {
            result[i] = readRawZigZagVarInt64();
        }
        checkPackedEnd(end);
        return result;
    }

    @Override
    public boolean[] readRawPackedBool() throws ProtobufDeserializationException {
        return readRawPackedBool(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    boolean[] readRawPackedBool(int length) throws ProtobufDeserializationException {
        checkAvailable(length);
        var resultsCount = countVarInts(buffer, offset, length);
        var results = new boolean[resultsCount];
        var ptr = offset;
        var end = offset + length;
        var dst = 0;
        try {
            while (end - ptr >= 8) {
                var word = BinaryUtils.readLongLE(buffer, ptr);
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
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        offset = ptr;
        checkPackedEnd(end);
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
    public int[] readRawPackedFixedInt32() throws ProtobufDeserializationException {
        return readRawPackedFixedInt32(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    int[] readRawPackedFixedInt32(int length) throws ProtobufDeserializationException {
        checkPackedFixedLength(length, Integer.BYTES);
        var result = new int[length / Integer.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer()
                .get(result);
        offset += length;
        return result;
    }

    @Override
    public long[] readRawPackedFixedInt64() throws ProtobufDeserializationException {
        return readRawPackedFixedInt64(readLengthDelimitedFieldLength());
    }

    // Decodes a packed payload of the given length: also used by the stream reader on its buffered bytes
    long[] readRawPackedFixedInt64(int length) throws ProtobufDeserializationException {
        checkPackedFixedLength(length, Long.BYTES);
        var result = new long[length / Long.BYTES];
        ByteBuffer.wrap(buffer, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer()
                .get(result);
        offset += length;
        return result;
    }
}
