package com.github.auties00.daedalus.protobuf.processor.generator;

import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.palantir.javapoet.*;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufSizeTable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An abstract base class for size calculation method generators that computes the serialized
 * byte size of protobuf values.
 *
 * <p>This class extends {@link ProtobufSerializationGenerator} and provides methods for
 * calculating the encoded sizes of normal, repeated, and map fields, including field tags,
 * varint lengths, and nested message structures. The computed sizes are used during
 * serialization to write length-prefixed message headers.
 *
 * <p>Size calculation accounts for all components of the wire format: field tag bytes,
 * varint-encoded lengths for length-delimited fields, and the actual data bytes for each
 * field value.
 */
public abstract class ProtobufSizeGenerator extends ProtobufSerializationGenerator {
    /**
     * The name of the generated size calculation method.
     */
    public static final String METHOD_NAME = "sizeOfBinary";
    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";
    private static final String OUTPUT_SIZE_NAME = "protoOutputSize";

    /**
     * Constructs a new size generator for the given protobuf type element.
     *
     * @param element the protobuf type element whose fields this generator computes sizes for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufSizeGenerator(ProtobufTypeElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
    }

    /**
     * Generates the helper method name for calculating the size of a single map entry.
     *
     * <p>The returned name follows the pattern {@code sizeOfBinary} + capitalized field name.
     * For example, a field named "scores" produces the method name {@code sizeOfBinaryScores}.
     *
     * @param name the name of the map field
     * @return the generated helper method name
     */
    public static String getMapFieldMethodName(String name) {
        return METHOD_NAME + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Generates code that calculates the serialized byte size of a repeated (collection) field.
     *
     * <p>For packed repeated fields, the generated code computes the length of the payload
     * without allocating, converting the elements to their wire values exactly like
     * {@link #writeRepeatedSerializer} does, and adds the size of the tag, of the length and
     * of the payload. For non-packed repeated fields, the generated code iterates over the
     * collection and accumulates the size of each element including its field tag.
     *
     * <p>For example, given a packed {@code List<Integer>} field named "numbers" at index 2:
     *
     * <pre>{@code
     * var numbersPackedSource = protoInputObject.numbers();
     * if (numbersPackedSource != null && !numbersPackedSource.isEmpty()) {
     *     var numbersPackedLength = 0;
     *     for (var numbersEntry : numbersPackedSource) {
     *         if (numbersEntry != null) {
     *             numbersPackedLength += ProtobufBinarySizeCalculator.getVarInt32Size(numbersEntry);
     *         }
     *     }
     *     if (numbersPackedLength != 0) {
     *         protoOutputSize += ProtobufBinarySizeCalculator.getMessageFieldSize(2, numbersPackedLength);
     *     }
     * }
     * }</pre>
     *
     * <p>For a non-packed {@code List<String>} field named "names":
     *
     * <pre>{@code
     * if (names != null) {
     *     for (var namesEntry : names) {
     *         protoOutputSize += ProtobufBinarySizeCalculator.getStringFieldSize(1, namesEntry);
     *     }
     * }
     * }</pre>
     *
     * @param methodBuilder  the method builder to append the size calculation code to
     * @param index          the protobuf field index
     * @param name           the name of the repeated field
     * @param accessor       the expression that accesses the collection value
     * @param packed         {@code true} if the repeated field uses packed encoding
     * @param delimited      {@code true} if the elements are messages encoded as groups
     * @param collectionType the collection field type descriptor
     * @param cast           {@code true} if a type cast is needed for generic element types
     */
    protected void writeRepeatedSize(MethodSpec.Builder methodBuilder, int index, String name, String accessor, boolean packed, boolean delimited, ProtobufCollectionFieldType collectionType, boolean cast) {
        if (collectionType.bulk()) {
            writeBulkSize(methodBuilder, index, name, accessor, collectionType);
            return;
        }

        writeRepeatedElementsSize(methodBuilder, index, name, accessor, collectionType.valueType(), false, packed, delimited, cast);
    }

    /**
     * Generates code that calculates the serialized byte size of a repeated field held by an
     * array.
     *
     * <p>A packed array that is converted to the array of the primitive type of its wire
     * values, which needs no converter if it already is one, is sized in bulk by the packed
     * variant of the size method. Any other array is sized like a collection.
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the field
     * @param accessor      the expression that accesses the array
     * @param packed        {@code true} if the repeated field uses packed encoding
     * @param delimited     {@code true} if the elements are messages encoded as groups
     * @param arrayType     the array field type descriptor
     */
    protected void writeArraySize(MethodSpec.Builder methodBuilder, int index, String name, String accessor, boolean packed, boolean delimited, ProtobufArrayFieldType arrayType) {
        if (arrayType.bulk()) {
            writeBulkSize(methodBuilder, index, name, accessor, arrayType);
            return;
        }

        writeRepeatedElementsSize(methodBuilder, index, name, accessor, arrayType.valueType(), true, packed, delimited, false);
    }

    /**
     * Generates code that calculates the serialized byte size of a packed field written in
     * bulk, converting its value to the array of the primitive type of its wire values like
     * the serializer does.
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the field
     * @param accessor      the expression that accesses the value of the field
     * @param type          the type of the field, whose converters produce the array
     */
    private void writeBulkSize(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufValueFieldType type) {
        var array = writeBulkSource(methodBuilder, name, accessor, type);
        methodBuilder.addStatement("$L += $T.$L($L, $L)", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, getPackedSizeCalculator(type.protobufType()), index, array);
        for (var i = 0; i <= type.serializers().size(); i++) {
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Generates code that calculates the serialized byte size of the elements of a repeated
     * field held by a collection or by an array that is not sized in bulk.
     *
     * <p>Elements that are, or are converted to, {@code null} are not counted: the serializer
     * rejects them.
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the field
     * @param accessor      the expression that accesses the collection or the array
     * @param valueType     the type of the elements
     * @param array         {@code true} if the elements are held by an array
     * @param packed        {@code true} if the repeated field uses packed encoding
     * @param delimited     {@code true} if the elements are messages encoded as groups
     * @param cast          {@code true} if a type cast is needed for generic element types
     */
    private void writeRepeatedElementsSize(MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufSimpleFieldType valueType, boolean array, boolean packed, boolean delimited, boolean cast) {
        var source = writeRepeatedSource(methodBuilder, name, accessor, array);
        if(packed) {
            // Packed encoding: field_tag + varint(total_length) + all_elements_packed
            var length = writePackedLength(methodBuilder, name, source, valueType, cast, null);
            if (usesSizeTable()) {
                methodBuilder.addStatement("$L.set($L.reserve(), $L)", SIZE_TABLE_PARAMETER, SIZE_TABLE_PARAMETER, length);
            }
            methodBuilder.beginControlFlow("if ($L != 0)", length);
            methodBuilder.addStatement("$L += $T.getMessageFieldSize($L, $L)", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, index, length);
            methodBuilder.endControlFlow();
        } else {
            // Non-packed encoding: iterate and calculate (field_tag + element) for each item
            var repeatedEntryFieldName = name + "Entry";
            methodBuilder.beginControlFlow("for (var $L : $L)", repeatedEntryFieldName, source);
            writeNormalSize(
                    methodBuilder,
                    index,
                    repeatedEntryFieldName,
                    valueType,
                    cast ? valueType.serializedType() : null,
                    null,
                    false,
                    delimited
            );
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Maps a packed repeated field type to its corresponding {@code ProtobufBinarySizeCalculator}
     * method name.
     *
     * <p>Fixed-size types (e.g., {@code FIXED32}, {@code FIXED64}) have known element sizes,
     * while varint-encoded types have variable sizes that depend on the actual values.
     *
     * @param protobufType the type of the elements of the packed field
     * @return the packed size size method name
     * @throws IllegalArgumentException if the type is not a valid packed type
     */
    private String getPackedSizeCalculator(ProtobufType protobufType) {
        return switch (protobufType) {
            case FLOAT -> "getPackedFloatFieldSize";
            case DOUBLE -> "getPackedDoubleFieldSize";
            case BOOL -> "getPackedBoolFieldSize";
            case ENUM, INT32 -> "getPackedInt32FieldSize";
            case SINT32 -> "getPackedSInt32FieldSize";
            case UINT32 -> "getPackedUInt32FieldSize";
            case INT64 -> "getPackedInt64FieldSize";
            case SINT64 -> "getPackedSInt64FieldSize";
            case UINT64 -> "getPackedUInt64FieldSize";
            case FIXED32 -> "getPackedFixed32FieldSize";
            case SFIXED32 -> "getPackedSFixed32FieldSize";
            case FIXED64 -> "getPackedFixed64FieldSize";
            case SFIXED64 -> "getPackedSFixed64FieldSize";
            default ->
                    throw new IllegalArgumentException("Internal bug: unexpected packed type " + protobufType);
        };
    }

    /**
     * Generates code that calculates the serialized byte size of a map field, and queues the
     * generation of a helper method for computing individual map entry sizes.
     *
     * <p>The generated code in the main method stores the map in a local variable, iterates
     * over its entries, and accumulates the size of each entry (including its field tag and
     * length prefix) by delegating to a generated helper method.
     *
     * <p>For example, given a {@code Map<String, Integer>} field named "scores" at index 3:
     *
     * <pre>{@code
     * var scoresMapField = protoInputObject.scores();
     * if (scoresMapField != null) {
     *     for (var scoresMapEntry : scoresMapField.entrySet()) {
     *         var scoresMapEntrySize = sizeOfBinaryScores(scoresMapEntry);
     *         protoOutputSize += ProtobufBinarySizeCalculator.getMessageFieldSize(3, scoresMapEntrySize);
     *     }
     * }
     * }</pre>
     *
     * <p>A private helper method (e.g., {@code sizeOfBinaryScores}) is generated via a deferred
     * operation to calculate the size of a single map entry.
     *
     * @param classBuilder  the class builder to add the helper method to
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the map field
     * @param accessor      the expression that accesses the map value
     * @param mapType       the map field type descriptor containing key and value type information
     * @param cast          {@code true} if a type cast is needed for generic map entry types
     */
    protected void writeMapSize(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder, int index, String name, String accessor, ProtobufMapFieldType mapType, boolean cast) {
        // Store map in local variable
        var mapFieldName = name + "MapField";
        methodBuilder.addStatement("var $L = $L", mapFieldName, accessor);

        // Queue generation of the helper method (will be created after main method completes)
        var methodName = getMapFieldMethodName(name);
        deferredOperations.add(() -> writeMapEntryFieldSizeMethod(classBuilder, name, mapType, methodName, cast));

        // Iterate over map entries
        methodBuilder.beginControlFlow("if ($L != null)", mapFieldName);
        var mapEntryFieldName = name + "MapEntry";
        methodBuilder.beginControlFlow("for (var $L : $L.entrySet())", mapEntryFieldName, mapFieldName);

        // Field tag size for the map entry (always length-delimited)
        // Calculate size of this entry using helper method
        var mapEntrySizeFieldName = mapEntryFieldName + "Size";
        if (usesSizeTable()) {
            // The slot of the entry is reserved before the ones of the message that its value may hold
            methodBuilder.addStatement("var $L = $L.set($L.reserve(), $L($L$L, $L))", mapEntrySizeFieldName, SIZE_TABLE_PARAMETER, SIZE_TABLE_PARAMETER, methodName, cast ? "(java.util.Map.Entry) " : "", mapEntryFieldName, SIZE_TABLE_PARAMETER);
        } else {
            methodBuilder.addStatement("var $L = $L($L$L)", mapEntrySizeFieldName, methodName, cast ? "(java.util.Map.Entry) " : "", mapEntryFieldName);
        }

        // Add tag + varint(entry_length) + entry content
        methodBuilder.addStatement("$L += $T.getMessageFieldSize($L, $L)", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, index, mapEntrySizeFieldName);
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
    }

    /**
     * Generates a private helper method that calculates the serialized byte size of a single
     * map entry.
     *
     * <p>The generated method accepts a {@link java.util.Map.Entry} parameter and returns the
     * total size of the key (at field index 1) and value (at field index 2) including their
     * respective field tags.
     *
     * <p>For example, given a {@code Map<String, Integer>} field named "scores":
     *
     * <pre>{@code
     * private static int sizeOfBinaryScores(Map.Entry<String, Integer> protoInputObject) {
     *     var protoOutputSize = 0;
     *     protoOutputSize += ProtobufBinarySizeCalculator.getStringFieldSize(1, protoInputObject.getKey());
     *     var scoresMapValue = protoInputObject.getValue();
     *     protoOutputSize += ProtobufBinarySizeCalculator.getInt32FieldSize(2, scoresMapValue);
     *     return protoOutputSize;
     * }
     * }</pre>
     *
     * @param classBuilder the class builder to add the generated method to
     * @param name         the name of the map field
     * @param mapType      the map field type descriptor containing key and value type information
     * @param methodName   the name for the generated helper method
     * @param cast         {@code true} if type casts are needed for generic key and value types
     */
    private void writeMapEntryFieldSizeMethod(TypeSpec.Builder classBuilder, String name, ProtobufMapFieldType mapType, String methodName, boolean cast) {
        // Build parameter type: Map.Entry<KeyType, ValueType>
        var keyTypeName = getMapEntryTypeArgument(mapType.keyType().accessorType());
        var valueTypeName = getMapEntryTypeArgument(mapType.valueType().accessorType());

        // Create the helper method signature
        var mapEntryMethodBuilder = MethodSpec.methodBuilder(methodName);
        mapEntryMethodBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        mapEntryMethodBuilder.returns(TypeName.INT);
        mapEntryMethodBuilder.addParameter(ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(Map.Entry.class), keyTypeName, valueTypeName),
                INPUT_OBJECT_PARAMETER
        ).build());
        if (usesSizeTable()) {
            mapEntryMethodBuilder.addParameter(ParameterSpec.builder(ProtobufSizeTable.class, SIZE_TABLE_PARAMETER).build());
        }

        instrumentWrapped(mapEntryMethodBuilder, () -> {
            // Initialize size accumulator
            mapEntryMethodBuilder.addStatement("var $L = 0", OUTPUT_SIZE_NAME);

            // Calculate key size (field index 1 in map entry message)
            writeNormalSize(
                    mapEntryMethodBuilder,
                    1,
                    name + "MapKey",
                    mapType.keyType(),
                    cast ? mapType.keyType().serializedType() : null,
                    INPUT_OBJECT_PARAMETER + ".getKey()",
                    false,
                    false
            );

            // Calculate value size (field index 2 in map entry message)
            writeNormalSize(
                    mapEntryMethodBuilder,
                    2,
                    name + "MapValue",
                    mapType.valueType(),
                    cast ? mapType.valueType().serializedType() : null,
                    INPUT_OBJECT_PARAMETER + ".getValue()",
                    false,
                    false
            );

            // Return total size
            mapEntryMethodBuilder.addStatement("return $L", OUTPUT_SIZE_NAME);
        });
        mapEntryMethodBuilder.addExceptions(thrownTypes());
        classBuilder.addMethod(mapEntryMethodBuilder.build());
    }

    /**
     * Returns the type argument of the {@code Map.Entry} parameter of a map entry size method
     * for the given key or value type.
     *
     * <p>Declared types are erased to their qualified name, while arrays, such as the
     * {@code byte[]} of a bytes value, keep their component type.
     *
     * @param type the key or value type
     * @return the type argument
     */
    private TypeName getMapEntryTypeArgument(TypeMirror type) {
        if (type instanceof DeclaredType) {
            return ClassName.bestGuess(getQualifiedName(type));
        }
        return TypeName.get(type).box();
    }

    /**
     * Generates code that calculates the serialized byte size of a normal (non-repeated,
     * non-map) field.
     *
     * <p>This method delegates to
     * {@link ProtobufSerializationGenerator#writeCustomSerializer} to handle custom serializer
     * chains, null checks, and type casting. The final size calculation is routed to either an
     * object size (for {@code MESSAGE}, {@code ENUM}, and {@code GROUP} types) or a
     * primitive size (for strings, numbers, bytes, and booleans).
     *
     * <p>For example, given a {@code String} field named "name" at index 1:
     *
     * <pre>{@code
     * var name = protoInputObject.name();
     * if (name != null) {
     *     protoOutputSize += ProtobufBinarySizeCalculator.getStringFieldSize(1, name);
     * }
     * }</pre>
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the field
     * @param normalType    the simple field type descriptor
     * @param castType      the type to cast to, or {@code null} if no cast is needed
     * @param accessorCall  the expression that accesses the field value, or {@code null} to
     *                      use the field name directly
     * @param implicit      {@code true} if the field has implicit presence, so that its
     *                      default value is not counted
     * @param delimited     {@code true} if the field is a message encoded as a group
     */
    protected void writeNormalSize(MethodSpec.Builder methodBuilder, int index, String name, ProtobufSimpleFieldType normalType, TypeMirror castType, String accessorCall, boolean implicit, boolean delimited) {
        writeCustomSerializer(
                methodBuilder,
                index,
                name,
                accessorCall == null ? name : accessorCall,
                normalType,
                accessorCall != null, // declareVariable
                true, // variableNullCheck
                castType != null, // cast
                implicit,
                delimited,
                null, // A value that is not set is not counted
                // Handler for messages
                (nestedBuilder, serializedName, _) -> writeObjectCalculator(
                        nestedBuilder,
                        index,
                        name,
                        normalType,
                        serializedName,
                        delimited
                ),
                // Handler for primitive types
                (nestedBuilder, serializedName, _) -> writePrimitiveCalculator(
                        nestedBuilder,
                        index,
                        normalType.protobufType(),
                        castType,
                        serializedName
                )
        );
    }

    /**
     * Generates code that calculates the serialized byte size of a message.
     *
     * <p>The size of the message is computed by the sizer attached to its last serializer:
     * <ul>
     * <li>A length prefixed message takes its tag, the varint of its length and its body.
     * <li>A message encoded as a group takes its start tag, its body and its end tag.
     * </ul>
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param name          the name of the field
     * @param type          the simple field type descriptor
     * @param accessor      the expression that provides the field value
     * @param delimited     {@code true} if the message is encoded as a group
     * @throws IllegalArgumentException if the type is not {@code MESSAGE}
     */
    private void writeObjectCalculator(MethodSpec.Builder methodBuilder, int index, String name, ProtobufSimpleFieldType type, String accessor, boolean delimited) {
        if (type.protobufType() != ProtobufType.MESSAGE) {
            throw new IllegalArgumentException("Internal bug: %s property types should not reach writeObjectCalculator".formatted(type.protobufType().name()));
        }

        var lastSerializer = type.serializers().getLast();
        var serializedObjectFieldName = name + "SerializedSize";
        var specReceiver = usesSizeTable() ? getSpecReceiver(lastSerializer) : Optional.<String>empty();
        if (specReceiver.isEmpty()) {
            methodBuilder.addStatement("var $L = $L", serializedObjectFieldName, createSizerInvocation(lastSerializer, accessor));
        } else if (delimited) {
            // A group has no length, but the messages nested in it do
            methodBuilder.addStatement("var $L = $L.$L($L, $L)", serializedObjectFieldName, specReceiver.get(), METHOD_NAME, accessor, SIZE_TABLE_PARAMETER);
        } else {
            // The slot of the message is reserved before the ones of the messages nested in it
            methodBuilder.addStatement("var $L = $L.set($L.reserve(), $L.$L($L, $L))", serializedObjectFieldName, SIZE_TABLE_PARAMETER, SIZE_TABLE_PARAMETER, specReceiver.get(), METHOD_NAME, accessor, SIZE_TABLE_PARAMETER);
        }
        if (delimited) {
            methodBuilder.addStatement("$L += (int) ($T.getStartGroupFieldSize($L) + $T.getEndGroupFieldSize($L)) + $L", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, index, ProtobufBinarySizeCalculator.class, index, serializedObjectFieldName);
        } else {
            methodBuilder.addStatement("$L += $T.getMessageFieldSize($L, $L)", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, index, serializedObjectFieldName);
        }
    }

    /**
     * Generates code that calculates the serialized byte size for primitive types including
     * strings, numbers, bytes, and booleans.
     *
     * <p>The generated code uses combined field size methods from
     * {@code ProtobufBinarySizeCalculator} that include both the field tag size and the data
     * size in a single call.
     *
     * @param methodBuilder the method builder to append the size calculation code to
     * @param index         the protobuf field index
     * @param protobufType  the protobuf type of the field
     * @param castType      the type to cast to, or {@code null} if no cast is needed
     * @param accessor      the expression that provides the field value
     * @throws IllegalArgumentException if the type is not a valid primitive protobuf type
     */
    private void writePrimitiveCalculator(MethodSpec.Builder methodBuilder, int index, ProtobufType protobufType, TypeMirror castType, String accessor) {
        var cast = castType != null ? "(" + castType + ") " : "";
        var sizeMethod = switch (protobufType) {
            case BOOL -> "getBoolFieldSize";
            case STRING -> "getStringFieldSize";
            case BYTES -> "getBytesFieldSize";
            case ENUM, INT32 -> "getInt32FieldSize";
            case SINT32 -> "getSInt32FieldSize";
            case UINT32 -> "getUInt32FieldSize";
            case INT64 -> "getInt64FieldSize";
            case SINT64 -> "getSInt64FieldSize";
            case UINT64 -> "getUInt64FieldSize";
            case FLOAT -> "getFloatFieldSize";
            case DOUBLE -> "getDoubleFieldSize";
            case FIXED32 -> "getFixed32FieldSize";
            case SFIXED32 -> "getSFixed32FieldSize";
            case FIXED64 -> "getFixed64FieldSize";
            case SFIXED64 -> "getSFixed64FieldSize";
            default -> throw new IllegalArgumentException("Internal bug: %s property types should not reach writePrimitiveCalculator".formatted(protobufType.name()));
        };
        methodBuilder.addStatement("$L += $T.$L($L, $L$L)", OUTPUT_SIZE_NAME, ProtobufBinarySizeCalculator.class, sizeMethod, index, cast, accessor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true}, as size calculation methods require instrumentation to track
     * the accumulated byte size.
     *
     * @return {@code true}
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the exceptions resolved by {@link ProtobufExceptionManager#sizeExceptions(ProtobufTypeElement)}:
     * size calculation methods never write the wire format, so they only throw the checked
     * exceptions of the converters they invoke.
     *
     * @return the checked exceptions of the generated size calculation method
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return exceptions.sizeExceptions(ownerElement);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code int}, as size calculation methods return the total serialized byte
     * count.
     *
     * @return {@link TypeName#INT}
     */
    @Override
    protected TypeName returnType() {
        return TypeName.INT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@value #METHOD_NAME}, the standard name for generated size calculation
     * methods.
     *
     * @return the method name
     */
    @Override
    public String name() {
        return METHOD_NAME;
    }
}
