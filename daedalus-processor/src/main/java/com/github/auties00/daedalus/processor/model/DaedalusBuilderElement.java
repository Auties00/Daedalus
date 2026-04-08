package com.github.auties00.daedalus.processor.model;

import com.github.auties00.daedalus.processor.util.DaedalusBuilderNameUtils;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;

import javax.lang.model.element.ExecutableElement;
import java.util.SequencedCollection;

/**
 * A builder element that represents a named factory or builder method for
 * constructing instances of a processed type.
 *
 * <p>Each builder element carries the full set of attributes from the
 * {@code @TypeBuilder} annotation, including class modifiers, setter name
 * segments, and the terminal build method name.
 */
public interface DaedalusBuilderElement {
    /**
     * Returns the name of the builder class.
     *
     * @return a non-null name
     */
    String name();

    /**
     * Returns the modifiers for the generated builder class.
     *
     * @return a non-null array of class modifiers
     */
    ClassModifier[] modifiers();

    /**
     * Returns the name of the terminal build method.
     *
     * <p>An empty string indicates the default name {@code "build"} should be used.
     *
     * @return the build method name, or an empty string for the default
     */
    String buildMethodName();

    /**
     * Returns the name segments used to construct setter method names.
     *
     * <p>Segments may contain placeholders such as {@code $FIELD_NAME},
     * {@code $CLASS_NAME}, and {@code $FIELD_TYPE_NAME} that are resolved
     * by {@link DaedalusBuilderNameUtils}.
     * An empty array indicates the default naming convention should be used.
     *
     * @return a non-null array of name segments
     */
    String[] settersMethodNameSegments();

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
