package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumConstantStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufReservedStatement;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.EnumConstantMode;
import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source files for proto enum definitions.
 *
 * <p>Supports all five enum forms and both constant modes
 * ({@link EnumConstantMode#CONSTANT_ANNOTATION} and
 * {@link EnumConstantMode#SERIALIZER_DESERIALIZER}).
 *
 * <p>When {@code generateDefaultValue} is enabled in the configuration,
 * the first enum constant (by declaration order) is annotated with
 * {@code @ProtobufDefaultValue}.
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

        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum");
        if (generateDefaultValue) {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDefaultValue");
        }

        var enumForm = resolveEnumForm(protoFqn);
        var constantMode = resolveConstantMode(protoFqn);
        var typeName = naming.toTypeName(enumStmt.name());
        var constants = collectConstants(enumStmt);

        switch (enumForm) {
            case JAVA_ENUM -> generateJavaEnum(cu, enumStmt, typeName, protoFqn, constants, constantMode);
            case CLASS -> generateClass(cu, enumStmt, typeName, protoFqn, constants, constantMode);
            case RECORD -> generateRecord(cu, enumStmt, typeName, protoFqn, constants, constantMode);
            case SEALED_INTERFACE -> generateSealedInterface(cu, enumStmt, typeName, protoFqn, constants, constantMode);
            case SEALED_ABSTRACT_CLASS -> generateSealedAbstractClass(cu, enumStmt, typeName, protoFqn, constants, constantMode);
        }

        return cu;
    }

    /**
     * Generates a nested enum type declaration inside a parent type.
     *
     * @param parentType the parent type to add the nested enum into
     * @param enumStmt the proto enum statement
     * @param protoFqn the fully-qualified proto name
     */
    public void generateNested(TypeDeclaration<?> parentType, ProtobufEnumStatement enumStmt, String protoFqn) {
        var typeName = naming.toTypeName(enumStmt.name());
        var constants = collectConstants(enumStmt);
        var constantMode = resolveConstantMode(protoFqn);
        var enumForm = resolveEnumForm(protoFqn);

        if (enumForm == EnumForm.JAVA_ENUM) {
            var enumDecl = createJavaEnumDeclaration(enumStmt, typeName, protoFqn, constants, constantMode);
            enumDecl.addModifier(Modifier.Keyword.STATIC);
            parentType.addMember(enumDecl);
        } else {
            var classDecl = createClassEnumDeclaration(enumStmt, typeName, protoFqn, constants, constantMode);
            classDecl.addModifier(Modifier.Keyword.STATIC);
            parentType.addMember(classDecl);
        }
    }

    private void generateJavaEnum(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                   String typeName, String protoFqn,
                                   List<ProtobufEnumConstantStatement> constants,
                                   EnumConstantMode constantMode) {
        var enumDecl = createJavaEnumDeclaration(enumStmt, typeName, protoFqn, constants, constantMode);
        enumDecl.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(enumDecl);

        if (constantMode == EnumConstantMode.SERIALIZER_DESERIALIZER) {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer");
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer");
            cu.addImport("java.util.Arrays");
            cu.addImport("java.util.Map");
            cu.addImport("java.util.function.Function");
            cu.addImport("java.util.stream.Collectors");
        }
    }

    private EnumDeclaration createJavaEnumDeclaration(ProtobufEnumStatement enumStmt,
                                                      String typeName, String protoFqn,
                                                      List<ProtobufEnumConstantStatement> constants,
                                                      EnumConstantMode constantMode) {
        var enumDecl = new EnumDeclaration();
        enumDecl.setName(typeName);
        enumDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        if (constantMode == EnumConstantMode.CONSTANT_ANNOTATION) {
            var isFirst = true;
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();
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
        } else {
            var isFirst = true;
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();
                var entry = new EnumConstantDeclaration(name);
                entry.addArgument(new IntegerLiteralExpr(String.valueOf(index)));
                if (isFirst && generateDefaultValue) {
                    entry.addAnnotation(new MarkerAnnotationExpr("ProtobufDefaultValue"));
                    isFirst = false;
                }
                enumDecl.addEntry(entry);
            }

            enumDecl.addField("Map<Integer, " + typeName + ">", "VALUES",
                    Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

            var indexField = enumDecl.addField("int", "index", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

            var constructor = enumDecl.addConstructor();
            constructor.addParameter("int", "index");
            constructor.setBody(new BlockStmt().addStatement(
                    new AssignExpr(
                            new FieldAccessExpr(new ThisExpr(), "index"),
                            new NameExpr("index"),
                            AssignExpr.Operator.ASSIGN
                    )
            ));

            var serializer = enumDecl.addMethod("index");
            serializer.setType("int");
            serializer.addAnnotation(new MarkerAnnotationExpr("ProtobufSerializer"));
            serializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new NameExpr("index"))
            ));

            var deserializer = enumDecl.addMethod("of", Modifier.Keyword.STATIC);
            deserializer.setType(typeName);
            deserializer.addParameter("int", "index");
            deserializer.addAnnotation(new MarkerAnnotationExpr("ProtobufDeserializer"));
            deserializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new MethodCallExpr(new NameExpr("VALUES"), "get", new NodeList<>(new NameExpr("index"))))
            ));
        }

        return enumDecl;
    }

    private void generateClass(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                String typeName, String protoFqn,
                                List<ProtobufEnumConstantStatement> constants,
                                EnumConstantMode constantMode) {
        var classDecl = createClassEnumDeclaration(enumStmt, typeName, protoFqn, constants, constantMode);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        cu.addType(classDecl);

        if (constantMode == EnumConstantMode.SERIALIZER_DESERIALIZER) {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer");
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer");
        }
    }

    private ClassOrInterfaceDeclaration createClassEnumDeclaration(ProtobufEnumStatement enumStmt,
                                                                    String typeName, String protoFqn,
                                                                    List<ProtobufEnumConstantStatement> constants,
                                                                    EnumConstantMode constantMode) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        if (constantMode == EnumConstantMode.CONSTANT_ANNOTATION) {
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();

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

        if (constantMode == EnumConstantMode.SERIALIZER_DESERIALIZER) {
            var serializer = classDecl.addMethod("index");
            serializer.setType("int");
            serializer.addAnnotation(new MarkerAnnotationExpr("ProtobufSerializer"));
            serializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new NameExpr("index"))
            ));

            var deserializer = classDecl.addMethod("of", Modifier.Keyword.STATIC);
            deserializer.setType(typeName);
            deserializer.addParameter("int", "index");
            deserializer.addAnnotation(new MarkerAnnotationExpr("ProtobufDeserializer"));
            deserializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new ObjectCreationExpr(null,
                            new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new NameExpr("index"))))
            ));
        }

        return classDecl;
    }

    private void generateRecord(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                 String typeName, String protoFqn,
                                 List<ProtobufEnumConstantStatement> constants,
                                 EnumConstantMode constantMode) {
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

        if (constantMode == EnumConstantMode.CONSTANT_ANNOTATION) {
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();

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
        } else {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer");
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer");

            var serializer = record.addMethod("index");
            serializer.setType("int");
            serializer.addAnnotation(new MarkerAnnotationExpr("ProtobufSerializer"));
            serializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new NameExpr("index"))
            ));

            var deserializer = record.addMethod("of", Modifier.Keyword.STATIC);
            deserializer.setType(typeName);
            deserializer.addParameter("int", "index");
            deserializer.addAnnotation(new MarkerAnnotationExpr("ProtobufDeserializer"));
            deserializer.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new ObjectCreationExpr(null,
                            new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new NameExpr("index"))))
            ));
        }

        cu.addType(record);
    }

    private void generateSealedInterface(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                          String typeName, String protoFqn,
                                          List<ProtobufEnumConstantStatement> constants,
                                          EnumConstantMode constantMode) {
        var interfaceDecl = new ClassOrInterfaceDeclaration();
        interfaceDecl.setName(typeName);
        interfaceDecl.setInterface(true);
        interfaceDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED);
        interfaceDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        var indexMethod = interfaceDecl.addMethod("index");
        indexMethod.setType("int");
        indexMethod.removeBody();

        if (constantMode == EnumConstantMode.CONSTANT_ANNOTATION) {
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var subtypeName = naming.toTypeName(constant.name());
                var index = constant.index().value().intValueExact();

                var record = new RecordDeclaration(
                        new NodeList<>(),
                        new NodeList<>(),
                        new SimpleName(subtypeName),
                        new NodeList<>(),
                        new NodeList<>(),
                        new NodeList<>(new ClassOrInterfaceType(null, typeName)),
                        new NodeList<>(),
                        null
                );
                record.addModifier(Modifier.Keyword.STATIC);
                record.addAnnotation(new NormalAnnotationExpr(
                        new Name("ProtobufEnum.Constant"),
                        new NodeList<>(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))))
                ));

                var overrideIndex = record.addMethod("index");
                overrideIndex.setType("int");
                overrideIndex.addAnnotation(new MarkerAnnotationExpr("Override"));
                overrideIndex.setBody(new BlockStmt().addStatement(
                        new ReturnStmt(new IntegerLiteralExpr(String.valueOf(index)))
                ));

                interfaceDecl.addMember(record);

                var staticField = interfaceDecl.addField(typeName, name,
                        Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                staticField.getVariable(0).setInitializer(
                        new ObjectCreationExpr(null, new ClassOrInterfaceType(null, subtypeName), new NodeList<>())
                );
            }
        } else {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer");
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer");

            indexMethod.addAnnotation(new MarkerAnnotationExpr("ProtobufSerializer"));

            for (var constant : constants) {
                var subtypeName = naming.toTypeName(constant.name());
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();

                var record = new RecordDeclaration(
                        new NodeList<>(),
                        new NodeList<>(),
                        new SimpleName(subtypeName),
                        new NodeList<>(),
                        new NodeList<>(),
                        new NodeList<>(new ClassOrInterfaceType(null, typeName)),
                        new NodeList<>(),
                        null
                );
                record.addModifier(Modifier.Keyword.STATIC);

                var overrideIndex = record.addMethod("index");
                overrideIndex.setType("int");
                overrideIndex.addAnnotation(new MarkerAnnotationExpr("Override"));
                overrideIndex.setBody(new BlockStmt().addStatement(
                        new ReturnStmt(new IntegerLiteralExpr(String.valueOf(index)))
                ));

                interfaceDecl.addMember(record);

                var staticField = interfaceDecl.addField(typeName, name,
                        Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                staticField.getVariable(0).setInitializer(
                        new ObjectCreationExpr(null, new ClassOrInterfaceType(null, subtypeName), new NodeList<>())
                );
            }

            var deserializer = interfaceDecl.addMethod("of", Modifier.Keyword.STATIC);
            deserializer.setType(typeName);
            deserializer.addParameter("int", "index");
            deserializer.addAnnotation(new MarkerAnnotationExpr("ProtobufDeserializer"));
            var switchEntries = new StringBuilder("return switch (index) {\n");
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();
                switchEntries.append("            case ").append(index).append(" -> ").append(name).append(";\n");
            }
            switchEntries.append("            default -> null;\n        }");
            deserializer.setBody(new BlockStmt().addStatement(
                    com.github.javaparser.StaticJavaParser.parseStatement(switchEntries.toString() + ";")
            ));
        }

        cu.addType(interfaceDecl);
    }

    private void generateSealedAbstractClass(CompilationUnit cu, ProtobufEnumStatement enumStmt,
                                              String typeName, String protoFqn,
                                              List<ProtobufEnumConstantStatement> constants,
                                              EnumConstantMode constantMode) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED, Modifier.Keyword.ABSTRACT);
        classDecl.addAnnotation(createEnumAnnotation(protoFqn, enumStmt));

        classDecl.addField("int", "index", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        var constructor = classDecl.addConstructor(Modifier.Keyword.PROTECTED);
        constructor.addParameter("int", "index");
        constructor.setBody(new BlockStmt().addStatement(
                new AssignExpr(
                        new FieldAccessExpr(new ThisExpr(), "index"),
                        new NameExpr("index"),
                        AssignExpr.Operator.ASSIGN
                )
        ));

        if (constantMode == EnumConstantMode.CONSTANT_ANNOTATION) {
            var indexAccessor = classDecl.addMethod("index");
            indexAccessor.setType("int");
            indexAccessor.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr("index"))));

            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var subtypeName = naming.toTypeName(constant.name()) + "Impl";
                var index = constant.index().value().intValueExact();

                var subtype = new ClassOrInterfaceDeclaration();
                subtype.setName(subtypeName);
                subtype.setInterface(false);
                subtype.addModifier(Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                subtype.addExtendedType(typeName);
                subtype.addAnnotation(new NormalAnnotationExpr(
                        new Name("ProtobufEnum.Constant"),
                        new NodeList<>(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))))
                ));

                var subtypeConstructor = subtype.addConstructor();
                subtypeConstructor.setBody(new BlockStmt().addStatement(
                        new com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt(
                                false, null,
                                new NodeList<>(new IntegerLiteralExpr(String.valueOf(index)))
                        )
                ));

                classDecl.addMember(subtype);

                var staticField = classDecl.addField(typeName, name,
                        Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                staticField.getVariable(0).setInitializer(
                        new ObjectCreationExpr(null, new ClassOrInterfaceType(null, subtypeName), new NodeList<>())
                );
            }
        } else {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer");
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer");

            var serializer = classDecl.addMethod("index");
            serializer.setType("int");
            serializer.addAnnotation(new MarkerAnnotationExpr("ProtobufSerializer"));
            serializer.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr("index"))));

            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var subtypeName = naming.toTypeName(constant.name()) + "Impl";
                var index = constant.index().value().intValueExact();

                var subtype = new ClassOrInterfaceDeclaration();
                subtype.setName(subtypeName);
                subtype.setInterface(false);
                subtype.addModifier(Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                subtype.addExtendedType(typeName);

                var subtypeConstructor = subtype.addConstructor();
                subtypeConstructor.setBody(new BlockStmt().addStatement(
                        new com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt(
                                false, null,
                                new NodeList<>(new IntegerLiteralExpr(String.valueOf(index)))
                        )
                ));

                classDecl.addMember(subtype);

                var staticField = classDecl.addField(typeName, name,
                        Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                staticField.getVariable(0).setInitializer(
                        new ObjectCreationExpr(null, new ClassOrInterfaceType(null, subtypeName), new NodeList<>())
                );
            }

            var deserializer = classDecl.addMethod("of", Modifier.Keyword.STATIC);
            deserializer.setType(typeName);
            deserializer.addParameter("int", "index");
            deserializer.addAnnotation(new MarkerAnnotationExpr("ProtobufDeserializer"));
            var switchEntries = new StringBuilder("return switch (index) {\n");
            for (var constant : constants) {
                var name = naming.toEnumConstantName(constant.name());
                var index = constant.index().value().intValueExact();
                switchEntries.append("            case ").append(index).append(" -> ").append(name).append(";\n");
            }
            switchEntries.append("            default -> null;\n        }");
            deserializer.setBody(new BlockStmt().addStatement(
                    com.github.javaparser.StaticJavaParser.parseStatement(switchEntries.toString() + ";")
            ));
        }

        cu.addType(classDecl);
    }

    private AnnotationExpr createEnumAnnotation(String protoFqn, ProtobufEnumStatement enumStmt) {
        var pairs = new ArrayList<MemberValuePair>();

        if (protoFqn != null && !protoFqn.isEmpty()) {
            pairs.add(new MemberValuePair("protoName", new StringLiteralExpr(protoFqn)));
        }

        var reservedNames = new ArrayList<String>();
        var reservedIndexes = new ArrayList<Integer>();
        var reservedRanges = new ArrayList<long[]>();

        enumStmt.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufLiteralExpression literal) {
                        reservedNames.add(literal.value());
                    } else if (expr instanceof ProtobufIntegerExpression intExpr) {
                        reservedIndexes.add(intExpr.value().value().intValueExact());
                    } else if (expr instanceof ProtobufIntegerRangeExpression rangeExpr) {
                        var range = rangeExpr.value();
                        var lower = range.lowerBound().value().longValueExact();
                        var upper = range.upperBound()
                                .map(u -> u.value().longValueExact())
                                .orElse(536_870_911L);
                        reservedRanges.add(new long[]{lower, upper});
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
                                new MemberValuePair("min", new LongLiteralExpr(String.valueOf(range[0]))),
                                new MemberValuePair("max", new LongLiteralExpr(String.valueOf(range[1])))
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

    private List<ProtobufEnumConstantStatement> collectConstants(ProtobufEnumStatement enumStmt) {
        return new ArrayList<>(enumStmt.getDirectChildrenByType(ProtobufEnumConstantStatement.class).toList());
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

    private EnumConstantMode resolveConstantMode(String protoFqn) {
        if (protoFqn != null) {
            var override = config.typeOverrides().get(protoFqn);
            if (override != null && override.enumConstantMode() != null) {
                return override.enumConstantMode();
            }
        }
        return config.defaults().enumConstantMode();
    }
}
