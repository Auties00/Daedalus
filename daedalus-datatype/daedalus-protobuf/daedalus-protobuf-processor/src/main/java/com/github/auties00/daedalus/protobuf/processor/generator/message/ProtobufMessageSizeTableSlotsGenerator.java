package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufMessageElement;
import com.github.auties00.daedalus.protobuf.processor.generator.ProtobufSerializationGenerator;
import com.github.auties00.daedalus.protobuf.processor.manager.ProtobufExceptionManager;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * A method generator that produces the {@code sizeTableSlots} method of the Spec class of a
 * message, which counts the slots of the size table needed to encode a message.
 *
 * <p>The generated method counts one slot for every length delimited part of the message
 * that the generated size calculation method records in the size table, which are:
 * <ul>
 * <li>every nested message written by its Spec, followed by the slots of the nested message,
 *     unless it is encoded as a group, which has no length;
 * <li>every map entry, followed by the slot and the slots of its value, if the value is a
 *     message written by its Spec;
 * <li>every packed field that is not empty and is not written in bulk.
 * </ul>
 * The other fields are not read, so their converters are not invoked. A value that is, or is
 * converted to, {@code null} has no slot, as it is not written.
 *
 * <p>For example, given a message with a field named "inner" at index 1, whose type is the
 * message {@code Inner}, and a packed {@code List<Integer>} field named "numbers":
 *
 * <pre>{@code
 * @Override
 * public int sizeTableSlots(Outer protoInputObject) throws ProtobufSerializationException {
 *     if (protoInputObject == null) {
 *         return 0;
 *     }
 *     var protoOutputSlots = 0;
 *     var inner = protoInputObject.inner();
 *     if (inner != null) {
 *         protoOutputSlots += 1 + InnerSpec.global().sizeTableSlots(inner);
 *     }
 *     var numbersSource = protoInputObject.numbers();
 *     if (numbersSource != null && !numbersSource.isEmpty()) {
 *         protoOutputSlots++;
 *     }
 *     return protoOutputSlots;
 * }
 * }</pre>
 */
public class ProtobufMessageSizeTableSlotsGenerator extends ProtobufSerializationGenerator {
    /**
     * The name of the generated method.
     */
    public static final String METHOD_NAME = "sizeTableSlots";

    private static final String INPUT_OBJECT_PARAMETER = "protoInputObject";
    private static final String OUTPUT_SLOTS_NAME = "protoOutputSlots";

    /**
     * The message whose slots are counted.
     */
    private final ProtobufMessageElement messageElement;

    /**
     * Constructs a new generator for the given message.
     *
     * @param element the message whose slots are counted
     * @param exceptions the manager that resolves the checked exceptions of the generated method
     */
    public ProtobufMessageSizeTableSlotsGenerator(ProtobufMessageElement element, ProtobufExceptionManager exceptions) {
        super(element, exceptions);
        this.messageElement = element;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the null check of the message, then the code that counts the slots of each
     * of its fields that can have one, as described by this class.
     */
    @Override
    protected void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", INPUT_OBJECT_PARAMETER);
        methodBuilder.addStatement("return 0");
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("var $L = 0", OUTPUT_SLOTS_NAME);
        for (var property : messageElement.fields()) {
            if (property instanceof ProtobufFieldElement.OneOf oneOf) {
                writeOneOfSlots(methodBuilder, oneOf);
                continue;
            }

            switch (property.type()) {
                case ProtobufArrayFieldType arrayType -> writeRepeatedSlots(methodBuilder, property, arrayType.valueType(), arrayType.bulk(), true);
                case ProtobufCollectionFieldType collectionType -> writeRepeatedSlots(methodBuilder, property, collectionType.valueType(), collectionType.bulk(), false);
                case ProtobufMapFieldType mapType -> writeMapSlots(methodBuilder, property, mapType);
                case ProtobufSimpleFieldType simpleType -> {
                    if (hasSlots(simpleType)) {
                        writeValueSlots(methodBuilder, property.index(), property.name(), getAccessorCall(property.accessor()), simpleType, true, true, property.delimited());
                    }
                }
                case ProtobufOneOfFieldType _ -> throw new IllegalStateException("Internal bug: oneofs are handled before the switch");
            }
        }
        methodBuilder.addStatement("return $L", OUTPUT_SLOTS_NAME);
    }

    /**
     * Returns whether a value of the given type is a message written by its Spec, which has a
     * slot, and the slots of the messages nested in it.
     *
     * @param type the type of the value
     * @return {@code true} if the value is a message written by its Spec
     */
    private static boolean hasSlots(ProtobufValueFieldType type) {
        return type.protobufType() == ProtobufType.MESSAGE
                && !type.serializers().isEmpty()
                && getSpecReceiver(type.serializers().getLast()).isPresent();
    }

