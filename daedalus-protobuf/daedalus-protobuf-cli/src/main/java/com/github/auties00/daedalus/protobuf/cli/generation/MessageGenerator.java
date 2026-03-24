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
import com.github.auties00.daedalus.protobuf.compiler.tree.*;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source files for proto message definitions.
 *
 * <p>Supports all five type forms: {@link TypeForm#RECORD}, {@link TypeForm#CLASS},
 * {@link TypeForm#INTERFACE}, {@link TypeForm#SEALED_CLASS}, and
 * {@link TypeForm#SEALED_INTERFACE}.
 */
public final class MessageGenerator {
    private final SchemaConfig config;
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;
    private final EnumGenerator enumGenerator;
    private final OneofGenerator oneofGenerator;
    private final GroupGenerator groupGenerator;
    private final BuilderGenerator builderGenerator;
    private final UnknownFieldsGenerator unknownFieldsGenerator;

    /**
     * Constructs a message generator with the given config and collaborating generators.
     *
     * @param config the schema configuration
     * @param propertyGenerator the property generator for field annotations
     * @param naming the naming strategy
     * @param enumGenerator the enum generator for nested enums
     * @param oneofGenerator the oneof generator for oneof fields
     * @param groupGenerator the group generator for nested groups
     * @param builderGenerator the builder generator for builder constructors
     * @param unknownFieldsGenerator the unknown fields generator
     */
    public MessageGenerator(SchemaConfig config, PropertyGenerator propertyGenerator,
                             NamingStrategy naming, EnumGenerator enumGenerator,
                             OneofGenerator oneofGenerator, GroupGenerator groupGenerator,
                             BuilderGenerator builderGenerator,
                             UnknownFieldsGenerator unknownFieldsGenerator) {
        this.config = config;
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
        this.enumGenerator = enumGenerator;
        this.oneofGenerator = oneofGenerator;
        this.groupGenerator = groupGenerator;
        this.builderGenerator = builderGenerator;
        this.unknownFieldsGenerator = unknownFieldsGenerator;
    }

    /**
     * Generates a Java {@link CompilationUnit} for a top-level proto message.
     *
     * @param message the proto message statement
     * @param javaPackage the target Java package
     * @param protoFqn the fully-qualified proto name for the {@code name} attribute
     * @return the generated compilation unit
     */
    public CompilationUnit generate(ProtobufMessageStatement message, String javaPackage, String protoFqn) {
        var cu = new CompilationUnit();
        if (javaPackage != null && !javaPackage.isEmpty()) {
            cu.setPackageDeclaration(javaPackage);
        }

        var typeForm = resolveTypeForm(protoFqn);
        var typeName = naming.toTypeName(message.name());
        var fields = collectFields(message);

        addImports(cu, fields);

        switch (typeForm) {
            case RECORD -> generateRecord(cu, message, typeName, protoFqn, fields);
            case CLASS -> generateClass(cu, message, typeName, protoFqn, fields);
            case INTERFACE -> generateInterface(cu, message, typeName, protoFqn, fields);
            case SEALED_CLASS -> generateSealedClass(cu, message, typeName, protoFqn, fields);
            case SEALED_INTERFACE -> generateSealedInterface(cu, message, typeName, protoFqn, fields);
        }

        var includeUnknownFields = resolveIncludeUnknownFields(protoFqn);
        if (includeUnknownFields && !cu.getTypes().isEmpty()) {
            var primaryType = cu.getType(0);
            switch (typeForm) {
                case RECORD -> {
                    if (primaryType instanceof RecordDeclaration rd) {
                        unknownFieldsGenerator.addToRecord(rd, cu);
                    }
                }
                case CLASS, SEALED_CLASS -> {
                    if (primaryType instanceof ClassOrInterfaceDeclaration cd) {
                        unknownFieldsGenerator.addToClass(cd, cu);
                    }
                }
                case INTERFACE, SEALED_INTERFACE -> {
                    if (primaryType instanceof ClassOrInterfaceDeclaration cd) {
                        unknownFieldsGenerator.addToInterface(cd, cu);
                    }
                }
            }
        }

        if (config.defaults().generateBuilders() && !cu.getTypes().isEmpty()) {
            var primaryType = cu.getType(0);
            switch (typeForm) {
                case RECORD -> {
                    if (primaryType instanceof RecordDeclaration rd) {
                        builderGenerator.addBuilderToRecord(rd, fields, cu);
                    }
                }
                case CLASS, SEALED_CLASS -> {
                    if (primaryType instanceof ClassOrInterfaceDeclaration cd) {
                        builderGenerator.addBuilderToClass(cd, fields, cu);
                    }
                }
                default -> {
                    // Interfaces and sealed interfaces don't have constructors
                }
            }
        }

        generateNestedTypes(cu, message, javaPackage, protoFqn);

        return cu;
    }

    /**
     * Generates a nested type declaration for a nested proto message.
     *
     * @param parentType the parent type declaration to add the nested type into
     * @param message the nested proto message statement
     * @param protoFqn the fully-qualified proto name
     */
    public void generateNested(TypeDeclaration<?> parentType, ProtobufMessageStatement message, String protoFqn) {
        var typeForm = resolveTypeForm(protoFqn);
        var typeName = naming.toTypeName(message.name());
        var fields = collectFields(message);

        switch (typeForm) {
            case RECORD -> {
                var record = createRecordDeclaration(typeName, protoFqn, message, fields);
                record.addModifier(Modifier.Keyword.STATIC);
                parentType.addMember(record);
            }
            case CLASS -> {
                var classDecl = createClassDeclaration(typeName, protoFqn, message, fields);
                classDecl.addModifier(Modifier.Keyword.STATIC);
                parentType.addMember(classDecl);
            }
            case INTERFACE -> {
                var interfaceDecl = createInterfaceDeclaration(typeName, protoFqn, message, fields);
                interfaceDecl.addModifier(Modifier.Keyword.STATIC);
                parentType.addMember(interfaceDecl);
            }
            default -> {
                var record = createRecordDeclaration(typeName, protoFqn, message, fields);
                record.addModifier(Modifier.Keyword.STATIC);
                parentType.addMember(record);
            }
        }
    }

    private void generateRecord(CompilationUnit cu, ProtobufMessageStatement message,
                                String typeName, String protoFqn,
                                List<ProtobufFieldStatement> fields) {
        var record = createRecordDeclaration(typeName, protoFqn, message, fields);
        record.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(record);
    }

    private RecordDeclaration createRecordDeclaration(String typeName, String protoFqn,
                                                      ProtobufMessageStatement message,
                                                      List<ProtobufFieldStatement> fields) {
        var components = new NodeList<Parameter>();
        for (var field : fields) {
            components.add(propertyGenerator.createRecordComponent(field));
        }

        var record = new RecordDeclaration(
                new NodeList<>(),
                new NodeList<>(),
                new com.github.javaparser.ast.expr.SimpleName(typeName),
                components,
                new NodeList<>(),
                new NodeList<>(),
                new NodeList<>(),
                null
        );

        record.addAnnotation(createMessageAnnotation(protoFqn, message));
        return record;
    }

    private void generateClass(CompilationUnit cu, ProtobufMessageStatement message,
                               String typeName, String protoFqn,
                               List<ProtobufFieldStatement> fields) {
        var classDecl = createClassDeclaration(typeName, protoFqn, message, fields);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        cu.addType(classDecl);
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufAccessor");
    }

    private ClassOrInterfaceDeclaration createClassDeclaration(String typeName, String protoFqn,
                                                               ProtobufMessageStatement message,
                                                               List<ProtobufFieldStatement> fields) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addAnnotation(createMessageAnnotation(protoFqn, message));

        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);

            var fieldDecl = classDecl.addField(javaType, fieldName, Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
            fieldDecl.getVariable(0).getParentNode().ifPresent(parent -> {
                if (parent instanceof FieldDeclaration fd) {
                    fd.addAnnotation(propertyGenerator.createPropertyAnnotation(field));
                }
            });
        }

        var constructorParams = new NodeList<Parameter>();
        var constructorBody = new BlockStmt();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            constructorParams.add(new Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    fieldName
            ));
            constructorBody.addStatement(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), fieldName),
                    new NameExpr(fieldName),
                    AssignExpr.Operator.ASSIGN
            ));
        }
        var constructor = classDecl.addConstructor(Modifier.Keyword.PUBLIC);
        constructor.setParameters(constructorParams);
        constructor.setBody(constructorBody);

        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            var index = field.index().value().longValueExact();

            var accessor = classDecl.addMethod(fieldName, Modifier.Keyword.PUBLIC);
            accessor.setType(javaType);
            accessor.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufAccessor"),
                    new NodeList<>(new MemberValuePair("index", new LongLiteralExpr(String.valueOf(index))))
            ));
            accessor.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new NameExpr(fieldName))
            ));
        }

        return classDecl;
    }

    private void generateInterface(CompilationUnit cu, ProtobufMessageStatement message,
                                   String typeName, String protoFqn,
                                   List<ProtobufFieldStatement> fields) {
        var interfaceDecl = createInterfaceDeclaration(typeName, protoFqn, message, fields);
        interfaceDecl.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(interfaceDecl);
    }

    private ClassOrInterfaceDeclaration createInterfaceDeclaration(String typeName, String protoFqn,
                                                                    ProtobufMessageStatement message,
                                                                    List<ProtobufFieldStatement> fields) {
        var interfaceDecl = new ClassOrInterfaceDeclaration();
        interfaceDecl.setName(typeName);
        interfaceDecl.setInterface(true);
        interfaceDecl.addAnnotation(createMessageAnnotation(protoFqn, message));

        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);

            var method = interfaceDecl.addMethod(fieldName);
            method.setType(javaType);
            method.removeBody();
            method.addAnnotation(propertyGenerator.createPropertyAnnotation(field));
        }

        return interfaceDecl;
    }

    private void generateSealedClass(CompilationUnit cu, ProtobufMessageStatement message,
                                     String typeName, String protoFqn,
                                     List<ProtobufFieldStatement> fields) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED, Modifier.Keyword.ABSTRACT);
        classDecl.addAnnotation(createMessageAnnotation(protoFqn, message));

        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);

            var fieldDecl = classDecl.addField(javaType, fieldName, Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
            fieldDecl.getVariable(0).getParentNode().ifPresent(parent -> {
                if (parent instanceof FieldDeclaration fd) {
                    fd.addAnnotation(propertyGenerator.createPropertyAnnotation(field));
                }
            });
        }

        var constructorParams = new NodeList<Parameter>();
        var constructorBody = new BlockStmt();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            constructorParams.add(new Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    fieldName
            ));
            constructorBody.addStatement(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), fieldName),
                    new NameExpr(fieldName),
                    AssignExpr.Operator.ASSIGN
            ));
        }
        var constructor = classDecl.addConstructor(Modifier.Keyword.PROTECTED);
        constructor.setParameters(constructorParams);
        constructor.setBody(constructorBody);

        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufAccessor");
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            var index = field.index().value().longValueExact();

            var accessor = classDecl.addMethod(fieldName, Modifier.Keyword.PUBLIC);
            accessor.setType(javaType);
            accessor.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufAccessor"),
                    new NodeList<>(new MemberValuePair("index", new LongLiteralExpr(String.valueOf(index))))
            ));
            accessor.setBody(new BlockStmt().addStatement(
                    new ReturnStmt(new NameExpr(fieldName))
            ));
        }

        var defaultSubtype = "Default" + typeName;

        var subtypeDecl = new ClassOrInterfaceDeclaration();
        subtypeDecl.setName(defaultSubtype);
        subtypeDecl.setInterface(false);
        subtypeDecl.addModifier(Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
        subtypeDecl.addExtendedType(typeName);
        subtypeDecl.addAnnotation(new MarkerAnnotationExpr("ProtobufMessage"));

        var subtypeConstructor = subtypeDecl.addConstructor(Modifier.Keyword.PUBLIC);
        var superArgs = new NodeList<Expression>();
        var subtypeParams = new NodeList<Parameter>();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            subtypeParams.add(new Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    fieldName
            ));
            superArgs.add(new NameExpr(fieldName));
        }
        subtypeConstructor.setParameters(subtypeParams);
        subtypeConstructor.setBody(new BlockStmt().addStatement(
                new com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt(
                        false, null, new NodeList<>(superArgs)
                )
        ));

        classDecl.addMember(subtypeDecl);
        cu.addType(classDecl);
    }

    private void generateSealedInterface(CompilationUnit cu, ProtobufMessageStatement message,
                                         String typeName, String protoFqn,
                                         List<ProtobufFieldStatement> fields) {
        var interfaceDecl = new ClassOrInterfaceDeclaration();
        interfaceDecl.setName(typeName);
        interfaceDecl.setInterface(true);
        interfaceDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED);
        interfaceDecl.addAnnotation(createMessageAnnotation(protoFqn, message));

        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);

            var method = interfaceDecl.addMethod(fieldName);
            method.setType(javaType);
            method.removeBody();
            method.addAnnotation(propertyGenerator.createPropertyAnnotation(field));
        }

        var defaultSubtype = "Default" + typeName;

        var components = new NodeList<Parameter>();
        for (var field : fields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            components.add(new Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    fieldName
            ));
        }

        var recordDecl = new RecordDeclaration(
                new NodeList<>(),
                new NodeList<>(),
                new com.github.javaparser.ast.expr.SimpleName(defaultSubtype),
                components,
                new NodeList<>(),
                new NodeList<>(new ClassOrInterfaceType(null, typeName)),
                new NodeList<>(),
                null
        );
        recordDecl.addModifier(Modifier.Keyword.STATIC);
        recordDecl.addAnnotation(new MarkerAnnotationExpr("ProtobufMessage"));

        interfaceDecl.addMember(recordDecl);
        cu.addType(interfaceDecl);
    }

    private void generateNestedTypes(CompilationUnit cu, ProtobufMessageStatement message,
                                     String javaPackage, String parentProtoFqn) {
        var primaryType = cu.getTypes().isEmpty() ? null : cu.getType(0);
        if (primaryType == null) {
            return;
        }

        message.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(nested -> {
            var nestedFqn = parentProtoFqn + "." + nested.name();
            generateNested(primaryType, nested, nestedFqn);
        });

        message.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(nested -> {
            var nestedFqn = parentProtoFqn + "." + nested.name();
            enumGenerator.generateNested(primaryType, nested, nestedFqn);
        });

        message.getDirectChildrenByType(ProtobufOneofStatement.class).forEach(oneof ->
                oneofGenerator.generate(primaryType, oneof)
        );

        message.getDirectChildrenByType(ProtobufGroupStatement.class).forEach(group -> {
            var nestedFqn = parentProtoFqn + "." + group.name();
            var groupCu = groupGenerator.generate(group, javaPackage, nestedFqn);
            if (!groupCu.getTypes().isEmpty()) {
                var groupType = groupCu.getType(0);
                groupType.addModifier(Modifier.Keyword.STATIC);
                primaryType.addMember(groupType);
            }
        });
    }

    private AnnotationExpr createMessageAnnotation(String protoFqn, ProtobufMessageStatement message) {
        var pairs = new ArrayList<MemberValuePair>();

        if (protoFqn != null && !protoFqn.isEmpty()) {
            pairs.add(new MemberValuePair("protoName", new StringLiteralExpr(protoFqn)));
        }

        var reservedNames = collectReservedNames(message);
        if (!reservedNames.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var name : reservedNames) {
                values.add(new StringLiteralExpr(name));
            }
            pairs.add(new MemberValuePair("reservedNames", new ArrayInitializerExpr(values)));
        }

        var reservedIndexes = collectReservedIndexes(message);
        if (!reservedIndexes.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var idx : reservedIndexes) {
                values.add(new IntegerLiteralExpr(String.valueOf(idx)));
            }
            pairs.add(new MemberValuePair("reservedIndexes", new ArrayInitializerExpr(values)));
        }

        var reservedRanges = collectReservedRanges(message);
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
            return new MarkerAnnotationExpr("ProtobufMessage");
        }

        return new NormalAnnotationExpr(
                new Name("ProtobufMessage"),
                new NodeList<>(pairs)
        );
    }

    private List<ProtobufFieldStatement> collectFields(ProtobufMessageStatement message) {
        return new ArrayList<>(message.getDirectChildrenByType(ProtobufFieldStatement.class).toList());
    }

    private List<String> collectReservedNames(ProtobufMessageStatement message) {
        var names = new ArrayList<String>();
        message.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufLiteralExpression literal) {
                        names.add(literal.value());
                    }
                })
        );
        return names;
    }

    private List<Long> collectReservedIndexes(ProtobufMessageStatement message) {
        var indexes = new ArrayList<Long>();
        message.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufIntegerExpression intExpr) {
                        indexes.add(intExpr.value().value().longValueExact());
                    }
                })
        );
        return indexes;
    }

    private List<long[]> collectReservedRanges(ProtobufMessageStatement message) {
        var ranges = new ArrayList<long[]>();
        message.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufIntegerRangeExpression rangeExpr) {
                        var range = rangeExpr.value();
                        var lower = range.lowerBound().value().longValueExact();
                        var upper = range.upperBound()
                                .map(u -> u.value().longValueExact())
                                .orElse(536_870_911L);
                        ranges.add(new long[]{lower, upper});
                    }
                })
        );
        return ranges;
    }

    private void addImports(CompilationUnit cu, List<ProtobufFieldStatement> fields) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage");
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage.ProtobufProperty");
        cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufType");

        var hasReservedRanges = false;
        var hasMap = false;
        var hasList = false;

        for (var field : fields) {
            if (field.type() instanceof com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference) {
                hasMap = true;
            }
            if (field.modifier() == ProtobufModifier.REPEATED
                    && !(field.type() instanceof com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference)) {
                hasList = true;
            }
        }

        if (hasMap) {
            cu.addImport("java.util.Map");
        }
        if (hasList) {
            cu.addImport("java.util.List");
        }
    }

    private TypeForm resolveTypeForm(String protoFqn) {
        if (protoFqn != null) {
            var override = config.typeOverrides().get(protoFqn);
            if (override != null && override.typeForm() != null) {
                return override.typeForm();
            }
        }
        return config.defaults().typeForm();
    }

    private boolean resolveIncludeUnknownFields(String protoFqn) {
        if (protoFqn != null) {
            var override = config.typeOverrides().get(protoFqn);
            if (override != null && override.includeUnknownFields() != null) {
                return override.includeUnknownFields();
            }
        }
        return config.defaults().includeUnknownFields();
    }
}
