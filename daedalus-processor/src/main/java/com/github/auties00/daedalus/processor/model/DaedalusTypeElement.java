package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * A processed type element that has been analyzed by a {@link com.github.auties00.daedalus.processor.DaedalusProcessorExtension}.
 *
 * <p>Each processed type element represents a type annotated with a format specific annotation
 * (e.g. {@code @ProtobufMessage}) and exposes its properties for converter attribution
 * by the common processor framework.
 */
public interface DaedalusTypeElement {

    /**
     * Returns the type element that this processed type element represents.
     *
     * @return the type element
     */
    TypeElement typeElement();

    /**
     * Returns the fields
     *
     * @return the list of property types to attribute
     */
    SequencedCollection<? extends DaedalusFieldElement> fields();

    /**
     * Returns the builder elements declared on this type.
     *
     * @return a non-null collection of builder elements
     */
    SequencedCollection<? extends DaedalusBuilderElement> builders();

    /**
     * Returns whether this type supports generation of a default builder.
     *
     * <p>Format extensions control this: for example, protobuf returns {@code true}
     * for messages and groups but {@code false} for enums.
     *
     * @return {@code true} if a default builder should be generated
     */
    boolean supportsDefaultBuilder();

    /**
     * Returns the method that constructs this type if the type is constructable.
     *
     * @return an optional containing the method that constructs this type if present, or empty otherwise
     */
    Optional<? extends DaedalusMethodElement> constructor();
}
