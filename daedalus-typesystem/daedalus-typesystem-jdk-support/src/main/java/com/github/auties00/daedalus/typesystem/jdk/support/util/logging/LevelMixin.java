package com.github.auties00.daedalus.typesystem.jdk.support.util.logging;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.util.logging.Level;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Level}.
 *
 * <p>{@code Level} is encoded as the {@code String} that contains its name, such as {@code INFO}.
 *
 * <p>This mixin only requires the {@code java.logging} module statically, so it can
 * only be used by an application that reads that module.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class LevelMixin {
    private LevelMixin() {
        throw new UnsupportedOperationException("LevelMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the name of the given level.
     *
     * @param value the level, or {@code null}
     * @return the name, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static String toText(Level value) {
        return value == null ? null : value.getName();
    }

    /**
     * Returns the level with the given name or integer value.
     *
     * @param value the name or integer value, or {@code null}
     * @return the level, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is neither the name of a known
     *         level nor an integer
     */
    @TypeDeserializer
    public static Level ofLevel(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : Level.parse(value);
        } catch (IllegalArgumentException exception) {
            throw TypeDeserializationException.malformedValue(Level.class, value, exception);
        }
    }
}
