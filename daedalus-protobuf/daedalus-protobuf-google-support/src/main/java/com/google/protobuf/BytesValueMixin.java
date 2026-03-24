package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

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
