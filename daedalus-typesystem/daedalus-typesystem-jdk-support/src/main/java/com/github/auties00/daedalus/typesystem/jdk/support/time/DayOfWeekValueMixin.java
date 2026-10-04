package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.DayOfWeek;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link DayOfWeek}.
 *
 * <p>A day of the week is encoded as the {@code int} that contains its ISO 8601 value,
 * from {@code 1} (Monday) to {@code 7} (Sunday).
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The name of a day of the week is already provided globally by {@link DayOfWeekMixin},
 * and a global mixin that provided both encodings would let a conversion enter through
 * one encoding and leave through the other, such as an {@code int} field silently
 * converted to a {@code String} through a {@link DayOfWeek}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class DayOfWeekValueMixin {
    private DayOfWeekValueMixin() {
        throw new UnsupportedOperationException("DayOfWeekValueMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 value of the given day of the week, from {@code 1} (Monday)
     * to {@code 7} (Sunday).
     *
     * @param value the day of the week, or {@code null}
     * @return the value, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(DayOfWeek value) {
        return value == null ? null : value.getValue();
    }

    /**
     * Returns the day of the week with the given ISO 8601 value, from {@code 1} (Monday)
     * to {@code 7} (Sunday).
     *
     * @param value the value, or {@code null}
     * @return the day of the week, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not between {@code 1} and {@code 7}
     */
    @TypeDeserializer
    public static DayOfWeek ofDayOfWeek(Integer value) throws TypeDeserializationException {
        try {
            return value == null ? null : DayOfWeek.of(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(DayOfWeek.class, value, exception);
        }
    }
}
