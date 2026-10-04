package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldType;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * A simple (non-collection, non-map) protobuf field type.
 *
 * <p>This record covers scalar types ({@code int32}, {@code float}, etc.),
 * strings, bytes, enums, and nested messages. It holds the protobuf wire type,
 * the Java types used for declaration and access, a default value expression,
 * associated mixins, and an ordered converter chain for serialization and
 * deserialization.
 *
 * @param protobufType the protobuf wire type of the field
 * @param descriptorElementType the Java type used to declare the field (input
 *        type for deserialization and constructors)
 * @param accessorType the Java type returned by the field accessor (output
 *        type for serialization)
 * @param descriptorDefaultValue the default initialization expression for
 *        the field
 * @param mixins the mixins that apply to this field
 * @param converters the mutable list of converters forming the conversion chain
 */
public record ProtobufSimpleFieldType(
        ProtobufType protobufType,
        TypeMirror descriptorElementType,
        TypeMirror accessorType,
        String descriptorDefaultValue,
        DaedalusMixins mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufValueFieldType {

    /**
     * Creates a simple field type with an empty converter chain.
     *
     * @param protobufType the protobuf wire type of the field
     * @param descriptorElementType the Java type used to declare the field
     * @param accessorType the Java type returned by the field accessor
     * @param descriptorDefaultValue the default initialization expression
     * @param mixins the mixins that apply to this field
     */
    public ProtobufSimpleFieldType(
            ProtobufType protobufType,
            TypeMirror descriptorElementType,
            TypeMirror accessorType,
            String descriptorDefaultValue,
            DaedalusMixins mixins
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
    public Collection<? extends DaedalusFieldType> nestedTypes() {
        return List.of();
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
