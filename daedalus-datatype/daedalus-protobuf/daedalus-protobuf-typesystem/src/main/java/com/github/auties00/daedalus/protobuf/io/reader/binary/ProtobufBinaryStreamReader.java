package com.github.auties00.daedalus.protobuf.io.reader.binary;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.typesystem.util.CpuUtils;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ProtobufBinaryStreamReader extends ProtobufBinaryReader {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_VAR_INT_LENGTH = 10;

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

    private boolean tryEnsureAvailable(int needed) throws ProtobufDeserializationException {
        if (bufferLimit - bufferPosition >= needed) {
            return true;
        }
        return tryRefillBuffer(needed);
    }

    private boolean tryRefillBuffer(int needed) throws ProtobufDeserializationException {
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

    // A length delimited reader must never consume more bytes than its limit allows
    private void checkLimit(long size) throws ProtobufDeserializationException {
        if (size < 0) {
            throw ProtobufDeserializationException.negativeLength(size);
        } else if (limit != -1 && size > limit - position) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    private void checkPosition() throws ProtobufDeserializationException {
        if (limit != -1 && position > limit) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public byte readRawByte() throws ProtobufDeserializationException {
        assertReadable();
        if (!tryEnsureAvailable(1)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        position++;
        return buffer[bufferPosition++];
    }

    @Override
    public byte[] readRawBytes(int size) throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
        checkLimit(size);

        var result = new byte[size];
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
    public String readRawString(int size, boolean validateUtf8) throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
        checkLimit(size);
        if (size > buffer.length) {
            return super.readRawString(size, validateUtf8);
        }

        if (!tryEnsureAvailable(size)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        if (validateUtf8 && !CharsetUtils.isValidUtf8(buffer, bufferPosition, size)) {
            throw ProtobufDeserializationException.invalidUtf8();
        }
        var result = new String(buffer, bufferPosition, size, StandardCharsets.UTF_8);
        bufferPosition += size;
        position += size;
        return result;
    }

    @Override
    public ByteBuffer readRawBuffer(int size) throws ProtobufDeserializationException {
        return ByteBuffer.wrap(readRawBytes(size));
    }

    @Override
    public MemorySegment readRawMemorySegment(int size) throws ProtobufDeserializationException {
        return MemorySegment.ofArray(readRawBytes(size));
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.BYTE_ARRAY;
    }

    @Override
    public boolean isFinished() throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
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
    public void skipRawBytes(int length) throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
        checkLimit(length);

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

    private void ensureFixed(int size) throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
        checkLimit(size);
        if (!tryEnsureAvailable(size)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public int readRawFixedInt32() throws ProtobufDeserializationException {
        ensureFixed(Integer.BYTES);
        var result = BinaryUtils.readIntLE(buffer, bufferPosition);
        bufferPosition += Integer.BYTES;
        position += Integer.BYTES;
        return result;
    }

    @Override
    public long readRawFixedInt64() throws ProtobufDeserializationException {
        ensureFixed(Long.BYTES);
        var result = BinaryUtils.readLongLE(buffer, bufferPosition);
        bufferPosition += Long.BYTES;
        position += Long.BYTES;
        return result;
    }

    @Override
    public float readRawFloat() throws ProtobufDeserializationException {
        ensureFixed(Float.BYTES);
        var result = BinaryUtils.readFloatLE(buffer, bufferPosition);
        bufferPosition += Float.BYTES;
        position += Float.BYTES;
        return result;
    }

    @Override
    public double readRawDouble() throws ProtobufDeserializationException {
        ensureFixed(Double.BYTES);
        var result = BinaryUtils.readDoubleLE(buffer, bufferPosition);
        bufferPosition += Double.BYTES;
        position += Double.BYTES;
        return result;
    }

    @Override
    public int readRawVarInt32() throws ProtobufDeserializationException {
        assertReadable();

        if (CpuUtils.isFastBmi2Supported() && tryEnsureAvailable(VARINT32_FAST_PATH_BYTES)) {
            var word = BinaryUtils.readLongLE(buffer, bufferPosition);
            var cont = ~word & VARINT32_CONT_BITS;
            // A varint32 longer than five bytes is a sign extended negative int32: decode it as a varint64
            if (cont != 0) {
                var spread = cont ^ (cont - 1);
                var read = Long.bitCount(spread) >>> 3;
                bufferPosition += read;
                position += read;
                checkPosition();
                return (int) Long.compress(word, spread & VARINT32_PAYLOAD_BITS);
            }
        }

        return readRawVarInt32Slow();
    }

    private int readRawVarInt32Slow() throws ProtobufDeserializationException {
        // Buffer the longest possible varint: if the stream ends first, every remaining byte is buffered
        tryEnsureAvailable(MAX_VAR_INT_LENGTH);
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
                var y = buffer[bufferPosition++];
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                // Negative int32 values are sign extended to ten bytes: the upper bytes are discarded
                if (y < 0
                        && buffer[bufferPosition++] < 0
                        && buffer[bufferPosition++] < 0
                        && buffer[bufferPosition++] < 0
                        && buffer[bufferPosition++] < 0
                        && buffer[bufferPosition++] < 0) {
                    throw ProtobufDeserializationException.malformedVarInt();
                }
            }

            if (bufferPosition > bufferLimit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            position += bufferPosition - startPos;
            checkPosition();
            return x;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public long readRawVarInt64() throws ProtobufDeserializationException {
        assertReadable();

        if (CpuUtils.isFastBmi2Supported() && tryEnsureAvailable(VARINT64_FAST_PATH_BYTES)) {
            var lo = BinaryUtils.readLongLE(buffer, bufferPosition);
            var hi = BinaryUtils.readLongLE(buffer, bufferPosition + Long.BYTES);
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
            var read = (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
            bufferPosition += read;
            position += read;
            checkPosition();
            return loResult | (hiResult << 56);
        }

        return readRawVarInt64Slow();
    }

    private long readRawVarInt64Slow() throws ProtobufDeserializationException {
        // Buffer the longest possible varint: if the stream ends first, every remaining byte is buffered
        tryEnsureAvailable(MAX_VAR_INT_LENGTH);
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
                var last = buffer[bufferPosition++];
                if (last < 0) {
                    throw ProtobufDeserializationException.malformedVarInt();
                }
                x ^= ((long) last << 63);
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
            }

            if (bufferPosition > bufferLimit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            position += bufferPosition - startPos;
            checkPosition();
            return x;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }

    @Override
    public boolean readRawBool() throws ProtobufDeserializationException {
        assertReadable();

        if (tryEnsureAvailable(1) && buffer[bufferPosition] >= 0) {
            position++;
            return buffer[bufferPosition++] != 0;
        }

        return readRawVarInt64() != 0;
    }

    @Override
    public ProtobufBinaryStreamReader readRawLengthDelimited(long lengthDelimitedFieldLength) throws ProtobufDeserializationException {
        absorbLengthDelimitedReader();
        checkLimit(lengthDelimitedFieldLength);
        return lengthDelimitedReader = new ProtobufBinaryStreamReader(
                inputStream, position + lengthDelimitedFieldLength,
                buffer, bufferPosition, bufferLimit, position
        );
    }

    // Returns a reader over the next length bytes, which are consumed from this reader.
    // Payloads that fit in the buffer are decoded in place, larger ones are copied.
    private ProtobufBinaryByteArrayReader readPackedPayload() throws ProtobufDeserializationException {
        var length = readLengthDelimitedFieldLength();
        checkLimit(length);
        if (length > buffer.length) {
            return new ProtobufBinaryByteArrayReader(readRawBytes(length), 0, length);
        }

        if (!tryEnsureAvailable(length)) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
        var result = new ProtobufBinaryByteArrayReader(buffer, bufferPosition, bufferPosition + length);
        bufferPosition += length;
        position += length;
        return result;
    }

    @Override
    public float[] readRawPackedFloat() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedFloat(payload.remaining());
    }

    @Override
    public double[] readRawPackedDouble() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedDouble(payload.remaining());
    }

    @Override
    public int[] readRawPackedVarInt32() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedVarInt32(payload.remaining());
    }

    @Override
    public int[] readRawPackedZigZagVarInt32() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedZigZagVarInt32(payload.remaining());
    }

    @Override
    public long[] readRawPackedVarInt64() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedVarInt64(payload.remaining());
    }

    @Override
    public long[] readRawPackedZigZagVarInt64() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedZigZagVarInt64(payload.remaining());
    }

    @Override
    public boolean[] readRawPackedBool() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedBool(payload.remaining());
    }

    @Override
    public int[] readRawPackedFixedInt32() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedFixedInt32(payload.remaining());
    }

    @Override
    public long[] readRawPackedFixedInt64() throws ProtobufDeserializationException {
        var payload = readPackedPayload();
        return payload.readRawPackedFixedInt64(payload.remaining());
    }

    @Override
    public void close() throws IOException {
        if (autoclose) {
            inputStream.close();
        }
    }

    // Absorbs the final state of the length delimited reader, which shares this reader's stream and buffer
    private void absorbLengthDelimitedReader() throws ProtobufDeserializationException {
        if (lengthDelimitedReader != null) {
            if (!lengthDelimitedReader.isFinished()) {
                throw new IllegalStateException("Length delimited read is in progress");
            }
            this.bufferPosition = lengthDelimitedReader.bufferPosition;
            this.bufferLimit = lengthDelimitedReader.bufferLimit;
            this.position = lengthDelimitedReader.position;
            this.lengthDelimitedReader = null;
        }
    }

    private void assertReadable() throws ProtobufDeserializationException {
        if (finished) {
            throw ProtobufDeserializationException.truncatedMessage();
        }

        absorbLengthDelimitedReader();

        if (limit != -1 && position >= limit) {
            throw ProtobufDeserializationException.truncatedMessage();
        }
    }
}
