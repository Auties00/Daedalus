package com.github.auties00.daedalus.protobuf.model;

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
     * Uses the default UTF-8 validation for the active protobuf version or edition.
     */
    EDITION_DEFAULT,

    /**
     * String fields are validated as UTF-8. Invalid UTF-8 sequences cause an error.
     * This is the proto3 default behaviour.
     */
    VERIFY,

    /**
     * No UTF-8 validation is performed on string fields.
     * This is the proto2 default behaviour.
     */
    NONE
}