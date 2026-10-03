package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import javax.lang.model.element.Modifier;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSizeGenerator;
import com.github.auties00.daedalus.typesystem.annotation.TypeSize;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import java.util.List;

/**
 * A size calculation method generator that produces the {@code sizeOfBinary(EnumType)} method
 * for protobuf enum types.
 *
 * <p>The generated method computes the varint-encoded byte size of the enum's protobuf
 * index value. For Java enums, the ordinal is used; for non-Java enums, the explicit
 * index field is used. If the input is {@code null}, the method returns {@code 0}.
 */
public class ProtobufEnumSizeGenerator extends ProtobufSizeGenerator {
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";

    /**
     * The protobuf enum element whose size calculation method is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new enum size generator for the given enum element.
     *
     * @param element the protobuf enum element to generate a size calculation method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufEnumSizeGenerator(ProtobufEnumElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.enumElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns {@code 0} for {@code null} input, or the varint
     * size of the enum's index value otherwise.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return 0");
        methodBuilder.endControlFlow();

        if (!enumElement.javaEnum()) {
            // The index of a value that is not a Java enum constant is computed by the serializer
            methodBuilder.addStatement("var protoEnumIndex = $L($L)", ProtobufEnumSerializationGenerator.METHOD_NAME, INPUT_OBJECT_PARAMETER);
            methodBuilder.addStatement("return protoEnumIndex == null ? 0 : $T.getVarInt32Size(protoEnumIndex)", ProtobufBinarySizeCalculator.class);
            return;
        }

        var enumType = ClassName.get(ownerElement.typeElement());
        var switchBlock = CodeBlock.builder()
                .add("return switch ($L) {\n", INPUT_OBJECT_PARAMETER)
                .indent();
        for (var constant : enumElement.fields()) {
            switchBlock.add("case $T.$L -> $L;\n", enumType, constant.name(), ProtobufBinarySizeCalculator.getVarInt32Size(constant.index()));
        }
        switchBlock.unindent().add("}");
        methodBuilder.addStatement(switchBlock.build());
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, followed by the {@link TypeSize} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#sizeOfBinary(Object)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        return List.of(AnnotationSpec.builder(Override.class).build(), AnnotationSpec.builder(TypeSize.class).build());
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
}
