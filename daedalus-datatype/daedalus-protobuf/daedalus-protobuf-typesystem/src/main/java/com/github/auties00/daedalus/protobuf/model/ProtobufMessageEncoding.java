package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls the wire encoding of nested message fields in protobuf.
 * <p>
 * In protobuf editions, this replaces the proto2 {@code group} keyword. The group
 * wire format is still available via {@link #DELIMITED} encoding, but it is configured
 * as a field-level feature rather than a distinct type-level concept.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 * </ul>
 */
public enum ProtobufMessageEncoding {
    /**
     * Uses the default encoding for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * Standard message encoding using wire type 2 (length delimited).
     * The message is serialized with a length prefix followed by the encoded bytes.
     */
    LENGTH_PREFIXED("LENGTH_PREFIXED"),

    /**
     * Group style encoding using wire types 3 (start group) and 4 (end group).
     * The message is serialized between start and end group markers without a length prefix.
     * This is the encoding previously used by proto2 groups.
     */
    DELIMITED("DELIMITED");

    private final String token;

    ProtobufMessageEncoding(String token) {
        this.token = token;
    }

    /**
     * Returns the token string as used in .proto files.
     *
     * @return the token string
     */
    public String token() {
        return token;
    }

    private static final Map<String, ProtobufMessageEncoding> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a message encoding value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufMessageEncoding> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}