package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class BytesValueMixin {
    @ProtobufDeserializer
    public static byte[] ofNullable(BytesValue value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static BytesValue toNullable(byte[] value) {
        return value == null ? null : new BytesValue(value);
    }
}
