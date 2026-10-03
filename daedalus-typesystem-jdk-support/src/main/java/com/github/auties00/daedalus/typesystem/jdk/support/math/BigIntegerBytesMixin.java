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
 * <p>An integer is encoded as the {@code byte[]} that contains its two's complement
 * representation, in big endian order.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The decimal form of an integer is already provided globally by {@link BigIntegerMixin},
 * and a global mixin that provided both encodings would let a conversion enter through
 * one encoding and leave through the other, such as a {@code byte[]} field silently
 * converted to a {@code String} through a {@link BigInteger}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class BigIntegerBytesMixin {
    private BigIntegerBytesMixin() {
        throw new UnsupportedOperationException("BigIntegerBytesMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the two's complement representation of the given integer, in big endian
     * order.
     *
     * @param value the integer, or {@code null}
     * @return the two's complement representation, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(BigInteger value) {
        return value == null ? null : value.toByteArray();
    }

    /**
     * Returns the integer with the given two's complement representation, in big endian
     * order.
     *
     * @param value the two's complement representation, or {@code null}
     * @return the integer, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is empty
     */
    @TypeDeserializer
    public static BigInteger ofBigInteger(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : new BigInteger(value);
        } catch (NumberFormatException exception) {
            throw TypeDeserializationException.malformedValue(BigInteger.class, value, exception);
        }
    }
}
