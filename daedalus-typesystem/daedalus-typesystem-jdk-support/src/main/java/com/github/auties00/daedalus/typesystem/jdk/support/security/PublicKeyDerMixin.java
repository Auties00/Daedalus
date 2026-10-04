package com.github.auties00.daedalus.typesystem.jdk.support.security;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link PublicKey}.
 *
 * <p>A public key is encoded as the {@code byte[]} that contains its X.509 DER encoding,
 * which cannot be decoded without knowing the algorithm of the key, so it can only be
 * used by a field annotated with {@code @PublicKeyDerMixinParameters}, which supplies the
 * name of the algorithm, such as {@code RSA} or {@code EC}.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The PEM form of a public key is already provided globally by {@link PublicKeyMixin},
 * and a global mixin that provided both encodings would let a conversion enter through
 * one encoding and leave through the other, such as a {@code byte[]} field silently
 * converted to a {@code String} through a {@link PublicKey}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class PublicKeyDerMixin {
    private PublicKeyDerMixin() {
        throw new UnsupportedOperationException("PublicKeyDerMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the X.509 DER encoding of the given public key.
     *
     * @param value the public key, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the key does not support encoding
     */
    @TypeSerializer
    public static byte[] toBytes(PublicKey value) throws TypeSerializationException {
        return value == null ? null : getEncoded(value, PublicKey.class);
    }

    /**
     * Returns the public key of the given algorithm with the given X.509 DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @param algorithm the name of the algorithm of the key
     * @return the public key, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if the algorithm is not supported, or
     *         {@code value} is not a valid encoding of a key of that algorithm
     */
    @TypeDeserializer
    public static PublicKey ofPublicKey(byte[] value, String algorithm) throws TypeDeserializationException {
        try {
            return value == null ? null : KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(value));
        } catch (GeneralSecurityException exception) {
            throw TypeDeserializationException.malformedValue(PublicKey.class, value, exception);
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
