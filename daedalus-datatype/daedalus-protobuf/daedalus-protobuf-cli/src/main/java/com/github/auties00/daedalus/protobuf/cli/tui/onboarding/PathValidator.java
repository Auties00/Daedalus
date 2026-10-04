package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.protobuf.cli.config.ProtoSources;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Validates the directories entered in the onboarding wizard, so that a wrong path is
 * reported as soon as it is confirmed instead of when the generation starts.
 *
 * <p>Relative paths are resolved against the working directory, which is the same
 * directory that the generator resolves them against.
 */
final class PathValidator {
    private PathValidator() {
    }

    /**
     * Validates the sources of the {@code .proto} files.
     *
     * <p>Every source, whether it is a file, a directory or a pattern, has to match at least
     * one {@code .proto} file, as the generator would otherwise have nothing to generate from it.
     *
     * @param workingDir the directory that relative paths are resolved against
     * @param sources the entered sources, which are not blank
     * @return the reason why a source is not valid, or empty if all of them are valid
     */
    static Optional<String> protoSources(Path workingDir, List<String> sources) {
        if (sources.isEmpty()) {
            return Optional.of("Enter at least one .proto file, directory or pattern");
        }

        for (var source : sources) {
            var error = ProtoSources.validate(workingDir, source);
            if (error.isPresent()) {
                return error;
            }
        }

        return Optional.empty();
    }

    /**
     * Validates the directory that the generated Java files are written to.
     *
     * <p>The directory does not have to exist, because the generator creates it, but the
     * closest directory that does exist has to be writable.
     *
     * @param workingDir the directory that relative paths are resolved against
     * @param value the entered path
     * @return the reason why the path is not valid, or empty if it is valid
     */
    static Optional<String> outputDirectory(Path workingDir, String value) {
        var directory = resolve(workingDir, value);
        if (directory.isEmpty()) {
            return Optional.of("\"" + value + "\" is not a valid path");
        }

        var existing = directory.get();
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }

        if (existing == null) {
            return Optional.of("No part of " + directory.get() + " exists");
        }

        if (!Files.isDirectory(existing)) {
            return Optional.of("Not a directory: " + existing);
        }

        if (!Files.isWritable(existing)) {
            return Optional.of("Not writable: " + existing);
        }

        return Optional.empty();
    }

    private static Optional<Path> resolve(Path workingDir, String value) {
        try {
            return Optional.of(workingDir.resolve(value.strip()).normalize());
        } catch (InvalidPathException exception) {
            return Optional.empty();
        }
    }
}
