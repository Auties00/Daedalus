package com.github.auties00.daedalus.typesystem.jdk.support.javax.security.auth.x500;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.security.auth.x500.X500Principal;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for
 * {@link X500Principal}.
 *
 * <p>A principal is encoded as the {@code String} that contains its RFC 2253
 * distinguished name.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The ASN.1 DER encoding of a principal is already provided globally by
 * {@link X500PrincipalMixin}, and a global mixin that provided both encodings would let
 * a conversion enter through one encoding and leave through the other, such as a
 * {@code byte[]} field silently converted to a {@code String} through an
 * {@link X500Principal}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class X500PrincipalTextMixin {
    private X500PrincipalTextMixin() {
        throw new UnsupportedOperationException("X500PrincipalTextMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the RFC 2253 distinguished name of the given principal.
     *
     * @param value the principal, or {@code null}
     * @return the distinguished name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(X500Principal value) {
        return value == null ? null : value.getName();
    }

    /**
     * Returns the principal with the given distinguished name.
     *
     * @param value the distinguished name, or {@code null}
     * @return the principal, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid distinguished name
     */
    @TypeDeserializer
    public static X500Principal ofX500Principal(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new X500Principal(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(X500Principal.class, value, exception);
        }
    }
}
