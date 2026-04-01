package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls field presence tracking for protobuf fields.
 * <p>
 * In protobuf editions, this replaces the {@code required} and {@code optional} keywords
 * from proto2/proto3 with a unified feature flag.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #EXPLICIT}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #IMPLICIT}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #EXPLICIT}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #EXPLICIT}</li>
 * </ul>
 */
public enum ProtobufFieldPresence {
    /**
     * Uses the default field presence for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * The field has explicit presence: its value is tracked and can be distinguished
     * from the default value. This is the proto2 default behaviour.
     */
    EXPLICIT("EXPLICIT"),

    /**
     * The field has implicit presence: a field set to its default value is considered
     * unset and is not serialized. This is the proto3 default behaviour.
     */
    IMPLICIT("IMPLICIT"),

    /**
     * The field is required: the decoder will report an error if this field is missing.
     * This replaces the proto2 {@code required} keyword.
     */
    LEGACY_REQUIRED("LEGACY_REQUIRED");

    private final String token;

    ProtobufFieldPresence(String token) {
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

    private static final Map<String, ProtobufFieldPresence> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a field presence value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufFieldPresence> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}