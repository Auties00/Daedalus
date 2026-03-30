package com.github.auties00.daedalus.protobuf.processor.graph;

import com.github.auties00.daedalus.protobuf.processor.model.ProtobufConverterMethod;

import javax.lang.model.type.TypeMirror;

public record ProtobufConverterArc(
        ProtobufConverterMethod method,
        TypeMirror returnType
) {

}
