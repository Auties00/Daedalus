package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

/**
 * A {@link TypeMixin} that provides a serializer for {@link CharSequence}.
 *
 * <p>A character sequence is encoded as the {@code String} that contains its
 * characters. A field declared as a {@code CharSequence} is deserialized as a
 * {@code String}, which needs no conversion.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CharSequenceMixin {
    private CharSequenceMixin() {
        throw new UnsupportedOperationException("CharSequenceMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the string that contains the characters of the given sequence.
     *
     * @param value the character sequence, or {@code null}
     * @return the string, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(CharSequence value) {
        return value == null ? null : value.toString();
    }
}
