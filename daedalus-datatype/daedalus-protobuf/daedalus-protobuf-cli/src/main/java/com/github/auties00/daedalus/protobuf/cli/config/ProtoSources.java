package com.github.auties00.daedalus.protobuf.cli.config;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Resolves the {@code .proto} files that a configuration refers to.
 *
 * <p>A source is written as one of the following:
 * <ul>
 * <li>the path of a file, which is used as it is;
 * <li>the path of a directory, which stands for every {@code .proto} file below it;
 * <li>a glob pattern, such as {@code ./hello/**}{@code /*.proto}, which stands for every
 *     {@code .proto} file that matches it. As in a shell with globstar enabled, {@code **}{@code /}
 *     also matches no directory at all, so the pattern above includes {@code ./hello/a.proto}.
 * </ul>
 *
 * <p>Relative paths are resolved against a base directory, and both kinds of path separators
 * are accepted.
 */
public final class ProtoSources {
    private static final String PROTO_EXTENSION = ".proto";
    private static final String GLOB_CHARACTERS = "*?[{";
    private static final String GLOBSTAR = "**/";

    private ProtoSources() {
    }

    /**
     * Resolves the files of all the given sources.
     *
     * @param base the directory that relative paths are resolved against
     * @param sources the files, directories and patterns to resolve
     * @return the distinct {@code .proto} files, in the order of the sources
     * @throws IOException if a source matches no file, or if a directory cannot be read
     */
    public static List<Path> resolve(Path base, Collection<String> sources) throws IOException {
        var files = new LinkedHashMap<Path, Path>();
        for (var source : sources) {
            var matches = expand(base, source);
            if (matches.isEmpty()) {
                throw new IOException("No .proto file matches \"" + source + "\"");
            }

            for (var match : matches) {
                files.putIfAbsent(match.toAbsolutePath().normalize(), match);
            }
        }
        return List.copyOf(files.values());
    }

    /**
     * Checks that a source matches at least one file.
     *
     * @param base the directory that relative paths are resolved against
     * @param source the file, directory or pattern to check
     * @return the reason why the source is not valid, or empty if it is valid
     */
    public static Optional<String> validate(Path base, String source) {
        try {
            if (!expand(base, source).isEmpty()) {
                return Optional.empty();
            }
        } catch (IOException exception) {
            return Optional.of("Cannot read " + source + ": " + exception.getMessage());
        }

        if (isPattern(source)) {
            return Optional.of("No .proto file matches " + source);
        }

        return Optional.of(Files.isDirectory(resolvePath(base, source))
                ? "No .proto files found in " + source
                : "File not found: " + source);
    }

    private static List<Path> expand(Path base, String source) throws IOException {
        var normalized = source.strip().replace('\\', '/');
        if (!isPattern(normalized)) {
            var path = resolvePath(base, normalized);
            if (Files.isDirectory(path)) {
                return protoFilesBelow(path);
            }
            return Files.isRegularFile(path) ? List.of(path) : List.of();
        }

        var segments = normalized.split("/");
        var firstPattern = 0;
        while (!isPattern(segments[firstPattern])) {
            firstPattern++;
        }

        var root = resolvePath(base, String.join("/", List.of(segments).subList(0, firstPattern)));
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        var pattern = String.join("/", List.of(segments).subList(firstPattern, segments.length));
        var matchers = new ArrayList<PathMatcher>();
        for (var variant : globstarVariants(pattern)) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + variant));
        }

        try (var walk = Files.walk(root)) {
            return sorted(walk
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(PROTO_EXTENSION))
                    .filter(file -> matchers.stream().anyMatch(matcher -> matcher.matches(root.relativize(file)))));
        }
    }

    private static List<Path> protoFilesBelow(Path directory) throws IOException {
        try (var walk = Files.walk(directory)) {
            return sorted(walk
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(PROTO_EXTENSION)));
        }
    }

    private static List<Path> sorted(Stream<Path> files) {
        return files.sorted().toList();
    }

    private static Path resolvePath(Path base, String path) {
        if (path.isEmpty()) {
            return base;
        }

        try {
            return base.resolve(path);
        } catch (InvalidPathException exception) {
            return base.resolve("<invalid>");
        }
    }

    private static boolean isPattern(String source) {
        for (var character : GLOB_CHARACTERS.toCharArray()) {
            if (source.indexOf(character) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the pattern together with the patterns that match the same files when some of its
     * {@code **}{@code /} stand for no directory, because the glob syntax of the JDK requires
     * a separator after every {@code **}.
     *
     * @param pattern the pattern to expand
     * @return the pattern and its variants
     */
    private static Set<String> globstarVariants(String pattern) {
        var index = pattern.indexOf(GLOBSTAR);
        if (index < 0) {
            return Set.of(pattern);
        }

        var head = pattern.substring(0, index);
        var variants = new LinkedHashSet<String>();
        for (var rest : globstarVariants(pattern.substring(index + GLOBSTAR.length()))) {
            variants.add(head + GLOBSTAR + rest);
            variants.add(head + rest);
        }
        return variants;
    }
}
