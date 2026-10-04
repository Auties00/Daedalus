package com.github.auties00.daedalus.protobuf.cli.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtoSourcesTest {

    @Nested
    class ResolveTests {
        @Test
        public void testFileIsUsedAsItIs() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("top.proto"));
            assertEquals(List.of("top.proto"), relative(base, files));
        }

        @Test
        public void testDirectoryStandsForEveryProtoFileBelowIt() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("hello"));
            assertEquals(List.of("hello/a.proto", "hello/deep/b.proto", "hello/deep/deeper/c.proto"), relative(base, files));
        }

        @Test
        public void testGlobstarMatchesEveryLevelIncludingTheFirst() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("./hello/**/*.proto"));
            assertEquals(List.of("hello/a.proto", "hello/deep/b.proto", "hello/deep/deeper/c.proto"), relative(base, files));
        }

        @Test
        public void testSingleStarDoesNotCrossDirectories() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("hello/*.proto"));
            assertEquals(List.of("hello/a.proto"), relative(base, files));
        }

        @Test
        public void testPatternWithoutDirectoryMatchesTheBaseDirectory() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("*.proto"));
            assertEquals(List.of("top.proto"), relative(base, files));
        }

        @Test
        public void testBraceAlternatives() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("{other,hello/deep}/*.proto"));
            assertEquals(List.of("hello/deep/b.proto", "other/d.proto"), relative(base, files).stream().sorted().toList());
        }

        @Test
        public void testPatternOnlyMatchesProtoFiles() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("hello/a.*"));
            assertEquals(List.of("hello/a.proto"), relative(base, files));
        }

        @Test
        public void testBackslashesSeparateDirectoriesLikeSlashes() throws IOException {
            var base = createProject();
            var backslash = String.valueOf((char) 92);
            var files = ProtoSources.resolve(base, List.of("hello" + backslash + "deep" + backslash + "*.proto"));
            assertEquals(List.of("hello/deep/b.proto"), relative(base, files));
        }

        @Test
        public void testSeveralSourcesKeepTheirOrderWithoutDuplicates() throws IOException {
            var base = createProject();
            var files = ProtoSources.resolve(base, List.of("other", "hello/a.proto", "hello/**/*.proto", "top.proto"));
            assertEquals(List.of("other/d.proto", "hello/a.proto", "hello/deep/b.proto", "hello/deep/deeper/c.proto", "top.proto"), relative(base, files));
        }

        @Test
        public void testSourceThatMatchesNothing() throws IOException {
            var base = createProject();
            var error = assertThrows(IOException.class, () -> ProtoSources.resolve(base, List.of("top.proto", "missing/**/*.proto")));
            assertTrue(error.getMessage().contains("missing/**/*.proto"));
        }
    }

    @Nested
    class ValidateTests {
        @Test
        public void testValidSources() throws IOException {
            var base = createProject();
            assertTrue(ProtoSources.validate(base, "hello/**/*.proto").isEmpty());
            assertTrue(ProtoSources.validate(base, "hello").isEmpty());
            assertTrue(ProtoSources.validate(base, "top.proto").isEmpty());
        }

        @Test
        public void testMissingFile() throws IOException {
            var base = createProject();
            assertTrue(ProtoSources.validate(base, "nothing.proto").orElseThrow().startsWith("File not found"));
        }

        @Test
        public void testDirectoryWithoutProtoFiles() throws IOException {
            var base = createProject();
            create(base, "empty/readme.txt");
            assertTrue(ProtoSources.validate(base, "empty").orElseThrow().startsWith("No .proto files found"));
        }

        @Test
        public void testPatternThatMatchesNothing() throws IOException {
            var base = createProject();
            assertTrue(ProtoSources.validate(base, "nowhere/*.proto").orElseThrow().startsWith("No .proto file matches"));
        }
    }

    private static Path createProject() throws IOException {
        var base = Files.createTempDirectory("protobuf-test");
        create(base, "hello/a.proto");
        create(base, "hello/deep/b.proto");
        create(base, "hello/deep/deeper/c.proto");
        create(base, "hello/notes.txt");
        create(base, "other/d.proto");
        create(base, "top.proto");
        return base;
    }

    private static void create(Path base, String relative) throws IOException {
        var file = base.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "syntax = \"proto3\";", StandardOpenOption.CREATE);
    }

    private static List<String> relative(Path base, List<Path> files) {
        return files.stream()
                .map(file -> base.relativize(file).toString().replace((char) 92, '/'))
                .toList();
    }
}
