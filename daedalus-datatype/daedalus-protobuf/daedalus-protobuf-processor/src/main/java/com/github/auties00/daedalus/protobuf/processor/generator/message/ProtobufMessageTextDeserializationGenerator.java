package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import java.util.List;

/**
 * A deserialization method generator that produces the {@code decodeText(ProtobufTextReader)}
 * method of protobuf message types, which decodes a message written in the text format.
 *
 * <p>Like the {@code decodeBinary} method, the generated method decodes the fields of the
 * message into a new builder, through the text {@code decodePartial} method, and builds the
 * message, converting the exception thrown for a required field that is not set into a
 * {@code ProtobufDeserializationException}.
 */
public class ProtobufMessageTextDeserializationGenerator extends ProtobufMessageDeserializationGenerator {
    /**
     * The name of the generated method, which decodes a message from the text format.
     */
    public static final String TEXT_METHOD_NAME = "decodeText";

    /**
     * Constructs a new text deserialization generator for the given message element.
     *
     * @param element the protobuf message element to generate the method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageTextDeserializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#decodeText(ProtobufTextReader)}
     */
    @Override
    public List<AnnotationSpec> annotations() {
        return List.of(AnnotationSpec.builder(Override.class).build());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #TEXT_METHOD_NAME}
     */
    @Override
    protected String name() {
        return TEXT_METHOD_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the text reader type
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ProtobufTextReader.class));
    }
}
