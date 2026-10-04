package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DayOfWeek;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link DayOfWeek}.
 *
 * <p>A day of the week is encoded as the {@code String} that contains its name, such as
 * {@code MONDAY}. The ISO 8601 value can be used instead by listing
 * {@link DayOfWeekValueMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class DayOfWeekMixin {
    private DayOfWeekMixin() {
        throw new UnsupportedOperationException("DayOfWeekMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the day of the week with the given name.
     *
     * <p>The constants are looked up by iterating {@link DayOfWeek#values()}, as
     * {@link DayOfWeek#valueOf(String)} relies on reflection.
     *
     * @param value the name, or {@code null}
     * @return the day of the week, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not the name of a day of the week
     */
    @TypeDeserializer
    public static DayOfWeek ofDayOfWeek(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        for (var day : DayOfWeek.values()) {
            if (day.name().equals(value)) {
                return day;
            }
        }

        throw TypeDeserializationException.malformedValue(DayOfWeek.class, value, null);
    }

    /**
     * Returns the name of the given day of the week, such as {@code MONDAY}.
     *
     * @param value the day of the week, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(DayOfWeek value) {
        return value == null ? null : value.name();
    }
}
