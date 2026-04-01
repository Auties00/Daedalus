package com.google.protobuf;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class StringValueMixin {
    @TypeDeserializer
    public static String ofNullable(StringValue value) {
        return value == null ? null : value.value();
    }

    @TypeSerializer
    public static StringValue toNullable(String value) {
        return value == null ? null : new StringValue(value);
    }
}
