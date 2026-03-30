package com.github.auties00.daedalus.protobuf.model;

/**
 * Controls the encoding of repeated fields of scalar numeric types in protobuf.
 * <p>
 * In protobuf editions, this replaces the {@code packed} option from proto2/proto3
 * with a unified feature flag.
 *
 * <ul>
 *     <li><strong>proto2:</strong> defaults to {@link #EXPANDED}</li>
 *     <li><strong>proto3:</strong> defaults to {@link #PACKED}</li>
 *     <li><strong>edition 2023:</strong> defaults to {@link #PACKED}</li>
 *     <li><strong>edition 2024:</strong> defaults to {@link #PACKED}</li>
 * </ul>
 */
public enum ProtobufRepeatedFieldEncoding {
    /**
     * Uses the default encoding for the active protobuf version or edition.
     */
    EDITION_DEFAULT,

    /**
     * Packed encoding: repeated scalar values are serialized as a single length-delimited
     * record, omitting field tags for each element. More compact on the wire.
     */
    PACKED,

    /**
     * Expanded encoding: each repeated scalar value is serialized with its own field tag.
     * This is the proto2 default behaviour.
     */
    EXPANDED
}