package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class UInt32ValueMixin {
    @ProtobufDeserializer
    public static Integer ofNullable(UInt32Value value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static UInt32Value toNullable(Integer value) {
        return value == null ? null : new UInt32Value(value);
    }
}
