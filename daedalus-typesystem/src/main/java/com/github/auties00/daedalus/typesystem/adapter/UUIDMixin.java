package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.util.UUID;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link UUID}.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class UUIDMixin {
    private UUIDMixin() {
        throw new UnsupportedOperationException("UUIDMixin is a mixin and cannot be instantiated");
    }

    @TypeDeserializer
    public static UUID ofNullable(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new TypeDeserializationException(e);
        }
    }

    @TypeSerializer
    public static String toNullable(UUID value) {
        return value == null ? null : value.toString();
    }
}
