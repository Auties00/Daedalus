package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.processor.metadata.ProtobufEnumMetadata;
import com.github.auties00.daedalus.protobuf.processor.metadata.ProtobufFieldMetadata;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

public class ProtobufObjectElement implements DaedalusTypeElement {
    private final Type type;
    private final TypeElement typeElement;
    private final SortedMap<Long, ProtobufFieldElement> properties;
    private final List<ProtobufBuilderElement> builders;
    private final SortedMap<Integer, String> constants;
    private final ProtobufEnumMetadata enumMetadata;
    private final DaedalusMethodElement deserializer;
    private final Set<? extends ProtobufReservedFieldElement> reservedElements;
    private ProtobufUnknownFieldsElement unknownFieldsElement;

    public static ProtobufObjectElement ofEnum(TypeElement typeElement, ProtobufEnumMetadata enumMetadata, Set<? extends ProtobufReservedFieldElement> reserved) {
        return new ProtobufObjectElement(Type.ENUM, typeElement, enumMetadata, null, reserved);
    }

    public static ProtobufObjectElement ofMessage(TypeElement typeElement, DaedalusMethodElement deserializer, Set<? extends ProtobufReservedFieldElement> reserved) {
        return new ProtobufObjectElement(Type.MESSAGE, typeElement, null, deserializer, reserved);
    }

    public static ProtobufObjectElement ofGroup(TypeElement typeElement, DaedalusMethodElement deserializer, Set<? extends ProtobufReservedFieldElement> reserved) {
        return new ProtobufObjectElement(Type.GROUP, typeElement, null, deserializer, reserved);
    }

    private ProtobufObjectElement(
            Type type,
            TypeElement typeElement,
            ProtobufEnumMetadata enumMetadata,
            DaedalusMethodElement deserializer,
            Set<? extends ProtobufReservedFieldElement> reservedElements
    ) {
        this.type = type;
        this.typeElement = typeElement;
        this.enumMetadata = enumMetadata;
        this.deserializer = deserializer;
        this.reservedElements = reservedElements;
        this.builders = new ArrayList<>();
        this.properties = new TreeMap<>();
        this.constants = new TreeMap<>();
    }

    public Type type() {
        return type;
    }

    public TypeElement typeElement() {
        return typeElement;
    }

    public Optional<ProtobufEnumMetadata> enumMetadata() {
        return Optional.of(enumMetadata);
    }

    public List<ProtobufFieldElement> protobufProperties() {
        return List.copyOf(properties.values());
    }

    @Override
    public SequencedCollection<ProtobufFieldElement> properties() {
        return Collections.unmodifiableSequencedCollection(properties.sequencedValues());
    }

    public Map<Integer, String> constants() {
        return Collections.unmodifiableMap(constants);
    }

    public Optional<String> addEnumConstant(int fieldIndex, String fieldName) {
        return Optional.ofNullable(constants.put(fieldIndex, fieldName));
    }

    public Optional<ProtobufFieldElement> addProperty(Element element, Element accessor, ProtobufFieldType type, ProtobufFieldMetadata property) {
        var fieldName = element.getSimpleName().toString();
        var result = new ProtobufFieldElement(
                property.index(),
                fieldName,
                accessor,
                property.required(),
                property.packed(),
                element instanceof ExecutableElement,
                type
        );
        return Optional.ofNullable(properties.put(property.index(), result));
    }

    public void addBuilder(String className, SequencedCollection<ProtobufBuilderParameterElement> parameters, ExecutableElement executableElement) {
        var builderElement = new ProtobufBuilderElement(className, parameters, executableElement);
        builders.add(builderElement);
    }

    public List<ProtobufBuilderElement> builders() {
        return Collections.unmodifiableList(builders);
    }

    public Optional<DaedalusMethodElement> deserializer() {
        return Optional.ofNullable(deserializer);
    }

    public Optional<ProtobufUnknownFieldsElement> unknownFieldsElement() {
        return Optional.ofNullable(unknownFieldsElement);
    }

    public void setUnknownFieldsElement(ProtobufUnknownFieldsElement unknownFieldsElement) {
        this.unknownFieldsElement = unknownFieldsElement;
    }

    public Set<? extends ProtobufReservedFieldElement> reservedElements() {
        return Collections.unmodifiableSet(reservedElements);
    }

    public boolean isIndexAllowed(long value) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Index)
                .allMatch(element -> ((ProtobufReservedFieldElement.Index) element).allows(value));
    }

    public boolean isNameAllowed(String name) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Name)
                .allMatch(element -> ((ProtobufReservedFieldElement.Name) element).allows(name));

    }

    public enum Type {
        MESSAGE,
        ENUM,
        GROUP
    }
}
