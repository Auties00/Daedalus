package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

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
