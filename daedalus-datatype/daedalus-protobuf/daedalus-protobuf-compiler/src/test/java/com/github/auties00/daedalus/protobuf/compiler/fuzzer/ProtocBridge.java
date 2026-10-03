package com.github.auties00.daedalus.protobuf.compiler.fuzzer;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FFM bridge to the native protoc validation library.
 * <p>
 * Calls the C function {@code validate_proto(content, content_len)} which parses and
 * validates a .proto string using libprotobuf entirely in memory, without any file I/O
 * or process spawning.
 * </p>
 */
final class ProtocBridge implements AutoCloseable {
    private final Arena arena;
    private final MethodHandle validateProto;

    ProtocBridge(Path libraryPath) {
        if (!Files.exists(libraryPath)) {
            throw new IllegalArgumentException("Library not found: " + libraryPath);
        }

        this.arena = Arena.ofShared();
        var linker = Linker.nativeLinker();
        var lookup = SymbolLookup.libraryLookup(libraryPath, arena);

        var symbol = lookup.find("validate_proto")
                .orElseThrow(() -> new UnsatisfiedLinkError("Symbol validate_proto not found in " + libraryPath));

        this.validateProto = linker.downcallHandle(
                symbol,
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,   // return: 0=ok, 1=parse error
                        ValueLayout.ADDRESS,    // content (const char*)
                        ValueLayout.JAVA_INT    // content_len
                )
        );
    }

    /**
     * Validates a .proto string using the native protobuf library.
     *
     * @param protoContent the .proto file content
     * @return true if the proto is valid, false otherwise
     */
    boolean validate(String protoContent) {
        try (var localArena = Arena.ofConfined()) {
            var contentBytes = protoContent.getBytes(StandardCharsets.UTF_8);
            var contentSegment = localArena.allocateFrom(ValueLayout.JAVA_BYTE, contentBytes);
            var resultCode = (int) validateProto.invokeExact(contentSegment, contentBytes.length);
            return resultCode == 0;
        } catch (Throwable e) {
            throw new RuntimeException("FFM call failed", e);
        }
    }

    @Override
    public void close() {
        arena.close();
    }
}
