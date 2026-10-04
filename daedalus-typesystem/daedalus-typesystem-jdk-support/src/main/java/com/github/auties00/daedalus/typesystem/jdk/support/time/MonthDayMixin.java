package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.MonthDay;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link MonthDay}.
 *
 * <p>{@code MonthDay} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code --01-31}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class MonthDayMixin {
    private MonthDayMixin() {
        throw new UnsupportedOperationException("MonthDayMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given month and day.
     *
     * @param value the month and day, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(MonthDay value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the month and day represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the month and day, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid month and day
     */
    @TypeDeserializer
    public static MonthDay ofMonthDay(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : MonthDay.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(MonthDay.class, value, exception);
        }
    }
}
