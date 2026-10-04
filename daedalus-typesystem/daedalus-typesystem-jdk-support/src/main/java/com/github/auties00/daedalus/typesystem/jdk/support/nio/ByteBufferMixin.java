package com.github.auties00.daedalus.typesystem.jdk.support.nio;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.nio.ByteBuffer;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link ByteBuffer}.
 *
 * <p>A byte buffer is encoded as the {@code byte[]} that contains its remaining bytes.
 * The position of a serialized buffer is never modified, and a deserialized buffer wraps
 * the encoded value.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class ByteBufferMixin {
    private ByteBufferMixin() {
        throw new UnsupportedOperationException("ByteBufferMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns a copy of the remaining bytes of the given buffer, without modifying its
     * position.
     *
     * @param value the buffer, or {@code null}
     * @return the remaining bytes, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(ByteBuffer value) {
        if (value == null) {
            return null;
        }

        var result = new byte[value.remaining()];
        value.duplicate().get(result);
        return result;
    }

    /**
     * Returns a buffer that wraps the given bytes.
     *
     * @param value the bytes, or {@code null}
     * @return the buffer, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static ByteBuffer ofByteBuffer(byte[] value) {
        return value == null ? null : ByteBuffer.wrap(value);
    }
}
