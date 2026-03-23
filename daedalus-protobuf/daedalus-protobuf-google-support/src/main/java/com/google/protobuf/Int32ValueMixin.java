package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class Int32ValueMixin {
    @ProtobufDeserializer
    public static Integer ofNullable(Int32Value value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static Int32Value toNullable(Integer value) {
        return value == null ? null : new Int32Value(value);
    }
}
