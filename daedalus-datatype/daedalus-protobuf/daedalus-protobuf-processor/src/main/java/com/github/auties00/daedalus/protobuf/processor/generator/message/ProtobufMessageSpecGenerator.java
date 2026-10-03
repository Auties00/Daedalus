package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSizeGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderTypeGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSize;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.DeclaredType;
import java.io.IOException;

/**
 * A class generator that produces complete Spec classes for {@code @ProtobufMessage} types.
 *
 * <p>The generated Spec class extends {@link ProtobufMessageSpec}, and its only instance is
 * returned by its static {@value #GLOBAL_METHOD_NAME} method, which is annotated with
 * {@link TypeMixin.Instance}, so that its converters, which are instance methods, are invoked
 * on it. The Spec class implements the methods of the binary format and of the text format
 * that depend on the fields of the message, while {@link ProtobufMessageSpec} implements their
 * overloads, such as the ones that take a byte array or a string.
 *
 * <p>The generation of the methods is delegated to {@link ProtobufMessageSerializationGenerator},
 * {@link ProtobufMessageSizeGenerator}, {@link ProtobufMessageSizeTableSlotsGenerator},
 * {@link ProtobufMessageDeserializationGenerator}, {@link ProtobufMessagePartialDeserializationGenerator},
 * and to their text counterparts: {@link ProtobufMessageTextSerializationGenerator},
 * {@link ProtobufMessageTextSizeGenerator}, {@link ProtobufMessageTextDeserializationGenerator} and
 * {@link ProtobufMessageTextPartialDeserializationGenerator}. This generator adds the methods
 * that only carry the annotations of the converters of the message, which are
 * {@code encodeBinary(message, writer)}, annotated with {@link TypeSerializer}, and
 * {@code sizeOfBinary(message)}, annotated with {@link TypeSize}, and the method that creates
 * the builder of the message.
 */
public class ProtobufMessageSpecGenerator extends DaedalusClassGenerator {
    /**
     * The name of the static method of a Spec class that returns its only instance.
     */
    public static final String GLOBAL_METHOD_NAME = "global";

    /**
     * The name of the static field of a Spec class that holds its only instance.
     */
    private static final String GLOBAL_FIELD_NAME = "GLOBAL";

    /**
     * The name of the parameter that holds the message.
     */
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";

    /**
     * The name of the parameter that holds the writer.
     */
    private static final String OUTPUT_OBJECT_PARAMETER = "protoOutputStream";

    /**
     * The manager that resolves the checked exceptions of the generated methods.
     */
    private final ProtobufExceptionManager exceptions;

    /**
     * Constructs a new message spec generator with the given filer.
     *
     * @param filer the filer for creating generated source files
     * @param exceptions the manager that resolves the checked exceptions of the generated methods
     */
    public ProtobufMessageSpecGenerator(Filer filer, ProtobufExceptionManager exceptions) {
        super(filer);
        this.exceptions = exceptions;
    }

    /**
     * Returns the expression that evaluates to the only instance of the Spec class with the
     * given name, on which its converters are invoked.
     *
     * @param specName the fully qualified name of the Spec class
     * @return the expression
     */
    public static String getInstanceExpression(String specName) {
        return specName + "." + GLOBAL_METHOD_NAME + "()";
    }

