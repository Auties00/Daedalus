package com.github.auties00.daedalus.typesystem.jdk.support.nio;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.nio.CharBuffer;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link CharBuffer}.
 *
 * <p>A char buffer is encoded as the {@code String} that contains its remaining
 * characters. The position of a serialized buffer is never modified, and a deserialized
 * buffer is a read only buffer that wraps the encoded value.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CharBufferMixin {
    private CharBufferMixin() {
        throw new UnsupportedOperationException("CharBufferMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the remaining characters of the given buffer, without modifying its
     * position.
     *
     * @param value the buffer, or {@code null}
     * @return the remaining characters, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(CharBuffer value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns a read only buffer that wraps the given string.
     *
     * @param value the string, or {@code null}
     * @return the buffer, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static CharBuffer ofCharBuffer(String value) {
        return value == null ? null : CharBuffer.wrap(value);
    }
}
