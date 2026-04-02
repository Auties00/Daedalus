package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.ExecutableElement;
import java.util.SequencedCollection;

/**
 * A builder element that represents a named factory or builder method for
 * constructing instances of a processed type.
 */
public interface DaedalusBuilderElement {
    /**
     * Returns the name of the builder class.
     *
     * @return a non-null name
     */
    String name();

    /**
     * Returns the list of parameters of this builder.
     *
     * @return a non-null list of parameters
     */
    SequencedCollection<? extends DaedalusBuilderParameterElement> parameters();

    /**
     * Returns the element of the method that this builder delegates to.
     *
     * @return a non-null element
     */
    ExecutableElement delegate();
}
