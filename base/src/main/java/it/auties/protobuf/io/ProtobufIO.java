package it.auties.protobuf.io;

import it.auties.protobuf.exception.ProtobufSerializationException;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public sealed abstract class ProtobufIO
        implements AutoCloseable
        permits ProtobufReader, ProtobufWriter {
    protected static final long INT64_PEXT_MASK_LOW = 0x7f7f7f7f7f7f7f7fL;
    protected static final long INT64_PEXT_MASK_HIGH = 0x000000000000017fL;

    protected static final long VARINT32_CONT_BITS = 0x00000080_80808080L;
    protected static final long VARINT32_PAYLOAD_BITS = 0x0000007F_7F7F7F7FL;
    protected static final long VARINT64_LO_CONT_BITS = 0x80808080_80808080L;
    protected static final long VARINT64_HI_CONT_BITS = 0x8080L;
    protected static final long VARINT64_HI_PAYLOAD_BITS = 0x7F7FL;

    protected static final int VARINT32_FAST_PATH_BYTES = Long.BYTES;
    protected static final int VARINT64_FAST_PATH_BYTES = Long.BYTES * 2;

    private static final int[]  VARINT64_SIZE_TABLE    = new int[128];
    private static final long[] VARINT64_LO_CONT_TABLE = new long[128];
    private static final long[] VARINT64_HI_CONT_TABLE = new long[128];
    private static final int[]  VARINT32_SIZE_TABLE    = new int[64];
    private static final long[] VARINT32_LO_CONT_TABLE = new long[64];

    static {
        for (int nlz = 0; nlz <= 64; nlz++) {
            int size = (70 - nlz) / 7;
            VARINT64_SIZE_TABLE[nlz] = size;
            int loContBytes = Math.min(size, 9) - 1;
            long loMask = loContBytes <= 0 ? 0L
                    : ((1L << (loContBytes * 8)) - 1) & VARINT64_LO_CONT_BITS;
            VARINT64_LO_CONT_TABLE[nlz] = loMask;
            VARINT64_HI_CONT_TABLE[nlz] = size == 10 ? 0x80L : 0L;
        }
        for (int nlz = 1; nlz <= 32; nlz++) {
            int size = (38 - nlz) / 7;
            VARINT32_SIZE_TABLE[nlz] = size;
            long loMask = size <= 1 ? 0L
                    : ((1L << ((size - 1) * 8)) - 1) & VARINT64_LO_CONT_BITS;
            VARINT32_LO_CONT_TABLE[nlz] = loMask;
        }
        VARINT32_SIZE_TABLE[0] = 10;
    }

    private static final VarHandle ARRAY_AS_INT16_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_INT32_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_INT64_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_FLOAT_LE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_DOUBLE_LE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle ARRAY_AS_INT16_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle ARRAY_AS_INT32_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle ARRAY_AS_INT64_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle ARRAY_AS_FLOAT_BE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle ARRAY_AS_DOUBLE_BE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    private static final VarHandle BUFFER_AS_INT16_LE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_INT32_LE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_INT64_LE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_FLOAT_LE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_DOUBLE_LE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle BUFFER_AS_INT16_BE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle BUFFER_AS_INT32_BE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle BUFFER_AS_INT64_BE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle BUFFER_AS_FLOAT_BE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle BUFFER_AS_DOUBLE_BE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    private static final ValueLayout.OfShort INT16_LE_LAYOUT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfInt INT32_LE_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfLong INT64_LE_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfFloat FLOAT_LE_LAYOUT = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfDouble DOUBLE_LE_LAYOUT = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    private static final ValueLayout.OfShort INT16_BE_LAYOUT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfInt INT32_BE_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfLong INT64_BE_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfFloat FLOAT_BE_LAYOUT = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfDouble DOUBLE_BE_LAYOUT = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    protected static final VectorSpecies<Byte> SPECIES_PREFERRED = ByteVector.SPECIES_PREFERRED;

    //region get fixed

    protected static short getShortLE(byte[] arr, int offset) {
        return (short) ARRAY_AS_INT16_LE.get(arr, offset);
    }

    protected static int getIntLE(byte[] arr, int offset) {
        return (int) ARRAY_AS_INT32_LE.get(arr, offset);
    }

    protected static long getLongLE(byte[] arr, int offset) {
        return (long) ARRAY_AS_INT64_LE.get(arr, offset);
    }

    protected static int getFloatLE(byte[] arr, int offset) {
        return (int) ARRAY_AS_FLOAT_LE.get(arr, offset);
    }

    protected static long getDoubleLE(byte[] arr, int offset) {
        return (long) ARRAY_AS_DOUBLE_LE.get(arr, offset);
    }

    protected static short getShortBE(byte[] arr, int offset) {
        return (short) ARRAY_AS_INT16_BE.get(arr, offset);
    }

    protected static int getIntBE(byte[] arr, int offset) {
        return (int) ARRAY_AS_INT32_BE.get(arr, offset);
    }

    protected static long getLongBE(byte[] arr, int offset) {
        return (long) ARRAY_AS_INT64_BE.get(arr, offset);
    }

    protected static int getFloatBE(byte[] arr, int offset) {
        return (int) ARRAY_AS_FLOAT_BE.get(arr, offset);
    }

    protected static long getDoubleBE(byte[] arr, int offset) {
        return (long) ARRAY_AS_DOUBLE_BE.get(arr, offset);
    }

    protected static short getShortLE(ByteBuffer buffer, int offset) {
        return (short) BUFFER_AS_INT16_LE.get(buffer, offset);
    }

    protected static int getIntLE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_INT32_LE.get(buffer, offset);
    }

    protected static long getLongLE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_INT64_LE.get(buffer, offset);
    }

    protected static int getFloatLE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_FLOAT_LE.get(buffer, offset);
    }

    protected static long getDoubleLE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_DOUBLE_LE.get(buffer, offset);
    }

    protected static short getShortBE(ByteBuffer buffer, int offset) {
        return (short) BUFFER_AS_INT16_BE.get(buffer, offset);
    }

    protected static int getIntBE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_INT32_BE.get(buffer, offset);
    }

    protected static long getLongBE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_INT64_BE.get(buffer, offset);
    }

    protected static int getFloatBE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_FLOAT_BE.get(buffer, offset);
    }

    protected static long getDoubleBE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_DOUBLE_BE.get(buffer, offset);
    }

    protected static short getShortLE(MemorySegment segment, long offset) {
        return segment.get(INT16_LE_LAYOUT, offset);
    }

    protected static int getIntLE(MemorySegment segment, long offset) {
        return segment.get(INT32_LE_LAYOUT, offset);
    }

    protected static long getLongLE(MemorySegment segment, long offset) {
        return segment.get(INT64_LE_LAYOUT, offset);
    }

    protected static float getFloatLE(MemorySegment segment, long offset) {
        return segment.get(FLOAT_LE_LAYOUT, offset);
    }

    protected static double getDoubleLE(MemorySegment segment, long offset) {
        return segment.get(DOUBLE_LE_LAYOUT, offset);
    }

    protected static short getShortBE(MemorySegment segment, long offset) {
        return segment.get(INT16_BE_LAYOUT, offset);
    }

    protected static int getIntBE(MemorySegment segment, long offset) {
        return segment.get(INT32_BE_LAYOUT, offset);
    }

    protected static long getLongBE(MemorySegment segment, long offset) {
        return segment.get(INT64_BE_LAYOUT, offset);
    }

    protected static float getFloatBE(MemorySegment segment, long offset) {
        return segment.get(FLOAT_BE_LAYOUT, offset);
    }

    protected static double getDoubleBE(MemorySegment segment, long offset) {
        return segment.get(DOUBLE_BE_LAYOUT, offset);
    }

    //endregion

    //region put fixed

    protected static void putShortLE(byte[] arr, int offset, short value) {
        ARRAY_AS_INT16_LE.set(arr, offset, value);
    }

    protected static void putIntLE(byte[] arr, int offset, int value) {
        ARRAY_AS_INT32_LE.set(arr, offset, value);
    }

    protected static void putLongLE(byte[] arr, int offset, long value) {
        ARRAY_AS_INT64_LE.set(arr, offset, value);
    }

    protected static void putFloatLE(byte[] arr, int offset, float value) {
        ARRAY_AS_FLOAT_LE.set(arr, offset, value);
    }

    protected static void putDoubleLE(byte[] arr, int offset, double value) {
        ARRAY_AS_DOUBLE_LE.set(arr, offset, value);
    }

    protected static void putShortBE(byte[] arr, int offset, short value) {
        ARRAY_AS_INT16_BE.set(arr, offset, value);
    }

    protected static void putIntBE(byte[] arr, int offset, int value) {
        ARRAY_AS_INT32_BE.set(arr, offset, value);
    }

    protected static void putLongBE(byte[] arr, int offset, long value) {
        ARRAY_AS_INT64_BE.set(arr, offset, value);
    }

    protected static void putFloatBE(byte[] arr, int offset, float value) {
        ARRAY_AS_FLOAT_BE.set(arr, offset, value);
    }

    protected static void putDoubleBE(byte[] arr, int offset, double value) {
        ARRAY_AS_DOUBLE_BE.set(arr, offset, value);
    }

    protected static void putShortLE(ByteBuffer buffer, int offset, short value) {
        BUFFER_AS_INT16_LE.set(buffer, offset, value);
    }

    protected static void putIntLE(ByteBuffer buffer, int offset, int value) {
        BUFFER_AS_INT32_LE.set(buffer, offset, value);
    }

    protected static void putLongLE(ByteBuffer buffer, int offset, long value) {
        BUFFER_AS_INT64_LE.set(buffer, offset, value);
    }

    protected static void putFloatLE(ByteBuffer buffer, int offset, float value) {
        BUFFER_AS_FLOAT_LE.set(buffer, offset, value);
    }

    protected static void putDoubleLE(ByteBuffer buffer, int offset, double value) {
        BUFFER_AS_DOUBLE_LE.set(buffer, offset, value);
    }

    protected static void putShortBE(ByteBuffer buffer, int offset, short value) {
        BUFFER_AS_INT16_BE.set(buffer, offset, value);
    }

    protected static void putIntBE(ByteBuffer buffer, int offset, int value) {
        BUFFER_AS_INT32_BE.set(buffer, offset, value);
    }

    protected static void putLongBE(ByteBuffer buffer, int offset, long value) {
        BUFFER_AS_INT64_BE.set(buffer, offset, value);
    }

    protected static void putFloatBE(ByteBuffer buffer, int offset, float value) {
        BUFFER_AS_FLOAT_BE.set(buffer, offset, value);
    }

    protected static void putDoubleBE(ByteBuffer buffer, int offset, double value) {
        BUFFER_AS_DOUBLE_BE.set(buffer, offset, value);
    }

    protected static void putShortLE(MemorySegment segment, long offset, short value) {
        segment.set(INT16_LE_LAYOUT, offset, value);
    }

    protected static void putIntLE(MemorySegment segment, long offset, int value) {
        segment.set(INT32_LE_LAYOUT, offset, value);
    }

    protected static void putLongLE(MemorySegment segment, long offset, long value) {
        segment.set(INT64_LE_LAYOUT, offset, value);
    }

    protected static void putFloatLE(MemorySegment segment, long offset, float value) {
        segment.set(FLOAT_LE_LAYOUT, offset, value);
    }

    protected static void putDoubleLE(MemorySegment segment, long offset, double value) {
        segment.set(DOUBLE_LE_LAYOUT, offset, value);
    }

    protected static void putShortBE(MemorySegment segment, long offset, short value) {
        segment.set(INT16_BE_LAYOUT, offset, value);
    }

    protected static void putIntBE(MemorySegment segment, long offset, int value) {
        segment.set(INT32_BE_LAYOUT, offset, value);
    }

    protected static void putLongBE(MemorySegment segment, long offset, long value) {
        segment.set(INT64_BE_LAYOUT, offset, value);
    }

    protected static void putFloatBE(MemorySegment segment, long offset, float value) {
        segment.set(FLOAT_BE_LAYOUT, offset, value);
    }

    protected static void putDoubleBE(MemorySegment segment, long offset, double value) {
        segment.set(DOUBLE_BE_LAYOUT, offset, value);
    }

    //endregion

    //region toArray fixed

    protected static int[] toIntArrayLE(byte[] arr, int offset, int length) {
        var result = new int[length / Integer.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(result);
        return result;
    }

    protected static long[] toLongArrayLE(byte[] arr, int offset, int length) {
        var result = new long[length / Long.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get(result);
        return result;
    }

    protected static float[] toFloatArrayLE(byte[] arr, int offset, int length) {
        var result = new float[length / Float.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(result);
        return result;
    }

    protected static double[] toDoubleArrayLE(byte[] arr, int offset, int length) {
        var result = new double[length / Double.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(result);
        return result;
    }

    protected static int[] toIntArrayBE(byte[] arr, int offset, int length) {
        var result = new int[length / Integer.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(result);
        return result;
    }

    protected static long[] toLongArrayBE(byte[] arr, int offset, int length) {
        var result = new long[length / Long.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.BIG_ENDIAN).asLongBuffer().get(result);
        return result;
    }

    protected static float[] toFloatArrayBE(byte[] arr, int offset, int length) {
        var result = new float[length / Float.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get(result);
        return result;
    }

    protected static double[] toDoubleArrayBE(byte[] arr, int offset, int length) {
        var result = new double[length / Double.BYTES];
        ByteBuffer.wrap(arr, offset, length).order(ByteOrder.BIG_ENDIAN).asDoubleBuffer().get(result);
        return result;
    }

    protected static boolean[] toBooleanArray(byte[] arr, int offset, int length) {
        var i = 0;
        var upper = SPECIES_PREFERRED.loopBound(length);
        var out = new boolean[length];
        for (; i < upper; i += SPECIES_PREFERRED.length()) {
            ByteVector.fromArray(SPECIES_PREFERRED, arr, offset + i)
                    .compare(VectorOperators.NE, (byte) 0)
                    .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = arr[offset + i] != 0;
        }
        return out;
    }

    protected static boolean[] toBooleanArray(ByteBuffer buffer) {
        if(buffer.hasArray()) {
            return toBooleanArray(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        } else {
            return toBooleanArray(MemorySegment.ofBuffer(buffer));
        }
    }

    protected static boolean[] toBooleanArray(MemorySegment segment) {
        var i = 0;
        int length;
        try {
            length = Math.toIntExact(segment.byteSize());
        } catch(ArithmeticException _) {
            throw ProtobufSerializationException.underflow();
        }
        var upper = SPECIES_PREFERRED.loopBound(length);
        var out = new boolean[length];
        for (; i < upper; i += SPECIES_PREFERRED.length()) {
            ByteVector.fromMemorySegment(SPECIES_PREFERRED, segment, i, ByteOrder.nativeOrder())
                    .compare(VectorOperators.NE, (byte) 0)
                    .intoArray(out, i);
        }
        for (; i < length; i++) {
            out[i] = segment.get(ValueLayout.JAVA_BYTE, i) != 0;
        }
        return out;
    }

    protected static int[] toIntArrayLE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.LITTLE_ENDIAN);
        var result = new int[buf.remaining() / Integer.BYTES];
        buf.asIntBuffer().get(result);
        return result;
    }

    protected static long[] toLongArrayLE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.LITTLE_ENDIAN);
        var result = new long[buf.remaining() / Long.BYTES];
        buf.asLongBuffer().get(result);
        return result;
    }

    protected static float[] toFloatArrayLE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.LITTLE_ENDIAN);
        var result = new float[buf.remaining() / Float.BYTES];
        buf.asFloatBuffer().get(result);
        return result;
    }

    protected static double[] toDoubleArrayLE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.LITTLE_ENDIAN);
        var result = new double[buf.remaining() / Double.BYTES];
        buf.asDoubleBuffer().get(result);
        return result;
    }

    protected static int[] toIntArrayBE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.BIG_ENDIAN);
        var result = new int[buf.remaining() / Integer.BYTES];
        buf.asIntBuffer().get(result);
        return result;
    }

    protected static long[] toLongArrayBE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.BIG_ENDIAN);
        var result = new long[buf.remaining() / Long.BYTES];
        buf.asLongBuffer().get(result);
        return result;
    }

    protected static float[] toFloatArrayBE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.BIG_ENDIAN);
        var result = new float[buf.remaining() / Float.BYTES];
        buf.asFloatBuffer().get(result);
        return result;
    }

    protected static double[] toDoubleArrayBE(ByteBuffer buffer) {
        var buf = buffer.order(ByteOrder.BIG_ENDIAN);
        var result = new double[buf.remaining() / Double.BYTES];
        buf.asDoubleBuffer().get(result);
        return result;
    }

    protected static int[] toIntArrayLE(MemorySegment segment) {
        return segment.toArray(INT32_LE_LAYOUT);
    }

    protected static long[] toLongArrayLE(MemorySegment segment) {
        return segment.toArray(INT64_LE_LAYOUT);
    }

    protected static float[] toFloatArrayLE(MemorySegment segment) {
        return segment.toArray(FLOAT_LE_LAYOUT);
    }

    protected static double[] toDoubleArrayLE(MemorySegment segment) {
        return segment.toArray(DOUBLE_LE_LAYOUT);
    }

    protected static int[] toIntArrayBE(MemorySegment segment) {
        return segment.toArray(INT32_BE_LAYOUT);
    }

    protected static long[] toLongArrayBE(MemorySegment segment) {
        return segment.toArray(INT64_BE_LAYOUT);
    }

    protected static float[] toFloatArrayBE(MemorySegment segment) {
        return segment.toArray(FLOAT_BE_LAYOUT);
    }

    protected static double[] toDoubleArrayBE(MemorySegment segment) {
        return segment.toArray(DOUBLE_BE_LAYOUT);
    }

    //endregion

    //region put varint LE

    protected static int putVarInt64LE(byte[] arr, int offset, long value) {
        if (arr.length - offset < 16) {
            return putVarInt64Slow(arr, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongLE(arr, offset, lo | loCont);
        putLongLE(arr, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32LE(byte[] arr, int offset, int value) {
        if (value < 0) {
            return putVarInt64LE(arr, offset, value);
        }
        if (arr.length - offset < 8) {
            return putVarInt32Slow(arr, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongLE(arr, offset, scattered | loCont);
        return size;
    }

    protected static int putVarInt64LE(ByteBuffer buffer, int offset, long value) {
        if (buffer.limit() - offset < 16) {
            return putVarInt64Slow(buffer, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongLE(buffer, offset, lo | loCont);
        putLongLE(buffer, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32LE(ByteBuffer buffer, int offset, int value) {
        if (value < 0) {
            return putVarInt64LE(buffer, offset, value);
        }
        if (buffer.limit() - offset < 8) {
            return putVarInt32Slow(buffer, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongLE(buffer, offset, scattered | loCont);
        return size;
    }

    protected static int putVarInt64LE(MemorySegment segment, long offset, long value) {
        if (segment.byteSize() - offset < 16) {
            return putVarInt64Slow(segment, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongLE(segment, offset, lo | loCont);
        putLongLE(segment, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32LE(MemorySegment segment, long offset, int value) {
        if (value < 0) {
            return putVarInt64LE(segment, offset, value);
        }
        if (segment.byteSize() - offset < 8) {
            return putVarInt32Slow(segment, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongLE(segment, offset, scattered | loCont);
        return size;
    }

    //endregion

    //region put varint BE

    protected static int putVarInt64BE(byte[] arr, int offset, long value) {
        if (arr.length - offset < 16) {
            return putVarInt64Slow(arr, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongBE(arr, offset, lo | loCont);
        putLongBE(arr, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32BE(byte[] arr, int offset, int value) {
        if (value < 0) {
            return putVarInt64BE(arr, offset, value);
        }
        if (arr.length - offset < 8) {
            return putVarInt32Slow(arr, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongBE(arr, offset, scattered | loCont);
        return size;
    }

    protected static int putVarInt64BE(ByteBuffer buffer, int offset, long value) {
        if (buffer.limit() - offset < 16) {
            return putVarInt64Slow(buffer, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongBE(buffer, offset, lo | loCont);
        putLongBE(buffer, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32BE(ByteBuffer buffer, int offset, int value) {
        if (value < 0) {
            return putVarInt64BE(buffer, offset, value);
        }
        if (buffer.limit() - offset < 8) {
            return putVarInt32Slow(buffer, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongBE(buffer, offset, scattered | loCont);
        return size;
    }

    protected static int putVarInt64BE(MemorySegment segment, long offset, long value) {
        if (segment.byteSize() - offset < 16) {
            return putVarInt64Slow(segment, offset, value);
        }
        var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
        var size = VARINT64_SIZE_TABLE[nlz];
        var loCont = VARINT64_LO_CONT_TABLE[nlz];
        var hiCont = VARINT64_HI_CONT_TABLE[nlz];
        var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
        var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
        putLongBE(segment, offset, lo | loCont);
        putLongBE(segment, offset + 8, hi | hiCont);
        return size;
    }

    protected static int putVarInt32BE(MemorySegment segment, long offset, int value) {
        if (value < 0) {
            return putVarInt64BE(segment, offset, value);
        }
        if (segment.byteSize() - offset < 8) {
            return putVarInt32Slow(segment, offset, value);
        }
        var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
        var size = VARINT32_SIZE_TABLE[nlz];
        var loCont = VARINT32_LO_CONT_TABLE[nlz];
        var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
        putLongBE(segment, offset, scattered | loCont);
        return size;
    }

    //endregion

    //region get varint slow path

    private static int getVarInt32Slow(byte[] arr, int offset) {
        int x;
        if ((x = arr[offset++]) >= 0) {
            return x;
        } else if ((x ^= (arr[offset++] << 7)) < 0) {
            return x ^ (~0 << 7);
        } else if ((x ^= (arr[offset++] << 14)) >= 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((x ^= (arr[offset++] << 21)) < 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else {
            x ^= arr[offset] << 28;
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
        }
    }

    private static long getVarInt64Slow(byte[] arr, int offset) {
        long x;
        int y;
        if ((y = arr[offset++]) >= 0) {
            return y;
        } else if ((y ^= (arr[offset++] << 7)) < 0) {
            x = y ^ (~0 << 7);
        } else if ((y ^= (arr[offset++] << 14)) >= 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((y ^= (arr[offset++] << 21)) < 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else if ((x = y ^ ((long) arr[offset++] << 28)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
        } else if ((x ^= ((long) arr[offset++] << 35)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
        } else if ((x ^= ((long) arr[offset++] << 42)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
        } else if ((x ^= ((long) arr[offset++] << 49)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
        } else if ((x ^= ((long) arr[offset++] << 56)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
        } else {
            x ^= ((long) arr[offset] << 63);
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
        }
        return x;
    }

    private static int getVarInt32Slow(ByteBuffer buffer, int offset) {
        int x;
        if ((x = buffer.get(offset++)) >= 0) {
            return x;
        } else if ((x ^= (buffer.get(offset++) << 7)) < 0) {
            return x ^ (~0 << 7);
        } else if ((x ^= (buffer.get(offset++) << 14)) >= 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((x ^= (buffer.get(offset++) << 21)) < 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else {
            x ^= buffer.get(offset) << 28;
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
        }
    }

    private static long getVarInt64Slow(ByteBuffer buffer, int offset) {
        long x;
        int y;
        if ((y = buffer.get(offset++)) >= 0) {
            return y;
        } else if ((y ^= (buffer.get(offset++) << 7)) < 0) {
            x = y ^ (~0 << 7);
        } else if ((y ^= (buffer.get(offset++) << 14)) >= 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((y ^= (buffer.get(offset++) << 21)) < 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else if ((x = y ^ ((long) buffer.get(offset++) << 28)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
        } else if ((x ^= ((long) buffer.get(offset++) << 35)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
        } else if ((x ^= ((long) buffer.get(offset++) << 42)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
        } else if ((x ^= ((long) buffer.get(offset++) << 49)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
        } else if ((x ^= ((long) buffer.get(offset++) << 56)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
        } else {
            x ^= ((long) buffer.get(offset) << 63);
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
        }
        return x;
    }

    private static int getVarInt32Slow(MemorySegment segment, long offset) {
        int x;
        if ((x = segment.get(ValueLayout.JAVA_BYTE, offset++)) >= 0) {
            return x;
        } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 7)) < 0) {
            return x ^ (~0 << 7);
        } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 14)) >= 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((x ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 21)) < 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else {
            x ^= segment.get(ValueLayout.JAVA_BYTE, offset) << 28;
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
        }
    }

    private static long getVarInt64Slow(MemorySegment segment, long offset) {
        long x;
        int y;
        if ((y = segment.get(ValueLayout.JAVA_BYTE, offset++)) >= 0) {
            return y;
        } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 7)) < 0) {
            x = y ^ (~0 << 7);
        } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 14)) >= 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((y ^= (segment.get(ValueLayout.JAVA_BYTE, offset++) << 21)) < 0) {
            x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else if ((x = y ^ ((long) segment.get(ValueLayout.JAVA_BYTE, offset++) << 28)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
        } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, offset++) << 35)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
        } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, offset++) << 42)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
        } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, offset++) << 49)) < 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49);
        } else if ((x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, offset++) << 56)) >= 0L) {
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56);
        } else {
            x ^= ((long) segment.get(ValueLayout.JAVA_BYTE, offset) << 63);
            x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42) ^ (~0L << 49) ^ (~0L << 56) ^ (~0L << 63);
        }
        return x;
    }

    //endregion

    //region put varint slow path

    private static int putVarInt32Slow(byte[] arr, int offset, int value) {
        int pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                arr[pos++] = (byte) value;
                return pos - offset;
            }
            arr[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    private static int putVarInt64Slow(byte[] arr, int offset, long value) {
        int pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                arr[pos++] = (byte) value;
                return pos - offset;
            }
            arr[pos++] = (byte) (((int) value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    private static int putVarInt32Slow(ByteBuffer buffer, int offset, int value) {
        int pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                buffer.put(pos++, (byte) value);
                return pos - offset;
            }
            buffer.put(pos++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    private static int putVarInt64Slow(ByteBuffer buffer, int offset, long value) {
        int pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buffer.put(pos++, (byte) value);
                return pos - offset;
            }
            buffer.put(pos++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    private static int putVarInt32Slow(MemorySegment segment, long offset, int value) {
        long pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) value);
                return (int) (pos - offset);
            }
            segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    private static int putVarInt64Slow(MemorySegment segment, long offset, long value) {
        long pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) value);
                return (int) (pos - offset);
            }
            segment.set(ValueLayout.JAVA_BYTE, pos++, (byte) (((int) value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    //endregion

    protected ProtobufIO() {

    }

    public abstract DataType rawDataTypePreference();

    @Override
    public abstract void close() throws IOException;

    public enum DataType {
        BYTE_ARRAY,
        BYTE_BUFFER,
        MEMORY_SEGMENT
    }
}
