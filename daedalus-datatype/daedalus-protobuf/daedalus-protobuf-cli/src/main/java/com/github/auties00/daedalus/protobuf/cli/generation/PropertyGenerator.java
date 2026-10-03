package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufValueTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufUnresolvedTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufPrimitiveTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMessageTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufGroupTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufEnumTypeReference;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufBoolExpression;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufModifier;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOneofStatement;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference;
import com.github.auties00.daedalus.protobuf.model.ProtobufFieldPresence;
import com.github.auties00.daedalus.protobuf.model.ProtobufMessageEncoding;
import com.github.auties00.daedalus.protobuf.model.ProtobufRepeatedFieldEncoding;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufUtf8Validation;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Java declarations of the fields of a protobuf message, together with
 * the typed field annotations, such as {@code @ProtobufMessage.Int32Field}, that
 * describe them.
 *
 * <p>The Java type of a field follows the presence rules of the official Protocol
 * Buffers implementation:
 * <ul>
 * <li>a singular field with explicit presence is declared with a boxed type, so that a
 *     field that is not set, which is {@code null}, can be told apart from a field set
 *     to its default value. A field has explicit presence if it is an {@code optional}
 *     or {@code required} field of proto2, an {@code optional} field of proto3, a member
 *     of a {@code oneof}, a message field, or a field of an edition whose
 *     {@code features.field_presence} is not {@code IMPLICIT};
 * <li>a singular field with implicit presence, which is a proto3 field without a label
 *     or a field of an edition whose {@code features.field_presence} is
 *     {@code IMPLICIT}, is declared with a primitive type when its type has one;
 * <li>a repeated or map field has no presence, and its elements are always boxed.
 * </ul>
 *
 * <p>The {@code fieldPresence}, {@code repeatedFieldEncoding}, {@code messageEncoding}
 * and {@code utf8Validation} attributes of a field annotation are only emitted when they
 * differ from the default of the version or edition of the {@code .proto} file, which
 * the enclosing message declares in its {@code version} attribute. The
 * {@code fieldPresence} attribute is never emitted on a repeated, map or message field:
 * the first two have no presence, and a message field always has explicit presence.
 *
 * <p>The public accessor of a field that can be unset, that is a singular field with
 * explicit presence that is not required, returns a {@link java.util.OptionalInt},
 * {@link java.util.OptionalLong} or {@link java.util.OptionalDouble} for an
 * {@code Integer}, {@code Long} or {@code Double} field, and a {@link java.util.Optional}
 * for any other type. The field itself keeps its nullable type and is package private,
 * so that the generated Spec class reads it directly.
 */
public final class PropertyGenerator {
    /**
     * The fully qualified name of the annotation that encloses the typed field annotations.
     */
    private static final String PROTOBUF_MESSAGE_IMPORT = "com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage";

    /**
     * The type mapper used to resolve the Java type of a field.
     */
    private final TypeMapper typeMapper;

    /**
     * The naming strategy used to resolve the Java name of a field.
     */
    private final NamingStrategy naming;

    /**
     * Constructs a property generator with the given type mapper and naming strategy.
     *
     * @param typeMapper the type mapper for resolving Java types
     * @param naming the naming strategy for field names
     */
    public PropertyGenerator(TypeMapper typeMapper, NamingStrategy naming) {
        this.typeMapper = typeMapper;
        this.naming = naming;
    }

    /**
     * Returns the fields of a message in declaration order, including the members of
     * its {@code oneof} declarations.
     *
     * <p>A member of a {@code oneof} is declared as a regular field of the message with
     * explicit presence, like the official Protocol Buffers implementation does, so that
     * at most one of the members is set.
     *
     * @param message the proto message statement
     * @return a mutable list of the fields of the message
     */
    public List<ProtobufFieldStatement> collectFields(ProtobufMessageStatement message) {
        var fields = new ArrayList<ProtobufFieldStatement>();
        for (var child : message.children()) {
            switch (child) {
                case ProtobufFieldStatement field -> fields.add(field);
                case ProtobufOneofStatement oneof -> oneof.getDirectChildrenByType(ProtobufFieldStatement.class)
                        .forEach(fields::add);
                default -> {
                    // Nested types, options and reserved statements are not fields
                }
            }
        }
        return fields;
    }

