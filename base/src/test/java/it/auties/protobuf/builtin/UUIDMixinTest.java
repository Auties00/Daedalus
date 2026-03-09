package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDMixinTest {

    // Deserializer with null

    @Test
    void ofNullableWithNullReturnsNull() {
        assertNull(UUIDMixin.ofNullable(null));
    }

    // Deserializer with valid UUIDs

    @Test
    void ofNullableWithRandomUuid() {
        var uuid = UUID.randomUUID();
        var result = UUIDMixin.ofNullable(uuid.toString());
        assertEquals(uuid, result);
    }

    @Test
    void ofNullableWithKnownUuid() {
        var result = UUIDMixin.ofNullable("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result);
    }

    @Test
    void ofNullableWithNilUuid() {
        var result = UUIDMixin.ofNullable("00000000-0000-0000-0000-000000000000");
        assertEquals(new UUID(0, 0), result);
    }

    @Test
    void ofNullableWithMaxUuid() {
        var result = UUIDMixin.ofNullable("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertEquals(new UUID(-1, -1), result);
    }

    @Test
    void ofNullableWithUpperCaseUuid() {
        var result = UUIDMixin.ofNullable("550E8400-E29B-41D4-A716-446655440000");
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result);
    }

    @Test
    void ofNullableWithVersion4Uuid() {
        var uuid = UUID.randomUUID();
        var result = UUIDMixin.ofNullable(uuid.toString());
        assertEquals(4, result.version());
    }

    // Deserializer with invalid UUID throws

    @Test
    void ofNullableWithInvalidUuidThrows() {
        assertThrows(IllegalArgumentException.class, () -> UUIDMixin.ofNullable("not-a-uuid"));
    }

    @Test
    void ofNullableWithEmptyStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> UUIDMixin.ofNullable(""));
    }

    @Test
    void ofNullableWithTruncatedUuidThrows() {
        assertThrows(IllegalArgumentException.class, () -> UUIDMixin.ofNullable("550e8400-e29b-41d4"));
    }

    // Serializer with null

    @Test
    void toValueWithNullReturnsNull() {
        assertNull(UUIDMixin.toValue(null));
    }

    // Serializer with valid UUIDs

    @Test
    void toValueWithRandomUuid() {
        var uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), UUIDMixin.toValue(uuid));
    }

    @Test
    void toValueWithKnownUuid() {
        var uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", UUIDMixin.toValue(uuid));
    }

    @Test
    void toValueWithNilUuid() {
        var uuid = new UUID(0, 0);
        assertEquals("00000000-0000-0000-0000-000000000000", UUIDMixin.toValue(uuid));
    }

    @Test
    void toValueReturnsLowerCase() {
        var uuid = UUID.fromString("550E8400-E29B-41D4-A716-446655440000");
        var result = UUIDMixin.toValue(uuid);
        assertEquals(result.toLowerCase(), result);
    }

    // Round-trip tests

    @Test
    void roundTripRandomUuid() {
        var uuid = UUID.randomUUID();
        String serialized = UUIDMixin.toValue(uuid);
        UUID deserialized = UUIDMixin.ofNullable(serialized);
        assertEquals(uuid, deserialized);
    }

    @Test
    void roundTripFromString() {
        String original = "550e8400-e29b-41d4-a716-446655440000";
        var uuid = UUIDMixin.ofNullable(original);
        String result = UUIDMixin.toValue(uuid);
        assertEquals(original, result);
    }

    @Test
    void roundTripNull() {
        assertNull(UUIDMixin.toValue(UUIDMixin.ofNullable(null)));
    }

    @Test
    void roundTripNilUuid() {
        String original = "00000000-0000-0000-0000-000000000000";
        var uuid = UUIDMixin.ofNullable(original);
        String result = UUIDMixin.toValue(uuid);
        assertEquals(original, result);
    }

    @Test
    void roundTripMultipleUuids() {
        for (int i = 0; i < 100; i++) {
            var uuid = UUID.randomUUID();
            var serialized = UUIDMixin.toValue(uuid);
            var deserialized = UUIDMixin.ofNullable(serialized);
            assertEquals(uuid, deserialized);
        }
    }

    // Consistency

    @Test
    void toValueProducesConsistentOutput() {
        var uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(UUIDMixin.toValue(uuid), UUIDMixin.toValue(uuid));
    }

    @Test
    void ofNullableProducesConsistentOutput() {
        String input = "550e8400-e29b-41d4-a716-446655440000";
        assertEquals(UUIDMixin.ofNullable(input), UUIDMixin.ofNullable(input));
    }
}
