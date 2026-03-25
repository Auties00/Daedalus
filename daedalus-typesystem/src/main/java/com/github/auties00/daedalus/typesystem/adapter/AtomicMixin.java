package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link TypeMixin} that provides default values, serializers, and deserializers for the atomic types in the {@code java.util.concurrent.atomic} package.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class AtomicMixin {
    private AtomicMixin() {
        throw new UnsupportedOperationException("AtomicMixin is a mixin and cannot be instantiated");
    }

    @TypeDefaultValue
    public static AtomicInteger newAtomicInt() {
        return new AtomicInteger();
    }

    @TypeDefaultValue
    public static AtomicLong newAtomicLong() {
        return new AtomicLong();
    }

    @TypeDefaultValue
    public static AtomicBoolean newAtomicBoolean() {
        return new AtomicBoolean();
    }

    @TypeDefaultValue
    public static <T> AtomicReference<T> newAtomicReference() {
        return new AtomicReference<>();
    }

    @TypeDeserializer
    public static AtomicInteger ofNullable(Integer value) {
        return value == null ? new AtomicInteger() : new AtomicInteger(value);
    }

    @TypeDeserializer
    public static AtomicLong ofNullable(Long value) {
        return value == null ? new AtomicLong() : new AtomicLong(value);
    }

    @TypeDeserializer
    public static AtomicBoolean ofNullable(Boolean value) {
        return value == null ? new AtomicBoolean() : new AtomicBoolean(value);
    }

    @TypeDeserializer
    public static <T> AtomicReference<T> ofNullable(T value) {
        return new AtomicReference<>(value);
    }

    @TypeSerializer
    public static int toNullable(AtomicInteger value) {
        return value.get();
    }

    @TypeSerializer
    public static long toNullable(AtomicLong value) {
        return value.get();
    }

    @TypeSerializer
    public static boolean toNullable(AtomicBoolean value) {
        return value.get();
    }

    @TypeSerializer
    public static <T> T toNullable(AtomicReference<T> value) {
        return value.get();
    }
}
