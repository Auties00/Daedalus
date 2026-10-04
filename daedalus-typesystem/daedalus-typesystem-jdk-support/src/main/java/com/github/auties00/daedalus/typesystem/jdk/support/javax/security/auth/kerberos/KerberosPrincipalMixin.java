package com.github.auties00.daedalus.typesystem.jdk.support.javax.security.auth.kerberos;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.security.auth.kerberos.KerberosPrincipal;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link KerberosPrincipal}.
 *
 * <p>{@code KerberosPrincipal} is encoded as the {@code String} that contains its name, including its realm.
 *
 * <p>This mixin only requires the {@code java.security.jgss} module statically, so it
 * can only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class KerberosPrincipalMixin {
    private KerberosPrincipalMixin() {
        throw new UnsupportedOperationException("KerberosPrincipalMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the name of the given Kerberos principal, including its realm.
     *
     * @param value the Kerberos principal, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(KerberosPrincipal value) {
        return value == null ? null : value.getName();
    }

    /**
     * Returns the Kerberos principal with the given name.
     *
     * <p>A name without a realm is completed with the default realm of the Kerberos
     * configuration, if one is available.
     *
     * @param value the name, or {@code null}
     * @return the Kerberos principal, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid principal name,
     *         or has no realm and no default realm is configured
     */
    @TypeDeserializer
    public static KerberosPrincipal ofKerberosPrincipal(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : new KerberosPrincipal(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(KerberosPrincipal.class, value, exception);
        }
    }
}
