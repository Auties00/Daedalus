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
 * <p>A year is encoded as the {@code int} that contains its value.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The ISO 8601 form of a year is already provided globally by {@link YearMixin}, and a
 * global mixin that provided both encodings would let a conversion enter through one
 * encoding and leave through the other, such as an {@code int} field silently converted
 * to a {@code String} through a {@link Year}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class YearValueMixin {
    private YearValueMixin() {
        throw new UnsupportedOperationException("YearValueMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the value of the given year.
     *
     * @param value the year, or {@code null}
     * @return the value of the year, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(Year value) {
        return value == null ? null : value.getValue();
    }

    /**
     * Returns the year with the given value.
     *
     * @param value the value of the year, or {@code null}
     * @return the year, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is out of the supported range
     */
    @TypeDeserializer
    public static Year ofYear(Integer value) throws TypeDeserializationException {
        try {
            return value == null ? null : Year.of(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Year.class, value, exception);
        }
    }
}
