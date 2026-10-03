package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A method generator that produces the {@code indexOf(String)} method of protobuf enum types,
 * which returns the index of the constant that has a given Protocol Buffers name.
 *
 * <p>The text format can write an enum value by the name of its constant: the name is
 * converted to the index of the constant, which is then decoded like an index read from the
 * binary format. A name that no constant has cannot be decoded, like in the official protobuf
 * library. For example, given an enum named {@code Color} whose constants {@code RED} and
 * {@code GREEN} have indexes {@code 0} and {@code 1}:
 *
 * <pre>{@code
 * public static int indexOf(String protoEnumName) throws ProtobufDeserializationException {
 *     return switch (protoEnumName) {
 *         case "RED" -> 0;
 *         case "GREEN" -> 1;
 *         default -> throw ProtobufDeserializationException.unknownEnumConstant("Color", protoEnumName);
 *     };
 * }
 * }</pre>
 */
public class ProtobufEnumIndexGenerator extends DaedalusMethodGenerator {
    /**
     * The name of the generated method.
     */
    public static final String METHOD_NAME = "indexOf";

    /**
     * The name of the parameter that holds the name.
     */
    private static final String ENUM_NAME_PARAMETER = "protoEnumName";

    /**
     * The protobuf enum element whose method is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new generator for the given enum element.
     *
     * @param enumElement the protobuf enum element
     */
    public ProtobufEnumIndexGenerator(ProtobufEnumElement enumElement) {
        this.enumElement = enumElement;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a switch on the name that returns the index of each constant, and throws
     * for any other name.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        // An enum whose values are created by a deserializer has no constants, so no name can be decoded
        if (enumElement.fields().isEmpty()) {
            methodBuilder.addStatement("throw $T.unknownEnumConstant($S, $L)", ProtobufDeserializationException.class, ProtobufTypeElement.getProtobufName(enumElement.typeElement()), ENUM_NAME_PARAMETER);
            return;
        }

        var switchBlock = CodeBlock.builder()
                .add("return switch ($L) {\n", ENUM_NAME_PARAMETER)
                .indent();
        for (var constant : enumElement.fields()) {
            switchBlock.add("case $S -> $L;\n", constant.textName(), constant.index());
        }
        switchBlock.add("case null, default -> throw $T.unknownEnumConstant($S, $L);\n", ProtobufDeserializationException.class, ProtobufTypeElement.getProtobufName(enumElement.typeElement()), ENUM_NAME_PARAMETER);
        switchBlock.unindent().add("}");
        methodBuilder.addStatement(switchBlock.build());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since every enum can be read from the text format
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#indexOf(String)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        return List.of(AnnotationSpec.builder(Override.class).build());
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
     * @return the {@code int} type of the index
     */
    @Override
    protected TypeName returnType() {
        return TypeName.INT;
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

    /**
     * {@inheritDoc}
     *
     * @return the {@code String} type of the name
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(String.class));
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the parameter that holds the name
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(ENUM_NAME_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ProtobufDeserializationException}, thrown for a name that no constant has
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return List.of(ClassName.get(ProtobufDeserializationException.class));
    }
}
