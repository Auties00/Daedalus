package com.github.auties00.daedalus.typesystem.jdk.support.time;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.time.DateTimeException;
import java.time.Period;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Period}.
 *
 * <p>{@code Period} is encoded as the {@code String} that contains its ISO 8601 form, such as
 * {@code P1Y2M3D}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PeriodMixin {
    private PeriodMixin() {
        throw new UnsupportedOperationException("PeriodMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 8601 form of the given period.
     *
     * @param value the period, or {@code null}
     * @return the ISO 8601 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Period value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the period represented by the given ISO 8601 form.
     *
     * @param value the ISO 8601 form, or {@code null}
     * @return the period, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid period
     */
    @TypeDeserializer
    public static Period ofPeriod(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Period.parse(value);
        } catch (DateTimeException exception) {
            throw TypeDeserializationException.malformedValue(Period.class, value, exception);
        }
    }
}
