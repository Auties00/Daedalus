package com.github.auties00.daedalus.protobuf.processor.generator.message;

import javax.lang.model.element.Modifier;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufUninitializedMessageException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import java.util.List;

/**
 * A deserialization method generator that produces the {@code decodeBinary(ProtobufBinaryReader)}
 * method of protobuf message types, which is their deserializer.
 *
 * <p>The generated method decodes the fields of the message into a new builder, through the
 * {@code decodePartial} method implemented by
 * {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec}, and builds the message. A
 * required field that is not set, in the message or in a nested message, makes the builder
 * throw a {@link ProtobufUninitializedMessageException}, which is converted into a
 * {@link ProtobufDeserializationException}, as the input is not a valid encoding of the
 * message.
 */
public class ProtobufMessageDeserializationGenerator extends ProtobufDeserializationGenerator {
    /**
     * The name of the parameter that holds the reader.
     */
    private static final String INPUT_STREAM_NAME = "protoInputStream";

    /**
     * The name of the local variable that holds the builder.
     */
    private static final String BUILDER_NAME = "protoBuilder";

    /**
     * Constructs a new generator for the given message element.
     *
     * @param element the protobuf message element to generate the deserializer for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageDeserializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the statements that decode the fields into a new builder and build the
     * message, converting a missing required field into a
     * {@link ProtobufDeserializationException}.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("var $L = $L($L)", BUILDER_NAME, PARTIAL_METHOD_NAME, INPUT_STREAM_NAME);
        methodBuilder.beginControlFlow("try");
        methodBuilder.addStatement("return $L.build()", BUILDER_NAME);
        methodBuilder.nextControlFlow("catch ($T exception)", ProtobufUninitializedMessageException.class);
        methodBuilder.addStatement("throw $T.missingRequiredField(exception)", ProtobufDeserializationException.class);
        methodBuilder.endControlFlow();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since the deserializer of a message is always generated
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, followed by the annotations of the converter, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#decodeBinary(ProtobufBinaryReader)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        return List.of(AnnotationSpec.builder(Override.class).build(), AnnotationSpec.builder(TypeDeserializer.class).build());
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
     * @return the type name of the message being deserialized
     */
    @Override
    protected TypeName returnType() {
        return ClassName.get(ownerElement.typeElement());
    }

    /**
     * {@inheritDoc}
     *
     * @return the binary reader type
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ProtobufBinaryReader.class));
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the reader parameter
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_STREAM_NAME);
    }
}
