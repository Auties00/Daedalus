package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeAccumulator;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeCalculator;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.type.TypeMirror;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A size method generator that produces the
 * {@code sizeOfText(MessageType, ProtobufTextSizeAccumulator, int)} method of protobuf message
 * types, which computes the size of the text written by the {@code encodeText} method without
 * writing it.
 *
 * <p>The generated method visits the fields exactly like {@code encodeText} does, but adds the
 * size of every line it would write to the accumulator instead: the size of the name of a
 * field is a constant computed by the processor, and the size of a value is computed by
 * {@link ProtobufTextSizeCalculator}, or by the accumulator for the values whose size depends
 * on the settings of the writer. For example, given an {@code int32} field named "id" and a
 * {@code string} field named "name":
 *
 * <pre>{@code
 * var id = protoInputObject.id();
 * if (id != null) {
 *     protoSizeAccumulator.addLine(protoDepth, 4 + ProtobufTextSizeCalculator.getSignedSize(id));
 * }
 * var name = protoInputObject.name();
 * if (name != null) {
 *     protoSizeAccumulator.addLine(protoDepth, 10 + protoSizeAccumulator.getStringSize(name));
 * }
 * }</pre>
 *
 * <p>A message whose Spec is generated is measured by the {@code sizeOfText} method of its
 * Spec, one level deeper, after the lines that open and close it. A
 * converter that writes a value with a text writer is measured by writing the value to a
 * writer that only counts the bytes it would write, returned by
 * {@link ProtobufTextSizeAccumulator#newWriter(int)}.
 *
 * <p>The required fields are not checked, and the fields whose converters only write the
 * binary format are not measured, as {@code encodeText} throws for them, so the size is never
 * used.
 */
public final class ProtobufMessageTextSizeGenerator extends ProtobufMessageTextSerializationGenerator {
    /**
     * The name of the generated method, which computes the size of the text format of a
     * message.
     */
    public static final String TEXT_SIZE_METHOD_NAME = "sizeOfText";

    /**
     * The name of the parameter that holds the accumulator of the size of the text.
     */
    private static final String ACCUMULATOR_PARAMETER = "protoSizeAccumulator";

    /**
     * The name of the parameter that holds the number of messages that enclose the fields of
     * the message.
     */
    private static final String DEPTH_PARAMETER = "protoDepth";

    /**
     * The number of levels between the fields of the message and the field whose size is being
     * generated, which is one for the key and the value of a map entry.
     */
    private int depthOffset;

    /**
     * Whether the generated method measures a value with a counting text writer, which is
     * declared at the start of the method.
     */
    private boolean measuresWithWriter;

    /**
     * Constructs a new text size generator for the given message element.
     *
     * @param element the protobuf message element to generate the method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageTextSizeGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates code that returns immediately for {@code null} input, and adds the size of
     * each field and of the unknown fields. The fields are generated first, so that the
     * counting writer, which is assigned before every value it measures, is declared only if a
     * value is measured with it.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        var bodyBuilder = MethodSpec.methodBuilder(name());
        writeFields(bodyBuilder);
        writeUnknownFields(bodyBuilder);

        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return");
        methodBuilder.endControlFlow();
        if (measuresWithWriter) {
            methodBuilder.addStatement("$T $L", ParameterizedTypeName.get(ProtobufTextWriter.class, Long.class), OUTPUT_OBJECT_PARAMETER);
        }
        methodBuilder.addCode(bodyBuilder.build().code());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nothing is generated, as the size of a message that cannot be written is never used.
     */
    @Override
    protected void writeBinaryOnlyField(MethodSpec.Builder methodBuilder, String accessor, TypeMirror accessorType, String fieldName, DaedalusMethodElement converter) {

    }

    /**
     * {@inheritDoc}
     *
     * <p>Nothing is generated, as the size of a message that cannot be written is never used.
     */
    @Override
    protected void writeBinaryOnlyValue(MethodSpec.Builder methodBuilder, String fieldName, DaedalusMethodElement converter) {

    }

    /**
     * {@inheritDoc}
     *
     * @return the statement that adds the size of the unknown fields, written at the depth of
     *         the fields of the message, to the accumulator
     */
    @Override
    protected String getUnknownFieldsWrite(String store) {
        return "%s.addTextSize(%s, %s)".formatted(store, ACCUMULATOR_PARAMETER, DEPTH_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every entry of the map is measured as the lines that open and close it, and its key
     * and its value one level deeper.
     */
    @Override
    protected void writeMapSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufMapFieldType mapType) {
        var entryNameSize = getNameSize(index);
        var messageFieldNames = fieldNames;
        fieldNames = Map.of(1, MAP_KEY_NAME, 2, MAP_VALUE_NAME);
        methodBuilder.beginControlFlow("if ($L != null)", accessor);
        var localVariableName = "%sEntry".formatted(name);
        methodBuilder.beginControlFlow("for (var $L : $L.entrySet())", localVariableName, accessor);
        methodBuilder.addStatement("$L.addMessageLines($L, $L)", ACCUMULATOR_PARAMETER, getDepth(0), entryNameSize + 2);
        depthOffset++;
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
        depthOffset--;
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        fieldNames = messageFieldNames;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A message whose Spec is generated is measured as the lines that open and close it,
     * and the {@code sizeOfText} method of its Spec one level deeper. Any
     * other converter writes the value to a counting writer, whose count is added to the
     * accumulator.
     */
    @Override
    protected List<String> getMessageWrite(int index, ProtobufValueFieldType type, DaedalusConverterElement.Attributed.Serializer serializer, String propertyName, String invocation, boolean delimited) {
        var message = type.protobufType() == ProtobufType.MESSAGE;
        if (message) {
            var specReceiver = getSpecReceiver(serializer);
            if (specReceiver.isPresent()) {
                return List.of(
                        "%s.addMessageLines(%s, %d)".formatted(ACCUMULATOR_PARAMETER, getDepth(0), getNameSize(index) + 2),
                        "%s.%s(%s, %s, %s)".formatted(specReceiver.get(), TEXT_SIZE_METHOD_NAME, propertyName, ACCUMULATOR_PARAMETER, getDepth(1))
                );
            }
        }

        measuresWithWriter = true;
        var statements = new ArrayList<String>();
        statements.add("%s = %s.newWriter(%s)".formatted(OUTPUT_OBJECT_PARAMETER, ACCUMULATOR_PARAMETER, getDepth(0)));
        if (message) {
            statements.add("%s.writeStartMessageField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, getFieldName(index)));
        }
        statements.add(invocation);
        if (message) {
            statements.add("%s.writeEndMessageField()".formatted(OUTPUT_OBJECT_PARAMETER));
        }
        statements.add("%s.addWritten(%s)".formatted(ACCUMULATOR_PARAMETER, OUTPUT_OBJECT_PARAMETER));
        return statements;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The line of the value holds the name of the field, a colon and a space, and the value,
     * whose size is computed like the text writer method that matches the protobuf type of the
     * field writes it. The sizes that are known to the processor, which are the ones of the
     * name, of the separator and of the quotes of a {@code string} or {@code bytes} value, are
     * added up into a single constant.
     */
    @Override
    protected String getValueWrite(int index, ProtobufValueFieldType type, String value) {
        var calculator = ProtobufTextSizeCalculator.class.getName();
        // The name is followed by a colon and a space, and a quoted value by its two quotes
        var quoted = type.protobufType() == ProtobufType.STRING || type.protobufType() == ProtobufType.BYTES;
        var constantSize = getNameSize(index) + (quoted ? 4 : 2);
        var valueSize = switch (type.protobufType()) {
            case FLOAT -> "%s.getFloatSize(%s)".formatted(ACCUMULATOR_PARAMETER, value);
            case DOUBLE -> "%s.getDoubleSize(%s)".formatted(ACCUMULATOR_PARAMETER, value);
            case BOOL -> "%s.getBoolSize(%s)".formatted(calculator, value);
            case STRING -> "%s.getStringSize(%s)".formatted(ACCUMULATOR_PARAMETER, value);
            case BYTES -> "%s.getEscapedBytesSize(%s)".formatted(calculator, value);
            case ENUM -> "%s.getEnumSize(%s, %s)".formatted(calculator, value, getEnumConstantName(type, value));
            case INT32, SINT32, SFIXED32, INT64, SINT64, SFIXED64 -> "%s.getSignedSize(%s)".formatted(calculator, value);
            case UINT32, FIXED32 -> "%s.getUnsignedSize(Integer.toUnsignedLong(%s))".formatted(calculator, value);
            case UINT64, FIXED64 -> "%s.getUnsignedSize(%s)".formatted(calculator, value);
            case MESSAGE -> throw new IllegalArgumentException("Internal bug: message fields have no text value");
        };
        return "%s.addLine(%s, %d + %s)".formatted(ACCUMULATOR_PARAMETER, getDepth(0), constantSize, valueSize);
    }

    /**
     * Returns the size of the UTF-8 encoding of the name written for the field with the given
     * index, which is computed by the processor.
     *
     * @param index the index of the field
     * @return the size of the name
     */
    private int getNameSize(int index) {
        return getWrittenName(index).getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Returns the expression of the depth of a line that is the given number of levels deeper
     * than the field whose size is being generated.
     *
     * @param levels the number of additional levels
     * @return the expression of the depth
     */
    private String getDepth(int levels) {
        var offset = depthOffset + levels;
        return offset == 0 ? DEPTH_PARAMETER : "%s + %d".formatted(DEPTH_PARAMETER, offset);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #TEXT_SIZE_METHOD_NAME}
     */
    @Override
    protected String name() {
        return TEXT_SIZE_METHOD_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the message type, the accumulator and the depth
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()), ClassName.get(ProtobufTextSizeAccumulator.class), TypeName.INT);
    }

    /**
     * {@inheritDoc}
     *
     * @return the parameter names matching the types returned by {@link #parametersTypes()}
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER, ACCUMULATOR_PARAMETER, DEPTH_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * @return the checked exceptions of the size methods of the message
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return exceptions.sizeExceptions(ownerElement);
    }
}
