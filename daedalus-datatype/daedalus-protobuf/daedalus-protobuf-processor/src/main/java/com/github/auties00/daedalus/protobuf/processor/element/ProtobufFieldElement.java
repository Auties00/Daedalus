package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;

import javax.lang.model.element.Element;

public record ProtobufFieldElement(
        long index,
        String name,
        Element accessor,
        boolean required,
        boolean packed,
        boolean synthetic,
        ProtobufFieldType type
) implements DaedalusFieldElement {

}

