package com.github.auties00.daedalus.protobuf.support.google;

import java.io.InputStream;
import java.util.List;

/**
 * Static accessor for the Google Protocol Buffers well-known {@code .proto} schema files
 * bundled with this module.
 *
 * <p>The schema files are distributed as resources under {@code google/protobuf/} inside this
 * module. Because that directory is a non-open package of the module, they are encapsulated and
 * cannot be loaded by code in other modules through {@link ClassLoader#getResource} or
 * {@link Class#getResourceAsStream}. This class provides an in-module accessor that bypasses that
 * encapsulation: the call to {@link Class#getResourceAsStream} originates from a class in the same
 * module as the resource, so JPMS does not require the package to be opened.
 *
 * @apiNote
 * Intended for tooling that needs to parse the Google built-in {@code .proto} files at runtime.
 * The list of names in {@link #NAMES} matches the layout of the {@code google/protobuf/}
 * resource tree.
 */
public final class WellKnownProtos {
    /**
     * The resource paths of every Google built-in {@code .proto} file shipped by this module,
     * relative to the module's resource root.
     */
    public static final List<String> NAMES = List.of(
            "google/protobuf/any.proto",
            "google/protobuf/api.proto",
            "google/protobuf/compiler/plugin.proto",
            "google/protobuf/cpp_features.proto",
            "google/protobuf/descriptor.proto",
            "google/protobuf/duration.proto",
            "google/protobuf/empty.proto",
            "google/protobuf/field_mask.proto",
            "google/protobuf/go_features.proto",
            "google/protobuf/java_features.proto",
            "google/protobuf/java_mutable_features.proto",
            "google/protobuf/source_context.proto",
            "google/protobuf/struct.proto",
            "google/protobuf/timestamp.proto",
            "google/protobuf/type.proto",
            "google/protobuf/wrappers.proto"
    );

    private WellKnownProtos() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an {@code InputStream} for the named built-in {@code .proto} resource, or
     * {@code null} if no such resource exists in this module.
     *
     * <p>The {@code name} is a resource path relative to the module's resource root, e.g.
     * {@code "google/protobuf/descriptor.proto"}. The lookup is performed against this class's own
     * module so that resources in non-open packages are still locatable.
     *
     * @param name the resource path
     * @return an input stream for the resource, or {@code null} if absent
     */
    public static InputStream open(String name) {
        return WellKnownProtos.class.getResourceAsStream("/" + name);
    }
}
