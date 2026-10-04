package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Instant}.
 *
 * <p>An instant is encoded as the {@code String} that contains its ISO 8601 form, such
 * as {@code 2024-01-31T10:15:30Z}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} since the epoch.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @InstantMixinParameters}, which supplies the unit.
 *
 * <p>A count that is finer than the precision of the instant truncates it towards
 * negative infinity, while a count that overflows a {@code long} causes a
 * {@link TypeSerializationException}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class InstantMixin {
    private InstantMixin() {
        throw new UnsupportedOperationException("InstantMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given instant.
     *
     * @param value the instant, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Instant value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the instant represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the instant, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid instant
     */
    @TypeDeserializer
    public static Instant ofInstant(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Instant.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Instant.class, value, exception);
        }
    }

    /**
     * Returns the time elapsed from the epoch to the given instant, as a count of the
     * given unit.
     *
     * @param value the instant, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    @TypeSerializer
    public static Long toLong(Instant value, TimeUnit unit) throws TypeSerializationException {
        return value == null ? null : TimeCounts.toCount(value, value.getEpochSecond(), value.getNano(), unit);
    }

    /**
     * Returns the instant that follows the epoch by the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the instant, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the instant is out of the supported range
     */
    @TypeDeserializer
    public static Instant ofInstant(Long value, TimeUnit unit) throws TypeDeserializationException {
        try {
            return value == null ? null : Instant.EPOCH.plus(value, unit.toChronoUnit());
        } catch (DateTimeException | ArithmeticException exception) {
            throw TypeDeserializationException.malformedValue(Instant.class, value, exception);
        }
    }
}
