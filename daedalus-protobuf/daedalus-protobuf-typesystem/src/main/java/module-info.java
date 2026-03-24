module com.github.auties00.daedalus.protobuf.typesystem {
    requires java.compiler;
    requires jdk.incubator.vector;

    exports com.github.auties00.daedalus.protobuf.exception;
    exports com.github.auties00.daedalus.protobuf.annotation;
    exports com.github.auties00.daedalus.protobuf.io;
    exports com.github.auties00.daedalus.protobuf.model;
    exports com.github.auties00.daedalus.protobuf.builtin;
    exports com.github.auties00.daedalus.protobuf.io.reader;
    exports com.github.auties00.daedalus.protobuf.io.writer;
    exports com.github.auties00.daedalus.protobuf.io.calculator;
}