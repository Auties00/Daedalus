package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

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
