package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class OptionalMixinTest {

    // Default values

    @Test
    void newOptionalReturnsEmpty() {
        Optional<String> result = OptionalMixin.newOptional();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void newOptionalIntReturnsEmpty() {
        var result = OptionalMixin.newOptionalInt();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void newOptionalLongReturnsEmpty() {
        var result = OptionalMixin.newOptionalLong();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void newOptionalDoubleReturnsEmpty() {
        var result = OptionalMixin.newOptionalDouble();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Deserializers with null input

    @Test
    void ofOptionalWithNullReturnsEmpty() {
        Optional<String> result = OptionalMixin.ofOptional((String) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void ofOptionalIntWithNullReturnsEmpty() {
        var result = OptionalMixin.ofOptional((Integer) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void ofOptionalLongWithNullReturnsEmpty() {
        var result = OptionalMixin.ofOptional((Long) null);
        assertTrue(result.isEmpty());
    }

    @Test
    void ofOptionalDoubleWithNullReturnsEmpty() {
        var result = OptionalMixin.ofOptional((Double) null);
        assertTrue(result.isEmpty());
    }

    // Deserializers with non-null input

    @Test
    void ofOptionalWithStringValue() {
        var result = OptionalMixin.ofOptional("hello");
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    void ofOptionalWithEmptyString() {
        var result = OptionalMixin.ofOptional("");
        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    void ofOptionalIntWithValue() {
        var result = OptionalMixin.ofOptional(42);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    void ofOptionalIntWithZero() {
        var result = OptionalMixin.ofOptional(0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    void ofOptionalIntWithNegative() {
        var result = OptionalMixin.ofOptional(-1);
        assertTrue(result.isPresent());
        assertEquals(-1, result.getAsInt());
    }

    @Test
    void ofOptionalIntWithMaxValue() {
        var result = OptionalMixin.ofOptional(Integer.MAX_VALUE);
        assertTrue(result.isPresent());
        assertEquals(Integer.MAX_VALUE, result.getAsInt());
    }

    @Test
    void ofOptionalLongWithValue() {
        var result = OptionalMixin.ofOptional(999L);
        assertTrue(result.isPresent());
        assertEquals(999L, result.getAsLong());
    }

    @Test
    void ofOptionalLongWithMaxValue() {
        var result = OptionalMixin.ofOptional(Long.MAX_VALUE);
        assertTrue(result.isPresent());
        assertEquals(Long.MAX_VALUE, result.getAsLong());
    }

    @Test
    void ofOptionalDoubleWithValue() {
        var result = OptionalMixin.ofOptional(3.14);
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble());
    }

    @Test
    void ofOptionalDoubleWithZero() {
        var result = OptionalMixin.ofOptional(0.0);
        assertTrue(result.isPresent());
        assertEquals(0.0, result.getAsDouble());
    }

    @Test
    void ofOptionalDoubleWithNaN() {
        var result = OptionalMixin.ofOptional(Double.NaN);
        assertTrue(result.isPresent());
        assertTrue(Double.isNaN(result.getAsDouble()));
    }

    @Test
    void ofOptionalDoubleWithInfinity() {
        var result = OptionalMixin.ofOptional(Double.POSITIVE_INFINITY);
        assertTrue(result.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, result.getAsDouble());
    }

    @Test
    void ofOptionalDoubleWithNegativeInfinity() {
        var result = OptionalMixin.ofOptional(Double.NEGATIVE_INFINITY);
        assertTrue(result.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, result.getAsDouble());
    }

    // Serializers with null Optional

    @Test
    void toNullableValueWithNullOptionalReturnsNull() {
        assertNull(OptionalMixin.toNullableValue(null));
    }

    @Test
    void toNullableIntWithNullReturnsNull() {
        assertNull(OptionalMixin.toNullableInt(null));
    }

    @Test
    void toNullableLongWithNullReturnsNull() {
        assertNull(OptionalMixin.toNullableLong(null));
    }

    @Test
    void toNullableDoubleWithNullReturnsNull() {
        assertNull(OptionalMixin.toNullableDouble(null));
    }

    // Serializers with empty Optional

    @Test
    void toNullableValueWithEmptyOptionalReturnsNull() {
        assertNull(OptionalMixin.toNullableValue(Optional.empty()));
    }

    @Test
    void toNullableIntWithEmptyReturnsNull() {
        assertNull(OptionalMixin.toNullableInt(OptionalInt.empty()));
    }

    @Test
    void toNullableLongWithEmptyReturnsNull() {
        assertNull(OptionalMixin.toNullableLong(OptionalLong.empty()));
    }

    @Test
    void toNullableDoubleWithEmptyReturnsNull() {
        assertNull(OptionalMixin.toNullableDouble(OptionalDouble.empty()));
    }

    // Serializers with present Optional

    @Test
    void toNullableValueWithPresentOptional() {
        assertEquals("hello", OptionalMixin.toNullableValue(Optional.of("hello")));
    }

    @Test
    void toNullableValueWithOptionalContainingNull() {
        assertNull(OptionalMixin.toNullableValue(Optional.ofNullable(null)));
    }

    @Test
    void toNullableIntWithPresentValue() {
        assertEquals(42, OptionalMixin.toNullableInt(OptionalInt.of(42)));
    }

    @Test
    void toNullableIntWithZero() {
        assertEquals(0, OptionalMixin.toNullableInt(OptionalInt.of(0)));
    }

    @Test
    void toNullableLongWithPresentValue() {
        assertEquals(999L, OptionalMixin.toNullableLong(OptionalLong.of(999L)));
    }

    @Test
    void toNullableDoubleWithPresentValue() {
        assertEquals(3.14, OptionalMixin.toNullableDouble(OptionalDouble.of(3.14)));
    }

    // Round-trip tests

    @Test
    void roundTripOptionalString() {
        String original = "round-trip";
        var opt = OptionalMixin.ofOptional(original);
        String result = OptionalMixin.toNullableValue(opt);
        assertEquals(original, result);
    }

    @Test
    void roundTripOptionalNull() {
        var opt = OptionalMixin.ofOptional((String) null);
        assertNull(OptionalMixin.toNullableValue(opt));
    }

    @Test
    void roundTripOptionalInt() {
        var opt = OptionalMixin.ofOptional(42);
        assertEquals(42, OptionalMixin.toNullableInt(opt));
    }

    @Test
    void roundTripOptionalIntNull() {
        var opt = OptionalMixin.ofOptional((Integer) null);
        assertNull(OptionalMixin.toNullableInt(opt));
    }

    @Test
    void roundTripOptionalLong() {
        var opt = OptionalMixin.ofOptional(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, OptionalMixin.toNullableLong(opt));
    }

    @Test
    void roundTripOptionalLongNull() {
        var opt = OptionalMixin.ofOptional((Long) null);
        assertNull(OptionalMixin.toNullableLong(opt));
    }

    @Test
    void roundTripOptionalDouble() {
        var opt = OptionalMixin.ofOptional(2.718);
        assertEquals(2.718, OptionalMixin.toNullableDouble(opt));
    }

    @Test
    void roundTripOptionalDoubleNull() {
        var opt = OptionalMixin.ofOptional((Double) null);
        assertNull(OptionalMixin.toNullableDouble(opt));
    }
}
