package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt32ValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class UInt32ValueMixin {
    @TypeDeserializer
    public static Integer ofNullable(UInt32Value value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static UInt32Value toNullable(Integer value) {
        return value == null ? null : new UInt32ValueBuilder().value(value).build();
    }
}
