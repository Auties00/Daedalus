package it.auties.protobuf.model;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.annotation.ProtobufSize;

/**
 * Defines the optimization strategy for protobuf serialization and deserialization.
 * <p>
 * This can be optionally declared as a trailing parameter of methods annotated with
 * {@link ProtobufSerializer}, {@link ProtobufDeserializer} or {@link ProtobufSize}
 * to provide different implementations depending on the active optimization profile.
 */
public enum ProtobufOptimizationProfile {
    /**
     * A balanced optimization strategy that provides a trade-off between
     * runtime performance and memory efficiency.
     */
    BALANCED,

    /**
     * Optimizes for maximum runtime performance, potentially at the cost
     * of higher memory usage.
     */
    RUNTIME_PERFORMANCE,

    /**
     * Optimizes for minimal memory usage, potentially at the cost
     * of slower runtime performance.
     */
    MEMORY_EFFICIENCY
}