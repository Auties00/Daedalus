package it.auties.protobuf.schema.update;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Java type discovered by the {@link JavaSourceScanner}.
 *
 * <p>Contains the parsed AST, the annotation metadata, and the set of
 * existing field indexes for matching against proto definitions.
 *
 * @param filePath the path to the Java source file containing this type
 * @param compilationUnit the parsed compilation unit (with lexical preservation enabled)
 * @param typeDeclaration the type declaration node in the AST
 * @param annotationType the protobuf annotation type found on this declaration
 * @param protoFqn the proto fully-qualified name from the annotation's {@code name} attribute,
 *                 or {@code null} if no explicit name was specified
 * @param javaPackage the Java package of this type, or empty string if in the default package
 * @param simpleTypeName the simple name of the Java type
 * @param existingFieldIndexes the set of {@code @ProtobufProperty} indexes already present
 * @param existingFieldsByIndex a map from field index to the field/method name in the Java source
 * @param existingEnumConstantIndexes the set of {@code @ProtobufEnum.Constant} indexes already present
 */
public record ScannedType(
        Path filePath,
        CompilationUnit compilationUnit,
        TypeDeclaration<?> typeDeclaration,
        AnnotationType annotationType,
        String protoFqn,
        String javaPackage,
        String simpleTypeName,
        Set<Long> existingFieldIndexes,
        Map<Long, String> existingFieldsByIndex,
        Set<Long> existingEnumConstantIndexes
) {

    /**
     * The kind of protobuf annotation found on the scanned type.
     */
    public enum AnnotationType {
        /**
         * The type is annotated with {@code @ProtobufMessage}.
         */
        MESSAGE,

        /**
         * The type is annotated with {@code @ProtobufEnum}.
         */
        ENUM,

        /**
         * The type is annotated with {@code @ProtobufGroup}.
         */
        GROUP
    }
}
