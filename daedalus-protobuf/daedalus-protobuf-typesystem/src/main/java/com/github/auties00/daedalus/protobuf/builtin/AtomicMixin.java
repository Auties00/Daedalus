package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDefaultValue;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@ProtobufMixin
public final class AtomicMixin {
    @ProtobufDefaultValue
    public static AtomicInteger newAtomicInt() {
        return new AtomicInteger();
    }

    @ProtobufDefaultValue
    public static AtomicLong newAtomicLong() {
        return new AtomicLong();
    }

    @ProtobufDefaultValue
    public static AtomicBoolean newAtomicBoolean() {
        return new AtomicBoolean();
    }

    @ProtobufDefaultValue
    public static <T> AtomicReference<T> newAtomicReference() {
        return new AtomicReference<>();
    }

    @ProtobufDeserializer
    public static AtomicInteger ofNullable(Integer value) {
        return value == null ? new AtomicInteger() : new AtomicInteger(value);
    }

    @ProtobufDeserializer
    public static AtomicLong ofNullable(Long value) {
        return value == null ? new AtomicLong() : new AtomicLong(value);
    }

    @ProtobufDeserializer
    public static AtomicBoolean ofNullable(Boolean value) {
        return value == null ? new AtomicBoolean() : new AtomicBoolean(value);
    }

    @ProtobufDeserializer
    public static <T> AtomicReference<T> ofNullable(T value) {
        return new AtomicReference<>(value);
    }

    @ProtobufSerializer
    public static int toNullable(AtomicInteger value) {
        return value.get();
    }

    @ProtobufSerializer
    public static long toNullable(AtomicLong value) {
        return value.get();
    }

    @ProtobufSerializer
    public static boolean toNullable(AtomicBoolean value) {
        return value.get();
    }

    @ProtobufSerializer
    public static <T> T toNullable(AtomicReference<T> value) {
        return value.get();
    }
}
