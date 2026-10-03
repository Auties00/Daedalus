package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.protobuf.processor.generator.message.ProtobufMessageSpecGenerator;
import com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import java.io.IOException;

/**
 * A class generator that produces complete Spec classes for {@code @ProtobufEnum} types.
 *
 * <p>The generated Spec class contains methods for serialization, deserialization, size
 * calculation, and the conversion between the indexes and the names of the constants, which
 * the text format uses. The generation of individual methods is delegated to
 * {@link ProtobufEnumSerializationGenerator}, {@link ProtobufEnumDeserializationGenerator},
 * {@link ProtobufEnumDeserializationOverloadGenerator}, {@link ProtobufEnumSizeGenerator},
 * {@link ProtobufEnumNameGenerator}, and {@link ProtobufEnumIndexGenerator}.
 */
public class ProtobufEnumSpecGenerator extends DaedalusClassGenerator {
    /**
     * The name of the static field of a Spec class that holds its only instance.
     */
    private static final String GLOBAL_FIELD_NAME = "GLOBAL";

    /**
     * The manager that resolves the checked exceptions of the generated methods.
     */
    private final ProtobufExceptionManager exceptions;

    /**
     * Constructs a new enum spec generator with the given filer.
     *
     * @param filer the filer for creating generated source files
     * @param exceptions the manager that resolves the checked exceptions of the generated methods
     */
    public ProtobufEnumSpecGenerator(Filer filer, ProtobufExceptionManager exceptions) {
        super(filer);
        this.exceptions = exceptions;
    }

    /**
     * Creates and writes the Spec class for the given protobuf enum element.
     *
     * <p>The generated class extends {@link ProtobufEnumSpec}, and its only instance is returned
     * by its static {@value ProtobufMessageSpecGenerator#GLOBAL_METHOD_NAME} method, which is
     * annotated with {@link TypeMixin.Instance}, so that its converters, which are instance
     * methods, are invoked on it. It includes the {@code encode}, {@code decode},
     * {@code sizeOfBinary}, {@code nameOf}, and {@code indexOf} methods.
     *
     * @param enumElement    the protobuf enum element to generate a Spec class for
     * @param packageElement the package in which to place the generated class, or
     *                       {@code null} for the default package
     * @throws IOException if an I/O error occurs while writing the generated source file
     */
    public void createClass(ProtobufEnumElement enumElement, PackageElement packageElement) throws IOException {
        var simpleGeneratedClassName = getGeneratedClassNameWithSuffix(enumElement.typeElement(), "Spec");
        var packageName = packageElement != null ? packageElement.getQualifiedName().toString() : "";

        var specName = ClassName.get(packageName, simpleGeneratedClassName);
        var classBuilder = TypeSpec.classBuilder(simpleGeneratedClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(ProtobufEnumSpec.class), ClassName.get(enumElement.typeElement())))
                .addAnnotation(AnnotationSpec.builder(TypeMixin.class)
                        .addMember("scope", "$T.Scope.GLOBAL", TypeMixin.class)
                        .build())
                .addField(FieldSpec.builder(specName, GLOBAL_FIELD_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", specName)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addMethod(MethodSpec.methodBuilder(ProtobufMessageSpecGenerator.GLOBAL_METHOD_NAME)
                        .addAnnotation(TypeMixin.Instance.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(specName)
                        .addStatement("return $L", GLOBAL_FIELD_NAME)
                        .build());

        var serializationVisitor = new ProtobufEnumSerializationGenerator(enumElement, exceptions);
        serializationVisitor.generate(classBuilder);

        var deserializationOverloadVisitor = new ProtobufEnumDeserializationOverloadGenerator(enumElement, exceptions);
        deserializationOverloadVisitor.generate(classBuilder);
        var deserializationVisitor = new ProtobufEnumDeserializationGenerator(enumElement, exceptions);
        deserializationVisitor.generate(classBuilder);

        var sizeVisitor = new ProtobufEnumSizeGenerator(enumElement, exceptions);
        sizeVisitor.generate(classBuilder);

        var nameVisitor = new ProtobufEnumNameGenerator(enumElement);
        nameVisitor.generate(classBuilder);
        var indexVisitor = new ProtobufEnumIndexGenerator(enumElement);
        indexVisitor.generate(classBuilder);

        var javaFile = JavaFile.builder(packageName, classBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }
}
