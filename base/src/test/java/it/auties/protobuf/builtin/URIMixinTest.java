package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class URIMixinTest {

    // Deserializer with null

    @Test
    void ofNullableWithNullReturnsNull() {
        assertNull(URIMixin.ofNullable(null));
    }

    // Deserializer with valid URIs

    @Test
    void ofNullableWithSimpleUri() {
        var result = URIMixin.ofNullable("https://example.com");
        assertNotNull(result);
        assertEquals(URI.create("https://example.com"), result);
    }

    @Test
    void ofNullableWithPath() {
        var result = URIMixin.ofNullable("https://example.com/path/to/resource");
        assertEquals("https", result.getScheme());
        assertEquals("example.com", result.getHost());
        assertEquals("/path/to/resource", result.getPath());
    }

    @Test
    void ofNullableWithQueryParams() {
        var result = URIMixin.ofNullable("https://example.com?key=value&foo=bar");
        assertEquals("key=value&foo=bar", result.getQuery());
    }

    @Test
    void ofNullableWithFragment() {
        var result = URIMixin.ofNullable("https://example.com/page#section");
        assertEquals("section", result.getFragment());
    }

    @Test
    void ofNullableWithPort() {
        var result = URIMixin.ofNullable("https://example.com:8080/path");
        assertEquals(8080, result.getPort());
    }

    @Test
    void ofNullableWithFileScheme() {
        var result = URIMixin.ofNullable("file:///tmp/test.txt");
        assertEquals("file", result.getScheme());
    }

    @Test
    void ofNullableWithOpaqueUri() {
        var result = URIMixin.ofNullable("mailto:user@example.com");
        assertEquals("mailto", result.getScheme());
    }

    @Test
    void ofNullableWithEmptyString() {
        var result = URIMixin.ofNullable("");
        assertNotNull(result);
        assertEquals(URI.create(""), result);
    }

    // Deserializer with invalid URI throws

    @Test
    void ofNullableWithInvalidUriThrows() {
        assertThrows(IllegalArgumentException.class, () -> URIMixin.ofNullable("://invalid"));
    }

    // Serializer with null

    @Test
    void toValueWithNullReturnsNull() {
        assertNull(URIMixin.toValue(null));
    }

    // Serializer with valid URIs

    @Test
    void toValueWithSimpleUri() {
        var uri = URI.create("https://example.com");
        assertEquals("https://example.com", URIMixin.toValue(uri));
    }

    @Test
    void toValueWithComplexUri() {
        var uri = URI.create("https://user:pass@example.com:443/path?q=1#frag");
        assertEquals("https://user:pass@example.com:443/path?q=1#frag", URIMixin.toValue(uri));
    }

    // Round-trip tests

    @Test
    void roundTripSimpleUri() {
        String original = "https://example.com";
        var uri = URIMixin.ofNullable(original);
        String result = URIMixin.toValue(uri);
        assertEquals(original, result);
    }

    @Test
    void roundTripComplexUri() {
        String original = "https://example.com:8080/path?key=value#section";
        var uri = URIMixin.ofNullable(original);
        String result = URIMixin.toValue(uri);
        assertEquals(original, result);
    }

    @Test
    void roundTripNull() {
        assertNull(URIMixin.toValue(URIMixin.ofNullable(null)));
    }

    @Test
    void roundTripUnicodeUri() {
        String original = "https://example.com/path%20with%20spaces";
        var uri = URIMixin.ofNullable(original);
        String result = URIMixin.toValue(uri);
        assertEquals(original, result);
    }

    @Test
    void reverseRoundTrip() {
        var uri = URI.create("https://example.com/test");
        var serialized = URIMixin.toValue(uri);
        var deserialized = URIMixin.ofNullable(serialized);
        assertEquals(uri, deserialized);
    }
}
