package com.github.auties00.daedalus.protobuf.processor.generator;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.protobuf.model.ProtobufMessageEncoding;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufUtf8Validation;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderTypeGenerator;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * An abstract base class for deserialization method generators that provides shared logic
 * for decoding protobuf values from a binary input stream.
 *
 * <p>This class extends {@link DaedalusMethodGenerator} and offers reusable methods for
 * deserializing normal, repeated, and map fields, including support for custom
 * {@code @ProtobufDeserializer} converter chains. Concrete subclasses in the message and
 * enumeration subpackages implement type-specific decoding strategies.
 *
 * <p>Each generated method reads field tags from a {@code ProtobufBinaryReader} and dispatches
 * to the appropriate read method based on the field's protobuf type.
 */
public abstract class ProtobufDeserializationGenerator extends DaedalusMethodGenerator {
    /**
     * The name of the generated deserialization method.
     */
    public static final String METHOD_NAME = "decodeBinary";

    /**
     * The name of the generated method that decodes the fields of a message into its builder.
     */
    public static final String PARTIAL_METHOD_NAME = "decodePartial";
    private static final String INPUT_STREAM_NAME = "protoInputStream";

    /**
     * The protobuf type element that owns the fields being deserialized.
     */
    protected final ProtobufTypeElement ownerElement;

    /**
     * The manager that resolves the checked exceptions of the generated method.
     */
    protected final ProtobufExceptionManager exceptions;

    /**
     * Constructs a new deserialization generator for the given protobuf type element.
     *
     * @param ownerElement the protobuf type element whose fields this generator deserializes
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufDeserializationGenerator(ProtobufTypeElement ownerElement, ProtobufExceptionManager exceptions) {
        this.ownerElement = ownerElement;
        this.exceptions = exceptions;
    }

    /**
     * Generates a switch case block that deserializes a map field from the protobuf stream.
     *
     * <p>The generated code reads a length-delimited map entry, declares temporary key and value
     * variables, iterates over the nested fields (field 1 for the key, field 2 for the value),
     * and inserts the deserialized entry into the target map. For example, given a map field
     * named "scores" with {@code String} keys and {@code Integer} values:
     *
     * <pre>{@code
     * var scoresInputStream = protoInputStream.readLengthDelimitedField();
     * String scoresKey = null;
     * Integer scoresValue = null;
     * while (scoresInputStream.nextFieldTag()) {
     *     switch (scoresInputStream.fieldIndex()) {
     *         case 1:
     *             scoresKey = scoresInputStream.readString();
     *             break;
     *         case 2:
     *             scoresValue = scoresInputStream.readInt32();
     *             break;
     *     }
     * }
     * scores.put(scoresKey, scoresValue);
     * break;
     * }</pre>
     *
     * <p>The entry is stored by the statements returned by
     * {@link #getMapPut(String, ProtobufMapFieldType, String, String)}.
     *
     * @param name           the name of the map field being deserialized
     * @param mapType        the map field type descriptor containing key and value type information
     * @param utf8Validation the UTF-8 validation strategy to apply when reading string fields
     * @return a {@link CodeBlock} containing the generated map deserialization logic
     */
    protected CodeBlock writeMapDeserializer(String name, ProtobufMapFieldType mapType, ProtobufUtf8Validation utf8Validation) {
        var caseBlock = CodeBlock.builder();

        // Read the length-delimited map entry into a new stream
        var streamName = "%sInputStream".formatted(name);
        caseBlock.addStatement("var $L = $L.readLengthDelimitedField()", streamName, INPUT_STREAM_NAME);

        // Declare key and value variables
        var keyTypeName = getQualifiedName(mapType.keyType().accessorType());
        var valueTypeName = getQualifiedName(mapType.valueType().accessorType());
        var keyName = "%sKey".formatted(name);
        var valueName = "%sValue".formatted(name);
        caseBlock.addStatement("$L $L = null", keyTypeName, keyName);
        caseBlock.addStatement("$L $L = null", valueTypeName, valueName);

        // Get the read methods and conversion chains for key and value
        var keyReadFunction = applyDeserializers(getReadExpression(streamName, mapType.keyType(), utf8Validation), mapType.keyType(), streamName);
        var valueReadFunction = applyDeserializers(getReadExpression(streamName, mapType.valueType(), utf8Validation), mapType.valueType(), streamName);

        // Read the map entry stream (field 1 = key, field 2 = value)
        caseBlock.beginControlFlow("while ($L.nextFieldTag())", streamName);
        caseBlock.beginControlFlow("switch ($L.fieldIndex())", streamName);
        caseBlock.add("case 1:\n").indent();
        caseBlock.addStatement("$L = $L", keyName, keyReadFunction);
        caseBlock.addStatement("break");
        caseBlock.unindent();
        caseBlock.add("case 2:\n").indent();
        caseBlock.addStatement("$L = $L", valueName, valueReadFunction);
        caseBlock.addStatement("break");
        caseBlock.unindent();
        // Per the protobuf spec, unknown fields inside a map entry must be skipped
        caseBlock.add("default:\n").indent();
        caseBlock.addStatement("$L.skipUnknownField()", streamName);
        caseBlock.addStatement("break");
        caseBlock.unindent();
        caseBlock.endControlFlow();
        caseBlock.endControlFlow();

        // Put the deserialized entry into the map
        caseBlock.add(getMapPut(name, mapType, keyName, valueName));
        caseBlock.addStatement("break");
        return caseBlock.build();
    }

