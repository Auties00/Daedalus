package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.YearMonth;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link YearMonth}.
 *
 * <p>{@code YearMonth} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code 2024-01}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class YearMonthMixin {
    private YearMonthMixin() {
        throw new UnsupportedOperationException("YearMonthMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given year and month.
     *
     * @param value the year and month, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(YearMonth value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the year and month represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the year and month, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid year and month
     */
    @TypeDeserializer
    public static YearMonth ofYearMonth(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : YearMonth.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(YearMonth.class, value, exception);
        }
    }
}
