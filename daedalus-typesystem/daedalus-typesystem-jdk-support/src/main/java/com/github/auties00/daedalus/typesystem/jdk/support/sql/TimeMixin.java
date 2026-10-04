package com.github.auties00.daedalus.typesystem.jdk.support.sql;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.sql.Time;
import java.time.DateTimeException;
import java.time.LocalTime;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Time}.
 *
 * <p>{@code Time} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 10:15:30}, interpreted in the default time zone.
 *
 * <p>This mixin only requires the {@code java.sql} module statically, so it can only be
 * used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class TimeMixin {
    private TimeMixin() {
        throw new UnsupportedOperationException("TimeMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given time.
     *
     * @param value the time, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Time value) {
        return value == null ? null : value.toLocalTime().toString();
    }

    /**
     * Returns the time represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the time, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid time
     */
    @TypeDeserializer
    public static Time ofTime(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Time.valueOf(LocalTime.parse(value));
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Time.class, value, exception);
        }
    }
}
