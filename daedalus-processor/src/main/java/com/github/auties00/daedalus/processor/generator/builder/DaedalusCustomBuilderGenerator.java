package com.github.auties00.daedalus.processor.generator.builder;

import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusBuilderMixinManager;
import com.github.auties00.daedalus.processor.util.DaedalusBuilderNameUtils;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusBuilderElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;
import com.palantir.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;

/**
 * An abstract generator for custom builder classes declared via {@code @TypeBuilder}.
 *
 * <p>Custom builders are generated for each {@link DaedalusBuilderElement} on a type.
 * The builder class name, modifiers, setter names, and terminal method name are all
 * driven by the annotation attributes.
 *
 * <p>Subclasses provide the terminal build statement that delegates construction
 * to the annotated method or constructor.
 */
public abstract class DaedalusCustomBuilderGenerator extends DaedalusClassGenerator {

    /**
     * The common type utilities for type comparisons.
     */
    protected final DaedalusTypeManager types;

    /**
     * The mixin resolver for generating builder mixin convenience methods.
     */
    private final DaedalusBuilderMixinManager mixinResolver;

    /**
     * Constructs a new custom builder generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     */
    protected DaedalusCustomBuilderGenerator(Filer filer, DaedalusTypeManager types) {
        super(filer);
        this.types = types;
        this.mixinResolver = new DaedalusBuilderMixinManager(types);
    }

    /**
     * Generates a custom builder class for the given builder element.
     *
     * @param packageName the package name for the generated class
     * @param typeElement the type element that owns the builder
     * @param builderElement the builder element with annotation attributes
     * @throws IOException if writing the source file fails
     */
    public void createClass(
            String packageName,
            DaedalusTypeElement typeElement,
            DaedalusBuilderElement builderElement
    ) throws IOException {
        var className = getGeneratedClassName(typeElement.typeElement(), builderElement.name());
        var classBuilder = TypeSpec.classBuilder(className);

        for (var modifier : toJavaModifiers(builderElement.modifiers())) {
            classBuilder.addModifiers(modifier);
        }

        var invocationArgs = new ArrayList<String>();
        for (var parameter : builderElement.parameters()) {
            var fieldType = parameter.element().asType();
            var fieldName = parameter.element().getSimpleName().toString();
            var fieldSpec = FieldSpec.builder(TypeName.get(fieldType), fieldName)
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            classBuilder.addField(fieldSpec);
            invocationArgs.add(fieldName);
        }

        var constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        classBuilder.addMethod(constructorBuilder.build());

        var settersSegments = builderElement.settersMethodNameSegments();
        for (var parameter : builderElement.parameters()) {
            var fieldName = parameter.element().getSimpleName().toString();
            var fieldType = parameter.element().asType();
            var setterName = DaedalusBuilderNameUtils.resolveSetterName(
                    settersSegments,
                    fieldName,
                    fieldType,
                    typeElement.typeElement()
            );
            var setter = createBuilderSetter(setterName, fieldName, fieldType, className);
            classBuilder.addMethod(setter);
        }

        mixinResolver.generateMixinMethods(classBuilder, className, typeElement.typeElement(),
                typeElement.fields().stream().toList());

        var resultType = TypeName.get(typeElement.typeElement().asType());
        var buildMethodName = builderElement.buildMethodName().isEmpty() ? "build" : builderElement.buildMethodName();
        var buildMethodBuilder = MethodSpec.methodBuilder(buildMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(resultType);

        createBuildStatement(buildMethodBuilder, typeElement, builderElement, invocationArgs);
        classBuilder.addMethod(buildMethodBuilder.build());

        var javaFile = JavaFile.builder(packageName, classBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }

    /**
     * Adds the build statement to the terminal build method.
     *
     * <p>Subclasses provide format-specific construction logic.
     *
     * @param buildMethodBuilder the build method builder
     * @param typeElement the type element being built
     * @param builderElement the builder element with annotation attributes
     * @param invocationArgs the parameter names to pass as arguments
     */
    protected abstract void createBuildStatement(
            MethodSpec.Builder buildMethodBuilder,
            DaedalusTypeElement typeElement,
            DaedalusBuilderElement builderElement,
            ArrayList<String> invocationArgs
    );

    /**
     * Creates a fluent setter method for a builder field.
     *
     * @param setterName the setter method name
     * @param fieldName the field name (used as parameter name and assignment target)
     * @param fieldType the parameter type
     * @param className the builder class name (for return type)
     * @return the generated setter method
     */
    private MethodSpec createBuilderSetter(String setterName, String fieldName, TypeMirror fieldType, String className) {
        var parameterType = TypeName.get(fieldType);
        var returnType = ClassName.bestGuess(className);
        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterType, fieldName)
                .returns(returnType)
                .addStatement("this.$L = $L", fieldName, fieldName)
                .addStatement("return this")
                .build();
    }

    /**
     * Converts {@link ClassModifier} values to JavaPoet {@link Modifier} values.
     *
     * @param classModifiers the class modifiers
     * @return the corresponding JavaPoet modifiers
     */
    private Modifier[] toJavaModifiers(ClassModifier[] classModifiers) {
        var result = new Modifier[classModifiers.length];
        for (var i = 0; i < classModifiers.length; i++) {
            result[i] = Modifier.valueOf(classModifiers[i].name());
        }
        return result;
    }
}
