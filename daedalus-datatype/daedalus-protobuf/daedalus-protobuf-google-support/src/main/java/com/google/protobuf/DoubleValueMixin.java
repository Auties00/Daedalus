package com.google.protobuf;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class DoubleValueMixin {
    @TypeDeserializer
    public static Double ofNullable(DoubleValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static DoubleValue toNullable(Double value) {
        return value == null ? null : new DoubleValue(value);
    }
}