    /**
     * Returns the expression that accesses the value of the given field, which a deserialized
     * value is stored in.
     *
     * @implSpec
     * The default implementation returns the name of the field, which is a local variable.
     *
     * @param name the name of the field
     * @return the expression that accesses the value of the field
     */
    protected String getFieldTarget(String name) {
        return name;
    }

    /**
     * Returns the statements that assign a deserialized value to the given field that is not
     * repeated.
     *
     * @implSpec
     * The default implementation assigns the value to the
     * {@linkplain #getFieldTarget(String) field}.
     *
     * @param name the name of the field
     * @param value the expression that provides the value
     * @return the statements
     */
    protected CodeBlock getFieldAssignment(String name, String value) {
        return CodeBlock.builder()
                .addStatement("$L = $L", getFieldTarget(name), value)
                .build();
    }

    /**
     * Returns the statements that add a deserialized element to the collection of the given
     * repeated field.
     *
     * @implSpec
     * The default implementation adds the element to the collection of the
     * {@linkplain #getFieldTarget(String) field}.
     *
     * @param name the name of the field
     * @param value the expression that provides the element
     * @return the statements
     */
    protected CodeBlock getCollectionAdd(String name, String value) {
        return CodeBlock.builder()
                .addStatement("$L.add($L)", getFieldTarget(name), value)
                .build();
    }

    /**
     * Returns the statements that put a deserialized entry in the map of the given map field.
     *
     * @implSpec
     * The default implementation puts the entry in the map of the
     * {@linkplain #getFieldTarget(String) field}.
     *
     * @param name the name of the field
     * @param mapType the type of the field
     * @param key the expression that provides the key
     * @param value the expression that provides the value
     * @return the statements
     */
    protected CodeBlock getMapPut(String name, ProtobufMapFieldType mapType, String key, String value) {
        return CodeBlock.builder()
                .addStatement("$L.put($L, $L)", getFieldTarget(name), key, value)
                .build();
    }

