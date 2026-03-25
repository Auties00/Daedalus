package com.github.auties00.daedalus.protobuf.io.reader.binary;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.ProtobufIODataType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.platform.BMI2;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class ProtobufBinaryStreamReader extends ProtobufBinaryReader {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream inputStream;
    private final boolean autoclose;
    private long position;
    private final long limit;
    private boolean finished;

    private final byte[] buffer;
    private int bufferPosition;
    private int bufferLimit;

    private ProtobufBinaryStreamReader lengthDelimitedReader;

    public ProtobufBinaryStreamReader(InputStream inputStream, long limit, boolean autoclose) {
        this(inputStream, limit, autoclose, DEFAULT_BUFFER_SIZE);
    }

    public ProtobufBinaryStreamReader(InputStream inputStream, long limit, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        if (bufferSize < VARINT64_FAST_PATH_BYTES) {
            throw new IllegalArgumentException("Buffer size must be at least " + VARINT64_FAST_PATH_BYTES);
        }
        this.inputStream = inputStream;
        this.autoclose = autoclose;
        this.limit = limit;
        this.buffer = new byte[bufferSize];
    }

    private ProtobufBinaryStreamReader(InputStream inputStream, long limit, byte[] buffer, int bufferPosition, int bufferLimit, long position) {
        this.inputStream = inputStream;
        this.autoclose = false;
        this.limit = limit;
        this.buffer = buffer;
        this.bufferPosition = bufferPosition;
        this.bufferLimit = bufferLimit;
        this.position = position;
    }

    private boolean tryEnsureAvailable(int needed) {
        if (bufferLimit - bufferPosition >= needed) {
            return true;
        }
        return tryRefillBuffer(needed);
    }

    private boolean tryRefillBuffer(int needed) {
        var remaining = bufferLimit - bufferPosition;
        if (remaining > 0) {
            System.arraycopy(buffer, bufferPosition, buffer, 0, remaining);
        }
        bufferPosition = 0;
        bufferLimit = remaining;

        while (bufferLimit < needed) {
            try {
                var read = inputStream.read(buffer, bufferLimit, buffer.length - bufferLimit);
                if (read == -1) {
                    return false;
                }
                bufferLimit += read;
            } catch (IOException e) {
                throw ProtobufDeserializationException.truncatedMessage(e);
            }
        }
        return true;
    }

    @Override
    public byte readRawByte() {
        assertReadable();
        if (!tryEnsureAvailable(1)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        position++;
        return buffer[bufferPosition++];
    }

    @Override
    public byte[] readRawBytes(int size) {
        assertReadable();

        byte[] result;
        try {
            result = new byte[size];
        } catch (NegativeArraySizeException _) {
            throw ProtobufDeserializationException.negativeLength(size);
        }

        var resultOffset = 0;

        // Drain buffer first
        var available = bufferLimit - bufferPosition;
        if (available > 0) {
            var toCopy = Math.min(available, size);
            System.arraycopy(buffer, bufferPosition, result, 0, toCopy);
            bufferPosition += toCopy;
            position += toCopy;
            resultOffset += toCopy;
        }

        // Read remainder directly from stream (bypass buffer)
        while (resultOffset < size) {
            try {
                var read = inputStream.read(result, resultOffset, size - resultOffset);
                if (read == -1) {
                    throw ProtobufDeserializationException.truncatedMessage();
                }
                resultOffset += read;
                position += read;
            } catch (IOException e) {
                throw ProtobufDeserializationException.truncatedMessage(e);
            }
        }

        return result;
    }

    @Override
    public ByteBuffer readRawBuffer(int size) {
        assertReadable();
        return ByteBuffer.wrap(readRawBytes(size));
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) {
        assertReadable();
        return MemorySegment.ofArray(readRawBytes(size));
    }

    @Override
    public ProtobufIODataType rawDataTypePreference() {
        return ProtobufIODataType.BYTE_ARRAY;
    }

    @Override
    public boolean isFinished() {
        if (finished) {
            return true;
        } else if (limit != -1 && position >= limit) {
            return true;
        } else if (bufferPosition < bufferLimit) {
            return false;
        } else if (tryRefillBuffer(1)) {
            return false;
        } else {
            finished = true;
            return true;
        }
    }

    @Override
    public void skipRawBytes(int length) {
        assertReadable();

        if (length < 0) {
            throw ProtobufDeserializationException.negativeLength(length);
        }

        // Drain buffer first
        var available = bufferLimit - bufferPosition;
        if (available > 0) {
            var toSkip = Math.min(length, available);
            bufferPosition += toSkip;
            position += toSkip;
            length -= toSkip;
        }

        // Skip remainder from stream directly
        while (length > 0) {
            try {
                var skipped = (int) inputStream.skip(length);
                if (skipped > 0) {
                    length -= skipped;
                    position += skipped;
                } else {
                    var read = inputStream.read();
                    if (read == -1) {
                        throw ProtobufDeserializationException.truncatedMessage();
                    }
                    length--;
                    position++;
                }
            } catch (IOException e) {
                throw ProtobufDeserializationException.truncatedMessage(e);
            }
        }
    }

    @Override
    public int readRawFixedInt32() {
        assertReadable();
        if (!tryEnsureAvailable(Integer.BYTES)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        var result = (int) ARRAY_AS_INT32_LE.get(buffer, bufferPosition);
        bufferPosition += Integer.BYTES;
        position += Integer.BYTES;
        return result;
    }

    @Override
    public long readRawFixedInt64() {
        assertReadable();
        if (!tryEnsureAvailable(Long.BYTES)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        var result = (long) ARRAY_AS_INT64_LE.get(buffer, bufferPosition);
        bufferPosition += Long.BYTES;
        position += Long.BYTES;
        return result;
    }

    @Override
    public float readRawFloat() {
        assertReadable();
        if (!tryEnsureAvailable(Float.BYTES)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        var result = (float) ARRAY_AS_FLOAT_LE.get(buffer, bufferPosition);
        bufferPosition += Float.BYTES;
        position += Float.BYTES;
        return result;
    }

    @Override
    public double readRawDouble() {
        assertReadable();
        if (!tryEnsureAvailable(Double.BYTES)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        var result = (double) ARRAY_AS_DOUBLE_LE.get(buffer, bufferPosition);
        bufferPosition += Double.BYTES;
        position += Double.BYTES;
        return result;
    }

    @Override
    public int readRawVarInt32() {
        assertReadable();

        if (tryEnsureAvailable(VARINT32_FAST_PATH_BYTES) && BMI2.isHardwareSupported()) {
            var word = (long) ARRAY_AS_INT64_LE.get(buffer, bufferPosition);
            var cont = ~word & VARINT32_CONT_BITS;
            var spread = cont ^ (cont - 1);
            var mask = spread & VARINT32_PAYLOAD_BITS;
            var read = Long.bitCount(spread) >>> 3;
            bufferPosition += read;
            position += read;
            return (int) Long.compress(word, mask);
        }

        return readRawVarInt32Slow();
    }

    private int readRawVarInt32Slow() {
        if (!tryEnsureAvailable(1)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        var startPos = bufferPosition;
        try {
            int x;
            if ((x = buffer[bufferPosition++]) >= 0) {
                // 1 byte
            } else if ((x ^= (buffer[bufferPosition++] << 7)) < 0) {
                x ^= (~0 << 7);
            } else if ((x ^= (buffer[bufferPosition++] << 14)) >= 0) {
                x ^= ((~0 << 7) ^ (~0 << 14));
            } else if ((x ^= (buffer[bufferPosition++] << 21)) < 0) {
                x ^= ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else {
                x ^= buffer[bufferPosition++] << 28;
                x ^= ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
            }

            if (bufferPosition <= bufferLimit) {
                position += bufferPosition - startPos;
                return x;
            }

            throw ProtobufDeserializationException.truncatedMessage();
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() {
        assertReadable();

        if (tryEnsureAvailable(VARINT64_FAST_PATH_BYTES) && BMI2.isHardwareSupported()) {
            var lo = (long) ARRAY_AS_INT64_LE.get(buffer, bufferPosition);
            var hi = (long) ARRAY_AS_INT64_LE.get(buffer, bufferPosition + Long.BYTES);
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
            var read = (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
            bufferPosition += read;
            position += read;
            return loResult | (hiResult << 56);
        }

        return readRawVarInt64Slow();
    }

    private long readRawVarInt64Slow() {
        if (!tryEnsureAvailable(1)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        var startPos = bufferPosition;
        try {
            long x;
            int y;
            if ((y = buffer[bufferPosition++]) >= 0) {
                x = y;
            } else if ((y ^= (buffer[bufferPosition++] << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (buffer[bufferPosition++] << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (buffer[bufferPosition++] << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) buffer[bufferPosition++] << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) buffer[bufferPosition++] << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) buffer[bufferPosition++] << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) buffer[bufferPosition++] << 49)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
            } else if ((x ^= ((long) buffer[bufferPosition++] << 56)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
            } else {
                x ^= ((long) buffer[bufferPosition++] << 63);
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
            }

            if (bufferPosition <= bufferLimit) {
                position += bufferPosition - startPos;
                return x;
            }

            throw ProtobufDeserializationException.truncatedMessage();
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public boolean readRawBool() {
        assertReadable();

        if (bufferPosition < bufferLimit && buffer[bufferPosition] >= 0) {
            position++;
            return buffer[bufferPosition++] != 0;
        }

        return readRawVarInt64() != 0;
    }

    @Override
    public ProtobufBinaryStreamReader readRawLengthDelimited(long lengthDelimitedPropertyLength) {
        assertReadable();
        if (lengthDelimitedPropertyLength >= 0) {
            return lengthDelimitedReader = new ProtobufBinaryStreamReader(
                    inputStream, position + lengthDelimitedPropertyLength,
                    buffer, bufferPosition, bufferLimit, position
            );
        }

        throw ProtobufDeserializationException.negativeLength(lengthDelimitedPropertyLength);
    }

    @Override
    public float[] readRawPackedFloat() {
        assertReadable();

        var length = readLengthDelimitedPropertyLength();
        var result = new float[length / Float.BYTES];
        readRawBuffer(length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(result);
        return result;
    }

    @Override
    public double[] readRawPackedDouble() {
        assertReadable();

        var length = readLengthDelimitedPropertyLength();
        var result = new double[length / Double.BYTES];
        readRawBuffer(length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asDoubleBuffer()
                .get(result);
        return result;
    }

    private void ensurePayloadBuffered(int length) {
        if (bufferLimit - bufferPosition >= length) {
            return;
        }
        // Payload spans buffer boundary — copy to a contiguous array and point bufferPosition at it
        var payload = readRawBytes(length);
        // readRawBytes consumed the data; put it back so readRawVarInt* can consume it
        System.arraycopy(payload, 0, buffer, 0, length);
        bufferPosition = 0;
        bufferLimit = length;
    }

    @Override
    public int[] readRawPackedVarInt32() {
        assertReadable();
        var length = readLengthDelimitedPropertyLength();
        ensurePayloadBuffered(length);

        var count = countVarInts(buffer, bufferPosition, length);
        var end = bufferPosition + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (BMI2.isHardwareSupported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - bufferPosition < 16) break;
                var bv = ByteVector.fromArray(B128, buffer, bufferPosition);
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
                bufferPosition += firstLen + secondLen;
                position += firstLen + secondLen;
                dst += 2;
            }
        }

        while (dst < count) {
            result[dst++] = readRawVarInt32();
        }
        return result;
    }

    @Override
    public int[] readRawPackedZigZagVarInt32() {
        assertReadable();
        var length = readLengthDelimitedPropertyLength();
        ensurePayloadBuffered(length);

        var count = countVarInts(buffer, bufferPosition, length);
        var end = bufferPosition + length;
        var result = new int[count];
        var dst = 0;

        // BMI2 implies x86-64, which guarantees SSE2 — so SPECIES_128 is always hardware-supported
        // when this branch is taken. Long.compress (PEXT) also requires BMI2.
        if (BMI2.isHardwareSupported()) {
            var pairs = count / 2;
            for (var i = 0; i < pairs; i++) {
                if (end - bufferPosition < 16) break;
                var bv = ByteVector.fromArray(B128, buffer, bufferPosition);
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
                bufferPosition += firstLen + secondLen;
                position += firstLen + secondLen;
                dst += 2;
            }
        }

        while (dst < count) {
            result[dst++] = readRawZigZagVarInt32();
        }
        return result;
    }

    @Override
    public long[] readRawPackedVarInt64() {
        assertReadable();
        var length = readLengthDelimitedPropertyLength();
        ensurePayloadBuffered(length);

        var count = countVarInts(buffer, bufferPosition, length);
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawVarInt64();
        }
        return result;
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() {
        assertReadable();
        var length = readLengthDelimitedPropertyLength();
        ensurePayloadBuffered(length);

        var count = countVarInts(buffer, bufferPosition, length);
        var result = new long[count];
        for (var i = 0; i < count; i++) {
            result[i] = readRawZigZagVarInt64();
        }
        return result;
    }

    @Override
    public boolean[] readRawPackedBool() {
        assertReadable();

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
        assertReadable();

        var length = readLengthDelimitedPropertyLength();
        var result = new int[length / Integer.BYTES];
        readRawBuffer(length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer()
                .get(result);
        return result;
    }

    @Override
    public long[] readRawPackedFixedInt64() {
        assertReadable();

        var length = readLengthDelimitedPropertyLength();
        var result = new long[length / Long.BYTES];
        readRawBuffer(length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asLongBuffer()
                .get(result);
        return result;
    }

    @Override
    public void close() throws IOException {
        if (autoclose) {
            inputStream.close();
        }
    }

    private void assertReadable() {
        if (finished) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        if (lengthDelimitedReader != null) {
            if (!lengthDelimitedReader.isFinished()) {
                throw new IllegalStateException("Length delimited read is in progress");
            }
            // Absorb sub-reader's final state
            this.bufferPosition = lengthDelimitedReader.bufferPosition;
            this.bufferLimit = lengthDelimitedReader.bufferLimit;
            this.position = lengthDelimitedReader.position;
            this.lengthDelimitedReader = null;
        }

        if (limit != -1 && position >= limit) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }
}