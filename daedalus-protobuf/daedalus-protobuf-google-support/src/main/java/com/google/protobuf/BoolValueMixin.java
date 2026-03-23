package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class BoolValueMixin {
    @ProtobufDeserializer
    public static Boolean ofNullable(BoolValue value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static BoolValue toNullable(Boolean value) {
        return value == null ? null : new BoolValue(value);
    }
}
