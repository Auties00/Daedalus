package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link StringBuffer}.
 *
 * <p>{@code StringBuffer} is encoded as the {@code String} that contains its characters.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class StringBufferMixin {
    private StringBufferMixin() {
        throw new UnsupportedOperationException("StringBufferMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string that contains the characters of the given string buffer.
     *
     * @param value the string buffer, or {@code null}
     * @return the string, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(StringBuffer value) {
        return value == null ? null : value.toString();
    }

    /**
     * Returns a new string buffer that contains the characters of the given string.
     *
     * @param value the string, or {@code null}
     * @return the string buffer, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static StringBuffer ofStringBuffer(String value) {
        return value == null ? null : new StringBuffer(value);
    }
}
