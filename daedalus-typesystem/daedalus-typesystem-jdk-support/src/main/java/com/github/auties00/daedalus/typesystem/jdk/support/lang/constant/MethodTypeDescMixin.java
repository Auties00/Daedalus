package com.github.auties00.daedalus.typesystem.jdk.support.lang.constant;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.lang.constant.MethodTypeDesc;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link MethodTypeDesc}.
 *
 * <p>{@code MethodTypeDesc} is encoded as the {@code String} that contains its descriptor string, such as
 * {@code (I)V}.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class MethodTypeDescMixin {
    private MethodTypeDescMixin() {
        throw new UnsupportedOperationException("MethodTypeDescMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the descriptor string of the given method type descriptor.
     *
     * @param value the method type descriptor, or {@code null}
     * @return the descriptor string, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(MethodTypeDesc value) {
        return value == null ? null : value.descriptorString();
    }

    /**
     * Returns the method type descriptor with the given descriptor string.
     *
     * @param value the descriptor string, or {@code null}
     * @return the method type descriptor, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not a valid method descriptor
     */
    @TypeDeserializer
    public static MethodTypeDesc ofMethodTypeDesc(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : MethodTypeDesc.ofDescriptor(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(MethodTypeDesc.class, value, exception);
        }
    }
}
