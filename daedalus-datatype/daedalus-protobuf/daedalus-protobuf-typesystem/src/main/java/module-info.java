module com.github.auties00.daedalus.protobuf.typesystem {
    requires java.compiler;
    requires jdk.incubator.vector;
    requires com.github.auties00.daedalus.typesystem;

    exports com.github.auties00.daedalus.protobuf;
    exports com.github.auties00.daedalus.protobuf.adapter;
    exports com.github.auties00.daedalus.protobuf.annotation;
    exports com.github.auties00.daedalus.protobuf.exception;
    exports com.github.auties00.daedalus.protobuf.io;
    exports com.github.auties00.daedalus.protobuf.io.reader;
    exports com.github.auties00.daedalus.protobuf.io.writer;
    exports com.github.auties00.daedalus.protobuf.io.calculator;
    exports com.github.auties00.daedalus.protobuf.model;
}