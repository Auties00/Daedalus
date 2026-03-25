package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeSerializationException;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A {@link TypeMixin} that provides default values, serializers, and deserializers for the {@link Future} type.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class FutureMixin {
    private FutureMixin() {
        throw new UnsupportedOperationException("FutureMixin is a mixin and cannot be instantiated");
    }

    @TypeDefaultValue
    public static <T> Future<T> newFuture() {
        return CompletableFuture.completedFuture(null);
    }

    @TypeDeserializer
    public static <T> Future<T> ofNullable(T value) {
        return CompletableFuture.completedFuture(value);
    }

    @TypeSerializer
    public static <T> T toNullable(Future<T> future) throws TypeSerializationException {
        try {
            return future == null ? null : future.get();
        } catch (ExecutionException | CancellationException ex) {
            throw new TypeSerializationException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TypeSerializationException(ex);
        }
    }
}