    /**
     * Generates the code that counts the slots of the given value, which is a message written
     * by its Spec, converting it like the serializer does.
     *
     * @param methodBuilder the method builder to append the code to
     * @param index the index of the field
     * @param name the name of the variable that holds the value, if it is declared
     * @param value the expression that provides the value
     * @param type the type of the value
     * @param declareVariable {@code true} if a variable should be declared for the value
     * @param nullCheck {@code true} if the value may be {@code null}
     * @param delimited {@code true} if the message is encoded as a group, which has no slot
     */
    private void writeValueSlots(MethodSpec.Builder methodBuilder, int index, String name, String value, ProtobufValueFieldType type, boolean declareVariable, boolean nullCheck, boolean delimited) {
        writeCustomSerializer(
                methodBuilder,
                index,
                name,
                value,
                type,
                declareVariable,
                nullCheck,
                false,
                false,
                delimited,
                null,
                (builder, message, _) -> builder.addStatement(
                        "$L += $L$L.$L($L)",
                        OUTPUT_SLOTS_NAME,
                        delimited ? "" : "1 + ",
                        getSpecReceiver(type.serializers().getLast()).orElseThrow(),
                        METHOD_NAME,
                        message
                ),
                (_, _, _) -> {}
        );
    }

    /**
     * Generates the code that counts the slots of a repeated field, which has one slot if it is
     * packed, not written in bulk and not empty, or the slots of its elements if they are
     * messages written by their Spec.
     *
     * @param methodBuilder the method builder to append the code to
     * @param property the field
     * @param valueType the type of the elements
     * @param bulk {@code true} if the field is written in bulk
     * @param array {@code true} if the elements are held by an array
     */
    private void writeRepeatedSlots(MethodSpec.Builder methodBuilder, ProtobufFieldElement property, ProtobufSimpleFieldType valueType, boolean bulk, boolean array) {
        var packed = property.packed() && !bulk;
        if (!packed && (bulk || !hasSlots(valueType))) {
            return;
        }

        var source = writeRepeatedSource(methodBuilder, property.name(), getAccessorCall(property.accessor()), array);
        if (packed) {
            methodBuilder.addStatement("$L++", OUTPUT_SLOTS_NAME);
        } else {
            var entry = property.name() + "Entry";
            methodBuilder.beginControlFlow("for (var $L : $L)", entry, source);
            writeValueSlots(methodBuilder, property.index(), entry, entry, valueType, false, true, property.delimited());
            methodBuilder.endControlFlow();
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Generates the code that counts the slots of a map field, which has one slot for each of
     * its entries, followed by the slots of its value if it is a message written by its Spec.
     *
     * @param methodBuilder the method builder to append the code to
     * @param property the field
     * @param mapType the type of the map
     */
    private void writeMapSlots(MethodSpec.Builder methodBuilder, ProtobufFieldElement property, ProtobufMapFieldType mapType) {
        var map = property.name() + "MapField";
        methodBuilder.addStatement("var $L = $L", map, getAccessorCall(property.accessor()));
        methodBuilder.beginControlFlow("if ($L != null)", map);
        if (hasSlots(mapType.valueType())) {
            var entry = property.name() + "MapEntry";
            methodBuilder.beginControlFlow("for (var $L : $L.entrySet())", entry, map);
            methodBuilder.addStatement("$L++", OUTPUT_SLOTS_NAME);
            writeValueSlots(methodBuilder, 2, property.name() + "MapValue", entry + ".getValue()", mapType.valueType(), true, true, false);
            methodBuilder.endControlFlow();
        } else {
            methodBuilder.addStatement("$L += $L.size()", OUTPUT_SLOTS_NAME, map);
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Generates the code that counts the slots of the case of a oneof that is set, if it is a
     * message written by its Spec.
     *
     * @param methodBuilder the method builder to append the code to
     * @param oneOf the oneof
     */
    private void writeOneOfSlots(MethodSpec.Builder methodBuilder, ProtobufFieldElement.OneOf oneOf) {
        var cases = oneOf.cases()
                .stream()
                .filter(oneOfCase -> hasSlots((ProtobufValueFieldType) oneOfCase.type()))
                .toList();
        if (cases.isEmpty()) {
            return;
        }

        var caseValue = oneOf.name() + "Case";
        methodBuilder.addStatement("var $L = $L", oneOf.name(), getAccessorCall(oneOf.accessor()));
        methodBuilder.beginControlFlow("switch ($L)", oneOf.name());
        for (var oneOfCase : cases) {
            methodBuilder.beginControlFlow("case $T $L ->", TypeName.get(oneOfCase.type().descriptorElementType()), caseValue);
            writeValueSlots(methodBuilder, oneOfCase.index(), caseValue, caseValue, (ProtobufValueFieldType) oneOfCase.type(), false, false, oneOfCase.delimited());
            methodBuilder.endControlFlow();
        }
        methodBuilder.addCode("case null, default -> {}\n");
        methodBuilder.endControlFlow();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} always, since the slots of a message are always counted
     */
    @Override
    public boolean shouldInstrument() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return a list containing the {@link Override} annotation, as the generated method
     *         implements {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec#sizeTableSlots(Object)}
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
     * @return {@link TypeName#INT}, the number of slots
     */
    @Override
    protected TypeName returnType() {
        return TypeName.INT;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #METHOD_NAME}
     */
    @Override
    protected String name() {
        return METHOD_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the message type
     */
    @Override
    protected List<TypeName> parametersTypes() {
        return List.of(ClassName.get(ownerElement.typeElement()));
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the parameter that holds the message
     */
    @Override
    protected List<String> parametersNames() {
        return List.of(INPUT_OBJECT_PARAMETER);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the exceptions of the size calculation method, as the generated method
     * invokes the same converters to reach the nested messages.
     *
     * @return the checked exceptions of the generated method
     */
    @Override
    protected List<TypeName> thrownTypes() {
        return exceptions.sizeExceptions(ownerElement);
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
