package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int32ValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class Int32ValueMixin {
    @TypeDeserializer
    public static Integer ofNullable(Int32Value value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static Int32Value toNullable(Integer value) {
        return value == null ? null : new Int32ValueBuilder().value(value).build();
    }
}
