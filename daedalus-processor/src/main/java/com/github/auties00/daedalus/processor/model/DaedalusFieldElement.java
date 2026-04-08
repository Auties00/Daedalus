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

    /**
     * Returns the name of this field.
     *
     * @return a non-null field name
     */
    String name();

    /**
     * Returns whether this field should be included in the default builder.
     *
     * <p>Format extensions control this: for example, protobuf excludes
     * synthetic fields (those not present in the constructor).
     *
     * @return {@code true} if included in the default builder
     */
    boolean includedInDefaultBuilder();
}
