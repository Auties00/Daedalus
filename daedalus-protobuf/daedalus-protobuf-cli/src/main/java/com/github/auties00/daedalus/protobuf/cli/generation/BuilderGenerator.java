package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;

import java.util.List;

/**
 * Generates {@code @ProtobufBuilder}-annotated constructors for proto message types.
 *
 * <p>When builder generation is enabled, this class annotates the primary constructor
 * (for class-based types) or adds a compact constructor (for records) with the
 * {@code @ProtobufBuilder} annotation and marks each parameter with
 * {@code @ProtobufBuilder.PropertyParameter(index = N)}.
 */
public final class BuilderGenerator {
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;

    /**
     * Constructs a builder generator with the given property generator and naming strategy.
     *
     * @param propertyGenerator the property generator for resolving Java types and names
     * @param naming the naming strategy
     */
    public BuilderGenerator(PropertyGenerator propertyGenerator, NamingStrategy naming) {
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
    }

    /**
     * Adds a {@code @ProtobufBuilder}-annotated constructor to a class declaration.
     *
     * <p>The constructor parameters are annotated with
     * {@code @ProtobufBuilder.PropertyParameter(index = N)} to map each parameter
     * to its corresponding protobuf property index.
     *
     * @param classDecl the class declaration to add the builder constructor to
     * @param fields the proto field statements
     * @param cu the compilation unit (for adding imports)
     */
    public void addBuilderToClass(ClassOrInterfaceDeclaration classDecl,
                                   List<ProtobufFieldStatement> fields,
                                   CompilationUnit cu) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufBuilder");

        var existingConstructors = classDecl.getConstructors();
        if (!existingConstructors.isEmpty()) {
            var constructor = existingConstructors.getFirst();
            constructor.addAnnotation(new MarkerAnnotationExpr("ProtobufBuilder"));

            for (var param : constructor.getParameters()) {
                var matchingField = findFieldByName(fields, param.getNameAsString());
                if (matchingField != null) {
                    var index = matchingField.index().value().longValueExact();
                    param.addAnnotation(new NormalAnnotationExpr(
                            new Name("ProtobufBuilder.PropertyParameter"),
                            new NodeList<>(new MemberValuePair("index",
                                    new LongLiteralExpr(String.valueOf(index))))
                    ));
                }
            }
        }
    }

    /**
     * Adds a {@code @ProtobufBuilder}-annotated compact constructor to a record declaration.
     *
     * <p>Creates an explicit compact constructor with the {@code @ProtobufBuilder}
     * annotation and marks each parameter with
     * {@code @ProtobufBuilder.PropertyParameter(index = N)}.
     *
     * @param recordDecl the record declaration to add the builder constructor to
     * @param fields the proto field statements
     * @param cu the compilation unit (for adding imports)
     */
    public void addBuilderToRecord(RecordDeclaration recordDecl,
                                    List<ProtobufFieldStatement> fields,
                                    CompilationUnit cu) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufBuilder");

        var params = new NodeList<com.github.javaparser.ast.body.Parameter>();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            var index = field.index().value().longValueExact();

            var param = new com.github.javaparser.ast.body.Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    fieldName
            );
            param.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufBuilder.PropertyParameter"),
                    new NodeList<>(new MemberValuePair("index",
                            new LongLiteralExpr(String.valueOf(index))))
            ));
            params.add(param);
        }

        var constructor = new ConstructorDeclaration();
        constructor.setName(recordDecl.getNameAsString());
        constructor.setModifiers(Modifier.Keyword.PUBLIC);
        constructor.setParameters(params);
        constructor.addAnnotation(new MarkerAnnotationExpr("ProtobufBuilder"));

        var body = new BlockStmt();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            body.addStatement(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), fieldName),
                    new NameExpr(fieldName),
                    AssignExpr.Operator.ASSIGN
            ));
        }
        constructor.setBody(body);

        recordDecl.addMember(constructor);
    }

    private ProtobufFieldStatement findFieldByName(List<ProtobufFieldStatement> fields, String javaName) {
        for (var field : fields) {
            if (propertyGenerator.resolveFieldName(field).equals(javaName)) {
                return field;
            }
        }
        return null;
    }
}
