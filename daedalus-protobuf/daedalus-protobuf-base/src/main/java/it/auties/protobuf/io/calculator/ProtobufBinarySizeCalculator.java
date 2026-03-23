package it.auties.protobuf.io.calculator;

import it.auties.protobuf.model.ProtobufWireType;

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
        for (int nlz = 1; nlz <= 32; nlz++) {
            NLZ32_TO_VAR_INT_SIZE[nlz] = (38 - nlz) / 7;
        }

        NLZ_TO_VAR_INT_SIZE = new int[128];
        for (int nlz = 0; nlz <= 64; nlz++) {
            NLZ_TO_VAR_INT_SIZE[nlz] = (70 - nlz) / 7;
        }
    }

    private ProtobufBinarySizeCalculator() {
        throw new UnsupportedOperationException("ProtobufBinarySizeCalculator is a utility class");
    }

    public static int getPropertyWireTagSize(long fieldIndex, int wireType) {
        return getVarInt64Size(ProtobufWireType.makeTag(fieldIndex, wireType));
    }

    public static int getVarInt32Size(int value) {
        return NLZ32_TO_VAR_INT_SIZE[Integer.numberOfLeadingZeros(value | 1) & 0x3F];
    }

    public static int getVarInt64Size(long value) {
        return NLZ_TO_VAR_INT_SIZE[Long.numberOfLeadingZeros(value | 1) & 0x7F];
    }

    public static long getVarIntPropertySize(long fieldIndex, long value) {
        return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
               + getVarInt64Size(value);
    }

    public static long getBoolPropertySize(long fieldIndex, boolean ignored) {
        return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
               + 1; // getVarInt64Size(0 or 1) = 1
    }

    public static long getBoolPropertySize(long fieldIndex, Boolean value) {
        if (value != null) {
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT)
                   + 1; // getVarInt64Size(0 or 1) = 1
        }
        return 0;
    }

    public static long getFixed64PropertySize(long fieldIndex, long ignored) {
        return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64)
               + Long.BYTES;
    }

    public static long getFixed64PropertySize(long fieldIndex, Long value) {
        if (value != null) {
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64)
                   + Long.BYTES;
        }
        return 0;
    }

    public static long getLengthDelimitedPropertySize(long fieldIndex, long length) {
        return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
               + getVarInt64Size(length)
               + length;
    }

    public static long getFixed32PropertySize(long fieldIndex, int ignored) {
        return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32)
               + Integer.BYTES;
    }

    public static long getFixed32PropertySize(long fieldIndex, Integer value) {
        if (value != null) {
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32)
                   + Integer.BYTES;
        }
        return 0;
    }

    public static int getVarInt32PackedSize(long fieldIndex, int[] values) {
        if (values != null) {
            var valueSize = 0;
            for (var value : values) {
                valueSize += getVarInt64Size(value);
            }
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valueSize)
                   + valueSize;
        }
        return 0;
    }

    public static int getVarInt64PackedSize(long fieldIndex, long[] values) {
        if (values != null) {
            var valueSize = 0;
            for (var value : values) {
                valueSize += getVarInt64Size(value);
            }
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valueSize)
                   + valueSize;
        }
        return 0;
    }

    public static int getBoolPackedSize(long fieldIndex, boolean[] values) {
        if (values != null) {
            var valuesSize = values.length;
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valuesSize)
                   + valuesSize;
        }
        return 0;
    }

    public static int getFixed64PackedSize(long fieldIndex, long[] values) {
        if (values != null) {
            var valuesSize = values.length * Long.BYTES;
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valuesSize)
                   + valuesSize;
        }
        return 0;
    }

    public static int getFixed32PackedSize(long fieldIndex, int[] values) {
        if (values != null) {
            var valuesSize = values.length * Integer.BYTES;
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valuesSize)
                   + valuesSize;
        }
        return 0;
    }

    public static int getFloatPackedSize(long fieldIndex, float[] values) {
        if (values != null) {
            var valuesSize = values.length * Float.BYTES;
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valuesSize)
                   + valuesSize;
        }
        return 0;
    }

    public static int getDoublePackedSize(long fieldIndex, double[] values) {
        if (values != null) {
            var valuesSize = values.length * Double.BYTES;
            return getPropertyWireTagSize(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED)
                   + getVarInt64Size(valuesSize)
                   + valuesSize;
        }
        return 0;
    }
}
