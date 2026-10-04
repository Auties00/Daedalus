package com.github.auties00.daedalus.typesystem.jdk.support.org.ietf.jgss;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Oid}.
 *
 * <p>An object identifier is encoded as the {@code byte[]} that contains its ASN.1 DER
 * encoding. The dot separated form can be used instead by listing {@link OidTextMixin}
 * in the mixins of a field.
 *
 * <p>This mixin only requires the {@code java.security.jgss} module statically, so it
 * can only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class OidMixin {
    private OidMixin() {
        throw new UnsupportedOperationException("OidMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the ASN.1 DER encoding of the given object identifier.
     *
     * @param value the object identifier, or {@code null}
     * @return the DER encoding, or {@code null} if {@code value} is {@code null}
     * @throws TypeSerializationException if the object identifier cannot be encoded
     */
    @TypeSerializer
    public static byte[] toBytes(Oid value) throws TypeSerializationException {
        try {
            return value == null ? null : value.getDER();
        } catch (GSSException exception) {
            throw TypeSerializationException.unencodableValue(Oid.class, exception);
        }
    }

    /**
     * Returns the object identifier with the given ASN.1 DER encoding.
     *
     * @param value the DER encoding, or {@code null}
     * @return the object identifier, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid encoding
     */
    @TypeDeserializer
    public static Oid ofOid(byte[] value) throws TypeDeserializationException {
        try {
            return value == null ? null : new Oid(value);
        } catch (GSSException exception) {
            throw TypeDeserializationException.malformedValue(Oid.class, value, exception);
        }
    }
}
