package com.github.auties00.daedalus.protobuf.io.reader.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;

final class ProtobufTextLimits {
    private ProtobufTextLimits() {
        throw new UnsupportedOperationException("ProtobufTextLimits is a utility class and cannot be instantiated");
    }

    // Overflow thresholds for unsigned 64-bit decimal parsing.
    // max unsigned long = 18446744073709551615
    static final long ULONG_MAX_DIV_10 = Long.divideUnsigned(-1L, 10);
    static final int ULONG_MAX_MOD_10 = (int) Long.remainderUnsigned(-1L, 10);

    /**
     * Applies a sign to a 32-bit unsigned magnitude and checks that the
     * signed result fits in the range [{@code -2^31}, {@code 2^31 - 1}].
     *
     * @param magnitude the unsigned magnitude (as a long)
     * @param negative  whether the value is negative
     * @return the signed 32-bit result
     * @throws ProtobufDeserializationException on overflow
     */
    static int applySignAndCheckInt32(long magnitude, boolean negative) {
        var maxMagnitude = negative ? (long) Integer.MAX_VALUE + 1 : Integer.MAX_VALUE;
        if (magnitude > maxMagnitude) {
            throw new ProtobufDeserializationException("Value out of range for int32");
        }
        return (int) (negative ? -magnitude : magnitude);
    }

    /**
     * Checks that an unsigned magnitude fits in 32 bits ([{@code 0}, {@code 2^32 - 1}]).
     *
     * @param magnitude the unsigned magnitude (as a long)
     * @return the 32-bit result (unsigned, stored as int)
     * @throws ProtobufDeserializationException on overflow
     */
    static int checkUInt32(long magnitude) {
        if ((magnitude & ~0xFFFFFFFFL) != 0) {
            throw new ProtobufDeserializationException("Value out of range for uint32");
        }
        return (int) magnitude;
    }

    /**
     * Applies a sign to a 64-bit unsigned magnitude and checks that the
     * signed result fits in the range [{@code -2^63}, {@code 2^63 - 1}].
     *
     * @param magnitude the unsigned magnitude
     * @param negative  whether the value is negative
     * @return the signed 64-bit result
     * @throws ProtobufDeserializationException on overflow
     */
    static long applySignAndCheckInt64(long magnitude, boolean negative) {
        if (negative) {
            // magnitude must be <= 2^63 (which is Long.MIN_VALUE as unsigned)
            if (Long.compareUnsigned(magnitude, Long.MIN_VALUE) > 0) {
                throw new ProtobufDeserializationException("Value out of range for int64");
            }
            return -magnitude;
        } else {
            // magnitude must be <= Long.MAX_VALUE (high bit must be 0)
            if (magnitude < 0) {
                throw new ProtobufDeserializationException("Value out of range for int64");
            }
            return magnitude;
        }
    }
}
