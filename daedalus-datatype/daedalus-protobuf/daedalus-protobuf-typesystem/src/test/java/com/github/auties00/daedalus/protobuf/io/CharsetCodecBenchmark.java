package com.github.auties00.daedalus.protobuf.io;

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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Compares the UTF-8 {@link CharsetEncoder} with {@link String#getBytes(java.nio.charset.Charset)},
 * and the UTF-8 {@link CharsetDecoder} with {@link String#String(byte[], java.nio.charset.Charset)},
 * reusing the encoder, the decoder and their output buffers across invocations.
 *
 * <p>Every strategy is checked to produce the same result as the {@code String} method it is
 * compared with before it is measured.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 4, time = 1)
@Fork(value = 2, jvmArgsAppend = "--enable-preview")
public class CharsetCodecBenchmark {
    /**
     * The number of characters of the string.
     */
    @Param({"64", "1024", "16384"})
    public int length;

    /**
     * The characters the string is made of: ASCII, Latin-1 with accents, CJK, or ASCII mixed
     * with emoji, which are surrogate pairs.
     */
    @Param({"ascii", "latin1", "cjk", "emoji"})
    public String content;

    /**
     * The string to encode.
     */
    private String input;

    /**
     * The UTF-8 encoding of {@link #input}, to decode.
     */
    private byte[] encoded;

    /**
     * The encoder reused by every invocation.
     */
    private CharsetEncoder encoder;

    /**
     * The decoder reused by every invocation.
     */
    private CharsetDecoder decoder;

    /**
     * The output of the encoder, large enough for any input.
     */
    private ByteBuffer encoderOutput;

    /**
     * The output of the decoder, large enough for any input.
     */
    private CharBuffer decoderOutput;

    /**
     * A direct output of the encoder, large enough for any input, which the encoder writes
     * through its loop for buffers that are not backed by an array.
     */
    private ByteBuffer directEncoderOutput;

    /**
     * A direct copy of {@link #encoded}, which the decoder reads through its loop for buffers
     * that are not backed by an array.
     */
    private ByteBuffer directEncoded;

    /**
     * Generates the string and checks every strategy.
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
        encoded = input.getBytes(StandardCharsets.UTF_8);
        encoder = StandardCharsets.UTF_8.newEncoder();
        decoder = StandardCharsets.UTF_8.newDecoder();
        encoderOutput = ByteBuffer.allocate(length * 3);
        decoderOutput = CharBuffer.allocate(length);
        directEncoderOutput = ByteBuffer.allocateDirect(length * 3);
        directEncoded = ByteBuffer.allocateDirect(encoded.length).put(encoded);

        encoderCharArrayToDirect();
        var directBytes = new byte[directEncoderOutput.position()];
        directEncoderOutput.get(0, directBytes);
        if (!Arrays.equals(encoded, directBytes)) {
            throw new AssertionError("encoder writes different bytes to a direct buffer");
        }
        if (!input.equals(decoderFromDirect().flip().toString())) {
            throw new AssertionError("decoder reads a different string from a direct buffer");
        }

        encoder();
        if (!Arrays.equals(encoded, Arrays.copyOf(encoderOutput.array(), encoderOutput.position()))) {
            throw new AssertionError("encoder writes different bytes");
        }
        if (!input.equals(decoderToString())) {
            throw new AssertionError("decoder reads a different string");
        }
    }

    @Benchmark
    public byte[] getBytes() {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public ByteBuffer encoder() {
        encoder.reset();
        encoderOutput.clear();
        encoder.encode(CharBuffer.wrap(input), encoderOutput, true);
        encoder.flush(encoderOutput);
        return encoderOutput;
    }

    @Benchmark
    public ByteBuffer encoderCharArray() {
        encoder.reset();
        encoderOutput.clear();
        encoder.encode(CharBuffer.wrap(input.toCharArray()), encoderOutput, true);
        encoder.flush(encoderOutput);
        return encoderOutput;
    }

    @Benchmark
    public ByteBuffer encoderCharArrayToDirect() {
        encoder.reset();
        directEncoderOutput.clear();
        encoder.encode(CharBuffer.wrap(input.toCharArray()), directEncoderOutput, true);
        encoder.flush(directEncoderOutput);
        return directEncoderOutput;
    }

    @Benchmark
    public CharBuffer decoderFromDirect() {
        decoder.reset();
        decoderOutput.clear();
        directEncoded.clear();
        decoder.decode(directEncoded, decoderOutput, true);
        decoder.flush(decoderOutput);
        return decoderOutput;
    }

    @Benchmark
    public String newString() {
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @Benchmark
    public CharBuffer decoder() {
        decoder.reset();
        decoderOutput.clear();
        decoder.decode(ByteBuffer.wrap(encoded), decoderOutput, true);
        decoder.flush(decoderOutput);
        return decoderOutput;
    }

    @Benchmark
    public String decoderToString() {
        return decoder().flip().toString();
    }
}