    /**
     * Creates the typed field annotation, such as {@code @ProtobufMessage.Int32Field},
     * for a proto field.
     *
     * <p>The annotation always declares the {@code index} of the field, and the
     * {@code mapKeyType} and {@code mapValueType} of a map field. The other attributes
     * are only declared when they differ from the default of the version or edition of
     * the field, as described in the documentation of this class.
     *
     * @param field the proto field statement
     * @return the annotation expression
     */
    public NormalAnnotationExpr createFieldAnnotation(ProtobufFieldStatement field) {
        var pairs = new NodeList<MemberValuePair>();
        pairs.add(new MemberValuePair("index", new IntegerLiteralExpr(
                String.valueOf((int) field.index().value())
        )));

        if (field.type() instanceof ProtobufMapTypeReference map) {
            pairs.add(new MemberValuePair("mapKeyType", new FieldAccessExpr(
                    new NameExpr("ProtobufType"), typeMapper.toMapKeyTypeName(map)
            )));
            pairs.add(new MemberValuePair("mapValueType", new FieldAccessExpr(
                    new NameExpr("ProtobufType"), typeMapper.toMapValueTypeName(map)
            )));
        }

        if (hasFieldPresenceAttribute(field)) {
            pairs.add(new MemberValuePair("fieldPresence", new FieldAccessExpr(
                    new NameExpr("ProtobufFieldPresence"), resolveFieldPresence(field).name()
            )));
        }

        if (hasRepeatedFieldEncodingAttribute(field)) {
            pairs.add(new MemberValuePair("repeatedFieldEncoding", new FieldAccessExpr(
                    new NameExpr("ProtobufRepeatedFieldEncoding"), resolveRepeatedFieldEncoding(field).name()
            )));
        }

        if (hasMessageEncodingAttribute(field)) {
            pairs.add(new MemberValuePair("messageEncoding", new FieldAccessExpr(
                    new NameExpr("ProtobufMessageEncoding"), resolveMessageEncoding(field).name()
            )));
        }

        if (hasUtf8ValidationAttribute(field)) {
            pairs.add(new MemberValuePair("utf8Validation", new FieldAccessExpr(
                    new NameExpr("ProtobufUtf8Validation"), field.utf8Validation().name()
            )));
        }

        return new NormalAnnotationExpr(
                new Name("ProtobufMessage." + resolveAnnotationName(field.type())),
                pairs
        );
    }

    /**
     * Returns the Java type string for a proto field, accounting for repeated
     * (wrapping in {@code List<>}) and map types.
     *
     * <p>A singular field with explicit presence is declared with a boxed type, while a
     * singular field with implicit presence keeps the primitive type, if its type has
     * one.
     *
     * @param field the proto field statement
     * @return the Java type string
     */
    public String resolveJavaType(ProtobufFieldStatement field) {
        var repeated = isRepeated(field);
        var map = field.type() instanceof ProtobufMapTypeReference;
        var boxed = repeated || map || resolveFieldPresence(field) != ProtobufFieldPresence.IMPLICIT;
        var baseType = typeMapper.toJavaType(field.type(), boxed);
        if (repeated && !map) {
            return "List<" + baseType + ">";
        }
        return baseType;
    }

    /**
     * Returns the Java field name for a proto field.
     *
     * @param field the proto field statement
     * @return the Java field name in camelCase
     */
    public String resolveFieldName(ProtobufFieldStatement field) {
        return naming.toFieldName(field.name());
    }

