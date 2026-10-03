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
import java.util.List;
import java.util.SequencedCollection;

/**
 * An abstract generator for default builder classes.
 *
 * <p>A default builder is generated for each type where
 * {@link DaedalusTypeElement#supportsDefaultBuilder()} returns {@code true}.
 * The builder includes a field, a setter, and an initialization statement
 * in the constructor for each field where
 * {@link DaedalusFieldElement#includedInDefaultBuilder()} returns {@code true}.
 *
 * <p>Subclasses provide the terminal build statement, field default values,
 * and any format-specific extra setter overloads. They can also change the
 * modifiers of the builder and of its fields, and the type that a field of
 * the builder holds, for example to hold the builder of a nested object
 * instead of the object itself.
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
                .addModifiers(getClassModifiers(typeElement).toArray(Modifier[]::new));

        var fields = collectBuilderFields(typeElement);

        generateFields(classBuilder, fields, typeElement);
        generateConstructor(classBuilder, fields, typeElement);
        generateSetters(classBuilder, fields, className, typeElement);
        generateExtraSetters(classBuilder, fields, className, typeElement);
        mixinResolver.generateMixinMethods(classBuilder, className, typeElement.typeElement(), collectMixinFields(fields));
        generateBuildMethod(classBuilder, typeElement, fields, className);
        extendBuilderClass(classBuilder, typeElement, fields, className);

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
     * Collects the fields of the builder that hold a value of their declared type, which are
     * the only ones that the builder mixin methods can be applied to.
     *
     * @param fields the fields of the builder
     * @return the fields that hold a value of their declared type
     */
    private SequencedCollection<? extends DaedalusFieldElement> collectMixinFields(SequencedCollection<? extends DaedalusFieldElement> fields) {
        return fields.stream()
                .filter(field -> getBuilderFieldType(field).equals(TypeName.get(field.type().descriptorElementType())))
                .toList();
    }

    /**
     * Generates the fields of the builder class.
     *
     * @param classBuilder the class builder
     * @param fields the fields to generate
     * @param typeElement the type element being built
     */
    private void generateFields(TypeSpec.Builder classBuilder, SequencedCollection<? extends DaedalusFieldElement> fields, DaedalusTypeElement typeElement) {
        var modifiers = getFieldModifiers().toArray(Modifier[]::new);
        for (var field : fields) {
            var fieldSpec = FieldSpec.builder(getBuilderFieldType(field), field.name())
                    .addModifiers(modifiers)
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
     * Returns the modifiers of the builder class generated for the given type.
     *
     * @implSpec
     * The default implementation returns {@link Modifier#PUBLIC}.
     *
     * @param typeElement the type element being built
     * @return the modifiers of the builder class
     */
    protected List<Modifier> getClassModifiers(DaedalusTypeElement typeElement) {
        return List.of(Modifier.PUBLIC);
    }

    /**
     * Returns the modifiers of the fields of the builder class that hold the values of the
     * fields of the type.
     *
     * @implSpec
     * The default implementation returns {@link Modifier#PRIVATE}.
     *
     * @return the modifiers of the fields
     */
    protected List<Modifier> getFieldModifiers() {
        return List.of(Modifier.PRIVATE);
    }

    /**
     * Returns the type of the field of the builder class that holds the value of the given
     * field, which is also the type of the parameter of its setter.
     *
     * <p>Builder mixin methods are only generated for a field whose builder field has its
     * declared type.
     *
     * @implSpec
     * The default implementation returns the declared type of the field.
     *
     * @param field the field element
     * @return the type of the field of the builder
     */
    protected TypeName getBuilderFieldType(DaedalusFieldElement field) {
        return TypeName.get(field.type().descriptorElementType());
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
     * Hook for subclasses to add extra methods to the builder class, after the terminal
     * build method.
     *
     * <p>The default implementation does nothing.
     *
     * @param classBuilder the class builder to mutate
     * @param typeElement the type element being built
     * @param fields the builder fields
     * @param className the builder class name
     */
    protected void extendBuilderClass(
            TypeSpec.Builder classBuilder,
            DaedalusTypeElement typeElement,
            SequencedCollection<? extends DaedalusFieldElement> fields,
            String className
    ) {

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
            for (var setter : createSetters(field, className)) {
                classBuilder.addMethod(setter);
            }
        }
    }

    /**
     * Creates the setters of the given field.
     *
     * @implSpec
     * The default implementation returns a single setter, named after the field, whose
     * parameter is the {@linkplain #getBuilderFieldType(DaedalusFieldElement) type of the
     * field of the builder}.
     *
     * @param field the field element
     * @param className the builder class name (for return type)
     * @return the setters of the field
     */
    protected List<MethodSpec> createSetters(DaedalusFieldElement field, String className) {
        return List.of(createBuilderSetter(field.name(), field.name(), getBuilderFieldType(field), className));
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
        return createBuilderSetter(fieldName, fieldValue, TypeName.get(fieldType), className);
    }

    /**
     * Creates a fluent setter method for a builder field.
     *
     * @param fieldName the field name (used as both parameter name and assignment target)
     * @param fieldValue the value expression to assign
     * @param parameterType the parameter type
     * @param className the builder class name (for return type)
     * @return the generated setter method
     */
    protected MethodSpec createBuilderSetter(String fieldName, String fieldValue, TypeName parameterType, String className) {
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
