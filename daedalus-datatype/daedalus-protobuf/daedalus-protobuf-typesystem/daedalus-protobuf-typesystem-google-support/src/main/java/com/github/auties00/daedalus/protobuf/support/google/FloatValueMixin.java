package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.FloatValue;
import com.google.protobuf.FloatValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class FloatValueMixin {
    @TypeDeserializer
    public static Float ofNullable(FloatValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static FloatValue toNullable(Float value) {
        return value == null ? null : new FloatValueBuilder().value(value).build();
    }
}
