package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

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
