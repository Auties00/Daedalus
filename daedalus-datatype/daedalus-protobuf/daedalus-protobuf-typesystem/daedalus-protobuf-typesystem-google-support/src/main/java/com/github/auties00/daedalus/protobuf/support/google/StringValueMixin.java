package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.StringValue;
import com.google.protobuf.StringValueBuilder;

@SuppressWarnings({"unused", "preview"})
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class StringValueMixin {
    @TypeDeserializer
    public static LazyConstant<String> ofNullable(StringValue value) {
        return value == null ? null : LazyConstant.of(value::value);
    }

    @TypeSerializer
    public static StringValue toNullable(LazyConstant<String> value) {
        return value == null ? null : new StringValueBuilder().value(value.get()).build();
    }
}
