package com.github.auties00.daedalus.protobuf.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controls how out-of-range enum values are handled during deserialization.
 * <p>
 * In protobuf editions, this replaces the implicit proto2/proto3 enum behaviour
 * with a unified feature flag.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #CLOSED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #OPEN}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #OPEN}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #OPEN}</li>
 * </ul>
 */
public enum ProtobufEnumType {
    /**
     * Uses the default enum type for the active protobuf version or edition.
     */
    VERSION_DEFAULT(""),

    /**
     * Open enum: unknown values are preserved during deserialization.
     * The first enum value must have index 0, as it is the default value of
     * a field with implicit presence.
     * This is the proto3 default behaviour.
     *
     * <p>A Java enum cannot hold a value that has no constant, so an unknown value
     * is assigned the default value supplied to the field, if any, and is kept in
     * the unknown fields of the message otherwise.
     */
    OPEN("OPEN"),

    /**
     * Closed enum: unknown values are not assigned to the field during
     * deserialization, and are kept in the unknown fields of the message
     * instead, if it has any. This is the proto2 default behaviour.
     */
    CLOSED("CLOSED");

    private final String token;

    ProtobufEnumType(String token) {
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

    private static final Map<String, ProtobufEnumType> VALUES = Arrays.stream(values())
            .filter(entry -> !entry.token.isEmpty())
            .collect(Collectors.toUnmodifiableMap(entry -> entry.token, Function.identity()));

    /**
     * Looks up an enum type value by its token.
     *
     * @param token the token to look up
     * @return optional containing the matching value, or empty if not found
     */
    public static Optional<ProtobufEnumType> of(String token) {
        return Optional.ofNullable(VALUES.get(token));
    }

    public ProtobufEnumType resolve(ProtobufVersion version) {
        if (this != VERSION_DEFAULT) {
            return this;
        } else {
            return switch (version) {
                case PROTOBUF_2 -> CLOSED;
                case PROTOBUF_3, EDITION_2023, EDITION_2024 -> OPEN;
            };
        }
    }
}