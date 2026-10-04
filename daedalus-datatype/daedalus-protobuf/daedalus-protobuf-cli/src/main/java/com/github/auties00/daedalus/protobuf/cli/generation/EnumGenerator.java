package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumConstantStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufReservedStatement;
import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source files for proto enum definitions.
 *
 * <p>Supports all three enum forms, which annotate every constant with
 * {@code @ProtobufEnum.Constant}.
 *
 * <p>When {@code generateDefaultValue} is enabled in the configuration,
 * the first enum constant (by declaration order) is annotated with
 * {@code @ProtobufDefaultValue}.
 *
 * <p>Every generated {@code @ProtobufEnum} annotation declares the {@code version} of
 * the {@code .proto} file that declares the enum, unless it is proto2, which is the
 * default of the annotation, and declares whether the enum is open or closed in its
 * {@code type} attribute when that differs from the default of the version.
 */
public final class EnumGenerator {
    private final SchemaConfig config;
    private final NamingStrategy naming;
    private final boolean generateDefaultValue;

    /**
     * Constructs an enum generator with the given config and naming strategy.
     *
     * @param config the schema configuration
     * @param naming the naming strategy
     */
    public EnumGenerator(SchemaConfig config, NamingStrategy naming) {
        this.config = config;
        this.naming = naming;
        this.generateDefaultValue = config.defaults().generateDefaultValue();
    }

    /**
     * Generates a Java {@link CompilationUnit} for a top-level proto enum.
     *
     * @param enumStmt the proto enum statement
     * @param javaPackage the target Java package
     * @param protoFqn the fully-qualified proto name
     * @return the generated compilation unit
     */
    public CompilationUnit generate(ProtobufEnumStatement enumStmt, String javaPackage, String protoFqn) {
        var cu = new CompilationUnit();
        if (javaPackage != null && !javaPackage.isEmpty()) {
            cu.setPackageDeclaration(javaPackage);
        }

        addImports(cu, enumStmt);

        var enumForm = resolveEnumForm(protoFqn);
        var typeName = naming.toTypeName(enumStmt.name());
        var constants = collectConstants(enumStmt);

        switch (enumForm) {
            case JAVA_ENUM -> generateJavaEnum(cu, enumStmt, typeName, protoFqn, constants);
            case CLASS -> generateClass(cu, enumStmt, typeName, protoFqn, constants);
            case RECORD -> generateRecord(cu, enumStmt, typeName, protoFqn, constants);
        }

        return cu;
    }

    /**
     * Generates a nested enum type declaration inside a parent type, and adds the
     * imports it needs to the compilation unit of the parent type.
     *
     * @param parentType the parent type to add the nested enum into
     * @param enumStmt the proto enum statement
     * @param protoFqn the fully-qualified proto name
     */
    public void generateNested(TypeDeclaration<?> parentType, ProtobufEnumStatement enumStmt, String protoFqn) {
        var typeName = naming.toTypeName(enumStmt.name());
        var constants = collectConstants(enumStmt);
        var enumForm = resolveEnumForm(protoFqn);
        parentType.findCompilationUnit()
                .ifPresent(cu -> addImports(cu, enumStmt));

        if (enumForm == EnumForm.JAVA_ENUM) {
            var enumDecl = createJavaEnumDeclaration(enumStmt, typeName, protoFqn, constants);
            enumDecl.addModifier(Modifier.Keyword.STATIC);
            parentType.addMember(enumDecl);
        } else {
            var classDecl = createClassEnumDeclaration(enumStmt, typeName, protoFqn, constants);
            classDecl.addModifier(Modifier.Keyword.STATIC);
            parentType.addMember(classDecl);
        }
    }

    private void generateJavaEnum(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                   String typeName, String protoFqn,
                                   List<ProtobufEnumConstantStatement> constants) {
        var enumDecl = createJavaEnumDeclaration(enumStmt, typeName, protoFqn, constants);
        enumDecl.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(enumDecl);
    }

