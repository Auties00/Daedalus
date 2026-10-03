package com.github.auties00.daedalus.typesystem.jdk.support.org.ietf.jgss;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Oid}.
 *
 * <p>An object identifier is encoded as the {@code String} that contains its dot
 * separated form, such as {@code 1.2.840.113554.1.2.2}.
 *
 * <p>This mixin is not global, so it must be listed explicitly in the mixins of a field.
 * The ASN.1 DER encoding of an object identifier is already provided globally by
 * {@link OidMixin}, and a global mixin that provided both encodings would let a
 * conversion enter through one encoding and leave through the other, such as a
 * {@code byte[]} field silently converted to a {@code String} through an {@link Oid}.
 *
 * <p>This mixin only requires the {@code java.security.jgss} module statically, so it
 * can only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class OidTextMixin {
    private OidTextMixin() {
        throw new UnsupportedOperationException("OidTextMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the dot separated form of the given object identifier.
     *
     * @param value the object identifier, or {@code null}
     * @return the dot separated form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Oid value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the object identifier with the given dot separated form.
     *
     * @param value the dot separated form, or {@code null}
     * @return the object identifier, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid object identifier
     */
    @TypeDeserializer
    public static Oid ofOid(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new Oid(value);
        } catch (GSSException exception) {
            throw TypeDeserializationException.malformedValue(Oid.class, value, exception);
        }
    }
}
