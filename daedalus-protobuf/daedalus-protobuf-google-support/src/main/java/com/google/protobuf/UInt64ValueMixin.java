package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class UInt64ValueMixin {
    @ProtobufDeserializer
    public static Long ofNullable(UInt64Value value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static UInt64Value toNullable(Long value) {
        return value == null ? null : new UInt64Value(value);
    }
}
