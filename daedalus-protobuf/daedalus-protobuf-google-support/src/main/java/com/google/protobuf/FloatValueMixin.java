package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class FloatValueMixin {
    @ProtobufDeserializer
    public static Float ofNullable(FloatValue value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static FloatValue toNullable(Float value) {
        return value == null ? null : new FloatValue(value);
    }
}
