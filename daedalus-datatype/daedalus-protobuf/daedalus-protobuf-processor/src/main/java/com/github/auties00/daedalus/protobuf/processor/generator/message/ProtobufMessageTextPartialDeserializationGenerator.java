package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufTextConverters;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderTypeGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.enumeration.ProtobufEnumIndexGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A deserialization method generator that produces the
 * {@code decodePartial(ProtobufTextReader, MessageBuilder)} method of protobuf message types,
 * which decodes the fields of a message written in the text format into its builder.
 *
 * <p>The generated method opens the message, switches on the name of each field to store it in
 * the builder, and closes the message, so it decodes both a top level message and a nested
 * one, whose fields are enclosed in braces or angle brackets. Like the binary
 * {@code decodePartial} method, it merges the fields with the values that the builder already
 * holds, like {@code merge} in the {@code TextFormat} parser of the official protobuf library:
 * <ul>
 * <li>a field that is not repeated replaces the previous value;
 * <li>a field that holds the builder of a message is decoded into that builder;
 * <li>a repeated or map field is appended to, and accepts both its values written one by one
 *     and a list of values enclosed in square brackets;
 * <li>an enum value can be written by the name of its constant, which is converted to its
 *     index by the {@code indexOf} method of the Spec of the enum, or by its index;
 * <li>a group can be written by its name or by the name of its message type;
 * <li>a field that the message does not declare is added to the unknown fields of the
 *     builder, if the message keeps them, and throws a
 *     {@link ProtobufDeserializationException} otherwise, like in the official protobuf
 *     library.
 * </ul>
 *
 * <p>For example, given a message with a {@code string} field named "name" and a repeated
 * {@code int32} field named "numbers":
 *
 * <pre>{@code
 * protoInputStream.readStartMessage();
 * while (protoInputStream.nextField()) {
 *     switch (protoInputStream.fieldName()) {
 *         case "name":
 *             protoBuilder.name = protoInputStream.readStringField();
 *             break;
 *         case "numbers":
 *             if (protoInputStream.readStartList()) {
 *                 while (protoInputStream.nextListElement()) {
 *                     protoBuilder.numbers = addNumbers(protoBuilder.numbers, protoInputStream.readInt32Field());
 *                 }
 *             } else {
 *                 protoBuilder.numbers = addNumbers(protoBuilder.numbers, protoInputStream.readInt32Field());
 *             }
 *             break;
 *         default:
 *             throw ProtobufDeserializationException.unknownField("Person", protoInputStream.fieldName());
 *     }
 * }
 * protoInputStream.readEndMessage();
 * }</pre>
 */
public class ProtobufMessageTextPartialDeserializationGenerator extends ProtobufMessagePartialDeserializationGenerator {
    /**
     * The name of the parameter that holds the reader.
     */
    private static final String INPUT_STREAM_NAME = "protoInputStream";

    /**
     * The name of the key of a map entry in the text format.
     */
    private static final String MAP_KEY_NAME = "key";

    /**
     * The name of the value of a map entry in the text format.
     */
    private static final String MAP_VALUE_NAME = "value";

