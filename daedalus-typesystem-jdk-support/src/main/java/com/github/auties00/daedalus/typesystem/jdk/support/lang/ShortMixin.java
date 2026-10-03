package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Short} and
 * {@code short}.
 *
 * <p>Data formats rarely have a dedicated encoding for a short, so it is encoded as an
 * {@code int}. Deserializing an {@code int} out of the range of a short causes a
 * {@link TypeDeserializationException}, instead of silently truncating it.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ShortMixin {
    private ShortMixin() {
        throw new UnsupportedOperationException("ShortMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the given short widened to an {@code int}.
     *
     * @param value the short, or {@code null}
     * @return the widened value, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }

    /**
     * Returns the short with the given value.
     *
     * @param value the value, or {@code null}
     * @return the short, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is out of the range of a short
     */
    @TypeDeserializer
    public static Short ofShort(Integer value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw TypeDeserializationException.outOfRange(Short.class, value);
        }

        return value.shortValue();
    }
}
