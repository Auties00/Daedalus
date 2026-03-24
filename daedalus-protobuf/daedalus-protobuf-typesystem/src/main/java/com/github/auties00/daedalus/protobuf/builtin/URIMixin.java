package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

import java.net.URI;

@SuppressWarnings("unused")
@ProtobufMixin
public final class URIMixin {
    @ProtobufDeserializer
    public static URI ofNullable(String value) {
        return value == null ? null : URI.create(value);
    }

    @ProtobufSerializer
    public static String toNullable(URI value) {
        return value == null ? null : value.toString();
    }
}
