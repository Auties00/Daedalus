package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufTextConverters;
import com.github.auties00.daedalus.protobuf.processor.generator.enumeration.ProtobufEnumNameGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.WildcardTypeName;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialization method generator that produces the {@code encodeText(MessageType, ProtobufTextWriter)}
 * method of protobuf message types, which writes a message in the text format.
 *
 * <p>The generated method writes every field that is set, ordered by index, like the
 * {@code TextFormat} printer of the official protobuf library:
 * <ul>
 * <li>a field that is not a message is written as its name, a colon, and its value;
 * <li>an enum value is written by the name of its constant, or by its index if the enum has no
 *     constant for it;
 * <li>a message is written as its name and its fields, enclosed in braces, which the Spec of
 *     the message writes, and a group as the name of its message type if its name is the name
 *     of the type in lower case;
 * <li>every element of a repeated field is written as a field of its own;
 * <li>every entry of a map is written as a message whose fields are named {@code key} and
 *     {@code value};
 * <li>the unknown fields read from the text format are written after every other field, by
 *     their names. The ones read from the binary format are not written, as they have no name,
 *     so the text format could not read them back.
 * </ul>
 *
 * <p>Like the binary {@code encode} method, the value of each field goes through the
 * serializers of its type, the default value of a field with implicit presence is not
 * written, and a required field that is not set throws a {@link ProtobufSerializationException}.
 * The method writes the fields of the message without enclosing them in braces, so that it can
 * write both a top level message and a nested one.
 */
public class ProtobufMessageTextSerializationGenerator extends ProtobufSerializationGenerator {
    /**
     * The name of the generated method, which encodes a message in the text format.
     */
    public static final String TEXT_METHOD_NAME = "encodeText";

    /**
     * The name of the parameter that holds the message.
     */
    protected static final String INPUT_OBJECT_PARAMETER = "protoInputObject";

    /**
     * The name of the local variable that holds the unknown fields of the message.
     */
    private static final String UNKNOWN_FIELDS_VALUE = "protoUnknownFieldsValue";

    /**
     * The name of the local variable that holds the store of the unknown fields of the message.
     */
    private static final String UNKNOWN_FIELDS_STORE = "protoUnknownFields";

    /**
     * The name of the key of a map entry in the text format.
     */
    protected static final String MAP_KEY_NAME = "key";

    /**
     * The name of the value of a map entry in the text format.
     */
    protected static final String MAP_VALUE_NAME = "value";

    /**
     * The protobuf message element whose serialization method is being generated.
     */
    protected final ProtobufMessageElement messageElement;

    /**
     * The names written for the indexes of the fields whose serializers are being generated:
     * the names of the fields of the message, or the names of the key and of the value of a map
     * entry.
     */
    protected Map<Integer, String> fieldNames;

