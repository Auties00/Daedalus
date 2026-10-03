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
 * <p>A principal is encoded as the {@code byte[]} that contains its ASN.1 DER encoding.
 * The RFC 2253 distinguished name can be used instead by listing
 * {@link X500PrincipalTextMixin} in the mixins of a field.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class X500PrincipalMixin {
    private X500PrincipalMixin() {
        throw new UnsupportedOperationException("X500PrincipalMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ASN.1 DER encoding of the given principal.
     *
     * @param value the principal, or {@code null}
     * @return the encoding, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(X500Principal value) {
        return value == null ? null : value.getEncoded();
    }

    /**
     * Returns the principal with the given ASN.1 DER encoding.
     *
     * @param value the encoding, or {@code null}
     * @return the principal, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid encoding
     */
    @TypeDeserializer
    public static X500Principal ofX500Principal(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : new X500Principal(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(X500Principal.class, value, exception);
        }
    }
}
