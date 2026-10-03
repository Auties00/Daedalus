package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.util.concurrent.TimeUnit;

/**
 * Static utility methods for expressing an amount of time as a count of a
 * {@link TimeUnit}, shared by the mixins of this package.
 */
final class TimeCounts {
    private TimeCounts() {
        throw new UnsupportedOperationException("TimeCounts is a utility class and cannot be instantiated");
    }

    /**
     * Returns the given amount of time, expressed as whole seconds and a non negative
     * adjustment in nanoseconds, as a count of the given unit, truncating it towards
     * negative infinity.
     *
     * @param value the value that is serialized, used to report an overflow
     * @param seconds the whole seconds of the amount of time
     * @param nanos the adjustment in nanoseconds, between {@code 0} and {@code 999_999_999}
     * @param unit the unit of the count
     * @return the count
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    static long toCount(Object value, long seconds, int nanos, TimeUnit unit) throws TypeSerializationException {
        try {
            return switch (unit) {
                case NANOSECONDS -> Math.addExact(Math.multiplyExact(seconds, 1_000_000_000L), nanos);
                case MICROSECONDS -> Math.addExact(Math.multiplyExact(seconds, 1_000_000L), nanos / 1_000);
                case MILLISECONDS -> Math.addExact(Math.multiplyExact(seconds, 1_000L), nanos / 1_000_000);
                case SECONDS -> seconds;
                case MINUTES -> Math.floorDiv(seconds, 60L);
                case HOURS -> Math.floorDiv(seconds, 3_600L);
                case DAYS -> Math.floorDiv(seconds, 86_400L);
            };
        } catch (ArithmeticException exception) {
            throw TypeSerializationException.overflow(value, unit);
        }
    }
}
