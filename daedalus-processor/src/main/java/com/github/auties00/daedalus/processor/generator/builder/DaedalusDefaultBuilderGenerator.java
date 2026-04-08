package com.github.auties00.daedalus.processor.generator.builder;

import com.github.auties00.daedalus.processor.generator.DaedalusClassGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusBuilderMixinManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.palantir.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SequencedCollection;

/**
 * An abstract generator for default builder classes.
 *
 * <p>A default builder is generated for each type where
 * {@link DaedalusTypeElement#supportsDefaultBuilder()} returns {@code true}.
 * The builder includes a private field, a setter, and an initialization statement
 * in the constructor for each field where
 * {@link DaedalusFieldElement#includedInDefaultBuilder()} returns {@code true}.
 *
 * <p>Subclasses provide the terminal build statement, field default values,
 * and any format-specific extra setter overloads.
 */
public abstract class DaedalusDefaultBuilderGenerator extends DaedalusClassGenerator {

    /**
     * The common type utilities for type comparisons.
     */
    protected final DaedalusTypeManager types;

    /**
     * The mixin resolver for generating builder mixin convenience methods.
     */
    private final DaedalusBuilderMixinManager mixinResolver;

    /**
     * Constructs a new default builder generator.
     *
     * @param filer the filer for creating source files
     * @param types the common type utilities
     */
    protected DaedalusDefaultBuilderGenerator(Filer filer, DaedalusTypeManager types) {
        super(filer);
        this.types = types;
        this.mixinResolver = new DaedalusBuilderMixinManager(types);
    }

