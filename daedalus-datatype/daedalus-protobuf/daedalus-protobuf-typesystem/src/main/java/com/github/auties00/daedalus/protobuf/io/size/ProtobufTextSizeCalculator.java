package com.github.auties00.daedalus.protobuf.io.size;

import com.github.auties00.daedalus.typesystem.util.CharsetUtils;

/**
 * Static utility methods for computing the size, in bytes, of the UTF-8 encoding of the
 * values written by a {@link com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter},
 * without writing them.
 *
 * <p>The sizes match the output of the writer. They do not include the separators of the line
 * that holds a value, which are added by {@link ProtobufTextSizeAccumulator#addLine(int, long)}.
 * The values that depend on the settings of the writer, or that need a buffer to be measured,
 * are measured by {@link ProtobufTextSizeAccumulator}.
 */
public final class ProtobufTextSizeCalculator {
    /**
     * The size of every byte of a quoted string, escaped like a text writer escapes it, indexed
     * by the unsigned value of the byte: {@code 1} for a printable character, {@code 2} for a
     * character that has a C escape sequence, and {@code 4} for any other byte, which is written
     * as an octal escape sequence.
     */
    private static final byte[] ESCAPED_BYTE_SIZES = createEscapedByteSizes();

    /**
     * The powers of ten that fit in a {@code long}, indexed by their exponent.
     */
    private static final long[] POWERS_OF_TEN = createPowersOfTen();

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufTextSizeCalculator() {
        throw new UnsupportedOperationException("ProtobufTextSizeCalculator is a utility class");
    }

    /**
     * Returns the size of the given name of a field.
     *
     * @param name the name
     * @return the size of the UTF-8 encoding of the name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static int getNameSize(String name) {
        return CharsetUtils.utf8Length(name);
    }

    /**
     * Returns the number of characters of the given signed value, written in decimal.
     *
     * @param value the value
     * @return the number of digits of the value, plus one if it is negative
     */
    public static int getSignedSize(long value) {
        if (value >= 0) {
            return getDigits(value);
        }

        return value == Long.MIN_VALUE ? 20 : getDigits(-value) + 1;
    }

    /**
     * Returns the number of characters of the given unsigned value, written in decimal.
     *
     * @param value the value, interpreted as unsigned
     * @return the number of digits of the value
     */
    public static int getUnsignedSize(long value) {
        if (value >= 0) {
            return getDigits(value);
        }

        // The quotient of the division by ten of an unsigned value that does not fit in a long is positive
        return getDigits((value >>> 1) / 5) + 1;
    }

    /**
     * Returns the number of characters of the given {@code bool}, which is written as
     * {@code true} or {@code false}.
     *
     * @param value the value
     * @return {@code 4} or {@code 5}
     */
    public static int getBoolSize(boolean value) {
        return value ? 4 : 5;
    }

    /**
     * Returns the size of the value of an enum field, which is the name of its constant, or its
     * index if the enum has no constant for it.
     *
     * @param index the index of the value
     * @param constantName the name of the constant whose index is {@code index}, or
     *                     {@code null} if the enum has no such constant
     * @return the size of the value
     */
    public static int getEnumSize(int index, String constantName) {
        return constantName != null ? CharsetUtils.utf8Length(constantName) : getSignedSize(index);
    }

    /**
     * Returns the size of the value of a {@code string} field, without its quotes, escaped like
     * a text writer escapes it.
     *
     * <p>A printable ASCII character takes one byte, and a double quote, a single quote, a
     * backslash or a control character that has a C escape sequence two. Any other ASCII
     * character is written as an octal escape sequence of four characters. A character that is
     * not ASCII takes the bytes of its UTF-8 encoding, or four characters for each of them if
     * it is escaped. A surrogate that is not part of a valid pair is written as a question
     * mark.
     *
     * @param value the value
     * @param escapeNonAscii whether the characters that are not ASCII are escaped
     * @return the size of the escaped value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static long getEscapedStringSize(String value, boolean escapeNonAscii) {
        return CharsetUtils.escapedUtf8Length(value, ESCAPED_BYTE_SIZES, escapeNonAscii ? 4 : 1);
    }

    /**
     * Returns the size of the value of a {@code bytes} field, without its quotes, every byte of
     * which is escaped like a text writer escapes it.
     *
     * @param value the value
     * @return the size of the escaped value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static long getEscapedBytesSize(byte[] value) {
        var size = 0L;
        for (var entry : value) {
            size += ESCAPED_BYTE_SIZES[entry & 0xFF];
        }
        return size;
    }

    /**
     * Returns the size of the given byte of a quoted string, escaped like a text writer
     * escapes it.
     *
     * @param value the unsigned value of the byte
     * @return {@code 1} for a printable character, {@code 2} for a character that has a C
     *         escape sequence, or {@code 4} for any other byte, which is written as an octal
     *         escape sequence
     * @throws ArrayIndexOutOfBoundsException if {@code value} is not the unsigned value of a
     *         byte
     */
    public static int getEscapedByteSize(int value) {
        return ESCAPED_BYTE_SIZES[value];
    }

    /**
     * Returns the number of decimal digits of the given value.
     *
     * @implNote
     * The number of digits is estimated from the number of significant bits of the value, as
     * {@code 1233 / 4096} approximates the decimal logarithm of two. The estimate is either
     * exact or one digit short, which a single comparison with a power of ten corrects. The
     * lowest bit is set so that zero has one digit, which does not change the number of digits
     * of any other value, as no power of ten above one is odd.
     *
     * @param value the value, which is not negative
     * @return the number of digits
     */
    private static int getDigits(long value) {
        var nonZero = value | 1;
        var estimate = ((Long.SIZE - Long.numberOfLeadingZeros(nonZero)) * 1233) >>> 12;
        return nonZero < POWERS_OF_TEN[estimate] ? estimate : estimate + 1;
    }

    /**
     * Creates the table of the sizes of the escaped bytes of a quoted string.
     *
     * @return the table, indexed by the unsigned value of a byte
     */
    private static byte[] createEscapedByteSizes() {
        var sizes = new byte[256];
        for (var value = 0; value < sizes.length; value++) {
            sizes[value] = (byte) switch (value) {
                case 0x07, '\b', '\f', '\n', '\r', '\t', 0x0B, '\\', '\'', '"' -> 2;
                default -> value >= 0x20 && value < 0x7F ? 1 : 4;
            };
        }
        return sizes;
    }

    /**
     * Creates the table of the powers of ten that fit in a {@code long}.
     *
     * @return the table, indexed by the exponent of the power
     */
    private static long[] createPowersOfTen() {
        var powers = new long[19];
        powers[0] = 1;
        for (var exponent = 1; exponent < powers.length; exponent++) {
            powers[exponent] = powers[exponent - 1] * 10;
        }
        return powers;
    }
}
