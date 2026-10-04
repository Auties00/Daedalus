package com.github.auties00.daedalus.protobuf.support.google;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.google.protobuf.Duration;
import com.google.protobuf.DurationBuilder;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class DurationMixin {
    @TypeDeserializer
    public static java.time.Duration ofNullable(Duration value) {
        return value == null ? null : java.time.Duration.ofSeconds(value.seconds(), value.nanos());
    }

    @TypeSerializer
    public static Duration toNullable(java.time.Duration value) {
        return value == null ? null : new DurationBuilder().seconds(value.getSeconds()).nanos(value.getNano()).build();
    }
}
