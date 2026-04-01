package com.google.protobuf;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.time.Instant;

@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class TimestampMixin {
    @TypeDeserializer
    public static Instant ofNullable(Timestamp value) {
        return value == null ? null : Instant.ofEpochSecond(value.seconds(), value.nanos());
    }

    @TypeSerializer
    public static Timestamp toNullable(Instant value) {
        return value == null ? null : new Timestamp(value.getEpochSecond(), value.getNano());
    }
}
