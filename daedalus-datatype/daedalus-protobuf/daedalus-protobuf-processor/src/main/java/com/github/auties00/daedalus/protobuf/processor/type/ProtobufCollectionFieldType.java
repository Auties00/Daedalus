package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A repeated (collection) protobuf field type whose Java type is assignable to
 * {@link java.util.Collection}.
 *
 * <p>The protobuf wire type is derived from the element type carried by
 * {@code valueType}.
 * <p>A packed field is a candidate for the bulk path: its value is converted as a whole, by
 * the {@link #converters() converters of the field}, to an array of the primitive type of
 * its wire values, such as {@code int[]} for {@code int32}, which is written and read as a
 * whole, and decoded with the Vector API where the platform supports it. The converters are
 * optional: if the value cannot be converted to and from that array, the field is
 * {@linkplain #bulk() not bulk}, and each element is converted by the converters of
 * {@code valueType} instead. The elements are only attributed in that case.
 *
 * @param descriptorElementType the declared Java collection type of the field
 * @param valueType the simple field type describing each element in the
 *        collection
 * @param descriptorDefaultValue the default initialization expression for
 *        the field
 * @param bulkCandidate whether the field is packed, so that it is written and read in bulk
 *        if its value can be converted to and from an array of the primitive type of its
 *        wire values
 * @param mixins the mixins that apply to this field
 * @param converters the optional converters of the whole value of the field, to and from an
 *        array of the primitive type of its wire values
 */
public record ProtobufCollectionFieldType(
        TypeMirror descriptorElementType,
        ProtobufSimpleFieldType valueType,
        String descriptorDefaultValue,
        boolean bulkCandidate,
        DaedalusMixins mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufValueFieldType {
    /**
     * Creates a collection field type without converters.
     *
     * @param descriptorElementType the declared Java collection type of the field
     * @param valueType the simple field type describing each element in the collection
     * @param descriptorDefaultValue the default initialization expression for the field
     * @param bulkCandidate whether the field is packed
     * @param mixins the mixins that apply to this field
     */
    public ProtobufCollectionFieldType(TypeMirror descriptorElementType, ProtobufSimpleFieldType valueType, String descriptorDefaultValue, boolean bulkCandidate, DaedalusMixins mixins) {
        this(descriptorElementType, valueType, descriptorDefaultValue, bulkCandidate, mixins, new ArrayList<>());
    }

    @Override
    public TypeMirror accessorType() {
        return descriptorElementType;
    }

    @Override
    public ProtobufType protobufType() {
        return valueType.protobufType();
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
     * Returns whether this field is written and read in bulk, as an array of the primitive
     * type of its wire values.
     *
     * <p>This is the case if the field is a candidate for the bulk path and its value can be
     * converted to and from that array, which is only known once the converters of the
     * field are attributed: an optional converter that could not be resolved is left
     * unattributed.
     *
     * @return {@code true} if this field is written and read in bulk
     */
    public boolean bulk() {
        return bulkCandidate && converters.stream().noneMatch(converter -> converter instanceof DaedalusConverterElement.Unattributed);
    }

    /**
     * Returns the type of the elements of this field, unless it is written and read in
     * bulk, in which case its elements are never converted one by one.
     *
     * @return a list containing the element {@link #valueType()}, or an empty list
     */
    @Override
    public Collection<? extends DaedalusFieldType> nestedTypes() {
        return bulk() ? List.of() : List.of(valueType);
    }
}
