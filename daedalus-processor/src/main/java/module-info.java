module com.github.auties00.daedalus.processor {
    requires jdk.compiler;
    requires com.github.auties00.daedalus.typesystem;
    requires com.palantir.javapoet;

    exports com.github.auties00.daedalus.processor;
    exports com.github.auties00.daedalus.processor.model;
    exports com.github.auties00.daedalus.processor.graph;
    exports com.github.auties00.daedalus.processor.generator;
    exports com.github.auties00.daedalus.processor.manager;
    exports com.github.auties00.daedalus.processor.type;
    exports com.github.auties00.daedalus.processor.generator.builder;
    exports com.github.auties00.daedalus.processor.generator.converter;
    exports com.github.auties00.daedalus.processor.util;

    uses com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
}
