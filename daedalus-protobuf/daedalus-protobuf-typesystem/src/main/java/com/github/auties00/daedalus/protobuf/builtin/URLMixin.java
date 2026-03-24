package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@SuppressWarnings("unused")
@ProtobufMixin
public final class URLMixin {
    @ProtobufDeserializer
    public static URL ofNullable(String value) {
        try {
            return value == null ? null : URI.create(value).toURL();
        }catch (MalformedURLException | IllegalArgumentException exception) {
            throw new ProtobufDeserializationException("Cannot deserialize URL", exception);
        }
    }

    @ProtobufSerializer
    public static String toNullable(URL value) {
        return value == null ? null : value.toString();
    }
}
