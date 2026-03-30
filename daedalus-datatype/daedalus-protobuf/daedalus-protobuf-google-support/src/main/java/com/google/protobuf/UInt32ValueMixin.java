package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

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
