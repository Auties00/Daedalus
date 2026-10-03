module com.github.auties00.daedalus.typesystem {
    requires java.compiler;
    requires jdk.incubator.vector;
    requires jdk.unsupported;

    exports com.github.auties00.daedalus.typesystem.annotation;
    exports com.github.auties00.daedalus.typesystem.exception;
    exports com.github.auties00.daedalus.typesystem.model;
    exports com.github.auties00.daedalus.typesystem.util;
}