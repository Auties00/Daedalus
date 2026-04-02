package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ProtobufMapFieldType(
        TypeMirror descriptorElementType,
        ProtobufSimpleFieldType keyType,
        ProtobufFieldType valueType,
        String descriptorDefaultValue,
        List<TypeElement> mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufFieldType {

    public ProtobufMapFieldType(
            TypeMirror descriptorElementType,
            ProtobufSimpleFieldType keyType,
            ProtobufFieldType valueType,
            String descriptorDefaultValue,
            List<TypeElement> mixins
    ) {
        this(descriptorElementType, keyType, valueType, descriptorDefaultValue, mixins, new ArrayList<>());
    }

    @Override
    public TypeMirror accessorType() {
        return descriptorElementType;
    }

    @Override
    public List<DaedalusConverterElement> converters() {
        return Collections.unmodifiableList(converters);
    }

    @Override
    public List<TypeElement> mixins() {
        return Collections.unmodifiableList(mixins);
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
        return ProtobufType.MAP;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProtobufMapFieldType) obj;
        return Objects.equals(this.descriptorElementType, that.descriptorElementType) &&
               Objects.equals(this.keyType, that.keyType) &&
               Objects.equals(this.valueType, that.valueType) &&
               Objects.equals(this.descriptorDefaultValue, that.descriptorDefaultValue) &&
               Objects.equals(this.mixins, that.mixins) &&
               Objects.equals(this.converters, that.converters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptorElementType, keyType, valueType, descriptorDefaultValue, mixins, converters);
    }

    @Override
    public String toString() {
        return "MapType[" +
               "descriptorElementType=" + descriptorElementType + ", " +
               "keyType=" + keyType + ", " +
               "valueType=" + valueType + ", " +
               "descriptorDefaultValue=" + descriptorDefaultValue + ", " +
               "mixins=" + mixins + ", " +
               "converters=" + converters + ']';
    }
}
