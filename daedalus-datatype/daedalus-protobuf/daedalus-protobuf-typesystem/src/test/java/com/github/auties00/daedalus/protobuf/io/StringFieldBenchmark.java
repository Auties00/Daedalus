package com.github.auties00.daedalus.protobuf.io;

import com.github.auties00.daedalus.typesystem.util.CharsetUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Compares the strategies for encoding a {@code string} field in the binary format, measuring
 * the size pass, which computes the size of the field, and the write pass, which writes its
 * tag, its length and its UTF-8 encoding into a reused output array.
 *
 * <p>Every strategy is checked to write the same bytes as {@link #today()} before it is
 * measured.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"--enable-preview", "--add-modules", "jdk.incubator.vector", "--add-opens", "java.base/java.lang=ALL-UNNAMED"})
public class StringFieldBenchmark {
    /**
     * The tag of the field, which is field 1 with the length delimited wire type.
     */
    private static final byte TAG = 0x0A;

    /**
     * The number of characters of the string.
     */
    @Param({"8", "64", "1024"})
    public int length;

    /**
     * The characters the string is made of.
     */
    @Param({"ascii", "latin1", "cjk", "emoji"})
    public String content;

    /**
     * The string of the field.
     */
    private String input;

    /**
     * The output, which is large enough for any strategy and is reused by every invocation.
     */
    private byte[] output;

    /**
     * Generates the string and checks that every strategy writes the same bytes.
     */
    @Setup(Level.Trial)
    public void setup() {
        var random = new SplittableRandom(42);
        var builder = new StringBuilder(length);
        while (builder.length() < length) {
            switch (content) {
                case "ascii" -> builder.append((char) ('a' + random.nextInt(26)));
                case "latin1" -> builder.append(random.nextInt(4) == 0 ? (char) (0xE0 + random.nextInt(32)) : (char) ('a' + random.nextInt(26)));
                case "cjk" -> builder.append((char) (0x4E00 + random.nextInt(0x5000)));
                case "emoji" -> {
                    if (random.nextInt(8) == 0 && builder.length() + 2 <= length) {
                        builder.appendCodePoint(0x1F600 + random.nextInt(64));
                    } else {
                        builder.append((char) ('a' + random.nextInt(26)));
                    }
                }
                default -> throw new IllegalArgumentException(content);
            }
        }
        input = builder.toString();
        output = new byte[length * 4 + 16];

        var expected = written(today());
        check("cachedBytes", expected, written(cachedBytes()));
        check("lengthThenGetBytes", expected, written(lengthThenGetBytes()));
        check("lengthThenDirect", expected, written(lengthThenDirect()));
        check("lengthThenHybrid", expected, written(lengthThenHybrid()));
        for (var malformed : new String[]{"a\uD800b", "\uDC00", "x\uD83D", "😀\uD800𐀀"}) {
            var bytes = malformed.getBytes(StandardCharsets.UTF_8);
            var direct = new byte[bytes.length];
            if (encodeUtf8(malformed, direct, 0) != bytes.length || !Arrays.equals(bytes, direct)) {
                throw new AssertionError("encodeUtf8 malformed " + malformed);
            }
        }
    }

    private byte[] written(int end) {
        return Arrays.copyOf(output, end);
    }

