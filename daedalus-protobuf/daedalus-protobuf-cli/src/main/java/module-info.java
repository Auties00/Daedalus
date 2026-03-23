module it.auties.protobuf.schema {
    requires it.auties.protobuf.base;
    requires it.auties.protobuf.parser;
    requires info.picocli;
    requires com.github.javaparser.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires tui4j;
    requires java.logging;
    requires io.github.javadiffutils;

    opens it.auties.protobuf.schema.cli to info.picocli;
    opens it.auties.protobuf.schema.config to com.fasterxml.jackson.databind;
}
