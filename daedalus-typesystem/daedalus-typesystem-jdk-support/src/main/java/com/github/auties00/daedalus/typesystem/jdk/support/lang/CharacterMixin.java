package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Character}
 * and {@code char}.
 *
 * <p>A character is encoded as the {@code int} that contains its UTF-16 code unit.
 * Deserializing an {@code int} that is not between {@code 0} and {@code 0xFFFF} causes a
 * {@link TypeDeserializationException}, instead of silently truncating it.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class CharacterMixin {
    private CharacterMixin() {
        throw new UnsupportedOperationException("CharacterMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the UTF-16 code unit of the given character.
     *
     * @param value the character, or {@code null}
     * @return the code unit, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(Character value) {
        return value == null ? null : (int) value;
    }

    /**
     * Returns the character with the given UTF-16 code unit.
     *
     * @param value the code unit, or {@code null}
     * @return the character, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is not between {@code 0} and
     *         {@code 0xFFFF}
     */
    @TypeDeserializer
    public static Character ofCharacter(Integer value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
            throw TypeDeserializationException.outOfRange(Character.class, value);
        }

        return (char) value.intValue();
    }
}
