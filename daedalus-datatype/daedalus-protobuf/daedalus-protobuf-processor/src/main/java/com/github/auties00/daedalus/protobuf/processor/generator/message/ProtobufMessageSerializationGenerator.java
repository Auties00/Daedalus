package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufSizeTable;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.WildcardTypeName;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A serialization method generator that produces the main {@code encode} method for
 * protobuf message types.
 *
 * <p>The generated method validates that all required fields are set, then iterates over
 * the message's fields and writes each one to the provided {@link ProtobufBinaryWriter}
 * using the appropriate write method for its protobuf type. Collection, map, and scalar
 * fields are each handled with their own serialization strategy, and the unknown fields
 * of the message, if it keeps them, are written last.
 */
public class ProtobufMessageSerializationGenerator extends ProtobufSerializationGenerator {
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";
    private static final String OUTPUT_OBJECT_PARAMETER = "protoOutputStream";
    private static final String UNKNOWN_FIELDS_VALUE = "protoUnknownFieldsValue";
    private static final String UNKNOWN_FIELDS_STORE = "protoUnknownFields";

    /**
     * The protobuf message element whose serialization method is being generated.
     */
    private final ProtobufMessageElement messageElement;

    /**
     * Constructs a new message serialization generator for the given message element.
     *
     * @param element the protobuf message element to generate a serialization method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageSerializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.messageElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns immediately for {@code null} input, validates
     * required fields, serializes each field, and writes the unknown fields.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return");
        methodBuilder.endControlFlow();

        createRequiredPropertiesNullCheck(methodBuilder);
        for(var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                writeOneOfSerializer(methodBuilder, oneOf);
                continue;
            }

            switch (property.type()) {
                case ProtobufArrayFieldType arrayType -> writeArraySerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), arrayType, property.packed(), property.delimited());
                case ProtobufCollectionFieldType collectionType -> writeRepeatedSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), collectionType, property.packed(), property.delimited(), false);
                case ProtobufMapFieldType mapType -> writeMapSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), mapType);
                case ProtobufSimpleFieldType simpleType -> writeNormalSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), simpleType, true, true, false, property.implicit(), property.delimited(), null);
                case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled before the switch");
            }
        }

        // Only the unknown fields read from the binary format can be written to it
        messageElement.unknownFieldsElement().ifPresent(unknownFieldsElement -> {
            methodBuilder.addStatement("var $L = $L", UNKNOWN_FIELDS_VALUE, getAccessorCall(unknownFieldsElement.accessor()));
            methodBuilder.beginControlFlow("if ($L != null && $L instanceof $T $L)", UNKNOWN_FIELDS_VALUE, unknownFieldsElement.toStore(UNKNOWN_FIELDS_VALUE), ProtobufUnknownFields.class, UNKNOWN_FIELDS_STORE);
            methodBuilder.addStatement("$L.encodeBinary($L)", UNKNOWN_FIELDS_STORE, OUTPUT_OBJECT_PARAMETER);
            methodBuilder.endControlFlow();
        });
    }


    /**
     * Generates the statements that write the case of the given oneof that is set, if any.
     *
     * <p>The value of the oneof is matched against the type of each case, a subtype before
     * its supertypes, and is written like a field of the same kind whose value is the
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
    private void writeOneOfSerializer(MethodSpec.Builder methodBuilder, ProtobufFieldElement.OneOf oneOf) {
        var caseValue = oneOf.name() + "Case";
        methodBuilder.addStatement("var $L = $L", oneOf.name(), getAccessorCall(oneOf.accessor()));
        methodBuilder.beginControlFlow("switch ($L)", oneOf.name());
        methodBuilder.addCode("case null -> {}\n");
        for (var oneOfCase : oneOf.cases()) {
            methodBuilder.beginControlFlow("case $T $L ->", TypeName.get(oneOfCase.type().descriptorElementType()), caseValue);
            writeNormalSerializer(methodBuilder, oneOfCase.index(), caseValue, caseValue, (ProtobufValueFieldType) oneOfCase.type(), false, false, false, false, oneOfCase.delimited(), null);
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Adds a check that throws a {@link ProtobufSerializationException} for every required
     * field that is not set to the method builder.
     *
     * @param methodBuilder the method builder to add the checks to
     */
    private void createRequiredPropertiesNullCheck(MethodSpec.Builder methodBuilder) {
        for (var property : messageElement.fields()) {
            if (!property.required()) {
                continue;
            }

            methodBuilder.beginControlFlow("if ($L == null)", getAccessorCall(property.accessor()));
            methodBuilder.addStatement("throw $T.missingRequiredField($S)", ProtobufSerializationException.class, property.textName());
            methodBuilder.endControlFlow();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since message serialization is always generated
     */
    @Override
    public boolean shouldInstrument() {
        return true;
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
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#encodeBinary(Object, ProtobufBinaryWriter, ProtobufSizeTable)}
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
     * @return {@link TypeName#VOID}, since the method writes directly to the output stream
     */
    @Override
    protected TypeName returnType() {
        return TypeName.VOID;
    }

    /**
     * {@inheritDoc}
     *
     * @return the message type, the binary writer and the size table
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()), ParameterizedTypeName.get(ClassName.get(ProtobufBinaryWriter.class), WildcardTypeName.subtypeOf(Object.class)), ClassName.get(ProtobufSizeTable.class));
    }

    /**
     * {@inheritDoc}
     *
     * @return the parameter names matching the types returned by {@link #parametersTypes()}
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER, OUTPUT_OBJECT_PARAMETER, SIZE_TABLE_PARAMETER);
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
