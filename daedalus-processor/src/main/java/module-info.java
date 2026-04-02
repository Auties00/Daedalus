module com.github.auties00.daedalus.processor {
    requires jdk.compiler;
    requires com.github.auties00.daedalus.typesystem;
    requires com.palantir.javapoet;

    exports com.github.auties00.daedalus.processor;
    exports com.github.auties00.daedalus.processor.model;
    exports com.github.auties00.daedalus.processor.graph;
    exports com.github.auties00.daedalus.processor.generator;
    exports com.github.auties00.daedalus.processor.support;
    exports com.github.auties00.daedalus.processor.manager;
    exports com.github.auties00.daedalus.processor.type;

    uses com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
}
