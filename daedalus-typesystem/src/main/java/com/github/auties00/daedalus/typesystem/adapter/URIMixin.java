package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.net.URI;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link URI}.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class URIMixin {
    private URIMixin() {
        throw new UnsupportedOperationException("URIMixin is a mixin and cannot be instantiated");
    }

    @TypeDeserializer
    public static URI ofNullable(String value) {
        return value == null ? null : URI.create(value);
    }

    @TypeSerializer
    public static String toNullable(URI value) {
        return value == null ? null : value.toString();
    }
}
