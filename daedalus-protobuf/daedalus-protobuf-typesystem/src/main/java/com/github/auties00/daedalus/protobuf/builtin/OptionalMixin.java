package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDefaultValue;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

@SuppressWarnings({"OptionalAssignedToNull", "OptionalUsedAsFieldOrParameterType", "unused"})
@ProtobufMixin
public final class OptionalMixin {
    @ProtobufDefaultValue
    public static <T> Optional<T> newOptional() {
        return Optional.empty();
    }

    @ProtobufDefaultValue
    public static OptionalInt newOptionalInt() {
        return OptionalInt.empty();
    }

    @ProtobufDefaultValue
    public static OptionalLong newOptionalLong() {
        return OptionalLong.empty();
    }

    @ProtobufDefaultValue
    public static OptionalDouble newOptionalDouble() {
        return OptionalDouble.empty();
    }

    @ProtobufDeserializer
    public static <T> Optional<T> ofNullable(T value) {
        return Optional.ofNullable(value);
    }

    @ProtobufDeserializer
    public static OptionalInt ofNullable(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    @ProtobufDeserializer
    public static OptionalLong ofNullable(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @ProtobufDeserializer
    public static OptionalDouble ofNullable(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @ProtobufSerializer
    public static <T> T toNullable(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }

    @ProtobufSerializer
    public static Integer toNullable(OptionalInt value) {
        return value == null ||  value.isEmpty() ? null : value.getAsInt();
    }

    @ProtobufSerializer
    public static Long toNullable(OptionalLong value) {
        return value == null || value.isEmpty() ? null : value.getAsLong();
    }

    @ProtobufSerializer
    public static Double toNullable(OptionalDouble value) {
        return value == null || value.isEmpty() ? null : value.getAsDouble();
    }
}
