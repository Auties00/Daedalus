package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class DurationMixin {
    @ProtobufDeserializer
    public static java.time.Duration ofNullable(Duration value) {
        return value == null ? null : java.time.Duration.ofSeconds(value.seconds(), value.nanos());
    }

    @ProtobufSerializer
    public static Duration toNullable(java.time.Duration value) {
        return value == null ? null : new Duration(value.getSeconds(), value.getNano());
    }
}