    /**
     * Constructs a new text partial deserialization generator for the given message element.
     *
     * @param element the protobuf message element to generate the method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageTextPartialDeserializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the buffers of the repeated fields held by arrays or read in bulk, the
     * field reading loop, and the statements that append the buffers to the fields of the
     * builder. The methods that add an element to a collection, or put an entry in a map, are
     * the ones generated for the binary {@code decodePartial} method.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        for (var property : messageElement.fields()) {
            if (!property.isSynthetic() && isBuffered(property)) {
                var buffer = getBuffer(property);
                methodBuilder.addStatement("$T $L = $L", getBufferType(property), buffer, getBufferDefaultValue(property));
                methodBuilder.addStatement("int $LCount = 0", buffer);
            }
        }

        var unknownFields = messageElement.unknownFieldsElement().isPresent() ? getFieldTarget(UNKNOWN_FIELDS_NAME) : null;
        var cases = new ArrayList<TextCase>();
        for (var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                for (var oneOfCase : oneOf.cases()) {
                    cases.add(new TextCase(ProtobufTextNames.getReadNames(oneOfCase), writeOneOfCaseDeserializer(oneOf, oneOfCase, unknownFields)));
                }
                continue;
            }

            cases.add(new TextCase(ProtobufTextNames.getReadNames(property), writeFieldDeserializer(property, unknownFields)));
        }

        methodBuilder.addStatement("$L.readStartMessage()", INPUT_STREAM_NAME);
        methodBuilder.beginControlFlow("while ($L.nextField())", INPUT_STREAM_NAME);
        methodBuilder.beginControlFlow("switch ($L.fieldName())", INPUT_STREAM_NAME);
        var labels = new HashSet<String>();
        for (var textCase : cases) {
            labels.add(textCase.names().getFirst());
        }
        for (var textCase : cases) {
            var caseLabel = CodeBlock.builder();
            for (var i = 0; i < textCase.names().size(); i++) {
                var name = textCase.names().get(i);
                // The name of the type of a group is only an alias, so the name of another field wins
                if (i == 0 || labels.add(name)) {
                    caseLabel.add("case $S:\n", name);
                }
            }
            caseLabel.indent().add(textCase.body()).unindent();
            methodBuilder.addCode(caseLabel.build());
        }
        methodBuilder.addCode(CodeBlock.builder()
                .add("default:\n")
                .indent()
                .add(writeUnknownFieldDeserializer())
                .unindent()
                .build());
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("$L.readEndMessage()", INPUT_STREAM_NAME);

        for (var property : messageElement.fields()) {
            if (!property.isSynthetic() && isBuffered(property)) {
                writeBufferFinalizer(methodBuilder, property);
            }
        }
    }

    /**
     * Generates the statements that read a field that the message does not declare.
     *
     * <p>The values of the field are added to the unknown fields of the builder, by the name of
     * the field, if the message keeps them. Otherwise, like in the official protobuf library,
     * the field is rejected, as the text format cannot tell a misspelled name apart from a field
     * added by a newer version of the schema.
     *
     * @return the statements, which end with a {@code break} or a {@code throw}
     */
    private CodeBlock writeUnknownFieldDeserializer() {
        if (messageElement.unknownFieldsElement().isEmpty()) {
            return CodeBlock.builder()
                    .addStatement("throw $T.unknownField($S, $L.fieldName())", ProtobufDeserializationException.class, ProtobufTypeElement.getProtobufName(messageElement.typeElement()), INPUT_STREAM_NAME)
                    .build();
        }

        return CodeBlock.builder()
                .addStatement("$L.readUnknownField($L)", INPUT_STREAM_NAME, getFieldTarget(UNKNOWN_FIELDS_NAME))
                .addStatement("break")
                .build();
    }

    /**
     * A case of the switch on the name of the field being read.
     *
     * @param names the names that select the case, the first of which is the name of the field
     * @param body the statements of the case, which end with a {@code break}
     */
    private record TextCase(List<String> names, CodeBlock body) {

    }

