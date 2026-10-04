package com.github.auties00.daedalus.typesystem.jdk.support.math;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.math.BigInteger;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for
 * {@link BigInteger}.
 *
 * <p>An integer is encoded as the {@code String} that contains its decimal form. The
 * two's complement representation can be used instead by listing
 * {@link BigIntegerBytesMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BigIntegerMixin {
    private BigIntegerMixin() {
        throw new UnsupportedOperationException("BigIntegerMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the decimal form of the given integer.
     *
     * @param value the integer, or {@code null}
     * @return the decimal form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(BigInteger value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the integer represented by the given decimal form.
     *
     * @param value the decimal form, or {@code null}
     * @return the integer, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid integer
     */
    @TypeDeserializer
    public static BigInteger ofBigInteger(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new BigInteger(value);
        } catch (NumberFormatException exception) {
            throw TypeDeserializationException.malformedValue(BigInteger.class, value, exception);
        }
    }
}
