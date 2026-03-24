package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/**
 * Generates {@code @ProtobufUnknownFields} field declarations and setter methods
 * for proto message types.
 *
 * <p>When unknown fields support is enabled, this class adds a
 * {@code Map<Long, ProtobufUnknownValue>} field annotated with
 * {@code @ProtobufUnknownFields} and a setter method annotated with
 * {@code @ProtobufUnknownFields.Setter}.
 */
public final class UnknownFieldsGenerator {
    /**
     * Constructs an unknown fields generator.
     */
    public UnknownFieldsGenerator() {
    }

    /**
     * Adds an {@code @ProtobufUnknownFields} field and a corresponding setter
     * to a class declaration.
     *
     * <p>The generated field is:
     * <pre>{@code
     * @ProtobufUnknownFields
     * private Map<Long, ProtobufUnknownValue> unknownFields;
     * }</pre>
     *
     * <p>The generated setter is:
     * <pre>{@code
     * @ProtobufUnknownFields.Setter
     * public void setUnknownField(long index, ProtobufUnknownValue value) {
     *     if (this.unknownFields == null) {
     *         this.unknownFields = new LinkedHashMap<>();
     *     }
     *     this.unknownFields.put(index, value);
     * }
     * }</pre>
     *
     * @param classDecl the class declaration to add the field and setter to
     * @param cu the compilation unit (for adding imports)
     */
    public void addToClass(ClassOrInterfaceDeclaration classDecl, CompilationUnit cu) {
        addImports(cu);

        var field = classDecl.addField(
                "Map<Long, ProtobufUnknownValue>", "unknownFields", Modifier.Keyword.PRIVATE);
        field.addAnnotation(new MarkerAnnotationExpr("ProtobufUnknownFields"));

        var setter = classDecl.addMethod("setUnknownField", Modifier.Keyword.PUBLIC);
        setter.setType("void");
        setter.addParameter("long", "index");
        setter.addParameter("ProtobufUnknownValue", "value");
        setter.addAnnotation(new MarkerAnnotationExpr("ProtobufUnknownFields.Setter"));

        var body = new BlockStmt();
        body.addStatement(com.github.javaparser.StaticJavaParser.parseStatement(
                "if (this.unknownFields == null) { this.unknownFields = new java.util.LinkedHashMap<>(); }"
        ));
        body.addStatement(com.github.javaparser.StaticJavaParser.parseStatement(
                "this.unknownFields.put(index, value);"
        ));
        setter.setBody(body);

        var getter = classDecl.addMethod("unknownFields", Modifier.Keyword.PUBLIC);
        getter.setType("Map<Long, ProtobufUnknownValue>");
        getter.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr("unknownFields"))));
    }

    /**
     * Adds an {@code @ProtobufUnknownFields} record component to a record declaration.
     *
     * <p>The generated component is:
     * <pre>{@code
     * @ProtobufUnknownFields Map<Long, ProtobufUnknownValue> unknownFields
     * }</pre>
     *
     * @param recordDecl the record declaration to add the component to
     * @param cu the compilation unit (for adding imports)
     */
    public void addToRecord(RecordDeclaration recordDecl, CompilationUnit cu) {
        addImports(cu);

        var param = new Parameter(
                new ClassOrInterfaceType(null, "Map<Long, ProtobufUnknownValue>"),
                "unknownFields"
        );
        param.addAnnotation(new MarkerAnnotationExpr("ProtobufUnknownFields"));
        recordDecl.getParameters().add(param);
    }

    /**
     * Adds an {@code @ProtobufUnknownFields} abstract method to an interface declaration.
     *
     * <p>The generated method is:
     * <pre>{@code
     * @ProtobufUnknownFields
     * Map<Long, ProtobufUnknownValue> unknownFields();
     * }</pre>
     *
     * @param interfaceDecl the interface declaration to add the method to
     * @param cu the compilation unit (for adding imports)
     */
    public void addToInterface(ClassOrInterfaceDeclaration interfaceDecl, CompilationUnit cu) {
        addImports(cu);

        var method = interfaceDecl.addMethod("unknownFields");
        method.setType("Map<Long, ProtobufUnknownValue>");
        method.removeBody();
        method.addAnnotation(new MarkerAnnotationExpr("ProtobufUnknownFields"));
    }

    private void addImports(CompilationUnit cu) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage.ProtobufUnknownFields");
        cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue");
        cu.addImport("java.util.Map");
    }
}