    /**
     * Generates a switch case block that deserializes a normal or repeated field from the
     * protobuf stream.
     *
     * <p>For normal fields, the generated code assigns the value read from the stream. For
     * repeated fields, it adds each element to the collection. A repeated field of a type that
     * can be packed accepts both the packed and the expanded encoding, as Protocol Buffers
     * requires, whatever the encoding it was declared with. A message encoded as a group is
     * read between its start and end tags.
     *
     * <p>An enum value that has no constant is not assigned: it is added to the unknown fields
     * of the message if it has any, and dropped otherwise, as Protocol Buffers does for closed
     * enums.
     *
     * <p>For example, given a normal {@code String} field named "name":
     *
     * <pre>{@code
     * name = protoInputStream.readStringField(true);
     * break;
     * }</pre>
     *
     * <p>For a repeated {@code List<Integer>} field named "numbers":
     *
     * <pre>{@code
     * if (protoInputStream.fieldWireType() == ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
     *     for (var numbersPacked : protoInputStream.readPackedInt32Field()) {
     *         numbers.add(numbersPacked);
     *     }
     * } else {
     *     numbers.add(protoInputStream.readInt32Field());
     * }
     * break;
     * }</pre>
     *
     * @param name            the name of the field being deserialized
     * @param index           the index of the field being deserialized
     * @param type            the protobuf field type descriptor, or the type of the elements
     *                        of a repeated field
     * @param repeated        {@code true} if the field is a repeated (collection) field
     * @param utf8Validation  the UTF-8 validation strategy to apply when reading string fields
     * @param messageEncoding the message encoding format for nested message fields
     * @param unknownFields   the name of the variable that holds the unknown fields of the
     *                        message, or {@code null} if the message does not keep them
     * @return a {@link CodeBlock} containing the generated deserialization logic
     */
    protected CodeBlock writeDeserializer(String name, int index, ProtobufValueFieldType type, boolean repeated, ProtobufUtf8Validation utf8Validation, ProtobufMessageEncoding messageEncoding, String unknownFields) {
        var caseBlock = CodeBlock.builder();
        writeValuesDeserializer(caseBlock, name, index, type, repeated ? ValueTarget.COLLECTION : ValueTarget.FIELD, utf8Validation, messageEncoding, unknownFields);
        return caseBlock.addStatement("break").build();
    }

    /**
     * Generates the statements that read the values of a field from the stream and store them
     * in the given target.
     *
     * <p>A repeated packable field accepts both the packed and the expanded encoding, and a
     * message encoded as a group is read between its start and end tags.
     *
     * @param caseBlock       the block to add the statements to
     * @param name            the name of the field
     * @param index           the index of the field
     * @param type            the type of the field, or of the elements of a repeated field
     * @param target          where the values are stored
     * @param utf8Validation  the UTF-8 validation strategy to apply when reading string fields
     * @param messageEncoding the message encoding format for nested message fields
     * @param unknownFields   the name of the variable that holds the unknown fields of the
     *                        message, or {@code null} if the message does not keep them
     */
    private void writeValuesDeserializer(CodeBlock.Builder caseBlock, String name, int index, ProtobufValueFieldType type, ValueTarget target, ProtobufUtf8Validation utf8Validation, ProtobufMessageEncoding messageEncoding, String unknownFields) {
        if (target != ValueTarget.FIELD && type.protobufType().isPackable()) {
            // Packable fields accept both encodings
            caseBlock.beginControlFlow("if ($L.fieldWireType() == $T.WIRE_TYPE_LENGTH_DELIMITED)", INPUT_STREAM_NAME, ProtobufWireType.class);
            var packedEntryName = name + "Packed";
            caseBlock.beginControlFlow("for (var $L : $L.$L)", packedEntryName, INPUT_STREAM_NAME, getDeserializerStreamMethod(type, true, utf8Validation));
            writeValueAssignment(caseBlock, name, index, type, packedEntryName, target, unknownFields);
            caseBlock.endControlFlow();
            caseBlock.nextControlFlow("else");
            writeValueAssignment(caseBlock, name, index, type, getReadExpression(INPUT_STREAM_NAME, type, utf8Validation), target, unknownFields);
            caseBlock.endControlFlow();
        } else if (type.protobufType() == ProtobufType.MESSAGE && messageEncoding == ProtobufMessageEncoding.DELIMITED) {
            // A group shares the stream of its parent, between its start and end tags
            caseBlock.addStatement("$L.readStartGroupField($L)", INPUT_STREAM_NAME, index);
            writeValueAssignment(caseBlock, name, index, type, INPUT_STREAM_NAME, target, unknownFields);
            caseBlock.addStatement("$L.readEndGroupField($L)", INPUT_STREAM_NAME, index);
        } else {
            writeValueAssignment(caseBlock, name, index, type, getReadExpression(INPUT_STREAM_NAME, type, utf8Validation), target, unknownFields);
        }
    }

