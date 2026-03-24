package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

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
