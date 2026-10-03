package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import javax.lang.model.element.Modifier;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import java.util.List;

/**
 * A deserialization method generator that produces the main {@code decode(Integer, EnumType)}
 * method for protobuf enum types.
 *
 * <p>The generated method switches on the protobuf index and returns the corresponding
 * enum constant, or the provided default value if no constant has that index.
 *
 * <p>An index without a constant is not an error, for {@link ProtobufEnumType#OPEN OPEN}
 * and {@link ProtobufEnumType#CLOSED CLOSED} enums alike: when the default value is
 * {@code null}, the message that holds the enum keeps the index in its unknown fields, so
 * that it is written back when the message is serialized. Reserved indexes are treated
 * like any other index without a constant.
 */
public class ProtobufEnumDeserializationGenerator extends ProtobufDeserializationGenerator {
    /**
     * The name of the generated method, which converts an index to a value for both the
     * binary and the text format.
     */
    public static final String METHOD_NAME = "decode";
    private static final String ENUM_INDEX_PARAMETER = "protoEnumIndex";
    private static final String ENUM_DEFAULT_VALUE_PARAMETER = "defaultValue";

    /**
     * The protobuf enum element whose deserialization method is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new enum deserialization generator for the given enum element.
     *
     * @param element the protobuf enum element to generate a deserialization method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufEnumDeserializationGenerator(ProtobufEnumElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.enumElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that switches on the index to return the corresponding enum
     * constant, or the default value for a {@code null} index or an index without a
     * constant.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        var enumType = ClassName.get(ownerElement.typeElement());
        var deserializer = enumElement.deserializer();
        if (deserializer.isPresent()) {
            // The type creates the value of any index
            methodBuilder.beginControlFlow("if ($L == null)", ENUM_INDEX_PARAMETER);
            methodBuilder.addStatement("return $L", defaultValue());
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("var protoEnumValue = $T.$L($L)", enumType, deserializer.get().getSimpleName(), ENUM_INDEX_PARAMETER);
            methodBuilder.addStatement("return protoEnumValue != null ? protoEnumValue : $L", defaultValue());
            return;
        }

        var switchBlock = CodeBlock.builder()
                .add("return switch ($L) {\n", ENUM_INDEX_PARAMETER)
                .indent();
        for (var constant : enumElement.fields()) {
            switchBlock.add("case $L -> $T.$L;\n", constant.index(), enumType, constant.name());
        }
        switchBlock.add("case null, default -> $L;\n", defaultValue());
        switchBlock.unindent().add("}");
        methodBuilder.addStatement(switchBlock.build());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since enum deserialization is always generated
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link TypeDeserializer} annotation, preceded by the
     *         {@link Override} annotation if the enum is not a Java enum, as the generated
     *         method then implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#decode(Integer)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        if (!enumElement.javaEnum()) {
            return List.of(AnnotationSpec.builder(Override.class).build(), AnnotationSpec.builder(TypeDeserializer.class).build());
        }
        return List.of(AnnotationSpec.builder(TypeDeserializer.class).build());
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
     * @return the type name of the enum being deserialized
     */
    @Override
    protected TypeName returnType() {
        return ClassName.get(ownerElement.typeElement());
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Integer} index type and the enum type for the default value
     */
    @Override
    protected List<TypeName> parametersTypes() {
        if (!enumElement.javaEnum()) {
            return List.of(ClassName.get(Integer.class));
        }
        return List.of(ClassName.get(Integer.class), ClassName.get(ownerElement.typeElement()));
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the index parameter name and the default value parameter name
     */
    @Override
    protected List<String> parametersNames() {
        if (!enumElement.javaEnum()) {
            return List.of(ENUM_INDEX_PARAMETER);
        }
        return List.of(ENUM_INDEX_PARAMETER, ENUM_DEFAULT_VALUE_PARAMETER);
    }

    /**
     * Returns the value returned for an index that has no value.
     *
     * <p>The value of a Java enum can be supplied by the use site through the generated
     * parameters annotation, which can only hold the constants of a Java enum: the value
     * of any other type is {@code null}, so that the index is kept in the unknown fields of
     * the message.
     *
     * @return the name of the default value parameter, or {@code null}
     */
    private String defaultValue() {
        return enumElement.javaEnum() ? ENUM_DEFAULT_VALUE_PARAMETER : "null";
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
