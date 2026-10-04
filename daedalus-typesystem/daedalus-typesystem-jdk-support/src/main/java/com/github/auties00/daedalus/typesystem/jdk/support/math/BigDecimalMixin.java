package com.github.auties00.daedalus.typesystem.jdk.support.math;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.math.BigDecimal;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link BigDecimal}.
 *
 * <p>{@code BigDecimal} is encoded as the {@code String} that contains its string form, which preserves its exact
 * value and scale.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BigDecimalMixin {
    private BigDecimalMixin() {
        throw new UnsupportedOperationException("BigDecimalMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given decimal, which preserves its scale.
     *
     * @param value the decimal, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(BigDecimal value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the decimal represented by the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the decimal, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid decimal
     */
    @TypeDeserializer
    public static BigDecimal ofBigDecimal(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw TypeDeserializationException.malformedValue(BigDecimal.class, value, exception);
        }
    }
}
