package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A {@link TypeMixin} that provides default values, serializers, and deserializers for the {@link Optional} family of types.
 */
@SuppressWarnings({"OptionalAssignedToNull", "OptionalUsedAsFieldOrParameterType", "unused"})
@TypeMixin
public final class OptionalMixin {
    private OptionalMixin() {
        throw new UnsupportedOperationException("OptionalMixin is a mixin and cannot be instantiated");
    }

    @TypeDefaultValue
    public static <T> Optional<T> newOptional() {
        return Optional.empty();
    }

    @TypeDefaultValue
    public static OptionalInt newOptionalInt() {
        return OptionalInt.empty();
    }

    @TypeDefaultValue
    public static OptionalLong newOptionalLong() {
        return OptionalLong.empty();
    }

    @TypeDefaultValue
    public static OptionalDouble newOptionalDouble() {
        return OptionalDouble.empty();
    }

    @TypeDeserializer
    public static <T> Optional<T> ofNullable(T value) {
        return Optional.ofNullable(value);
    }

    @TypeDeserializer
    public static OptionalInt ofNullable(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    @TypeDeserializer
    public static OptionalLong ofNullable(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @TypeDeserializer
    public static OptionalDouble ofNullable(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @TypeSerializer
    public static <T> T toNullable(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }

    @TypeSerializer
    public static Integer toNullable(OptionalInt value) {
        return value == null ||  value.isEmpty() ? null : value.getAsInt();
    }

    @TypeSerializer
    public static Long toNullable(OptionalLong value) {
        return value == null || value.isEmpty() ? null : value.getAsLong();
    }

    @TypeSerializer
    public static Double toNullable(OptionalDouble value) {
        return value == null || value.isEmpty() ? null : value.getAsDouble();
    }
}
