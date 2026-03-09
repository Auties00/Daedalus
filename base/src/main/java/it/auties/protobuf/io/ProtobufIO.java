package it.auties.protobuf.io;

import jdk.incubator.vector.ByteVector;
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
    protected static final long INT8_PEXT_MASK = 0x000000000000017fL;
    protected static final long INT16_PEXT_MASK = 0x0000000000037f7fL;
    protected static final long INT32_PEXT_MASK = 0x0000000f7f7f7f7fL;
    protected static final long INT64_PEXT_MASK_LOW = 0x7f7f7f7f7f7f7f7fL;
    protected static final long INT64_PEXT_MASK_HIGH = 0x000000000000017fL;

    private static final VarHandle ARRAY_AS_INT16 = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_INT32 = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_INT64 = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle ARRAY_AS_DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle BUFFER_AS_INT16 = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_INT32 = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_INT64 = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_FLOAT = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_DOUBLE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final ValueLayout.OfInt INT32_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfLong INT64_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfFloat FLOAT_LAYOUT = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfDouble DOUBLE_LAYOUT = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    protected static final VectorSpecies<Byte> V64 = ByteVector.SPECIES_64;
    protected static final boolean SUPPORTS_V64 = isSpeciesSupported(V64);

    protected static final VectorSpecies<Byte> V128 = ByteVector.SPECIES_128;
    protected static final boolean SUPPORTS_V128 = isSpeciesSupported(V128);

    protected static final VectorSpecies<Byte> V256 = ByteVector.SPECIES_256;
    protected static final boolean SUPPORTS_V256 = isSpeciesSupported(V256);

    protected static final VectorSpecies<Byte> V512 = ByteVector.SPECIES_512;
    protected static final boolean SUPPORTS_V512 = isSpeciesSupported(V512);

    private static boolean isSpeciesSupported(VectorSpecies<?> species) {
        return species.vectorBitSize() <= ByteVector.SPECIES_PREFERRED.vectorBitSize();
    }

    protected static short getShortLE(byte[] arr, int offset) {
        return (short) ARRAY_AS_INT16.get(arr, offset);
    }

    protected static int getIntLE(byte[] arr, int offset) {
        return (int) ARRAY_AS_INT32.get(arr, offset);
    }

    protected static long getLongLE(byte[] arr, int offset) {
        return (long) ARRAY_AS_INT64.get(arr, offset);
    }

    protected static int getFloatLE(byte[] arr, int offset) {
        return (int) ARRAY_AS_FLOAT.get(arr, offset);
    }

    protected static long getDoubleLE(byte[] arr, int offset) {
        return (long) ARRAY_AS_DOUBLE.get(arr, offset);
    }

    protected static void putShortLE(byte[] arr, int offset, short value) {
        ARRAY_AS_INT16.set(arr, offset, value);
    }

    protected static void putIntLE(byte[] arr, int offset, int value) {
        ARRAY_AS_INT32.set(arr, offset, value);
    }

    protected static void putLongLE(byte[] arr, int offset, long value) {
        ARRAY_AS_INT64.set(arr, offset, value);
    }

    protected static void putFloatLE(byte[] arr, int offset, float value) {
        ARRAY_AS_FLOAT.set(arr, offset, value);
    }

    protected static void putDoubleLE(byte[] arr, int offset, double value) {
        ARRAY_AS_DOUBLE.set(arr, offset, value);
    }

    protected static short getShortLE(ByteBuffer buffer, int offset) {
        return (short) BUFFER_AS_INT16.get(buffer, offset);
    }

    protected static int getIntLE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_INT32.get(buffer, offset);
    }

    protected static long getLongLE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_INT64.get(buffer, offset);
    }

    protected static int getFloatLE(ByteBuffer buffer, int offset) {
        return (int) BUFFER_AS_FLOAT.get(buffer, offset);
    }

    protected static long getDoubleLE(ByteBuffer buffer, int offset) {
        return (long) BUFFER_AS_DOUBLE.get(buffer, offset);
    }

    protected static void putShortLE(ByteBuffer buffer, int offset, short value) {
        BUFFER_AS_INT16.set(buffer, offset, value);
    }

    protected static void putIntLE(ByteBuffer buffer, int offset, int value) {
        BUFFER_AS_INT32.set(buffer, offset, value);
    }

    protected static void putLongLE(ByteBuffer buffer, int offset, long value) {
        BUFFER_AS_INT64.set(buffer, offset, value);
    }

    protected static void putFloatLE(ByteBuffer arr, int offset, float value) {
        BUFFER_AS_FLOAT.set(arr, offset, value);
    }

    protected static void putDoubleLE(ByteBuffer arr, int offset, double value) {
        BUFFER_AS_DOUBLE.set(arr, offset, value);
    }

    protected static int[] toIntArrayLE(MemorySegment segment) {
        return segment.toArray(INT32_LAYOUT);
    }

    protected static long[] toLongArrayLE(MemorySegment segment) {
        return segment.toArray(INT64_LAYOUT);
    }

    protected static float[] toFloatArrayLE(MemorySegment segment) {
        return segment.toArray(FLOAT_LAYOUT);
    }

    protected static double[] toDoubleArrayLE(MemorySegment segment) {
        return segment.toArray(DOUBLE_LAYOUT);
    }

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
