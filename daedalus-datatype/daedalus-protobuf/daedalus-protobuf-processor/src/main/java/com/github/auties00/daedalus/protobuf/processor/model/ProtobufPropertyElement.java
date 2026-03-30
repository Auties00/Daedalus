package com.github.auties00.daedalus.protobuf.processor.model;

import javax.lang.model.element.Element;

public record ProtobufPropertyElement(
        long index,
        String name,
        Element accessor,
        ProtobufPropertyType type,
        boolean required,
        boolean packed,
        boolean synthetic
) {

}
