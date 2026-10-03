package com.github.auties00.daedalus.typesystem.jdk.support.javax.naming.ldap;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link LdapName}.
 *
 * <p>{@code LdapName} is encoded as the {@code String} that contains its RFC 4514 form.
 *
 * <p>This mixin only requires the {@code java.naming} module statically, so it can only
 * be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LdapNameMixin {
    private LdapNameMixin() {
        throw new UnsupportedOperationException("LdapNameMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the RFC 4514 form of the given LDAP name.
     *
     * @param value the LDAP name, or {@code null}
     * @return the RFC 4514 form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(LdapName value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the LDAP name with the given RFC 4514 form.
     *
     * @param value the RFC 4514 form, or {@code null}
     * @return the LDAP name, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid LDAP name
     */
    @TypeDeserializer
    public static LdapName ofLdapName(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new LdapName(value);
        } catch (InvalidNameException | IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(LdapName.class, value, exception);
        }
    }
}
