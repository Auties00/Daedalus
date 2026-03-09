package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AtomicValueMixinTest {

    // Default values

    @Test
    void newAtomicIntReturnsZero() {
        var result = AtomicValueMixin.newAtomicInt();
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    @Test
    void newAtomicLongReturnsZero() {
        var result = AtomicValueMixin.newAtomicLong();
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void newAtomicBooleanReturnsFalse() {
        var result = AtomicValueMixin.newAtomicBoolean();
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void newAtomicReferenceReturnsNull() {
        AtomicReference<String> result = AtomicValueMixin.newAtomicReference();
        assertNotNull(result);
        assertNull(result.get());
    }

    // Deserializers with null input

    @Test
    void ofAtomicIntegerWithNullReturnsDefault() {
        var result = AtomicValueMixin.ofAtomic((Integer) null);
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    @Test
    void ofAtomicLongWithNullReturnsDefault() {
        var result = AtomicValueMixin.ofAtomic((Long) null);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void ofAtomicBooleanWithNullReturnsDefault() {
        var result = AtomicValueMixin.ofAtomic((Boolean) null);
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    void ofAtomicReferenceWithNullWrapsNull() {
        AtomicReference<String> result = AtomicValueMixin.ofAtomic((String) null);
        assertNotNull(result);
        assertNull(result.get());
    }

    // Deserializers with non-null input

    @Test
    void ofAtomicIntegerWithValue() {
        var result = AtomicValueMixin.ofAtomic(42);
        assertEquals(42, result.get());
    }

    @Test
    void ofAtomicIntegerWithNegativeValue() {
        var result = AtomicValueMixin.ofAtomic(-100);
        assertEquals(-100, result.get());
    }

    @Test
    void ofAtomicIntegerWithMaxValue() {
        var result = AtomicValueMixin.ofAtomic(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, result.get());
    }

    @Test
    void ofAtomicIntegerWithMinValue() {
        var result = AtomicValueMixin.ofAtomic(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, result.get());
    }

    @Test
    void ofAtomicLongWithValue() {
        var result = AtomicValueMixin.ofAtomic(123456789L);
        assertEquals(123456789L, result.get());
    }

    @Test
    void ofAtomicLongWithNegativeValue() {
        var result = AtomicValueMixin.ofAtomic(-999L);
        assertEquals(-999L, result.get());
    }

    @Test
    void ofAtomicLongWithMaxValue() {
        var result = AtomicValueMixin.ofAtomic(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, result.get());
    }

    @Test
    void ofAtomicLongWithMinValue() {
        var result = AtomicValueMixin.ofAtomic(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, result.get());
    }

    @Test
    void ofAtomicBooleanWithTrue() {
        var result = AtomicValueMixin.ofAtomic(true);
        assertTrue(result.get());
    }

    @Test
    void ofAtomicBooleanWithFalse() {
        var result = AtomicValueMixin.ofAtomic(false);
        assertFalse(result.get());
    }

    @Test
    void ofAtomicReferenceWithValue() {
        var result = AtomicValueMixin.ofAtomic("hello");
        assertEquals("hello", result.get());
    }

    @Test
    void ofAtomicReferenceWithComplexObject() {
        var list = java.util.List.of(1, 2, 3);
        var result = AtomicValueMixin.ofAtomic(list);
        assertSame(list, result.get());
    }

    // Serializers

    @Test
    void toIntExtractsValue() {
        assertEquals(42, AtomicValueMixin.toInt(new AtomicInteger(42)));
    }

    @Test
    void toIntExtractsZero() {
        assertEquals(0, AtomicValueMixin.toInt(new AtomicInteger(0)));
    }

    @Test
    void toIntExtractsNegative() {
        assertEquals(-1, AtomicValueMixin.toInt(new AtomicInteger(-1)));
    }

    @Test
    void toLongExtractsValue() {
        assertEquals(999L, AtomicValueMixin.toLong(new AtomicLong(999L)));
    }

    @Test
    void toLongExtractsZero() {
        assertEquals(0L, AtomicValueMixin.toLong(new AtomicLong(0)));
    }

    @Test
    void toLongExtractsNegative() {
        assertEquals(-1L, AtomicValueMixin.toLong(new AtomicLong(-1)));
    }

    @Test
    void toBooleanExtractsTrue() {
        assertTrue(AtomicValueMixin.toBoolean(new AtomicBoolean(true)));
    }

    @Test
    void toBooleanExtractsFalse() {
        assertFalse(AtomicValueMixin.toBoolean(new AtomicBoolean(false)));
    }

    @Test
    void toValueExtractsReference() {
        assertEquals("test", AtomicValueMixin.toValue(new AtomicReference<>("test")));
    }

    @Test
    void toValueExtractsNullReference() {
        assertNull(AtomicValueMixin.toValue(new AtomicReference<>(null)));
    }

    // Round-trip tests

    @Test
    void roundTripAtomicInteger() {
        int original = 42;
        var atomic = AtomicValueMixin.ofAtomic(original);
        int result = AtomicValueMixin.toInt(atomic);
        assertEquals(original, result);
    }

    @Test
    void roundTripAtomicLong() {
        long original = Long.MAX_VALUE;
        var atomic = AtomicValueMixin.ofAtomic(original);
        long result = AtomicValueMixin.toLong(atomic);
        assertEquals(original, result);
    }

    @Test
    void roundTripAtomicBoolean() {
        var atomic = AtomicValueMixin.ofAtomic(true);
        boolean result = AtomicValueMixin.toBoolean(atomic);
        assertTrue(result);
    }

    @Test
    void roundTripAtomicReference() {
        String original = "round-trip";
        var atomic = AtomicValueMixin.ofAtomic(original);
        String result = AtomicValueMixin.toValue(atomic);
        assertEquals(original, result);
    }

    // Each call returns a distinct instance

    @Test
    void newAtomicIntReturnsDistinctInstances() {
        var a = AtomicValueMixin.newAtomicInt();
        var b = AtomicValueMixin.newAtomicInt();
        assertNotSame(a, b);
    }

    @Test
    void newAtomicLongReturnsDistinctInstances() {
        var a = AtomicValueMixin.newAtomicLong();
        var b = AtomicValueMixin.newAtomicLong();
        assertNotSame(a, b);
    }

    @Test
    void newAtomicBooleanReturnsDistinctInstances() {
        var a = AtomicValueMixin.newAtomicBoolean();
        var b = AtomicValueMixin.newAtomicBoolean();
        assertNotSame(a, b);
    }

    @Test
    void newAtomicReferenceReturnsDistinctInstances() {
        var a = AtomicValueMixin.newAtomicReference();
        var b = AtomicValueMixin.newAtomicReference();
        assertNotSame(a, b);
    }

    @Test
    void ofAtomicReturnsDistinctInstances() {
        var a = AtomicValueMixin.ofAtomic(5);
        var b = AtomicValueMixin.ofAtomic(5);
        assertNotSame(a, b);
        assertEquals(a.get(), b.get());
    }

    // Mutability of returned atomics

    @Test
    void returnedAtomicIntegerIsMutable() {
        var atomic = AtomicValueMixin.ofAtomic(10);
        atomic.set(20);
        assertEquals(20, AtomicValueMixin.toInt(atomic));
    }

    @Test
    void returnedAtomicLongIsMutable() {
        var atomic = AtomicValueMixin.ofAtomic(10L);
        atomic.set(20L);
        assertEquals(20L, AtomicValueMixin.toLong(atomic));
    }

    @Test
    void returnedAtomicBooleanIsMutable() {
        var atomic = AtomicValueMixin.ofAtomic(false);
        atomic.set(true);
        assertTrue(AtomicValueMixin.toBoolean(atomic));
    }

    @Test
    void returnedAtomicReferenceIsMutable() {
        var atomic = AtomicValueMixin.ofAtomic("before");
        atomic.set("after");
        assertEquals("after", AtomicValueMixin.toValue(atomic));
    }
}