    /**
     * Returns the presence of a proto field, following the rules of the official
     * Protocol Buffers implementation.
     *
     * <p>The presence is resolved as follows:
     * <ul>
     * <li>a repeated or map field has no presence, which is reported as
     *     {@link ProtobufFieldPresence#IMPLICIT}, as an empty field is never written;
     * <li>a member of a {@code oneof} and a message field always have
     *     {@link ProtobufFieldPresence#EXPLICIT} presence;
     * <li>in proto2, a {@code required} field has
     *     {@link ProtobufFieldPresence#LEGACY_REQUIRED} presence, and any other field has
     *     {@link ProtobufFieldPresence#EXPLICIT} presence;
     * <li>in proto3, an {@code optional} field has {@link ProtobufFieldPresence#EXPLICIT}
     *     presence, and any other field has {@link ProtobufFieldPresence#IMPLICIT}
     *     presence;
     * <li>in an edition, the presence is the value of the {@code features.field_presence}
     *     feature of the field, or of its closest enclosing declaration, defaulting to
     *     {@link ProtobufFieldPresence#EXPLICIT}.
     * </ul>
     *
     * @param field the proto field statement
     * @return the presence of the field, never {@link ProtobufFieldPresence#VERSION_DEFAULT}
     */
    public ProtobufFieldPresence resolveFieldPresence(ProtobufFieldStatement field) {
        if (isRepeated(field) || field.type() instanceof ProtobufMapTypeReference) {
            return ProtobufFieldPresence.IMPLICIT;
        }

        if (field.parent() instanceof ProtobufOneofStatement || isMessage(field)) {
            return ProtobufFieldPresence.EXPLICIT;
        }

        return switch (resolveVersion(field)) {
            case PROTOBUF_2 -> field.modifier() == ProtobufModifier.REQUIRED
                    ? ProtobufFieldPresence.LEGACY_REQUIRED
                    : ProtobufFieldPresence.EXPLICIT;
            case PROTOBUF_3 -> field.modifier() == ProtobufModifier.OPTIONAL
                    ? ProtobufFieldPresence.EXPLICIT
                    : ProtobufFieldPresence.IMPLICIT;
            case EDITION_2023, EDITION_2024 -> field.fieldPresence();
        };
    }

    /**
     * Returns whether the value of a proto field can be unset, that is whether the field
     * is a singular field that tracks its presence.
     *
     * <p>A field with explicit presence is not set unless it is on the wire, and a required
     * field may not be set either, by a message that is built or decoded partially. The
     * accessor of such a field returns an {@link java.util.Optional}, or one of its
     * primitive specializations, which is empty when the field is not set. A field with
     * implicit presence, and a repeated or map field, always hold a value.
     *
     * @param field the proto field statement
     * @return {@code true} if the field can be unset, otherwise {@code false}
     */
    public boolean canBeUnset(ProtobufFieldStatement field) {
        return resolveFieldPresence(field) != ProtobufFieldPresence.IMPLICIT;
    }

    /**
     * Returns the return type of the public accessor of a proto field.
     *
     * <p>If the field {@linkplain #canBeUnset(ProtobufFieldStatement) can be unset}, the
     * accessor returns an {@code OptionalInt}, {@code OptionalLong} or
     * {@code OptionalDouble} for an {@code Integer}, {@code Long} or {@code Double} field,
     * and an {@code Optional} of the type of the field otherwise. Any other field is
     * returned with the type of the field.
     *
     * @param field the proto field statement
     * @return the return type of the accessor
     */
    public String resolveAccessorType(ProtobufFieldStatement field) {
        var javaType = resolveJavaType(field);
        if (!canBeUnset(field)) {
            return javaType;
        }

        return switch (javaType) {
            case "Integer" -> "OptionalInt";
            case "Long" -> "OptionalLong";
            case "Double" -> "OptionalDouble";
            default -> "Optional<" + javaType + ">";
        };
    }

