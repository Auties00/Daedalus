package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls whether JSON serialization/deserialization code is generated for a message.
 * <p>
 * When {@link #ENABLED}, the JSON processor (from the daedalus json module) must be active;
 * if it is not, the compiler will issue a warning.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #DISABLED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #ENABLED}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #ENABLED}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #ENABLED}</li>
 * </ul>
 */
public enum ProtobufJsonCompatibility {
    /**
     * Uses the default JSON compatibility for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * JSON serialization/deserialization is enabled.
     * Maps to the protobuf spec value {@code ALLOW}.
     * This is the proto3 default behaviour.
     */
    ENABLED("ALLOW"),

    /**
     * JSON serialization/deserialization is disabled.
     * Maps to the protobuf spec value {@code LEGACY_BEST_EFFORT}.
     * This is the proto2 default behaviour.
     */
    DISABLED("LEGACY_BEST_EFFORT");

    private final String token;

    ProtobufJsonCompatibility(String token) {
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

    private static final Map<String, ProtobufJsonCompatibility> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a JSON compatibility value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufJsonCompatibility> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}