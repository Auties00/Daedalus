package com.github.auties00.daedalus.protobuf.processor.generator;

import com.github.auties00.daedalus.protobuf.io.size.ProtobufSizeTable;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

/**
 * An abstract base class for serialization method generators that provides shared logic
 * for encoding protobuf values to a binary output stream.
 *
 * <p>This class extends {@link DaedalusMethodGenerator} and offers reusable methods for
 * serializing normal, repeated, and map fields, including support for custom
 * {@code @ProtobufSerializer} converter chains. Concrete subclasses in the message and
 * enumeration subpackages implement type-specific encoding strategies.
 *
 * <p>Each generated method writes field tags and values to a {@code ProtobufOutputStream}
 * using the appropriate wire format for each field's protobuf type.
 */
public abstract class ProtobufSerializationGenerator extends DaedalusMethodGenerator {
    /**
     * The name of the generated serialization method.
     */
    public static final String METHOD_NAME = "encodeBinary";

    /**
     * The name of the parameter that holds the writer.
     */
    protected static final String OUTPUT_OBJECT_PARAMETER = "protoOutputStream";

    /**
     * The name of the parameter that holds the {@link ProtobufSizeTable}
     * of a generated method that {@linkplain #usesSizeTable() uses one}.
     */
    protected static final String SIZE_TABLE_PARAMETER = "protoSizeTable";

    /**
     * The protobuf type element that owns the fields being serialized.
     */
    protected final ProtobufTypeElement ownerElement;

    /**
     * The manager that resolves the checked exceptions of the generated method.
     */
    protected final ProtobufExceptionManager exceptions;

    /**
     * Constructs a new serialization generator for the given protobuf type element.
     *
     * @param ownerElement the protobuf type element whose fields this generator serializes
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufSerializationGenerator(ProtobufTypeElement ownerElement, ProtobufExceptionManager exceptions) {
        this.ownerElement = ownerElement;
        this.exceptions = exceptions;
    }

    /**
     * Returns whether the generated method takes the {@link ProtobufSizeTable}
     * of the message it encodes or measures, as the parameter named {@value #SIZE_TABLE_PARAMETER}.
     *
     * <p>A method that uses a size table reserves a slot for the length of every nested message
     * written by a Spec, of every map entry and of every packed field while it measures them,
     * and reads the slots back in the same order while it writes them, instead of measuring
     * them again.
     *
     * @implSpec
     * The default implementation returns {@code false}.
     *
     * @return {@code true} if the generated method uses a size table
     */
    protected boolean usesSizeTable() {
        return false;
    }

    /**
     * Returns the expression of the instance of the Spec of a message that the given
     * serializer writes, if the serializer is the serialization method of that Spec.
     *
     * <p>The parts of a message written by its Spec are measured and written with the size
     * table, while a message written by any other serializer, like a mixin that writes it with
     * a binary writer, is measured by the sizer attached to the serializer.
     *
     * @param serializer the last serializer of a field whose type is a message
     * @return the expression of the instance of the Spec, or an empty {@link Optional} if the
     *         serializer is not the serialization method of a Spec
     */
    protected static Optional<String> getSpecReceiver(DaedalusConverterElement.Attributed.Serializer serializer) {
        var delegate = serializer.delegate();
        if (delegate.parameters().isEmpty()) {
            return Optional.empty();
        }

        var spec = DaedalusMethodGenerator.getSpecByType(delegate.parameters().getFirst());
        if (!spec.equals(delegate.ownerName())) {
            return Optional.empty();
        }

        return delegate.receiver();
    }

