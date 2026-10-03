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
 * <p>A date is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 2024-01-31}. The count of days since the epoch can be used instead by listing
 * {@link LocalDateEpochDayMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LocalDateMixin {
    private LocalDateMixin() {
        throw new UnsupportedOperationException("LocalDateMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given date.
     *
     * @param value the date, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(LocalDate value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the date represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the date, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid date
     */
    @TypeDeserializer
    public static LocalDate ofLocalDate(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(LocalDate.class, value, exception);
        }
    }
}