    /**
     * Generates the statements that read the value of the given field.
     *
     * <p>A synthetic field is written by the {@code encode} method, so its value is accepted and
     * skipped.
     *
     * @param property the field
     * @param unknownFields the name of the variable that holds the unknown fields of the
     *        message, or {@code null} if the message does not keep them
     * @return the statements, which end with a {@code break}
     */
    private CodeBlock writeFieldDeserializer(ProtobufFieldElement property, String unknownFields) {
        var caseBlock = CodeBlock.builder();
        if (property.isSynthetic()) {
            caseBlock.addStatement("$L.skipUnknownField()", INPUT_STREAM_NAME);
            return caseBlock.addStatement("break").build();
        }

        var binaryOnly = ProtobufTextConverters.findBinaryOnlyDeserializer(property.type());
        if (binaryOnly.isPresent()) {
            return writeBinaryOnlyField(property, binaryOnly.get());
        }

        switch (property.type()) {
            case ProtobufMapFieldType mapType -> writeRepeatedValues(caseBlock, () -> writeMapEntryDeserializer(caseBlock, property, mapType));
            case ProtobufArrayFieldType arrayType when arrayType.bulk() -> writeRepeatedValues(caseBlock, () -> caseBlock.add(appendToArray(getBuffer(property), getTextReadExpression(arrayType))));
            case ProtobufArrayFieldType arrayType -> writeRepeatedValues(caseBlock, () -> writeValueAssignment(caseBlock, property.name(), property.index(), arrayType.valueType(), getTextReadExpression(arrayType.valueType()), ValueTarget.ARRAY, unknownFields));
            case ProtobufCollectionFieldType collectionType when collectionType.bulk() -> writeRepeatedValues(caseBlock, () -> caseBlock.add(appendToArray(getBuffer(property), getTextReadExpression(collectionType))));
            case ProtobufCollectionFieldType collectionType -> writeRepeatedValues(caseBlock, () -> writeValueAssignment(caseBlock, property.name(), property.index(), collectionType.valueType(), getTextReadExpression(collectionType.valueType()), ValueTarget.COLLECTION, unknownFields));
            case ProtobufSimpleFieldType simpleType -> writeValueAssignment(caseBlock, property.name(), property.index(), simpleType, getTextReadExpression(simpleType), ValueTarget.FIELD, unknownFields);
            case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled by writeOneOfCaseDeserializer");
        }
        return caseBlock.addStatement("break").build();
    }

    /**
     * Generates the statement that throws a {@link ProtobufDeserializationException} for the
     * given field, which cannot be read from the text format.
     *
     * @param property the field
     * @param converter the converter that only reads the binary format
     * @return the statement
     */
    private CodeBlock writeBinaryOnlyField(ProtobufFieldElement property, DaedalusMethodElement converter) {
        return CodeBlock.builder()
                .addStatement("throw $T.binaryOnlyField($S, $S, $S)", ProtobufDeserializationException.class, ProtobufTypeElement.getProtobufName(messageElement.typeElement()), property.textName(), converter.ownerName() + "." + converter.name())
                .build();
    }

    /**
     * Generates the statements that read the values of a repeated field, which are either a
     * list enclosed in square brackets or a single value.
     *
     * <p>The statements that read a value are added twice, each time in its own block, so that
     * the variables they declare do not clash.
     *
     * @param caseBlock the block to add the statements to
     * @param valueReader the action that adds the statements that read a single value
     */
    private void writeRepeatedValues(CodeBlock.Builder caseBlock, Runnable valueReader) {
        caseBlock.beginControlFlow("if ($L.readStartList())", INPUT_STREAM_NAME);
        caseBlock.beginControlFlow("while ($L.nextListElement())", INPUT_STREAM_NAME);
        valueReader.run();
        caseBlock.endControlFlow();
        caseBlock.nextControlFlow("else");
        valueReader.run();
        caseBlock.endControlFlow();
    }

