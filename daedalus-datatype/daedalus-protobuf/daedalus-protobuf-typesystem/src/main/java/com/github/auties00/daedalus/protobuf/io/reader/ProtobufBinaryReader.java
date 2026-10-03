// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

// I'm not sure if the LICENSE copyright header is necessary as only two methods in this class are taken from Google's source code
// But just to be sure I included it

package com.github.auties00.daedalus.protobuf.io.reader;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.reader.binary.ProtobufBinaryByteArrayReader;
import com.github.auties00.daedalus.protobuf.io.reader.binary.ProtobufBinaryDirectByteBufferReader;
import com.github.auties00.daedalus.protobuf.io.reader.binary.ProtobufBinaryMemorySegmentReader;
import com.github.auties00.daedalus.protobuf.io.reader.binary.ProtobufBinaryStreamReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * An abstract reader for Protocol Buffer binary wire format.
 * <p>
 * This class provides a comprehensive API for deserializing Protocol Buffer messages from various
 * binary data sources including byte arrays, ByteBuffers, MemorySegments, and InputStreams.
 * It supports all Protocol Buffer wire types and provides both type-safe and unchecked reading methods.
 *
 * @see ProtobufBinaryWriter
 */
public abstract non-sealed class ProtobufBinaryReader extends ProtobufReader {


    protected static final long MSB8 = 0x80808080_80808080L;
    protected static final int MSB4 = 0x80808080;
    protected static final long LO_BYTES = 0x01010101_01010101L;

    protected static final long INT64_PEXT_MASK_LOW = 0x7f7f7f7f7f7f7f7fL;
    protected static final long INT64_PEXT_MASK_HIGH = 0x000000000000017fL;

    protected static final long VARINT32_CONT_BITS = 0x00000080_80808080L;
    protected static final long VARINT32_PAYLOAD_BITS = 0x0000007F_7F7F7F7FL;
    protected static final long VARINT64_LO_CONT_BITS = 0x80808080_80808080L;
    protected static final long VARINT64_HI_CONT_BITS = 0x8080L;
    protected static final long VARINT64_HI_PAYLOAD_BITS = 0x7F7FL;

    protected static final int VARINT32_FAST_PATH_BYTES = Long.BYTES;
    protected static final int VARINT64_FAST_PATH_BYTES = Long.BYTES * 2;

    protected static final VectorSpecies<Byte> B128 = ByteVector.SPECIES_128;

    // SIMD 2x lookup tables for packed varint32 decoding

    // Maps a 10-bit MSB bitmask to packed (shuffleIndex | firstLen << 8 | secondLen << 16).
    // The bitmask encodes which of the first 10 bytes have their MSB set (continuation bits).
    protected static final int[] VARINT32_2X_LOOKUP_STEP1 = new int[1024];

    // Flat shuffle table: 100 entries x 16 bytes.
    // Entry index = (firstLen-1)*10 + (secondLen-1).
    // First varint bytes -> positions 0..7, second varint bytes -> positions 8..15.
    // Padding positions use index 0 (don't-care; per-length PEXT masks ignore them).
    protected static final byte[] VARINT32_2X_SHUFFLE_TABLE = new byte[100 * 16];

    // Per-length PEXT masks for u32 varint: index by byte-length (0-10).
    // Only lengths 1-5 are valid for u32; entries 6-10 repeat the 5-byte mask.
    protected static final long[] VARINT32_PEXT_MASKS = new long[11];

    static {
        // Build per-length masks
        VARINT32_PEXT_MASKS[1] = 0x000000000000007fL;
        VARINT32_PEXT_MASKS[2] = 0x0000000000007f7fL;
        VARINT32_PEXT_MASKS[3] = 0x00000000007f7f7fL;
        VARINT32_PEXT_MASKS[4] = 0x000000007f7f7f7fL;
        VARINT32_PEXT_MASKS[5] = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[6] = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[7] = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[8] = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[9] = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[10] = 0x0000000f7f7f7f7fL;

        // Build shuffle table
        for (var fl = 1; fl <= 10; fl++) {
            for (var sl = 1; sl <= 10; sl++) {
                var base = ((fl - 1) * 10 + (sl - 1)) * 16;
                for (var i = 0; i < 8; i++) {
                    VARINT32_2X_SHUFFLE_TABLE[base + i] = (byte) (i < fl ? i : 0);
                    VARINT32_2X_SHUFFLE_TABLE[base + 8 + i] = (byte) (i < sl ? Math.min(fl + i, 15) : 0);
                }
            }
        }

        // Build bitmask → entry lookup
        for (var bm = 0; bm < 1024; bm++) {
            var notBm = (~bm) & 0x3FF;
            var fl = notBm == 0 ? 10 : Math.min(Integer.numberOfTrailingZeros(notBm) + 1, 10);
            var notBm2 = notBm >>> fl;
            var sl = notBm2 == 0 ? 10 : Math.min(Integer.numberOfTrailingZeros(notBm2) + 1, 10);
            VARINT32_2X_LOOKUP_STEP1[bm] = ((fl - 1) * 10 + (sl - 1)) | (fl << 8) | (sl << 16);
        }
    }

    protected static int countVarInts(byte[] buf, int off, int len) {
        int count = 0, ptr = off, end = off + len;

        while (end - ptr >= 31 * 32) {
            long acc0 = 0, acc1 = 0, acc2 = 0, acc3 = 0;
            for (var i = 0; i < 31; i++) {
                acc0 += (~BinaryUtils.readLongLE(buf, ptr) & MSB8) >>> 7;
                acc1 += (~BinaryUtils.readLongLE(buf, ptr + 8) & MSB8) >>> 7;
                acc2 += (~BinaryUtils.readLongLE(buf, ptr + 16) & MSB8) >>> 7;
                acc3 += (~BinaryUtils.readLongLE(buf, ptr + 24) & MSB8) >>> 7;
                ptr += 32;
            }
            count += (int) ((acc0 * LO_BYTES) >>> 56);
            count += (int) ((acc1 * LO_BYTES) >>> 56);
            count += (int) ((acc2 * LO_BYTES) >>> 56);
            count += (int) ((acc3 * LO_BYTES) >>> 56);
        }

        while (end - ptr >= Long.BYTES) {
            var word = BinaryUtils.readLongLE(buf, ptr);
            count += Long.bitCount(~word & MSB8);
            ptr += Long.BYTES;
        }

        if (end - ptr >= Integer.BYTES) {
            var word = BinaryUtils.readIntLE(buf, ptr);
            count += Integer.bitCount(~word & MSB4);
            ptr += Integer.BYTES;
        }

        while (ptr < end) {
            if ((buf[ptr++] & 0x80) == 0) {
                count++;
            }
        }

        return count;
    }

    protected static int countVarInts(ByteBuffer bb) {
        if (bb.hasArray()) {
            return countVarInts(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        }

        var order = bb.order();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        try {
            int count = 0, ptr = bb.position(), end = bb.limit();

            while (end - ptr >= 31 * 32) {
                long acc0 = 0, acc1 = 0, acc2 = 0, acc3 = 0;
                for (var i = 0; i < 31; i++) {
                    acc0 += (~bb.getLong(ptr) & MSB8) >>> 7;
                    acc1 += (~bb.getLong(ptr + 8) & MSB8) >>> 7;
                    acc2 += (~bb.getLong(ptr + 16) & MSB8) >>> 7;
                    acc3 += (~bb.getLong(ptr + 24) & MSB8) >>> 7;
                    ptr += 32;
                }
                count += (int) ((acc0 * LO_BYTES) >>> 56);
                count += (int) ((acc1 * LO_BYTES) >>> 56);
                count += (int) ((acc2 * LO_BYTES) >>> 56);
                count += (int) ((acc3 * LO_BYTES) >>> 56);
            }

            while (end - ptr >= Long.BYTES) {
                count += Long.bitCount(~bb.getLong(ptr) & MSB8);
                ptr += Long.BYTES;
            }

            if (end - ptr >= Integer.BYTES) {
                count += Integer.bitCount(~bb.getInt(ptr) & MSB4);
                ptr += Integer.BYTES;
            }

            while (ptr < end) {
                if ((bb.get(ptr++) & 0x80) == 0) {
                    count++;
                }
            }

            return count;
        } finally {
            bb.order(order);
        }
    }

    protected int wireType;
    protected int index;

    /**
     * The number of groups opened by {@link #readStartGroupField(int)} that were not closed
     * yet by {@link #readEndGroupField(int)}.
     *
     * <p>An end group tag is only valid while a group is open: one read at depth {@code 0}
     * closes a group that was never opened, so the input is malformed.
     */
    private int groupDepth;

    /**
     * The default maximum number of messages and groups that can be nested inside each other,
     * which is the same as the default recursion limit of the official protobuf library.
     */
    public static final int DEFAULT_RECURSION_LIMIT = 100;

    /**
     * The maximum number of messages and groups that can be nested inside each other.
     */
    private int recursionLimit = DEFAULT_RECURSION_LIMIT;

    /**
     * The number of messages and groups that enclose the field being read, counting the ones
     * read by the readers that this reader was created from.
     *
     * <p>Decoding a nested message or group recurses, so a malicious input that nests them
     * deeply enough could overflow the stack: entering one more level than
     * {@link #recursionLimit} fails instead.
     */
    private int recursionDepth;

    protected ProtobufBinaryReader() {
        this.wireType = -1;
        this.index = -1;
    }

    public static ProtobufBinaryReader fromBytes(byte[] bytes) {
        return new ProtobufBinaryByteArrayReader(bytes, 0, bytes.length);
    }

    public static ProtobufBinaryReader fromBytes(byte[] bytes, int offset, int length) {
        return new ProtobufBinaryByteArrayReader(bytes, offset, offset + length);
    }

    public static ProtobufBinaryReader fromBuffer(ByteBuffer buffer) {
        if(buffer.hasArray()) {
            var offset = buffer.arrayOffset() + buffer.position();
            var length = buffer.remaining();
            return new ProtobufBinaryByteArrayReader(buffer.array(), offset, offset + length);
        } else {
            return new ProtobufBinaryDirectByteBufferReader(buffer);
        }
    }

    public static ProtobufBinaryReader fromStream(InputStream stream) {
        return fromStream(stream, true);
    }

    public static ProtobufBinaryReader fromStream(InputStream stream, boolean autoclose) {
        return new ProtobufBinaryStreamReader(stream, -1, autoclose);
    }

    public static ProtobufBinaryReader fromStream(InputStream stream, boolean autoclose, int bufferSize) {
        return new ProtobufBinaryStreamReader(stream, -1, autoclose, bufferSize);
    }

    public static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit) {
        return fromLimitedStream(stream, limit, true);
    }

    public static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return new ProtobufBinaryStreamReader(stream, limit, autoclose);
    }

    public static ProtobufBinaryReader fromLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return new ProtobufBinaryStreamReader(stream, limit, autoclose, bufferSize);
    }

    public static ProtobufBinaryReader fromMemorySegment(MemorySegment segment) {
        var heapBase = segment.heapBase();
        if (heapBase.isPresent() && heapBase.get() instanceof byte[] array) {
            // The address of a heap segment is its offset in the backing array
            var offset = (int) segment.address();
            return new ProtobufBinaryByteArrayReader(array, offset, offset + (int) segment.byteSize());
        } else {
            return new ProtobufBinaryMemorySegmentReader(segment);
        }
    }

    public int fieldWireType() {
        return wireType;
    }

    public int fieldIndex() {
        return index;
    }

    public boolean nextFieldTag() throws ProtobufDeserializationException {
        if (wireType != -1 || index != -1) {
            throw ProtobufDeserializationException.fieldTagAlreadyRead();
        } else if (isFinished()) {
            return false;
        } else {
            var rawTag = readRawVarInt32();
            this.wireType = rawTag & 7;
            this.index = rawTag >>> 3;
            if (index == 0) {
                throw ProtobufDeserializationException.invalidFieldIndex(index);
            } else if (wireType != ProtobufWireType.WIRE_TYPE_END_OBJECT) {
                return true;
            } else if (groupDepth == 0) {
                throw ProtobufDeserializationException.unmatchedEndGroup(index);
            } else {
                return false;
            }
        }
    }

    public void resetFieldTag() throws ProtobufDeserializationException {
        if (wireType == -1 || index == -1) {
            throw ProtobufDeserializationException.missingFieldTag();
        } else {
            this.wireType = -1;
            this.index = -1;
        }
    }

    public int readLengthDelimitedFieldLength() throws ProtobufDeserializationException {
        var length = readRawVarInt32();
        if (length < 0) {
            throw ProtobufDeserializationException.negativeLength(length);
        } else {
            return length;
        }
    }

    /**
     * Returns the maximum number of messages and groups that can be nested inside each other.
     *
     * @return the recursion limit, which is {@value #DEFAULT_RECURSION_LIMIT} unless changed
     */
    public int recursionLimit() {
        return recursionLimit;
    }

    /**
     * Sets the maximum number of messages and groups that can be nested inside each other.
     *
     * <p>Like the recursion limit of the official protobuf library, it protects the decoder from
     * inputs that nest messages deeply enough to overflow the stack, so it should only be raised
     * for trusted inputs. The readers created by this reader for nested messages inherit it.
     *
     * @param recursionLimit the new recursion limit
     * @throws IllegalArgumentException if {@code recursionLimit} is negative
     */
    public void setRecursionLimit(int recursionLimit) {
        if (recursionLimit < 0) {
            throw new IllegalArgumentException("recursionLimit cannot be negative: " + recursionLimit);
        }
        this.recursionLimit = recursionLimit;
    }

    /**
     * Enters one more level of nesting.
     *
     * @throws ProtobufDeserializationException if the new level exceeds the recursion limit
     */
    private void enterNestedLevel() throws ProtobufDeserializationException {
        if (recursionDepth >= recursionLimit) {
            throw ProtobufDeserializationException.recursionLimitExceeded(recursionLimit);
        }
        recursionDepth++;
    }

    public ProtobufBinaryReader readLengthDelimitedField() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else if (recursionDepth >= recursionLimit) {
            throw ProtobufDeserializationException.recursionLimitExceeded(recursionLimit);
        } else {
            var size = readLengthDelimitedFieldLength();
            var result = readRawLengthDelimited(size);
            // The nested message is one level deeper than the one that holds it
            result.recursionLimit = recursionLimit;
            result.recursionDepth = recursionDepth + 1;
            resetFieldTag();
            return result;
        }
    }

    public void readStartGroupField(int groupIndex) throws ProtobufDeserializationException {
        if ((wireType == -1 && !nextFieldTag()) || wireType != ProtobufWireType.WIRE_TYPE_START_OBJECT || index != groupIndex) {
            throw ProtobufDeserializationException.invalidStartObject(groupIndex);
        } else {
            enterNestedLevel();
            resetFieldTag();
            groupDepth++;
        }
    }

    public void readEndGroupField(int groupIndex) throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_END_OBJECT) {
            throw ProtobufDeserializationException.malformedGroup();
        } else if (index != groupIndex) {
            throw ProtobufDeserializationException.invalidEndObject(index, groupIndex);
        } else {
            resetFieldTag();
            groupDepth--;
            recursionDepth--;
        }
    }

    public float readFloatField() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_FIXED32) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFloat();
            resetFieldTag();
            return result;
        }
    }

    public double readDoubleField() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_FIXED64) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawDouble();
            resetFieldTag();
            return result;
        }
    }

    public boolean readBoolField() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawBool();
            resetFieldTag();
            return result;
        }
    }

    public String readStringField(boolean validateUtf8) throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        }
        var length = readLengthDelimitedFieldLength();
        var result = readRawString(length, validateUtf8);
        resetFieldTag();
        return result;
    }

    @SuppressWarnings("preview")
    public Supplier<String> readLazyStringField(boolean validateUtf8) throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        }
        var length = readLengthDelimitedFieldLength();
        // The bytes are always copied: the input could be reused or released before the value is decoded
        var source = readRawBytes(length);
        if (validateUtf8 && !CharsetUtils.isValidUtf8(source)) {
            throw ProtobufDeserializationException.invalidUtf8();
        }
        resetFieldTag();
        return LazyConstant.of(() -> new String(source, StandardCharsets.UTF_8));
    }

    public String readRawString(int size, boolean validateUtf8) throws ProtobufDeserializationException {
        var source = readRawBytes(size);
        if (validateUtf8 && !CharsetUtils.isValidUtf8(source)) {
            throw ProtobufDeserializationException.invalidUtf8();
        }
        return new String(source, StandardCharsets.UTF_8);
    }

    public byte[] readBytesField() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        }
        var length = readLengthDelimitedFieldLength();
        var result = readRawBytes(length);
        resetFieldTag();
        return result;
    }

    public int readInt32Field() throws ProtobufDeserializationException {
        return readVarInt32();
    }

    public int readUInt32Field() throws ProtobufDeserializationException {
        return readVarInt32();
    }

    private int readVarInt32() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawVarInt32();
            resetFieldTag();
            return result;
        }
    }

    public int readSInt32Field() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawZigZagVarInt32();
            resetFieldTag();
            return result;
        }
    }

    public long readInt64Field() throws ProtobufDeserializationException {
        return readVarInt64();
    }

    public long readUInt64Field() throws ProtobufDeserializationException {
        return readVarInt64();
    }

    private long readVarInt64() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawVarInt64();
            resetFieldTag();
            return result;
        }
    }

    public long readSInt64Field() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawZigZagVarInt64();
            resetFieldTag();
            return result;
        }
    }

    public int readFixed32Field() throws ProtobufDeserializationException {
        return readFixed32();
    }

    public int readSFixed32Field() throws ProtobufDeserializationException {
        return readFixed32();
    }

    private int readFixed32() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_FIXED32) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFixedInt32();
            resetFieldTag();
            return result;
        }
    }

    public long readFixed64Field() throws ProtobufDeserializationException {
        return readFixed64();
    }

    public long readSFixed64Field() throws ProtobufDeserializationException {
        return readFixed64();
    }

    private long readFixed64() throws ProtobufDeserializationException {
        if (wireType != ProtobufWireType.WIRE_TYPE_FIXED64) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFixedInt64();
            resetFieldTag();
            return result;
        }
    }

    public ProtobufUnknownValue.Binary readUnknownField() throws ProtobufDeserializationException {
        // A group resets the field tag when its end tag is read
        if (wireType == ProtobufWireType.WIRE_TYPE_START_OBJECT) {
            var result = new ProtobufUnknownFields();
            var index = this.index;
            readStartGroupField(index);
            while (nextFieldTag()) {
                var key = this.index;
                var value = readUnknownField();
                result.add(key, value);
            }
            readEndGroupField(index);
            return new ProtobufUnknownValue.Binary.Group(result);
        }

        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> {
                var value = readRawFixedInt32();
                yield new ProtobufUnknownValue.Binary.Fixed32(value);
            }
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> {
                var value = readRawFixedInt64();
                yield new ProtobufUnknownValue.Binary.Fixed64(value);
            }
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> {
                var size = readLengthDelimitedFieldLength();
                yield switch (rawDataTypePreference()) {
                    case BYTE_ARRAY -> {
                        var bytes = readRawBytes(size);
                        yield new ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(bytes);
                    }
                    case BYTE_BUFFER -> {
                        var buffer = readRawBuffer(size);
                        yield new ProtobufUnknownValue.Binary.LengthDelimited.ByteBufferBacked(buffer);
                    }
                    case MEMORY_SEGMENT -> {
                        var segment = readRawMemorySegment(size);
                        yield new ProtobufUnknownValue.Binary.LengthDelimited.MemorySegmentBacked(segment);
                    }
                };
            }
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> {
                var value = readRawVarInt64();
                yield new ProtobufUnknownValue.Binary.VarInt(value);
            }
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public float[] readPackedFloatField() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFloat();
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> new float[]{readRawFloat()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public double[] readPackedDoubleField() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedDouble();
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> new double[]{readRawDouble()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public int[] readPackedInt32Field() throws ProtobufDeserializationException {
        return readPackedVarInt32();
    }

    public int[] readPackedUInt32Field() throws ProtobufDeserializationException {
        return readPackedVarInt32();
    }

    private int[] readPackedVarInt32() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedVarInt32();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new int[]{readRawVarInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public int[] readPackedSInt32Field() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedZigZagVarInt32();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new int[]{readRawZigZagVarInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public long[] readPackedInt64Field() throws ProtobufDeserializationException {
        return readPackedVarInt64();
    }

    public long[] readPackedUInt64Field() throws ProtobufDeserializationException {
        return readPackedVarInt64();
    }

    private long[] readPackedVarInt64() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedVarInt64();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new long[]{readRawVarInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public long[] readPackedSInt64Field() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedZigZagVarInt64();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new long[]{readRawZigZagVarInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public boolean[] readPackedBoolField() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedBool();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new boolean[]{readRawBool()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public int[] readPackedFixed32Field() throws ProtobufDeserializationException {
        return readPackedFixed32();
    }

    public int[] readPackedSFixed32Field() throws ProtobufDeserializationException {
        return readPackedFixed32();
    }

    private int[] readPackedFixed32() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFixedInt32();
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> new int[]{readRawFixedInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public long[] readPackedFixed64Field() throws ProtobufDeserializationException {
        return readPackedFixed64();
    }

    public long[] readPackedSFixed64Field() throws ProtobufDeserializationException {
        return readPackedFixed64();
    }

    private long[] readPackedFixed64() throws ProtobufDeserializationException {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFixedInt64();
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> new long[]{readRawFixedInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetFieldTag();
        return result;
    }

    public void skipUnknownField() throws ProtobufDeserializationException {
        switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> {
                readRawVarInt64();
                resetFieldTag();
            }
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> {
                skipRawBytes(Integer.BYTES);
                resetFieldTag();
            }
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> {
                skipRawBytes(Long.BYTES);
                resetFieldTag();
            }
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> {
                skipRawBytes(readLengthDelimitedFieldLength());
                resetFieldTag();
            }
            case ProtobufWireType.WIRE_TYPE_START_OBJECT -> {
                var index = this.index;
                readStartGroupField(index);
                while (nextFieldTag()) {
                    skipUnknownField();
                }
                readEndGroupField(index);
            }
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        }
    }

    public int readRawZigZagVarInt32() throws ProtobufDeserializationException {
        var value = readRawVarInt32();
        var unsigned = Integer.toUnsignedLong(value);
        return (int) ((unsigned >> 1) ^ (-(unsigned & 1)));
    }

    public long readRawZigZagVarInt64() throws ProtobufDeserializationException {
        var value = readRawVarInt64();
        return (value >>> 1) ^ (-(value & 1));
    }

    public abstract byte readRawByte() throws ProtobufDeserializationException;

    public abstract byte[] readRawBytes(int size) throws ProtobufDeserializationException;

    public abstract void skipRawBytes(int size) throws ProtobufDeserializationException;

    public abstract ByteBuffer readRawBuffer(int size) throws ProtobufDeserializationException;

    public abstract MemorySegment readRawMemorySegment(int size) throws ProtobufDeserializationException;

    public abstract int readRawFixedInt32() throws ProtobufDeserializationException;

    public abstract long readRawFixedInt64() throws ProtobufDeserializationException;

    public abstract float readRawFloat() throws ProtobufDeserializationException;

    public abstract double readRawDouble() throws ProtobufDeserializationException;

    public abstract int readRawVarInt32() throws ProtobufDeserializationException;

    public abstract long readRawVarInt64() throws ProtobufDeserializationException;

    public abstract boolean readRawBool() throws ProtobufDeserializationException;

    public abstract int[] readRawPackedFixedInt32() throws ProtobufDeserializationException;

    public abstract long[] readRawPackedFixedInt64() throws ProtobufDeserializationException;

    public abstract float[] readRawPackedFloat() throws ProtobufDeserializationException;

    public abstract double[] readRawPackedDouble() throws ProtobufDeserializationException;

    public abstract int[] readRawPackedVarInt32() throws ProtobufDeserializationException;

    public abstract int[] readRawPackedZigZagVarInt32() throws ProtobufDeserializationException;

    public abstract long[] readRawPackedVarInt64() throws ProtobufDeserializationException;

    public abstract long[] readRawPackedZigZagVarInt64() throws ProtobufDeserializationException;

    public abstract boolean[] readRawPackedBool() throws ProtobufDeserializationException;

    public abstract ProtobufBinaryReader readRawLengthDelimited(long size) throws ProtobufDeserializationException;

    public abstract boolean isFinished() throws ProtobufDeserializationException;

    public abstract BinaryDataType rawDataTypePreference();
}