    /**
     * Generates a switch case block that deserializes a repeated field held by an array.
     *
     * <p>A packed array that is converted from the array of the primitive type of its wire
     * values is read in bulk, by {@link #writeBulkDeserializer(String, ProtobufValueFieldType)}.
     * Any other array is read like a collection, converting each element from its wire
     * value and appending it to the array, used as a buffer whose number of elements is
     * tracked by a separate {@code int} variable, named after the field with the
     * {@code Count} suffix, that the caller must declare. The caller must store the elements
     * of the buffer in the field after the last field is read.
     *
     * @param name            the name of the field being deserialized
     * @param index           the index of the field being deserialized
     * @param arrayType       the array field type descriptor
     * @param utf8Validation  the UTF-8 validation strategy to apply when reading string fields
     * @param messageEncoding the message encoding format for nested message fields
     * @param unknownFields   the name of the variable that holds the unknown fields of the
     *                        message, or {@code null} if the message does not keep them
     * @return a {@link CodeBlock} containing the generated deserialization logic
     */
    protected CodeBlock writeArrayDeserializer(String name, int index, ProtobufArrayFieldType arrayType, ProtobufUtf8Validation utf8Validation, ProtobufMessageEncoding messageEncoding, String unknownFields) {
        if (arrayType.bulk()) {
            return writeBulkDeserializer(getBulkBuffer(name, arrayType), arrayType);
        }

        var caseBlock = CodeBlock.builder();
        writeValuesDeserializer(caseBlock, name, index, arrayType.valueType(), ValueTarget.ARRAY, utf8Validation, messageEncoding, unknownFields);
        return caseBlock.addStatement("break").build();
    }

    /**
     * Generates a switch case block that deserializes a packed field read in bulk, into a
     * buffer that is an array of the primitive type of its wire values.
     *
     * <p>The number of elements of the buffer is tracked by a separate {@code int} variable,
     * named after the buffer with the {@code Count} suffix, that the caller must declare.
     * Like any packable field, the field accepts both the packed and the expanded encoding:
     * the first packed occurrence becomes the buffer, the following ones are copied at its
     * end, and an expanded element is appended to the buffer, whose capacity doubles when it
     * is full.
     *
     * <p>For example, given a buffer named "numbers" of an {@code int32} field:
     *
     * <pre>{@code
     * if (protoInputStream.fieldWireType() == ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED) {
     *     var numbersPacked = protoInputStream.readPackedInt32Field();
     *     if (numbersCount == 0) {
     *         numbers = numbersPacked;
     *     } else {
     *         numbers = Arrays.copyOf(numbers, numbersCount + numbersPacked.length);
     *         System.arraycopy(numbersPacked, 0, numbers, numbersCount, numbersPacked.length);
     *     }
     *     numbersCount += numbersPacked.length;
     * } else {
     *     if (numbersCount == numbers.length) {
     *         numbers = Arrays.copyOf(numbers, Math.max(8, numbersCount * 2));
     *     }
     *     numbers[numbersCount++] = protoInputStream.readInt32Field();
     * }
     * break;
     * }</pre>
     *
     * @param buffer the name of the buffer
     * @param type   the type of the field
     * @return a {@link CodeBlock} containing the generated deserialization logic
     */
    protected CodeBlock writeBulkDeserializer(String buffer, ProtobufValueFieldType type) {
        var count = buffer + "Count";
        var packed = buffer + "Packed";
        return CodeBlock.builder()
                .beginControlFlow("if ($L.fieldWireType() == $T.WIRE_TYPE_LENGTH_DELIMITED)", INPUT_STREAM_NAME, ProtobufWireType.class)
                .addStatement("var $L = $L.$L", packed, INPUT_STREAM_NAME, getDeserializerStreamMethod(type, true, ProtobufUtf8Validation.NONE))
                .beginControlFlow("if ($L == 0)", count)
                .addStatement("$L = $L", buffer, packed)
                .nextControlFlow("else")
                .addStatement("$L = $T.copyOf($L, $L + $L.length)", buffer, Arrays.class, buffer, count, packed)
                .addStatement("$T.arraycopy($L, 0, $L, $L, $L.length)", System.class, packed, buffer, count, packed)
                .endControlFlow()
                .addStatement("$L += $L.length", count, packed)
                .nextControlFlow("else")
                .add(appendToArray(buffer, "%s.%s".formatted(INPUT_STREAM_NAME, getDeserializerStreamMethod(type, false, ProtobufUtf8Validation.NONE))))
                .endControlFlow()
                .addStatement("break")
                .build();
    }