    /**
     * Generates the statements that read an entry of the given map field, which is a message
     * whose fields are named {@code key} and {@code value}, and put it in the map.
     *
     * <p>For example, given a {@code Map<String, Integer>} field named "scores":
     *
     * <pre>{@code
     * protoInputStream.readStartMessage();
     * java.lang.String scoresKey = null;
     * java.lang.Integer scoresValue = null;
     * while (protoInputStream.nextField()) {
     *     switch (protoInputStream.fieldName()) {
     *         case "key":
     *             scoresKey = protoInputStream.readStringField();
     *             break;
     *         case "value":
     *             scoresValue = protoInputStream.readInt32Field();
     *             break;
     *         default:
     *             throw ProtobufDeserializationException.unknownField("Person.ScoresEntry", protoInputStream.fieldName());
     *     }
     * }
     * protoInputStream.readEndMessage();
     * protoBuilder.scores = putScores(protoBuilder.scores, scoresKey, scoresValue);
     * }</pre>
     *
     * @param caseBlock the block to add the statements to
     * @param property the map field
     * @param mapType the type of the map field
     */
    private void writeMapEntryDeserializer(CodeBlock.Builder caseBlock, ProtobufFieldElement property, ProtobufMapFieldType mapType) {
        var name = property.name();
        var keyName = "%sKey".formatted(name);
        var valueName = "%sValue".formatted(name);
        var entryName = "%s.%s%sEntry".formatted(ProtobufTypeElement.getProtobufName(messageElement.typeElement()), Character.toUpperCase(name.charAt(0)), name.substring(1));
        caseBlock.addStatement("$L.readStartMessage()", INPUT_STREAM_NAME);
        caseBlock.addStatement("$L $L = null", getQualifiedName(mapType.keyType().accessorType()), keyName);
        caseBlock.addStatement("$L $L = null", getQualifiedName(mapType.valueType().accessorType()), valueName);
        caseBlock.beginControlFlow("while ($L.nextField())", INPUT_STREAM_NAME);
        caseBlock.beginControlFlow("switch ($L.fieldName())", INPUT_STREAM_NAME);
        caseBlock.add("case $S:\n", MAP_KEY_NAME).indent();
        caseBlock.addStatement("$L = $L", keyName, applyDeserializers(getTextReadExpression(mapType.keyType()), mapType.keyType(), INPUT_STREAM_NAME));
        caseBlock.addStatement("break");
        caseBlock.unindent();
        caseBlock.add("case $S:\n", MAP_VALUE_NAME).indent();
        caseBlock.addStatement("$L = $L", valueName, applyDeserializers(getTextReadExpression(mapType.valueType()), mapType.valueType(), INPUT_STREAM_NAME));
        caseBlock.addStatement("break");
        caseBlock.unindent();
        caseBlock.add("default:\n").indent();
        caseBlock.addStatement("throw $T.unknownField($S, $L.fieldName())", ProtobufDeserializationException.class, entryName, INPUT_STREAM_NAME);
        caseBlock.unindent();
        caseBlock.endControlFlow();
        caseBlock.endControlFlow();
        caseBlock.addStatement("$L.readEndMessage()", INPUT_STREAM_NAME);
        caseBlock.add(getMapPut(name, mapType, keyName, valueName));
    }

    /**
     * Generates the statements that read a case of the given oneof into the field of the
     * builder that holds the oneof, like the binary {@code decodePartial} method does.
     *
     * <p>A case of a synthetic oneof is skipped. A message case that the builder holds as a
     * builder is decoded into the builder of the message held by the oneof if it is the same
     * case, and into a new builder otherwise, which replaces the case that was set. Any other
     * case replaces the value of the oneof. The statements are enclosed in a block, so that the
     * variables of different cases of the same oneof do not clash.
     *
     * @param oneOf the oneof
     * @param oneOfCase the case of the oneof
     * @param unknownFields the name of the variable that holds the unknown fields of the
     *        message, or {@code null} if the message does not keep them
     * @return the statements, which end with a {@code break}
     */
    private CodeBlock writeOneOfCaseDeserializer(ProtobufFieldElement.OneOf oneOf, ProtobufFieldElement oneOfCase, String unknownFields) {
        var caseBlock = CodeBlock.builder().add("{\n").indent();
        var builderMessage = oneOfCase.type().builderMessage();
        if (oneOf.isSynthetic()) {
            caseBlock.addStatement("$L.skipUnknownField()", INPUT_STREAM_NAME);
        } else if (ProtobufTextConverters.findBinaryOnlyDeserializer(oneOfCase.type()).isPresent()) {
            caseBlock.add(writeBinaryOnlyField(oneOfCase, ProtobufTextConverters.findBinaryOnlyDeserializer(oneOfCase.type()).get()));
        } else if (builderMessage.isPresent()) {
            var target = getFieldTarget(oneOf.name());
            var caseTarget = getFieldTarget(ProtobufBuilderTypeGenerator.getOneOfCaseField(oneOf.name()));
            var builderType = ProtobufBuilderTypeGenerator.getBuilderClassName(builderMessage.get());
            var decoder = oneOfCase.type().deserializers().getFirst().delegate();
            caseBlock.beginControlFlow("if ($L != $L)", caseTarget, oneOfCase.index());
            caseBlock.addStatement("$L = new $T()", target, builderType);
            caseBlock.addStatement("$L = $L", caseTarget, oneOfCase.index());
            caseBlock.endControlFlow();
            caseBlock.addStatement("$L.$L($L, ($T) $L)", decoder.receiver().orElse(decoder.ownerName()), PARTIAL_METHOD_NAME, INPUT_STREAM_NAME, builderType, target);
        } else {
            currentOneOfCase = oneOfCase;
            var caseType = (ProtobufValueFieldType) oneOfCase.type();
            writeValueAssignment(caseBlock, oneOf.name(), oneOfCase.index(), caseType, getTextReadExpression(caseType), ValueTarget.FIELD, unknownFields);
            currentOneOfCase = null;
        }
        caseBlock.addStatement("break");
        return caseBlock.unindent().add("}\n").build();
    }