    /**
     * Generates code that serializes a repeated (collection) field to the protobuf output stream.
     *
     * <p>For packed repeated fields, the generated code streams the elements without
     * allocating: a first pass converts every element to its wire value and adds up the
     * length of the packed payload, then the tag and the length are written, and a second
     * pass converts and writes every element. For non-packed repeated fields, the generated
     * code iterates over the collection and writes each element individually.
     *
     * <p>Like in the official protobuf library, a {@code null} element, or an element
     * converted to {@code null}, is not a valid value: the generated code throws a
     * {@code ProtobufSerializationException}.
     *
     * <p>For example, given a packed {@code List<Integer>} field named "numbers" at index 2:
     *
     * <pre>{@code
     * var numbersPackedSource = protoInputObject.numbers();
     * if (numbersPackedSource != null && !numbersPackedSource.isEmpty()) {
     *     var numbersPackedLength = 0;
     *     for (var numbersEntry : numbersPackedSource) {
     *         if (numbersEntry == null) {
     *             throw ProtobufSerializationException.nullRepeatedElement("numbers");
     *         }
     *         numbersPackedLength += ProtobufBinarySizeCalculator.getVarInt32Size(numbersEntry);
     *     }
     *     if (numbersPackedLength != 0) {
     *         protoOutputStream.writeFieldTag(2, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
     *         protoOutputStream.writeLengthDelimitedFieldLength(numbersPackedLength);
     *         for (var numbersEntry : numbersPackedSource) {
     *             ...
     *             protoOutputStream.writeRawVarInt32(numbersEntry);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param methodBuilder  the method builder to append the serialization code to
     * @param index          the protobuf field index
     * @param name           the name of the field being serialized
     * @param accessor       the expression that accesses the field value
     * @param collectionType the collection field type descriptor
     * @param packed         {@code true} if the repeated field uses packed encoding
     * @param delimited      {@code true} if the elements are messages encoded as groups
     * @param cast           {@code true} if a type cast is needed for generic element types
     */
    protected void writeRepeatedSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufCollectionFieldType collectionType, boolean packed, boolean delimited, boolean cast) {
        if (collectionType.bulk()) {
            writeBulkSerializer(methodBuilder, index, name, accessor, collectionType);
            return;
        }

        writeRepeatedElementsSerializer(methodBuilder, index, name, accessor, collectionType.valueType(), false, packed, delimited, cast);
    }

    /**
     * Generates code that serializes a repeated field held by an array.
     *
     * <p>A packed array that is converted to the array of the primitive type of its wire
     * values, which needs no converter if it already is one, is written in bulk. Any other
     * array is written like a collection, converting each element to its wire value.
     *
     * @param methodBuilder the method builder to append the serialization code to
     * @param index         the protobuf field index
     * @param name          the name of the field being serialized
     * @param accessor      the expression that accesses the array
     * @param arrayType     the array field type descriptor
     * @param packed        {@code true} if the repeated field uses packed encoding
     * @param delimited     {@code true} if the elements are messages encoded as groups
     */
    protected void writeArraySerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufArrayFieldType arrayType, boolean packed, boolean delimited) {
        if (arrayType.bulk()) {
            writeBulkSerializer(methodBuilder, index, name, accessor, arrayType);
            return;
        }

        writeRepeatedElementsSerializer(methodBuilder, index, name, accessor, arrayType.valueType(), true, packed, delimited, false);
    }

    /**
     * Generates code that serializes a packed field in bulk: its value is converted by the
     * converters of the field to the array of the primitive type of its wire values, which
     * is passed as a whole to the packed variant of the write method.
     *
     * <p>For example, given a packed {@code int[]} field named "numbers" at index 2, which
     * needs no converter:
     *
     * <pre>{@code
     * var numbersBulk = protoInputObject.numbers();
     * if (numbersBulk != null && numbersBulk.length != 0) {
     *     protoOutputStream.writePackedInt32Field(2, numbersBulk);
     * }
     * }</pre>
     *
     * @param methodBuilder the method builder to append the serialization code to
     * @param index         the protobuf field index
     * @param name          the name of the field being serialized
     * @param accessor      the expression that accesses the value of the field
     * @param type          the type of the field, whose converters produce the array
     */
    protected void writeBulkSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufValueFieldType type) {
        var array = writeBulkSource(methodBuilder, name, accessor, type);
        methodBuilder.addStatement("$L.$L($L, $L)", OUTPUT_OBJECT_PARAMETER, getStreamMethodName(type.protobufType(), true).orElseThrow(), index, array);
        for (var i = 0; i <= type.serializers().size(); i++) {
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Generates code that converts the value of a packed field to the array of the primitive
     * type of its wire values, and opens the blocks that are only entered if the value and
     * every intermediate result are not {@code null}, and the array is not empty.
     *
     * <p>The caller must close one block for each serializer of the field, plus one.
     *
     * @param methodBuilder the method builder to append the code to
     * @param name          the name of the field
     * @param accessor      the expression that accesses the value of the field
     * @param type          the type of the field, whose serializers produce the array
     * @return the name of the local variable that holds the array
     */
    protected String writeBulkSource(MethodSpec.Builder methodBuilder, String name, String accessor, ProtobufValueFieldType type) {
        var value = name + "Bulk";
        methodBuilder.addStatement("var $L = $L", value, accessor);
        var serializers = type.serializers();
        for (var i = 0; i < serializers.size(); i++) {
            methodBuilder.beginControlFlow("if ($L != null)", value);
            var next = name + "Bulk" + i;
            methodBuilder.addStatement("var $L = $L", next, createSerializerInvocation(serializers.get(i), value));
            value = next;
        }
        methodBuilder.beginControlFlow("if ($L != null && $L.length != 0)", value, value);
        return value;
    }

    /**
     * Generates code that serializes the elements of a repeated field held by a collection or
     * by an array that is not written in bulk, converting each element to its wire value.
     *
     * @param methodBuilder the method builder to append the serialization code to
     * @param index         the protobuf field index
     * @param name          the name of the field being serialized
     * @param accessor      the expression that accesses the collection or the array
     * @param valueType     the type of the elements
     * @param array         {@code true} if the elements are held by an array
     * @param packed        {@code true} if the repeated field uses packed encoding
     * @param delimited     {@code true} if the elements are messages encoded as groups
     * @param cast          {@code true} if a type cast is needed for generic element types
     */
    private void writeRepeatedElementsSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufSimpleFieldType valueType, boolean array, boolean packed, boolean delimited, boolean cast) {
        var nullElement = CodeBlock.of("throw $T.nullRepeatedElement($S)", ProtobufSerializationException.class, name);
        var source = writeRepeatedSource(methodBuilder, name, accessor, array);
        if(packed) {
            String length;
            if (usesSizeTable()) {
                length = name + "PackedLength";
                methodBuilder.addStatement("var $L = $L.next()", length, SIZE_TABLE_PARAMETER);
            } else {
                length = writePackedLength(methodBuilder, name, source, valueType, cast, nullElement);
            }
            methodBuilder.beginControlFlow("if ($L != 0)", length);
            methodBuilder.addStatement("$L.writeFieldTag($L, $T.WIRE_TYPE_LENGTH_DELIMITED)", OUTPUT_OBJECT_PARAMETER, index, ProtobufWireType.class);
            methodBuilder.addStatement("$L.writeLengthDelimitedFieldLength($L)", OUTPUT_OBJECT_PARAMETER, length);
            writePackedElements(methodBuilder, name, source, valueType, cast, nullElement, (builder, value, _) -> builder.addStatement("$L", getPackedElementWrite(valueType.protobufType(), value)));
            if (usesSizeTable()) {
                // Every element that is not null takes at least one byte, so a field that has elements and no length only has null elements
                methodBuilder.nextControlFlow("else");
                methodBuilder.addStatement(nullElement);
            }
            methodBuilder.endControlFlow();
        }else {
            var localVariableName = "%sEntry".formatted(name);
            methodBuilder.beginControlFlow("for (var $L : $L)", localVariableName, source);
            writeNormalSerializer(methodBuilder, index, name, localVariableName, valueType, false, true, cast, false, delimited, nullElement);
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Generates code that stores the collection or the array of a repeated field in a local
     * variable and opens a block that is only entered if it has elements.
     *
     * <p>The caller must close the block.
     *
     * @param methodBuilder the method builder to append the code to
     * @param name          the name of the field
     * @param accessor      the expression that accesses the collection or the array
     * @param array         {@code true} if the elements are held by an array
     * @return the name of the local variable that holds the collection or the array
     */
    protected String writeRepeatedSource(MethodSpec.Builder methodBuilder, String name, String accessor, boolean array) {
        var source = name + "Source";
        methodBuilder.addStatement("var $L = $L", source, accessor);
        if (array) {
            methodBuilder.beginControlFlow("if ($L != null && $L.length != 0)", source, source);
        } else {
            methodBuilder.beginControlFlow("if ($L != null && !$L.isEmpty())", source, source);
        }
        return source;
    }

    /**
     * Generates code that computes the length of the payload of a packed field, which is the
     * sum of the sizes of the wire values of its elements.
     *
     * <p>Each element goes through the serializers of the element type, like the element of
     * a non-packed field. A payload length of {@code 0} means that no element is written.
     *
     * @param methodBuilder the method builder to append the code to
     * @param name          the name of the field
     * @param source        the name of the local variable that holds the elements
     * @param valueType     the type of the elements
     * @param cast          {@code true} if a type cast is needed for generic element types
     * @param nullElement   the statement executed for a {@code null} element, or {@code null}
     *                      to skip it
     * @return the name of the local variable that holds the length of the payload
     */
    protected String writePackedLength(MethodSpec.Builder methodBuilder, String name, String source, ProtobufSimpleFieldType valueType, boolean cast, CodeBlock nullElement) {
        var length = name + "PackedLength";
        methodBuilder.addStatement("var $L = 0", length);
        writePackedElements(methodBuilder, name, source, valueType, cast, nullElement, (builder, value, _) -> builder.addStatement("$L += $L", length, getPackedElementSize(valueType.protobufType(), value)));
        return length;
    }

    /**
     * Generates a loop that converts every element of a packed field to its wire value, and
     * passes the value to the given handler.
     *
     * @param methodBuilder the method builder to append the code to
     * @param name          the name of the field
     * @param source        the name of the local variable that holds the elements
     * @param valueType     the type of the elements
     * @param cast          {@code true} if a type cast is needed for generic element types
     * @param nullElement   the statement executed for an element that is, or is converted to,
     *                      {@code null}, or {@code null} to skip it
     * @param handler       the handler invoked with the name of the variable that holds the
     *                      wire value of each element
     */
    private void writePackedElements(MethodSpec.Builder methodBuilder, String name, String source, ProtobufSimpleFieldType valueType, boolean cast, CodeBlock nullElement, CustomSerializerHandler handler) {
        var entry = name + "Entry";
        methodBuilder.beginControlFlow("for (var $L : $L)", entry, source);
        writeCustomSerializer(
                methodBuilder,
                0,
                entry,
                entry,
                valueType,
                false,
                true,
                cast,
                false,
                false,
                nullElement,
                (_, _, _) -> {
                    throw new IllegalStateException("Internal bug: packed values cannot be messages");
                },
                handler
        );
        methodBuilder.endControlFlow();
    }

    /**
     * Returns the expression that computes the size of the given wire value in the payload
     * of a packed field, which has no tag.
     *
     * @param protobufType the type of the elements of the packed field
     * @param value        the expression that provides the wire value
     * @return the size expression
     * @throws IllegalArgumentException if the type cannot be packed
     */
    private static String getPackedElementSize(ProtobufType protobufType, String value) {
        var calculator = ProtobufBinarySizeCalculator.class.getName();
        return switch (protobufType) {
            case BOOL -> "1";
            case FLOAT, FIXED32, SFIXED32 -> "Integer.BYTES";
            case DOUBLE, FIXED64, SFIXED64 -> "Long.BYTES";
            case ENUM, INT32 -> "%s.getVarInt32Size(%s)".formatted(calculator, value);
            case UINT32 -> "%s.getUnsignedVarInt32Size(%s)".formatted(calculator, value);
            case SINT32 -> "%s.getZigZagVarInt32Size(%s)".formatted(calculator, value);
            case INT64, UINT64 -> "%s.getVarInt64Size(%s)".formatted(calculator, value);
            case SINT64 -> "%s.getZigZagVarInt64Size(%s)".formatted(calculator, value);
            default -> throw new IllegalArgumentException("Internal bug: %s fields cannot be packed".formatted(protobufType));
        };
    }

    /**
     * Returns the statement that writes the given wire value in the payload of a packed
     * field, which has no tag.
     *
     * @param protobufType the type of the elements of the packed field
     * @param value        the expression that provides the wire value
     * @return the write statement
     * @throws IllegalArgumentException if the type cannot be packed
     */
    private static String getPackedElementWrite(ProtobufType protobufType, String value) {
        return switch (protobufType) {
            case BOOL -> "%s.writeRawByte((byte) (%s ? 1 : 0))".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case FLOAT -> "%s.writeRawFloat(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case DOUBLE -> "%s.writeRawDouble(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case FIXED32, SFIXED32 -> "%s.writeRawFixedInt32(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case FIXED64, SFIXED64 -> "%s.writeRawFixedInt64(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case ENUM, INT32 -> "%s.writeRawVarInt32(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case UINT32 -> "%s.writeRawUnsignedVarInt32(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case SINT32 -> "%s.writeRawZigZagVarInt32(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case INT64, UINT64 -> "%s.writeRawVarInt64(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            case SINT64 -> "%s.writeRawZigZagVarInt64(%s)".formatted(OUTPUT_OBJECT_PARAMETER, value);
            default -> throw new IllegalArgumentException("Internal bug: %s fields cannot be packed".formatted(protobufType));
        };
    }

    /**
     * Generates code that serializes a map field by writing each entry as a length-delimited
     * message to the protobuf output stream.
     *
     * <p>Each map entry is encoded as a nested message with the key at field index 1 and the
     * value at field index 2. The generated code iterates over the map's entry set, writes the
     * message header with the pre-computed entry size, then writes the key and value fields.
     *
     * <p>For example, given a {@code Map<String, Integer>} field named "scores" at index 3:
     *
     * <pre>{@code
     * if (scores != null) {
     *     for (var scoresEntry : scores.entrySet()) {
     *         protoOutputStream.writeMessageField(3, sizeOfScores(scoresEntry));
     *         protoOutputStream.writeString(1, scoresEntry.getKey());
     *         var scoresValue = scoresEntry.getValue();
     *         if (scoresValue != null) {
     *             protoOutputStream.writeInt32(2, scoresValue);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param methodBuilder the method builder to append the serialization code to
     * @param index         the protobuf field index
     * @param name          the name of the map field being serialized
     * @param accessor      the expression that accesses the map value
     * @param mapType       the map field type descriptor containing key and value type information
     */
    protected void writeMapSerializer(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufMapFieldType mapType) {
        // Null check the map
        methodBuilder.beginControlFlow("if ($L != null)", accessor);
        var localVariableName = "%sEntry".formatted(name);

        // Iterate over map entries
        methodBuilder.beginControlFlow("for (var $L : $L.entrySet())", localVariableName, accessor);

        // Write the map entry as a message, whose length is read from the size table or computed by the size size
        if (usesSizeTable()) {
            methodBuilder.addStatement("$L.writeMessageField($L, $L.next())", OUTPUT_OBJECT_PARAMETER, index, SIZE_TABLE_PARAMETER);
        } else {
            var methodName = ProtobufSizeGenerator.getMapFieldMethodName(name);
            methodBuilder.addStatement("$L.writeMessageField($L, $L($L))", OUTPUT_OBJECT_PARAMETER, index, methodName, localVariableName);
        }

        // Write key (field index 1 in map entry message)
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

        // Write value (field index 2 in map entry message)
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

        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
    }

    /**
     * Generates code that serializes a normal (non-repeated, non-map) field to the protobuf
     * output stream.
     *
     * <p>This method delegates to {@link #writeCustomSerializer} with default handlers that
     * simply add each serialization statement to the method builder.
     *
     * @param methodBuilder    the method builder to append the serialization code to
     * @param index            the protobuf field index
     * @param name             the name of the field being serialized
     * @param value            the expression that provides the field value
     * @param type             the protobuf field type descriptor
     * @param declareVariable  {@code true} if a local variable should be declared for the value
     * @param variableNullCheck {@code true} if a null check should guard the serialization
     * @param cast             {@code true} if a type cast is needed for generic types
     * @param implicit         {@code true} if the field has implicit presence, so that its
     *                         default value is not written
     * @param delimited        {@code true} if the field is a message encoded as a group
     * @param nullValue        the statement executed if the value is, or is converted to,
     *                         {@code null}, or {@code null} to skip the value, which is how a
     *                         field that is not set is not written
     */
    protected void writeNormalSerializer(MethodSpec.Builder methodBuilder, int index, String name, String value, ProtobufValueFieldType type, boolean declareVariable, boolean variableNullCheck, boolean cast, boolean implicit, boolean delimited, CodeBlock nullValue) {
        writeCustomSerializer(
                methodBuilder,
                index,
                name,
                value,
                type,
                declareVariable,
                variableNullCheck,
                cast,
                implicit,
                delimited,
                nullValue,
                // Both object and primitive handlers just add statements directly
                (builder, serializedName, serializerStatements) -> {
                    for(var serializerStatement : serializerStatements) {
                        builder.addStatement("$L", serializerStatement);
                    }
                },
                (builder, serializedName, serializerStatements) -> {
                    for(var serializerStatement : serializerStatements) {
                        builder.addStatement("$L", serializerStatement);
                    }
                }
        );
    }

    /**
     * Generates code that serializes a field with an optional custom serializer chain, using
     * the provided handlers to write the final result.
     *
     * <p>This method implements the full serialization pipeline:
     * <ul>
     * <li>Applies a type cast if needed for generic or wildcard types.
     * <li>Declares a local variable to avoid repeated accessor calls.
     * <li>Inserts a null check for non-primitive values.
     * <li>Applies each custom serializer in the chain, null-checking intermediate results.
     * <li>Skips the default value of a field with implicit presence.
     * <li>Writes the final value to the stream using the appropriate write method, or
     *     the header and the body of a message, delimited by group tags if the message
     *     is encoded as a group.
     * <li>Closes all null-check control flow blocks.
     * </ul>
     *
     * <p>For example, given a field annotated with a custom serializer:
     *
     * <pre>{@code
     * var timestamp = protoInputObject.timestamp();
     * if (timestamp != null) {
     *     var timestamp0 = MyConverter.toProto(timestamp);
     *     if (timestamp0 != null) {
     *         protoOutputStream.writeInt64(1, timestamp0);
     *     }
     * }
     * }</pre>
     *
     * @param methodBuilder     the method builder to append the serialization code to
     * @param index             the protobuf field index
     * @param name              the name of the field being serialized
     * @param value             the expression that provides the field value
     * @param type              the protobuf field type descriptor
     * @param declareVariable   {@code true} if a local variable should be declared for the value
     * @param variableNullCheck {@code true} if a null check should guard the initial value
     * @param cast              {@code true} if a type cast is needed for generic types
     * @param implicit          {@code true} if the field has implicit presence, so that its
     *                          default value is not written
     * @param delimited         {@code true} if the field is a message encoded as a group
     * @param nullValue         the statement executed if the value is, or is converted to,
     *                          {@code null}, or {@code null} to skip the value
     * @param objectWriter      the handler invoked to write messages
     * @param streamWriter      the handler invoked to write primitive, string and enum types
     */
    protected void writeCustomSerializer(MethodSpec.Builder methodBuilder, int index, String name, String value, ProtobufValueFieldType type, boolean declareVariable, boolean variableNullCheck, boolean cast, boolean implicit, boolean delimited, CodeBlock nullValue, CustomSerializerHandler objectWriter, CustomSerializerHandler streamWriter) {
        // Step 1: Apply cast if necessary (for generics/wildcards)
        if(cast) {
            var castType = getQualifiedName(type.descriptorElementType());
            value = "((%s) %s)".formatted(castType, value);
        }

        // Step 2: Declare a variable to avoid repeated accessor calls
        var propertyName = value;
        if(declareVariable) {
            methodBuilder.addStatement("var $L = $L", name, value);
            propertyName = name;
        }

        // Get the stream write method for the final result (empty for MESSAGE/ENUM/GROUP)
        var writeMethod = getStreamMethodName(type.protobufType(), false);

        // Track how many null-check blocks we open (so we can close them all at the end)
        var controlFlowDepth = 0;

        // Step 3: Null check the initial value if it's a non-primitive type
        if(variableNullCheck && !(type.accessorType() instanceof PrimitiveType)) {
            controlFlowDepth += writeNullCheck(methodBuilder, propertyName, nullValue);
        }

        // Step 4: Apply each custom serializer in the chain
        var serializers = type.serializers();
        for(var i = 0; i < serializers.size(); i++) {
            var serializer = serializers.get(i);

            // Build the serializer invocation (e.g., "MyConverter.toProto(value)")
            var result = createSerializerInvocation(serializer, propertyName);

            // Check if this is the last serializer and it's an object type or void
            var lastSerializer = i == serializers.size() - 1;
            if ((lastSerializer && writeMethod.isEmpty()) || serializer.returnType().getKind() == TypeKind.VOID) {
                objectWriter.handle(methodBuilder, propertyName, getMessageWrite(index, type, serializer, propertyName, result, delimited));
                continue;
            }

            // Store the result of this serialization round in a new variable
            var newPropertyName = name + i;
            methodBuilder.addStatement("var $L = $L", newPropertyName, result);

            // Null check the result if it's not a primitive: the index of an unknown enum constant is null
            if (serializer.returnType() instanceof PrimitiveType) {
                propertyName = newPropertyName;
                continue;
            }

            controlFlowDepth += writeNullCheck(methodBuilder, newPropertyName, nullValue);
            propertyName = newPropertyName;
        }

        // Step 5: Write the final value to the stream, unless it is the default value of an implicit field
        if(writeMethod.isPresent()) {
            if(implicit) {
                methodBuilder.beginControlFlow("if ($L)", getNonDefaultCondition(type.protobufType(), propertyName));
            }
            streamWriter.handle(methodBuilder, propertyName, List.of(getValueWrite(index, type, propertyName)));
            if(implicit) {
                methodBuilder.endControlFlow();
            }
        }

        // Step 6: Close all null-check control flows
        for (var i = 0; i < controlFlowDepth; i++) {
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Returns the statements that write a message, whose body is written by the given
     * invocation of the last serializer of its type.
     *
     * @implSpec
     * The default implementation precedes the body of the message by its tag and its length,
     * computed by the sizer attached to the serializer, or encloses it between the start and
     * end tags of a group if the message is encoded as a group. If the generated method
     * {@linkplain #usesSizeTable() uses a size table} and the serializer is the one of a Spec,
     * the length is read from the size table instead, and the body is written with the size
     * table. A serializer that returns {@code void} and does not write a message is only
     * invoked.
     *
     * @param index        the protobuf field index
     * @param type         the type of the field
     * @param serializer   the last serializer of the type of the field
     * @param propertyName the name of the variable that holds the value passed to the
     *                     serializer
     * @param invocation   the invocation of the serializer
     * @param delimited    {@code true} if the field is a message encoded as a group
     * @return the statements
     */
    protected List<String> getMessageWrite(int index, ProtobufValueFieldType type, DaedalusConverterElement.Attributed.Serializer serializer, String propertyName, String invocation, boolean delimited) {
        if (type.protobufType() != ProtobufType.MESSAGE) {
            return List.of(invocation);
        }

        var specReceiver = usesSizeTable() ? getSpecReceiver(serializer) : Optional.<String>empty();
        if (specReceiver.isPresent()) {
            var body = "%s.%s(%s, %s, %s)".formatted(specReceiver.get(), METHOD_NAME, propertyName, OUTPUT_OBJECT_PARAMETER, SIZE_TABLE_PARAMETER);
            if (delimited) {
                return List.of(
                        "%s.writeStartGroupField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, index),
                        body,
                        "%s.writeEndGroupField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, index)
                );
            }

            return List.of("%s.writeMessageField(%s, %s.next())".formatted(OUTPUT_OBJECT_PARAMETER, index, SIZE_TABLE_PARAMETER), body);
        }

        if (delimited) {
            return List.of(
                    "%s.writeStartGroupField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, index),
                    invocation,
                    "%s.writeEndGroupField(%s)".formatted(OUTPUT_OBJECT_PARAMETER, index)
            );
        }

        return List.of(getMessageMethod(index, serializer, propertyName), invocation);
    }

    /**
     * Returns the statement that writes a wire value of a field that is not a message.
     *
     * @implSpec
     * The default implementation invokes the write method of the binary writer that matches
     * the protobuf type of the field, passing it the index of the field and the value.
     *
     * @param index the protobuf field index
     * @param type  the type of the field
     * @param value the expression that provides the wire value
     * @return the statement
     */
    protected String getValueWrite(int index, ProtobufValueFieldType type, String value) {
        return "%s.%s(%s, %s)".formatted(
                OUTPUT_OBJECT_PARAMETER,
                getStreamMethodName(type.protobufType(), false).orElseThrow(),
                index,
                value
        );
    }

    /**
     * Generates the null check of a value, which either executes the given statement if the
     * value is {@code null}, or opens a block that is only entered if it is not.
     *
     * @param methodBuilder the method builder to append the null check to
     * @param value         the name of the variable that holds the value
     * @param nullValue     the statement executed if the value is {@code null}, or
     *                      {@code null} to open a block that skips it
     * @return the number of blocks opened, which the caller must close
     */
    private int writeNullCheck(MethodSpec.Builder methodBuilder, String value, CodeBlock nullValue) {
        if (nullValue == null) {
            methodBuilder.beginControlFlow("if ($L != null)", value);
            return 1;
        }

        methodBuilder.beginControlFlow("if ($L == null)", value);
        methodBuilder.addStatement(nullValue);
        methodBuilder.endControlFlow();
        return 0;
    }

    /**
     * Generates the message header write expression that includes the message size.
     *
     * <p>The returned expression calls the sizer attached to the serializer and writes
     * the result as a message field header to the output stream.
     *
     * @param index        the protobuf field index
     * @param serializer   the serializer carrying the sizer to invoke
     * @param propertyName the name of the property being serialized
     * @return the message header write expression as a string
     */
    private String getMessageMethod(int index, DaedalusConverterElement.Attributed.Serializer serializer, String propertyName) {
        return "%s.writeMessageField(%s, %s)".formatted(
                OUTPUT_OBJECT_PARAMETER,
                index,
                createSizerInvocation(serializer, propertyName)
        );
    }

    /**
     * Builds the invocation of the sizer attached to the given serializer, passing the
     * arguments supplied by the use site to its additional parameters.
     *
     * <p>For example, given the synthetic sizer of a message named {@code Person}, which is
     * invoked on the instance of its Spec:
     *
     * <pre>{@code
     * PersonSpec.global().sizeOfBinary(value)
     * }</pre>
     *
     * @param serializer the serializer carrying the sizer to invoke
     * @param value      the expression that provides the value whose size is computed
     * @return the sizer invocation expression as a string
     */
    protected String createSizerInvocation(DaedalusConverterElement.Attributed.Serializer serializer, String value) {
        return serializer.sizer().invoke(value, serializer.sizerArguments(), _ -> OUTPUT_OBJECT_PARAMETER);
    }

    /**
     * Returns the condition that is {@code true} if the given wire value is not the default
     * value of its type, which a field with implicit presence does not write.
     *
     * <p>Floating point values are compared by their bits, so that {@code -0.0} is written,
     * as it is not the default value.
     *
     * @param protobufType the type of the value
     * @param value        the expression that provides the wire value
     * @return the condition
     * @throws IllegalArgumentException if the type has no default value, like a message
     */
    protected static String getNonDefaultCondition(ProtobufType protobufType, String value) {
        return switch (protobufType) {
            case BOOL -> value;
            case STRING -> "!%s.isEmpty()".formatted(value);
            case BYTES -> "%s.length != 0".formatted(value);
            case FLOAT -> "Float.floatToRawIntBits(%s) != 0".formatted(value);
            case DOUBLE -> "Double.doubleToRawLongBits(%s) != 0".formatted(value);
            case ENUM, INT32, SINT32, UINT32, FIXED32, SFIXED32, INT64, SINT64, UINT64, FIXED64, SFIXED64 -> "%s != 0".formatted(value);
            default -> throw new IllegalArgumentException("Internal bug: %s fields have no default value".formatted(protobufType));
        };
    }

    /**
     * A callback interface for handling the final write operation during custom serialization.
     *
     * <p>Implementations of this interface define different strategies for emitting the
     * serialization statements, allowing callers to customize how object types (MESSAGE, ENUM,
     * GROUP) and primitive types are written to the output stream or size accumulator.
     */
    protected interface CustomSerializerHandler {
        /**
         * Handles the emission of serialization statements for a given field value.
         *
         * @param builder    the method builder to append the statements to
         * @param value      the name of the variable holding the value to serialize
         * @param statements the list of serialization statement expressions to emit
         */
        void handle(MethodSpec.Builder builder, String value, List<String> statements);
    }

    /**
     * Builds a serializer method invocation string based on the serializer's signature.
     *
     * <p>The invocation format depends on the serializer method's characteristics:
     * <ul>
     * <li>Instance methods of the type of the value: {@code value.toProto()}
     * <li>Static methods, or instance methods of a mixin invoked on its instance, with 1
     *     parameter (custom mixin): {@code MyConverter.toProto(value)}
     * <li>Methods with 2 parameters (MESSAGE Spec), invoked on the instance of the Spec:
     *     {@code MessageSpec.global().encodeBinary(value, protoOutputStream)}
     * </ul>
     *
     * <p>A serializer that declares additional parameters instead receives the arguments
     * supplied by the use site, and the output stream for each parameter whose type is a
     * writer: for example, {@code InstantMixin.toNullable(value, java.util.concurrent.TimeUnit.SECONDS)}.
     *
     * @param serializer the serializer element describing the conversion method
     * @param value      the expression that provides the value to serialize
     * @return the serializer invocation expression as a string
     * @throws IllegalArgumentException if the serializer has an unexpected number of parameters
     */
    protected String createSerializerInvocation(DaedalusConverterElement.Attributed.Serializer serializer, String value) {
        // Additional parameters: pass the arguments supplied by the use site, and the stream to writer parameters
        if (!serializer.arguments().isEmpty()) {
            return serializer.delegate().invoke(value, serializer.arguments(), _ -> OUTPUT_OBJECT_PARAMETER);
        }

        // Instance method of the type of the value: call on the value object
        var receiver = serializer.delegate().receiver();
        if (receiver.isEmpty() && !serializer.delegate().modifiers().contains(Modifier.STATIC)) {
            return "%s.%s()".formatted(value, serializer.delegate().name());
        }

        // Static method, or instance method of a mixin called on its instance: dispatch based on parameter count
        var target = receiver.orElse(serializer.delegate().ownerName());
        return switch (serializer.delegate().parameters().size()) {
            // Normal mixin serializer: takes just the value
            case 1 -> "%s.%s(%s)".formatted(
                    target,
                    serializer.delegate().name(),
                    value
            );

            // Spec serializer for MESSAGE: takes value + stream
            case 2 -> "%s.%s(%s, %s)".formatted(
                    target,
                    serializer.delegate().name(),
                    value,
                    OUTPUT_OBJECT_PARAMETER
            );

            default -> throw new IllegalArgumentException(
                    "Unexpected number of arguments for serializer "
                            +  serializer.delegate().name()
                            + " in "
                            + serializer.delegate().ownerName()
            );
        };
    }

    /**
     * Maps a protobuf type to its corresponding {@code ProtobufOutputStream} write method name.
     *
     * <p>Returns an empty {@link Optional} for {@code MESSAGE} and {@code GROUP} types, which
     * are handled separately through synthetic serializer methods on the Spec class. Packed
     * variants return method names that write entire collections at once (e.g.,
     * {@code writePackedInt32Field}).
     *
     * @param protobufType the protobuf type to map
     * @param packed       {@code true} if the field uses packed encoding
     * @return an {@code Optional} containing the write method name, or empty for message and
     *         group types
     */
    private Optional<String> getStreamMethodName(ProtobufType protobufType, boolean packed) {
        var result = switch (protobufType) {
            case STRING -> "writeStringField";
            case ENUM, INT32 -> packed ? "writePackedInt32Field" : "writeInt32Field";
            case SINT32 -> packed ? "writePackedSInt32Field" : "writeSInt32Field";
            case UINT32 -> packed ? "writePackedUInt32Field" : "writeUInt32Field";
            case MESSAGE -> null; // Handled separately
            case BYTES -> "writeBytesField";
            case BOOL -> packed ? "writePackedBoolField" : "writeBoolField";
            case FLOAT -> packed ? "writePackedFloatField" : "writeFloatField";
            case DOUBLE -> packed ? "writePackedDoubleField" : "writeDoubleField";
            case FIXED32 -> packed ? "writePackedFixed32Field" : "writeFixed32Field";
            case SFIXED32 -> packed ? "writePackedSFixed32Field" : "writeSFixed32Field";
            case INT64 -> packed ? "writePackedInt64Field" : "writeInt64Field";
            case SINT64 -> packed ? "writePackedSInt64Field" : "writeSInt64Field";
            case UINT64 -> packed ? "writePackedUInt64Field" : "writeUInt64Field";
            case FIXED64 -> packed ? "writePackedFixed64Field" : "writeFixed64Field";
            case SFIXED64 -> packed ? "writePackedSFixed64Field" : "writeSFixed64Field";
        };

        return Optional.ofNullable(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code public static} modifiers, as all generated serialization methods
     * are static methods on the Spec class.
     *
     * @return a list containing {@link Modifier#PUBLIC} and {@link Modifier#STATIC}
     */
    @Override
    protected List<Modifier> modifiers() {
        return List.of(Modifier.PUBLIC, Modifier.STATIC);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the exceptions resolved by {@link ProtobufExceptionManager#serializationExceptions(ProtobufTypeElement)}.
     *
     * @return the checked exceptions of the generated serialization method
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return exceptions.serializationExceptions(ownerElement);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the statement that wraps the checked exceptions of the converters of a message
     * in a {@link ProtobufSerializationException}, if they throw any, as described by
     * {@link ProtobufExceptionManager#wrapsOutgoing(ProtobufTypeElement)}.
     *
     * @return the statement, or an empty {@link Optional} if no exception is wrapped
     */
    @Override
    protected Optional<CodeBlock> wrapperStatement() {
        if (!exceptions.wrapsOutgoing(ownerElement)) {
            return Optional.empty();
        }

        return Optional.of(CodeBlock.of("throw $T.converterFailure($S, $L)", ProtobufSerializationException.class, ProtobufTypeElement.getProtobufName(ownerElement.typeElement()), WRAPPED_EXCEPTION));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@value #METHOD_NAME}, the standard name for generated serialization methods.
     *
     * @return the method name
     */
    @Override
    protected String name() {
        return METHOD_NAME;
    }
}
