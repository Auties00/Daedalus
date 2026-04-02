package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;

public record ProtobufCollectionFieldType(
        TypeMirror descriptorElementType,
        ProtobufSimpleFieldType valueType,
        String descriptorDefaultValue,
        List<TypeElement> mixins
) implements ProtobufFieldType {
    @Override
    public TypeMirror accessorType() {
        return descriptorElementType;
    }

    @Override
    public ProtobufType protobufType() {
        return valueType.protobufType();
    }

    // The fact that a type is a CollectionType is inferred from the fact that it's assignable to Collection,
    // So by hypothesis it cannot have any converters
    @Override
    public List<DaedalusConverterElement> converters() {
        return List.of();
    }

    @Override
    public List<TypeElement> mixins() {
        return Collections.unmodifiableList(mixins);
    }

    @Override
    public void addConverter(DaedalusConverterElement element) {

    }

    @Override
    public void clearConverters() {

    }
}
