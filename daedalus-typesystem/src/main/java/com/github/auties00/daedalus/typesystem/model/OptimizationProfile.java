package com.github.auties00.daedalus.typesystem.model;

import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSize;

/**
 * Defines the optimization strategy for protobuf serialization and deserialization.
 * <p>
 * This can be optionally declared as a trailing parameter of methods annotated with
 * {@link TypeSerializer}, {@link TypeDeserializer} or {@link TypeSize}
 * to provide different implementations depending on the active optimization profile.
 */
public enum OptimizationProfile {
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