    /**
     * Creates and writes the Spec class for the given protobuf message element.
     *
     * @param messageElement the protobuf message element to generate a Spec class for
     * @param packageElement the package in which to place the generated class, or
     *                       {@code null} for the default package
     * @throws IOException if an I/O error occurs while writing the generated source file
     */
    public void createClass(ProtobufMessageElement messageElement, PackageElement packageElement) throws IOException {
        var simpleGeneratedClassName = getGeneratedClassNameWithSuffix(messageElement.typeElement(), "Spec");
        var packageName = packageElement != null ? packageElement.getQualifiedName().toString() : "";
        var specName = ClassName.get(packageName, simpleGeneratedClassName);
        var messageName = ClassName.get(messageElement.typeElement());
        var builderName = ProtobufBuilderTypeGenerator.getBuilderClassName((DeclaredType) messageElement.typeElement().asType());

        var classBuilder = TypeSpec.classBuilder(simpleGeneratedClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(ProtobufMessageSpec.class), messageName, builderName))
                .addAnnotation(AnnotationSpec.builder(TypeMixin.class)
                        .addMember("scope", "$T.Scope.GLOBAL", TypeMixin.class)
                        .build())
                .addField(FieldSpec.builder(specName, GLOBAL_FIELD_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", specName)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addMethod(MethodSpec.methodBuilder(GLOBAL_METHOD_NAME)
                        .addAnnotation(TypeMixin.Instance.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(specName)
                        .addStatement("return $L", GLOBAL_FIELD_NAME)
                        .build());

        new ProtobufMessageSerializationGenerator(messageElement, exceptions).generate(classBuilder);
        classBuilder.addMethod(createSerializer(messageName));
        new ProtobufMessageSizeGenerator(messageElement, exceptions).generate(classBuilder);
        classBuilder.addMethod(createSizer(messageName));
        new ProtobufMessageSizeTableSlotsGenerator(messageElement, exceptions).generate(classBuilder);

        new ProtobufMessageDeserializationGenerator(messageElement, exceptions).generate(classBuilder);
        new ProtobufMessagePartialDeserializationGenerator(messageElement, exceptions).generate(classBuilder);

        new ProtobufMessageTextSerializationGenerator(messageElement, exceptions).generate(classBuilder);
        new ProtobufMessageTextSizeGenerator(messageElement, exceptions).generate(classBuilder);
        new ProtobufMessageTextDeserializationGenerator(messageElement, exceptions).generate(classBuilder);
        new ProtobufMessageTextPartialDeserializationGenerator(messageElement, exceptions).generate(classBuilder);

        classBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(builderName)
                .addStatement("return new $T()", builderName)
                .build());

        var javaFile = JavaFile.builder(packageName, classBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }

    /**
     * Returns the serializer of the message, which encodes it with a binary writer and is
     * annotated with {@link TypeSerializer}, so that the messages and the mixins that use it
     * write it.
     *
     * @param messageName the type of the message
     * @return the method
     */
    private static MethodSpec createSerializer(TypeName messageName) {
        return MethodSpec.methodBuilder(ProtobufSerializationGenerator.METHOD_NAME)
                .addAnnotation(Override.class)
                .addAnnotation(TypeSerializer.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(messageName, INPUT_OBJECT_PARAMETER)
                .addParameter(ParameterizedTypeName.get(ClassName.get(ProtobufBinaryWriter.class), WildcardTypeName.subtypeOf(Object.class)), OUTPUT_OBJECT_PARAMETER)
                .addException(ProtobufSerializationException.class)
                .addStatement("super.$L($L, $L)", ProtobufSerializationGenerator.METHOD_NAME, INPUT_OBJECT_PARAMETER, OUTPUT_OBJECT_PARAMETER)
                .build();
    }

    /**
     * Returns the sizer of the message, which computes the size of its binary encoding and is
     * annotated with {@link TypeSize}, so that the messages and the mixins that write it with
     * its serializer precede it by its length.
     *
     * @param messageName the type of the message
     * @return the method
     */
    private static MethodSpec createSizer(TypeName messageName) {
        return MethodSpec.methodBuilder(ProtobufSizeGenerator.METHOD_NAME)
                .addAnnotation(Override.class)
                .addAnnotation(TypeSize.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(messageName, INPUT_OBJECT_PARAMETER)
                .addException(ProtobufSerializationException.class)
                .addStatement("return super.$L($L)", ProtobufSizeGenerator.METHOD_NAME, INPUT_OBJECT_PARAMETER)
                .build();
    }
}
