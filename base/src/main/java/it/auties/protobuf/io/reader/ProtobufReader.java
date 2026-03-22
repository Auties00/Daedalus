// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

// I'm not sure if the LICENSE copyright header is necessary as only two methods in this class are taken from Google's source code
// But just to be sure I included it

package it.auties.protobuf.io.reader;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.io.ProtobufDataType;
import it.auties.protobuf.io.writer.ProtobufWriter;
import it.auties.protobuf.model.ProtobufUnknownValue;
import it.auties.protobuf.model.ProtobufWireType;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

/**
 * An abstract input stream for reading Protocol Buffer encoded data.
 * <p>
 * This class provides a comprehensive API for deserializing Protocol Buffer messages from various
 * data sources including byte arrays, ByteBuffers, and InputStreams. It supports all Protocol Buffer
 * wire types and provides both type-safe and unchecked reading methods.
 * <p>
 *
 * @see ProtobufWriter
 */
public abstract class ProtobufReader implements AutoCloseable{
    protected static final VarHandle ARRAY_AS_INT16_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_INT32_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_INT64_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_FLOAT_LE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_DOUBLE_LE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    protected static final VarHandle BUFFER_AS_INT16_LE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_INT32_LE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_INT64_LE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_FLOAT_LE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_DOUBLE_LE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

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
        VARINT32_PEXT_MASKS[1]  = 0x000000000000007fL;
        VARINT32_PEXT_MASKS[2]  = 0x0000000000007f7fL;
        VARINT32_PEXT_MASKS[3]  = 0x00000000007f7f7fL;
        VARINT32_PEXT_MASKS[4]  = 0x000000007f7f7f7fL;
        VARINT32_PEXT_MASKS[5]  = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[6]  = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[7]  = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[8]  = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[9]  = 0x0000000f7f7f7f7fL;
        VARINT32_PEXT_MASKS[10] = 0x0000000f7f7f7f7fL;

        // Build shuffle table
        for (int fl = 1; fl <= 10; fl++) {
            for (int sl = 1; sl <= 10; sl++) {
                var base = ((fl - 1) * 10 + (sl - 1)) * 16;
                for (int i = 0; i < 8; i++) {
                    VARINT32_2X_SHUFFLE_TABLE[base + i] = (byte) (i < fl ? i : 0);
                    VARINT32_2X_SHUFFLE_TABLE[base + 8 + i] = (byte) (i < sl ? Math.min(fl + i, 15) : 0);
                }
            }
        }

        // Build bitmask → entry lookup
        for (int bm = 0; bm < 1024; bm++) {
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
            for (int i = 0; i < 31; i++) {
                acc0 += (~(long) (long) ARRAY_AS_INT64_LE.get(buf, ptr) & MSB8) >>> 7;
                acc1 += (~(long) (long) ARRAY_AS_INT64_LE.get(buf, ptr + 8) & MSB8) >>> 7;
                acc2 += (~(long) (long) ARRAY_AS_INT64_LE.get(buf, ptr + 16) & MSB8) >>> 7;
                acc3 += (~(long) (long) ARRAY_AS_INT64_LE.get(buf, ptr + 24) & MSB8) >>> 7;
                ptr += 32;
            }
            count += (int) ((acc0 * LO_BYTES) >>> 56);
            count += (int) ((acc1 * LO_BYTES) >>> 56);
            count += (int) ((acc2 * LO_BYTES) >>> 56);
            count += (int) ((acc3 * LO_BYTES) >>> 56);
        }

        while (end - ptr >= Long.BYTES) {
            var word = (long) ARRAY_AS_INT64_LE.get(buf, ptr);
            count += Long.bitCount(~word & MSB8);
            ptr += Long.BYTES;
        }

