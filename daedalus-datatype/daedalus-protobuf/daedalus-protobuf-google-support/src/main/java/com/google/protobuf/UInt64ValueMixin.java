package com.google.protobuf;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class UInt64ValueMixin {
    @TypeDeserializer
    public static Long ofNullable(UInt64Value value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static UInt64Value toNullable(Long value) {
        return value == null ? null : new UInt64Value(value);
    }
}
