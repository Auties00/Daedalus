package com.github.auties00.daedalus.typesystem.jdk.support.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;
import com.github.auties00.daedalus.typesystem.jdk.support.time.InstantMixin;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for the legacy
 * {@link Date}.
 *
 * <p>A date represents an instant, and is encoded as the {@code String} that contains
 * the ISO 8601 form of that instant, such as {@code 2024-01-31T10:15:30Z}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} since the epoch.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @DateMixinParameters}, which supplies the unit.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class DateMixin {
    private DateMixin() {
        throw new UnsupportedOperationException("DateMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the instant represented by the given legacy date.
     *
     * @param value the date, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Date value) {
        return value == null ? null : Instant.ofEpochMilli(value.getTime()).toString();
    }

    /**
     * Returns the legacy date that represents the instant with the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the date, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid instant, or is
     *         out of the range of a date
     */
    @TypeDeserializer
    public static Date ofDate(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Date.from(Instant.parse(value));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Date.class, value, exception);
        }
    }

    /**
     * Returns the time elapsed from the epoch to the given date, as a count of the given
     * unit.
     *
     * @param value the date, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    @TypeSerializer
    public static Long toLong(Date value, TimeUnit unit) throws TypeSerializationException {
        return value == null ? null : InstantMixin.toLong(Instant.ofEpochMilli(value.getTime()), unit);
    }

    /**
     * Returns the legacy date that follows the epoch by the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the date, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the date is out of the supported range
     */
    @TypeDeserializer
    public static Date ofDate(Long value, TimeUnit unit) throws TypeDeserializationException {
        try {
            return value == null ? null : Date.from(Instant.EPOCH.plus(value, unit.toChronoUnit()));
        } catch (DateTimeException | ArithmeticException | IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Date.class, value, exception);
        }
    }
}
