package com.github.auties00.daedalus.protobuf.processor.generator.message;

import javax.lang.model.element.Modifier;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSizeGenerator;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufSizeTable;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import java.util.List;

/**
 * A size calculation method generator that produces the {@code sizeOfBinary} method for
 * protobuf message types.
 *
 * <p>The generated method accumulates the serialized byte size of all fields in the
 * message, including field tag sizes and data sizes. Collection fields, map fields,
 * and scalar fields are each handled with their own size calculation strategy.
 *
 * <p>The size of a message does not include its tag and length prefix, or the start and
 * end tags of a group: they are counted by the message that holds it.
 *
 * <p>If the input message is {@code null}, the method returns {@code 0}.
 */
public class ProtobufMessageSizeGenerator extends ProtobufSizeGenerator {
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";
    private static final String OUTPUT_SIZE_NAME = "protoOutputSize";
    private static final String UNKNOWN_FIELDS_VALUE = "protoUnknownFieldsValue";

    /**
     * The protobuf message element whose size calculation method is being generated.
     */
    private final ProtobufMessageElement messageElement;

    /**
     * Constructs a new message size generator for the given message element.
     *
     * @param element the protobuf message element to generate a size calculation method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageSizeGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.messageElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns {@code 0} for {@code null} input, initializes a
     * size accumulator, iterates over all fields to accumulate their sizes, adds the size
     * of the unknown fields, and returns the total.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return 0");
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("var $L = 0", OUTPUT_SIZE_NAME);

        for(var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                writeOneOfSize(methodBuilder, oneOf);
                continue;
            }

            switch (property.type()) {
                case ProtobufArrayFieldType arrayType -> writeArraySize(
                        methodBuilder,
                        property.index(),
                        property.name(),
                        getAccessorCall(property.accessor()),
                        property.packed(),
                        property.delimited(),
                        arrayType
                );
                case ProtobufCollectionFieldType collectionType -> writeRepeatedSize(
                        methodBuilder,
                        property.index(),
                        property.name(),
                        getAccessorCall(property.accessor()),
                        property.packed(),
                        property.delimited(),
                        collectionType,
                        false
                );
                case ProtobufMapFieldType mapType -> writeMapSize(
                        classBuilder,
                        methodBuilder,
                        property.index(),
                        property.name(),
                        getAccessorCall(property.accessor()),
                        mapType,
                        false
                );
                case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled before the switch");
                case ProtobufSimpleFieldType simpleType -> {
                    var accessorCall = getAccessorCall(INPUT_OBJECT_PARAMETER, property.accessor());
                    writeNormalSize(
                            methodBuilder,
                            property.index(),
                            property.name(),
                            simpleType,
                            null,
                            accessorCall,
                            property.implicit(),
                            property.delimited()
                    );
                }
            }
        }

        messageElement.unknownFieldsElement().ifPresent(unknownFieldsElement -> {
            methodBuilder.addStatement("var $L = $L", UNKNOWN_FIELDS_VALUE, getAccessorCall(unknownFieldsElement.accessor()));
            methodBuilder.beginControlFlow("if ($L != null && $L instanceof $T protoUnknownFields)", UNKNOWN_FIELDS_VALUE, unknownFieldsElement.toStore(UNKNOWN_FIELDS_VALUE), ProtobufUnknownFields.class);
            methodBuilder.addStatement("$L += protoUnknownFields.wireSize()", OUTPUT_SIZE_NAME);
            methodBuilder.endControlFlow();
        });

        methodBuilder.addStatement("return $L", OUTPUT_SIZE_NAME);
    }

    /**
     * Generates the statements that count the size of the case of the given oneof that is set, if any.
     *
     * <p>The value of the oneof is matched against the type of each case, a subtype before
     * its supertypes, and its size is counted like a field of the same kind whose value is the
     * matched value. For example, given a oneof named "shape" whose cases hold {@code Circle}
     * and {@code Label}:
     *
     * <pre>{@code
     * var shape = protoInputObject.shape();
     * switch (shape) {
     *     case null -> {}
     *     case Circle shapeCase -> {
     *         // the statements of a field of type Circle, whose value is shapeCase
     *     }
     *     case Label shapeCase -> {
     *         // the statements of a field of type Label, whose value is shapeCase
     *     }
     * }
     * }</pre>
     *
     * @param methodBuilder the method builder to append the statements to
     * @param oneOf the oneof
     */
    private void writeOneOfSize(MethodSpec.Builder methodBuilder, ProtobufFieldElement.OneOf oneOf) {
        var caseValue = oneOf.name() + "Case";
        methodBuilder.addStatement("var $L = $L", oneOf.name(), getAccessorCall(INPUT_OBJECT_PARAMETER, oneOf.accessor()));
        methodBuilder.beginControlFlow("switch ($L)", oneOf.name());
        methodBuilder.addCode("case null -> {}\n");
        for (var oneOfCase : oneOf.cases()) {
            methodBuilder.beginControlFlow("case $T $L ->", TypeName.get(oneOfCase.type().descriptorElementType()), caseValue);
            writeNormalSize(methodBuilder, oneOfCase.index(), caseValue, (ProtobufSimpleFieldType) oneOfCase.type(), null, null, false, oneOfCase.delimited());
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#sizeOfBinary(Object, ProtobufSizeTable)}
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
     * @return {@code true}, as the generated method takes the size table of the message
     */
    @Override
    protected boolean usesSizeTable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return the message type and the size table
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()), ClassName.get(ProtobufSizeTable.class));
    }

    /**
     * {@inheritDoc}
     *
     * @return the parameter names matching the types returned by {@link #parametersTypes()}
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER, SIZE_TABLE_PARAMETER);
    }

    /**
     * Returns the accessor expression for the given element on the input object.
     *
     * @param accessor the element to access (field or method)
     * @return the accessor call expression string
     */
    private String getAccessorCall(Element accessor) {
        return getAccessorCall(INPUT_OBJECT_PARAMETER, accessor);
    }
}
