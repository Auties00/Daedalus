package com.google.protobuf;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BytesValueMixin {
    @TypeDeserializer
    public static byte[] ofNullable(BytesValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static BytesValue toNullable(byte[] value) {
        return value == null ? null : new BytesValue(value);
    }
}
