package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

import java.time.Instant;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class TimestampMixin {
    @ProtobufDeserializer
    public static Instant ofNullable(Timestamp value) {
        return value == null ? null : Instant.ofEpochSecond(value.seconds(), value.nanos());
    }

    @ProtobufSerializer
    public static Timestamp toNullable(Instant value) {
        return value == null ? null : new Timestamp(value.getEpochSecond(), value.getNano());
    }
}
