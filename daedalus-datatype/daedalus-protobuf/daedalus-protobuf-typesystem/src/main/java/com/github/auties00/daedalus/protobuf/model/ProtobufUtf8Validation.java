package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls whether string fields are validated as UTF-8 during serialization and deserialization.
 * <p>
 * In protobuf editions, this replaces the implicit proto2/proto3 string validation behaviour
 * with a unified feature flag.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #NONE}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #VERIFY}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #VERIFY}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #VERIFY}</li>
 * </ul>
 */
public enum ProtobufUtf8Validation {
    /**
     * Uses the default UTF8 validation for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * String fields are validated as UTF8. Invalid UTF8 sequences cause an error.
     * This is the proto3 default behaviour.
     */
    VERIFY("VERIFY"),

    /**
     * No UTF8 validation is performed on string fields.
     * This is the proto2 default behaviour.
     */
    NONE("NONE");

    private final String token;

    ProtobufUtf8Validation(String token) {
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

    private static final Map<String, ProtobufUtf8Validation> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up a UTF8 validation value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufUtf8Validation> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }
}