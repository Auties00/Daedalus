package com.github.auties00.daedalus.protobuf.processor.generator.enumeration;

import javax.lang.model.element.Modifier;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufEnumElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.palantir.javapoet.*;

import java.util.List;

/**
 * A deserialization method generator that produces a convenience {@code decode(Integer)}
 * overload for protobuf enum types.
 *
 * <p>The generated method delegates to the two-argument
 * {@code decode(Integer, defaultValue)} method with {@code null} as the default value.
 * This provides a simpler API for callers who do not need to specify a fallback constant.
 */
public class ProtobufEnumDeserializationOverloadGenerator extends ProtobufDeserializationGenerator {
    private static final String ENUM_INDEX_PARAMETER = "protoEnumIndex";

    /**
     * The protobuf enum element whose deserialization overload is being generated.
     */
    private final ProtobufEnumElement enumElement;

    /**
     * Constructs a new enum deserialization overload generator for the given enum element.
     *
     * @param element the protobuf enum element to generate the convenience overload for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufEnumDeserializationOverloadGenerator(ProtobufEnumElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.enumElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a single delegation statement that calls the two-argument
     * {@code decode} method with {@code null} as the default value.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return $L($L, null)", name(), ENUM_INDEX_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since the convenience overload is always generated
     */
    @Override
    public boolean shouldInstrument() {
        // Only a Java enum has a decode method with a default value to delegate to
        return enumElement.javaEnum();
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
     * @return a list containing the {@link Integer} index type
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(Integer.class));
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the index parameter name
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(ENUM_INDEX_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufEnumSpec#decode(Integer)}
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
     * @return {@value ProtobufEnumDeserializationGenerator#METHOD_NAME}
     */
    @Override
    protected String name() {
        return ProtobufEnumDeserializationGenerator.METHOD_NAME;
    }
}
