package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusBuilderParameterElement;

import javax.lang.model.element.VariableElement;

/**
 * A parameter of a protobuf builder method.
 *
 * <p>Parameters annotated with {@code @ProtobufBuilderParameter.Field} carry the
 * field index they map to, which is used to resolve the field name for
 * setter generation. Parameters annotated with {@code @ProtobufBuilderParameter.Synthetic}
 * have no field index.
 */
public sealed interface ProtobufBuilderParameterElement extends DaedalusBuilderParameterElement {

    /**
     * Returns the variable element backing this parameter.
     *
     * @return the non-null variable element
     */
    VariableElement element();

    /**
     * A builder parameter that maps to a protobuf field by index.
     *
     * @param element the parameter variable element
     * @param fieldIndex the protobuf field index
     */
    record Field(
            VariableElement element,
            int fieldIndex
    ) implements ProtobufBuilderParameterElement {

    }

    /**
     * A synthetic builder parameter with no protobuf field mapping.
     *
     * @param element the parameter variable element
     */
    record Synthetic(
            VariableElement element
    ) implements ProtobufBuilderParameterElement {

    }
}
