package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A serialization method generator that produces the {@code encode(EnumType)} method
 * for protobuf enum types.
 *
 * <p>The generated method returns the protobuf index value of the given enum constant.
 * For Java enums, the ordinal is returned directly. For non-Java enums (classes
 * implementing the protobuf enum contract), the value of the explicit index field is
 * returned instead. If the input is {@code null}, the method returns {@code null}.
 */
public class ProtobufEnumSerializationGenerator extends ProtobufSerializationGenerator {
    /**
     * The name of the generated method, which converts a value to its index for both the
     * binary and the text format.
     */
    public static final String METHOD_NAME = "encode";
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";

    /**
     * The protobuf enum element whose serialization method is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new enum serialization generator for the given enum element.
     *
     * @param element the protobuf enum element to generate a serialization method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufEnumSerializationGenerator(ProtobufEnumElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.enumElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns {@code null} for {@code null} input, the ordinal
     * for Java enums, or the explicit index field value for non-Java enums.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return null");
        methodBuilder.endControlFlow();

        var enumType = ClassName.get(ownerElement.typeElement());
        var serializer = enumElement.serializer();
        if (serializer.isPresent()) {
            // The type computes the index of any of its values
            methodBuilder.addStatement("return $L.$L()", INPUT_OBJECT_PARAMETER, serializer.get().getSimpleName());
            return;
        }

        if (!enumElement.javaEnum()) {
            // A value that is not one of the constants has no index
            for (var constant : enumElement.fields()) {
                methodBuilder.beginControlFlow("if ($L.equals($T.$L))", INPUT_OBJECT_PARAMETER, enumType, constant.name());
                methodBuilder.addStatement("return $L", constant.index());
                methodBuilder.endControlFlow();
            }
            methodBuilder.addStatement("return null");
            return;
        }

        var switchBlock = CodeBlock.builder()
                .add("return switch ($L) {\n", INPUT_OBJECT_PARAMETER)
                .indent();
        for (var constant : enumElement.fields()) {
            switchBlock.add("case $T.$L -> $L;\n", enumType, constant.name(), constant.index());
        }
        switchBlock.unindent().add("}");
        methodBuilder.addStatement(switchBlock.build());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since enum serialization is always generated
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, followed by the {@link TypeSerializer} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#encode(Object)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        return List.of(AnnotationSpec.builder(Override.class).build(), AnnotationSpec.builder(TypeSerializer.class).build());
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@code public} modifier, as the generated method is an
     *         instance method of the Spec class
     */
    @Override
    protected List<Modifier> modifiers() {
        return List.of(Modifier.PUBLIC);
    }

    /**
     * {@inheritDoc}
     *
     * @return the boxed {@link Integer} type, representing the serialized enum index
     */
    @Override
    protected TypeName returnType() {
        return ClassName.get(Integer.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the enum type as the sole parameter type
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()));
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the input object parameter name
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #METHOD_NAME}
     */
    @Override
    protected String name() {
        return METHOD_NAME;
    }
}
