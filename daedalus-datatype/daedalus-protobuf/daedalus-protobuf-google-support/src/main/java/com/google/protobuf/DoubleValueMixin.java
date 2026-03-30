package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class DoubleValueMixin {
    @ProtobufDeserializer
    public static Double ofNullable(DoubleValue value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static DoubleValue toNullable(Double value) {
        return value == null ? null : new DoubleValue(value);
    }
}