    /**
     * Generates the default builder class for the given type element.
     *
     * @param packageName the package name for the generated class
     * @param typeElement the type element to generate a builder for
     * @throws IOException if writing the source file fails
     */
    public void createClass(String packageName, DaedalusTypeElement typeElement) throws IOException {
        var className = getGeneratedClassNameWithSuffix(typeElement.typeElement(), "Builder");
        var classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        var fields = collectBuilderFields(typeElement);

        generateFields(classBuilder, fields, typeElement);
        generateConstructor(classBuilder, fields, typeElement);
        generateSetters(classBuilder, fields, className, typeElement);
        generateExtraSetters(classBuilder, fields, className, typeElement);
        mixinResolver.generateMixinMethods(classBuilder, className, typeElement.typeElement(), fields);
        generateBuildMethod(classBuilder, typeElement, fields, className);

        var javaFile = JavaFile.builder(packageName, classBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }

    /**
     * Collects the fields that should be included in the default builder.
     *
     * @param typeElement the type element
     * @return the fields to include
     */
    private SequencedCollection<? extends DaedalusFieldElement> collectBuilderFields(DaedalusTypeElement typeElement) {
        return typeElement.fields()
                .stream()
                .filter(DaedalusFieldElement::includedInDefaultBuilder)
                .toList();
    }

    /**
     * Generates private fields on the builder class.
     *
     * @param classBuilder the class builder
     * @param fields the fields to generate
     * @param typeElement the type element being built
     */
    private void generateFields(TypeSpec.Builder classBuilder, SequencedCollection<? extends DaedalusFieldElement> fields, DaedalusTypeElement typeElement) {
        for (var field : fields) {
            var fieldType = TypeName.get(field.type().descriptorElementType());
            var fieldSpec = FieldSpec.builder(fieldType, field.name())
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            classBuilder.addField(fieldSpec);
        }
        extendBuilderFields(classBuilder, typeElement);
    }

    /**
     * Generates the constructor that initializes fields with default values.
     *
     * @param classBuilder the class builder
     * @param fields the fields to initialize
     * @param typeElement the type element being built
     */
    private void generateConstructor(TypeSpec.Builder classBuilder, SequencedCollection<? extends DaedalusFieldElement> fields, DaedalusTypeElement typeElement) {
        var constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (var field : fields) {
            var defaultValue = getFieldDefaultValue(field);
            constructorBuilder.addStatement("this.$L = $L", field.name(), defaultValue);
        }
        extendBuilderConstructor(constructorBuilder, typeElement);
        classBuilder.addMethod(constructorBuilder.build());
    }

    /**
     * Hook for subclasses to add extra fields to the builder class beyond
     * those derived from the type's declared properties.
     *
     * <p>The default implementation does nothing.
     *
     * @param classBuilder the class builder to mutate
     * @param typeElement the type element being built
     */
    protected void extendBuilderFields(TypeSpec.Builder classBuilder, DaedalusTypeElement typeElement) {

    }

    /**
     * Hook for subclasses to append extra initialization statements to the
     * builder's constructor body.
     *
     * <p>The default implementation does nothing.
     *
     * @param constructorBuilder the constructor builder to mutate
     * @param typeElement the type element being built
     */
    protected void extendBuilderConstructor(MethodSpec.Builder constructorBuilder, DaedalusTypeElement typeElement) {

    }

    /**
     * Generates setter methods for each field.
     *
     * @param classBuilder the class builder
     * @param fields the fields to generate setters for
     * @param className the builder class name (for return type)
     * @param typeElement the enclosing type element
     */
    private void generateSetters(
            TypeSpec.Builder classBuilder,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className,
            DaedalusTypeElement typeElement
    ) {
        for (var field : fields) {
            var setter = createBuilderSetter(
                    field.name(),
                    field.name(),
                    field.type().descriptorElementType(),
                    className
            );
            classBuilder.addMethod(setter);
        }
    }

    /**
     * Generates the terminal build method.
     *
     * @param classBuilder the class builder
     * @param typeElement the type element being built
     * @param fields the builder fields
     * @param className the builder class name
     */
    private void generateBuildMethod(
            TypeSpec.Builder classBuilder,
            DaedalusTypeElement typeElement,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className
    ) {
        var resultType = TypeName.get(typeElement.typeElement().asType());
        var buildMethodName = getDefaultBuildMethodName();
        var buildMethodBuilder = MethodSpec.methodBuilder(buildMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(resultType);

        var invocationArgs = new ArrayList<String>();
        for (var field : fields) {
            invocationArgs.add(field.name());
        }

        createBuildStatement(buildMethodBuilder, typeElement, invocationArgs);
        classBuilder.addMethod(buildMethodBuilder.build());
    }

    /**
     * Returns the default value expression for the given field.
     *
     * <p>Subclasses provide format-specific default value resolution.
     *
     * @param field the field element
     * @return the default value expression
     */
    protected abstract String getFieldDefaultValue(DaedalusFieldElement field);

    /**
     * Adds the build statement to the build method.
     *
     * <p>Subclasses provide the format-specific construction logic, such as
     * handling unknown fields or delegating to a deserializer factory method.
     *
     * @param buildMethodBuilder the build method builder
     * @param typeElement the type element being built
     * @param invocationArgs the field names to pass as arguments
     */
    protected abstract void createBuildStatement(
            MethodSpec.Builder buildMethodBuilder,
            DaedalusTypeElement typeElement,
            ArrayList<String> invocationArgs
    );

    /**
     * Generates any format-specific extra setter overloads.
     *
     * <p>For example, protobuf generates converter-based setter overloads for
     * each deserialization step. The default implementation does nothing.
     *
     * @param classBuilder the class builder
     * @param fields the builder fields
     * @param className the builder class name
     * @param typeElement the type element
     */
    protected void generateExtraSetters(
            TypeSpec.Builder classBuilder,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className,
            DaedalusTypeElement typeElement
    ) {

    }

    /**
     * Returns the name for the terminal build method.
     *
     * <p>The default implementation returns {@code "build"}.
     *
     * @return the build method name
     */
    protected String getDefaultBuildMethodName() {
        return "build";
    }

    /**
     * Creates a fluent setter method for a builder field.
     *
     * @param fieldName the field name (used as both parameter name and assignment target)
     * @param fieldValue the value expression to assign
     * @param fieldType the parameter type
     * @param className the builder class name (for return type)
     * @return the generated setter method
     */
    protected MethodSpec createBuilderSetter(String fieldName, String fieldValue, TypeMirror fieldType, String className) {
        var parameterType = TypeName.get(fieldType);
        var returnType = ClassName.bestGuess(className);
        return MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterType, fieldName)
                .returns(returnType)
                .addStatement("this.$L = $L", fieldName, fieldValue)
                .addStatement("return this")
                .build();
    }
}
