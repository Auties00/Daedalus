package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BoolValueBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BoolValueMixin {
    @TypeDeserializer
    public static Boolean ofNullable(BoolValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static BoolValue toNullable(Boolean value) {
        return value == null ? null : new BoolValueBuilder().value(value).build();
    }
}
