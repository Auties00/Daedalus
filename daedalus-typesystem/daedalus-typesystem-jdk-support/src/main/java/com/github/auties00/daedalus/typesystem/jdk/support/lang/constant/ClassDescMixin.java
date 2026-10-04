package com.github.auties00.daedalus.typesystem.jdk.support.lang.constant;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.lang.constant.ClassDesc;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ClassDesc}.
 *
 * <p>{@code ClassDesc} is encoded as the {@code String} that contains its descriptor string, such as
 * {@code Ljava/lang/String;}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ClassDescMixin {
    private ClassDescMixin() {
        throw new UnsupportedOperationException("ClassDescMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the descriptor string of the given class descriptor.
     *
     * @param value the class descriptor, or {@code null}
     * @return the descriptor string, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(ClassDesc value) {
        return value == null ? null : value.descriptorString();
    }

    /**
     * Returns the class descriptor with the given descriptor string.
     *
     * @param value the descriptor string, or {@code null}
     * @return the class descriptor, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid field descriptor
     */
    @TypeDeserializer
    public static ClassDesc ofClassDesc(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : ClassDesc.ofDescriptor(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(ClassDesc.class, value, exception);
        }
    }
}
