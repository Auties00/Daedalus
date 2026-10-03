package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.BytesValue;
import com.google.protobuf.BytesValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BytesValueMixin {
    @TypeDeserializer
    public static byte[] ofNullable(BytesValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static BytesValue toNullable(byte[] value) {
        return value == null ? null : new BytesValueBuilder().value(value).build();
    }
}
