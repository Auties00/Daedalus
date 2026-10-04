package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.Month;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Month}.
 *
 * <p>A month is encoded as the {@code String} that contains its name, such as
 * {@code JANUARY}. The ISO 8601 value can be used instead by listing
 * {@link MonthValueMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class MonthMixin {
    private MonthMixin() {
        throw new UnsupportedOperationException("MonthMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the month with the given name.
     *
     * <p>The constants are looked up by iterating {@link Month#values()}, as
     * {@link Month#valueOf(String)} relies on reflection.
     *
     * @param value the name, or {@code null}
     * @return the month, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not the name of a month
     */
    @TypeDeserializer
    public static Month ofMonth(String value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        for (var month : Month.values()) {
            if (month.name().equals(value)) {
                return month;
            }
        }

        throw TypeDeserializationException.malformedValue(Month.class, value, null);
    }

    /**
     * Returns the name of the given month, such as {@code JANUARY}.
     *
     * @param value the month, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Month value) {
        return value == null ? null : value.name();
    }
}