    /**
     * Creates the package private field declaration of a proto field, annotated with
     * its {@linkplain #createFieldAnnotation(ProtobufFieldStatement) field annotation}.
     *
     * <p>The field is package private, instead of private, so that the Spec class
     * generated by the annotation processor can read it directly, without going through
     * the public accessor, which may wrap its value in an {@code Optional}.
     *
     * @param field the proto field statement
     * @return the field declaration
     */
    public FieldDeclaration createField(ProtobufFieldStatement field) {
        var fieldDeclaration = new FieldDeclaration(
                new NodeList<>(Modifier.finalModifier()),
                parseType(resolveJavaType(field)),
                resolveFieldName(field)
        );
        fieldDeclaration.addAnnotation(createFieldAnnotation(field));
        return fieldDeclaration;
    }

    /**
     * Creates the public accessor of a proto field, together with its javadoc.
     *
     * <p>The accessor of a field that
     * {@linkplain #canBeUnset(ProtobufFieldStatement) can be unset} wraps the nullable
     * value of the field in an {@code Optional}, or one of its primitive
     * specializations, which is empty when the field is not set, for example:
     * <pre>{@code
     * public OptionalInt start() {
     *     return start == null ? OptionalInt.empty() : OptionalInt.of(start);
     * }
     * }</pre>
     *
     * <p>Any other accessor returns the value of the field as it is.
     *
     * <p>The accessor is not annotated with {@code @ProtobufAccessor}: the field is
     * package private, so the Spec class reads it directly, and an annotated accessor
     * would take precedence over the field even when it returns an {@code Optional}.
     *
     * @param field the proto field statement
     * @return the accessor method declaration
     */
    public MethodDeclaration createAccessor(ProtobufFieldStatement field) {
        var fieldName = resolveFieldName(field);
        var accessorType = resolveAccessorType(field);

        var accessor = new MethodDeclaration(
                new NodeList<>(Modifier.publicModifier()),
                parseType(accessorType),
                fieldName
        );
        accessor.setBody(new BlockStmt().addStatement(new ReturnStmt(createAccessorExpression(field, fieldName, accessorType))));

        var protoName = "{@code " + field.name() + "}";
        if (canBeUnset(field)) {
            var wrapperName = accessorType.startsWith("Optional<") ? "Optional" : accessorType;
            var javadoc = new Javadoc(JavadocDescription.parseText("Returns the value of the " + protoName + " field, if it is set."));
            javadoc.addBlockTag("return", "an {@code " + wrapperName + "} containing the value of the field, or an empty {@code " + wrapperName + "} if the field is not set");
            accessor.setJavadocComment(javadoc);
        } else {
            var javadoc = new Javadoc(JavadocDescription.parseText("Returns the value of the " + protoName + " field."));
            javadoc.addBlockTag("return", "the value of the field");
            accessor.setJavadocComment(javadoc);
        }

        return accessor;
    }

    /**
     * Creates a record component {@link Parameter} for this field,
     * with the field annotation applied.
     *
     * <p>The accessor of a record component always returns the type of the component,
     * so the component of a field that can be unset is declared with the nullable boxed
     * type of the field instead of an {@code Optional}.
     *
     * @param field the proto field statement
     * @return the annotated parameter for a record component
     */
    public Parameter createRecordComponent(ProtobufFieldStatement field) {
        var param = new Parameter(
                parseType(resolveJavaType(field)),
                resolveFieldName(field)
        );
        param.addAnnotation(createFieldAnnotation(field));
        return param;
    }

    /**
     * Returns the list of imports needed by the declaration of a proto field and by its
     * field annotation.
     *
     * @param field the proto field statement
     * @return the list of fully qualified class names to import
     */
    public List<String> requiredImports(ProtobufFieldStatement field) {
        var imports = new ArrayList<String>();
        imports.add(PROTOBUF_MESSAGE_IMPORT);

        if (field.type() instanceof ProtobufMapTypeReference) {
            imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufType");
            imports.add("java.util.Map");
        } else if (isRepeated(field)) {
            imports.add("java.util.List");
        }

        if (hasFieldPresenceAttribute(field)) {
            imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufFieldPresence");
        }

        if (hasRepeatedFieldEncodingAttribute(field)) {
            imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufRepeatedFieldEncoding");
        }

        if (hasMessageEncodingAttribute(field)) {
            imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufMessageEncoding");
        }

        if (hasUtf8ValidationAttribute(field)) {
            imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufUtf8Validation");
        }

        return imports;
    }

