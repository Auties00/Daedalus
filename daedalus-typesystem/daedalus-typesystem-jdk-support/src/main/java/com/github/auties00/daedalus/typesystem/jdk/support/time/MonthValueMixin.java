package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.Month;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Month}.
 *
 * <p>A month is encoded as the {@code int} that contains its ISO 8601 value, from
 * {@code 1} (January) to {@code 12} (December).
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The name of a month is already provided globally by {@link MonthMixin}, and a global
 * mixin that provided both encodings would let a conversion enter through one encoding
 * and leave through the other, such as an {@code int} field silently converted to a
 * {@code String} through a {@link Month}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class MonthValueMixin {
    private MonthValueMixin() {
        throw new UnsupportedOperationException("MonthValueMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 value of the given month, from {@code 1} (January) to
     * {@code 12} (December).
     *
     * @param value the month, or {@code null}
     * @return the value, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(Month value) {
        return value == null ? null : value.getValue();
    }

    /**
     * Returns the month with the given ISO 8601 value, from {@code 1} (January) to
     * {@code 12} (December).
     *
     * @param value the value, or {@code null}
     * @return the month, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not between {@code 1} and {@code 12}
     */
    @TypeDeserializer
    public static Month ofMonth(Integer value) throws TypeDeserializationException {
        try {
            return value == null ? null : Month.of(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Month.class, value, exception);
        }
    }
}
