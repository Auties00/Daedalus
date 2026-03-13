package it.auties.protobuf.io;

import it.auties.protobuf.platform.BMI2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks different approaches I've thought about when implementing reading a var int field.
 * Never inline implementations or the results will be completely skewed because then C2 can apply a bunch of optimizations like loop unrolling that skew the results.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class ProtobufReaderVarintBenchmark {

    private static final VarHandle LONG_LE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private static final long VARINT32_CONT_BITS    = 0x00000080_80808080L;
    private static final long VARINT32_PAYLOAD_BITS  = 0x0000007F_7F7F7F7FL;

    @Param({"allOne", "allTwo", "allFive", "uniform", "realistic"})
    String distribution;

    private static final int COUNT = 4096;

    private byte[] buffer;       // padded — safe for unconditional 8-byte reads
    private byte[] bufferTight;  // exact encoded size — no padding
    private int[] offsets;
    private int end;

    @Setup(Level.Trial)
    public void setup() {
        Random rng = new Random(42);
        int[] values = new int[COUNT];

        switch (distribution) {
            case "allOne" -> {
                for (int i = 0; i < COUNT; i++) values[i] = rng.nextInt(128);
            }
            case "allTwo" -> {
                for (int i = 0; i < COUNT; i++) values[i] = 128 + rng.nextInt(16256);
            }
            case "allFive" -> {
                for (int i = 0; i < COUNT; i++) values[i] = rng.nextInt() | 0x10000000;
            }
            case "uniform" -> {
                int[] maxes = {128, 1 << 14, 1 << 21, 1 << 28, Integer.MIN_VALUE};
                for (int i = 0; i < COUNT; i++) {
                    int width = rng.nextInt(5);
                    values[i] = width < 4
                            ? rng.nextInt(maxes[width])
                            : rng.nextInt() | 0x10000000;
                }
            }
            case "realistic" -> {
                for (int i = 0; i < COUNT; i++) {
                    double r = rng.nextDouble();
                    if (r < 0.50)      values[i] = rng.nextInt(128);
                    else if (r < 0.75) values[i] = 128 + rng.nextInt(16256);
                    else if (r < 0.90) values[i] = (1 << 14) + rng.nextInt((1 << 21) - (1 << 14));
                    else if (r < 0.98) values[i] = (1 << 21) + rng.nextInt((1 << 28) - (1 << 21));
                    else               values[i] = rng.nextInt() | 0x10000000;
                }
            }
            default -> throw new IllegalArgumentException("Unknown distribution: " + distribution);
        }

        byte[] tmp = new byte[COUNT * 5 + 8];
        offsets = new int[COUNT];
        int pos = 0;
        for (int i = 0; i < COUNT; i++) {
            offsets[i] = pos;
            pos = encodeVarint32(tmp, pos, values[i]);
        }
        end = pos;
        buffer = tmp;

        bufferTight = new byte[end];
        System.arraycopy(tmp, 0, bufferTight, 0, end);
    }

    private static int encodeVarint32(byte[] buf, int pos, int value) {
        while ((value & ~0x7F) != 0) {
            buf[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf[pos++] = (byte) value;
        return pos;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static int decodeGoogle(byte[] buffer, int offset) {
        int x;
        if ((x = buffer[offset++]) >= 0) {
            return x;
        } else if ((x ^= (buffer[offset++] << 7)) < 0) {
            return x ^ (~0 << 7);
        } else if ((x ^= (buffer[offset++] << 14)) >= 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14));
        } else if ((x ^= (buffer[offset++] << 21)) < 0) {
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
        } else {
            x ^= buffer[offset] << 28;
            return x ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28));
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static int decodePext(byte[] buffer, int offset, int end) {
        if (BMI2.isSupported() && offset + 8 <= end) {
            var word = (long) LONG_LE.get(buffer, offset);
            var cont = ~word & VARINT32_CONT_BITS;
            var spread = cont ^ (cont - 1);
            var mask = spread & VARINT32_PAYLOAD_BITS;
            return (int) Long.compress(word, mask);
        }
        return decodeGoogle(buffer, offset);
    }

    @Benchmark
    @Fork(value = 2)
    public void google(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            bh.consume(decodeGoogle(buffer, offsets[i]));
        }
    }

    @Benchmark
    @Fork(value = 2, jvmArgsAppend = {
            "--add-modules=jdk.incubator.vector",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:ControlIntrinsic=+_compress_l"
    })
    public void pext(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            bh.consume(decodePext(bufferTight, offsets[i], end));
        }
    }

    @Benchmark
    @Fork(value = 2, jvmArgsAppend = {
            "--add-modules=jdk.incubator.vector",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:ControlIntrinsic=-_compress_l"
    })
    public void pextNoIntrinsic(Blackhole bh) {
        for (int i = 0; i < COUNT; i++) {
            bh.consume(decodePext(buffer, offsets[i], end));
        }
    }
}