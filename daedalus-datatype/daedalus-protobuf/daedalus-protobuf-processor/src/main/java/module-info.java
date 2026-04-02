module com.github.auties00.daedalus.protobuf.processor {
    requires jdk.compiler;
    requires com.github.auties00.daedalus.processor;
    requires com.github.auties00.daedalus.protobuf.typesystem;
    requires com.palantir.javapoet;
    requires com.github.auties00.daedalus.typesystem;

    provides com.github.auties00.daedalus.processor.DaedalusProcessorExtension
            with com.github.auties00.daedalus.protobuf.processor.ProtobufProcessorExtension;
}