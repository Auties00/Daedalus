package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusBuilderParameterElement;

import javax.lang.model.element.VariableElement;

public record ProtobufBuilderParameterElement(
        VariableElement element
) implements DaedalusBuilderParameterElement {

}
