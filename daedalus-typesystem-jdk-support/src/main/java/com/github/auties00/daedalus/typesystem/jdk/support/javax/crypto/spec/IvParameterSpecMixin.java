package com.github.auties00.daedalus.typesystem.jdk.support.javax.crypto.spec;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import javax.crypto.spec.IvParameterSpec;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for
 * {@link IvParameterSpec}.
 *
 * <p>An initialization vector is encoded as the {@code byte[]} that contains it.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class IvParameterSpecMixin {
    private IvParameterSpecMixin() {
        throw new UnsupportedOperationException("IvParameterSpecMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the initialization vector contained by the given parameter specification.
     *
     * @param value the parameter specification, or {@code null}
     * @return the initialization vector, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(IvParameterSpec value) {
        return value == null ? null : value.getIV();
    }

    /**
     * Returns the parameter specification that contains the given initialization vector.
     *
     * @param value the initialization vector, or {@code null}
     * @return the parameter specification, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static IvParameterSpec ofIvParameterSpec(byte[] value) {
        return value == null ? null : new IvParameterSpec(value);
    }
}