    /**
     * Returns whether the given type is a repeated field read in bulk.
     *
     * @param type the type of the field
     * @return {@code true} if the type is a collection or an array read in bulk
     */
    protected static boolean isBulk(ProtobufFieldType type) {
        return switch (type) {
            case ProtobufCollectionFieldType collectionType -> collectionType.bulk();
            case ProtobufArrayFieldType arrayType -> arrayType.bulk();
            default -> false;
        };
    }

    /**
     * Returns the name of the buffer that a field read in bulk is read into.
     *
     * <p>A field that needs no converter, because it already is the array of the primitive
     * type of its wire values, is its own buffer. Any other field is converted from its
     * buffer, named after the field with the {@code Bulk} suffix, once every field is read.
     *
     * @param name the name of the field
     * @param type the type of the field
     * @return the name of the buffer
     */
    protected static String getBulkBuffer(String name, ProtobufFieldType type) {
        return type.deserializers().isEmpty() ? name : name + "Bulk";
    }

    /**
     * Returns the statements that append a value to the buffer of an array field, doubling
     * its capacity if it is full.
     *
     * @param name  the name of the array field
     * @param value the expression that provides the value
     * @return the statements
     */
    protected CodeBlock appendToArray(String name, String value) {
        var count = name + "Count";
        return CodeBlock.builder()
                .beginControlFlow("if ($L == $L.length)", count, name)
                .addStatement("$L = $T.copyOf($L, $T.max(8, $L * 2))", name, Arrays.class, name, Math.class, count)
                .endControlFlow()
                .addStatement("$L[$L++] = $L", name, count, value)
                .build();
    }

    /**
     * Generates the statements that convert a wire value with the deserializers of its type,
     * and assign it to its field, or add it to its collection.
     *
     * <p>The value of an enum is first stored in a variable, so that it can be added to the
     * unknown fields if it has no constant.
     *
     * <p>A field that holds the builder of a message, as described by
     * {@link ProtobufFieldType#builderMessage()}, is decoded into that builder, which is
     * created if the field does not hold one yet, so that an occurrence of the message decoded
     * more than once is merged into the previous ones, like the official protobuf library
     * does.
     *
     * @param caseBlock     the block to add the statements to
     * @param name          the name of the field
     * @param index         the index of the field
     * @param type          the type of the field, or of the elements of a repeated field
     * @param value         the expression that provides the wire value
     * @param target        where the value is stored
     * @param unknownFields the name of the variable that holds the unknown fields of the
     *                      message, or {@code null} if the message does not keep them
     */
    protected void writeValueAssignment(CodeBlock.Builder caseBlock, String name, int index, ProtobufValueFieldType type, String value, ValueTarget target, String unknownFields) {
        var builderMessage = target == ValueTarget.FIELD ? type.builderMessage() : Optional.<DeclaredType>empty();
        if (builderMessage.isPresent()) {
            var fieldTarget = getFieldTarget(name);
            var decoder = type.deserializers().getFirst().delegate();
            caseBlock.beginControlFlow("if ($L == null)", fieldTarget);
            caseBlock.addStatement("$L = new $T()", fieldTarget, ProtobufBuilderTypeGenerator.getBuilderClassName(builderMessage.get()));
            caseBlock.endControlFlow();
            caseBlock.addStatement("$L.$L($L, $L)", decoder.receiver().orElse(decoder.ownerName()), PARTIAL_METHOD_NAME, value, fieldTarget);
            return;
        }

        if (type.protobufType() != ProtobufType.ENUM) {
            caseBlock.add(getReadAssignment(name, target, applyDeserializers(value, type, INPUT_STREAM_NAME)));
            return;
        }

        var rawName = name + "Raw";
        var valueName = name + "Value";
        caseBlock.addStatement("var $L = $L", rawName, value);
        caseBlock.addStatement("var $L = $L", valueName, applyDeserializers(rawName, type, INPUT_STREAM_NAME));
        caseBlock.beginControlFlow("if ($L != null)", valueName);
        caseBlock.add(getReadAssignment(name, target, valueName));
        if (unknownFields != null) {
            caseBlock.nextControlFlow("else");
            caseBlock.addStatement("$L.add($L, new $T($L))", unknownFields, index, ProtobufUnknownValue.Binary.VarInt.class, rawName);
        }
        caseBlock.endControlFlow();
    }

