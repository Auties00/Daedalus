package com.github.auties00.daedalus.typesystem.jdk.support.security;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.security.PEMDecoder;
import java.security.PEMEncoder;
import java.security.PrivateKey;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link PrivateKey}.
 *
 * <p>A private key is encoded as the {@code String} that contains its PEM form, which
 * identifies the algorithm of the key. The PKCS#8 DER encoding can be used instead by
 * listing {@link PrivateKeyDerMixin} in the mixins of a field.
 *
 * <p>Private keys are encoded without encryption: a field that stores one must be
 * protected by other means.
 *
 * <p>The PEM form uses {@link PEMEncoder} and {@link PEMDecoder}, which are a preview API
 * of the JDK.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PrivateKeyMixin {
    private PrivateKeyMixin() {
        throw new UnsupportedOperationException("PrivateKeyMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the unencrypted PEM form of the given private key.
     *
     * @param value the private key, or {@code null}
     * @return the PEM form, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the key cannot be encoded
     */
    @TypeSerializer
    public static String toText(PrivateKey value) throws TypeSerializationException {
        try {
            return value == null ? null : PEMEncoder.of().encodeToString(value);
        } catch (IllegalArgumentException exception) {
            throw TypeSerializationException.unencodableValue(PrivateKey.class, exception);
        }
    }

    /**
     * Returns the private key with the given unencrypted PEM form.
     *
     * @param value the PEM form, or {@code null}
     * @return the private key, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not the unencrypted PEM form
     *         of a private key
     */
    @TypeDeserializer
    public static PrivateKey ofPrivateKey(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : PEMDecoder.of().decode(value, PrivateKey.class);
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw TypeDeserializationException.malformedValue(PrivateKey.class, value, exception);
        }
    }
}
