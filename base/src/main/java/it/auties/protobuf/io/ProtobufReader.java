// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

// I'm not sure if the LICENSE copyright header is necessary as only two methods in this class are taken from Google's source code
// But just to be sure I included it

package it.auties.protobuf.io;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.model.ProtobufUnknownValue;
import it.auties.protobuf.model.ProtobufWireType;
import it.auties.protobuf.platform.BMI2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Objects;

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
public abstract non-sealed class ProtobufReader extends ProtobufIO {
    protected int wireType;
    protected long index;
    protected ProtobufReader() {
        resetPropertyTag();
    }

    public static ProtobufReader fromBytes(byte[] bytes) {
        return new ByteArrayReader(bytes, 0, bytes.length);
    }

    public static ProtobufReader fromBytes(byte[] bytes, int offset, int length) {
        return new ByteArrayReader(bytes, offset, offset + length);
    }

    public static ProtobufReader fromBuffer(ByteBuffer buffer) {
        return new ByteBufferReader(buffer);
    }

    public static ProtobufReader fromStream(InputStream buffer) {
        return new InputStreamReader(buffer, true);
    }

    public static ProtobufReader fromStream(InputStream buffer, boolean autoclose) {
        return new InputStreamReader(buffer, autoclose);
    }

    public static ProtobufReader fromMemorySegment(MemorySegment segment) {
        return new MemorySegmentReader(segment);
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
            var result = readRawVarInt64() == 1;
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

    public abstract void skipRawBytes(int size);
    public abstract byte readRawByte();

    public abstract byte[] readRawBytes(int size);
    public abstract ByteBuffer readRawBuffer(int size);
    public abstract MemorySegment readRawMemorySegment(int size);

    public abstract int readRawFixedInt32();
    public abstract long readRawFixedInt64();
    public abstract float readRawFloat();
    public abstract double readRawDouble();
    public abstract int readRawVarInt32();
    public abstract long readRawVarInt64();

    public abstract float[] readRawPackedFloat();
    public abstract double[] readRawPackedDouble();
    public abstract int[] readRawPackedVarInt32();
    public abstract int[] readRawPackedZigZagVarInt32();
    public abstract long[] readRawPackedVarInt64();
    public abstract long[] readRawPackedZigZagVarInt64();
    public abstract boolean[] readRawPackedBool();
    public abstract int[] readRawPackedFixedInt32();
    public abstract long[] readRawPackedFixedInt64();

    public abstract ProtobufReader readRawLengthDelimited(int size);

    public abstract boolean isFinished();

    private static final class ByteArrayReader extends ProtobufReader {
        private final byte[] buffer;
        private final int limit;
        private int offset;

        ByteArrayReader(byte[] buffer, int offset, int limit) {
            Objects.requireNonNull(buffer, "buffer cannot be null");
            Objects.checkFromToIndex(offset, limit, buffer.length);
            this.buffer = buffer;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public byte readRawByte() {
            if (offset >= limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            } else {
                return buffer[offset++];
            }
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
        public DataType rawDataTypePreference() {
            return DataType.BYTE_ARRAY;
        }

        @Override
        public boolean isFinished() {
            return offset >= limit;
        }

        @Override
        public void skipRawBytes(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("size cannot be negative");
            } else {
                offset += size;
                if (offset > limit) {
                    throw ProtobufDeserializationException.truncatedMessage();
                }
            }
        }

        @Override
        public int readRawFixedInt32() {
            if (offset + Integer.BYTES > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            } else {
                var result = getIntLE(buffer, offset);
                offset += Integer.BYTES;
                return result;
            }
        }

        @Override
        public long readRawFixedInt64() {
            if (offset + Long.BYTES > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            } else {
                var result = getLongLE(buffer, offset);
                offset += Long.BYTES;
                return result;
            }
        }

        @Override
        public float readRawFloat() {
            if (offset + Float.BYTES > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            } else {
                var result = getFloatLE(buffer, offset);
                offset += Float.BYTES;
                return result;
            }
        }

        @Override
        public double readRawDouble() {
            if (offset + Double.BYTES > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            } else {
                var result = getDoubleLE(buffer, offset);
                offset += Double.BYTES;
                return result;
            }
        }

        @Override
        public ByteArrayReader readRawLengthDelimited(int size) {
            try {
                var result = new ByteArrayReader(buffer, offset, offset + size);
                offset += size;
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
            if (BMI2.isSupported() && buffer.length - offset >= VARINT32_FAST_PATH_BYTES) {
                var word = getLongLE(buffer, offset);
                var cont = ~word & VARINT32_CONT_BITS;
                var spread = cont ^ (cont - 1);
                var mask = spread & VARINT32_PAYLOAD_BITS;
                offset += Long.bitCount(spread) >>> 3;
                return (int) Long.compress(word, mask);
            }

            var unsafeResult = getVarInt32SlowUnsafe();
            if (offset > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
            return unsafeResult;
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
                var lo = getLongLE(buffer, offset);
                var hi = getLongLE(buffer, offset + Long.BYTES);
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

            var unsafeResult = getVarInt64SlowUnsafe();
            if (offset > limit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
            return unsafeResult;
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
        public float[] readRawPackedFloat() {
            var length = readLengthDelimitedPropertyLength();
            var result = toFloatArrayLE(buffer, offset, length);
            offset += length;
            return result;
        }

        @Override
        public double[] readRawPackedDouble() {
            var length = readLengthDelimitedPropertyLength();
            var result = toDoubleArrayLE(buffer, offset, length);
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
            var result = toBooleanArray(buffer, offset, length);
            offset += length;
            return result;
        }

        @Override
        public int[] readRawPackedFixedInt32() {
            var length = readLengthDelimitedPropertyLength();
            var result = toIntArrayLE(buffer, offset, length);
            offset += length;
            return result;
        }

        @Override
        public long[] readRawPackedFixedInt64() {
            var length = readLengthDelimitedPropertyLength();
            var result = toLongArrayLE(buffer, offset, length);
            offset += length;
            return result;
        }
    }

    private static final class ByteBufferReader extends ProtobufReader {
        private final ByteBuffer buffer;

        ByteBufferReader(ByteBuffer buffer) {
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
        public DataType rawDataTypePreference() {
            return DataType.BYTE_BUFFER;
        }

        @Override
        public boolean isFinished() {
            return !buffer.hasRemaining();
        }

        @Override
        public void skipRawBytes(int size) {
            if(size < 0) {
                throw new IllegalArgumentException("Size cannot be negative");
            }

            try {
                buffer.position(buffer.position() + size);
            } catch (IllegalArgumentException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
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
        public ByteBufferReader readRawLengthDelimited(int size) {
            try {
                var position = buffer.position();
                var result = new ByteBufferReader(buffer.slice(position, size));
                buffer.position(position + size);
                return result;
            } catch (IndexOutOfBoundsException _) {
                if(size < 0) {
                    throw new IllegalArgumentException("Size cannot be negative");
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
            if (BMI2.isSupported() && buffer.remaining() >= VARINT32_FAST_PATH_BYTES) {
                var position = buffer.position();
                var word = getLongLE(buffer, position);
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
            if (BMI2.isSupported() && buffer.remaining() >= VARINT64_FAST_PATH_BYTES) {
                var offset = buffer.position();
                var lo = getLongLE(buffer, offset);
                var hi = getLongLE(buffer, offset + 8);
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
            }catch (BufferUnderflowException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public float[] readRawPackedFloat() {
            var length = readLengthDelimitedPropertyLength();
            var buffer = readRawBuffer(length); // Zero copy
            return toFloatArrayLE(buffer);
        }

        @Override
        public double[] readRawPackedDouble() {
            var length = readLengthDelimitedPropertyLength();
            var buffer = readRawBuffer(length); // Zero copy
            return toDoubleArrayLE(buffer);
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
            var buffer = readRawBuffer(length); // Zero copy
            return toBooleanArray(buffer);
        }

        @Override
        public int[] readRawPackedFixedInt32() {
            var length = readLengthDelimitedPropertyLength();
            var buffer = readRawBuffer(length); // Zero copy
            return toIntArrayLE(buffer);
        }

        @Override
        public long[] readRawPackedFixedInt64() {
            var length = readLengthDelimitedPropertyLength();
            var buffer = readRawBuffer(length); // Zero copy
            return toLongArrayLE(buffer);
        }
    }

    private static final class MemorySegmentReader extends ProtobufReader {
        private final MemorySegment segment;
        private int position;

        MemorySegmentReader(MemorySegment segment) {
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
            } catch (BufferUnderflowException _) {
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
            } catch (BufferUnderflowException _) {
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
        public DataType rawDataTypePreference() {
            return DataType.MEMORY_SEGMENT;
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
        public MemorySegmentReader readRawLengthDelimited(int size) {
            try {
                var result = new MemorySegmentReader(segment.asSlice(position, size));
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
                var word = getLongLE(segment, position);
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
                var lo = getLongLE(segment, position);
                var hi = getLongLE(segment, position + Long.BYTES);
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
        public float[] readRawPackedFloat() {
            try {
                var length = readLengthDelimitedPropertyLength();
                var segment = readRawMemorySegment(length); // Zero copy
                return toFloatArrayLE(segment);
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public double[] readRawPackedDouble() {
            try {
                var length = readLengthDelimitedPropertyLength();
                var segment = readRawMemorySegment(length); // Zero copy
                return toDoubleArrayLE(segment);
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
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
            try {
                var length = readLengthDelimitedPropertyLength();
                var segment = readRawMemorySegment(length); // Zero copy
                return toBooleanArray(segment);
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public int[] readRawPackedFixedInt32() {
            try {
                var length = readRawVarInt32();
                var segment = readRawMemorySegment(length); // Zero copy
                return toIntArrayLE(segment);
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public long[] readRawPackedFixedInt64() {
            try {
                var length = readRawVarInt32();
                var segment = readRawMemorySegment(length); // Zero copy
                return toLongArrayLE(segment);
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }
    }

    private static final class InputStreamReader extends ProtobufReader {
        private final InputStream inputStream;
        private final boolean autoclose;
        private final long length;
        private long position;

        private final byte[] buffer;
        private int bufferPosition;
        private int bufferLimit;

        InputStreamReader(InputStream inputStream, boolean autoclose) {
            Objects.requireNonNull(inputStream, "inputStream cannot be null");
            this.inputStream = inputStream;
            this.autoclose = autoclose;
            this.length = -1;
            // This buffer can contain at most at the same time:
            //   + 1 byte, used by the isFinished method
            //   + 16 bytes, used by the varint64 decoder
            this.buffer = new byte[17];
        }

        private InputStreamReader(InputStream inputStream, long length, byte[] buffer, int bufferPosition, int bufferLimit) {
            this.inputStream = inputStream;
            this.autoclose = false;
            this.length = length;
            this.buffer = buffer;
            this.bufferPosition = bufferPosition;
            this.bufferLimit = bufferLimit;
        }

        @Override
        public byte readRawByte() {
            if (bufferPosition < bufferLimit) {
                position++;
                return buffer[bufferPosition++];
            } else {
                try {
                    var read = inputStream.read();
                    if (read == -1) {
                        throw ProtobufDeserializationException.truncatedMessage();
                    } else {
                        position++;
                        return (byte) read;
                    }
                } catch (IOException exception) {
                    throw ProtobufDeserializationException.truncatedMessage(exception);
                }
            }
        }

        @Override
        public byte[] readRawBytes(int size) {
            try {
                var result = new byte[size];
                if(consumeBufferOrStream(result, 0, size) != size) {
                    throw ProtobufDeserializationException.truncatedMessage();
                }
                return result;
            } catch (NegativeArraySizeException _) {
                throw new IllegalArgumentException("size cannot be negative");
            }
        }

        private int consumeBufferOrStream(byte[] output, int offset, int length) {
            checkConsume(output, offset, length);

            var totalReadLength = 0;

            // Read from buffer
            if (bufferPosition < bufferLimit) {
                var cappedBufferLength = Math.min(bufferLimit - bufferPosition, length);
                System.arraycopy(buffer, bufferPosition, output, offset + totalReadLength, cappedBufferLength);
                totalReadLength += cappedBufferLength;
                bufferPosition += cappedBufferLength;
                position += cappedBufferLength;
            }

            // Read from stream
            if(totalReadLength < length) {
                totalReadLength += consumeStream(output, offset + totalReadLength, length - totalReadLength);
            }

            return totalReadLength;
        }

        private int consumeStream(byte[] output, int offset, int length) {
            checkConsume(output, offset, length);

            var totalReadLength = 0;
            while (totalReadLength < length) {
                try {
                    var currentReadLength = inputStream.read(output, offset + totalReadLength, length - totalReadLength);
                    if (currentReadLength == -1) {
                        break;
                    } else {
                        totalReadLength += currentReadLength;
                        position += currentReadLength;
                    }
                } catch (IOException exception) {
                    throw ProtobufDeserializationException.truncatedMessage(exception);
                }
            }
            return totalReadLength;
        }

        private static void checkConsume(byte[] output, int offset, int length) {
            Objects.requireNonNull(output, "output cannot be null");

            if(offset < 0) {
                throw new IllegalArgumentException("offset cannot be negative");
            }

            if(length < 0) {
                throw new IllegalArgumentException("length cannot be negative");
            }

            var finalOffset = offset + length;
            if (finalOffset > output.length) {
                throw new IndexOutOfBoundsException("write offset cannot be greater than array length: " + finalOffset + " > " + output.length);
            }
        }

        @Override
        public ByteBuffer readRawBuffer(int size) {
            var bytes = readRawBytes(size);
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public MemorySegment readRawMemorySegment(int size) {
            var bytes = readRawBytes(size);
            return MemorySegment.ofArray(bytes);
        }

        @Override
        public DataType rawDataTypePreference() {
            return DataType.BYTE_ARRAY;
        }

        @Override
        public boolean isFinished() {
            if (length != -1) {
                return position >= length;
            } else if (bufferPosition < bufferLimit) {
                return false;
            } else {
                try {
                    var read = inputStream.read();
                    if (read == -1) {
                        position = length;
                        return true;
                    } else {
                        buffer[bufferPosition = 0] = (byte) read;
                        bufferLimit = 1;
                        return false;
                    }
                } catch (IOException exception) {
                    throw ProtobufDeserializationException.truncatedMessage(exception);
                }
            }
        }

        @Override
        public void skipRawBytes(int length) {
            if (length < 0) {
                throw new IllegalArgumentException("length cannot be negative");
            }

            if (length > 0 && bufferPosition < bufferLimit) {
                var cappedBufferLength = Math.min(length, bufferLimit - bufferPosition);
                length -= cappedBufferLength;
                position += cappedBufferLength;
            }
            while (length > 0) {
                try {
                    var skipped = inputStream.skip(length);
                    if (skipped == 0) {
                        if (isFinished()) {
                            throw ProtobufDeserializationException.truncatedMessage();
                        }
                    } else {
                        length -= (int) skipped;
                        position += skipped;
                    }
                } catch (IOException exception) {
                    throw ProtobufDeserializationException.truncatedMessage(exception);
                }
            }
        }

        @Override
        public int readRawFixedInt32() {
            var expectedRead = Integer.BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if(expectedRead > 0 && consumeStream(buffer, bufferPosition, expectedRead) != expectedRead) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            var result = getIntLE(buffer, 0);
            bufferPosition += Integer.BYTES;

            return result;
        }

        @Override
        public long readRawFixedInt64() {
            var expectedRead = Long.BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if(expectedRead > 0 && consumeStream(buffer, bufferPosition, expectedRead) != expectedRead) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            var result = getLongLE(buffer, 0);
            bufferPosition += Long.BYTES;

            return result;
        }

        @Override
        public float readRawFloat() {
            var expectedRead = Float.BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if(expectedRead > 0 && consumeStream(buffer, bufferPosition, expectedRead) != expectedRead) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            var result = getFloatLE(buffer, 0);
            bufferPosition += Float.BYTES;

            return result;
        }

        @Override
        public double readRawDouble() {
            var expectedRead = Double.BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if(expectedRead > 0 && consumeStream(buffer, bufferPosition, expectedRead) != expectedRead) {
                throw ProtobufDeserializationException.truncatedMessage();
            }

            var result = getDoubleLE(buffer, 0);
            bufferPosition += Double.BYTES;

            return result;
        }

        @Override
        public InputStreamReader readRawLengthDelimited(int size) {
            var result = new InputStreamReader(inputStream, size, buffer, bufferPosition, bufferLimit);
            position += size;
            return result;
        }

        @Override
        public void close() throws IOException {
            if (autoclose) {
                inputStream.close();
            }
        }

        @Override
        public int readRawVarInt32() {
            var expectedRead = VARINT32_FAST_PATH_BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if (BMI2.isSupported() &&  (expectedRead <= 0 || consumeStream(buffer, bufferPosition, expectedRead) == expectedRead)) {
                var word = getLongLE(buffer, 0);
                var cont = ~word & VARINT32_CONT_BITS;
                var spread = cont ^ (cont - 1);
                var mask = spread & VARINT32_PAYLOAD_BITS;
                var read = Long.bitCount(spread) >>> 3;
                bufferPosition += read;
                return (int) Long.compress(word, mask);
            }

            var unsafeResult = getVarInt32SlowUnsafe();
            if (bufferPosition > bufferLimit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
            return unsafeResult;
        }

        // This method is unsafe
        // It can read beyond the allowed limit for buffer (bufferLimit)
        // The caller is responsible for checking for this condition after the read
        private int getVarInt32SlowUnsafe() {
            try {
                int x;
                if ((x = buffer[bufferPosition++]) >= 0) {
                    return x;
                } else if ((x ^= (buffer[bufferPosition++] << 7)) < 0) {
                    return x ^ (~0 << 7);
                } else if ((x ^= (buffer[bufferPosition++] << 14)) >= 0) {
                    return x ^ ((~0 << 7) ^ (~0 << 14));
                } else if ((x ^= (buffer[bufferPosition++] << 21)) < 0) {
                    return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
                } else {
                    x ^= buffer[bufferPosition++] << 28;
                    return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
                }
            } catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public long readRawVarInt64() {
            var expectedRead = VARINT64_FAST_PATH_BYTES - Math.max(bufferLimit - bufferPosition, 0);
            if (BMI2.isSupported() && (expectedRead <= 0 || consumeStream(buffer, bufferPosition, expectedRead) == expectedRead)) {
                var lo = getLongLE(buffer, 0);
                var hi = getLongLE(buffer, Long.BYTES);
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
                bufferPosition += (Long.bitCount(loSpread) + Long.bitCount(hiSpread)) >>> 3;
                return loResult | (hiResult << 56);
            }

            var unsafeResult = getVarInt64SlowUnsafe();
            if (bufferPosition > bufferLimit) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
            return unsafeResult;
        }

        // This method is unsafe
        // It can read beyond the allowed limit for buffer (bufferLimit)
        // The caller is responsible for checking for this condition after the read
        private long getVarInt64SlowUnsafe() {
            try {
                long x;
                int y;
                if ((y = buffer[bufferPosition++]) >= 0) {
                    return y;
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
                return x;
            }catch (IndexOutOfBoundsException _) {
                throw ProtobufDeserializationException.truncatedMessage();
            }
        }

        @Override
        public float[] readRawPackedFloat() {
            var length = readLengthDelimitedPropertyLength();
            var segment = readRawMemorySegment(length);
            return toFloatArrayLE(segment);
        }

        @Override
        public double[] readRawPackedDouble() {
            var length = readLengthDelimitedPropertyLength();
            var segment = readRawMemorySegment(length);
            return toDoubleArrayLE(segment);
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
            return toBooleanArray(segment);
        }

        @Override
        public int[] readRawPackedFixedInt32() {
            var length = readLengthDelimitedPropertyLength();
            var segment = readRawMemorySegment(length);
            return toIntArrayLE(segment);
        }

        @Override
        public long[] readRawPackedFixedInt64() {
            var length = readLengthDelimitedPropertyLength();
            var segment = readRawMemorySegment(length);
            return toLongArrayLE(segment);
        }
    }
}