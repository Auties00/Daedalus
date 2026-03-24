package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

import java.util.UUID;

@SuppressWarnings("unused")
@ProtobufMixin
public final class UUIDMixin {
    @ProtobufDeserializer
    public static UUID ofNullable(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    @ProtobufSerializer
    public static String toNullable(UUID value) {
        return value == null ? null : value.toString();
    }
}
