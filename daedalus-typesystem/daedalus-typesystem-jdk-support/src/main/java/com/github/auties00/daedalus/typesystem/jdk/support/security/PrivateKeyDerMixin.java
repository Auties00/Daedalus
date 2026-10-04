package com.github.auties00.daedalus.typesystem.jdk.support.security;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link PrivateKey}.
 *
 * <p>A private key is encoded as the {@code byte[]} that contains its PKCS#8 DER
 * encoding, which cannot be decoded without knowing the algorithm of the key, so it can
 * only be used by a field annotated with {@code @PrivateKeyDerMixinParameters}, which
 * supplies the name of the algorithm, such as {@code RSA} or {@code EC}.
 *
 * <p>Private keys are encoded without encryption: a field that stores one must be
 * protected by other means.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The PEM form of a private key is already provided globally by {@link PrivateKeyMixin},
 * and a global mixin that provided both encodings would let a conversion enter through
 * one encoding and leave through the other, such as a {@code byte[]} field silently
 * converted to a {@code String} through a {@link PrivateKey}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class PrivateKeyDerMixin {
    private PrivateKeyDerMixin() {
        throw new UnsupportedOperationException("PrivateKeyDerMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the unencrypted PKCS#8 DER encoding of the given private key.
     *
     * @param value the private key, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the key does not support encoding
     */
    @TypeSerializer
    public static byte[] toBytes(PrivateKey value) throws TypeSerializationException {
        return value == null ? null : getEncoded(value, PrivateKey.class);
    }

    /**
     * Returns the private key of the given algorithm with the given unencrypted PKCS#8
     * DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @param algorithm the name of the algorithm of the key
     * @return the private key, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the algorithm is not supported, or
     *         {@code value} is not a valid encoding of a key of that algorithm
     */
    @TypeDeserializer
    public static PrivateKey ofPrivateKey(byte[] value, String algorithm) throws TypeDeserializationException {
        try {
            return value == null ? null : KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(value));
        } catch (GeneralSecurityException exception) {
            throw TypeDeserializationException.malformedValue(PrivateKey.class, value, exception);
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
