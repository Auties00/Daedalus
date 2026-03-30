package com.github.auties00.daedalus.protobuf.compiler.tree;

public sealed interface ProtobufOptionDefinition
        extends ProtobufTree, ProtobufTree.WithName, ProtobufTree.WithType
        permits ProtobufFieldStatement, ProtobufGroupStatement {
}
