package com.github.auties00.daedalus.protobuf.processor.generator.builder;

import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.protobuf.exception.ProtobufUninitializedMessageException;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.processor.generator.builder.DaedalusDefaultBuilderGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterArgument;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * A generator of the builder of a protobuf message, which the Spec class of the message
 * decodes into.
 *
 * <p>The fields of the builder are package private, so that the Spec class, which is in the
 * same package, reads into them directly. A field starts from the value of a field that is
 * not on the wire: the default value of its wire type, converted by its deserializers, for a
 * field with implicit presence, and the default value of its type otherwise. A field whose
 * value is a message, as described by {@link ProtobufFieldType#builderMessage()}, holds the
 * builder of that message instead, so that its setter takes a builder, and an occurrence of
 * the message decoded more than once is merged into it. The unknown fields are held as a
 * {@link ProtobufUnknownFields}, which is never {@code null}.
 *
 * <p>Besides the setters of the fields, the builder has setters that take the type of each
 * deserialization step of a field, and two terminal methods: {@code build()}, which throws a
 * {@link ProtobufUninitializedMessageException} if a required field of the message, or of a
 * nested message, is not set, and {@code buildPartial()}, which skips the check.
 */
public class ProtobufBuilderTypeGenerator extends DaedalusDefaultBuilderGenerator {
    /**
     * The name of the field of the builder that holds the unknown fields of the message.
     */
    public static final String UNKNOWN_FIELDS_NAME = "unknownFields";

    /**
     * The name of the terminal method that builds the message without checking its required
     * fields.
     */
    private static final String BUILD_PARTIAL_METHOD_NAME = "buildPartial";

    /**
     * The manager that resolves the checked exceptions of the generated methods.
     */
    private final ProtobufExceptionManager exceptions;

    /**
     * Constructs a new protobuf builder generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     * @param exceptions the manager that resolves the checked exceptions of the generated methods
     */
    public ProtobufBuilderTypeGenerator(Filer filer, DaedalusTypeManager types, ProtobufExceptionManager exceptions) {
        super(filer, types);
        this.exceptions = exceptions;
    }

    /**
     * Returns the name of the field of the builder that holds the index of the case of the
     * given oneof that is set, which is {@code 0} if no case is set.
     *
     * <p>The value of the oneof alone cannot tell which case is set, as two cases may hold the
     * builder of the same message.
     *
     * @param oneOfName the name of the oneof
     * @return the name of the field, such as {@code protoShapeCase} for a oneof named
     *         {@code shape}
     */
    public static String getOneOfCaseField(String oneOfName) {
        return "proto" + Character.toUpperCase(oneOfName.charAt(0)) + oneOfName.substring(1) + "Case";
    }

    /**
     * Returns the name of the builder class generated for the given message.
     *
     * @param message the type of the message
     * @return the name of the builder class of the message
     */
    public static ClassName getBuilderClassName(DeclaredType message) {
        var qualifiedName = DaedalusMethodGenerator.getBuilderByType(message);
        Element element = message.asElement();
        while (!(element instanceof PackageElement packageElement)) {
            element = element.getEnclosingElement();
        }
        var packageName = packageElement.getQualifiedName().toString();
        var simpleName = packageName.isEmpty() ? qualifiedName : qualifiedName.substring(packageName.length() + 1);
        return ClassName.get(packageName, simpleName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the modifiers described by {@link ProtobufMessageElement#builderModifiers()},
     * which validation restricts to the ones that a builder instantiated by the Spec class
     * can have.
     */
    @Override
    protected List<Modifier> getClassModifiers(DaedalusTypeElement typeElement) {
        var protobufMessage = (ProtobufMessageElement) typeElement;
        var result = new ArrayList<Modifier>();
        for (var modifier : protobufMessage.builderModifiers()) {
            result.add(switch (modifier) {
                case PUBLIC -> Modifier.PUBLIC;
                case FINAL -> Modifier.FINAL;
                default -> throw new IllegalStateException("Internal bug: validation should reject the builder modifier " + modifier);
            });
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns no modifier, so that the fields are package private, and the Spec class of the
     * message decodes into them directly.
     */
    @Override
    protected List<Modifier> getFieldModifiers() {
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the builder of the message held by a field whose value is a message, and
     * {@link Object} for a oneof, which holds either the value of the case that is set, or
     * the builder of its message.
     */
    @Override
    protected TypeName getBuilderFieldType(DaedalusFieldElement field) {
        if (field instanceof ProtobufFieldElement.OneOf) {
            return ClassName.OBJECT;
        }

        if (field instanceof ProtobufFieldElement protobufField) {
            var message = protobufField.type().builderMessage();
            if (message.isPresent()) {
                return getBuilderClassName(message.get());
            }
        }
        return super.getBuilderFieldType(field);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A oneof has a setter for each of its cases, which takes the builder of the message of
     * a case that the builder holds as a builder, and the type of the case otherwise, and
     * records the index of the case. The setters are overloads named after the oneof, unless
     * two cases take the same type, like two cases that hold the builder of the same
     * message: the setters of those cases are named after the oneof and the type of the case.
     * A {@code clear} method unsets the oneof. For example, given a oneof named "shape" whose
     * cases hold the message {@code Circle} and {@code Label}:
     *
     * <pre>{@code
     * public DrawingBuilder shape(CircleBuilder shape) {
     *     this.shape = shape;
     *     this.protoShapeCase = 1;
     *     return this;
     * }
     *
     * public DrawingBuilder shape(Label shape) {
     *     this.shape = shape;
     *     this.protoShapeCase = 2;
     *     return this;
     * }
     *
     * public DrawingBuilder clearShape() {
     *     this.shape = null;
     *     this.protoShapeCase = 0;
     *     return this;
     * }
     * }</pre>
     */
    @Override
    protected List<MethodSpec> createSetters(DaedalusFieldElement field, String className) {
        if (!(field instanceof ProtobufFieldElement.OneOf oneOf)) {
            return super.createSetters(field, className);
        }

        var parameterTypes = new ArrayList<TypeName>();
        for (var oneOfCase : oneOf.cases()) {
            var caseBuilderMessage = oneOfCase.type().builderMessage();
            parameterTypes.add(caseBuilderMessage.isPresent()
                    ? getBuilderClassName(caseBuilderMessage.get())
                    : TypeName.get(oneOfCase.type().descriptorElementType()));
        }

        var caseField = getOneOfCaseField(oneOf.name());
        var returnType = ClassName.bestGuess(className);
        var result = new ArrayList<MethodSpec>();
        for (var i = 0; i < oneOf.cases().size(); i++) {
            var oneOfCase = oneOf.cases().get(i);
            var parameterType = parameterTypes.get(i);
            var setterName = oneOf.name();
            if (parameterTypes.indexOf(parameterType) != parameterTypes.lastIndexOf(parameterType)) {
                var caseTypeName = ((DeclaredType) oneOfCase.type().descriptorElementType()).asElement().getSimpleName().toString();
                setterName += caseTypeName;
            }
            result.add(MethodSpec.methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(parameterType, oneOf.name())
                    .returns(returnType)
                    .addStatement("this.$L = $L", oneOf.name(), oneOf.name())
                    .addStatement("this.$L = $L", caseField, oneOfCase.index())
                    .addStatement("return this")
                    .build());
        }
        var clearName = "clear" + Character.toUpperCase(oneOf.name().charAt(0)) + oneOf.name().substring(1);
        result.add(MethodSpec.methodBuilder(clearName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addStatement("this.$L = null", oneOf.name())
                .addStatement("this.$L = 0", caseField)
                .addStatement("return this")
                .build());
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A field that holds the builder of a message starts from {@code null}, and a field with
     * implicit presence starts from the default value of its wire type, converted by its
     * deserializers, unless one of them receives an injected argument, like a reader, which a
     * builder cannot provide.
     */
    @Override
    protected String getFieldDefaultValue(DaedalusFieldElement field) {
        var protobufField = (ProtobufFieldElement) field;
        if (protobufField.type().builderMessage().isPresent()) {
            return "null";
        }

        if (protobufField.implicit() && protobufField.type() instanceof ProtobufValueFieldType valueType) {
            var implicitDefaultValue = getImplicitDefaultValue(valueType);
            if (implicitDefaultValue.isPresent()) {
                return implicitDefaultValue.get();
            }
        }

        return field.type().descriptorDefaultValue();
    }

    /**
     * Returns the value of a field with implicit presence that is not set, which is the
     * default value of its wire type converted by its deserializers.
     *
     * @param type the type of the field
     * @return the expression that provides the value, or an empty optional if a deserializer
     *         of the field receives an injected argument
     */
    private Optional<String> getImplicitDefaultValue(ProtobufValueFieldType type) {
        var deserializers = type.deserializers();
        if (hasInjectedArguments(deserializers, 0)) {
            return Optional.empty();
        }

        var value = ProtobufDeserializationGenerator.getWireDefaultValue(type.protobufType());
        for (var deserializer : deserializers) {
            value = deserializer.delegate().invoke(value, deserializer.arguments(), _ -> {
                throw new IllegalStateException("Internal bug: injected arguments should not reach a builder default value");
            });
        }
        return Optional.of(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the message after checking that its required fields are set.
     */
    @Override
    protected void createBuildStatement(
            MethodSpec.Builder buildMethodBuilder,
            DaedalusTypeElement typeElement,
            ArrayList<String> invocationArgs
    ) {
        writeBuildStatements(buildMethodBuilder, (ProtobufMessageElement) typeElement, false);
    }

    /**
     * Adds the {@code buildPartial()} method, which builds the message without checking its
     * required fields, to the builder class.
     */
    @Override
    protected void extendBuilderClass(
            TypeSpec.Builder classBuilder,
            DaedalusTypeElement typeElement,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className
    ) {
        var buildPartialMethodBuilder = MethodSpec.methodBuilder(BUILD_PARTIAL_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(typeElement.typeElement().asType()));
        writeBuildStatements(buildPartialMethodBuilder, (ProtobufMessageElement) typeElement, true);
        classBuilder.addMethod(buildPartialMethodBuilder.build());
    }

    /**
     * Adds the statements that build the message from the fields of the builder to a terminal
     * method of the builder.
     *
     * <p>Each field that holds the builder of a message is built by the terminal method of the
     * same kind of that builder, and converted to the type of the field by the deserializers
     * that follow the decoder of the message. The message is built through its constructor, or
     * through its factory method if it declares one.
     *
     * @param methodBuilder the terminal method to add the statements to
     * @param protobufMessage the message being built
     * @param partial {@code true} to skip the check of the required fields, {@code false} to
     *                throw a {@link ProtobufUninitializedMessageException} if one is not set
     */
    private void writeBuildStatements(MethodSpec.Builder methodBuilder, ProtobufMessageElement protobufMessage, boolean partial) {
        methodBuilder.addExceptions(exceptions.buildExceptions(protobufMessage));
        var fields = protobufMessage.fields()
                .stream()
                .filter(ProtobufFieldElement::includedInDefaultBuilder)
                .toList();
        if (!partial) {
            var messageName = protobufMessage.typeElement().getQualifiedName().toString();
            for (var field : fields) {
                // A required field is never repeated and never has a primitive type, as validation rejects both
                if (field.required()) {
                    methodBuilder.beginControlFlow("if (this.$L == null)", field.name());
                    methodBuilder.addStatement("throw $T.missingRequiredField($S, $S)", ProtobufUninitializedMessageException.class, messageName, field.textName());
                    methodBuilder.endControlFlow();
                }
            }
        }

        var arguments = new ArrayList<String>();
        for (var field : fields) {
            arguments.add(getBuildArgument(field, partial));
        }
        protobufMessage.unknownFieldsElement()
                .ifPresent(unknownFieldsElement -> arguments.add(unknownFieldsElement.fromStore("this." + UNKNOWN_FIELDS_NAME)));

        var resultType = TypeName.get(protobufMessage.typeElement().asType());
        var argumentsJoined = String.join(", ", arguments);
        var factory = protobufMessage.constructor();
        if (factory.isEmpty()) {
            methodBuilder.addStatement("return new $T($L)", resultType, argumentsJoined);
        } else {
            methodBuilder.addStatement("return $T.$L($L)", resultType, factory.get().name(), argumentsJoined);
        }
    }

    /**
     * Returns the expression that provides the value of the given field to the constructor or
     * factory method of the message.
     *
     * <p>A field that has no presence cannot be unset, so a {@code null} value set through its
     * setter is built as the value of a field that is not on the wire: the default value of a
     * field with implicit presence, and an empty collection, array or map for a repeated or
     * map field. A field with explicit presence, which includes every singular field of
     * proto2, keeps {@code null}, which means that it is not set.
     *
     * @param field the field
     * @param partial whether the builder of a nested message is built partially
     * @return the expression that provides the value of the field
     */
    private String getBuildArgument(ProtobufFieldElement field, boolean partial) {
        if (field instanceof ProtobufFieldElement.OneOf oneOf) {
            return getOneOfBuildArgument(oneOf, partial);
        }

        var fieldValue = "this." + field.name();
        if (field.type().builderMessage().isEmpty()) {
            return getAbsentValue(field)
                    .map(absentValue -> "%s == null ? %s : %s".formatted(fieldValue, absentValue, fieldValue))
                    .orElse(fieldValue);
        }

        var value = "%s.%s()".formatted(fieldValue, partial ? BUILD_PARTIAL_METHOD_NAME : getDefaultBuildMethodName());
        var deserializers = field.type().deserializers();
        for (var i = 1; i < deserializers.size(); i++) {
            var deserializer = deserializers.get(i);
            value = deserializer.delegate().invoke(value, deserializer.arguments(), _ -> {
                throw new IllegalStateException("Internal bug: injected arguments should not reach a builder of a message");
            });
        }
        return "%s == null ? %s : %s".formatted(fieldValue, field.type().descriptorDefaultValue(), value);
    }

    /**
     * Returns the value of the given field when it is not on the wire, if the field has no
     * presence and its type can be {@code null}.
     *
     * @param field the field
     * @return the default value of a field with implicit presence, an empty collection, array
     *         or map for a repeated or map field, or an empty optional if the field has explicit
     *         presence, has a primitive type, or has no such value
     */
    private Optional<String> getAbsentValue(ProtobufFieldElement field) {
        if (field.type().descriptorElementType().getKind().isPrimitive()) {
            return Optional.empty();
        }

        var absentValue = switch (field.type()) {
            case ProtobufCollectionFieldType _, ProtobufArrayFieldType _, ProtobufMapFieldType _ -> Optional.of(field.type().descriptorDefaultValue());
            case ProtobufSimpleFieldType simpleType when field.implicit() -> getImplicitDefaultValue(simpleType);
            default -> Optional.<String>empty();
        };
        return absentValue.filter(value -> !value.equals("null"));
    }

    /**
     * Returns the expression that provides the value of the given oneof to the constructor or
     * factory method of the message.
     *
     * <p>The case that is set is the one whose index is held by the
     * {@linkplain #getOneOfCaseField(String) case field} of the oneof. The builder of the
     * message of a case is built by the terminal method of the same kind, and converted to
     * the type of the case by the deserializers that follow the decoder of the message,
     * while the value of any other case is the value of the oneof. For example, given a
     * oneof named "shape" of type {@code Shape} whose message case with index 1 holds
     * {@code Circle}:
     *
     * <pre>{@code
     * switch (this.protoShapeCase) {
     *     case 1 -> (Shape) ((CircleBuilder) this.shape).build();
     *     case 0 -> null;
     *     default -> (Shape) this.shape;
     * }
     * }</pre>
     *
     * @param oneOf the oneof
     * @param partial whether the builder of a nested message is built partially
     * @return the expression that provides the value of the oneof
     */
    private String getOneOfBuildArgument(ProtobufFieldElement.OneOf oneOf, boolean partial) {
        var fieldValue = "this." + oneOf.name();
        var sealedType = TypeName.get(oneOf.type().descriptorElementType());
        var arms = new StringBuilder();
        for (var oneOfCase : oneOf.cases()) {
            var caseBuilderMessage = oneOfCase.type().builderMessage();
            if (caseBuilderMessage.isEmpty()) {
                continue;
            }

            var builderValue = "((%s) %s)".formatted(getBuilderClassName(caseBuilderMessage.get()), fieldValue);
            var value = "%s.%s()".formatted(builderValue, partial ? BUILD_PARTIAL_METHOD_NAME : getDefaultBuildMethodName());
            var deserializers = oneOfCase.type().deserializers();
            for (var i = 1; i < deserializers.size(); i++) {
                var deserializer = deserializers.get(i);
                value = deserializer.delegate().invoke(value, deserializer.arguments(), _ -> {
                    throw new IllegalStateException("Internal bug: injected arguments should not reach a builder of a message");
                });
            }
            arms.append("case %d -> (%s) %s; ".formatted(oneOfCase.index(), sealedType, value));
        }

        return "switch (this.%s) { %scase 0 -> null; default -> (%s) %s; }".formatted(getOneOfCaseField(oneOf.name()), arms, sealedType, fieldValue);
    }

    /**
     * Adds the fields that hold the index of the case of each oneof that is set, and the
     * field that holds the unknown fields of the message, if it keeps them, to the builder
     * class.
     */
    @Override
    protected void extendBuilderFields(TypeSpec.Builder classBuilder, DaedalusTypeElement typeElement) {
        var protobufMessage = (ProtobufMessageElement) typeElement;
        for (var field : protobufMessage.fields()) {
            if (field instanceof ProtobufFieldElement.OneOf oneOf && oneOf.includedInDefaultBuilder()) {
                classBuilder.addField(FieldSpec.builder(TypeName.INT, getOneOfCaseField(oneOf.name())).build());
            }
        }
        if (protobufMessage.unknownFieldsElement().isPresent()) {
            classBuilder.addField(FieldSpec.builder(ProtobufUnknownFields.class, UNKNOWN_FIELDS_NAME).build());
        }
    }

    /**
     * Initializes the unknown fields of the message, if it keeps them, with an empty store, and
     * declares the checked exceptions of the deserializers that convert the default values of
     * the fields with implicit presence.
     */
    @Override
    protected void extendBuilderConstructor(MethodSpec.Builder constructorBuilder, DaedalusTypeElement typeElement) {
        var protobufMessage = (ProtobufMessageElement) typeElement;
        if (protobufMessage.unknownFieldsElement().isPresent()) {
            constructorBuilder.addStatement("this.$L = new $T()", UNKNOWN_FIELDS_NAME, ProtobufUnknownFields.class);
        }

        var thrownTypes = new LinkedHashSet<TypeName>();
        for (var field : protobufMessage.fields()) {
            if (field.includedInDefaultBuilder() && field.implicit() && !hasInjectedArguments(field.type().deserializers(), 0)) {
                for (var deserializer : field.type().deserializers()) {
                    thrownTypes.addAll(exceptions.deserializerExceptions(deserializer.delegate()));
                }
            }
        }
        constructorBuilder.addExceptions(thrownTypes);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates converter-based setter overloads for simple fields that have
     * deserialization steps. For each deserialization step, an additional setter
     * is generated that accepts the intermediate type and applies the remaining
     * conversion chain. A field that holds the builder of a message only accepts the
     * builder. Also exposes a typed {@code unknownFields(...)} setter when the message
     * declares a {@code @ProtobufMessage.UnknownFields} sink.
     */
    @Override
    protected void generateExtraSetters(
            TypeSpec.Builder classBuilder,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className,
            DaedalusTypeElement typeElement
    ) {
        for (var field : fields) {
            if (!(field instanceof ProtobufFieldElement protobufField)) {
                continue;
            }

            if (!(protobufField.type() instanceof ProtobufSimpleFieldType simpleType) || simpleType.builderMessage().isPresent()) {
                continue;
            }

            var deserializers = protobufField.type().deserializers();
            var startIndex = simpleType.protobufType() == ProtobufType.MESSAGE ? 1 : 0;
            for (var i = startIndex; i < deserializers.size(); i++) {
                var deserializer = deserializers.get(i);
                // A setter has no reader to pass to the remaining deserializers
                if (hasInjectedArguments(deserializers, i)) {
                    continue;
                }

                var value = field.name();
                var thrownTypes = new LinkedHashSet<TypeName>();
                for (var j = i; j < deserializers.size(); j++) {
                    var override = deserializers.get(j);
                    value = override.delegate().invoke(value, override.arguments(), _ -> {
                        throw new IllegalStateException("Internal bug: injected arguments should not reach a builder setter");
                    });
                    thrownTypes.addAll(exceptions.deserializerExceptions(override.delegate()));
                }

                var setter = createBuilderSetter(
                        field.name(),
                        value,
                        deserializer.parameterType(),
                        className
                );
                classBuilder.addMethod(setter.toBuilder()
                        .addExceptions(thrownTypes)
                        .build());
            }
        }

        var protobufMessage = (ProtobufMessageElement) typeElement;
        protobufMessage.unknownFieldsElement().ifPresent(unknownFieldsElement -> {
            // A value that is not a store, like null, holds no unknown field
            var setter = MethodSpec.methodBuilder(UNKNOWN_FIELDS_NAME)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(unknownFieldsElement.type()), UNKNOWN_FIELDS_NAME)
                    .returns(ClassName.bestGuess(className))
                    .addExceptions(getThrownTypes(unknownFieldsElement.serializerMethod()))
                    .addStatement("this.$L = $L != null && $L instanceof $T store ? store : new $T()", UNKNOWN_FIELDS_NAME, UNKNOWN_FIELDS_NAME, unknownFieldsElement.toStore(UNKNOWN_FIELDS_NAME), ProtobufUnknownFields.class, ProtobufUnknownFields.class)
                    .addStatement("return this")
                    .build();
            classBuilder.addMethod(setter);
        });
    }

    /**
     * Returns the checked exceptions declared by the given method, if any.
     *
     * @param method the method
     * @return the type names of the checked exceptions
     */
    private static List<TypeName> getThrownTypes(Optional<ExecutableElement> method) {
        var result = new ArrayList<TypeName>();
        method.ifPresent(executable -> {
            for (TypeMirror thrownType : executable.getThrownTypes()) {
                result.add(TypeName.get(thrownType));
            }
        });
        return result;
    }

    /**
     * Returns whether any of the given deserializers, starting from the given index,
     * receives an argument that is injected by the generated code, such as a reader.
     *
     * @param deserializers the deserializer chain
     * @param startIndex the index of the first deserializer to check
     * @return {@code true} if an injected argument is found
     */
    private boolean hasInjectedArguments(List<DaedalusConverterElement.Attributed.Deserializer> deserializers, int startIndex) {
        for (var i = startIndex; i < deserializers.size(); i++) {
            for (var argument : deserializers.get(i).arguments()) {
                if (argument instanceof DaedalusConverterArgument.Injected) {
                    return true;
                }
            }
        }
        return false;
    }
}
