package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ProtobufSimpleFieldType(
        ProtobufType protobufType,
        TypeMirror descriptorElementType,
        TypeMirror accessorType,
        String descriptorDefaultValue,
        List<TypeElement> mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufFieldType {

    public ProtobufSimpleFieldType(
            ProtobufType protobufType,
            TypeMirror descriptorElementType,
            TypeMirror accessorType,
            String descriptorDefaultValue,
            List<TypeElement> mixins
    ) {
        this(protobufType, descriptorElementType, accessorType, descriptorDefaultValue, mixins, new ArrayList<>());
    }

    @Override
    public List<DaedalusConverterElement> converters() {
        return Collections.unmodifiableList(converters);
    }

    @Override
    public void addConverter(DaedalusConverterElement element) {
        converters.add(element);
    }

    @Override
    public void clearConverters() {
        converters.clear();
    }

    @Override
    public ProtobufType protobufType() {
        return protobufType;
    }

    @Override
    public TypeMirror descriptorElementType() {
        return descriptorElementType;
    }

    @Override
    public TypeMirror accessorType() {
        return accessorType;
    }

    @Override
    public String descriptorDefaultValue() {
        return descriptorDefaultValue;
    }

    @Override
    public List<TypeElement> mixins() {
        return Collections.unmodifiableList(mixins);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProtobufSimpleFieldType) obj;
        return Objects.equals(this.protobufType, that.protobufType) &&
               Objects.equals(this.descriptorElementType, that.descriptorElementType) &&
               Objects.equals(this.accessorType, that.accessorType) &&
               Objects.equals(this.converters, that.converters) &&
               Objects.equals(this.descriptorDefaultValue, that.descriptorDefaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protobufType, descriptorElementType, accessorType, converters, descriptorDefaultValue);
    }

    @Override
    public String toString() {
        return "NormalType[" +
               "protobufType=" + protobufType + ", " +
               "descriptorElementType=" + descriptorElementType + ", " +
               "accessorType=" + accessorType + ", " +
               "defaultValue=" + descriptorDefaultValue + ']';
    }
}
