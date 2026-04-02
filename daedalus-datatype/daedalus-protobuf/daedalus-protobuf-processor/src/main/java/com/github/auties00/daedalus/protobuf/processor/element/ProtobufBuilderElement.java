package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusBuilderElement;

import javax.lang.model.element.ExecutableElement;
import java.util.SequencedCollection;

public record ProtobufBuilderElement(
        String name,
        SequencedCollection<ProtobufBuilderParameterElement> parameters,
        ExecutableElement delegate
) implements DaedalusBuilderElement {

}
