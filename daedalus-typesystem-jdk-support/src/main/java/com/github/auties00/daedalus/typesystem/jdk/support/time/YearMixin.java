package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.Year;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Year}.
 *
 * <p>A year is encoded as the {@code String} that contains its ISO 8601 form. The
 * {@code int} value of the year can be used instead by listing {@link YearValueMixin}
 * in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class YearMixin {
    private YearMixin() {
        throw new UnsupportedOperationException("YearMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given year.
     *
     * @param value the year, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Year value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the year represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the year, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid year
     */
    @TypeDeserializer
    public static Year ofYear(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Year.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Year.class, value, exception);
        }
    }
}
