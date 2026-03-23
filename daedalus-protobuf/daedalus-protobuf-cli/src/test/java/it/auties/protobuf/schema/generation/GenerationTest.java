package it.auties.protobuf.schema.generation;

import it.auties.protobuf.schema.config.SchemaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenerationTest {
    public static void main(String[] args) throws IOException {
        var config = new SchemaConfig();
        config.setProtoDir("src/test/proto");

        var tempDir = Files.createTempDirectory("protobuf-gen-test");
        config.setJavaSourceDir(tempDir.toString());

        System.out.println("Output directory: " + tempDir);

        var generator = new JavaSourceGenerator(config);
        var files = generator.generate();

        System.out.println("Generated " + files.size() + " files:");
        for (var file : files) {
            System.out.println("  " + file);
            var fullPath = tempDir.resolve(file);
            System.out.println("--- " + file + " ---");
            System.out.println(Files.readString(fullPath));
            System.out.println();
        }
    }
}
