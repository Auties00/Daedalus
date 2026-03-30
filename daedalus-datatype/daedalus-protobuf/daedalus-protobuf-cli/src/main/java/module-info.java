module com.github.auties00.daedalus.protobuf.cli {
    requires com.github.auties00.daedalus.cli;
    requires com.github.auties00.daedalus.protobuf.typesystem;
    requires com.github.auties00.daedalus.protobuf.compiler;
    requires info.picocli;
    requires com.github.javaparser.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires tui4j;
    requires java.logging;
    requires io.github.javadiffutils;

    opens com.github.auties00.daedalus.protobuf.cli.command to info.picocli;
    opens com.github.auties00.daedalus.protobuf.cli.config to com.fasterxml.jackson.databind;

    provides com.github.auties00.daedalus.cli.spi.DaedalusExtension
            with com.github.auties00.daedalus.protobuf.cli.ProtobufExtension;
}
