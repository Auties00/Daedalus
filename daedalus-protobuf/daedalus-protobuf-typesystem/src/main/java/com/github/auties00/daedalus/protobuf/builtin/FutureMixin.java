package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDefaultValue;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;

import java.util.concurrent.*;

@SuppressWarnings("unused")
@ProtobufMixin
public final class FutureMixin {
    @ProtobufDefaultValue
    public static <T> Future<T> newFuture() {
        return CompletableFuture.completedFuture(null);
    }

    @ProtobufDeserializer
    public static <T> Future<T> ofNullable(T value) {
        return CompletableFuture.completedFuture(value);
    }

    @ProtobufSerializer
    public static <T> T toNullable(Future<T> future) {
        try {
            return future == null ? null : future.get();
        } catch (ExecutionException | CancellationException ex) {
            throw new ProtobufSerializationException("Cannot serialize future", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProtobufSerializationException("Cannot serialize future", ex);
        }
    }
}
