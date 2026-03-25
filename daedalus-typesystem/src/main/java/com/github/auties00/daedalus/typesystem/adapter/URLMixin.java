package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * A {@link TypeMixin} that provides serializers and deserializers for {@link URL}.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class URLMixin {
    private URLMixin() {
        throw new UnsupportedOperationException("URLMixin is a mixin and cannot be instantiated");
    }

    @TypeDeserializer
    public static URL ofNullable(String value) throws TypeDeserializationException {
        try {
            return value == null ? null : URI.create(value).toURL();
        }catch (MalformedURLException | IllegalArgumentException exception) {
            throw new TypeDeserializationException(exception);
        }
    }

    @TypeSerializer
    public static String toNullable(URL value) {
        return value == null ? null : value.toString();
    }
}