    /**
     * Returns the expression that reads a single value of the given type from the text reader.
     *
     * <p>A message is read by the deserializers of its type, which are given the reader itself.
     * An enum value can be written by the name of its constant, which is converted to its index
     * by the {@code indexOf} method of the Spec of the enum, the owner of the first deserializer
     * of the type, so that the deserializers of the enum convert it like an index read from the
     * binary format.
     *
     * @param type the type of the value
     * @return the read expression
     */
    private String getTextReadExpression(ProtobufValueFieldType type) {
        var method = switch (type.protobufType()) {
            case MESSAGE -> null;
            case ENUM -> null;
            case STRING -> "readStringField";
            case BYTES -> "readBytesField";
            case BOOL -> "readBoolField";
            case FLOAT -> "readFloatField";
            case DOUBLE -> "readDoubleField";
            case INT32 -> "readInt32Field";
            case SINT32 -> "readSInt32Field";
            case UINT32 -> "readUInt32Field";
            case FIXED32 -> "readFixed32Field";
            case SFIXED32 -> "readSFixed32Field";
            case INT64 -> "readInt64Field";
            case SINT64 -> "readSInt64Field";
            case UINT64 -> "readUInt64Field";
            case FIXED64 -> "readFixed64Field";
            case SFIXED64 -> "readSFixed64Field";
        };
        if (method != null) {
            return "%s.%s()".formatted(INPUT_STREAM_NAME, method);
        }

        if (type.protobufType() == ProtobufType.MESSAGE) {
            return INPUT_STREAM_NAME;
        }

        var enumConverter = type.deserializers().getFirst().delegate();
        var enumSpec = enumConverter.receiver().orElse(enumConverter.ownerName());
        return "(%s.isNextValueIdentifier() ? %s.%s(%s.readIdentifierField()) : %s.readInt32Field())".formatted(
                INPUT_STREAM_NAME,
                enumSpec,
                ProtobufEnumIndexGenerator.METHOD_NAME,
                INPUT_STREAM_NAME,
                INPUT_STREAM_NAME
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>A deserializer that reads the binary format directly, like the
     * {@code decodeBinary} method of the Spec of a message, is replaced by its text overload,
     * like the {@code decodeText} method.
     */
    @Override
    protected String applyDeserializers(String value, ProtobufFieldType type, String streamName) {
        for (var deserializer : type.deserializers()) {
            var invocation = deserializer.delegate().invoke(value, deserializer.arguments(), _ -> streamName);
            value = ProtobufTextConverters.toTextInvocation(deserializer.delegate(), invocation);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @return the text reader type and the builder type of the message
     */
    @Override
    protected List<TypeName> parametersTypes() {
        var builderType = ProtobufBuilderTypeGenerator.getBuilderClassName((DeclaredType) messageElement.typeElement().asType());
        return List.of(ClassName.get(ProtobufTextReader.class), builderType);
    }
}
