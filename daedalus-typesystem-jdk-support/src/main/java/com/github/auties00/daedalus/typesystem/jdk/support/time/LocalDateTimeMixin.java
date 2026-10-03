package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.LocalDateTime;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link LocalDateTime}.
 *
 * <p>{@code LocalDateTime} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 2024-01-31T10:15:30}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LocalDateTimeMixin {
    private LocalDateTimeMixin() {
        throw new UnsupportedOperationException("LocalDateTimeMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given date and time.
     *
     * @param value the date and time, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the date and time represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the date and time, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid date and time
     */
    @TypeDeserializer
    public static LocalDateTime ofLocalDateTime(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : LocalDateTime.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(LocalDateTime.class, value, exception);
        }
    }
}
