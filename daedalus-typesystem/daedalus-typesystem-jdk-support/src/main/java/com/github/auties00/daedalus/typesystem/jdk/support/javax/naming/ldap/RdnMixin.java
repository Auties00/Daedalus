package com.github.auties00.daedalus.typesystem.jdk.support.javax.naming.ldap;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Rdn}.
 *
 * <p>{@code Rdn} is encoded as the {@code String} that contains its RFC 4514 form.
 *
 * <p>This mixin only requires the {@code java.naming} module statically, so it can only
 * be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class RdnMixin {
    private RdnMixin() {
        throw new UnsupportedOperationException("RdnMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the RFC 4514 form of the given relative distinguished name.
     *
     * @param value the relative distinguished name, or {@code null}
     * @return the RFC 4514 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Rdn value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the relative distinguished name with the given RFC 4514 form.
     *
     * @param value the RFC 4514 form, or {@code null}
     * @return the relative distinguished name, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid relative
     *         distinguished name
     */
    @TypeDeserializer
    public static Rdn ofRdn(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new Rdn(value);
        } catch (InvalidNameException | IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Rdn.class, value, exception);
        }
    }
}
