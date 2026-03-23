package it.auties.protobuf.model;

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
    EDITION_DEFAULT,

    /**
     * The field has explicit presence: its value is tracked and can be distinguished
     * from the default value. This is the proto2 default behaviour.
     */
    EXPLICIT,

    /**
     * The field has implicit presence: a field set to its default value is considered
     * unset and is not serialized. This is the proto3 default behaviour.
     */
    IMPLICIT,

    /**
     * The field is required: the decoder will report an error if this field is missing.
     * This replaces the proto2 {@code required} keyword.
     */
    LEGACY_REQUIRED
}