package com.github.auties00.daedalus.typesystem.jdk.support.lang.module;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.lang.module.ModuleDescriptor;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ModuleDescriptor.Version}.
 *
 * <p>{@code ModuleDescriptor.Version} is encoded as the {@code String} that contains its string form, such as
 * {@code 1.2-beta}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ModuleVersionMixin {
    private ModuleVersionMixin() {
        throw new UnsupportedOperationException("ModuleVersionMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string form of the given module version.
     *
     * @param value the module version, or {@code null}
     * @return the string form, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ModuleDescriptor.Version value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns the module version represented by the given string form.
     *
     * @param value the string form, or {@code null}
     * @return the module version, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid module version
     */
    @TypeDeserializer
    public static ModuleDescriptor.Version ofModuleVersion(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ModuleDescriptor.Version.parse(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(ModuleDescriptor.Version.class, value, exception);
        }
    }
}
