package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link LocalDate}.
 *
 * <p>A date is encoded as a {@code long} count of days since the epoch.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The ISO 8601 form of a date is already provided globally by {@link LocalDateMixin},
 * and a global mixin that provided both encodings would let a conversion enter through
 * one encoding and leave through the other, such as a {@code long} field silently
 * converted to a {@code String} through a {@link LocalDate}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class LocalDateEpochDayMixin {
    private LocalDateEpochDayMixin() {
        throw new UnsupportedOperationException("LocalDateEpochDayMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the number of days elapsed from the epoch to the given date.
     *
     * @param value the date, or {@code null}
     * @return the number of days, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Long toLong(LocalDate value) {
        return value == null ? null : value.toEpochDay();
    }

    /**
     * Returns the date that follows the epoch by the given number of days.
     *
     * @param value the number of days, or {@code null}
     * @return the date, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the date is out of the supported range
     */
    @TypeDeserializer
    public static LocalDate ofLocalDate(Long value) throws TypeDeserializationException {
        try {
            return value == null ? null : LocalDate.ofEpochDay(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(LocalDate.class, value, exception);
        }
    }
}
