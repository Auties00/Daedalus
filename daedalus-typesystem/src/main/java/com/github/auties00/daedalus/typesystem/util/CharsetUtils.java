package com.github.auties00.daedalus.typesystem.util;

import com.github.auties00.daedalus.typesystem.exception.TypeInitializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Static utility methods for validating and measuring text encoded with a charset.
 *
 * <p>The validation methods check that binary data is well formed UTF-8, skipping runs of
 * ASCII bytes eight at a time, the measuring methods compute the length of the encoding
 * of a string without encoding it, and {@link #adoptUtf8(byte[])} creates a string from its
 * encoding without copying it.
 *
 * <p>This class is thread safe, as it has no mutable state.
 *
 * @implNote calculating the length of a string without encoding it first is not as simple as
 *           one might hope. For this reason, for now, we need to access JDK internals. I have
 *           a proposal in mind to contribute to the JDK a method to fix this, as other projects
 *           are doing similar things because of this missing feature.
 */
public final class CharsetUtils {
    private static final long ASCII_MASK = 0x8080_8080_8080_8080L;

    /**
     * The preferred species of 8 bit lanes, which hold one byte of the internal array of a
     * string each.
     */
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

    /**
     * The number of characters held by a vector of {@link #BYTE_SPECIES} reinterpreted as a
     * vector of characters.
     */
    private static final int CHAR_LANES = BYTE_SPECIES.length() / 2;

    /**
     * The mask of the bits of a vector mask converted to a {@code long} that hold a lane of a
     * vector of characters.
     */
    private static final long CHAR_LANES_MASK = (1L << CHAR_LANES) - 1;

    /**
     * Whether the native byte order is little endian, which is the order of the bytes of the
     * characters in the internal array of a string that holds two bytes per character.
     *
     * <p>A vector of bytes is always reinterpreted as a vector of characters in little endian
     * order, so on a big endian platform the bytes of every character are swapped back.
     */
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    /**
     * The number of surrogates, which are the characters from
     * {@link Character#MIN_SURROGATE} to {@link Character#MAX_SURROGATE}.
     */
    private static final short SURROGATES_RANGE = (short) (Character.MAX_SURROGATE - Character.MIN_SURROGATE + 1);

    /**
     * The number of high surrogates, which are the characters from
     * {@link Character#MIN_HIGH_SURROGATE} to {@link Character#MAX_HIGH_SURROGATE}.
     */
    private static final short HIGH_SURROGATES_RANGE = (short) (Character.MAX_HIGH_SURROGATE - Character.MIN_HIGH_SURROGATE + 1);

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

    private CharsetUtils() {
        throw new UnsupportedOperationException("CharsetUtils is a utility class and cannot be instantiated");
    }

    /**
     * Returns the number of bytes of the UTF-8 encoding of the given string, without encoding
     * it.
     *
     * <p>The result is the length of the array returned by
     * {@code value.getBytes(StandardCharsets.UTF_8)}: a character below {@code 0x80} takes one
     * byte, one below {@code 0x800} two, any other character three, and a valid surrogate pair
     * four. A surrogate that is not part of a valid pair takes one byte, as the JDK replaces it
     * with a question mark.
     *
     * @implNote
     * The internal array of the string is measured where it is, without copying or allocating
     * anything. An array that holds one byte per character adds one byte for every negative
     * byte, which is a character of at least {@code 0x80}. An array that holds two bytes per
     * character is read as vectors of characters: a vector made of ASCII characters is skipped
     * after a single comparison, and any other vector adds the number of characters that are
     * at least {@code 0x80} and {@code 0x800} with two unsigned comparisons. The surrogates of
     * a vector are paired with masks: every low surrogate must follow a high surrogate, either
     * in the previous lane or, for the first lane, at the end of the previous vector, and then
     * every surrogate takes two bytes, so a pair takes four. From the first surrogate that is
     * not part of a valid pair, the rest of the string is measured one character at a time, so
     * that it is replaced exactly like the JDK replaces it.
     *
     * @param value the string
     * @return the length of the UTF-8 encoding of the string
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws ExceptionInInitializerError if this is the first call to this method, and the
     *         {@code java.lang} package is not open to this module and cannot be opened by it
     * @throws NoClassDefFoundError if a previous call to this method threw an
     *         {@code ExceptionInInitializerError}
     */
    public static int utf8Length(String value) {
        var bytes = (byte[]) StringInternals.VALUE.get(value);
        if ((byte) StringInternals.CODER.get(value) == StringInternals.LATIN1) {
            return latin1Utf8Length(bytes);
        }
        return utf16Utf8Length(value, bytes);
    }

    /**
     * Returns the size of the given string written by a text format that escapes the bytes of
     * its UTF-8 encoding, without encoding or escaping it.
     *
     * <p>A character below {@code 0x80}, whose UTF-8 encoding is the character itself, takes
     * the size held by {@code asciiSizes} at its index, and every byte of the UTF-8 encoding of
     * any other character takes {@code nonAsciiByteSize}. A surrogate that is not part of a
     * valid pair takes the size of a question mark, which replaces it, like
     * {@link #utf8Length(String)} counts it. For example, the length of the UTF-8 encoding is
     * the escaped size where every size is {@code 1}.
     *
     * @implNote
     * The internal array of a string that holds one byte per character is read directly, one
     * byte at a time, which avoids the checks of {@link String#charAt(int)}. Any other string is
     * read one character at a time.
     *
     * @param value the string
     * @param asciiSizes the size of every character below {@code 0x80}, indexed by the
     *                   character, which holds at least {@code 128} sizes
     * @param nonAsciiByteSize the size of every byte of the UTF-8 encoding of a character of at
     *                         least {@code 0x80}
     * @return the escaped size of the string
     * @throws NullPointerException if {@code value} or {@code asciiSizes} is {@code null}
     * @throws IllegalArgumentException if {@code asciiSizes} holds less than {@code 128} sizes
     * @throws ExceptionInInitializerError if this is the first call to a method of this class
     *         that reads the internal array of a string, and the {@code java.lang} package is
     *         not open to this module and cannot be opened by it
     * @throws NoClassDefFoundError if a previous call to such a method threw an
     *         {@code ExceptionInInitializerError}
     */
    public static long escapedUtf8Length(String value, byte[] asciiSizes, int nonAsciiByteSize) {
        if (asciiSizes.length < 0x80) {
            throw new IllegalArgumentException("asciiSizes must hold the size of every character below 0x80");
        }

        var bytes = (byte[]) StringInternals.VALUE.get(value);
        if ((byte) StringInternals.CODER.get(value) != StringInternals.LATIN1) {
            return escapedUtf8Length(value, 0, 0, asciiSizes, nonAsciiByteSize);
        }

        var size = 0L;
        var latin1Size = 2L * nonAsciiByteSize;
        for (var entry : bytes) {
            size += entry >= 0 ? asciiSizes[entry] : latin1Size;
        }
        return size;
    }

    /**
     * Creates a string from the given UTF-8 encoding, which takes ownership of the array if
     * every byte of it is ASCII, so that the array is not copied.
     *
     * <p>A string whose characters are ASCII holds them as one byte each, which are the bytes of
     * their UTF-8 encoding, so the string uses the array as its internal array. The caller must
     * not modify the array afterwards, or the string would change. If a byte is not ASCII, the
     * array is decoded like {@link String#String(byte[], java.nio.charset.Charset)} decodes it,
     * and the caller keeps it.
     *
     * @apiNote
     * This method is meant for an array that was created to hold the encoding of a single
     * string, like the output of a writer whose size was computed exactly, and that is
     * discarded afterwards, so that the string is created without copying it.
     *
     * @param bytes the UTF-8 encoding of the string, which is owned by the returned string if
     *              it is ASCII
     * @return the string
     * @throws NullPointerException if {@code bytes} is {@code null}
     * @throws ExceptionInInitializerError if this is the first call to this method, and the
     *         {@code java.lang} package is not open to this module and cannot be opened by it
     * @throws NoClassDefFoundError if a previous call to this method threw an
     *         {@code ExceptionInInitializerError}
     */
    public static String adoptUtf8(byte[] bytes) {
        if (!isAscii(bytes)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        try {
            return (String) StringFactory.NEW_LATIN1.invokeExact(bytes);
        } catch (Throwable throwable) {
            // The factory only allocates the string, so it throws nothing but errors
            throw (Error) throwable;
        }
    }

    /**
     * Returns whether every byte of the given array is ASCII.
     *
     * @param bytes the array
     * @return {@code true} if no byte is negative
     */
    private static boolean isAscii(byte[] bytes) {
        var bound = BYTE_SPECIES.loopBound(bytes.length);
        var i = 0;
        for (; i < bound; i += BYTE_SPECIES.length()) {
            if (ByteVector.fromArray(BYTE_SPECIES, bytes, i).compare(VectorOperators.LT, (byte) 0).anyTrue()) {
                return false;
            }
        }
        for (; i < bytes.length; i++) {
            if (bytes[i] < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the first character of the given range of a string that a text
     * format which escapes the bytes of its UTF-8 encoding does not write as it is, which is a
     * character that is not ASCII, or whose size held by {@code asciiSizes} is not {@code 1}.
     *
     * <p>The characters before the returned index are ASCII characters that are written as
     * they are, so they can be copied at once.
     *
     * @implNote
     * The internal array of a string that holds one byte per character is read directly, one
     * byte at a time, which avoids the checks of {@link String#charAt(int)}. Any other string is
     * read one character at a time.
     *
     * @param value the string
     * @param from the index of the first character of the range
     * @param to the index after the last character of the range
     * @param asciiSizes the size of every character below {@code 0x80}, indexed by the
     *                   character, which holds at least {@code 128} sizes
     * @return the index of the first character that is not written as it is, or {@code to} if
     *         every character of the range is
     * @throws NullPointerException if {@code value} or {@code asciiSizes} is {@code null}
     * @throws IndexOutOfBoundsException if the range is not within {@code value}
     * @throws IllegalArgumentException if {@code asciiSizes} holds less than {@code 128} sizes
     * @throws ExceptionInInitializerError if this is the first call to a method of this class
     *         that reads the internal array of a string, and the {@code java.lang} package is
     *         not open to this module and cannot be opened by it
     * @throws NoClassDefFoundError if a previous call to such a method threw an
     *         {@code ExceptionInInitializerError}
     */
    public static int indexOfEscaped(String value, int from, int to, byte[] asciiSizes) {
        Objects.checkFromToIndex(from, to, value.length());
        if (asciiSizes.length < 0x80) {
            throw new IllegalArgumentException("asciiSizes must hold the size of every character below 0x80");
        }

        if ((byte) StringInternals.CODER.get(value) == StringInternals.LATIN1) {
            var bytes = (byte[]) StringInternals.VALUE.get(value);
            for (var i = from; i < to; i++) {
                var entry = bytes[i];
                if (entry < 0 || asciiSizes[entry] != 1) {
                    return i;
                }
            }
            return to;
        }

        for (var i = from; i < to; i++) {
            var character = value.charAt(i);
            if (character >= 0x80 || asciiSizes[character] != 1) {
                return i;
            }
        }
        return to;
    }

    /**
     * Adds the escaped size of the characters of the given string, from the given index, to the
     * given size, one character at a time, as described by
     * {@link #escapedUtf8Length(String, byte[], int)}.
     *
     * @param value the string
     * @param from the index of the first character to measure
     * @param size the escaped size of the characters before {@code from}
     * @param asciiSizes the size of every character below {@code 0x80}
     * @param nonAsciiByteSize the size of every byte of the UTF-8 encoding of any other
     *                         character
     * @return the escaped size of the string
     */
    private static long escapedUtf8Length(String value, int from, long size, byte[] asciiSizes, int nonAsciiByteSize) {
        var end = value.length();
        for (var i = from; i < end; i++) {
            var character = value.charAt(i);
            if (character < 0x80) {
                size += asciiSizes[character];
            } else if (character < 0x800) {
                size += 2L * nonAsciiByteSize;
            } else if (!Character.isSurrogate(character)) {
                size += 3L * nonAsciiByteSize;
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(value.charAt(i + 1))) {
                size += 4L * nonAsciiByteSize;
                i++;
            } else {
                size += asciiSizes['?'];
            }
        }
        return size;
    }

    /**
     * Returns the number of bytes of the UTF-8 encoding of the characters held by the given
     * internal array of a string, which holds one byte per character.
     *
     * <p>A character takes one byte, and a character of at least {@code 0x80}, whose byte is
     * negative, takes one more.
     *
     * @param bytes the internal array of the string
     * @return the length of the UTF-8 encoding of the string
     */
    private static int latin1Utf8Length(byte[] bytes) {
        var length = bytes.length;
        var bound = BYTE_SPECIES.loopBound(bytes.length);
        var i = 0;
        for (; i < bound; i += BYTE_SPECIES.length()) {
            length += ByteVector.fromArray(BYTE_SPECIES, bytes, i)
                    .compare(VectorOperators.LT, (byte) 0)
                    .trueCount();
        }
        for (; i < bytes.length; i++) {
            length += bytes[i] >>> 31 & 1;
        }
        return length;
    }

    /**
     * Returns the number of bytes of the UTF-8 encoding of the given string, whose internal
     * array holds two bytes per character in the native byte order.
     *
     * @param value the string
     * @param bytes the internal array of the string
     * @return the length of the UTF-8 encoding of the string
     */
    private static int utf16Utf8Length(String value, byte[] bytes) {
        var total = bytes.length >> 1;
        var length = total;
        var pending = 0;
        var i = 0;
        for (var bound = total - total % CHAR_LANES; i < bound; i += CHAR_LANES) {
            var vector = (ShortVector) ByteVector.fromArray(BYTE_SPECIES, bytes, i << 1).reinterpretAsShorts();
            if (!LITTLE_ENDIAN) {
                vector = vector.lanewise(VectorOperators.REVERSE_BYTES);
            }
            var wide = vector.compare(VectorOperators.UGE, (short) 0x80);
            if (pending == 0 && !wide.anyTrue()) {
                continue;
            }
            var extra = wide.trueCount() + vector.compare(VectorOperators.UGE, (short) 0x800).trueCount();
            var offset = vector.sub((short) Character.MIN_SURROGATE);
            var surrogates = offset.compare(VectorOperators.ULT, SURROGATES_RANGE);
            if (surrogates.anyTrue()) {
                var high = offset.compare(VectorOperators.ULT, HIGH_SURROGATES_RANGE).toLong();
                var low = surrogates.toLong() & ~high;
                if (low != (((high << 1) | pending) & CHAR_LANES_MASK)) {
                    return utf8Length(value, i - pending, length - total + i - 2 * pending);
                }
                pending = (int) (high >>> (CHAR_LANES - 1));
                extra -= Long.bitCount(high | low);
            } else if (pending != 0) {
                return utf8Length(value, i - 1, length - total + i - 2);
            }
            length += extra;
        }
        if (i < total || pending != 0) {
            return utf8Length(value, i - pending, length - total + i - 2 * pending);
        }
        return length;
    }

    /**
     * Adds the number of bytes of the UTF-8 encoding of the characters of the given string,
     * from the given index, to the given length, one character at a time.
     *
     * @param value the string
     * @param from the index of the first character to measure
     * @param length the number of bytes of the characters before {@code from}
     * @return the length of the UTF-8 encoding of the string
     */
    private static int utf8Length(String value, int from, int length) {
        var end = value.length();
        for (var i = from; i < end; i++) {
            var character = value.charAt(i);
            if (character < 0x80) {
                length++;
            } else if (character < 0x800) {
                length += 2;
            } else if (!Character.isSurrogate(character)) {
                length += 3;
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(value.charAt(i + 1))) {
                length += 4;
                i++;
            } else {
                length++;
            }
        }
        return length;
    }

    /**
     * The var handles that read the internal array and the coder of a {@link String}, which
     * are looked up when this class is initialized, the first time
     * {@link #utf8Length(String)}, {@link #escapedUtf8Length(String, byte[], int)} or
     * {@link #indexOfEscaped(String, int, int, byte[])} is called, so that the validation
     * methods do not depend on them.
     *
     * <p>The {@code java.lang} package is opened to the module of this class first, as
     * described by {@link ReflectionUtils#openPackages(String, String...)}, so that the private
     * fields of a string can be looked up.
     */
    private static final class StringInternals {
        /**
         * The var handle of the internal array of a string.
         */
        private static final VarHandle VALUE;

        /**
         * The var handle of the coder of a string, which tells how its internal array holds its
         * characters.
         */
        private static final VarHandle CODER;

        /**
         * The coder of a string whose internal array holds one byte per character.
         */
        private static final byte LATIN1;

        static {
            try {
                ReflectionUtils.openPackages("java.base", "java.lang");
                var lookup = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
                VALUE = lookup.findVarHandle(String.class, "value", byte[].class);
                CODER = lookup.findVarHandle(String.class, "coder", byte.class);
                LATIN1 = (byte) lookup.findStaticVarHandle(String.class, "LATIN1", byte.class).get();
            } catch (TypeReflectionException | ReflectiveOperationException | RuntimeException exception) {
                throw TypeInitializationException.stringInternalsInaccessible(exception);
            }
        }

        /**
         * Prevents the instantiation of this holder class.
         *
         * @throws UnsupportedOperationException always
         */
        private StringInternals() {
            throw new UnsupportedOperationException("StringInternals is a holder class and cannot be instantiated");
        }
    }

    /**
     * The method handle of the factory of the {@link String} class that creates a string which
     * uses a given array of Latin1 characters as its internal array, which is looked up the
     * first time {@link #adoptUtf8(byte[])} is called.
     *
     * <p>It is held apart from {@link StringInternals}, so that the methods that only read a
     * string do not depend on it.
     */
    private static final class StringFactory {
        /**
         * The method handle of the factory, which takes the array of Latin1 characters and
         * returns the string.
         */
        private static final MethodHandle NEW_LATIN1;

        static {
            try {
                ReflectionUtils.openPackages("java.base", "java.lang");
                var lookup = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
                NEW_LATIN1 = lookup.findStatic(String.class, "newStringWithLatin1Bytes", MethodType.methodType(String.class, byte[].class));
            } catch (TypeReflectionException | ReflectiveOperationException | RuntimeException exception) {
                throw TypeInitializationException.stringInternalsInaccessible(exception);
            }
        }

        /**
         * Prevents the instantiation of this holder class.
         *
         * @throws UnsupportedOperationException always
         */
        private StringFactory() {
            throw new UnsupportedOperationException("StringFactory is a holder class and cannot be instantiated");
        }
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
                if ((BinaryUtils.readLongLE(buffer, i) & ASCII_MASK) != 0) break;
                i += 8;
            }

            // Byte-at-a-time DFA with inline SWAR re-checks
            while (i < end) {
                if (state == S_ACCEPT && i < longEnd) {
                    var word = BinaryUtils.readLongLE(buffer, i);
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
                if ((BinaryUtils.readLongLE(segment, i) & ASCII_MASK) != 0) break;
                i += 8;
            }

            // Byte-at-a-time DFA with inline SWAR re-checks
            while (i < end) {
                if (state == S_ACCEPT && i < longEnd) {
                    var word = BinaryUtils.readLongLE(segment, i);
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
            if ((BinaryUtils.readLongLE(input, i) & ASCII_MASK) != 0) break;
            i += 8;
        }

        // Byte-at-a-time DFA with inline SWAR re-checks
        while (i < end) {
            if (state == S_ACCEPT && i < longEnd) {
                var word = BinaryUtils.readLongLE(input, i);
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