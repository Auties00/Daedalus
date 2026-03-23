package it.auties.protobuf.model;

/**
 * Controls the wire encoding of nested message fields in protobuf.
 * <p>
 * In protobuf editions, this replaces the proto2 {@code group} keyword. The group
 * wire format is still available via {@link #DELIMITED} encoding, but it is configured
 * as a field-level feature rather than a distinct type-level concept.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #LENGTH_PREFIXED}</li>
 * </ul>
 */
public enum ProtobufMessageEncoding {
    /**
     * Uses the default encoding for the active protobuf version or edition.
     */
    EDITION_DEFAULT,

    /**
     * Standard message encoding using wire type 2 (length-delimited).
     * The message is serialized with a length prefix followed by the encoded bytes.
     */
    LENGTH_PREFIXED,

    /**
     * Group-style encoding using wire types 3 (start group) and 4 (end group).
     * The message is serialized between start and end group markers without a length prefix.
     * This is the encoding previously used by proto2 groups.
     */
    DELIMITED
}