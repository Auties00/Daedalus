package it.auties.protobuf.schema.update;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans a Java source directory for types annotated with protobuf annotations.
 *
 * <p>Walks the directory tree recursively, parsing each {@code .java} file
 * with JavaParser and enabling {@link LexicalPreservingPrinter} for
 * format-preserving modifications. For each type declaration found with
 * a {@code @ProtobufMessage}, {@code @ProtobufEnum}, or {@code @ProtobufGroup}
 * annotation, a {@link ScannedType} is created with the extracted metadata.
 */
public final class JavaSourceScanner {
    private final AnnotationExtractor extractor;

    /**
     * Constructs a scanner with the given annotation extractor.
     *
     * @param extractor the annotation extractor for reading annotation values
     */
    public JavaSourceScanner(AnnotationExtractor extractor) {
        this.extractor = extractor;
    }

    /**
     * Scans the given source directory for protobuf-annotated Java types.
     *
     * @param sourceDir the root directory to scan
     * @return the list of scanned types found
     * @throws IOException if files cannot be read or the directory does not exist
     */
    public List<ScannedType> scan(Path sourceDir) throws IOException {
        if (!Files.exists(sourceDir)) {
            throw new IOException("Source directory does not exist: " + sourceDir);
        }

        var results = new ArrayList<ScannedType>();
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    try {
                        scanFile(file, results);
                    } catch (Exception e) {
                        // Skip files that cannot be parsed
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return results;
    }

    private void scanFile(Path file, List<ScannedType> results) throws IOException {
        var cu = StaticJavaParser.parse(file);
        LexicalPreservingPrinter.setup(cu);

        var javaPackage = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        for (var type : cu.getTypes()) {
            scanType(file, cu, type, javaPackage, results);
        }
    }

    private void scanType(Path file, com.github.javaparser.ast.CompilationUnit cu,
                           TypeDeclaration<?> typeDecl, String javaPackage,
                           List<ScannedType> results) {
        var annotationType = extractor.findAnnotationType(typeDecl);
        if (annotationType != null) {
            var protoFqn = extractor.extractProtoName(typeDecl, annotationType);
            var fieldIndexes = extractor.extractFieldIndexes(typeDecl);
            var fieldsByIndex = extractor.extractFieldsByIndex(typeDecl);
            var enumConstantIndexes = extractor.extractEnumConstantIndexes(typeDecl);

            results.add(new ScannedType(
                    file,
                    cu,
                    typeDecl,
                    annotationType,
                    protoFqn,
                    javaPackage,
                    typeDecl.getNameAsString(),
                    fieldIndexes,
                    fieldsByIndex,
                    enumConstantIndexes
            ));
        }

        // Scan nested types
        for (var member : typeDecl.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                scanType(file, cu, nested, javaPackage, results);
            }
        }
    }
}
