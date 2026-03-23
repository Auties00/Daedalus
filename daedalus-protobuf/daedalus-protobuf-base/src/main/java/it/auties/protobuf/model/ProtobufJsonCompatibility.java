package it.auties.protobuf.model;

/**
 * Controls whether JSON serialization/deserialization code is generated for a message.
 * <p>
 * When {@link #ENABLED}, the JSON processor (from the daedalus-json module) must be active;
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
    EDITION_DEFAULT,

    /**
     * JSON serialization/deserialization is enabled.
     * If the JSON processor is not active, the compiler will issue a warning.
     * This is the proto3 default behaviour.
     */
    ENABLED,

    /**
     * JSON serialization/deserialization is disabled.
     * This is the proto2 default behaviour.
     */
    DISABLED
}