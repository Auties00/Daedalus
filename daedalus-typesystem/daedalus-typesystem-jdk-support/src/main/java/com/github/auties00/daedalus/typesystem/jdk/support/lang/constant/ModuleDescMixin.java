package com.github.auties00.daedalus.typesystem.jdk.support.lang.constant;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.lang.constant.ModuleDesc;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ModuleDesc}.
 *
 * <p>{@code ModuleDesc} is encoded as the {@code String} that contains the name of the module, such as
 * {@code java.base}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ModuleDescMixin {
    private ModuleDescMixin() {
        throw new UnsupportedOperationException("ModuleDescMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the name of the given module descriptor.
     *
     * @param value the module descriptor, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ModuleDesc value) {
        return value == null ? null : value.name();
    }

    /**
     * Returns the module descriptor with the given name.
     *
     * @param value the name, or {@code null}
     * @return the module descriptor, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid module name
     */
    @TypeDeserializer
    public static ModuleDesc ofModuleDesc(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ModuleDesc.of(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(ModuleDesc.class, value, exception);
        }
    }
}
