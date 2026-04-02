package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.VariableElement;

/**
 * Represents a parameter of a builder method.
 */
public interface DaedalusBuilderParameterElement {
    /**
     * Returns the element of this parameter.
     *
     * @return the non-null element
     */
    VariableElement element();
}