    /**
     * Returns the statements that store a deserialized value in its target.
     *
     * @param name         the name of the field
     * @param target       where the value is stored
     * @param readFunction the expression that provides the value
     * @return the statements
     */
    private CodeBlock getReadAssignment(String name, ValueTarget target, String readFunction) {
        return switch (target) {
            case FIELD -> getFieldAssignment(name, readFunction);
            case COLLECTION -> getCollectionAdd(name, readFunction);
            case ARRAY -> appendToArray(name, readFunction);
        };
    }

    /**
     * Where a deserialized value of a field is stored.
     */
    protected enum ValueTarget {
        /**
         * The value is assigned to a field that is not repeated.
         */
        FIELD,

        /**
         * The value is added to the collection of a repeated field.
         */
        COLLECTION,

        /**
         * The value is appended to the buffer of a repeated field held by an array.
         */
        ARRAY
    }

    /**
     * Returns the expression that reads a single wire value of the given type from a stream.
     *
     * <p>A length prefixed message is read as a nested stream, which is decoded by the
     * deserializers of the message.
     *
     * @param streamName     the name of the stream
     * @param type           the type of the value
     * @param utf8Validation the UTF-8 validation strategy for string fields
     * @return the read expression
     */
    private String getReadExpression(String streamName, ProtobufValueFieldType type, ProtobufUtf8Validation utf8Validation) {
        if (type.protobufType() == ProtobufType.MESSAGE) {
            return "%s.readLengthDelimitedField()".formatted(streamName);
        }
        return "%s.%s".formatted(streamName, getDeserializerStreamMethod(type, false, utf8Validation));
    }

    /**
     * Returns the default value of the given wire type, which a field with implicit presence
     * holds when it is not on the wire.
     *
     * <p>A field with implicit presence does not write the default value of its wire type,
     * so a field that is not on the wire holds that value, converted by the deserializers of
     * the field exactly like a value read from the stream. For example, a string field is
     * {@code ""}, an enum field is the constant with index {@code 0}, and an {@code Instant}
     * encoded as milliseconds is the epoch.
     *
     * @param protobufType the wire type of the field
     * @return the expression that provides the default wire value
     * @throws IllegalArgumentException if the type has no default wire value, like a message
     */
    public static String getWireDefaultValue(ProtobufType protobufType) {
        return switch (protobufType) {
            case BOOL -> "false";
            case STRING -> "\"\"";
            case BYTES -> "new byte[0]";
            case FLOAT -> "0f";
            case DOUBLE -> "0d";
            case ENUM, INT32, SINT32, UINT32, FIXED32, SFIXED32 -> "0";
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> "0L";
            default -> throw new IllegalArgumentException("Internal bug: %s fields have no implicit default value".formatted(protobufType));
        };
    }

