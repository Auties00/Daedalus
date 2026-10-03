package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link LocalTime}.
 *
 * <p>A time is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 10:15:30}.
 *
 * <p>It can also be encoded as a {@code long} count of a {@link TimeUnit} since midnight.
 * As a number alone does not say which unit it counts, this encoding can only be
 * used by a field annotated with {@code @LocalTimeMixinParameters}, which supplies the unit.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LocalTimeMixin {
    private LocalTimeMixin() {
        throw new UnsupportedOperationException("LocalTimeMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given time.
     *
     * @param value the time, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(LocalTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the time represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the time, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid time
     */
    @TypeDeserializer
    public static LocalTime ofLocalTime(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : LocalTime.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(LocalTime.class, value, exception);
        }
    }

    /**
     * Returns the time elapsed from midnight to the given time, as a count of the given
     * unit.
     *
     * @param value the time, or {@code null}
     * @param unit the unit of the count
     * @return the count, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Long toLong(LocalTime value, TimeUnit unit) {
        return value == null ? null : unit.convert(value.toNanoOfDay(), TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the time that follows midnight by the given count of the given unit.
     *
     * @param value the count, or {@code null}
     * @param unit the unit of the count
     * @return the time, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the count is negative or does not fit in a day
     */
    @TypeDeserializer
    public static LocalTime ofLocalTime(Long value, TimeUnit unit) throws TypeDeserializationException {
        try {
            return value == null ? null : LocalTime.ofNanoOfDay(unit.toNanos(value));
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(LocalTime.class, value, exception);
        }
    }
}
