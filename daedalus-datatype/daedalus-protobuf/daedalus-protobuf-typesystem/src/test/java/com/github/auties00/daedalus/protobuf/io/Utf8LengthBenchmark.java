package com.github.auties00.daedalus.protobuf.io;

import com.github.auties00.daedalus.typesystem.util.CharsetUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Compares the strategies for computing the length of the UTF-8 encoding of a string, which
 * must be equal to the length of the array returned by {@code String.getBytes(UTF_8)}, as the
 * binary writer encodes strings with it.
 *
 * <p>Each strategy is checked against {@code getBytes} for the generated input, and for
 * malformed strings, before it is measured.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 4, time = 1)
@Fork(value = 1, jvmArgsAppend = {"--enable-preview", "--add-modules", "jdk.incubator.vector", "--add-opens", "java.base/java.lang=ALL-UNNAMED"})
public class Utf8LengthBenchmark {
    /**
     * The preferred species of 16 bit lanes, which hold one {@code char} each.
     */
    private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

    /**
     * The number of characters copied out of the string at once by the chunked strategies.
     */
    private static final int CHUNK = 512;

    /**
     * The number of characters copied out of the string at once by the small chunk strategies.
     */
    private static final int SMALL_CHUNK = 128;

    /**
     * The length below which the vectorized strategies use the scalar loop.
     */
    private static final int SCALAR_THRESHOLD = 32;

    /**
     * The buffer reused by every call on the same thread by {@link #threadLocalBuffer()}.
     */
    private static final ThreadLocal<char[]> BUFFER = ThreadLocal.withInitial(() -> new char[CHUNK]);

    /**
     * The number of characters of the string.
     */
    @Param({"8", "64", "1024", "16384"})
    public int length;

    /**
     * The characters the string is made of: ASCII, ASCII with a single Latin-1 character in
     * the middle, ASCII with a single character above {@code 0xFF} in the middle, which makes
     * the JDK store the string with two bytes per character, Latin-1 with accents, CJK, or
     * ASCII mixed with emoji, which are surrogate pairs.
     */
    @Param({"ascii", "asciiOneLatin1", "asciiOneUtf16", "latin1", "cjk", "emoji"})
    public String content;

    /**
     * The string whose UTF-8 length is computed.
     */
    private String input;

