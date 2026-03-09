package it.auties.protobuf.builtin;

import it.auties.protobuf.exception.ProtobufDeserializationException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class URLMixinTest {

    // Deserializer with null

    @Test
    void ofNullableWithNullReturnsNull() {
        assertNull(URLMixin.ofNullable(null));
    }

    // Deserializer with valid URLs

    @Test
    void ofNullableWithHttpUrl() throws MalformedURLException {
        var result = URLMixin.ofNullable("https://example.com");
        assertNotNull(result);
        assertEquals(URI.create("https://example.com").toURL(), result);
    }

    @Test
    void ofNullableWithPathUrl() {
        var result = URLMixin.ofNullable("https://example.com/path/to/resource");
        assertNotNull(result);
        assertEquals("/path/to/resource", result.getPath());
    }

    @Test
    void ofNullableWithPortUrl() {
        var result = URLMixin.ofNullable("https://example.com:8080");
        assertEquals(8080, result.getPort());
    }

    @Test
    void ofNullableWithQueryUrl() {
        var result = URLMixin.ofNullable("https://example.com?key=value");
        assertEquals("key=value", result.getQuery());
    }

    @Test
    void ofNullableWithHttpProtocol() {
        var result = URLMixin.ofNullable("http://example.com");
        assertEquals("http", result.getProtocol());
    }

    @Test
    void ofNullableWithFtpProtocol() {
        var result = URLMixin.ofNullable("ftp://ftp.example.com/file.txt");
        assertEquals("ftp", result.getProtocol());
    }

    @Test
    void ofNullableWithFileProtocol() {
        var result = URLMixin.ofNullable("file:///tmp/test.txt");
        assertEquals("file", result.getProtocol());
    }

    // Deserializer with malformed URL throws

    @Test
    void ofNullableWithMalformedUrlThrows() {
        var ex = assertThrows(ProtobufDeserializationException.class,
                () -> URLMixin.ofNullable("not-a-valid-url"));
        assertEquals("Cannot deserialize URL", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void ofNullableWithUnknownProtocolThrows() {
        assertThrows(ProtobufDeserializationException.class,
                () -> URLMixin.ofNullable("unknownprotocol://example.com"));
    }

    // Serializer with null

    @Test
    void toValueWithNullReturnsNull() {
        assertNull(URLMixin.toValue(null));
    }

    // Serializer with valid URLs

    @Test
    void toValueWithHttpUrl() throws MalformedURLException {
        var url = URI.create("https://example.com").toURL();
        assertEquals("https://example.com", URLMixin.toValue(url));
    }

    @Test
    void toValueWithComplexUrl() throws MalformedURLException {
        var url = URI.create("https://example.com:443/path?q=1").toURL();
        var result = URLMixin.toValue(url);
        assertNotNull(result);
        assertTrue(result.startsWith("https://"));
    }

    // Round-trip tests

    @Test
    void roundTripSimpleUrl() {
        String original = "https://example.com";
        var url = URLMixin.ofNullable(original);
        String result = URLMixin.toValue(url);
        assertEquals(original, result);
    }

    @Test
    void roundTripComplexUrl() {
        String original = "https://example.com:8080/path?key=value";
        var url = URLMixin.ofNullable(original);
        String result = URLMixin.toValue(url);
        assertEquals(original, result);
    }

    @Test
    void roundTripNull() {
        assertNull(URLMixin.toValue(URLMixin.ofNullable(null)));
    }

    @Test
    void reverseRoundTrip() throws MalformedURLException {
        var url = URI.create("https://example.com/test").toURL();
        var serialized = URLMixin.toValue(url);
        var deserialized = URLMixin.ofNullable(serialized);
        assertEquals(url, deserialized);
    }
}
