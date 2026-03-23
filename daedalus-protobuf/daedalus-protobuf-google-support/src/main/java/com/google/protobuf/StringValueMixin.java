package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

@SuppressWarnings("unused")
@ProtobufMixin(scope = ProtobufMixin.Scope.GLOBAL)
public final class StringValueMixin {
    @ProtobufDeserializer
    public static String ofNullable(StringValue value) {
        return value == null ? null : value.value();
    }

    @ProtobufSerializer
    public static StringValue toNullable(String value) {
        return value == null ? null : new StringValue(value);
    }
}
