package com.github.auties00.daedalus.typesystem.util;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Static utility methods for reading and writing primitive values at any offset of a
 * {@code byte[]}, a {@link ByteBuffer} or a {@link MemorySegment}, in little endian or big
 * endian byte order.
 *
 * <p>Every value is read or written at once, instead of byte by byte: the methods that operate
 * on a {@code byte[]} or a {@code ByteBuffer} use the view {@link VarHandle}s returned by
 * {@link MethodHandles#byteArrayViewVarHandle(Class, ByteOrder)} and
 * {@link MethodHandles#byteBufferViewVarHandle(Class, ByteOrder)}, and the methods that operate
 * on a {@code MemorySegment} use unaligned {@link ValueLayout}s with the requested byte order.
 * The offsets do not need to be aligned to the size of the values. As the handles and the
 * layouts are constants, the just in time compiler reduces each method to a single load or
 * store, swapping the bytes only if the byte order differs from the native one.
 *
 * <p>The methods that operate on a {@code ByteBuffer} use absolute indexes: they neither read
 * nor change the position, the limit or the byte order of the buffer.
 *
 * <p>Unless otherwise noted, passing a {@code null} array, buffer or segment to a method of this
 * class will cause a {@link NullPointerException} to be thrown.
 *
 * <p>This class is thread safe, as it has no state.
 */
public final class BinaryUtils {
    /**
     * The view of a {@code byte[]} as a {@code short[]} in little endian byte order.
     */
    private static final VarHandle ARRAY_AS_SHORT_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code short[]} in little endian byte order.
     */
    private static final VarHandle BUFFER_AS_SHORT_LE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The unaligned layout of a {@code short} in little endian byte order.
     */
    private static final ValueLayout.OfShort SEGMENT_SHORT_LE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code short[]} in big endian byte order.
     */
    private static final VarHandle ARRAY_AS_SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code short[]} in big endian byte order.
     */
    private static final VarHandle BUFFER_AS_SHORT_BE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The unaligned layout of a {@code short} in big endian byte order.
     */
    private static final ValueLayout.OfShort SEGMENT_SHORT_BE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code byte[]} as an {@code int[]} in little endian byte order.
     */
    private static final VarHandle ARRAY_AS_INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as an {@code int[]} in little endian byte order.
     */
    private static final VarHandle BUFFER_AS_INT_LE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The unaligned layout of an {@code int} in little endian byte order.
     */
    private static final ValueLayout.OfInt SEGMENT_INT_LE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code byte[]} as an {@code int[]} in big endian byte order.
     */
    private static final VarHandle ARRAY_AS_INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as an {@code int[]} in big endian byte order.
     */
    private static final VarHandle BUFFER_AS_INT_BE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The unaligned layout of an {@code int} in big endian byte order.
     */
    private static final ValueLayout.OfInt SEGMENT_INT_BE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code long[]} in little endian byte order.
     */
    private static final VarHandle ARRAY_AS_LONG_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code long[]} in little endian byte order.
     */
    private static final VarHandle BUFFER_AS_LONG_LE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The unaligned layout of a {@code long} in little endian byte order.
     */
    private static final ValueLayout.OfLong SEGMENT_LONG_LE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code long[]} in big endian byte order.
     */
    private static final VarHandle ARRAY_AS_LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code long[]} in big endian byte order.
     */
    private static final VarHandle BUFFER_AS_LONG_BE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The unaligned layout of a {@code long} in big endian byte order.
     */
    private static final ValueLayout.OfLong SEGMENT_LONG_BE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code float[]} in little endian byte order.
     */
    private static final VarHandle ARRAY_AS_FLOAT_LE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code float[]} in little endian byte order.
     */
    private static final VarHandle BUFFER_AS_FLOAT_LE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The unaligned layout of a {@code float} in little endian byte order.
     */
    private static final ValueLayout.OfFloat SEGMENT_FLOAT_LE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code float[]} in big endian byte order.
     */
    private static final VarHandle ARRAY_AS_FLOAT_BE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code float[]} in big endian byte order.
     */
    private static final VarHandle BUFFER_AS_FLOAT_BE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The unaligned layout of a {@code float} in big endian byte order.
     */
    private static final ValueLayout.OfFloat SEGMENT_FLOAT_BE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code double[]} in little endian byte order.
     */
    private static final VarHandle ARRAY_AS_DOUBLE_LE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code double[]} in little endian byte order.
     */
    private static final VarHandle BUFFER_AS_DOUBLE_LE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    /**
     * The unaligned layout of a {@code double} in little endian byte order.
     */
    private static final ValueLayout.OfDouble SEGMENT_DOUBLE_LE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * The view of a {@code byte[]} as a {@code double[]} in big endian byte order.
     */
    private static final VarHandle ARRAY_AS_DOUBLE_BE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The view of a {@code ByteBuffer} as a {@code double[]} in big endian byte order.
     */
    private static final VarHandle BUFFER_AS_DOUBLE_BE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    /**
     * The unaligned layout of a {@code double} in big endian byte order.
     */
    private static final ValueLayout.OfDouble SEGMENT_DOUBLE_BE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private BinaryUtils() {
        throw new UnsupportedOperationException("BinaryUtils is a utility class and cannot be instantiated");
    }

    /**
     * Reads a {@code short} in little endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it
     */
    public static short readShortLE(byte[] array, int offset) {
        return (short) ARRAY_AS_SHORT_LE.get(array, offset);
    }

    /**
     * Writes a {@code short} in little endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it
     */
    public static void writeShortLE(byte[] array, int offset, short value) {
        ARRAY_AS_SHORT_LE.set(array, offset, value);
    }

    /**
     * Reads a {@code short} in little endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it before the limit of the buffer
     */
    public static short readShortLE(ByteBuffer buffer, int index) {
        return (short) BUFFER_AS_SHORT_LE.get(buffer, index);
    }

    /**
     * Writes a {@code short} in little endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeShortLE(ByteBuffer buffer, int index, short value) {
        BUFFER_AS_SHORT_LE.set(buffer, index, value);
    }

    /**
     * Reads a {@code short} in little endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static short readShortLE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_SHORT_LE, offset);
    }

    /**
     * Writes a {@code short} in little endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeShortLE(MemorySegment segment, long offset, short value) {
        segment.set(SEGMENT_SHORT_LE, offset, value);
    }

    /**
     * Reads consecutive {@code short} values in little endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readShortsLE(MemorySegment segment, long offset, short[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_SHORT_LE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code short} values in little endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeShortsLE(MemorySegment segment, long offset, short[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_SHORT_LE, offset, count);
    }

    /**
     * Reads a {@code short} in big endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it
     */
    public static short readShortBE(byte[] array, int offset) {
        return (short) ARRAY_AS_SHORT_BE.get(array, offset);
    }

    /**
     * Writes a {@code short} in big endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it
     */
    public static void writeShortBE(byte[] array, int offset, short value) {
        ARRAY_AS_SHORT_BE.set(array, offset, value);
    }

    /**
     * Reads a {@code short} in big endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it before the limit of the buffer
     */
    public static short readShortBE(ByteBuffer buffer, int index) {
        return (short) BUFFER_AS_SHORT_BE.get(buffer, index);
    }

    /**
     * Writes a {@code short} in big endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeShortBE(ByteBuffer buffer, int index, short value) {
        BUFFER_AS_SHORT_BE.set(buffer, index, value);
    }

    /**
     * Reads a {@code short} in big endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static short readShortBE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_SHORT_BE, offset);
    }

    /**
     * Writes a {@code short} in big endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Short#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeShortBE(MemorySegment segment, long offset, short value) {
        segment.set(SEGMENT_SHORT_BE, offset, value);
    }

    /**
     * Reads consecutive {@code short} values in big endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readShortsBE(MemorySegment segment, long offset, short[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_SHORT_BE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code short} values in big endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeShortsBE(MemorySegment segment, long offset, short[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_SHORT_BE, offset, count);
    }

    /**
     * Reads an {@code int} in little endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it
     */
    public static int readIntLE(byte[] array, int offset) {
        return (int) ARRAY_AS_INT_LE.get(array, offset);
    }

    /**
     * Writes an {@code int} in little endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it
     */
    public static void writeIntLE(byte[] array, int offset, int value) {
        ARRAY_AS_INT_LE.set(array, offset, value);
    }

    /**
     * Reads an {@code int} in little endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it before the limit of the buffer
     */
    public static int readIntLE(ByteBuffer buffer, int index) {
        return (int) BUFFER_AS_INT_LE.get(buffer, index);
    }

    /**
     * Writes an {@code int} in little endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeIntLE(ByteBuffer buffer, int index, int value) {
        BUFFER_AS_INT_LE.set(buffer, index, value);
    }

    /**
     * Reads an {@code int} in little endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static int readIntLE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_INT_LE, offset);
    }

    /**
     * Writes an {@code int} in little endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeIntLE(MemorySegment segment, long offset, int value) {
        segment.set(SEGMENT_INT_LE, offset, value);
    }

    /**
     * Reads consecutive {@code int} values in little endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readIntsLE(MemorySegment segment, long offset, int[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_INT_LE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code int} values in little endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeIntsLE(MemorySegment segment, long offset, int[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_INT_LE, offset, count);
    }

    /**
     * Reads an {@code int} in big endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it
     */
    public static int readIntBE(byte[] array, int offset) {
        return (int) ARRAY_AS_INT_BE.get(array, offset);
    }

    /**
     * Writes an {@code int} in big endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it
     */
    public static void writeIntBE(byte[] array, int offset, int value) {
        ARRAY_AS_INT_BE.set(array, offset, value);
    }

    /**
     * Reads an {@code int} in big endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it before the limit of the buffer
     */
    public static int readIntBE(ByteBuffer buffer, int index) {
        return (int) BUFFER_AS_INT_BE.get(buffer, index);
    }

    /**
     * Writes an {@code int} in big endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeIntBE(ByteBuffer buffer, int index, int value) {
        BUFFER_AS_INT_BE.set(buffer, index, value);
    }

    /**
     * Reads an {@code int} in big endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static int readIntBE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_INT_BE, offset);
    }

    /**
     * Writes an {@code int} in big endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Integer#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeIntBE(MemorySegment segment, long offset, int value) {
        segment.set(SEGMENT_INT_BE, offset, value);
    }

    /**
     * Reads consecutive {@code int} values in big endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readIntsBE(MemorySegment segment, long offset, int[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_INT_BE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code int} values in big endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeIntsBE(MemorySegment segment, long offset, int[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_INT_BE, offset, count);
    }

    /**
     * Reads a {@code long} in little endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it
     */
    public static long readLongLE(byte[] array, int offset) {
        return (long) ARRAY_AS_LONG_LE.get(array, offset);
    }

    /**
     * Writes a {@code long} in little endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it
     */
    public static void writeLongLE(byte[] array, int offset, long value) {
        ARRAY_AS_LONG_LE.set(array, offset, value);
    }

    /**
     * Reads a {@code long} in little endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it before the limit of the buffer
     */
    public static long readLongLE(ByteBuffer buffer, int index) {
        return (long) BUFFER_AS_LONG_LE.get(buffer, index);
    }

    /**
     * Writes a {@code long} in little endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeLongLE(ByteBuffer buffer, int index, long value) {
        BUFFER_AS_LONG_LE.set(buffer, index, value);
    }

    /**
     * Reads a {@code long} in little endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static long readLongLE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_LONG_LE, offset);
    }

    /**
     * Writes a {@code long} in little endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeLongLE(MemorySegment segment, long offset, long value) {
        segment.set(SEGMENT_LONG_LE, offset, value);
    }

    /**
     * Reads consecutive {@code long} values in little endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readLongsLE(MemorySegment segment, long offset, long[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_LONG_LE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code long} values in little endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeLongsLE(MemorySegment segment, long offset, long[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_LONG_LE, offset, count);
    }

    /**
     * Reads a {@code long} in big endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it
     */
    public static long readLongBE(byte[] array, int offset) {
        return (long) ARRAY_AS_LONG_BE.get(array, offset);
    }

    /**
     * Writes a {@code long} in big endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it
     */
    public static void writeLongBE(byte[] array, int offset, long value) {
        ARRAY_AS_LONG_BE.set(array, offset, value);
    }

    /**
     * Reads a {@code long} in big endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it before the limit of the buffer
     */
    public static long readLongBE(ByteBuffer buffer, int index) {
        return (long) BUFFER_AS_LONG_BE.get(buffer, index);
    }

    /**
     * Writes a {@code long} in big endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeLongBE(ByteBuffer buffer, int index, long value) {
        BUFFER_AS_LONG_BE.set(buffer, index, value);
    }

    /**
     * Reads a {@code long} in big endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static long readLongBE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_LONG_BE, offset);
    }

    /**
     * Writes a {@code long} in big endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Long#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeLongBE(MemorySegment segment, long offset, long value) {
        segment.set(SEGMENT_LONG_BE, offset, value);
    }

    /**
     * Reads consecutive {@code long} values in big endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readLongsBE(MemorySegment segment, long offset, long[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_LONG_BE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code long} values in big endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeLongsBE(MemorySegment segment, long offset, long[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_LONG_BE, offset, count);
    }

    /**
     * Reads a {@code float} in little endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it
     */
    public static float readFloatLE(byte[] array, int offset) {
        return (float) ARRAY_AS_FLOAT_LE.get(array, offset);
    }

    /**
     * Writes a {@code float} in little endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it
     */
    public static void writeFloatLE(byte[] array, int offset, float value) {
        ARRAY_AS_FLOAT_LE.set(array, offset, value);
    }

    /**
     * Reads a {@code float} in little endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it before the limit of the buffer
     */
    public static float readFloatLE(ByteBuffer buffer, int index) {
        return (float) BUFFER_AS_FLOAT_LE.get(buffer, index);
    }

    /**
     * Writes a {@code float} in little endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeFloatLE(ByteBuffer buffer, int index, float value) {
        BUFFER_AS_FLOAT_LE.set(buffer, index, value);
    }

    /**
     * Reads a {@code float} in little endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static float readFloatLE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_FLOAT_LE, offset);
    }

    /**
     * Writes a {@code float} in little endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeFloatLE(MemorySegment segment, long offset, float value) {
        segment.set(SEGMENT_FLOAT_LE, offset, value);
    }

    /**
     * Reads consecutive {@code float} values in little endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readFloatsLE(MemorySegment segment, long offset, float[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_FLOAT_LE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code float} values in little endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeFloatsLE(MemorySegment segment, long offset, float[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_FLOAT_LE, offset, count);
    }

    /**
     * Reads a {@code float} in big endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it
     */
    public static float readFloatBE(byte[] array, int offset) {
        return (float) ARRAY_AS_FLOAT_BE.get(array, offset);
    }

    /**
     * Writes a {@code float} in big endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it
     */
    public static void writeFloatBE(byte[] array, int offset, float value) {
        ARRAY_AS_FLOAT_BE.set(array, offset, value);
    }

    /**
     * Reads a {@code float} in big endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it before the limit of the buffer
     */
    public static float readFloatBE(ByteBuffer buffer, int index) {
        return (float) BUFFER_AS_FLOAT_BE.get(buffer, index);
    }

    /**
     * Writes a {@code float} in big endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeFloatBE(ByteBuffer buffer, int index, float value) {
        BUFFER_AS_FLOAT_BE.set(buffer, index, value);
    }

    /**
     * Reads a {@code float} in big endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static float readFloatBE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_FLOAT_BE, offset);
    }

    /**
     * Writes a {@code float} in big endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Float#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeFloatBE(MemorySegment segment, long offset, float value) {
        segment.set(SEGMENT_FLOAT_BE, offset, value);
    }

    /**
     * Reads consecutive {@code float} values in big endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readFloatsBE(MemorySegment segment, long offset, float[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_FLOAT_BE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code float} values in big endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeFloatsBE(MemorySegment segment, long offset, float[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_FLOAT_BE, offset, count);
    }

    /**
     * Reads a {@code double} in little endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it
     */
    public static double readDoubleLE(byte[] array, int offset) {
        return (double) ARRAY_AS_DOUBLE_LE.get(array, offset);
    }

    /**
     * Writes a {@code double} in little endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it
     */
    public static void writeDoubleLE(byte[] array, int offset, double value) {
        ARRAY_AS_DOUBLE_LE.set(array, offset, value);
    }

    /**
     * Reads a {@code double} in little endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it before the limit of the buffer
     */
    public static double readDoubleLE(ByteBuffer buffer, int index) {
        return (double) BUFFER_AS_DOUBLE_LE.get(buffer, index);
    }

    /**
     * Writes a {@code double} in little endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeDoubleLE(ByteBuffer buffer, int index, double value) {
        BUFFER_AS_DOUBLE_LE.set(buffer, index, value);
    }

    /**
     * Reads a {@code double} in little endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static double readDoubleLE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_DOUBLE_LE, offset);
    }

    /**
     * Writes a {@code double} in little endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeDoubleLE(MemorySegment segment, long offset, double value) {
        segment.set(SEGMENT_DOUBLE_LE, offset, value);
    }

    /**
     * Reads consecutive {@code double} values in little endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readDoublesLE(MemorySegment segment, long offset, double[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_DOUBLE_LE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code double} values in little endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeDoublesLE(MemorySegment segment, long offset, double[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_DOUBLE_LE, offset, count);
    }

    /**
     * Reads a {@code double} in big endian byte order from the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it
     */
    public static double readDoubleBE(byte[] array, int offset) {
        return (double) ARRAY_AS_DOUBLE_BE.get(array, offset);
    }

    /**
     * Writes a {@code double} in big endian byte order to the given array, starting at
     * the given offset.
     *
     * @param array the array
     * @param offset the index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it
     */
    public static void writeDoubleBE(byte[] array, int offset, double value) {
        ARRAY_AS_DOUBLE_BE.set(array, offset, value);
    }

    /**
     * Reads a {@code double} in big endian byte order from the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it before the limit of the buffer
     */
    public static double readDoubleBE(ByteBuffer buffer, int index) {
        return (double) BUFFER_AS_DOUBLE_BE.get(buffer, index);
    }

    /**
     * Writes a {@code double} in big endian byte order to the given buffer, starting at
     * the given absolute index, whatever the byte order of the buffer.
     *
     * @param buffer the buffer
     * @param index the absolute index of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code index} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it before the limit of the buffer
     * @throws java.nio.ReadOnlyBufferException if the buffer is read only
     */
    public static void writeDoubleBE(ByteBuffer buffer, int index, double value) {
        BUFFER_AS_DOUBLE_BE.set(buffer, index, value);
    }

    /**
     * Reads a {@code double} in big endian byte order from the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @return the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static double readDoubleBE(MemorySegment segment, long offset) {
        return segment.get(SEGMENT_DOUBLE_BE, offset);
    }

    /**
     * Writes a {@code double} in big endian byte order to the given segment, starting at
     * the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the value
     * @param value the value
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if fewer than
     *         {@value Double#BYTES} bytes follow it in the segment
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeDoubleBE(MemorySegment segment, long offset, double value) {
        segment.set(SEGMENT_DOUBLE_BE, offset, value);
    }

    /**
     * Reads consecutive {@code double} values in big endian byte order from the given segment,
     * starting at the given offset, into the given array.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param destination the array the values are read into
     * @param index the index of the array where the first value is stored
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they do
     *         not fit in the array at the given index
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     */
    public static void readDoublesBE(MemorySegment segment, long offset, double[] destination, int index, int count) {
        MemorySegment.copy(segment, SEGMENT_DOUBLE_BE, offset, destination, index, count);
    }

    /**
     * Writes consecutive {@code double} values in big endian byte order from the given array to the
     * given segment, starting at the given offset.
     *
     * @param segment the segment
     * @param offset the offset of the first byte of the first value
     * @param source the array the values are written from
     * @param index the index of the array of the first value
     * @param count the number of values
     * @throws IndexOutOfBoundsException if the values are not within the segment, or if they
     *         are not within the array
     * @throws IllegalStateException if the segment is not alive, or if it is accessed from a
     *         thread that does not own it
     * @throws IllegalArgumentException if the segment is read only
     */
    public static void writeDoublesBE(MemorySegment segment, long offset, double[] source, int index, int count) {
        MemorySegment.copy(source, index, segment, SEGMENT_DOUBLE_BE, offset, count);
    }
}
