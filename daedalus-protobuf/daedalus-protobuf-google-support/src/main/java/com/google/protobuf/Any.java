package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * {@code Any} contains an arbitrary serialized protocol buffer message along with a
 * URL that describes the type of the serialized message.
 *
 * <p>In its binary encoding, an {@code Any} is an ordinary message; but in other wire
 * forms like JSON, it has a special encoding. The format of the type URL is
 * described on the {@code type_url} field.
 *
 * <p>Protobuf APIs provide utilities to interact with {@code Any} values:
 *
 * <p>- A 'pack' operation accepts a message and constructs a generic {@code Any} wrapper
 *
 * <pre>
 * around it.
 * </pre>
 *
 * <p>- An 'unpack' operation reads the content of an {@code Any} message, either into an
 *
 * <pre>
 * existing message or a new one. Unpack operations must check the type of the
 * value they unpack against the declared `type_url`.
 * </pre>
 *
 * <p>- An 'is' operation decides whether an {@code Any} contains a message of the given
 *
 * <pre>
 * type, i.e. whether it can 'unpack' that type.
 * </pre>
 *
 * <p>The JSON format representation of an {@code Any} follows one of these cases:
 *
 * <p>- For types without special-cased JSON encodings, the JSON format
 *
 * <pre>
 * representation of the `Any` is the same as that of the message, with an
 * additional `@type` field which contains the type URL.
 * </pre>
 *
 * <p>- For types with special-cased JSON encodings (typically called 'well-known'
 *
 * <pre>
 * types, listed in https://protobuf.dev/programming-guides/json/#any), the
 * JSON format representation has a key `@type` which contains the type URL
 * and a key `value` which contains the JSON-serialized value.
 * </pre>
 *
 * <p>The text format representation of an {@code Any} is like a message with one field
 * whose name is the type URL in brackets. For example, an {@code Any} containing a
 * {@code foo.Bar} message may be written {@code [type.googleapis.com/foo.Bar] { a: 2 }}.
 *
 * <p>Protobuf type {@code google.protobuf.Any}
 */
@ProtobufMessage
public final class Any {

    /**
     * Identifies the type of the serialized Protobuf message with a URI reference
     * consisting of a prefix ending in a slash and the fully-qualified type name.
     *
     * <p>Example: type.googleapis.com/google.protobuf.StringValue
     *
     * <p>This string must contain at least one {@code /} character, and the content after
     * the last {@code /} must be the fully-qualified name of the type in canonical
     * form, without a leading dot. Do not write a scheme on these URI references
     * so that clients do not attempt to contact them.
     *
     * <p>The prefix is arbitrary and Protobuf implementations are expected to
     * simply strip off everything up to and including the last {@code /} to identify
     * the type. {@code type.googleapis.com/} is a common default prefix that some
     * legacy implementations require. This prefix does not indicate the origin of
     * the type, and URIs containing it are not expected to respond to any
     * requests.
     *
     * <p>All type URL strings must be legal URI references with the additional
     * restriction (for the text format) that the content of the reference
     * must consist only of alphanumeric characters, percent-encoded escapes, and
     * characters in the following set (not including the outer backticks):
     * {@code /-.~_!$&()*+,;=}. Despite our allowing percent encodings, implementations
     * should not unescape them to prevent confusion with existing parsers. For
     * example, {@code type.googleapis.com%2FFoo} should be rejected.
     *
     * <p>In the original design of {@code Any}, the possibility of launching a type
     * resolution service at these type URLs was considered but Protobuf never
     * implemented one and considers contacting these URLs to be problematic and
     * a potential security issue. Do not attempt to contact type URLs.
     *
     * <p><code>string type_url = 1;</code>
     */
    @ProtobufMessage.StringField(index = 1)
    String typeUrl;

    /**
     * Holds a Protobuf serialization of the type described by type_url.
     *
     * <p><code>bytes value = 2;</code>
     */
    @ProtobufMessage.BytesField(index = 2)
    byte[] value;

    Any(String typeUrl, byte[] value) {
        this.typeUrl = typeUrl;
        this.value = value;
    }

    public String typeUrl() {
        return typeUrl;
    }

    public byte[] value() {
        return value;
    }
}