    private EnumDeclaration createJavaEnumDeclaration(ProtobufEnumStatement enumStmt,
                                                      String typeName, String protoFqn,
                                                      List<ProtobufEnumConstantStatement> constants) {
        var enumDecl = new EnumDeclaration();
        enumDecl.setName(typeName);
        enumDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        var isFirst = true;
        for (var constant : constants) {
            var name = naming.toEnumConstantName(constant.name());
            var index = (int) constant.index().value();
            var entry = new EnumConstantDeclaration(name);
            entry.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))))
            ));
            if (isFirst && generateDefaultValue) {
                entry.addAnnotation(new MarkerAnnotationExpr("ProtobufDefaultValue"));
                isFirst = false;
            }
            enumDecl.addEntry(entry);
        }

        return enumDecl;
    }

    private void generateClass(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                String typeName, String protoFqn,
                                List<ProtobufEnumConstantStatement> constants) {
        var classDecl = createClassEnumDeclaration(enumStmt, typeName, protoFqn, constants);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        cu.addType(classDecl);
    }

    private ClassOrInterfaceDeclaration createClassEnumDeclaration(ProtobufEnumStatement enumStmt,
                                                                    String typeName, String protoFqn,
                                                                    List<ProtobufEnumConstantStatement> constants) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        for (var constant : constants) {
            var name = naming.toEnumConstantName(constant.name());
            var index = (int) constant.index().value();

            var field = classDecl.addField(typeName, name,
                    Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            field.getVariable(0).setInitializer(
                    new ObjectCreationExpr(null, new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new IntegerLiteralExpr(String.valueOf(index))))
            );
            field.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))))
            ));
        }

        classDecl.addField("int", "index", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        var constructor = classDecl.addConstructor();
        constructor.addParameter("int", "index");
        constructor.setBody(new BlockStmt().addStatement(
                new AssignExpr(
                        new FieldAccessExpr(new ThisExpr(), "index"),
                        new NameExpr("index"),
                        AssignExpr.Operator.ASSIGN
                )
        ));

        return classDecl;
    }

    private void generateRecord(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                 String typeName, String protoFqn,
                                 List<ProtobufEnumConstantStatement> constants) {
        var components = new NodeList<Parameter>();
        components.add(new Parameter(com.github.javaparser.ast.type.PrimitiveType.intType(), "index"));

        var record = new RecordDeclaration(
                new NodeList<>(Modifier.publicModifier()),
                new NodeList<>(),
                new com.github.javaparser.ast.expr.SimpleName(typeName),
                components,
                new NodeList<>(),
                new NodeList<>(),
                new NodeList<>(),
                null
        );
        record.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        for (var constant : constants) {
            var name = naming.toEnumConstantName(constant.name());
            var index = (int) constant.index().value();

            var field = record.addField(typeName, name,
                    Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            field.getVariable(0).setInitializer(
                    new ObjectCreationExpr(null, new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new IntegerLiteralExpr(String.valueOf(index))))
            );
            field.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))))
            ));
        }

        cu.addType(record);
    }

    /**
     * Adds the imports needed by the {@code @ProtobufEnum} annotation of an enum, and by
     * the annotations of its constants, to a compilation unit.
     *
     * @param cu the compilation unit to add the imports to
     * @param enumStmt the proto enum statement
     */
    private void addImports(CompilationUnit cu, ProtobufEnumStatement enumStmt) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum");
        if (generateDefaultValue) {
            cu.addImport("com.github.auties00.daedalus.typesystem.annotation.ProtobufDefaultValue");
        }

        if (resolveVersion(enumStmt) != ProtobufVersion.DEFAULT_VERSION) {
            cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufVersion");
        }

        if (hasEnumTypeAttribute(enumStmt)) {
            cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufEnumType");
        }

        var hasReservedRanges = enumStmt.getDirectChildrenByType(ProtobufReservedStatement.class)
                .anyMatch(reserved -> reserved.expressions()
                        .stream()
                        .anyMatch(expression -> expression instanceof ProtobufIntegerRangeExpression));
        if (hasReservedRanges) {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange");
        }
    }

    /**
     * Returns the version or edition of the {@code .proto} file that declares an enum.
     *
     * @param enumStmt the proto enum statement
     * @return the version of the enclosing document
     */
    private ProtobufVersion resolveVersion(ProtobufEnumStatement enumStmt) {
        return enumStmt.document()
                .version();
    }

    /**
     * Returns whether the {@code @ProtobufEnum} annotation of an enum must declare the
     * {@code type} attribute.
     *
     * <p>The attribute is declared when the enum is open or closed while the default of
     * its version is the opposite, which can only happen in an edition that sets the
     * {@code features.enum_type} feature.
     *
     * @param enumStmt the proto enum statement
     * @return {@code true} if the attribute must be declared, otherwise {@code false}
     */
    private boolean hasEnumTypeAttribute(ProtobufEnumStatement enumStmt) {
        var defaultType = ProtobufEnumType.VERSION_DEFAULT.resolve(resolveVersion(enumStmt));
        return enumStmt.enumType() != defaultType;
    }

    private AnnotationExpr createEnumAnnotation(String protoFqn, ProtobufEnumStatement enumStmt) {
        var pairs = new ArrayList<MemberValuePair>();

        var version = resolveVersion(enumStmt);
        if (version != ProtobufVersion.DEFAULT_VERSION) {
            pairs.add(new MemberValuePair("version", new FieldAccessExpr(
                    new NameExpr("ProtobufVersion"), version.name()
            )));
        }

        if (protoFqn != null && !protoFqn.isEmpty()) {
            pairs.add(new MemberValuePair("protoName", new StringLiteralExpr(protoFqn)));
        }

        if (hasEnumTypeAttribute(enumStmt)) {
            pairs.add(new MemberValuePair("type", new FieldAccessExpr(
                    new NameExpr("ProtobufEnumType"), enumStmt.enumType().name()
            )));
        }

        var reservedNames = new ArrayList<String>();
        var reservedIndexes = new ArrayList<Integer>();
        var reservedRanges = new ArrayList<int[]>();

        enumStmt.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufLiteralExpression literal) {
                        reservedNames.add(literal.value());
                    } else if (expr instanceof ProtobufIntegerExpression intExpr) {
                        reservedIndexes.add((int) intExpr.value().value());
                    } else if (expr instanceof ProtobufIntegerRangeExpression rangeExpr) {
                        var range = rangeExpr.value();
                        var lower = (int) range.lowerBound().value();
                        var upper = range.upperBound()
                                .map(u -> (int) u.value())
                                .orElse(536_870_911);
                        reservedRanges.add(new int[]{lower, upper});
                    }
                })
        );

        if (!reservedNames.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var name : reservedNames) {
                values.add(new StringLiteralExpr(name));
            }
            pairs.add(new MemberValuePair("reservedNames", new ArrayInitializerExpr(values)));
        }

        if (!reservedIndexes.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var idx : reservedIndexes) {
                values.add(new IntegerLiteralExpr(String.valueOf(idx)));
            }
            pairs.add(new MemberValuePair("reservedIndexes", new ArrayInitializerExpr(values)));
        }

        if (!reservedRanges.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var range : reservedRanges) {
                values.add(new NormalAnnotationExpr(
                        new Name("ProtobufReservedRange"),
                        new NodeList<>(
                                new MemberValuePair("min", new IntegerLiteralExpr(String.valueOf(range[0]))),
                                new MemberValuePair("max", new IntegerLiteralExpr(String.valueOf(range[1])))
                        )
                ));
            }
            pairs.add(new MemberValuePair("reservedRanges", new ArrayInitializerExpr(values)));
        }

        if (pairs.isEmpty()) {
            return new MarkerAnnotationExpr("ProtobufEnum");
        }

        return new NormalAnnotationExpr(new Name("ProtobufEnum"), new NodeList<>(pairs));
    }

    /**
     * Returns the constants of an enum in declaration order.
     *
     * <p>The first constant of an open enum is its default value, which must have index
     * {@code 0}. Proto3 and the editions already require the first constant of an open
     * enum to have index {@code 0}, but, if it does not, the first constant with index
     * {@code 0} is moved before the others, so that the generated enum is accepted by
     * the annotation processor.
     *
     * @param enumStmt the proto enum statement
     * @return a mutable list of the constants of the enum
     */
    private List<ProtobufEnumConstantStatement> collectConstants(ProtobufEnumStatement enumStmt) {
        var constants = new ArrayList<>(enumStmt.getDirectChildrenByType(ProtobufEnumConstantStatement.class).toList());
        if (enumStmt.enumType() == ProtobufEnumType.OPEN && !constants.isEmpty() && constants.getFirst().index().value() != 0) {
            for (var i = 1; i < constants.size(); i++) {
                if (constants.get(i).index().value() == 0) {
                    constants.addFirst(constants.remove(i));
                    break;
                }
            }
        }
        return constants;
    }

    private EnumForm resolveEnumForm(String protoFqn) {
        if (protoFqn != null) {
            var override = config.typeOverrides().get(protoFqn);
            if (override != null && override.enumForm() != null) {
                return override.enumForm();
            }
        }
        return config.defaults().enumForm();
    }
}
