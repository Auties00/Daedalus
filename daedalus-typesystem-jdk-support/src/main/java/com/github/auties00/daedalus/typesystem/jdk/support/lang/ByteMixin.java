package com.github.auties00.daedalus.typesystem.jdk.support.lang;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link Byte} and
 * {@code byte}.
 *
 * <p>Data formats rarely have a dedicated encoding for a byte, so it is encoded as an
 * {@code int}. Deserializing an {@code int} out of the range of a byte causes a
 * {@link TypeDeserializationException}, instead of silently truncating it.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ByteMixin {
    private ByteMixin() {
        throw new UnsupportedOperationException("ByteMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns the given byte widened to an {@code int}.
     *
     * @param value the byte, or {@code null}
     * @return the widened value, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static Integer toInteger(Byte value) {
        return value == null ? null : value.intValue();
    }

    /**
     * Returns the byte with the given value.
     *
     * @param value the value, or {@code null}
     * @return the byte, or {@code null} if {@code value} is {@code null}
     * @throws TypeDeserializationException if {@code value} is out of the range of a byte
     */
    @TypeDeserializer
    public static Byte ofByte(Integer value) throws TypeDeserializationException {
        if (value == null) {
            return null;
        }

        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw TypeDeserializationException.outOfRange(Byte.class, value);
        }

        return value.byteValue();
    }
}
