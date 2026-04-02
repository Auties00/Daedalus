package com.github.auties00.daedalus.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;

import java.util.List;

/**
 * A property type that supports converter management for serialization and deserialization.
 *
 * <p>Property types hold a list of converters that form a chain from the source type
 * to the wire format type (for serialization) or vice versa (for deserialization).
 * The common processor framework uses this interface to resolve unattributed converters
 * into attributed ones via the converter graph.
 */
public interface DaedalusFieldType {

    /**
     * Returns the converters associated with this property type.
     *
     * @return an unmodifiable list of converters
     */
    List<DaedalusConverterElement> converters();

    /**
     * Adds a converter to this property type.
     *
     * @param element the converter to add
     */
    void addConverter(DaedalusConverterElement element);

    /**
     * Removes all converters from this property type.
     */
    void clearConverters();
}
