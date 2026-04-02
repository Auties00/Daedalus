package com.github.auties00.daedalus.processor.model;

import com.github.auties00.daedalus.processor.type.DaedalusFieldType;

/**
 * A field element that represents a property of a type.
 */
public interface DaedalusFieldElement {
    /**
     * Returns the type of this field.
     *
     * @return the non-null field type
     */
    DaedalusFieldType type();
}
