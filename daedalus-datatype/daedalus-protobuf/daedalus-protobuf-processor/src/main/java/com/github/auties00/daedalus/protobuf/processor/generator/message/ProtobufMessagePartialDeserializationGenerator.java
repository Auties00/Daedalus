package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.model.ProtobufMessageEncoding;
import com.github.auties00.daedalus.protobuf.model.ProtobufUtf8Validation;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufDeserializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.generator.builder.ProtobufBuilderTypeGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A deserialization method generator that produces the
 * {@code decodePartial(ProtobufBinaryReader, MessageBuilder)} method of protobuf message
 * types, which decodes the fields of a message into its builder.
 *
 * <p>The generated method reads field tags from a {@link ProtobufBinaryReader}, and switches on
 * field indexes to store each field in the builder, merging it with the values that the
 * builder already holds, like {@code mergeFrom} in the official protobuf library:
 * <ul>
 * <li>a field that is not repeated replaces the previous value;
 * <li>a field that holds the builder of a message is decoded into that builder, so that the
 *     message is merged recursively;
 * <li>a repeated or map field is appended to, by adding to its collection or map if it can be
 *     modified, or to a copy of it otherwise, so that a collection or map that cannot be
 *     modified is never changed;
 * <li>a repeated field held by an array, or read in bulk, is read into a buffer, which is
 *     appended to the previous value of the field once every field is read;
 * <li>an unknown field is added to the unknown fields of the builder.
 * </ul>
 *
 * <p>The required fields are not checked, as the builder can be decoded into again: the
 * {@code decode} method checks them when it builds the message.
 *
 * <p>The field reading loop ends when the stream is finished, or when the end tag of a
 * group is read: a message encoded as a group is decoded from the stream of the message
 * that holds it, which then checks the end tag.
 */
public class ProtobufMessagePartialDeserializationGenerator extends ProtobufDeserializationGenerator {
    /**
     * The name of the parameter that holds the reader.
     */
    private static final String INPUT_STREAM_NAME = "protoInputStream";

    /**
     * The name of the parameter that holds the builder that the fields are decoded into.
     */
    private static final String BUILDER_NAME = "protoBuilder";

    /**
     * The name of the field of the builder that holds the unknown fields of the message.
     */
    protected static final String UNKNOWN_FIELDS_NAME = ProtobufBuilderTypeGenerator.UNKNOWN_FIELDS_NAME;

    /**
     * The name of the local variable that holds the index of the field being read.
     */
    private static final String FIELD_INDEX_VARIABLE = "protoFieldIndex";

    /**
     * The protobuf message element whose partial deserialization method is being generated.
     */
    protected final ProtobufMessageElement messageElement;

    /**
     * The case of a oneof whose deserializer is being generated, or {@code null} if the
     * deserializer of a field is being generated.
     */
    protected ProtobufFieldElement currentOneOfCase;

