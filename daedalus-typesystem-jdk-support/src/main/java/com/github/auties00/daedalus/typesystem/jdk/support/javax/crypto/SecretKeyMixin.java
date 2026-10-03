package com.github.auties00.daedalus.typesystem.jdk.support.javax.crypto;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.Key;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link SecretKey}.
 *
 * <p>A secret key is encoded as the {@code byte[]} that contains its raw key material,
 * which does not identify the algorithm of the key, so it can only be decoded by a field
 * annotated with {@code @SecretKeyMixinParameters}, which supplies the name of the
 * algorithm, such as {@code AES}.
 *
 * <p>Secret keys are encoded without encryption: a field that stores one must be
 * protected by other means.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class SecretKeyMixin {
    private SecretKeyMixin() {
        throw new UnsupportedOperationException("SecretKeyMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the raw key material of the given secret key.
     *
     * @param value the secret key, or {@code null}
     * @return the key material, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the key does not expose its key material
     */
    @TypeSerializer
    public static byte[] toBytes(SecretKey value) throws TypeSerializationException {
        return value == null ? null : getEncoded(value, SecretKey.class);
    }

    /**
     * Returns the secret key of the given algorithm with the given raw key material.
     *
     * @param value the key material, or {@code null}
     * @param algorithm the name of the algorithm of the key
     * @return the secret key, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is empty
     */
    @TypeDeserializer
    public static SecretKeySpec ofSecretKey(byte[] value, String algorithm) throws TypeDeserializationException {
        try {
            return value == null ? null : new SecretKeySpec(value, algorithm);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(SecretKey.class, value, exception);
        }
    }

    /**
     * Returns the encoded form of the given key.
     *
     * @param value the key
     * @param type the type of the key, used to report a key that does not support encoding
     * @return the encoded form
     * @throws TypeSerializationException if the key does not support encoding
     */
    private static byte[] getEncoded(Key value, Class<? extends Key> type) throws TypeSerializationException {
        var encoded = value.getEncoded();
        if (encoded == null) {
            throw TypeSerializationException.unencodableValue(type, null);
        }

        return encoded;
    }
}
