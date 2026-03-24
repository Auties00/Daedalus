package com.github.auties00.daedalus.typesystem.charset;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Utility class to validate binary data against a charset
 */
public final class CharsetValidator {
    private static final long ASCII_MASK = 0x8080_8080_8080_8080L;
    private static final VarHandle ARRAY_AS_INT64_LE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle BUFFER_AS_INT64_LE =
            MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private static final int BITS       = 6;
    private static final int STATE_MASK = (1 << BITS) - 1;
    private static final int S_ACCEPT   = 0;

    private static final long[] DFA_TABLE = buildDfaTable();

    private static long[] buildDfaTable() {
        var byteClass = new int[]{
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, 9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,
                7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7, 7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                8,8,2,2,2,2,2,2,2,2,2,2,2,2,2,2, 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
                10,3,3,3,3,3,3,3,3,3,3,3,3,4,3,3, 11,6,6,6,5,8,8,8,8,8,8,8,8,8,8,8,
        };
        var transitions = new int[]{
                0,12,24,36,60,96,84,12,12,12,48,72,
                12,12,12,12,12,12,12,12,12,12,12,12,
                12, 0,12,12,12,12,12, 0,12, 0,12,12,
                12,24,12,12,12,12,12,24,12,24,12,12,
                12,12,12,12,12,12,12,24,12,12,12,12,
                12,24,12,12,12,12,12,12,12,24,12,12,
                12,12,12,12,12,12,12,36,12,36,12,12,
                12,36,12,12,12,12,12,36,12,36,12,12,
                12,36,12,12,12,12,12,12,12,12,12,12,
        };
        var table = new long[256];
        for (var b = 0; b < 256; b++) {
            var cls = byteClass[b];
            var entry = 0L;
            for (var s = 0; s < 9; s++) {
                var next = transitions[s * 12 + cls];
                var nextShift = (next / 12) * BITS;
                entry |= ((long) nextShift) << (s * BITS);
            }
            table[b] = entry;
        }
        return table;
    }

    private CharsetValidator() {
        throw new UnsupportedOperationException("CharsetValidator is a utility class and cannot be instantiated");
    }

    /**
     * Returns {@code true} if the entire byte array is valid UTF-8.
     *
     * @param input the bytes to validate
     * @return {@code true} when {@code input} is well-formed UTF-8
     */
    public static boolean isValidUtf8(byte[] input) {
        Objects.requireNonNull(input, "input cannot be null");
        return isValidUtf8(input, 0, input.length);
    }

    /**
     * Returns {@code true} if the entire buffer is valid UTF-8.
     *
     * @param buffer the buffer to validate
     * @return {@code true} when {@code input} is well-formed UTF-8
     */
    public static boolean isValidUtf8(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        if(buffer.hasArray()) {
            return isValidUtf8(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        } else {
            var off = buffer.position();
            var len = buffer.remaining();

            var end = off + len;
            var i   = off;
            var state = S_ACCEPT;

            // SWAR: skip 8-byte blocks that are pure ASCII
            var longEnd = end - 7;
            while (i < longEnd) {
                if (((long) BUFFER_AS_INT64_LE.get(buffer, i) & ASCII_MASK) != 0) break;
                i += 8;
            }

            // Byte-at-a-time DFA with inline SWAR re-checks
            while (i < end) {
                if (state == S_ACCEPT && i < longEnd) {
                    var word = (long) BUFFER_AS_INT64_LE.get(buffer, i);
                    if ((word & ASCII_MASK) == 0) {
                        i += 8;
                        continue;
                    }
                }
                state = (int) ((DFA_TABLE[buffer.get(i) & 0xFF] >>> state) & STATE_MASK);
                i++;
            }
            return state == S_ACCEPT;
        }
    }

    /**
     * Returns {@code true} if the entire segment is valid UTF-8.
     *
     * @param segment the segment to validate
     * @return {@code true} when {@code input} is well-formed UTF-8
     */
    public static boolean isValidUtf8(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment cannot be null");
        var heapBase = segment.heapBase();
        if(heapBase.isPresent() && heapBase.get() instanceof byte[] array) {
            return isValidUtf8(array, 0, array.length);
        } else {
            var off = 0;
            var len = segment.byteSize();

            var end = off + len;
            var i   = off;
            var state = S_ACCEPT;

            // SWAR: skip 8-byte blocks that are pure ASCII
            var longEnd = end - 7;
            while (i < longEnd) {
                if ((segment.get(ValueLayout.JAVA_LONG_UNALIGNED, i) & ASCII_MASK) != 0) break;
                i += 8;
            }

            // Byte-at-a-time DFA with inline SWAR re-checks
            while (i < end) {
                if (state == S_ACCEPT && i < longEnd) {
                    var word = segment.get(ValueLayout.JAVA_LONG_UNALIGNED, i);
                    if ((word & ASCII_MASK) == 0) {
                        i += 8;
                        continue;
                    }
                }
                state = (int) ((DFA_TABLE[segment.get(ValueLayout.JAVA_BYTE, i) & 0xFF] >>> state) & STATE_MASK);
                i++;
            }
            return state == S_ACCEPT;
        }
    }

    /**
     * Returns {@code true} if the sub-range {@code [off, off+len)} of
     * {@code input} is valid UTF-8.
     *
     * <p>Uses a SWAR (word-at-a-time) fast-path for runs of pure-ASCII bytes
     * and falls back to the shift-based DFA for multi-byte sequences.
     *
     * @param input the byte array
     * @param off   start offset (inclusive)
     * @param len   number of bytes to check
     * @return {@code true} when the specified range is well-formed UTF-8
     * @throws ArrayIndexOutOfBoundsException if {@code off} or {@code off+len}
     *         is out of bounds
     */
    // FIXME: this method's performance could be dramatically increased by using SIMD.
    //        unfortunately the implementation actually performs worse than this scalar implementation because of features it needs from Project Valhalla.
    public static boolean isValidUtf8(byte[] input, int off, int len) {
        var end = off + len;
        var i   = off;
        var state = S_ACCEPT;

        // SWAR: skip 8-byte blocks that are pure ASCII
        var longEnd = end - 7;
        while (i < longEnd) {
            if (((long) ARRAY_AS_INT64_LE.get(input, i) & ASCII_MASK) != 0) break;
            i += 8;
        }

        // Byte-at-a-time DFA with inline SWAR re-checks
        while (i < end) {
            if (state == S_ACCEPT && i < longEnd) {
                var word = (long) ARRAY_AS_INT64_LE.get(input, i);
                if ((word & ASCII_MASK) == 0) {
                    i += 8;
                    continue;
                }
            }
            state = (int) ((DFA_TABLE[input[i] & 0xFF] >>> state) & STATE_MASK);
            i++;
        }
        return state == S_ACCEPT;
    }
}