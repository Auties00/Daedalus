package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

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
