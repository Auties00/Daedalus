package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Duration}.
 *
 * <p>A duration is encoded as the {@code String} that contains its ISO 8601 form, such
 * as {@code PT8H6M12.345S}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} that measures its length.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @DurationMixinParameters}, which supplies the unit.
 *
 * <p>A count that is finer than the precision of the duration truncates it towards
 * negative infinity, while a count that overflows a {@code long} causes a
 * {@link TypeSerializationException}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class DurationMixin {
    private DurationMixin() {
        throw new UnsupportedOperationException("DurationMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given duration.
     *
     * @param value the duration, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Duration value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the duration represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the duration, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid duration
     */
    @TypeDeserializer
    public static Duration ofDuration(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Duration.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Duration.class, value, exception);
        }
    }

    /**
     * Returns the length of the given duration, as a count of the given unit.
     *
     * @param value the duration, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the count overflows a {@code long}
     */
    @TypeSerializer
    public static Long toLong(Duration value, TimeUnit unit) throws TypeSerializationException {
        return value == null ? null : TimeCounts.toCount(value, value.getSeconds(), value.getNano(), unit);
    }

    /**
     * Returns the duration whose length is the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the duration, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the duration is out of the supported range
     */
    @TypeDeserializer
    public static Duration ofDuration(Long value, TimeUnit unit) throws TypeDeserializationException {
        try {
            return value == null ? null : Duration.of(value, unit.toChronoUnit());
        } catch (DateTimeException | ArithmeticException exception) {
            throw TypeDeserializationException.malformedValue(Duration.class, value, exception);
        }
    }
}
