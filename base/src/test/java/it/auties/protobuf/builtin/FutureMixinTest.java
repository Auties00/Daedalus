package it.auties.protobuf.builtin;

import it.auties.protobuf.exception.ProtobufSerializationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class FutureMixinTest {

    // Default value

    @Test
    void newFutureIsCompleted() {
        var result = FutureMixin.newFuture();
        assertTrue(result.isDone());
    }

    @Test
    void newFutureHasNullValue() throws ExecutionException, InterruptedException {
        var result = FutureMixin.newFuture();
        assertNull(result.get());
    }

    @Test
    void newFutureIsNotCompletedExceptionally() {
        var result = FutureMixin.newFuture();
        assertFalse(result.isDone());
    }

    @Test
    void newFutureReturnsDistinctInstances() {
        assertNotSame(FutureMixin.newFuture(), FutureMixin.newFuture());
    }

    // Deserializer

    @Test
    void ofNullableWithNullValue() throws ExecutionException, InterruptedException {
        Future<String> result = FutureMixin.ofValue(null);
        assertNotNull(result);
        assertTrue(result.isDone());
        assertNull(result.get());
    }

    @Test
    void ofNullableWithStringValue() throws ExecutionException, InterruptedException {
        var result = FutureMixin.ofValue("hello");
        assertTrue(result.isDone());
        assertEquals("hello", result.get());
    }

    @Test
    void ofNullableWithIntegerValue() throws ExecutionException, InterruptedException {
        var result = FutureMixin.ofValue(42);
        assertTrue(result.isDone());
        assertEquals(42, result.get());
    }

    @Test
    void ofNullableWithComplexObject() throws ExecutionException, InterruptedException {
        var list = java.util.List.of(1, 2, 3);
        var result = FutureMixin.ofValue(list);
        assertTrue(result.isDone());
        assertSame(list, result.get());
    }

    @Test
    void ofNullableIsNotCompletedExceptionally() {
        var result = FutureMixin.ofValue("test");
        assertFalse(result.isDone());
    }

    // Serializer

    @Test
    void toValueWithNullFutureReturnsNull() {
        assertNull(FutureMixin.toValue(null));
    }

    @Test
    void toValueWithCompletedFuture() {
        var future = CompletableFuture.completedFuture("hello");
        assertEquals("hello", FutureMixin.toValue(future));
    }

    @Test
    void toValueWithCompletedNullFuture() {
        var future = CompletableFuture.completedFuture(null);
        assertNull(FutureMixin.toValue(future));
    }

    @Test
    void toValueWithIncompleteFutureReturnsNull() {
        var future = new CompletableFuture<String>();
        assertNull(FutureMixin.toValue(future));
    }

    @Test
    void toValueWithExceptionallyCompletedFuture() {
        var future = new CompletableFuture<String>();
        future.completeExceptionally(new RuntimeException("test"));
        var ex = assertThrows(ProtobufSerializationException.class, () -> FutureMixin.toValue(future));
        assertInstanceOf(RuntimeException.class, ex.getCause());
    }

    // Round-trip

    @Test
    void roundTripWithStringValue() {
        String original = "round-trip";
        var future = FutureMixin.ofValue(original);
        String result = FutureMixin.toValue(future);
        assertEquals(original, result);
    }

    @Test
    void roundTripWithNullValue() {
        var future = FutureMixin.ofValue(null);
        assertNull(FutureMixin.toValue(future));
    }

    @Test
    void roundTripWithIntegerValue() {
        var future = FutureMixin.ofValue(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, FutureMixin.toValue(future));
    }
}
