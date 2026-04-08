package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.processor.util.DaedalusBuilderNameUtils;
import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.SequencedCollection;

/**
 * A resolver that scans mixin classes for {@code @TypeBuilder.Mixin} methods and
 * generates convenience methods on builder classes.
 */
public final class DaedalusBuilderMixinManager {

    private final DaedalusTypeManager types;

    /**
     * Constructs a new mixin resolver with the given type utilities.
     *
     * @param types the common type utilities for assignability checks
     */
    public DaedalusBuilderMixinManager(DaedalusTypeManager types) {
        this.types = types;
    }

    /**
     * Generates builder mixin convenience methods for all fields of the given type
     * and adds them to the class builder.
     *
     * @param classBuilder the builder class being constructed
     * @param className the simple name of the builder class (for return type)
     * @param enclosingType the type element that owns the fields
     * @param fields the fields to scan for applicable mixin methods
     */
    public void generateMixinMethods(
            TypeSpec.Builder classBuilder,
            String className,
            TypeElement enclosingType,
            SequencedCollection<? extends DaedalusFieldElement> fields
    ) {
        for (var field : fields) {
            if (!field.includedInDefaultBuilder()) {
                continue;
            }

            var fieldType = field.type().descriptorElementType();
            var mixins = field.type().mixins();
            for (var mixin : mixins) {
                for (var enclosed : mixin.getEnclosedElements()) {
                    if (!(enclosed instanceof ExecutableElement method)) {
                        continue;
                    }

                    var mixinAnnotation = method.getAnnotation(TypeBuilder.Mixin.class);
                    if (mixinAnnotation == null) {
                        continue;
                    }

                    if (!isApplicable(method, fieldType)) {
                        continue;
                    }

                    var builderMethod = createMixinMethod(
                            method,
                            mixinAnnotation,
                            field.name(),
                            fieldType,
                            enclosingType,
                            className,
                            mixin
                    );
                    classBuilder.addMethod(builderMethod);
                }
            }
        }
    }

    /**
     * Returns whether the given mixin method is applicable to the given field type.
     *
     * @param method the mixin method to check
     * @param fieldType the field's descriptor element type
     * @return {@code true} if the method is applicable
     */
    private boolean isApplicable(ExecutableElement method, TypeMirror fieldType) {
        if (!method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }

        var parameters = method.getParameters();
        if (parameters.isEmpty()) {
            return false;
        }

        var firstParamType = parameters.getFirst().asType();
        if (!types.isSameType(firstParamType, fieldType, true)) {
            return false;
        }

        var bindings = types.inferTypeBindings(method, List.of(fieldType));
        var substituted = types.substituteTypeVariables(firstParamType, bindings);
        return types.isSameType(fieldType, substituted, false);
    }

    /**
     * Creates a builder convenience method that delegates to the mixin method.
     *
     * @param method the mixin method
     * @param mixinAnnotation the {@code @TypeBuilder.Mixin} annotation
     * @param fieldName the name of the builder field
     * @param fieldType the type of the builder field
     * @param enclosingType the type that owns the field
     * @param className the builder class name
     * @param mixinType the type element of the mixin class
     * @return the generated method spec
     */
    private MethodSpec createMixinMethod(
            ExecutableElement method,
            TypeBuilder.Mixin mixinAnnotation,
            String fieldName,
            TypeMirror fieldType,
            TypeElement enclosingType,
            String className,
            TypeElement mixinType
    ) {
        var methodName = DaedalusBuilderNameUtils.resolveSetterName(
                mixinAnnotation.builderMethodName(),
                fieldName,
                fieldType,
                enclosingType
        );

        var returnType = ClassName.bestGuess(className);
        var methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);

        var parameters = method.getParameters();
        var bindings = types.inferTypeBindings(method, List.of(fieldType));
        var delegateArgs = new StringBuilder("this." + fieldName);
        for (var i = 1; i < parameters.size(); i++) {
            var param = parameters.get(i);
            var paramName = param.getSimpleName().toString();
            var substituted = types.substituteTypeVariables(param.asType(), bindings);
            methodBuilder.addParameter(TypeName.get(substituted), paramName);
            delegateArgs.append(", ").append(paramName);
        }

        var mixinQualifiedName = mixinType.getQualifiedName().toString();
        var mixinMethodName = method.getSimpleName().toString();
        methodBuilder.addStatement("this.$L = $L.$L($L)", fieldName, mixinQualifiedName, mixinMethodName, delegateArgs);
        methodBuilder.addStatement("return this");

        return methodBuilder.build();
    }
}