    /**
     * Returns the list of imports needed by the
     * {@linkplain #createAccessor(ProtobufFieldStatement) accessor} of a proto field.
     *
     * @param field the proto field statement
     * @return the list of fully qualified class names to import, which is empty if the
     *         accessor does not wrap the value of the field
     */
    public List<String> requiredAccessorImports(ProtobufFieldStatement field) {
        if (!canBeUnset(field)) {
            return List.of();
        }

        var accessorType = resolveAccessorType(field);
        var wrapperName = accessorType.startsWith("Optional<") ? "Optional" : accessorType;
        return List.of("java.util." + wrapperName);
    }

    /**
     * Returns the Java name of a member of a message.
     *
     * @param member the member
     * @return the name of the field that holds the member
     */
    public String resolveFieldName(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> resolveFieldName(field.field());
            case MessageMember.OneOf oneOf -> oneOf.fieldName();
        };
    }

    /**
     * Returns the Java type of a member of a message.
     *
     * @param member the member
     * @return the type of the field that holds the member, which is the sealed interface
     *         of a oneof
     */
    public String resolveJavaType(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> resolveJavaType(field.field());
            case MessageMember.OneOf oneOf -> oneOf.typeName();
        };
    }

    /**
     * Creates the annotation of a member of a message.
     *
     * @param member the member
     * @return the field annotation of a field, or the {@code @ProtobufMessage.OneOfField}
     *         annotation of a oneof
     */
    public NormalAnnotationExpr createFieldAnnotation(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> createFieldAnnotation(field.field());
            case MessageMember.OneOf oneOf -> oneOf.annotation().clone();
        };
    }

    /**
     * Creates the record component of a member of a message.
     *
     * <p>The component of a oneof has the type of its sealed interface, and is
     * {@code null} if no case is set.
     *
     * @param member the member
     * @return the annotated record component
     */
    public Parameter createRecordComponent(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> createRecordComponent(field.field());
            case MessageMember.OneOf oneOf -> {
                var parameter = new Parameter(parseType(oneOf.typeName()), oneOf.fieldName());
                parameter.addAnnotation(oneOf.annotation().clone());
                yield parameter;
            }
        };
    }

    /**
     * Creates the package private field declaration of a member of a message.
     *
     * @param member the member
     * @return the annotated field declaration
     */
    public FieldDeclaration createField(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> createField(field.field());
            case MessageMember.OneOf oneOf -> {
                var fieldDeclaration = new FieldDeclaration(
                        new NodeList<>(Modifier.finalModifier()),
                        parseType(oneOf.typeName()),
                        oneOf.fieldName()
                );
                fieldDeclaration.addAnnotation(oneOf.annotation().clone());
                yield fieldDeclaration;
            }
        };
    }

    /**
     * Creates the public accessor of a member of a message.
     *
     * <p>The accessor of a oneof returns an {@code Optional} of its sealed interface, which
     * is empty if no case is set.
     *
     * @param member the member
     * @return the accessor method declaration
     */
    public MethodDeclaration createAccessor(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> createAccessor(field.field());
            case MessageMember.OneOf oneOf -> {
                var accessor = new MethodDeclaration(
                        new NodeList<>(Modifier.publicModifier()),
                        parseType("Optional<" + oneOf.typeName() + ">"),
                        oneOf.fieldName()
                );
                accessor.setBody(new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(
                        new NameExpr("Optional"), "ofNullable", new NodeList<>(new NameExpr(oneOf.fieldName()))
                ))));
                var javadoc = new Javadoc(JavadocDescription.parseText("Returns the case of the {@code " + oneOf.statement().name() + "} oneof that is set, if any."));
                javadoc.addBlockTag("return", "an {@code Optional} containing the case that is set, or an empty {@code Optional} if no case is set");
                accessor.setJavadocComment(javadoc);
                yield accessor;
            }
        };
    }

    /**
     * Returns the imports needed by the declaration of a member of a message.
     *
     * @param member the member
     * @return the fully qualified class names to import
     */
    public List<String> requiredImports(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> requiredImports(field.field());
            case MessageMember.OneOf oneOf -> oneOf.imports();
        };
    }

    /**
     * Returns the imports needed by the {@linkplain #createAccessor(MessageMember) accessor}
     * of a member of a message.
     *
     * @param member the member
     * @return the fully qualified class names to import
     */
    public List<String> requiredAccessorImports(MessageMember member) {
        return switch (member) {
            case MessageMember.Field field -> requiredAccessorImports(field.field());
            case MessageMember.OneOf _ -> List.of("java.util.Optional");
        };
    }

    /**
     * Returns the version or edition of the {@code .proto} file that declares a field.
     *
     * @param field the proto field statement
     * @return the version of the enclosing document
     */
    private ProtobufVersion resolveVersion(ProtobufFieldStatement field) {
        return field.parent()
                .document()
                .version();
    }

    /**
     * Returns whether a proto field is declared with the {@code repeated} label.
     *
     * @param field the proto field statement
     * @return {@code true} if the field is repeated, otherwise {@code false}
     */
    private boolean isRepeated(ProtobufFieldStatement field) {
        return field.modifier() == ProtobufModifier.REPEATED;
    }

    /**
     * Returns whether the type of a proto field is a message or a group.
     *
     * @param field the proto field statement
     * @return {@code true} if the field holds a message, otherwise {@code false}
     */
    private boolean isMessage(ProtobufFieldStatement field) {
        return field.type() instanceof ProtobufMessageTypeReference || field.type() instanceof ProtobufGroupTypeReference;
    }

    /**
     * Returns whether the field annotation of a proto field must declare the
     * {@code fieldPresence} attribute.
     *
     * <p>The attribute is declared when the presence of a singular field that does not
     * hold a message differs from the default presence of its version. A repeated or map
     * field has no presence, and a message field always has explicit presence, so the
     * attribute is never declared for them.
     *
     * @param field the proto field statement
     * @return {@code true} if the attribute must be declared, otherwise {@code false}
     */
    private boolean hasFieldPresenceAttribute(ProtobufFieldStatement field) {
        if (isRepeated(field) || field.type() instanceof ProtobufMapTypeReference || isMessage(field)) {
            return false;
        }

        var defaultPresence = ProtobufFieldPresence.VERSION_DEFAULT.resolve(resolveVersion(field));
        return resolveFieldPresence(field) != defaultPresence;
    }

    /**
     * Returns the encoding of a repeated proto field.
     *
     * <p>In proto2 a repeated field is expanded, unless it declares
     * {@code [packed = true]}, and in proto3 it is packed, unless it declares
     * {@code [packed = false]}. In an edition, the encoding is the value of the
     * {@code features.repeated_field_encoding} feature of the field, or of its closest
     * enclosing declaration, defaulting to {@link ProtobufRepeatedFieldEncoding#PACKED}.
     *
     * @param field the proto field statement
     * @return the encoding of the field, never {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
     */
    private ProtobufRepeatedFieldEncoding resolveRepeatedFieldEncoding(ProtobufFieldStatement field) {
        return switch (resolveVersion(field)) {
            case PROTOBUF_2 -> isPackedOption(field, false)
                    ? ProtobufRepeatedFieldEncoding.PACKED
                    : ProtobufRepeatedFieldEncoding.EXPANDED;
            case PROTOBUF_3 -> isPackedOption(field, true)
                    ? ProtobufRepeatedFieldEncoding.PACKED
                    : ProtobufRepeatedFieldEncoding.EXPANDED;
            case EDITION_2023, EDITION_2024 -> field.repeatedFieldEncoding();
        };
    }

    /**
     * Returns the value of the {@code packed} option of a proto field.
     *
     * @param field the proto field statement
     * @param defaultValue the value to return if the field does not declare the option
     * @return the value of the option, or {@code defaultValue} if it is not declared
     */
    private boolean isPackedOption(ProtobufFieldStatement field, boolean defaultValue) {
        return field.getOption("packed")
                .map(option -> option.value() instanceof ProtobufBoolExpression(var packed) ? packed : defaultValue)
                .orElse(defaultValue);
    }

    /**
     * Returns whether the field annotation of a proto field must declare the
     * {@code repeatedFieldEncoding} attribute.
     *
     * <p>The attribute is only declared for a repeated field whose type can be packed,
     * when its encoding differs from the default encoding of its version.
     *
     * @param field the proto field statement
     * @return {@code true} if the attribute must be declared, otherwise {@code false}
     */
    private boolean hasRepeatedFieldEncodingAttribute(ProtobufFieldStatement field) {
        if (!isRepeated(field) || !(field.type() instanceof ProtobufValueTypeReference valueType) || !valueType.protobufType().isPackable()) {
            return false;
        }

        var defaultEncoding = ProtobufRepeatedFieldEncoding.VERSION_DEFAULT.resolve(resolveVersion(field));
        return resolveRepeatedFieldEncoding(field) != defaultEncoding;
    }

    /**
     * Returns the wire encoding of a message field.
     *
     * <p>A group is always delimited, while the encoding of a message field is the value
     * of its {@code features.message_encoding} feature, or of its closest enclosing
     * declaration, defaulting to {@link ProtobufMessageEncoding#LENGTH_PREFIXED}.
     *
     * @param field the proto field statement
     * @return the encoding of the field, never {@link ProtobufMessageEncoding#VERSION_DEFAULT}
     */
    ProtobufMessageEncoding resolveMessageEncoding(ProtobufFieldStatement field) {
        if (field.type() instanceof ProtobufGroupTypeReference) {
            return ProtobufMessageEncoding.DELIMITED;
        }

        return field.messageEncoding();
    }

    /**
     * Returns whether the field annotation of a proto field must declare the
     * {@code messageEncoding} attribute.
     *
     * <p>The attribute is only declared for a message field whose encoding differs from
     * the default encoding of its version.
     *
     * @param field the proto field statement
     * @return {@code true} if the attribute must be declared, otherwise {@code false}
     */
    boolean hasMessageEncodingAttribute(ProtobufFieldStatement field) {
        if (!isMessage(field)) {
            return false;
        }

        var defaultEncoding = ProtobufMessageEncoding.VERSION_DEFAULT.resolve(resolveVersion(field));
        return resolveMessageEncoding(field) != defaultEncoding;
    }

    /**
     * Returns whether the field annotation of a proto field must declare the
     * {@code utf8Validation} attribute.
     *
     * <p>The attribute is only declared for a string or map field whose
     * {@code features.utf8_validation} feature differs from the default validation of
     * its version.
     *
     * @param field the proto field statement
     * @return {@code true} if the attribute must be declared, otherwise {@code false}
     */
    boolean hasUtf8ValidationAttribute(ProtobufFieldStatement field) {
        var string = field.type() instanceof ProtobufPrimitiveTypeReference primitive && primitive.protobufType() == ProtobufType.STRING;
        if (!string && !(field.type() instanceof ProtobufMapTypeReference)) {
            return false;
        }

        var defaultValidation = ProtobufUtf8Validation.VERSION_DEFAULT.resolve(resolveVersion(field));
        return field.utf8Validation() != defaultValidation;
    }

    /**
     * Returns the simple name of the field annotation, nested in
     * {@code @ProtobufMessage}, that describes a field of the given type.
     *
     * <p>A group, and a type that could not be resolved, are described by a
     * {@code MessageField}, as both refer to a message type.
     *
     * @param typeReference the type of the field
     * @return the simple name of the field annotation
     */
    private String resolveAnnotationName(ProtobufTypeReference typeReference) {
        return switch (typeReference) {
            case ProtobufMapTypeReference _ -> "MapField";
            case ProtobufEnumTypeReference _ -> "EnumField";
            case ProtobufPrimitiveTypeReference primitive -> resolveAnnotationName(primitive.protobufType());
            case ProtobufMessageTypeReference _, ProtobufGroupTypeReference _, ProtobufUnresolvedTypeReference _ -> "MessageField";
        };
    }

    /**
     * Returns the simple name of the field annotation, nested in
     * {@code @ProtobufMessage}, that describes a field of the given scalar type,
     * {@code string} or {@code bytes}.
     *
     * @param type the protobuf type of the field
     * @return the simple name of the field annotation
     */
    private String resolveAnnotationName(ProtobufType type) {
        return switch (type) {
            case FLOAT -> "FloatField";
            case DOUBLE -> "DoubleField";
            case BOOL -> "BoolField";
            case INT32 -> "Int32Field";
            case SINT32 -> "Sint32Field";
            case UINT32 -> "Uint32Field";
            case FIXED32 -> "Fixed32Field";
            case SFIXED32 -> "Sfixed32Field";
            case INT64 -> "Int64Field";
            case SINT64 -> "Sint64Field";
            case UINT64 -> "Uint64Field";
            case FIXED64 -> "Fixed64Field";
            case SFIXED64 -> "Sfixed64Field";
            case ENUM -> "EnumField";
            case STRING -> "StringField";
            case BYTES -> "BytesField";
            case MESSAGE -> "MessageField";
        };
    }

    /**
     * Creates the expression returned by the accessor of a proto field.
     *
     * <p>The value of a singular {@code bytes} field is an array, which is copied, so that
     * the caller cannot modify the message through it.
     *
     * @param field the proto field statement
     * @param fieldName the Java name of the field
     * @param accessorType the return type of the accessor
     * @return the expression returned by the accessor
     */
    private Expression createAccessorExpression(ProtobufFieldStatement field, String fieldName, String accessorType) {
        var copied = field.type() instanceof ProtobufPrimitiveTypeReference primitive && primitive.protobufType() == ProtobufType.BYTES && !isRepeated(field);
        Expression value = copied ? new MethodCallExpr(new NameExpr(fieldName), "clone") : new NameExpr(fieldName);
        if (!canBeUnset(field)) {
            return value;
        }

        if (accessorType.startsWith("Optional<")) {
            if (copied) {
                return new ConditionalExpr(
                        new BinaryExpr(new NameExpr(fieldName), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                        new MethodCallExpr(new NameExpr("Optional"), "empty"),
                        new MethodCallExpr(new NameExpr("Optional"), "of", new NodeList<>(value))
                );
            }
            return new MethodCallExpr(new NameExpr("Optional"), "ofNullable", new NodeList<>(new NameExpr(fieldName)));
        }

        return new ConditionalExpr(
                new BinaryExpr(new NameExpr(fieldName), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                new MethodCallExpr(new NameExpr(accessorType), "empty"),
                new MethodCallExpr(new NameExpr(accessorType), "of", new NodeList<>(new NameExpr(fieldName)))
        );
    }

    /**
     * Parses a Java type name into a JavaParser {@link Type}.
     *
     * @param typeName the Java type name
     * @return the parsed type
     */
    private Type parseType(String typeName) {
        return switch (typeName) {
            case "int" -> PrimitiveType.intType();
            case "long" -> PrimitiveType.longType();
            case "float" -> PrimitiveType.floatType();
            case "double" -> PrimitiveType.doubleType();
            case "boolean" -> PrimitiveType.booleanType();
            case "byte[]" -> StaticJavaParser.parseType("byte[]");
            default -> typeName.contains("<")
                    ? StaticJavaParser.parseType(typeName)
                    : new ClassOrInterfaceType(null, typeName);
        };
    }
}
