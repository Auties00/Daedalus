package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls whether naming conventions are enforced on protobuf descriptors.
 * <p>
 * In Edition 2024, the default is {@link #STYLE2024}, which enforces:
 * <ul>
 *     <li>TitleCase for messages, enums, services, and methods</li>
 *     <li>lower_snake_case for fields, oneofs, and packages</li>
 *     <li>UPPER_SNAKE_CASE for enum values</li>
 * </ul>
 * <p>
 * This is a source only feature (RETENTION_SOURCE) and is stripped from binary output.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #STYLE_LEGACY}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #STYLE_LEGACY}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #STYLE_LEGACY}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #STYLE2024}</li>
 * </ul>
 */
public enum ProtobufNamingStyle {
    /**
     * Uses the default naming style for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * Enforces strict naming conventions introduced in Edition 2024.
     */
    STYLE2024("STYLE2024"),

    /**
     * No naming convention enforcement.
     */
    STYLE_LEGACY("STYLE_LEGACY");

    private final String token;

    ProtobufNamingStyle(String token) {
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

    private static final Map<String, ProtobufNamingStyle> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a naming style value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufNamingStyle> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}