    private static void check(String name, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(name + " writes different bytes");
        }
    }

    /**
     * Encodes the string in the size pass to measure it, and again in the write pass.
     */
    @Benchmark
    public int today() {
        var size = 1 + varIntSize(input.getBytes(StandardCharsets.UTF_8).length) + input.getBytes(StandardCharsets.UTF_8).length;
        var bytes = input.getBytes(StandardCharsets.UTF_8);
        var position = writeHeader(bytes.length);
        System.arraycopy(bytes, 0, output, position, bytes.length);
        return position + bytes.length + (size & 0);
    }

    /**
     * Encodes the string once in the size pass, and writes the same bytes in the write pass.
     */
    @Benchmark
    public int cachedBytes() {
        var bytes = input.getBytes(StandardCharsets.UTF_8);
        var size = 1 + varIntSize(bytes.length) + bytes.length;
        var position = writeHeader(bytes.length);
        System.arraycopy(bytes, 0, output, position, bytes.length);
        return position + bytes.length + (size & 0);
    }

    /**
     * Measures the string without encoding it in the size pass, and encodes it in the write
     * pass.
     */
    @Benchmark
    public int lengthThenGetBytes() {
        var utf8Length = CharsetUtils.utf8Length(input);
        var size = 1 + varIntSize(utf8Length) + utf8Length;
        var bytes = input.getBytes(StandardCharsets.UTF_8);
        var position = writeHeader(utf8Length);
        System.arraycopy(bytes, 0, output, position, bytes.length);
        return position + bytes.length + (size & 0);
    }

    /**
     * Measures the string without encoding it in the size pass, and encodes it directly into
     * the output in the write pass: an ASCII string, whose UTF-8 length is its length, is
     * copied, and any other string is encoded one character at a time.
     */
    @Benchmark
    @SuppressWarnings("deprecation")
    public int lengthThenDirect() {
        var utf8Length = CharsetUtils.utf8Length(input);
        var size = 1 + varIntSize(utf8Length) + utf8Length;
        var position = writeHeader(utf8Length);
        if (utf8Length == input.length()) {
            input.getBytes(0, utf8Length, output, position);
            return position + utf8Length + (size & 0);
        }
        return encodeUtf8(input, output, position) + (size & 0);
    }

    /**
     * Like {@link #lengthThenDirect()}, but encodes a string that is not ASCII with
     * {@code getBytes}.
     */
    @Benchmark
    @SuppressWarnings("deprecation")
    public int lengthThenHybrid() {
        var utf8Length = CharsetUtils.utf8Length(input);
        var size = 1 + varIntSize(utf8Length) + utf8Length;
        var position = writeHeader(utf8Length);
        if (utf8Length == input.length()) {
            input.getBytes(0, utf8Length, output, position);
            return position + utf8Length + (size & 0);
        }
        var bytes = input.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, output, position, bytes.length);
        return position + bytes.length + (size & 0);
    }

    /**
     * Writes the tag and the length of the field at the start of the output.
     *
     * @param length the length of the payload
     * @return the position after the length
     */
    private int writeHeader(int length) {
        output[0] = TAG;
        var position = 1;
        while ((length & ~0x7F) != 0) {
            output[position++] = (byte) ((length & 0x7F) | 0x80);
            length >>>= 7;
        }
        output[position++] = (byte) length;
        return position;
    }

    private static int varIntSize(int value) {
        return (31 - Integer.numberOfLeadingZeros(value | 1)) / 7 + 1;
    }

    /**
     * Encodes the string as UTF-8 into the given array, like {@code getBytes} does.
     *
     * @param value the string
     * @param output the array
     * @param position the index of the first byte to write
     * @return the index after the last byte written
     */
    static int encodeUtf8(String value, byte[] output, int position) {
        var end = value.length();
        for (var i = 0; i < end; i++) {
            var character = value.charAt(i);
            if (character < 0x80) {
                output[position++] = (byte) character;
            } else if (character < 0x800) {
                output[position++] = (byte) (0xC0 | (character >>> 6));
                output[position++] = (byte) (0x80 | (character & 0x3F));
            } else if (!Character.isSurrogate(character)) {
                output[position++] = (byte) (0xE0 | (character >>> 12));
                output[position++] = (byte) (0x80 | ((character >>> 6) & 0x3F));
                output[position++] = (byte) (0x80 | (character & 0x3F));
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(value.charAt(i + 1))) {
                var codePoint = Character.toCodePoint(character, value.charAt(++i));
                output[position++] = (byte) (0xF0 | (codePoint >>> 18));
                output[position++] = (byte) (0x80 | ((codePoint >>> 12) & 0x3F));
                output[position++] = (byte) (0x80 | ((codePoint >>> 6) & 0x3F));
                output[position++] = (byte) (0x80 | (codePoint & 0x3F));
            } else {
                output[position++] = '?';
            }
        }
        return position;
    }
}