    /**
     * Constructs a new partial deserialization generator for the given message element.
     *
     * @param element the protobuf message element to generate a partial deserialization method for
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessagePartialDeserializationGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.messageElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the buffers of the repeated fields held by arrays or read in bulk, the
     * field reading loop, and the statements that append the buffers to the fields of the
     * builder. Also adds to the Spec class the methods that add an element to the collection,
     * or put an entry in the map, of each repeated or map field.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        for (var property : messageElement.fields()) {
            if (property.isSynthetic()) {
                continue;
            }

            if (isBuffered(property)) {
                var buffer = getBuffer(property);
                methodBuilder.addStatement("$T $L = $L", getBufferType(property), buffer, getBufferDefaultValue(property));
                methodBuilder.addStatement("int $LCount = 0", buffer);
            }

            switch (property.type()) {
                case ProtobufCollectionFieldType collectionType when !collectionType.bulk() -> classBuilder.addMethod(createCollectionAdd(property.name(), collectionType));
                case ProtobufMapFieldType mapType -> classBuilder.addMethod(createMapPut(property.name(), mapType));
                default -> {}
            }
        }

        var unknownFields = messageElement.unknownFieldsElement().isPresent() ? getFieldTarget(UNKNOWN_FIELDS_NAME) : null;
        methodBuilder.beginControlFlow("while ($L.nextFieldTag())", INPUT_STREAM_NAME);
        methodBuilder.addStatement("var $L = $L.fieldIndex()", FIELD_INDEX_VARIABLE, INPUT_STREAM_NAME);

        var switchCases = new ArrayList<CodeBlock>();
        var switchIndexes = new ArrayList<String>();
        for (var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                for (var oneOfCase : oneOf.cases()) {
                    switchIndexes.add(String.valueOf(oneOfCase.index()));
                    switchCases.add(writeOneOfCaseDeserializer(oneOf, oneOfCase, unknownFields));
                }
                continue;
            }

            switchIndexes.add(String.valueOf(property.index()));

            // A synthetic field is computed from the other fields, so its value is skipped instead of being kept as unknown
            if (property.isSynthetic()) {
                switchCases.add(CodeBlock.builder()
                        .addStatement("$L.skipUnknownField()", INPUT_STREAM_NAME)
                        .addStatement("break")
                        .build());
                continue;
            }

            var branch = switch (property.type()) {
                case ProtobufMapFieldType mapType -> writeMapDeserializer(property.name(), mapType, getUtf8Validation(property));
                case ProtobufArrayFieldType arrayType -> writeArrayDeserializer(property.name(), property.index(), arrayType, getUtf8Validation(property), getMessageEncoding(property), unknownFields);
                case ProtobufCollectionFieldType collectionType when collectionType.bulk() -> writeBulkDeserializer(getBulkBuffer(property.name(), collectionType), collectionType);
                case ProtobufCollectionFieldType collectionType -> writeDeserializer(property.name(), property.index(), collectionType.valueType(), true, getUtf8Validation(property), getMessageEncoding(property), unknownFields);
                case ProtobufSimpleFieldType simpleType -> writeDeserializer(property.name(), property.index(), simpleType, false, getUtf8Validation(property), getMessageEncoding(property), unknownFields);
                case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled before the switch");
            };
            switchCases.add(branch);
        }

        switchIndexes.add("default");
        switchCases.add(writeDefaultPropertyDeserializer());

        methodBuilder.beginControlFlow("switch ($L)", FIELD_INDEX_VARIABLE);
        for (var i = 0; i < switchIndexes.size(); i++) {
            var label = switchIndexes.get(i);
            var caseLine = label.equals("default") ? CodeBlock.of("default:\n") : CodeBlock.of("case $L:\n", label);
            var caseLabel = CodeBlock.builder()
                    .add(caseLine)
                    .indent()
                    .add(switchCases.get(i))
                    .unindent()
                    .build();
            methodBuilder.addCode(caseLabel);
        }
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        for (var property : messageElement.fields()) {
            if (!property.isSynthetic() && isBuffered(property)) {
                writeBufferFinalizer(methodBuilder, property);
            }
        }
    }

    /**
     * Generates a switch case block that deserializes a case of the given oneof into the
     * field of the builder that holds the oneof.
     *
     * <p>A case of a synthetic oneof is skipped. A message case that the builder holds as a
     * builder, as described by {@link com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType#builderMessage()},
     * is decoded into the builder of the message held by the oneof if it is the same case, so
     * that a message case that appears more than once is merged, and into a new builder
     * otherwise, which replaces the case that was set. Any other case replaces the value of
     * the oneof. The statements are enclosed in a block, so that the variables of different
     * cases of the same oneof do not clash. For example, given a oneof named "shape" whose
     * message case with index 1 holds {@code Circle}:
     *
     * <pre>{@code
     * {
     *     if (protoBuilder.protoShapeCase != 1) {
     *         protoBuilder.shape = new CircleBuilder();
     *         protoBuilder.protoShapeCase = 1;
     *     }
     *     CircleSpec.global().decodePartial(protoInputStream.readLengthDelimitedField(), (CircleBuilder) protoBuilder.shape);
     *     break;
     * }
     * }</pre>
     *
     * <p>Like a setter of the builder, every case records its index in the
     * {@linkplain ProtobufBuilderTypeGenerator#getOneOfCaseField(String) case field} of the
     * oneof, as two cases may hold the builder of the same message.
     *
     * @param oneOf the oneof
     * @param oneOfCase the case of the oneof
     * @param unknownFields the name of the variable that holds the unknown fields of the
     *        message, or {@code null} if the message does not keep them
     * @return a {@link CodeBlock} containing the generated deserialization logic
     */
    private CodeBlock writeOneOfCaseDeserializer(ProtobufFieldElement.OneOf oneOf, ProtobufFieldElement oneOfCase, String unknownFields) {
        var caseBlock = CodeBlock.builder().add("{\n").indent();
        var builderMessage = oneOfCase.type().builderMessage();
        if (oneOf.isSynthetic()) {
            caseBlock.addStatement("$L.skipUnknownField()", INPUT_STREAM_NAME);
            caseBlock.addStatement("break");
        } else if (builderMessage.isPresent()) {
            var target = getFieldTarget(oneOf.name());
            var caseTarget = getFieldTarget(ProtobufBuilderTypeGenerator.getOneOfCaseField(oneOf.name()));
            var builderType = ProtobufBuilderTypeGenerator.getBuilderClassName(builderMessage.get());
            var decoder = oneOfCase.type().deserializers().getFirst().delegate();
            caseBlock.beginControlFlow("if ($L != $L)", caseTarget, oneOfCase.index());
            caseBlock.addStatement("$L = new $T()", target, builderType);
            caseBlock.addStatement("$L = $L", caseTarget, oneOfCase.index());
            caseBlock.endControlFlow();
            if (oneOfCase.delimited()) {
                caseBlock.addStatement("$L.readStartGroupField($L)", INPUT_STREAM_NAME, oneOfCase.index());
                caseBlock.addStatement("$L.$L($L, ($T) $L)", decoder.receiver().orElse(decoder.ownerName()), PARTIAL_METHOD_NAME, INPUT_STREAM_NAME, builderType, target);
                caseBlock.addStatement("$L.readEndGroupField($L)", INPUT_STREAM_NAME, oneOfCase.index());
            } else {
                caseBlock.addStatement("$L.$L($L.readLengthDelimitedField(), ($T) $L)", decoder.receiver().orElse(decoder.ownerName()), PARTIAL_METHOD_NAME, INPUT_STREAM_NAME, builderType, target);
            }
            caseBlock.addStatement("break");
        } else {
            currentOneOfCase = oneOfCase;
            caseBlock.add(writeDeserializer(oneOf.name(), oneOfCase.index(), (ProtobufValueFieldType) oneOfCase.type(), false, getUtf8Validation(oneOfCase), getMessageEncoding(oneOfCase), unknownFields));
            currentOneOfCase = null;
        }
        return caseBlock.unindent().add("}\n").build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value of a case of a oneof also records the index of the case in the
     * {@linkplain ProtobufBuilderTypeGenerator#getOneOfCaseField(String) case field} of the
     * oneof.
     */
    @Override
    protected CodeBlock getFieldAssignment(String name, String value) {
        if (currentOneOfCase == null) {
            return super.getFieldAssignment(name, value);
        }

        return CodeBlock.builder()
                .addStatement("$L = $L", getFieldTarget(name), value)
                .addStatement("$L = $L", getFieldTarget(ProtobufBuilderTypeGenerator.getOneOfCaseField(name)), currentOneOfCase.index())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @return the field of the builder with the given name
     */
    @Override
    protected String getFieldTarget(String name) {
        return BUILDER_NAME + "." + name;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the element through the method generated by
     * {@link #createCollectionAdd(String, ProtobufCollectionFieldType)}, which copies the
     * collection if it cannot be modified.
     */
    @Override
    protected CodeBlock getCollectionAdd(String name, String value) {
        var target = getFieldTarget(name);
        return CodeBlock.builder()
                .addStatement("$L = $L($L, $L)", target, getAppenderName("add", name), target, value)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Puts the entry through the method generated by
     * {@link #createMapPut(String, ProtobufMapFieldType)}, which copies the map if it cannot be
     * modified.
     */
    @Override
    protected CodeBlock getMapPut(String name, ProtobufMapFieldType mapType, String key, String value) {
        var target = getFieldTarget(name);
        return CodeBlock.builder()
                .addStatement("$L = $L($L, $L, $L)", target, getAppenderName("put", name), target, key, value)
                .build();
    }

    /**
     * Returns the name of the method of the Spec class that appends to the given field.
     *
     * @param prefix the verb of the method, like {@code add} or {@code put}
     * @param name the name of the field
     * @return the name of the method
     */
    private static String getAppenderName(String prefix, String name) {
        return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Creates the method that adds an element to the collection of the given repeated field,
     * and returns the collection that holds it.
     *
     * <p>A {@code null} collection is replaced by a new one, and a collection that cannot be
     * modified, which throws an {@link UnsupportedOperationException}, is copied into a new
     * one, so that the collection set by the user is never modified if it cannot be. For
     * example, given a {@code List<Integer>} field named "numbers":
     *
     * <pre>{@code
     * private static List<Integer> addNumbers(List<Integer> collection, Integer value) {
     *     if (collection == null) {
     *         List<Integer> result = new ArrayList<>();
     *         result.add(value);
     *         return result;
     *     }
     *     try {
     *         collection.add(value);
     *         return collection;
     *     } catch (UnsupportedOperationException exception) {
     *         List<Integer> result = new ArrayList<>();
     *         result.addAll(collection);
     *         result.add(value);
     *         return result;
     *     }
     * }
     * }</pre>
     *
     * @param name the name of the field
     * @param collectionType the type of the field
     * @return the method
     */
    private MethodSpec createCollectionAdd(String name, ProtobufCollectionFieldType collectionType) {
        var collectionTypeName = TypeName.get(collectionType.descriptorElementType());
        var defaultValue = collectionType.descriptorDefaultValue();
        return MethodSpec.methodBuilder(getAppenderName("add", name))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(collectionTypeName)
                .addParameter(collectionTypeName, "collection")
                .addParameter(TypeName.get(collectionType.valueType().descriptorElementType()), "value")
                .beginControlFlow("if (collection == null)")
                .addStatement("$T result = $L", collectionTypeName, defaultValue)
                .addStatement("result.add(value)")
                .addStatement("return result")
                .endControlFlow()
                .beginControlFlow("try")
                .addStatement("collection.add(value)")
                .addStatement("return collection")
                .nextControlFlow("catch ($T exception)", UnsupportedOperationException.class)
                .addStatement("$T result = $L", collectionTypeName, defaultValue)
                .addStatement("result.addAll(collection)")
                .addStatement("result.add(value)")
                .addStatement("return result")
                .endControlFlow()
                .build();
    }

    /**
     * Creates the method that puts an entry in the map of the given map field, and returns
     * the map that holds it.
     *
     * <p>Like {@link #createCollectionAdd(String, ProtobufCollectionFieldType)}, a
     * {@code null} map is replaced by a new one, and a map that cannot be modified is copied
     * into a new one. An entry whose key is already in the map replaces the previous one, like
     * in the official protobuf library.
     *
     * @param name the name of the field
     * @param mapType the type of the field
     * @return the method
     */
    private MethodSpec createMapPut(String name, ProtobufMapFieldType mapType) {
        var mapTypeName = TypeName.get(mapType.descriptorElementType());
        var defaultValue = mapType.descriptorDefaultValue();
        return MethodSpec.methodBuilder(getAppenderName("put", name))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(mapTypeName)
                .addParameter(mapTypeName, "map")
                .addParameter(TypeName.get(mapType.keyType().descriptorElementType()), "key")
                .addParameter(TypeName.get(mapType.valueType().descriptorElementType()), "value")
                .beginControlFlow("if (map == null)")
                .addStatement("$T result = $L", mapTypeName, defaultValue)
                .addStatement("result.put(key, value)")
                .addStatement("return result")
                .endControlFlow()
                .beginControlFlow("try")
                .addStatement("map.put(key, value)")
                .addStatement("return map")
                .nextControlFlow("catch ($T exception)", UnsupportedOperationException.class)
                .addStatement("$T result = $L", mapTypeName, defaultValue)
                .addStatement("result.putAll(map)")
                .addStatement("result.put(key, value)")
                .addStatement("return result")
                .endControlFlow()
                .build();
    }

    /**
     * Returns whether the given field is read into a buffer, because it is held by an array or
     * read in bulk.
     *
     * @param property the field
     * @return {@code true} if the field is read into a buffer
     */
    protected static boolean isBuffered(ProtobufFieldElement property) {
        return property.type() instanceof ProtobufArrayFieldType || isBulk(property.type());
    }

    /**
     * Returns the name of the buffer that the given field is read into.
     *
     * @param property the field, which must be {@linkplain #isBuffered(ProtobufFieldElement) buffered}
     * @return the name of the buffer
     */
    protected static String getBuffer(ProtobufFieldElement property) {
        return isBulk(property.type()) ? getBulkBuffer(property.name(), property.type()) : property.name();
    }

    /**
     * Returns whether the buffer of the given field is converted to the type of the field by
     * its deserializers, which is the case of a field read in bulk whose type is not the array
     * of the primitive type of its wire values.
     *
     * @param property the field, which must be {@linkplain #isBuffered(ProtobufFieldElement) buffered}
     * @return {@code true} if the buffer is converted
     */
    private static boolean isConvertedBuffer(ProtobufFieldElement property) {
        return !getBuffer(property).equals(property.name());
    }

    /**
     * Returns the type of the buffer of the given field.
     *
     * @param property the field, which must be {@linkplain #isBuffered(ProtobufFieldElement) buffered}
     * @return the array of the primitive type of the wire values for a converted buffer, and
     *         the type of the field otherwise
     */
    protected static TypeName getBufferType(ProtobufFieldElement property) {
        if (!isConvertedBuffer(property)) {
            return TypeName.get(property.type().descriptorElementType());
        }

        var component = ProtobufArrayFieldType.getBulkComponentType(((ProtobufValueFieldType) property.type()).protobufType());
        return ArrayTypeName.of(switch (component) {
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            case "boolean" -> TypeName.BOOLEAN;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            default -> throw new IllegalStateException("Internal bug: %s fields cannot be read in bulk".formatted(((ProtobufValueFieldType) property.type()).protobufType()));
        });
    }

    /**
     * Returns the expression that creates the empty buffer of the given field.
     *
     * @param property the field, which must be {@linkplain #isBuffered(ProtobufFieldElement) buffered}
     * @return the expression that creates the empty buffer
     */
    protected static String getBufferDefaultValue(ProtobufFieldElement property) {
        if (!isConvertedBuffer(property)) {
            return property.type().descriptorDefaultValue();
        }

        return "new %s[0]".formatted(ProtobufArrayFieldType.getBulkComponentType(((ProtobufValueFieldType) property.type()).protobufType()));
    }

    /**
     * Generates the statements that append the elements read into the buffer of the given
     * field to the value of the field held by the builder, once every field is read.
     *
     * <p>Nothing is done if no element was read. Otherwise, the previous value of the field,
     * converted back to the type of the buffer by the serializers of the field if the buffer
     * is converted, is copied before the elements that were read. The buffer is trimmed to its
     * number of elements, which only copies it if its capacity exceeds its number of
     * elements, and a converted buffer is converted to the type of the field by its
     * deserializers. For example, given a buffer named "numbersBulk" of a {@code List<Integer>}
     * field named "numbers":
     *
     * <pre>{@code
     * if (numbersBulkCount != 0) {
     *     int[] numbersBulkPrevious = protoBuilder.numbers == null ? null : IntListMixin.toArray(protoBuilder.numbers);
     *     if (numbersBulkPrevious != null && numbersBulkPrevious.length != 0) {
     *         int[] numbersBulkMerged = Arrays.copyOf(numbersBulkPrevious, numbersBulkPrevious.length + numbersBulkCount);
     *         System.arraycopy(numbersBulk, 0, numbersBulkMerged, numbersBulkPrevious.length, numbersBulkCount);
     *         numbersBulk = numbersBulkMerged;
     *     } else if (numbersBulkCount != numbersBulk.length) {
     *         numbersBulk = Arrays.copyOf(numbersBulk, numbersBulkCount);
     *     }
     *     protoBuilder.numbers = IntListMixin.ofArray(numbersBulk);
     * }
     * }</pre>
     *
     * @param methodBuilder the method builder to append the statements to
     * @param property the field, which must be {@linkplain #isBuffered(ProtobufFieldElement) buffered}
     */
    protected void writeBufferFinalizer(MethodSpec.Builder methodBuilder, ProtobufFieldElement property) {
        var buffer = getBuffer(property);
        var bufferType = getBufferType(property);
        var count = buffer + "Count";
        var previous = buffer + "Previous";
        var merged = buffer + "Merged";
        var target = getFieldTarget(property.name());
        var converted = isConvertedBuffer(property);
        methodBuilder.beginControlFlow("if ($L != 0)", count);
        if (converted) {
            var previousValue = target;
            for (var serializer : property.type().serializers()) {
                previousValue = serializer.delegate().invoke(previousValue, serializer.arguments(), _ -> "null");
            }
            methodBuilder.addStatement("$T $L = $L == null ? null : $L", bufferType, previous, target, previousValue);
        } else {
            methodBuilder.addStatement("$T $L = $L", bufferType, previous, target);
        }
        methodBuilder.beginControlFlow("if ($L != null && $L.length != 0)", previous, previous);
        methodBuilder.addStatement("$T $L = $T.copyOf($L, $L.length + $L)", bufferType, merged, Arrays.class, previous, previous, count);
        methodBuilder.addStatement("$T.arraycopy($L, 0, $L, $L.length, $L)", System.class, buffer, merged, previous, count);
        methodBuilder.addStatement("$L = $L", buffer, merged);
        methodBuilder.nextControlFlow("else if ($L != $L.length)", count, buffer);
        methodBuilder.addStatement("$L = $T.copyOf($L, $L)", buffer, Arrays.class, buffer, count);
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("$L = $L", target, converted ? applyDeserializers(buffer, property.type(), INPUT_STREAM_NAME) : buffer);
        methodBuilder.endControlFlow();
    }

    /**
     * Generates the default switch case block for unknown field indexes.
     *
     * <p>If the message declares an unknown fields element, the generated code reads the
     * unknown field and adds it to the unknown fields of the builder via the canonical
     * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields#add}
     * method. Otherwise the unknown field is skipped.
     *
     * @return a {@link CodeBlock} containing the default case deserialization logic
     */
    private CodeBlock writeDefaultPropertyDeserializer() {
        var caseBlock = CodeBlock.builder();
        if (messageElement.unknownFieldsElement().isEmpty()) {
            caseBlock.addStatement("$L.skipUnknownField()", INPUT_STREAM_NAME);
        } else {
            caseBlock.addStatement("$L.add($L, $L.readUnknownField())", getFieldTarget(UNKNOWN_FIELDS_NAME), FIELD_INDEX_VARIABLE, INPUT_STREAM_NAME);
        }
        caseBlock.addStatement("break");
        return caseBlock.build();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since message deserialization is always generated
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated
     *         method implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#decodePartial(ProtobufBinaryReader, Object)}
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
     * @return {@code void}, as the fields are decoded into the given builder
     */
    @Override
    protected TypeName returnType() {
        return TypeName.VOID;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value ProtobufDeserializationGenerator#PARTIAL_METHOD_NAME}
     */
    @Override
    protected String name() {
        return PARTIAL_METHOD_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the binary reader type and the builder type of the message
     */
    @Override
    protected List<TypeName> parametersTypes() {
        var builderType = ProtobufBuilderTypeGenerator.getBuilderClassName((DeclaredType) messageElement.typeElement().asType());
        return List.of(ClassName.get(ProtobufBinaryReader.class), builderType);
    }

    /**
     * {@inheritDoc}
     *
     * @return the parameter names matching the types returned by {@link #parametersTypes()}
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_STREAM_NAME, BUILDER_NAME);
    }

    /**
     * Returns the UTF-8 validation strategy for the given field.
     *
     * <p>Only string and map fields support UTF-8 validation. All other field types
     * return {@link ProtobufUtf8Validation#NONE}.
     *
     * @param property the field element to inspect
     * @return the UTF-8 validation strategy for the field
     */
    private static ProtobufUtf8Validation getUtf8Validation(ProtobufFieldElement property) {
        return switch (property) {
            case ProtobufFieldElement.String f -> f.utf8Validation();
            case ProtobufFieldElement.Map f -> f.utf8Validation();
            default -> ProtobufUtf8Validation.NONE;
        };
    }

    /**
     * Returns the message encoding for the given field.
     *
     * <p>Only message fields can specify a non-default encoding. All other field types
     * return {@link ProtobufMessageEncoding#LENGTH_PREFIXED}.
     *
     * @param property the field element to inspect
     * @return the message encoding for the field
     */
    private static ProtobufMessageEncoding getMessageEncoding(ProtobufFieldElement property) {
        return switch (property) {
            case ProtobufFieldElement.Message f -> f.messageEncoding();
            default -> ProtobufMessageEncoding.LENGTH_PREFIXED;
        };
    }
}
