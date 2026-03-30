package com.github.auties00.daedalus.protobuf.processor.graph;

import com.github.auties00.daedalus.protobuf.processor.model.ProtobufConverterMethod;

import javax.lang.model.type.TypeMirror;

// A node in the graph
record ProtobufConverterNode(
        TypeMirror from,
        TypeMirror to,
        ProtobufConverterMethod arc
) {

}
