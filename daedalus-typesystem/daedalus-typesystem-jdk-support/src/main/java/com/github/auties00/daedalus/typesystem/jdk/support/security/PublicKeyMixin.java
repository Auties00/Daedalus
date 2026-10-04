package com.github.auties00.daedalus.typesystem.jdk.support.security;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.security.PEMDecoder;
import java.security.PEMEncoder;
import java.security.PublicKey;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link PublicKey}.
 *
 * <p>A public key is encoded as the {@code String} that contains its PEM form, which
 * identifies the algorithm of the key. The X.509 DER encoding can be used instead by
 * listing {@link PublicKeyDerMixin} in the mixins of a field.
 *
 * <p>The PEM form uses {@link PEMEncoder} and {@link PEMDecoder}, which are a preview API
 * of the JDK.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PublicKeyMixin {
    private PublicKeyMixin() {
        throw new UnsupportedOperationException("PublicKeyMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the PEM form of the given public key.
     *
     * @param value the public key, or {@code null}
     * @return the PEM form, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the key cannot be encoded
     */
    @TypeSerializer
    public static String toText(PublicKey value) throws TypeSerializationException {
        try {
            return value == null ? null : PEMEncoder.of().encodeToString(value);
        } catch (IllegalArgumentException exception) {
            throw TypeSerializationException.unencodableValue(PublicKey.class, exception);
        }
    }

    /**
     * Returns the public key with the given PEM form.
     *
     * @param value the PEM form, or {@code null}
     * @return the public key, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not the PEM form of a public key
     */
    @TypeDeserializer
    public static PublicKey ofPublicKey(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : PEMDecoder.of().decode(value, PublicKey.class);
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw TypeDeserializationException.malformedValue(PublicKey.class, value, exception);
        }
    }
}