    /**
     * Generates the string and checks every strategy against {@code getBytes}.
     */
    @Setup(Level.Trial)
    public void setup() {
        var random = new SplittableRandom(42);
        var builder = new StringBuilder(length);
        while (builder.length() < length) {
            switch (content) {
                case "ascii", "asciiOneLatin1", "asciiOneUtf16" -> builder.append((char) ('a' + random.nextInt(26)));
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
        switch (content) {
            case "asciiOneLatin1" -> builder.setCharAt(length / 2, 'é');
            case "asciiOneUtf16" -> builder.setCharAt(length / 2, '€');
            default -> {}
        }
        input = builder.toString();

        checkCandidates(input);
        for (var malformed : new String[]{"a\uD800b", "\uDC00", "x\uD83D", "😀\uD800𐀀", "😀"}) {
            for (var repeat : new int[]{1, 7, 40, 300}) {
                checkCandidates("abc".repeat(repeat) + malformed.repeat(repeat));
                checkCandidates(malformed.repeat(repeat));
            }
        }
    }

    /**
     * Checks every strategy against {@code getBytes} for the given string.
     */
    private static void checkCandidates(String value) {
        var expected = value.getBytes(StandardCharsets.UTF_8).length;
        check("scalar", expected, scalar(value));
        check("charsets", expected, CharsetUtils.utf8Length(value));
        check("branchless", expected, branchless(value));
        check("charsSpliterator", expected, charsSpliterator(value));
        check("charsStream", expected, charsStream(value));
        if (String.class.getModule().isOpen("java.lang", Utf8LengthBenchmark.class.getModule())) {
            check("internals", expected, StringInternals.utf8Length(value));
        }
        if (value.length() >= SCALAR_THRESHOLD) {
            check("asciiSkip", expected, vector(value, new char[Math.min(value.length(), CHUNK)], true));
            check("smallChunk", expected, vector(value, new char[Math.min(value.length(), SMALL_CHUNK)], false));
            check("asciiSkipSmallChunk", expected, vector(value, new char[Math.min(value.length(), SMALL_CHUNK)], true));
            check("threadLocalBuffer", expected, vector(value, BUFFER.get(), true));
        }
    }

    private static void check(String name, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(name + ": expected " + expected + " but was " + actual);
        }
    }

    @Benchmark
    public int getBytes() {
        return input.getBytes(StandardCharsets.UTF_8).length;
    }

    @Benchmark
    public int scalar() {
        return scalar(input);
    }

    @Benchmark
    public int charsets() {
        return CharsetUtils.utf8Length(input);
    }

    @Benchmark
    public int asciiSkip() {
        var total = input.length();
        return total < SCALAR_THRESHOLD ? scalar(input) : vector(input, new char[Math.min(total, CHUNK)], true);
    }

    @Benchmark
    public int smallChunk() {
        var total = input.length();
        return total < SCALAR_THRESHOLD ? scalar(input) : vector(input, new char[Math.min(total, SMALL_CHUNK)], false);
    }

    @Benchmark
    public int asciiSkipSmallChunk() {
        var total = input.length();
        return total < SCALAR_THRESHOLD ? scalar(input) : vector(input, new char[Math.min(total, SMALL_CHUNK)], true);
    }

    @Benchmark
    public int threadLocalBuffer() {
        return input.length() < SCALAR_THRESHOLD ? scalar(input) : vector(input, BUFFER.get(), true);
    }

    @Benchmark
    public int branchless() {
        return branchless(input);
    }

    @Benchmark
    public int internals() {
        return StringInternals.utf8Length(input);
    }

    /**
     * Reads the internal array and coder of a string through var handles, which requires
     * {@code java.lang} to be opened to this class with {@code --add-opens}.
     */
    static final class StringInternals {
        /**
         * The coder of a string whose internal array holds one byte per character.
         */
        private static final byte LATIN1 = 0;

        /**
         * The preferred species of 8 bit lanes, which hold one Latin-1 character each.
         */
        private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

        /**
         * Whether the platform is little endian, which is the byte order of the lanes of a
         * reinterpreted vector, so that the bytes of an internal array that holds two bytes per
         * character can be reinterpreted as characters.
         */
        private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

        /**
         * The var handle of the internal array of a string.
         */
        private static final VarHandle VALUE;

        /**
         * The var handle of the coder of a string.
         */
        private static final VarHandle CODER;

        static {
            try {
                var lookup = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
                VALUE = lookup.findVarHandle(String.class, "value", byte[].class);
                CODER = lookup.findVarHandle(String.class, "coder", byte.class);
            } catch (ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        /**
         * Returns the length of the UTF-8 encoding of the given string, reading its internal
         * array without copying it.
         */
        static int utf8Length(String input) {
            var value = (byte[]) VALUE.get(input);
            if ((byte) CODER.get(input) == LATIN1) {
                return latin1Utf8Length(value);
            }
            if (!LITTLE_ENDIAN) {
                return scalar(input);
            }
            return utf16Utf8Length(input, value);
        }

        /**
         * Counts one byte for every Latin-1 character, and one more for every character of at
         * least {@code 0x80}, whose byte is negative.
         */
        private static int latin1Utf8Length(byte[] value) {
            var length = value.length;
            var bound = BYTE_SPECIES.loopBound(length);
            var i = 0;
            for (; i < bound; i += BYTE_SPECIES.length()) {
                length += ByteVector.fromArray(BYTE_SPECIES, value, i)
                        .compare(VectorOperators.LT, (byte) 0)
                        .trueCount();
            }
            for (; i < value.length; i++) {
                length += value[i] >>> 31 & 1;
            }
            return length;
        }

        /**
         * Counts the characters of an internal array that holds two bytes per character, in
         * little endian order, like {@link #vector(String, char[], boolean)} does with its
         * buffer.
         */
        private static int utf16Utf8Length(String input, byte[] value) {
            var total = value.length >> 1;
            var lanes = SPECIES.length();
            var laneMask = (1L << lanes) - 1;
            var length = total;
            var pending = 0;
            var bound = SPECIES.loopBound(total);
            var i = 0;
            for (; i < bound; i += lanes) {
                var vector = (ShortVector) ByteVector.fromArray(BYTE_SPECIES, value, i << 1).reinterpretAsShorts();
                var wide = vector.compare(VectorOperators.UGE, (short) 0x80);
                if (pending == 0 && !wide.anyTrue()) {
                    continue;
                }
                var extra = wide.trueCount() + vector.compare(VectorOperators.UGE, (short) 0x800).trueCount();
                var offset = vector.sub((short) 0xD800);
                var surrogates = offset.compare(VectorOperators.ULT, (short) 0x800);
                if (surrogates.anyTrue()) {
                    var high = offset.compare(VectorOperators.ULT, (short) 0x400).toLong();
                    var low = surrogates.toLong() & ~high;
                    if (low != (((high << 1) | pending) & laneMask)) {
                        return scalar(input, i - pending, length - total + i - 2 * pending);
                    }
                    pending = (int) (high >>> (lanes - 1));
                    extra -= Long.bitCount(high | low);
                } else if (pending != 0) {
                    return scalar(input, i - 1, length - total + i - 2);
                }
                length += extra;
            }
            if (i < total || pending != 0) {
                return scalar(input, i - pending, length - total + i - 2 * pending);
            }
            return length;
        }
    }

    @Benchmark
    public int charsSpliterator() {
        return charsSpliterator(input);
    }

    @Benchmark
    public int charsStream() {
        return charsStream(input);
    }

    /**
     * Counts the characters of at least two and three bytes, and the surrogates, of the
     * characters it accepts.
     */
    static final class Counter implements IntConsumer {
        int extra;
        int surrogates;

        @Override
        public void accept(int character) {
            extra += ((0x7F - character) >>> 31) + ((0x7FF - character) >>> 31);
            surrogates += (((character & 0xF800) ^ 0xD800) - 1) >>> 31;
        }
    }

    /**
     * Like {@link #branchless(String)}, but reads the characters through the spliterator of
     * {@link String#chars()}, which iterates the internal array of the string directly.
     */
    static int charsSpliterator(String input) {
        var counter = new Counter();
        input.chars().spliterator().forEachRemaining(counter);
        if (counter.surrogates != 0) {
            return scalar(input);
        }
        return input.length() + counter.extra;
    }

    /**
     * Like {@link #branchless(String)}, but with two stream pipelines over
     * {@link String#chars()}: one that sums the extra bytes, and one that looks for a
     * surrogate.
     */
    static int charsStream(String input) {
        if (input.chars().anyMatch(character -> (character & 0xF800) == 0xD800)) {
            return scalar(input);
        }
        return input.length() + input.chars().map(character -> ((0x7F - character) >>> 31) + ((0x7FF - character) >>> 31)).sum();
    }

    /**
     * Counts the bytes of each character, pairing surrogates like {@code getBytes} does: a
     * valid pair takes four bytes, and an unpaired surrogate is replaced by a question mark.
     */
    static int scalar(String input) {
        return scalar(input, 0, 0);
    }

    /**
     * Counts the bytes of the characters of the given string from the given index, adding them
     * to the given length.
     */
    static int scalar(String input, int from, int length) {
        var end = input.length();
        for (var i = from; i < end; i++) {
            var character = input.charAt(i);
            if (character < 0x80) {
                length++;
            } else if (character < 0x800) {
                length += 2;
            } else if (!Character.isSurrogate(character)) {
                length += 3;
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(input.charAt(i + 1))) {
                length += 4;
                i++;
            } else {
                length++;
            }
        }
        return length;
    }

    /**
     * Copies the characters into the given buffer one chunk at a time, and counts the
     * characters of at least two and three bytes with unsigned vector comparisons, pairing
     * surrogates with masks like {@code CharsetUtils.utf8Length} does.
     *
     * <p>When {@code asciiSkip} is {@code true}, a vector without characters of at least
     * {@code 0x80} is skipped after a single comparison.
     */
    @CompilerControl(CompilerControl.Mode.INLINE)
    static int vector(String input, char[] buffer, boolean asciiSkip) {
        var total = input.length();
        var lanes = SPECIES.length();
        var laneMask = (1L << lanes) - 1;
        var chunk = buffer.length;
        var length = total;
        var pending = 0;
        for (var start = 0; start < total; start += chunk) {
            var count = Math.min(chunk, total - start);
            input.getChars(start, start + count, buffer, 0);
            var bound = SPECIES.loopBound(count);
            var i = 0;
            for (; i < bound; i += lanes) {
                var vector = ShortVector.fromCharArray(SPECIES, buffer, i);
                var wide = vector.compare(VectorOperators.UGE, (short) 0x80);
                if (asciiSkip && pending == 0 && !wide.anyTrue()) {
                    continue;
                }
                var extra = wide.trueCount() + vector.compare(VectorOperators.UGE, (short) 0x800).trueCount();
                var offset = vector.sub((short) 0xD800);
                var surrogates = offset.compare(VectorOperators.ULT, (short) 0x800);
                if (surrogates.anyTrue()) {
                    var high = offset.compare(VectorOperators.ULT, (short) 0x400).toLong();
                    var low = surrogates.toLong() & ~high;
                    if (low != (((high << 1) | pending) & laneMask)) {
                        return scalar(input, start + i - pending, length - total + start + i - 2 * pending);
                    }
                    pending = (int) (high >>> (lanes - 1));
                    extra -= Long.bitCount(high | low);
                } else if (pending != 0) {
                    return scalar(input, start + i - 1, length - total + start + i - 2);
                }
                length += extra;
            }
            for (; i < count; i++) {
                var character = buffer[i];
                if (pending != 0 || Character.isSurrogate(character)) {
                    return scalar(input, start + i - pending, length - total + start + i - 2 * pending);
                }
                length += (character >= 0x80 ? 1 : 0) + (character >= 0x800 ? 1 : 0);
            }
        }
        if (pending != 0) {
            return scalar(input, total - 1, length - 2);
        }
        return length;
    }

    /**
     * Counts the characters of at least two and three bytes, and the surrogates, with
     * arithmetic only, so that the loop has no branch and reads the string through
     * {@code charAt} without copying it. A string with surrogates is measured again by the
     * scalar loop, which pairs them.
     */
    static int branchless(String input) {
        var total = input.length();
        var extra = 0;
        var surrogates = 0;
        for (var i = 0; i < total; i++) {
            int character = input.charAt(i);
            extra += ((0x7F - character) >>> 31) + ((0x7FF - character) >>> 31);
            surrogates += (((character & 0xF800) ^ 0xD800) - 1) >>> 31;
        }
        if (surrogates != 0) {
            return scalar(input);
        }
        return total + extra;
    }
}
