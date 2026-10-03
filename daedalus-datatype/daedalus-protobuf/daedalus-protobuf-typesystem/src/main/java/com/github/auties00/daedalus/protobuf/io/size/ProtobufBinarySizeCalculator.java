package com.github.auties00.daedalus.protobuf.io.size;

import com.github.auties00.daedalus.typesystem.util.CharsetUtils;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;


public final class ProtobufBinarySizeCalculator {
    // Branchless varint32 size via lookup table indexed by numberOfLeadingZeros.
    // A negative var-int32 is sign-extended to 64 bits on the wire, so always takes 10 bytes.
    // A non-negative value takes ceil(bits/7) bytes, computed as (38 - nlz) / 7
    // where 38 = 32 (int width) + 6 (so integer division rounds up by 7s).
    // The table maps nlz -> size for all cases:
    //   nlz=0  (negative)                   -> 10 (manual override; 38/7=5 is wrong)
    //   nlz=1  (2^30..2^31-1)              -> 5
    //   nlz=25 (64..127)                    -> 1
    //   nlz=31 (1) or nlz=32 (0, via |1)   -> 1
    // Table is sized to 64 (power of 2) so the & 0x3F mask lets C2 elide bounds checks.
    private static final int[] NLZ32_TO_VAR_INT_SIZE;

    // Branchless varint64 size via lookup table indexed by numberOfLeadingZeros.
    // A negative var-int always takes 10 bytes; a non-negative value takes ceil(bits/7) bytes.
    // The table maps nlz -> (70 - nlz) / 7, which gives the correct size for all cases:
    //   nlz=0  (negative)                   -> 10
    //   nlz=1  (2^62..2^63-1)               -> 9
    //   nlz=57 (64..127)                    -> 1
    //   nlz=63 (1) or nlz=64 (0, via |1)    -> 1
    private static final int[] NLZ_TO_VAR_INT_SIZE;

    static {
        NLZ32_TO_VAR_INT_SIZE = new int[64];
        NLZ32_TO_VAR_INT_SIZE[0] = 10;
        for (var nlz = 1; nlz <= 32; nlz++) {
            NLZ32_TO_VAR_INT_SIZE[nlz] = (38 - nlz) / 7;
        }

        NLZ_TO_VAR_INT_SIZE = new int[128];
        for (var nlz = 0; nlz <= 64; nlz++) {
            NLZ_TO_VAR_INT_SIZE[nlz] = (70 - nlz) / 7;
        }
    }

    private ProtobufBinarySizeCalculator() {
        throw new UnsupportedOperationException("ProtobufBinarySizeCalculator is a utility class");
    }

    public static int getFieldWireTagSize(int fieldIndex, int wireType) {
        return getVarInt64Size(ProtobufWireType.makeTag(fieldIndex, wireType));
    }

    public static int getVarInt32Size(int value) {
        return NLZ32_TO_VAR_INT_SIZE[Integer.numberOfLeadingZeros(value | 1) & 0x3F];
    }

    public static int getVarInt64Size(long value) {
        return NLZ_TO_VAR_INT_SIZE[Long.numberOfLeadingZeros(value | 1) & 0x7F];
    }

    // uint32 values, and the zigzag encoding of sint32 values, are unsigned: they are zero extended, never sign extended
    public static int getUnsignedVarInt32Size(int value) {
        return getVarInt64Size(Integer.toUnsignedLong(value));
    }

    public static int getZigZagVarInt32Size(int value) {
        return getUnsignedVarInt32Size((value << 1) ^ (value >> 31));
    }

    public static int getZigZagVarInt64Size(long value) {
        return getVarInt64Size((value << 1) ^ (value >> 63));
    }

