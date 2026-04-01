open module com.github.auties00.daedalus.protobuf.compiler {
    requires com.github.auties00.daedalus.protobuf.typesystem;
    requires com.github.auties00.daedalus.protobuf.google;
    
    exports com.github.auties00.daedalus.protobuf.compiler.typeReference;
    exports com.github.auties00.daedalus.protobuf.compiler.tree;
    exports com.github.auties00.daedalus.protobuf.compiler.exception;
    exports com.github.auties00.daedalus.protobuf.compiler.token;
    exports com.github.auties00.daedalus.protobuf.compiler.expression;
    exports com.github.auties00.daedalus.protobuf.compiler.number;
    exports com.github.auties00.daedalus.protobuf.compiler;
}