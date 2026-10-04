package com.github.auties00.daedalus.typesystem.jdk.support.security.spec;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.security.spec.ECGenParameterSpec;
import java.security.spec.NamedParameterSpec;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for the named
 * parameter specifications of the {@code java.security.spec} package:
 * {@link NamedParameterSpec} and {@link ECGenParameterSpec}.
 *
 * <p>A parameter specification is encoded as the {@code String} that contains its name,
 * such as {@code X25519} or {@code secp256r1}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class NamedParameterSpecMixin {
    private NamedParameterSpecMixin() {
        throw new UnsupportedOperationException("NamedParameterSpecMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the name of the given parameter set.
     *
     * @param value the parameter set, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(NamedParameterSpec value) {
        return value == null ? null : value.getName();
    }

    /**
     * Returns the parameter set with the given name.
     *
     * @param value the name, or {@code null}
     * @return the parameter set, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static NamedParameterSpec ofNamedParameterSpec(String value) {
        return value == null ? null : new NamedParameterSpec(value);
    }

    /**
     * Returns the name of the curve of the given elliptic curve parameters.
     *
     * @param value the elliptic curve parameters, or {@code null}
     * @return the name of the curve, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ECGenParameterSpec value) {
        return value == null ? null : value.getName();
    }

    /**
     * Returns the elliptic curve parameters of the curve with the given name.
     *
     * @param value the name of the curve, or {@code null}
     * @return the elliptic curve parameters, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static ECGenParameterSpec ofECGenParameterSpec(String value) {
        return value == null ? null : new ECGenParameterSpec(value);
    }
}