    public static long getFloatFieldSize(int fieldIndex, float ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32)
                + Float.BYTES;
    }

    public static long getFloatFieldSize(int fieldIndex, Float value) {
        return value != null ? getFloatFieldSize(fieldIndex, value.floatValue()) : 0;
    }

    public static long getDoubleFieldSize(int fieldIndex, double ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64)
                + Double.BYTES;
    }

    public static long getDoubleFieldSize(int fieldIndex, Double value) {
        return value != null ? getDoubleFieldSize(fieldIndex, value.doubleValue()) : 0;
    }

    public static long getBoolFieldSize(int fieldIndex, boolean ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + 1;
    }

    public static long getBoolFieldSize(int fieldIndex, Boolean value) {
        return value != null ? getBoolFieldSize(fieldIndex, value.booleanValue()) : 0;
    }

    public static long getInt32FieldSize(int fieldIndex, int value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getVarInt32Size(value);
    }

    public static long getInt32FieldSize(int fieldIndex, Integer value) {
        return value != null ? getInt32FieldSize(fieldIndex, value.intValue()) : 0;
    }

    public static long getUInt32FieldSize(int fieldIndex, int value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getUnsignedVarInt32Size(value);
    }

    public static long getUInt32FieldSize(int fieldIndex, Integer value) {
        return value != null ? getUInt32FieldSize(fieldIndex, value.intValue()) : 0;
    }

    public static long getSInt32FieldSize(int fieldIndex, int value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getZigZagVarInt32Size(value);
    }

    public static long getSInt32FieldSize(int fieldIndex, Integer value) {
        return value != null ? getSInt32FieldSize(fieldIndex, value.intValue()) : 0;
    }

    public static long getInt64FieldSize(int fieldIndex, long value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getVarInt64Size(value);
    }

    public static long getInt64FieldSize(int fieldIndex, Long value) {
        return value != null ? getInt64FieldSize(fieldIndex, value.longValue()) : 0;
    }

    public static long getUInt64FieldSize(int fieldIndex, long value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getVarInt64Size(value);
    }

    public static long getUInt64FieldSize(int fieldIndex, Long value) {
        return value != null ? getUInt64FieldSize(fieldIndex, value.longValue()) : 0;
    }

    public static long getSInt64FieldSize(int fieldIndex, long value) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                + getZigZagVarInt64Size(value);
    }

    public static long getSInt64FieldSize(int fieldIndex, Long value) {
        return value != null ? getSInt64FieldSize(fieldIndex, value.longValue()) : 0;
    }

    public static long getFixed32FieldSize(int fieldIndex, int ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32)
                + Integer.BYTES;
    }

    public static long getFixed32FieldSize(int fieldIndex, Integer value) {
        return value != null ? getFixed32FieldSize(fieldIndex, value.intValue()) : 0;
    }

    public static long getSFixed32FieldSize(int fieldIndex, int ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32)
                + Integer.BYTES;
    }

    public static long getSFixed32FieldSize(int fieldIndex, Integer value) {
        return value != null ? getSFixed32FieldSize(fieldIndex, value.intValue()) : 0;
    }

    public static long getFixed64FieldSize(int fieldIndex, long ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64)
                + Long.BYTES;
    }

    public static long getFixed64FieldSize(int fieldIndex, Long value) {
        return value != null ? getFixed64FieldSize(fieldIndex, value.longValue()) : 0;
    }

    public static long getSFixed64FieldSize(int fieldIndex, long ignored) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64)
                + Long.BYTES;
    }

    public static long getSFixed64FieldSize(int fieldIndex, Long value) {
        return value != null ? getSFixed64FieldSize(fieldIndex, value.longValue()) : 0;
    }

    public static long getStringFieldSize(int fieldIndex, String value) {
        if (value == null) {
            return 0;
        }
        var length = CharsetUtils.utf8Length(value);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(length)
                + length;
    }

    public static long getBytesFieldSize(int fieldIndex, byte[] value) {
        if (value == null) {
            return 0;
        }
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(value.length)
                + value.length;
    }

    public static long getMessageFieldSize(int fieldIndex, int contentSize) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(contentSize)
                + contentSize;
    }

    public static long getStartGroupFieldSize(int fieldIndex) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_START_OBJECT);
    }

    public static long getEndGroupFieldSize(int fieldIndex) {
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_END_OBJECT);
    }

    public static int getPackedFloatSize(float[] values) {
        return values.length * Float.BYTES;
    }

    public static int getPackedDoubleSize(double[] values) {
        return values.length * Double.BYTES;
    }

    public static int getPackedBoolSize(boolean[] values) {
        return values.length;
    }

    public static int getPackedInt32Size(int[] values) {
        var size = 0;
        for (var value : values) {
            size += getVarInt32Size(value);
        }
        return size;
    }

    public static int getPackedUInt32Size(int[] values) {
        var size = 0;
        for (var value : values) {
            size += getUnsignedVarInt32Size(value);
        }
        return size;
    }

    public static int getPackedSInt32Size(int[] values) {
        var size = 0;
        for (var value : values) {
            size += getZigZagVarInt32Size(value);
        }
        return size;
    }

    public static int getPackedInt64Size(long[] values) {
        var size = 0;
        for (var value : values) {
            size += getVarInt64Size(value);
        }
        return size;
    }

    public static int getPackedUInt64Size(long[] values) {
        return getPackedInt64Size(values);
    }

    public static int getPackedSInt64Size(long[] values) {
        var size = 0;
        for (var value : values) {
            size += getZigZagVarInt64Size(value);
        }
        return size;
    }

    public static int getPackedFixed32Size(int[] values) {
        return values.length * Integer.BYTES;
    }

    public static int getPackedSFixed32Size(int[] values) {
        return getPackedFixed32Size(values);
    }

    public static int getPackedFixed64Size(long[] values) {
        return values.length * Long.BYTES;
    }

    public static int getPackedSFixed64Size(long[] values) {
        return getPackedFixed64Size(values);
    }

    public static int getPackedFloatFieldSize(int fieldIndex, float[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedFloatSize(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedDoubleFieldSize(int fieldIndex, double[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedDoubleSize(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedBoolFieldSize(int fieldIndex, boolean[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedBoolSize(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedInt32FieldSize(int fieldIndex, int[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedInt32Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedUInt32FieldSize(int fieldIndex, int[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedUInt32Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedSInt32FieldSize(int fieldIndex, int[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedSInt32Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedInt64FieldSize(int fieldIndex, long[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedInt64Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedUInt64FieldSize(int fieldIndex, long[] values) {
        return getPackedInt64FieldSize(fieldIndex, values);
    }

    public static int getPackedSInt64FieldSize(int fieldIndex, long[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedSInt64Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedFixed32FieldSize(int fieldIndex, int[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedFixed32Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedSFixed32FieldSize(int fieldIndex, int[] values) {
        return getPackedFixed32FieldSize(fieldIndex, values);
    }

    public static int getPackedFixed64FieldSize(int fieldIndex, long[] values) {
        if (values == null) {
            return 0;
        }
        var valuesSize = getPackedFixed64Size(values);
        return getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                + getVarInt32Size(valuesSize)
                + valuesSize;
    }

    public static int getPackedSFixed64FieldSize(int fieldIndex, long[] values) {
        return getPackedFixed64FieldSize(fieldIndex, values);
    }

    /**
     * Computes the wire-format size of an unknown field entry.
     *
     * <p>Mirrors {@code ProtobufBinaryWriter.writeUnknownField} so that
     * {@code sizeOf} and {@code encode} agree on the byte length emitted for
     * any value preserved by a {@link com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage.UnknownFields}
     * store.
     *
     * @param fieldIndex the field number the entry will be tagged with on the wire
     * @param value      the binary unknown field value
     * @return the number of bytes the encoded entry will occupy
     */
    public static long getUnknownFieldSize(int fieldIndex, ProtobufUnknownValue.Binary value) {
        return switch (value) {
            case null -> 0L;
            case ProtobufUnknownValue.Binary.Fixed32 _ ->
                    getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32) + Integer.BYTES;
            case ProtobufUnknownValue.Binary.Fixed64 _ ->
                    getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64) + Long.BYTES;
            case ProtobufUnknownValue.Binary.VarInt(var varIntValue) ->
                    getInt64FieldSize(fieldIndex, varIntValue);
            case ProtobufUnknownValue.Binary.LengthDelimited lengthDelimited -> {
                var payloadLength = switch (lengthDelimited) {
                    case ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(var array) -> array.length;
                    case ProtobufUnknownValue.Binary.LengthDelimited.ByteBufferBacked(var buffer) -> buffer.remaining();
                    case ProtobufUnknownValue.Binary.LengthDelimited.MemorySegmentBacked(var _, var length) -> length;
                };
                yield getFieldWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                        + getVarInt32Size(payloadLength)
                        + payloadLength;
            }
            case ProtobufUnknownValue.Binary.Group(var groupValue) ->
                    getStartGroupFieldSize(fieldIndex) + groupValue.wireSize() + getEndGroupFieldSize(fieldIndex);
        };
    }
}
