package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.ZonedDateTime;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ZonedDateTime}.
 *
 * <p>{@code ZonedDateTime} is encoded as the {@code String} that contains its ISO 8601 form extended with
 * the identifier of its time zone, such as
 * {@code 2024-01-31T10:15:30+01:00[Europe/Paris]}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ZonedDateTimeMixin {
    private ZonedDateTimeMixin() {
        throw new UnsupportedOperationException("ZonedDateTimeMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given date and time with a time zone, extended
     * with the identifier of the time zone.
     *
     * @param value the date and time with a time zone, or {@code null}
     * @return the extended ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ZonedDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the date and time with a time zone represented by the given extended
     * ISO 8601 form.
     *
     * @param value the extended ISO 8601 form, or {@code null}
     * @return the date and time with a time zone, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid date and time
     *         with a time zone
     */
    @TypeDeserializer
    public static ZonedDateTime ofZonedDateTime(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ZonedDateTime.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(ZonedDateTime.class, value, exception);
        }
    }
}
