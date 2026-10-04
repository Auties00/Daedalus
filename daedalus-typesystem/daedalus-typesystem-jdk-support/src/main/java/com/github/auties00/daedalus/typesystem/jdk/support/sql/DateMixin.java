package com.github.auties00.daedalus.typesystem.jdk.support.sql;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.sql.Date;
import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Date}.
 *
 * <p>{@code Date} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 2024-01-31}, interpreted in the default time zone.
 *
 * <p>This mixin only requires the {@code java.sql} module statically, so it can only be
 * used by an application that reads that module.
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
     * Returns the ISO 8601 form of the given date.
     *
     * @param value the date, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Date value) {
        return value == null ? null : value.toLocalDate().toString();
    }

    /**
     * Returns the date represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the date, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid date
     */
    @TypeDeserializer
    public static Date ofDate(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Date.class, value, exception);
        }
    }
}
