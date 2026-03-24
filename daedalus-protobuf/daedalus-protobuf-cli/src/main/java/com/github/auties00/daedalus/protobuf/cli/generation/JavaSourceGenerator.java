package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.auties00.daedalus.protobuf.compiler.ProtobufParser;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufDocumentTree;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufGroupStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates Java source generation from parsed proto schemas.
 *
 * <p>Parses {@code .proto} files using the protobuf parser module, then delegates
 * to {@link MessageGenerator}, {@link EnumGenerator}, and {@link GroupGenerator}
 * to produce annotated Java source files. Nested types (enums, oneofs, groups)
 * within messages are handled recursively by the {@link MessageGenerator}.
 */
public final class JavaSourceGenerator {
    private final SchemaConfig config;
    private final NamingStrategy naming;
    private final TypeMapper typeMapper;
    private final PropertyGenerator propertyGenerator;
    private final MessageGenerator messageGenerator;
    private final EnumGenerator enumGenerator;
    private final GroupGenerator groupGenerator;
    private final OneofGenerator oneofGenerator;
    private final BuilderGenerator builderGenerator;
    private final UnknownFieldsGenerator unknownFieldsGenerator;

    /**
     * Constructs a source generator with the given configuration.
     *
     * @param config the schema configuration
     */
    public JavaSourceGenerator(SchemaConfig config) {
        this.config = config;
        this.naming = new NamingStrategy();
        this.typeMapper = new TypeMapper(config, naming);
        this.propertyGenerator = new PropertyGenerator(typeMapper, naming);
        this.enumGenerator = new EnumGenerator(config, naming);
        this.oneofGenerator = new OneofGenerator(propertyGenerator, naming);
        this.groupGenerator = new GroupGenerator(config, propertyGenerator, naming);
        this.builderGenerator = new BuilderGenerator(propertyGenerator, naming);
        this.unknownFieldsGenerator = new UnknownFieldsGenerator();
        this.messageGenerator = new MessageGenerator(config, propertyGenerator, naming,
                enumGenerator, oneofGenerator, groupGenerator,
                builderGenerator, unknownFieldsGenerator);
    }

    /**
     * Parses proto files from the configured directory and generates Java source files.
     *
     * @return the list of generated file paths (relative to the output directory)
     * @throws IOException if proto files cannot be read or Java files cannot be written
     */
    public List<String> generate() throws IOException {
        var protoDir = Path.of(config.protoDir());
        if (!Files.exists(protoDir)) {
            throw new IOException("Proto directory does not exist: " + protoDir);
        }

        var documents = ProtobufParser.parse(protoDir);
        return generateFromDocuments(documents);
    }

    /**
     * Generates Java source files from pre-parsed proto documents.
     *
     * @param documents the parsed and attributed document trees
     * @return the list of generated file paths
     * @throws IOException if Java files cannot be written
     */
    public List<String> generateFromDocuments(Map<String, ProtobufDocumentTree> documents) throws IOException {
        var outputDir = Path.of(config.javaSourceDir());
        var generatedFiles = new ArrayList<String>();

        for (var entry : documents.entrySet()) {
            var document = entry.getValue();
            var protoPackage = document.packageName().orElse(null);
            var javaPackage = config.resolveJavaPackage(protoPackage);

            document.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(message -> {
                var protoFqn = buildProtoFqn(protoPackage, message.name());
                var cu = messageGenerator.generate(message, javaPackage, protoFqn);
                var typeName = naming.toTypeName(message.name());
                var filePath = writeCompilationUnit(cu, javaPackage, typeName, outputDir);
                if (filePath != null) {
                    generatedFiles.add(filePath);
                }
            });

            document.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(enumStmt -> {
                var protoFqn = buildProtoFqn(protoPackage, enumStmt.name());
                var cu = enumGenerator.generate(enumStmt, javaPackage, protoFqn);
                var typeName = naming.toTypeName(enumStmt.name());
                var filePath = writeCompilationUnit(cu, javaPackage, typeName, outputDir);
                if (filePath != null) {
                    generatedFiles.add(filePath);
                }
            });

            document.getDirectChildrenByType(ProtobufGroupStatement.class).forEach(group -> {
                var protoFqn = buildProtoFqn(protoPackage, group.name());
                var cu = groupGenerator.generate(group, javaPackage, protoFqn);
                var typeName = naming.toTypeName(group.name());
                var filePath = writeCompilationUnit(cu, javaPackage, typeName, outputDir);
                if (filePath != null) {
                    generatedFiles.add(filePath);
                }
            });
        }

        return generatedFiles;
    }

    private String writeCompilationUnit(com.github.javaparser.ast.CompilationUnit cu,
                                         String javaPackage, String typeName, Path outputDir) {
        try {
            var packagePath = javaPackage != null && !javaPackage.isEmpty()
                    ? javaPackage.replace('.', '/')
                    : "";
            var dir = packagePath.isEmpty() ? outputDir : outputDir.resolve(packagePath);
            Files.createDirectories(dir);

            var filePath = dir.resolve(typeName + ".java");
            Files.writeString(filePath, cu.toString());

            var relativePath = packagePath.isEmpty()
                    ? typeName + ".java"
                    : packagePath + "/" + typeName + ".java";
            return relativePath;
        } catch (IOException e) {
            System.err.println("Failed to write " + typeName + ".java: " + e.getMessage());
            return null;
        }
    }

    private String buildProtoFqn(String protoPackage, String typeName) {
        if (protoPackage == null || protoPackage.isEmpty()) {
            return typeName;
        }
        return protoPackage + "." + typeName;
    }
}
