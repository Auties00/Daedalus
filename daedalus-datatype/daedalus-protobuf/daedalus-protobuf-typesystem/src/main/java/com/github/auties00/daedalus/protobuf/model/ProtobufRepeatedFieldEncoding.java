package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls the encoding of repeated fields of scalar numeric types in protobuf.
 * <p>
 * In protobuf editions, this replaces the {@code packed} option from proto2/proto3
 * with a unified feature flag.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #EXPANDED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #PACKED}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #PACKED}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #PACKED}</li>
 * </ul>
 */
public enum ProtobufRepeatedFieldEncoding {
    /**
     * Uses the default encoding for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * Packed encoding: repeated scalar values are serialized as a single length delimited
     * record, omitting field tags for each element. More compact on the wire.
     */
    PACKED("PACKED"),

    /**
     * Expanded encoding: each repeated scalar value is serialized with its own field tag.
     * This is the proto2 default behaviour.
     */
    EXPANDED("EXPANDED");

    private final String token;

    ProtobufRepeatedFieldEncoding(String token) {
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

    private static final Map<String, ProtobufRepeatedFieldEncoding> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a repeated field encoding value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufRepeatedFieldEncoding> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}