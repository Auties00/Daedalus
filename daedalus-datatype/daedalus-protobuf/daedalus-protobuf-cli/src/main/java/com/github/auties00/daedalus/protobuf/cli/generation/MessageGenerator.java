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
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source files for proto message definitions.
 *
 * <p>Supports all five type forms: {@link TypeForm#RECORD}, {@link TypeForm#CLASS},
 * {@link TypeForm#INTERFACE}, {@link TypeForm#SEALED_CLASS}, and
 * {@link TypeForm#SEALED_INTERFACE}.
 *
 * <p>Every generated {@code @ProtobufMessage} annotation declares the {@code version}
 * of the {@code .proto} file that declares the message, unless it is proto2, which is
 * the default of the annotation. A {@code oneof} is declared as a single field whose type
 * is a sealed interface nested in the message, as described in {@link OneOfGenerator}, and
 * a message that holds a case of a {@code oneof} may implement its interface.
 *
 * <p>The presence of a field determines the Java type of the field and the type of its
 * accessor, as described in {@link PropertyGenerator}. The type forms differ in how an
 * accessor can be declared:
 * <ul>
 * <li>{@link TypeForm#CLASS} and {@link TypeForm#SEALED_CLASS} declare package private
 *     fields, which the generated Spec class reads directly, and public accessors that
 *     return an {@code Optional}, or one of its primitive specializations, for a field
 *     that can be unset;
 * <li>{@link TypeForm#RECORD} declares the field as a record component, whose accessor
 *     must return the type of the component, so a field that can be unset is returned
 *     as a nullable boxed value instead of an {@code Optional};
 * <li>{@link TypeForm#INTERFACE} and {@link TypeForm#SEALED_INTERFACE} declare the field
 *     as an annotated abstract method, which the annotation processor implements, so it
 *     also returns the nullable boxed value of a field that can be unset.
 * </ul>
 */
public final class MessageGenerator {
    private final SchemaConfig config;
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;
    private final EnumGenerator enumGenerator;
    private final GroupGenerator groupGenerator;
    private final BuilderGenerator builderGenerator;
    private final UnknownFieldsGenerator unknownFieldsGenerator;
    private final OneOfGenerator oneOfGenerator;

    /**
     * Constructs a message generator with the given config and collaborating generators.
     *
     * @param config the schema configuration
     * @param propertyGenerator the property generator for field annotations
     * @param naming the naming strategy
     * @param enumGenerator the enum generator for nested enums
     * @param groupGenerator the group generator for nested groups
     * @param builderGenerator the builder generator for builder constructors
     * @param unknownFieldsGenerator the unknown fields generator
     * @param oneOfGenerator the oneof generator, which collects the members of a message
     */
    public MessageGenerator(SchemaConfig config, PropertyGenerator propertyGenerator,
                             NamingStrategy naming, EnumGenerator enumGenerator,
                             GroupGenerator groupGenerator,
                             BuilderGenerator builderGenerator,
                             UnknownFieldsGenerator unknownFieldsGenerator,
                             OneOfGenerator oneOfGenerator) {
        this.config = config;
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
        this.enumGenerator = enumGenerator;
        this.groupGenerator = groupGenerator;
        this.builderGenerator = builderGenerator;
        this.unknownFieldsGenerator = unknownFieldsGenerator;
        this.oneOfGenerator = oneOfGenerator;
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
        var fields = oneOfGenerator.collectMembers(message);

        addImports(cu, message, fields, typeForm);

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

        if (!cu.getTypes().isEmpty()) {
            addOneOfDeclarations(cu.getType(0), fields);
            generateNestedTypes(cu, cu.getType(0), message, javaPackage, protoFqn);
        }

        return cu;
    }

    /**
     * Generates a nested type declaration for a nested proto message, together with the
     * types nested in it, and adds the imports they need to the compilation unit of the
     * parent type.
     *
     * @param parentType the parent type declaration to add the nested type into
     * @param message the nested proto message statement
     * @param protoFqn the fully-qualified proto name
     */
    public void generateNested(TypeDeclaration<?> parentType, ProtobufMessageStatement message, String protoFqn) {
        var typeForm = resolveTypeForm(protoFqn);
        var typeName = naming.toTypeName(message.name());
        var fields = oneOfGenerator.collectMembers(message);

        TypeDeclaration<?> nestedType = switch (typeForm) {
            case CLASS -> createClassDeclaration(typeName, protoFqn, message, fields);
            case INTERFACE -> createInterfaceDeclaration(typeName, protoFqn, message, fields);
            case RECORD, SEALED_CLASS, SEALED_INTERFACE -> createRecordDeclaration(typeName, protoFqn, message, fields);
        };
        nestedType.addModifier(Modifier.Keyword.STATIC);
        addOneOfDeclarations(nestedType, fields);
        parentType.addMember(nestedType);

        parentType.findCompilationUnit().ifPresent(cu -> {
            var nestedTypeForm = typeForm == TypeForm.CLASS || typeForm == TypeForm.INTERFACE ? typeForm : TypeForm.RECORD;
            addImports(cu, message, fields, nestedTypeForm);
            var javaPackage = cu.getPackageDeclaration()
                    .map(packageDeclaration -> packageDeclaration.getNameAsString())
                    .orElse("");
            generateNestedTypes(cu, nestedType, message, javaPackage, protoFqn);
        });
    }

    private void generateRecord(CompilationUnit cu, ProtobufMessageStatement message,
                                String typeName, String protoFqn,
                                List<MessageMember> fields) {
        var record = createRecordDeclaration(typeName, protoFqn, message, fields);
        record.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(record);
    }

    private RecordDeclaration createRecordDeclaration(String typeName, String protoFqn,
                                                      ProtobufMessageStatement message,
                                                      List<MessageMember> fields) {
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
        oneOfGenerator.implementedInterfaces(message).forEach(record::addImplementedType);
        return record;
    }

    private void generateClass(CompilationUnit cu, ProtobufMessageStatement message,
                               String typeName, String protoFqn,
                               List<MessageMember> fields) {
        var classDecl = createClassDeclaration(typeName, protoFqn, message, fields);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);
        cu.addType(classDecl);
    }

    private ClassOrInterfaceDeclaration createClassDeclaration(String typeName, String protoFqn,
                                                               ProtobufMessageStatement message,
                                                               List<MessageMember> fields) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addAnnotation(createMessageAnnotation(protoFqn, message));
        oneOfGenerator.implementedInterfaces(message).forEach(classDecl::addImplementedType);

        for (var field : fields) {
            classDecl.addMember(propertyGenerator.createField(field));
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
            classDecl.addMember(propertyGenerator.createAccessor(field));
        }

        return classDecl;
    }

    private void generateInterface(CompilationUnit cu, ProtobufMessageStatement message,
                                   String typeName, String protoFqn,
                                   List<MessageMember> fields) {
        var interfaceDecl = createInterfaceDeclaration(typeName, protoFqn, message, fields);
        interfaceDecl.addModifier(Modifier.Keyword.PUBLIC);
        cu.addType(interfaceDecl);
    }

    private ClassOrInterfaceDeclaration createInterfaceDeclaration(String typeName, String protoFqn,
                                                                    ProtobufMessageStatement message,
                                                                    List<MessageMember> fields) {
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
            method.addAnnotation(propertyGenerator.createFieldAnnotation(field));
        }

        return interfaceDecl;
    }

    private void generateSealedClass(CompilationUnit cu, ProtobufMessageStatement message,
                                     String typeName, String protoFqn,
                                     List<MessageMember> fields) {
        var classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(typeName);
        classDecl.setInterface(false);
        classDecl.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED, Modifier.Keyword.ABSTRACT);
        classDecl.addAnnotation(createMessageAnnotation(protoFqn, message));

        for (var field : fields) {
            classDecl.addMember(propertyGenerator.createField(field));
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

        for (var field : fields) {
            classDecl.addMember(propertyGenerator.createAccessor(field));
        }

        var defaultSubtype = "Default" + typeName;

        var subtypeDecl = new ClassOrInterfaceDeclaration();
        subtypeDecl.setName(defaultSubtype);
        subtypeDecl.setInterface(false);
        subtypeDecl.addModifier(Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
        subtypeDecl.addExtendedType(typeName);
        subtypeDecl.addAnnotation(createSubtypeAnnotation(message));

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
                                         List<MessageMember> fields) {
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
            method.addAnnotation(propertyGenerator.createFieldAnnotation(field));
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
        recordDecl.addAnnotation(createSubtypeAnnotation(message));

        interfaceDecl.addMember(recordDecl);
        cu.addType(interfaceDecl);
    }

    /**
     * Adds the sealed interfaces of the {@code oneof} declarations of a message, together
     * with the records that wrap their cases, to the type that declares the message.
     *
     * @param type the type that declares the message
     * @param members the members of the message
     */
    private void addOneOfDeclarations(TypeDeclaration<?> type, List<MessageMember> members) {
        for (var member : members) {
            if (member instanceof MessageMember.OneOf oneOf) {
                type.addMember(oneOf.declaration().clone());
            }
        }
    }

    /**
     * Generates the messages, enums and groups nested in a proto message, adding them as
     * members of the type that declares the message.
     *
     * @param cu the compilation unit that contains the declaring type, which receives
     *           the imports of the nested types
     * @param declaringType the type that declares the message
     * @param message the proto message statement
     * @param javaPackage the target Java package
     * @param parentProtoFqn the fully qualified proto name of the message
     */
    private void generateNestedTypes(CompilationUnit cu, TypeDeclaration<?> declaringType,
                                     ProtobufMessageStatement message,
                                     String javaPackage, String parentProtoFqn) {
        message.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(nested -> {
            var nestedFqn = parentProtoFqn + "." + nested.name();
            generateNested(declaringType, nested, nestedFqn);
        });

        message.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(nested -> {
            var nestedFqn = parentProtoFqn + "." + nested.name();
            enumGenerator.generateNested(declaringType, nested, nestedFqn);
        });

        message.getDirectChildrenByType(ProtobufGroupStatement.class).forEach(group -> {
            var nestedFqn = parentProtoFqn + "." + group.name();
            var groupCu = groupGenerator.generate(group, javaPackage, nestedFqn);
            if (!groupCu.getTypes().isEmpty()) {
                var groupType = groupCu.getType(0);
                groupType.addModifier(Modifier.Keyword.STATIC);
                declaringType.addMember(groupType);
                groupCu.getImports().forEach(cu::addImport);
            }
        });
    }

    private AnnotationExpr createMessageAnnotation(String protoFqn, ProtobufMessageStatement message) {
        var pairs = new ArrayList<MemberValuePair>();

        var version = resolveVersion(message);
        if (version != ProtobufVersion.DEFAULT_VERSION) {
            pairs.add(createVersionPair(version));
        }

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
                                new MemberValuePair("min", new IntegerLiteralExpr(String.valueOf(range[0]))),
                                new MemberValuePair("max", new IntegerLiteralExpr(String.valueOf(range[1])))
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

    private List<Integer> collectReservedIndexes(ProtobufMessageStatement message) {
        var indexes = new ArrayList<Integer>();
        message.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufIntegerExpression intExpr) {
                        indexes.add((int) intExpr.value().value());
                    }
                })
        );
        return indexes;
    }

    private List<int[]> collectReservedRanges(ProtobufMessageStatement message) {
        var ranges = new ArrayList<int[]>();
        message.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufIntegerRangeExpression rangeExpr) {
                        var range = rangeExpr.value();
                        var lower = (int) range.lowerBound().value();
                        var upper = range.upperBound()
                                .map(u -> (int) u.value())
                                .orElse(536_870_911);
                        ranges.add(new int[]{lower, upper});
                    }
                })
        );
        return ranges;
    }

    /**
     * Adds the imports needed by the declaration of a message to a compilation unit.
     *
     * <p>The imports cover the {@code @ProtobufMessage} annotation and its attributes,
     * the declarations and annotations of the fields of the message and, for the type
     * forms that declare accessors, the {@code Optional} types they return.
     *
     * @param cu the compilation unit to add the imports to
     * @param message the proto message statement
     * @param fields the fields of the message
     * @param typeForm the type form used to declare the message
     */
    private void addImports(CompilationUnit cu, ProtobufMessageStatement message,
                            List<MessageMember> fields, TypeForm typeForm) {
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage");

        if (resolveVersion(message) != ProtobufVersion.DEFAULT_VERSION) {
            cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufVersion");
        }

        if (!collectReservedRanges(message).isEmpty()) {
            cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange");
        }

        var hasAccessors = typeForm == TypeForm.CLASS || typeForm == TypeForm.SEALED_CLASS;
        for (var field : fields) {
            propertyGenerator.requiredImports(field).forEach(cu::addImport);
            if (hasAccessors) {
                propertyGenerator.requiredAccessorImports(field).forEach(cu::addImport);
            }
        }
    }

    /**
     * Returns the version or edition of the {@code .proto} file that declares a message.
     *
     * @param message the proto message statement
     * @return the version of the enclosing document
     */
    private ProtobufVersion resolveVersion(ProtobufMessageStatement message) {
        return message.document()
                .version();
    }

    /**
     * Creates the {@code @ProtobufMessage} annotation of the default subtype generated
     * for a sealed message.
     *
     * <p>The subtype inherits the fields of the sealed message, so it declares the same
     * {@code version}, which determines how the presence and encoding of those fields
     * are resolved.
     *
     * @param message the proto message statement
     * @return the annotation expression
     */
    private AnnotationExpr createSubtypeAnnotation(ProtobufMessageStatement message) {
        var version = resolveVersion(message);
        if (version == ProtobufVersion.DEFAULT_VERSION) {
            return new MarkerAnnotationExpr("ProtobufMessage");
        }

        return new NormalAnnotationExpr(
                new Name("ProtobufMessage"),
                new NodeList<>(createVersionPair(version))
        );
    }

    /**
     * Creates the {@code version} attribute of a {@code @ProtobufMessage} annotation.
     *
     * @param version the version or edition of the message
     * @return the attribute
     */
    private MemberValuePair createVersionPair(ProtobufVersion version) {
        return new MemberValuePair("version", new FieldAccessExpr(
                new NameExpr("ProtobufVersion"), version.name()
        ));
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
