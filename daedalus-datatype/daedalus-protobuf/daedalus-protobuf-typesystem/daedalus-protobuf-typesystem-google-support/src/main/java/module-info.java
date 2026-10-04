module com.github.auties00.daedalus.protobuf.google {
    requires com.github.auties00.daedalus.protobuf.typesystem;
    requires com.github.auties00.daedalus.typesystem;

    exports com.google.protobuf;
    exports com.google.protobuf.compiler;
    exports com.github.auties00.daedalus.protobuf.support.google;
}