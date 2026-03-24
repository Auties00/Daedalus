package com.github.auties00.daedalus.protobuf.model;

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
    EDITION_DEFAULT,

    /**
     * Open enum: unknown values are preserved during deserialization.
     * The first enum value must have index 0.
     * This is the proto3 default behaviour.
     */
    OPEN,

    /**
     * Closed enum: unknown values are rejected during deserialization and
     * treated as unknown fields. This is the proto2 default behaviour.
     */
    CLOSED
}