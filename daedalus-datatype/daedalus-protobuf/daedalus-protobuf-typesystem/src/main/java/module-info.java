module com.github.auties00.daedalus.protobuf.typesystem {
    requires java.compiler;
    requires jdk.incubator.vector;
    requires com.github.auties00.daedalus.typesystem;
    // The global mixins of the JDK types are read by every module that uses protobuf, as the generated code invokes them
    requires transitive com.github.auties00.daedalus.typesystem.jdk.support;

    exports com.github.auties00.daedalus.protobuf.annotation;
    exports com.github.auties00.daedalus.protobuf.exception;
    exports com.github.auties00.daedalus.protobuf.io.reader;
    exports com.github.auties00.daedalus.protobuf.io.writer;
    exports com.github.auties00.daedalus.protobuf.io.size;
    exports com.github.auties00.daedalus.protobuf.model;
    exports com.github.auties00.daedalus.protobuf.spec;
}