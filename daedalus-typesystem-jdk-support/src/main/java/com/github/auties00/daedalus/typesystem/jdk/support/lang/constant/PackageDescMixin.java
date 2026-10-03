package com.github.auties00.daedalus.typesystem.jdk.support.lang.constant;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.lang.constant.PackageDesc;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link PackageDesc}.
 *
 * <p>{@code PackageDesc} is encoded as the {@code String} that contains the name of the package, such as
 * {@code java.lang}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class PackageDescMixin {
    private PackageDescMixin() {
        throw new UnsupportedOperationException("PackageDescMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the name of the given package descriptor.
     *
     * @param value the package descriptor, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(PackageDesc value) {
        return value == null ? null : value.name();
    }

    /**
     * Returns the package descriptor with the given name.
     *
     * @param value the name, or {@code null}
     * @return the package descriptor, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid package name
     */
    @TypeDeserializer
    public static PackageDesc ofPackageDesc(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : PackageDesc.of(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(PackageDesc.class, value, exception);
        }
    }
}