    /**
     * Constructs a new text serialization generator for the given message element.
     *
     * @param element the protobuf message element to generate the method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageTextSerializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.messageElement = element;
        this.fieldNames = new HashMap<>();
        for (var field : element.fields()) {
            if (field instanceof ProtobufFieldElement.OneOf oneOf) {
                for (var oneOfCase : oneOf.cases()) {
                    fieldNames.put(oneOfCase.index(), ProtobufTextNames.getWrittenName(oneOfCase));
                }
            } else {
                fieldNames.put(field.index(), ProtobufTextNames.getWrittenName(field));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns immediately for {@code null} input, validates required
     * fields, writes each field, and writes the unknown fields.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return");
        methodBuilder.endControlFlow();

        createRequiredPropertiesNullCheck(methodBuilder);
        writeFields(methodBuilder);
        writeUnknownFields(methodBuilder);
    }

    /**
     * Generates the statements that write every field of the message that is set, ordered by
     * index, except for its unknown fields.
     *
     * @param methodBuilder the method builder to append the statements to
     */
    protected void writeFields(MethodSpec.Builder methodBuilder) {
        for (var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                writeOneOfSerializer(methodBuilder, oneOf);
                continue;
            }

            var binaryOnly = ProtobufTextConverters.findBinaryOnlySerializer(property.type());
            if (binaryOnly.isPresent()) {
                writeBinaryOnlyField(methodBuilder, getAccessorCall(property.accessor()), property.type().accessorType(), property.textName(), binaryOnly.get());
                continue;
            }

            switch (property.type()) {
                case ProtobufArrayFieldType arrayType -> writeArraySerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), arrayType, false, property.delimited());
                case ProtobufCollectionFieldType collectionType -> writeRepeatedSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), collectionType, false, property.delimited(), false);
                case ProtobufMapFieldType mapType -> writeMapSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), mapType);
                case ProtobufSimpleFieldType simpleType -> writeNormalSerializer(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), simpleType, true, true, false, property.implicit(), property.delimited(), null);
                case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled before the switch");
            }
        }
    }

    /**
     * Generates the statements that write the unknown fields of the message read from the text
     * format, if the message stores its unknown fields.
     *
     * <p>Only the unknown fields read from the text format are written, as the other ones have
     * no name.
     *
     * @param methodBuilder the method builder to append the statements to
     */
    protected void writeUnknownFields(MethodSpec.Builder methodBuilder) {
        messageElement.unknownFieldsElement().ifPresent(unknownFieldsElement -> {
            methodBuilder.addStatement("var $L = $L", UNKNOWN_FIELDS_VALUE, getAccessorCall(unknownFieldsElement.accessor()));
            methodBuilder.beginControlFlow("if ($L != null && $L instanceof $T $L)", UNKNOWN_FIELDS_VALUE, unknownFieldsElement.toStore(UNKNOWN_FIELDS_VALUE), ProtobufUnknownFields.class, UNKNOWN_FIELDS_STORE);
            methodBuilder.addStatement("$L", getUnknownFieldsWrite(UNKNOWN_FIELDS_STORE));
            methodBuilder.endControlFlow();
        });
    }

    /**
     * Returns the statement that writes the unknown fields held by the given store.
     *
     * @param store the name of the variable that holds the {@link ProtobufUnknownFields}
     * @return the statement
     */
    protected String getUnknownFieldsWrite(String store) {
        return "%s.encodeText(%s)".formatted(store, OUTPUT_OBJECT_PARAMETER);
    }

    /**
     * Generates the statements that write the case of the given oneof that is set, if any,
     * like the binary {@code encode} method does.
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
            var binaryOnly = ProtobufTextConverters.findBinaryOnlySerializer(oneOfCase.type());
            if (binaryOnly.isPresent()) {
                writeBinaryOnlyValue(methodBuilder, oneOfCase.textName(), binaryOnly.get());
            } else {
                writeNormalSerializer(methodBuilder, oneOfCase.index(), caseValue, caseValue, (ProtobufValueFieldType) oneOfCase.type(), false, false, false, false, oneOfCase.delimited(), null);
            }
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Generates the statements that throw a {@link ProtobufSerializationException} if the given
     * field, which cannot be written in the text format, is set.
     *
     * <p>A field whose accessor returns a primitive is always set.
     *
     * @param methodBuilder the method builder to append the statements to
     * @param accessor the expression that accesses the value of the field
     * @param accessorType the type returned by the accessor
     * @param fieldName the Protocol Buffers name of the field
     * @param converter the converter that only writes the binary format
     */
    protected void writeBinaryOnlyField(MethodSpec.Builder methodBuilder, String accessor, TypeMirror accessorType, String fieldName, DaedalusMethodElement converter) {
        var primitive = accessorType.getKind().isPrimitive();
        if (!primitive) {
            methodBuilder.beginControlFlow("if ($L != null)", accessor);
        }
        writeBinaryOnlyValue(methodBuilder, fieldName, converter);
        if (!primitive) {
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Generates the statement that throws a {@link ProtobufSerializationException} for a value
     * of the given field, which cannot be written in the text format, as it is converted by a
     * converter that only writes the binary format.
     *
     * @param methodBuilder the method builder to append the statement to
     * @param fieldName the Protocol Buffers name of the field
     * @param converter the converter that only writes the binary format
     */
    protected void writeBinaryOnlyValue(MethodSpec.Builder methodBuilder, String fieldName, DaedalusMethodElement converter) {
        methodBuilder.addStatement("throw $T.binaryOnlyField($S, $S)", ProtobufSerializationException.class, fieldName, getConverterName(converter));
    }

    /**
     * Returns the name of the given converter, used in the error messages.
     *
     * @param converter the converter
     * @return the qualified name of the owner of the converter, followed by its name
     */
    private static String getConverterName(DaedalusMethodElement converter) {
        return converter.ownerName() + "." + converter.name();
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
     * <p>The text format has no packed encoding: the array of the wire values is written element
     * by element, each as a field of its own.
     */
    @Override
    protected void writeBulkSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufValueFieldType type) {
        var array = writeBulkSource(methodBuilder, name, accessor, type);
        var element = name + "Element";
        methodBuilder.beginControlFlow("for (var $L : $L)", element, array);
        methodBuilder.addStatement("$L", getValueWrite(index, type, element));
        methodBuilder.endControlFlow();
        for (var i = 0; i <= type.serializers().size(); i++) {
            methodBuilder.endControlFlow();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every entry of the map is written as a message whose fields are named {@code key} and
     * {@code value}. For example, given a {@code Map<String, Integer>} field named "scores":
     *
     * <pre>{@code
     * if (scores != null) {
     *     for (var scoresEntry : scores.entrySet()) {
     *         protoOutputStream.writeStartMessageField("scores");
     *         protoOutputStream.writeStringField("key", scoresEntry.getKey());
     *         protoOutputStream.writeInt32Field("value", scoresEntry.getValue());
     *         protoOutputStream.writeEndMessageField();
     *     }
     * }
     * }</pre>
     */
    @Override
    protected void writeMapSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufMapFieldType mapType) {
        var messageFieldNames = fieldNames;
        fieldNames = Map.of(1, MAP_KEY_NAME, 2, MAP_VALUE_NAME);
        methodBuilder.beginControlFlow("if ($L != null)", accessor);
        var localVariableName = "%sEntry".formatted(name);
        methodBuilder.beginControlFlow("for (var $L : $L.entrySet())", localVariableName, accessor);
        methodBuilder.addStatement("$L.writeStartMessageField($S)", OUTPUT_OBJECT_PARAMETER, messageFieldNames.get(index));
        writeNormalSerializer(
                methodBuilder,
                1,
                name + "Key",
                "%s.getKey()".formatted(localVariableName),
                mapType.keyType(),
                true,
                true,
                false,
                false,
                false,
                CodeBlock.of("throw $T.nullMapKey($S)", ProtobufSerializationException.class, name)
        );
        writeNormalSerializer(
                methodBuilder,
                2,
                name + "Value",
                "%s.getValue()".formatted(localVariableName),
                mapType.valueType(),
                true,
                true,
                false,
                false,
                false,
                CodeBlock.of("throw $T.nullMapValue($S)", ProtobufSerializationException.class, name)
        );
        methodBuilder.addStatement("$L.writeEndMessageField()", OUTPUT_OBJECT_PARAMETER);
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        fieldNames = messageFieldNames;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A message, or a group, is written as its name and its fields, enclosed in braces,
     * which the {@code encodeText} method of the Spec of the message writes.
     */
    @Override
    protected List<String> getMessageWrite(int index, ProtobufValueFieldType type, DaedalusConverterElement.Attributed.Serializer serializer, String propertyName, String invocation, boolean delimited) {
        if (type.protobufType() != ProtobufType.MESSAGE) {
            return List.of(invocation);
        }

        return List.of(
                "%s.writeStartMessageField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, getFieldName(index)),
                invocation,
                "%s.writeEndMessageField()".formatted(OUTPUT_OBJECT_PARAMETER)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value is written by the text writer method that matches the protobuf type of the
     * field, passing it the name of the field. An enum value is written with the name of its
     * constant, which is resolved by the {@code nameOf} method of the Spec of the enum, the
     * owner of the last serializer of the type.
     */
    @Override
    protected String getValueWrite(int index, ProtobufValueFieldType type, String value) {
        var fieldName = getFieldName(index);
        if (type.protobufType() == ProtobufType.ENUM) {
            return "%s.writeEnumField(%s, %s, %s)".formatted(OUTPUT_OBJECT_PARAMETER, fieldName, value, getEnumConstantName(type, value));
        }

        var method = switch (type.protobufType()) {
            case FLOAT -> "writeFloatField";
            case DOUBLE -> "writeDoubleField";
            case BOOL -> "writeBoolField";
            case STRING -> "writeStringField";
            case BYTES -> "writeBytesField";
            case INT32 -> "writeInt32Field";
            case SINT32 -> "writeSInt32Field";
            case UINT32 -> "writeUInt32Field";
            case FIXED32 -> "writeFixed32Field";
            case SFIXED32 -> "writeSFixed32Field";
            case INT64 -> "writeInt64Field";
            case SINT64 -> "writeSInt64Field";
            case UINT64 -> "writeUInt64Field";
            case FIXED64 -> "writeFixed64Field";
            case SFIXED64 -> "writeSFixed64Field";
            case ENUM, MESSAGE -> throw new IllegalArgumentException("Internal bug: %s fields have no text write method".formatted(type.protobufType()));
        };
        return "%s.%s(%s, %s)".formatted(OUTPUT_OBJECT_PARAMETER, method, fieldName, value);
    }

    /**
     * Returns the expression that resolves the name of the constant of the given enum value,
     * which is the {@code nameOf} method of the Spec of the enum, the owner of the last
     * serializer of the type.
     *
     * @param type the type of the enum field
     * @param value the expression of the index of the value
     * @return the expression, whose value is {@code null} if the enum has no constant for the
     *         index
     */
    protected static String getEnumConstantName(ProtobufValueFieldType type, String value) {
        var enumConverter = type.serializers().getLast().delegate();
        var enumSpec = enumConverter.receiver().orElse(enumConverter.ownerName());
        return "%s.%s(%s)".formatted(enumSpec, ProtobufEnumNameGenerator.METHOD_NAME, value);
    }

    /**
     * Returns the name written for the field with the given index.
     *
     * @param index the index of the field
     * @return the name
     * @throws IllegalStateException if no field has the given index
     */
    protected String getWrittenName(int index) {
        var name = fieldNames.get(index);
        if (name == null) {
            throw new IllegalStateException("Internal bug: no text name for the field with index " + index);
        }
        return name;
    }

    /**
     * Returns the string literal of the name written for the field with the given index.
     *
     * @param index the index of the field
     * @return the string literal of the name
     */
    protected String getFieldName(int index) {
        return CodeBlock.of("$S", getWrittenName(index)).toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A converter that writes the binary format directly, like the
     * {@code encodeBinary} method of the Spec of a message, is replaced by its text overload,
     * like the {@code encodeText} method.
     */
    @Override
    protected String createSerializerInvocation(DaedalusConverterElement.Attributed.Serializer serializer, String value) {
        return ProtobufTextConverters.toTextInvocation(serializer.delegate(), super.createSerializerInvocation(serializer, value));
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
     * @return {@code true} always, since every message can be written in the text format
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#encodeText(Object, ProtobufTextWriter)}
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
     * @return {@link TypeName#VOID}, since the method writes directly to the writer
     */
    @Override
    protected TypeName returnType() {
        return TypeName.VOID;
    }

    /**
     * {@inheritDoc}
     *
     * @return the message type and the text writer
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()), ParameterizedTypeName.get(ClassName.get(ProtobufTextWriter.class), WildcardTypeName.subtypeOf(Object.class)));
    }

    /**
     * {@inheritDoc}
     *
     * @return the parameter names matching the types returned by {@link #parametersTypes()}
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER, OUTPUT_OBJECT_PARAMETER);
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
