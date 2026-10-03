package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Int64ValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class Int64ValueMixin {
    @TypeDeserializer
    public static Long ofNullable(Int64Value value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static Int64Value toNullable(Long value) {
        return value == null ? null : new Int64ValueBuilder().value(value).build();
    }
}
