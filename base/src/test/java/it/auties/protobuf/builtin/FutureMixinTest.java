package it.auties.protobuf.builtin;

import it.auties.protobuf.exception.ProtobufSerializationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class FutureMixinTest {

    // Default value

    @Test
    void newCompletableFutureIsCompleted() {
        var result = FutureMixin.newCompletableFuture();
        assertTrue(result.isDone());
    }

    @Test
    void newCompletableFutureHasNullValue() {
        var result = FutureMixin.newCompletableFuture();
        assertNull(result.getNow(new Object()));
    }

    @Test
    void newCompletableFutureIsNotCompletedExceptionally() {
        var result = FutureMixin.newCompletableFuture();
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void newCompletableFutureReturnsDistinctInstances() {
        assertNotSame(FutureMixin.newCompletableFuture(), FutureMixin.newCompletableFuture());
    }

    // Deserializer

    @Test
    void ofNullableWithNullValue() {
        CompletableFuture<String> result = FutureMixin.ofNullable(null);
        assertNotNull(result);
        assertTrue(result.isDone());
        assertNull(result.getNow("fallback"));
    }

    @Test
    void ofNullableWithStringValue() {
        var result = FutureMixin.ofNullable("hello");
        assertTrue(result.isDone());
        assertEquals("hello", result.getNow(null));
    }

    @Test
    void ofNullableWithIntegerValue() {
        var result = FutureMixin.ofNullable(42);
        assertTrue(result.isDone());
        assertEquals(42, result.getNow(null));
    }

    @Test
    void ofNullableWithComplexObject() {
        var list = java.util.List.of(1, 2, 3);
        var result = FutureMixin.ofNullable(list);
        assertTrue(result.isDone());
        assertSame(list, result.getNow(null));
    }

    @Test
    void ofNullableIsNotCompletedExceptionally() {
        var result = FutureMixin.ofNullable("test");
        assertFalse(result.isCompletedExceptionally());
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
        var future = FutureMixin.ofNullable(original);
        String result = FutureMixin.toValue(future);
        assertEquals(original, result);
    }

    @Test
    void roundTripWithNullValue() {
        var future = FutureMixin.ofNullable(null);
        assertNull(FutureMixin.toValue(future));
    }

    @Test
    void roundTripWithIntegerValue() {
        var future = FutureMixin.ofNullable(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, FutureMixin.toValue(future));
    }
}
