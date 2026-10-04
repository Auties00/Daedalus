package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldType;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A map protobuf field type with separate key and value types.
 *
 * <p>A map field has no type of its own: it is encoded as a repeated message whose
 * key and value have the types of {@link #keyType()} and {@link #valueType()}, which
 * are both {@link ProtobufSimpleFieldType simple field types}. An ordered converter chain mediates
 * between the wire format and the Java model during serialization and
 * deserialization.
 *
 * @param descriptorElementType the declared Java map type of the field
 * @param keyType the simple field type describing each map key
 * @param valueType the field type describing each map value
 * @param descriptorDefaultValue the default initialization expression for
 *        the field
 * @param mixins the mixins that apply to this field
 * @param converters the mutable list of converters forming the conversion chain
 */
public record ProtobufMapFieldType(
        TypeMirror descriptorElementType,
        ProtobufSimpleFieldType keyType,
        ProtobufSimpleFieldType valueType,
        String descriptorDefaultValue,
        DaedalusMixins mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufFieldType {

    /**
     * Creates a map field type with an empty converter chain.
     *
     * @param descriptorElementType the declared Java map type of the field
     * @param keyType the simple field type describing each map key
     * @param valueType the field type describing each map value
     * @param descriptorDefaultValue the default initialization expression
     * @param mixins the mixins that apply to this field
     */
    public ProtobufMapFieldType(
            TypeMirror descriptorElementType,
            ProtobufSimpleFieldType keyType,
            ProtobufSimpleFieldType valueType,
            String descriptorDefaultValue,
            DaedalusMixins mixins
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
    public void addConverter(DaedalusConverterElement element) {
        converters.add(element);
    }

    @Override
    public void clearConverters() {
        converters.clear();
    }

    /**
     * Returns the key and value field types so that the framework recurses into
     * them during converter attribution.
     *
     * @return a list containing {@link #keyType()} and {@link #valueType()}
     */
    @Override
    public Collection<? extends DaedalusFieldType> nestedTypes() {
        return List.of(keyType, valueType);
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
