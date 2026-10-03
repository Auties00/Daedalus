package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A method generator that produces the {@code nameOf(int)} method of protobuf enum types,
 * which returns the Protocol Buffers name of the constant that has a given index.
 *
 * <p>The text format writes an enum value by the name of its constant, and by its index if
 * the enum has no constant for it. For example, given an enum whose constants {@code RED} and
 * {@code GREEN} have indexes {@code 0} and {@code 1}:
 *
 * <pre>{@code
 * public static String nameOf(int protoEnumIndex) {
 *     return switch (protoEnumIndex) {
 *         case 0 -> "RED";
 *         case 1 -> "GREEN";
 *         default -> null;
 *     };
 * }
 * }</pre>
 *
 * <p>An enum whose values are created by a deserializer has no constants, so the method always
 * returns {@code null}.
 */
public class ProtobufEnumNameGenerator extends DaedalusMethodGenerator {
    /**
     * The name of the generated method.
     */
    public static final String METHOD_NAME = "nameOf";

    /**
     * The name of the parameter that holds the index.
     */
    private static final String ENUM_INDEX_PARAMETER = "protoEnumIndex";

    /**
     * The protobuf enum element whose method is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new generator for the given enum element.
     *
     * @param enumElement the protobuf enum element
     */
    public ProtobufEnumNameGenerator(ProtobufEnumElement enumElement) {
        this.enumElement = enumElement;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a switch on the index that returns the name of each constant.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        var switchBlock = CodeBlock.builder()
                .add("return switch ($L) {\n", ENUM_INDEX_PARAMETER)
                .indent();
        for (var constant : enumElement.fields()) {
            switchBlock.add("case $L -> $S;\n", constant.index(), constant.textName());
        }
        switchBlock.add("default -> null;\n");
        switchBlock.unindent().add("}");
        methodBuilder.addStatement(switchBlock.build());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since every enum can be written in the text format
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#nameOf(int)}
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
     * @return the {@code String} type
     */
    @Override
    protected TypeName returnType() {
        return ClassName.get(String.class);
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
     * @return the {@code int} type of the index
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(TypeName.INT);
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the parameter that holds the index
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(ENUM_INDEX_PARAMETER);
    }
}