    /**
     * Applies the deserializers of the given type to a wire value.
     *
     * <p>Each deserializer in the chain wraps the previous expression, producing invocations
     * like {@code MyConverter.fromProto(protoInputStream.readInt64Field())}. A deserializer
     * that declares additional parameters also receives the arguments supplied by the use
     * site, and the stream for each parameter whose type is a reader.
     *
     * @param value      the expression that provides the wire value
     * @param type       the protobuf field type descriptor containing the deserializer chain
     * @param streamName the name of the stream passed to reader parameters
     * @return the converted expression
     */
    protected String applyDeserializers(String value, ProtobufFieldType type, String streamName) {
        for (var deserializer : type.deserializers()) {
            value = deserializer.delegate().invoke(value, deserializer.arguments(), _ -> streamName);
        }
        return value;
    }

    /**
     * Maps a protobuf field type to its corresponding {@code ProtobufBinaryReader} read method name.
     *
     * <p>Packed variants return method names that read a primitive array of elements (e.g.,
     * {@code readPackedInt32Field()} returns an {@code int[]}).
     *
     * @param type           the protobuf field type to map
     * @param packed         {@code true} if the field uses packed encoding
     * @param utf8Validation the UTF-8 validation strategy for string fields
     * @return the stream read method invocation as a string
     * @throws IllegalArgumentException if the type is a message, which has no read method
     */
    private String getDeserializerStreamMethod(ProtobufValueFieldType type, boolean packed, ProtobufUtf8Validation utf8Validation) {
        return switch (type.protobufType()) {
            case STRING -> "readStringField(%s)".formatted(utf8Validation == ProtobufUtf8Validation.VERIFY);
            case MESSAGE -> throw new IllegalArgumentException("Internal bug: message types should not reach getDeserializerStreamMethod");
            case ENUM, INT32 -> packed ? "readPackedInt32Field()" : "readInt32Field()";
            case SINT32 -> packed ? "readPackedSInt32Field()" : "readSInt32Field()";
            case UINT32 -> packed ? "readPackedUInt32Field()" : "readUInt32Field()";
            case BYTES -> "readBytesField()";
            case BOOL -> packed ? "readPackedBoolField()" : "readBoolField()";
            case FLOAT -> packed ? "readPackedFloatField()" : "readFloatField()";
            case DOUBLE -> packed ? "readPackedDoubleField()" : "readDoubleField()";
            case FIXED32 -> packed ? "readPackedFixed32Field()" : "readFixed32Field()";
            case SFIXED32 -> packed ? "readPackedSFixed32Field()" : "readSFixed32Field()";
            case INT64 -> packed ? "readPackedInt64Field()" : "readInt64Field()";
            case SINT64 -> packed ? "readPackedSInt64Field()" : "readSInt64Field()";
            case UINT64 -> packed ? "readPackedUInt64Field()" : "readUInt64Field()";
            case FIXED64 -> packed ? "readPackedFixed64Field()" : "readFixed64Field()";
            case SFIXED64 -> packed ? "readPackedSFixed64Field()" : "readSFixed64Field()";
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code public static} modifiers, as all generated deserialization methods
     * are static factory methods on the Spec class.
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
     * <p>Returns the exceptions resolved by {@link ProtobufExceptionManager#deserializationExceptions(ProtobufTypeElement)}.
     *
     * @return the checked exceptions of the generated deserialization method
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return exceptions.deserializationExceptions(ownerElement);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the statement that wraps the checked exceptions of the converters and of the
     * builder of a message in a {@link ProtobufDeserializationException}, if they throw any,
     * as described by {@link ProtobufExceptionManager#wrapsIncoming(ProtobufTypeElement)}.
     *
     * @return the statement, or an empty {@link Optional} if no exception is wrapped
     */
    @Override
    protected Optional<CodeBlock> wrapperStatement() {
        if (!exceptions.wrapsIncoming(ownerElement)) {
            return Optional.empty();
        }

        return Optional.of(CodeBlock.of("throw $T.converterFailure($S, $L)", ProtobufDeserializationException.class, ProtobufTypeElement.getProtobufName(ownerElement.typeElement()), WRAPPED_EXCEPTION));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@value #METHOD_NAME}, the standard name for generated deserialization methods.
     *
     * @return the method name
     */
    @Override
    protected String name() {
        return METHOD_NAME;
    }
}
