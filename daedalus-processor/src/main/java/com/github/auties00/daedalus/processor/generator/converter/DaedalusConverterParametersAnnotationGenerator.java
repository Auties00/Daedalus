package com.github.auties00.daedalus.processor.generator.converter;

import com.github.auties00.daedalus.processor.diagnostic.DaedalusConverterErrors;
import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusConverterParametersManager;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSize;
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
 * A generator that creates companion parameter annotations for converters that declare
 * additional parameters beyond the mandatory one.
 *
 * <p>When a method annotated with {@code @TypeSerializer}, {@code @TypeDeserializer} or
 * {@code @TypeSize} has additional parameters whose arguments are not injected by a data
 * format, as described by {@link DaedalusConverterParametersManager}, the processor
 * generates an annotation named {@code {EnclosingTypeName}Parameters} with one method per
 * such parameter. This annotation can then be applied to fields or record components to
 * supply the parameter values at the use site.
 *
 * <p>The additional parameter types must be legal annotation element types whose values
 * can be written as Java expressions: primitives, {@code String}, {@code Class}, enums, or
 * one dimensional arrays thereof. Every converter of a type that declares additional
 * parameters must declare the same ones, in the same order, as they are supplied by the
 * same annotation.
 */
public final class DaedalusConverterParametersAnnotationGenerator extends DaedalusClassGenerator {

    private final DaedalusTypeManager types;
    private final DaedalusConverterParametersManager parameters;
    private final DaedalusLogManager messages;

    /**
     * Constructs a new parameters annotation generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     * @param parameters the manager of the additional parameters of converters
     * @param messages the compiler diagnostics utility
     */
    public DaedalusConverterParametersAnnotationGenerator(Filer filer, DaedalusTypeManager types, DaedalusConverterParametersManager parameters, DaedalusLogManager messages) {
        super(filer);
        this.types = types;
        this.parameters = parameters;
        this.messages = messages;
    }

    /**
     * Scans the round environment for converters with additional parameters and
     * generates a companion annotation for each type that declares them.
     *
     * <p>The first converter of a type that declares additional parameters defines the
     * elements of the annotation. Every other converter of the same type that declares
     * additional parameters is validated against it.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void generate(RoundEnvironment roundEnv) {
        var converters = new LinkedHashSet<Element>();
        converters.addAll(roundEnv.getElementsAnnotatedWith(TypeSerializer.class));
        converters.addAll(roundEnv.getElementsAnnotatedWith(TypeDeserializer.class));
        converters.addAll(roundEnv.getElementsAnnotatedWith(TypeSize.class));

        var declarations = new LinkedHashMap<TypeElement, ExecutableElement>();
        var invalid = new HashSet<TypeElement>();
        for (var converter : converters) {
            if (!(converter instanceof ExecutableElement method)) {
                continue;
            }

            var annotatedParameters = parameters.getAnnotatedParameters(method);
            if (annotatedParameters.isEmpty()) {
                continue;
            }

            var owner = types.getEnclosingTypeElement(method.getEnclosingElement());
            if (!validateAnnotationTypes(annotatedParameters)) {
                invalid.add(owner);
                continue;
            }

            var declaration = declarations.putIfAbsent(owner, method);
            if (declaration != null && !isSameSignature(annotatedParameters, parameters.getAnnotatedParameters(declaration))) {
                messages.printError(DaedalusConverterErrors.mismatchedParameters(method, annotatedParameters, declaration, parameters.getAnnotatedParameters(declaration), parameters.getAnnotation(method)), method);
                invalid.add(owner);
            }
        }

        for (var entry : declarations.entrySet()) {
            if (!invalid.contains(entry.getKey())) {
                generateAnnotation(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Returns whether two lists of additional parameters have the same names and types,
     * in the same order.
     *
     * @param first the first list of parameters
     * @param second the second list of parameters
     * @return {@code true} if the lists match
     */
    private boolean isSameSignature(List<? extends VariableElement> first, List<? extends VariableElement> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (var index = 0; index < first.size(); index++) {
            var firstParameter = first.get(index);
            var secondParameter = second.get(index);
            if (!firstParameter.getSimpleName().contentEquals(secondParameter.getSimpleName())
                    || !types.isSameType(firstParameter.asType(), secondParameter.asType(), false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that all additional parameter types are legal, reporting an error for
     * each one that is not.
     *
     * @param parameters the additional parameters to validate
     * @return {@code true} if all types are valid
     */
    private boolean validateAnnotationTypes(List<? extends VariableElement> parameters) {
        var valid = true;
        for (var param : parameters) {
            if (!isAnnotationLegalType(param.asType())) {
                messages.printError(DaedalusConverterErrors.illegalParameterType(param), param);
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Returns whether the given type is legal as an annotation element type and its
     * values can be written as Java expressions.
     *
     * <p>Annotation types are legal annotation element types, but are rejected, as an
     * annotation value cannot be passed to a method without reflection.
     *
     * @param type the type to check
     * @return {@code true} if the type is legal
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
     * Returns whether the given declared type is legal.
     *
     * @param type the declared type to check
     * @return {@code true} if it is {@code String}, {@code Class} or an enum
     */
    private boolean isDeclaredAnnotationLegalType(TypeMirror type) {
        if (types.isSameType(type, String.class)) {
            return true;
        }

        if (types.isAssignable(type, Class.class, true)) {
            return true;
        }

        return type instanceof DeclaredType declaredType
                && declaredType.asElement() instanceof TypeElement element
                && element.getKind() == ElementKind.ENUM;
    }

    /**
     * Generates the companion annotation of the given type, declaring one element for
     * each annotated parameter of the given converter.
     *
     * @param owner the type that declares the converters
     * @param declaration the converter whose annotated parameters define the elements
     */
    private void generateAnnotation(TypeElement owner, ExecutableElement declaration) {
        var annotationBuilder = TypeSpec.annotationBuilder(parameters.getAnnotationSimpleName(owner))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Target.class)
                        .addMember("value", "{$T.FIELD, $T.RECORD_COMPONENT, $T.PARAMETER}",
                                ElementType.class, ElementType.class, ElementType.class)
                        .build())
                .addAnnotation(AnnotationSpec.builder(Retention.class)
                        .addMember("value", "$T.RUNTIME", RetentionPolicy.class)
                        .build());

        for (var param : parameters.getAnnotatedParameters(declaration)) {
            var methodBuilder = MethodSpec.methodBuilder(param.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(TypeName.get(param.asType()));
            annotationBuilder.addMethod(methodBuilder.build());
        }

        var javaFile = JavaFile.builder(types.getPackageName(owner), annotationBuilder.build())
                .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException exception) {
            messages.printError(DaedalusConverterErrors.annotationGenerationFailed(parameters.getAnnotation(declaration), exception), declaration);
        }
    }
}
