package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.TypeElement;
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
     * Returns the property types that require converter attribution.
     *
     * <p>For composite property types (e.g. maps with separate key and value types),
     * this method should return each leaf type individually.
     *
     * @return the list of property types to attribute
     */
    SequencedCollection<? extends DaedalusFieldElement> properties();
}
