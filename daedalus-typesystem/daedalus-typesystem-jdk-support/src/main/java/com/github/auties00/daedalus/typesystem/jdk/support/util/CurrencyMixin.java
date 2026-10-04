package com.github.auties00.daedalus.typesystem.jdk.support.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.util.Currency;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Currency}.
 *
 * <p>{@code Currency} is encoded as the {@code String} that contains its ISO 4217 code, such as {@code EUR}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CurrencyMixin {
    private CurrencyMixin() {
        throw new UnsupportedOperationException("CurrencyMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ISO 4217 code of the given currency.
     *
     * @param value the currency, or {@code null}
     * @return the currency code, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Currency value) {
        return value == null ? null : value.getCurrencyCode();
    }

    /**
     * Returns the currency with the given ISO 4217 code.
     *
     * @param value the currency code, or {@code null}
     * @return the currency, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a supported currency code
     */
    @TypeDeserializer
    public static Currency ofCurrency(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Currency.getInstance(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Currency.class, value, exception);
        }
    }
}
