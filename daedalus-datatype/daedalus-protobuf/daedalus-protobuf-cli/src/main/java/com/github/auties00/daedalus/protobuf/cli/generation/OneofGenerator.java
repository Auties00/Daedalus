package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufValueTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufPrimitiveTypeReference;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufDocumentTree;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufGroupStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOneofStatement;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMessageTypeReference;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generates the Java declarations of the {@code oneof} declarations of the proto messages.
 *
 * <p>A {@code oneof} is held by a single field annotated with
 * {@code @ProtobufMessage.OneOfField}, whose type is a sealed interface nested in the
 * message and named after the {@code oneof}. Each member of the {@code oneof} is a case,
 * held by a permitted subtype of the interface:
 * <ul>
 * <li>a message member is held by the message itself, which implements the interface, if
 *     the message is generated from the same set of {@code .proto} files, in the same
 *     Java package, as a final class or a record, is not the message that declares the
 *     {@code oneof} or one that encloses it, and is the type of no other member of the
 *     same {@code oneof};
 * <li>any other member is held by a record nested in the interface, which wraps the
 *     value of the member and converts it with {@code @TypeSerializer} and
 *     {@code @TypeDeserializer}.
 * </ul>
 *
 * <p>The implementing messages must be known before any message is generated, so
 * {@link #prepare(Collection)} must be called with every document before the members of
 * a message are {@linkplain #collectMembers(ProtobufMessageStatement) collected}.
 */
public final class OneOfGenerator {
    /**
     * The fully qualified name of the annotation that encloses the oneof annotation.
     */
    private static final String PROTOBUF_MESSAGE_IMPORT = "com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage";

    /**
     * The fully qualified name of the annotation of the serializer of a wrapper record.
     */
    private static final String TYPE_SERIALIZER_IMPORT = "com.github.auties00.daedalus.typesystem.annotation.TypeSerializer";

    /**
     * The fully qualified name of the annotation of the deserializer of a wrapper record.
     */
    private static final String TYPE_DESERIALIZER_IMPORT = "com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer";

    /**
     * The name of the component that holds the value of a wrapper record.
     */
    private static final String WRAPPER_VALUE = "value";

    /**
     * The schema configuration.
     */
    private final SchemaConfig config;

    /**
     * The naming strategy.
     */
    private final NamingStrategy naming;

    /**
     * The type mapper used to resolve the Java type of a member.
     */
    private final TypeMapper typeMapper;

    /**
     * The property generator used to resolve the attributes of a member.
     */
    private final PropertyGenerator propertyGenerator;

    /**
     * The members whose message implements the interface of their oneof.
     */
    private final Set<ProtobufFieldStatement> implementingMembers;

    /**
     * The interfaces that each message implements, as names that resolve in its package.
     */
    private final Map<ProtobufMessageStatement, List<String>> implementedInterfaces;

    /**
     * Constructs a oneof generator.
     *
     * @param config the schema configuration
     * @param naming the naming strategy
     * @param typeMapper the type mapper
     * @param propertyGenerator the property generator
     */
    public OneOfGenerator(SchemaConfig config, NamingStrategy naming, TypeMapper typeMapper, PropertyGenerator propertyGenerator) {
        this.config = config;
        this.naming = naming;
        this.typeMapper = typeMapper;
        this.propertyGenerator = propertyGenerator;
        this.implementingMembers = Collections.newSetFromMap(new IdentityHashMap<>());
        this.implementedInterfaces = new IdentityHashMap<>();
    }

    /**
     * Decides which message members of the oneofs of the given documents are held by the
     * message itself, which then implements the interface of the oneof.
     *
     * @param documents every document that is generated
     */
    public void prepare(Collection<ProtobufDocumentTree> documents) {
        implementingMembers.clear();
        implementedInterfaces.clear();
        var generated = Collections.newSetFromMap(new IdentityHashMap<ProtobufDocumentTree, Boolean>());
        generated.addAll(documents);
        for (var document : documents) {
            document.getDirectChildrenByType(ProtobufMessageStatement.class)
                    .forEach(message -> prepare(message, generated));
        }
    }

    /**
     * Decides which message members of the oneofs of the given message, and of the
     * messages nested in it, are held by the message itself.
     *
     * @param owner the message
     * @param generated the documents that are generated
     */
    private void prepare(ProtobufMessageStatement owner, Set<ProtobufDocumentTree> generated) {
        owner.getDirectChildrenByType(ProtobufOneofStatement.class).forEach(oneof -> {
            var members = oneof.getDirectChildrenByType(ProtobufFieldStatement.class).toList();
            var interfaceName = resolveInterfaceName(owner, oneof);
            for (var member : members) {
                if (!(member.type() instanceof ProtobufMessageTypeReference reference) || !canImplement(owner, reference.declaration(), generated)) {
                    continue;
                }

                var sameType = members.stream()
                        .filter(other -> other.type() instanceof ProtobufMessageTypeReference otherReference && otherReference.declaration() == reference.declaration())
                        .count();
                if (sameType != 1) {
                    continue;
                }

                implementingMembers.add(member);
                implementedInterfaces.computeIfAbsent(reference.declaration(), _ -> new ArrayList<>())
                        .add(resolveJavaPath(owner) + "." + interfaceName);
            }
        });
        owner.getDirectChildrenByType(ProtobufMessageStatement.class)
                .forEach(nested -> prepare(nested, generated));
    }

    /**
     * Returns whether the given message can implement the interface of a oneof declared by
     * the given owner.
     *
     * <p>A sealed interface can only permit a type of the same package, unless both are in
     * a named module, which the generator cannot know. A message that declares the oneof,
     * or that encloses the message that does, cannot implement the interface, as its
     * supertypes would depend on its own members.
     *
     * @param owner the message that declares the oneof
     * @param message the message of a member of the oneof
     * @param generated the documents that are generated
     * @return {@code true} if the message can implement the interface
     */
    private boolean canImplement(ProtobufMessageStatement owner, ProtobufMessageStatement message, Set<ProtobufDocumentTree> generated) {
        if (message == null || !generated.contains(message.document())) {
            return false;
        }

        if (!Objects.equals(resolveJavaPackage(message.document()), resolveJavaPackage(owner.document()))) {
            return false;
        }

        var form = resolveTypeForm(resolveProtoFqn(message));
        var nested = message.parent() instanceof ProtobufMessageStatement;
        var effectiveForm = nested && (form == TypeForm.SEALED_CLASS || form == TypeForm.SEALED_INTERFACE) ? TypeForm.RECORD : form;
        if (effectiveForm != TypeForm.RECORD && effectiveForm != TypeForm.CLASS) {
            return false;
        }

        for (var current = (Object) owner; current instanceof ProtobufMessageStatement statement; current = statement.parent()) {
            if (statement == message) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the interfaces that the given message implements, because it holds a case of
     * the oneof of another message.
     *
     * @param message the message
     * @return the names of the interfaces, which resolve in the package of the message
     */
    public List<String> implementedInterfaces(ProtobufMessageStatement message) {
        return implementedInterfaces.getOrDefault(message, List.of());
    }

    /**
     * Returns the members of the given message, which are its fields and its oneofs,
     * ordered by index.
     *
     * @param message the proto message statement
     * @return a mutable list of the members of the message
     */
    public List<MessageMember> collectMembers(ProtobufMessageStatement message) {
        var members = new ArrayList<MessageMember>();
        for (var child : message.children()) {
            switch (child) {
                case ProtobufFieldStatement field -> members.add(new MessageMember.Field(field));
                case ProtobufOneofStatement oneof -> {
                    var member = createOneOf(message, oneof);
                    if (member != null) {
                        members.add(member);
                    }
                }
                default -> {
                    // Nested types, options and reserved statements are not members
                }
            }
        }
        members.sort(Comparator.comparingInt(MessageMember::index));
        return members;
    }

    /**
     * Creates the member that holds the given oneof.
     *
     * @param owner the message that declares the oneof
     * @param oneof the oneof
     * @return the member, or {@code null} if the oneof has no field member
     */
    private MessageMember.OneOf createOneOf(ProtobufMessageStatement owner, ProtobufOneofStatement oneof) {
        var members = oneof.getDirectChildrenByType(ProtobufFieldStatement.class).toList();
        if (members.isEmpty()) {
            return null;
        }

        var interfaceName = resolveInterfaceName(owner, oneof);
        var fieldName = naming.toFieldName(oneof.name());
        var imports = new LinkedHashSet<String>();
        imports.add(PROTOBUF_MESSAGE_IMPORT);

        var declaration = new ClassOrInterfaceDeclaration();
        declaration.setName(interfaceName);
        declaration.setInterface(true);
        declaration.addModifier(Modifier.Keyword.PUBLIC, Modifier.Keyword.SEALED);
        declaration.setJavadocComment(new Javadoc(JavadocDescription.parseText("The cases of the {@code " + oneof.name() + "} oneof.")));

        var cases = new LinkedHashMap<String, NodeList<Expression>>();
        var permitted = new NodeList<ClassOrInterfaceType>();
        var lowestIndex = Integer.MAX_VALUE;
        for (var member : members.stream().sorted(Comparator.comparingLong(member -> member.index().value())).toList()) {
            var index = (int) member.index().value();
            lowestIndex = Math.min(lowestIndex, index);

            String caseType;
            String caseSimpleName;
            if (implementingMembers.contains(member)) {
                caseType = typeMapper.toJavaType(member.type(), false);
                caseSimpleName = caseType;
            } else {
                var record = createWrapper(interfaceName, oneof, member);
                declaration.addMember(record);
                caseSimpleName = record.getNameAsString();
                caseType = interfaceName + "." + caseSimpleName;
                imports.add(TYPE_SERIALIZER_IMPORT);
                imports.add(TYPE_DESERIALIZER_IMPORT);
            }
            permitted.add(StaticJavaParser.parseClassOrInterfaceType(caseType));

            var pairs = new NodeList<MemberValuePair>();
            pairs.add(new MemberValuePair("index", new IntegerLiteralExpr(String.valueOf(index))));
            if (!naming.toSnakeCase(caseSimpleName).equals(member.name())) {
                pairs.add(new MemberValuePair("name", new StringLiteralExpr(member.name())));
            }
            pairs.add(new MemberValuePair("type", new ClassExpr(StaticJavaParser.parseClassOrInterfaceType(caseType))));
            if (propertyGenerator.hasUtf8ValidationAttribute(member)) {
                pairs.add(new MemberValuePair("utf8Validation", new FieldAccessExpr(new NameExpr("ProtobufUtf8Validation"), member.utf8Validation().name())));
                imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufUtf8Validation");
            }
            if (propertyGenerator.hasMessageEncodingAttribute(member)) {
                pairs.add(new MemberValuePair("messageEncoding", new FieldAccessExpr(new NameExpr("ProtobufMessageEncoding"), propertyGenerator.resolveMessageEncoding(member).name())));
                imports.add("com.github.auties00.daedalus.protobuf.model.ProtobufMessageEncoding");
            }

            var kind = member.type() instanceof ProtobufValueTypeReference valueType ? resolveCaseKind(valueType.protobufType()) : "message";
            cases.computeIfAbsent(kind, _ -> new NodeList<>())
                    .add(new NormalAnnotationExpr(new Name("ProtobufMessage.OneOfField." + Character.toUpperCase(kind.charAt(0)) + kind.substring(1) + "Case"), pairs));
        }
        declaration.setPermittedTypes(permitted);

        var annotationPairs = new NodeList<MemberValuePair>();
        if (!naming.toSnakeCase(fieldName).equals(oneof.name())) {
            annotationPairs.add(new MemberValuePair("name", new StringLiteralExpr(oneof.name())));
        }
        for (var entry : cases.entrySet()) {
            var values = entry.getValue();
            annotationPairs.add(new MemberValuePair(entry.getKey() + "Cases", values.size() == 1 ? values.get(0) : new ArrayInitializerExpr(values)));
        }
        var annotation = new NormalAnnotationExpr(new Name("ProtobufMessage.OneOfField"), annotationPairs);
        return new MessageMember.OneOf(oneof, lowestIndex, fieldName, interfaceName, annotation, declaration, List.copyOf(imports));
    }

    /**
     * Creates the record, nested in the interface of a oneof, that wraps the value of a
     * member that is not held by a message implementing the interface.
     *
     * <p>The record converts its value with an accessor annotated with
     * {@code @TypeSerializer} and a factory annotated with {@code @TypeDeserializer}. The
     * accessor of a {@code bytes} value returns a copy of the array, so that the caller
     * cannot modify the case through it, while the serializer is a package private method
     * that returns the array itself, so that writing the case does not copy it. For
     * example:
     * <pre>{@code
     * record Text(String value) implements Value {
     *     @TypeSerializer
     *     public String value() {
     *         return value;
     *     }
     *
     *     @TypeDeserializer
     *     static Text of(String value) {
     *         return new Text(value);
     *     }
     * }
     * }</pre>
     *
     * @param interfaceName the name of the interface of the oneof
     * @param oneof the oneof
     * @param member the member
     * @return the record
     */
    private RecordDeclaration createWrapper(String interfaceName, ProtobufOneofStatement oneof, ProtobufFieldStatement member) {
        var valueType = typeMapper.toJavaType(member.type(), false);
        var name = naming.toTypeName(member.name());
        if (name.equals(interfaceName) || name.equals(valueType)) {
            name += "Case";
        }

        var record = new RecordDeclaration(
                new NodeList<>(),
                new NodeList<>(),
                new SimpleName(name),
                new NodeList<>(new Parameter(StaticJavaParser.parseType(valueType), WRAPPER_VALUE)),
                new NodeList<>(),
                new NodeList<>(StaticJavaParser.parseClassOrInterfaceType(interfaceName)),
                new NodeList<>(),
                null
        );
        record.setJavadocComment(new Javadoc(JavadocDescription.parseText("The {@code " + member.name() + "} case of the {@code " + oneof.name() + "} oneof.")));

        var accessor = record.addMethod(WRAPPER_VALUE, Modifier.Keyword.PUBLIC);
        accessor.setType(valueType);
        if (member.type() instanceof ProtobufPrimitiveTypeReference primitive && primitive.protobufType() == ProtobufType.BYTES) {
            accessor.setBody(new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(WRAPPER_VALUE), "clone"))));
            var serializer = record.addMethod("serialize");
            serializer.setType(valueType);
            serializer.addAnnotation(new MarkerAnnotationExpr("TypeSerializer"));
            serializer.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr(WRAPPER_VALUE))));
        } else {
            accessor.addAnnotation(new MarkerAnnotationExpr("TypeSerializer"));
            accessor.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr(WRAPPER_VALUE))));
        }

        var deserializer = record.addMethod("of", Modifier.Keyword.STATIC);
        deserializer.setType(name);
        deserializer.addParameter(valueType, WRAPPER_VALUE);
        deserializer.addAnnotation(new MarkerAnnotationExpr("TypeDeserializer"));
        deserializer.setBody(new BlockStmt().addStatement(new ReturnStmt(new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(name), new NodeList<>(new NameExpr(WRAPPER_VALUE))))));
        return record;
    }

    /**
     * Returns the name of the interface of the given oneof, which is the name of the oneof
     * in {@code PascalCase}, followed by {@code OneOf} if it would clash with the name of
     * the message or of one of its nested types.
     *
     * @param owner the message that declares the oneof
     * @param oneof the oneof
     * @return the name of the interface
     */
    private String resolveInterfaceName(ProtobufMessageStatement owner, ProtobufOneofStatement oneof) {
        var name = naming.toTypeName(oneof.name());
        var taken = new LinkedHashSet<String>();
        taken.add(naming.toTypeName(owner.name()));
        owner.getDirectChildrenByType(ProtobufMessageStatement.class).forEach(nested -> taken.add(naming.toTypeName(nested.name())));
        owner.getDirectChildrenByType(ProtobufEnumStatement.class).forEach(nested -> taken.add(naming.toTypeName(nested.name())));
        owner.getDirectChildrenByType(ProtobufGroupStatement.class).forEach(nested -> taken.add(naming.toTypeName(nested.name())));
        return taken.contains(name) ? name + "OneOf" : name;
    }

    /**
     * Returns the prefix of the attribute of {@code @ProtobufMessage.OneOfField} that
     * holds the cases of the given type, such as {@code int32} for {@code int32Cases}.
     *
     * @param type the protobuf type of a member
     * @return the prefix of the attribute
     */
    private static String resolveCaseKind(ProtobufType type) {
        return switch (type) {
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case BOOL -> "bool";
            case INT32 -> "int32";
            case SINT32 -> "sint32";
            case UINT32 -> "uint32";
            case FIXED32 -> "fixed32";
            case SFIXED32 -> "sfixed32";
            case INT64 -> "int64";
            case SINT64 -> "sint64";
            case UINT64 -> "uint64";
            case FIXED64 -> "fixed64";
            case SFIXED64 -> "sfixed64";
            case ENUM -> "enum";
            case STRING -> "string";
            case BYTES -> "bytes";
            default -> "message";
        };
    }

    /**
     * Returns the name of the given message relative to its package, which is the names
     * of the messages that enclose it, followed by its own.
     *
     * @param message the message
     * @return the dotted Java names of the message and of its enclosing messages
     */
    private String resolveJavaPath(ProtobufMessageStatement message) {
        var names = new ArrayList<String>();
        for (var current = (Object) message; current instanceof ProtobufMessageStatement statement; current = statement.parent()) {
            names.addFirst(naming.toTypeName(statement.name()));
        }
        return String.join(".", names);
    }

    /**
     * Returns the fully qualified proto name of the given message.
     *
     * @param message the message
     * @return the proto package, followed by the names of the enclosing messages and of
     *         the message
     */
    private String resolveProtoFqn(ProtobufMessageStatement message) {
        var names = new ArrayList<String>();
        for (var current = (Object) message; current instanceof ProtobufMessageStatement statement; current = statement.parent()) {
            names.addFirst(statement.name());
        }
        message.document().packageName().ifPresent(packageName -> names.addFirst(packageName));
        return String.join(".", names);
    }

    /**
     * Returns the Java package that the given document is generated in.
     *
     * @param document the document
     * @return the Java package, which may be {@code null} or empty
     */
    private String resolveJavaPackage(ProtobufDocumentTree document) {
        return config.resolveJavaPackage(document.packageName().orElse(null));
    }

    /**
     * Returns the type form that the message with the given name is generated as.
     *
     * @param protoFqn the fully qualified proto name of the message
     * @return the type form
     */
    private TypeForm resolveTypeForm(String protoFqn) {
        var override = config.typeOverrides().get(protoFqn);
        if (override != null && override.typeForm() != null) {
            return override.typeForm();
        }
        return config.defaults().typeForm();
    }
}
