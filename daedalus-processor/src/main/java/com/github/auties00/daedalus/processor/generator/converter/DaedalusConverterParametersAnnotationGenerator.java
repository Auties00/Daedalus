package com.github.auties00.daedalus.processor.generator.converter;

import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.palantir.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;

/**
 * A generator that creates companion parameter annotations for serializers and
 * deserializers that declare additional parameters beyond the mandatory first one.
 *
 * <p>When a method annotated with {@code @TypeSerializer} or {@code @TypeDeserializer}
 * has additional parameters, the processor generates an annotation named
 * {@code {EnclosingTypeName}Parameters} with one method per additional parameter.
 * This annotation can then be applied to fields or record components to supply
 * the parameter values at the use site.
 *
 * <p>The additional parameter types must be annotation-legal: primitives, {@code String},
 * {@code Class}, enums, annotations, or one-dimensional arrays thereof.
 */
public final class DaedalusConverterParametersAnnotationGenerator extends DaedalusClassGenerator {

    private final DaedalusTypeManager types;
    private final DaedalusLogManager messages;
    private final Set<TypeMirror> supportedIOTypes;

    /**
     * Constructs a new parameters annotation generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     * @param messages the compiler diagnostics utility
     * @param supportedIOTypes the aggregated I/O types supplied by all extensions,
     *        used to exclude framework-injected parameters (readers/writers) from
     *        companion parameter annotations
     */
    public DaedalusConverterParametersAnnotationGenerator(Filer filer, DaedalusTypeManager types, DaedalusLogManager messages, Set<TypeMirror> supportedIOTypes) {
        super(filer);
        this.types = types;
        this.messages = messages;
        this.supportedIOTypes = supportedIOTypes;
    }

    /**
     * Scans the round environment for serializers and deserializers with additional
     * parameters and generates companion annotations for each.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void generate(RoundEnvironment roundEnv) {
        Set<String> generated = new HashSet<>();

        for (var element : roundEnv.getElementsAnnotatedWith(TypeSerializer.class)) {
            if (element instanceof ExecutableElement method) {
                generateIfNeeded(method, generated);
            }
        }

        for (var element : roundEnv.getElementsAnnotatedWith(TypeDeserializer.class)) {
            if (element instanceof ExecutableElement method) {
                generateIfNeeded(method, generated);
            }
        }
    }

    /**
     * Generates a companion annotation for the given method if it has additional
     * parameters and one has not already been generated for the enclosing type.
     *
     * @param method the serializer or deserializer method
     * @param generated the set of already-generated annotation names (to avoid duplicates)
     */
    private void generateIfNeeded(ExecutableElement method, Set<String> generated) {
        var inMixin = types.isMixin(method.getEnclosingElement().asType());
        var mandatoryParamCount = inMixin ? 1 : (method.getKind() == ElementKind.CONSTRUCTOR ? 1 : 0);
        var parameters = method.getParameters();
        if (parameters.size() <= mandatoryParamCount) {
            return;
        }

        var additionalParams = parameters.subList(mandatoryParamCount, parameters.size())
                .stream()
                .filter(param -> !isSupportedIOType(param.asType()))
                .toList();
        if (additionalParams.isEmpty()) {
            return;
        }

        var enclosingType = types.getEnclosingTypeElement(method);
        var packageElement = getPackageElement(enclosingType);
        var annotationName = enclosingType.getSimpleName() + "Parameters";
        var qualifiedName = packageElement.getQualifiedName() + "." + annotationName;
        if (!generated.add(qualifiedName)) {
            return;
        }

        if (!validateAnnotationTypes(method, additionalParams)) {
            return;
        }

        try {
            generateAnnotation(packageElement.getQualifiedName().toString(), annotationName, additionalParams);
        } catch (IOException exception) {
            messages.printError("Failed to generate companion annotation: " + exception.getMessage(), method);
        }
    }

    /**
     * Returns whether the given type is one of the supported I/O types supplied by an extension.
     *
     * @param type the parameter type to check
     * @return {@code true} if the type is an I/O type and should be excluded from the companion annotation
     */
    private boolean isSupportedIOType(TypeMirror type) {
        for (var ioType : supportedIOTypes) {
            if (types.isSameType(type, ioType, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that all additional parameter types are annotation-legal.
     *
     * @param method the method being validated (for error reporting)
     * @param parameters the additional parameters to validate
     * @return {@code true} if all types are valid
     */
    private boolean validateAnnotationTypes(ExecutableElement method, List<? extends VariableElement> parameters) {
        for (var param : parameters) {
            if (!isAnnotationLegalType(param.asType())) {
                messages.printError(
                        "Illegal parameter type: %s is not a valid annotation element type (must be a primitive, String, Class, enum, annotation, or array thereof)"
                                .formatted(param.asType()),
                        method
                );
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the given type is legal as an annotation element type.
     *
     * @param type the type to check
     * @return {@code true} if the type is annotation-legal
     */
    private boolean isAnnotationLegalType(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> true;
            case DECLARED -> isDeclaredAnnotationLegalType(type);
            case ARRAY -> {
                var componentType = ((ArrayType) type).getComponentType();
                yield componentType.getKind() != TypeKind.ARRAY && isAnnotationLegalType(componentType);
            }
            default -> false;
        };
    }

    /**
     * Returns whether the given declared type is annotation-legal.
     *
     * @param type the declared type to check
     * @return {@code true} if it is String, Class, an enum, or an annotation
     */
    private boolean isDeclaredAnnotationLegalType(TypeMirror type) {
        if (types.isSameType(type, String.class)) {
            return true;
        }

        if (types.isAssignable(type, Class.class, true)) {
            return true;
        }

        if (type instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement element) {
            return element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.ANNOTATION_TYPE;
        }

        return false;
    }

    /**
     * Generates the companion annotation type.
     *
     * @param packageName the package for the generated annotation
     * @param annotationName the simple name of the annotation
     * @param parameters the additional parameters to declare as annotation methods
     * @throws IOException if writing the source file fails
     */
    private void generateAnnotation(String packageName, String annotationName, List<? extends VariableElement> parameters) throws IOException {
        var annotationBuilder = TypeSpec.annotationBuilder(annotationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Target.class)
                        .addMember("value", "{$T.FIELD, $T.RECORD_COMPONENT, $T.PARAMETER}",
                                ElementType.class, ElementType.class, ElementType.class)
                        .build())
                .addAnnotation(AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.RUNTIME", RetentionPolicy.class)
                        .build());

        for (var param : parameters) {
            var methodBuilder = MethodSpec.methodBuilder(param.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(param.asType()));
            annotationBuilder.addMethod(methodBuilder.build());
        }

        var javaFile = JavaFile.builder(packageName, annotationBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }

    /**
     * Returns the package element for the given type element.
     *
     * @param typeElement the type element
     * @return the enclosing package element
     */
    private PackageElement getPackageElement(TypeElement typeElement) {
        var enclosing = typeElement.getEnclosingElement();
        while (enclosing != null) {
            if (enclosing instanceof PackageElement packageElement) {
                return packageElement;
            }
            enclosing = enclosing.getEnclosingElement();
        }
        throw new IllegalStateException("No package element found for " + typeElement.getQualifiedName());
    }
}