        if (end - ptr >= Integer.BYTES) {
            var word = (int) ARRAY_AS_INT32_LE.get(buf, ptr);
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
        var end = bb.remaining();
        if (bb.hasArray()) {
            return countVarInts(bb.array(), bb.arrayOffset(), end);
        }

        var order = bb.order();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        try {
            int count = 0, ptr = 0;

            while (end - ptr >= 31 * 32) {
                long acc0 = 0, acc1 = 0, acc2 = 0, acc3 = 0;
                for (int i = 0; i < 31; i++) {
                    acc0 += (~bb.getLong(ptr)      & MSB8) >>> 7;
                    acc1 += (~bb.getLong(ptr + 8)  & MSB8) >>> 7;
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
    protected long index;
    protected ProtobufReader() {
        resetPropertyTag();
    }

    public static ProtobufReader fromBytes(byte[] bytes) {
        return new ProtobufByteArrayReader(bytes, 0, bytes.length);
    }

    public static ProtobufReader fromBytes(byte[] bytes, int offset, int length) {
        return new ProtobufByteArrayReader(bytes, offset, offset + length);
    }

    public static ProtobufReader fromBuffer(ByteBuffer buffer) {
        return new ProtobufByteBufferReader(buffer);
    }

    public static ProtobufReader fromStream(InputStream stream) {
        return new ProtobufStreamReader(stream, true, -1);
    }

    public static ProtobufReader fromStream(InputStream stream, boolean autoclose) {
        return new ProtobufStreamReader(stream, autoclose, -1);
    }

    public static ProtobufReader fromStream(InputStream stream, boolean autoclose, int bufferSize) {
        return new ProtobufStreamReader(stream, autoclose, -1, bufferSize);
    }

    public static ProtobufReader fromLimitedStream(InputStream stream, long limit) {
        if(limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return new ProtobufStreamReader(stream, true, limit);
    }

    public static ProtobufReader fromLimitedStream(InputStream stream, long limit, boolean autoclose) {
        if(limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return new ProtobufStreamReader(stream, autoclose, limit);
    }

    public static ProtobufReader fromLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        if(limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return new ProtobufStreamReader(stream, autoclose, limit, bufferSize);
    }

    public static ProtobufReader fromMemorySegment(MemorySegment segment) {
        return new ProtobufMemorySegmentReader(segment);
    }

    public int propertyWireType() {
        return wireType;
    }

    public long propertyIndex() {
        return index;
    }

    public boolean readPropertyTag() {
        if(wireType != -1 || index != -1) {
            throw ProtobufDeserializationException.invalidPropertyState("a property tag was already read");
        } else if(isFinished()) {
            return false;
        }else {
            var rawTag = readRawVarInt32();
            this.wireType = rawTag & 7;
            this.index = rawTag >>> 3;
            if(index == 0) {
                throw ProtobufDeserializationException.invalidFieldIndex(index);
            }
            return wireType != ProtobufWireType.WIRE_TYPE_END_OBJECT;
        }
    }

    public void resetPropertyTag() {
        if(wireType == -1 || index == -1) {
            throw ProtobufDeserializationException.invalidPropertyState("no property tag");
        } else {
            this.wireType = -1;
            this.index = -1;
        }
    }

    public int readLengthDelimitedPropertyLength() {
        var length = readRawVarInt32();
        if(length < 0) {
            throw ProtobufDeserializationException.negativeLength(length);
        } else {
            return length;
        }
    }

    public ProtobufReader readLengthDelimitedProperty() {
        if(wireType != ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var size = readLengthDelimitedPropertyLength();
            var result = readRawLengthDelimited(size);
            resetPropertyTag();
            return result;
        }
    }

    public void readStartGroupProperty(long groupIndex) {
        if((wireType == -1 && !readPropertyTag()) || wireType != ProtobufWireType.WIRE_TYPE_START_OBJECT || index != groupIndex) {
            throw ProtobufDeserializationException.invalidStartObject(groupIndex);
        } else {
            resetPropertyTag();
        }
    }

    public void readEndGroupProperty(long groupIndex) {
        if(wireType != ProtobufWireType.WIRE_TYPE_END_OBJECT) {
            throw ProtobufDeserializationException.malformedGroup();
        } else if(index != groupIndex) {
            throw ProtobufDeserializationException.invalidEndObject(index, groupIndex);
        } else {
            resetPropertyTag();
        }
    }

    public float readFloatProperty() {
        if(wireType != ProtobufWireType.WIRE_TYPE_FIXED32) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFloat();
            resetPropertyTag();
            return result;
        }
    }

    public double readDoubleProperty() {
        if(wireType != ProtobufWireType.WIRE_TYPE_FIXED64) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawDouble();
            resetPropertyTag();
            return result;
        }
    }

    public boolean readBoolProperty() {
        if(wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawBool();
            resetPropertyTag();
            return result;
        }
    }

    public int readInt32Property() {
        return readVarInt32();
    }

    public int readUInt32Property() {
        return readVarInt32();
    }

    private int readVarInt32() {
        if(wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawVarInt32();
            resetPropertyTag();
            return result;
        }
    }

    public int readSInt32Property() {
        if(wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            return readRawZigZagVarInt32();
        }
    }

    public long readInt64Property() {
        return readVarInt64();
    }

    public long readUInt64Property() {
        return readVarInt64();
    }

    private long readVarInt64() {
        if(wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawVarInt64();
            resetPropertyTag();
            return result;
        }
    }

    public long readSInt64Property() {
        if(wireType != ProtobufWireType.WIRE_TYPE_VAR_INT) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawZigZagVarInt64();
            resetPropertyTag();
            return result;
        }
    }

    public int readFixed32Property() {
        return readFixed32();
    }

    public int readSFixed32Property() {
        return readFixed32();
    }

    private int readFixed32() {
        if(wireType != ProtobufWireType.WIRE_TYPE_FIXED32) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFixedInt32();
            resetPropertyTag();
            return result;
        }
    }

    public long readFixed64Property() {
        return readFixed64();
    }

    public long readSFixed64Property() {
        return readFixed64();
    }

    private long readFixed64() {
        if(wireType != ProtobufWireType.WIRE_TYPE_FIXED64) {
            throw ProtobufDeserializationException.invalidWireType(wireType);
        } else {
            var result = readRawFixedInt64();
            resetPropertyTag();
            return result;
        }
    }

    public ProtobufUnknownValue readUnknownProperty() {
        return switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> {
                var value = readRawFixedInt32();
                yield new ProtobufUnknownValue.Fixed32(value);
            }
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> {
                var value = readRawFixedInt64();
                yield new ProtobufUnknownValue.Fixed64(value);
            }
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> {
                var size = readLengthDelimitedPropertyLength();
                yield switch (rawDataTypePreference()) {
                    case BYTE_ARRAY -> {
                        var bytes = readRawBytes(size);
                        yield new ProtobufUnknownValue.LengthDelimited.ByteArrayBacked(bytes);
                    }
                    case BYTE_BUFFER -> {
                        var buffer = readRawBuffer(size);
                        yield new ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(buffer);
                    }
                    case MEMORY_SEGMENT -> {
                        var segment = readRawMemorySegment(size);
                        yield new ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(segment);
                    }
                };
            }
            case ProtobufWireType.WIRE_TYPE_START_OBJECT -> {
                var result = new HashMap<Long, ProtobufUnknownValue>();
                var index = this.index;
                while (readPropertyTag()) {
                    var key = this.index;
                    var value = readUnknownProperty();
                    result.put(key, value);
                }
                readEndGroupProperty(index);
                yield new ProtobufUnknownValue.Group(result);
            }
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> {
                var value = readRawVarInt64();
                yield new ProtobufUnknownValue.VarInt(value);
            }
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
    }

    public float[] readPackedFloatProperty() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFloat();
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> new float[]{readRawFloat()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public double[] readPackedDoubleProperty() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedDouble();
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> new double[]{readRawDouble()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public int[] readPackedInt32Property() {
        return readPackedVarInt32();
    }

    public int[] readPackedUInt32Property() {
        return readPackedVarInt32();
    }

    private int[] readPackedVarInt32() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedVarInt32();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new int[]{readRawVarInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public int[] readPackedSInt32Property() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedZigZagVarInt32();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new int[]{readRawZigZagVarInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public long[] readPackedInt64Property() {
        return readPackedVarInt64();
    }

    public long[] readPackedUInt64Property() {
        return readPackedVarInt64();
    }

    private long[] readPackedVarInt64() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedVarInt64();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new long[]{readRawVarInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public long[] readPackedSInt64Property() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedZigZagVarInt64();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new long[]{readRawZigZagVarInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public boolean[] readPackedBoolProperty() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedBool();
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> new boolean[]{readBoolProperty()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public int[] readPackedFixed32Property() {
        return readPackedFixed32();
    }

    public int[] readPackedSFixed32Property() {
        return readPackedFixed32();
    }

    private int[] readPackedFixed32() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFixedInt32();
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> new int[]{readRawFixedInt32()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public long[] readPackedFixed64Property() {
        return readPackedFixed64();
    }

    public long[] readPackedSFixed64Property() {
        return  readPackedFixed64();
    }

    private long[] readPackedFixed64() {
        var result = switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> readRawPackedFixedInt64();
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> new long[]{readRawFixedInt64()};
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
        resetPropertyTag();
        return result;
    }

    public void skipUnknownProperty() {
        switch (wireType) {
            case ProtobufWireType.WIRE_TYPE_VAR_INT -> {
                readRawVarInt64();
                resetPropertyTag();
            }
            case ProtobufWireType.WIRE_TYPE_FIXED32 -> {
                skipRawBytes(Integer.BYTES);
                resetPropertyTag();
            }
            case ProtobufWireType.WIRE_TYPE_FIXED64 -> {
                skipRawBytes(Long.BYTES);
                resetPropertyTag();
            }
            case ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED -> {
                skipRawBytes(readLengthDelimitedPropertyLength());
                resetPropertyTag();
            }
            case ProtobufWireType.WIRE_TYPE_START_OBJECT -> {
                var index = this.index;
                while (readPropertyTag()) {
                    skipUnknownProperty();
                }
                readEndGroupProperty(index);
            }
            default -> throw ProtobufDeserializationException.invalidWireType(wireType);
        };
    }

    public int readRawZigZagVarInt32() {
        var value = readRawVarInt32();
        var unsigned = Integer.toUnsignedLong(value);
        return (int) ((unsigned >> 1) ^ (-(unsigned & 1)));
    }

    public long readRawZigZagVarInt64() {
        var value = readRawVarInt64();
        return (value >>> 1) ^ (-(value & 1));
    }

    public abstract byte readRawByte();

    public abstract byte[] readRawBytes(int size);
    public abstract void skipRawBytes(int size);
    public abstract ByteBuffer readRawBuffer(int size);
    public abstract MemorySegment readRawMemorySegment(int size);

    public abstract int readRawFixedInt32();
    public abstract long readRawFixedInt64();
    public abstract float readRawFloat();
    public abstract double readRawDouble();
    public abstract int readRawVarInt32();
    public abstract long readRawVarInt64();
    public abstract boolean readRawBool();

    public abstract int[] readRawPackedFixedInt32();
    public abstract long[] readRawPackedFixedInt64();
    public abstract float[] readRawPackedFloat();
    public abstract double[] readRawPackedDouble();
    public abstract int[] readRawPackedVarInt32();
    public abstract int[] readRawPackedZigZagVarInt32();
    public abstract long[] readRawPackedVarInt64();
    public abstract long[] readRawPackedZigZagVarInt64();
    public abstract boolean[] readRawPackedBool();

    public abstract ProtobufReader readRawLengthDelimited(long size);

    public abstract boolean isFinished();

    public abstract ProtobufDataType rawDataTypePreference();
}