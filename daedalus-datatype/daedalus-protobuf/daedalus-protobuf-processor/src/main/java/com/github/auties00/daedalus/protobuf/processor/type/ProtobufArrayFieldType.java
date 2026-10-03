package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A repeated protobuf field type whose Java type is an array, such as {@code int[]} for a
 * repeated {@code int32} field, or {@code Instant[]} for a repeated {@code int64} field.
 *
 * <p>Like a collection, every element of the array is a value of the field, converted by the
 * converters of {@code valueType} from the component type of the array to the wire type of
 * the field. The only exception is {@code byte[]}, which is the type of a single
 * {@code bytes} field, so a repeated {@code bytes} field is a {@code byte[][]}.
 * <p>A packed field is a candidate for the bulk path: its value is converted as a whole, by
 * the {@link #converters() converters of the field}, to an array of the primitive type of
 * its wire values, such as {@code int[]} for {@code int32}, which is written and read as a
 * whole, and decoded with the Vector API where the platform supports it. The converters are
 * optional: if the value cannot be converted to and from that array, the field is
 * {@linkplain #bulk() not bulk}, and each element is converted by the converters of
 * {@code valueType} instead. The elements are only attributed in that case.
 * An array of the primitive type of the wire values needs no converter to take the bulk
 * path.
 *
 * @param descriptorElementType the declared array type of the field
 * @param valueType the simple field type describing each element of the array, whose
 *        type is the component type of the array
 * @param bulkCandidate whether the field is packed, so that it is written and read in bulk
 *        if its value can be converted to and from an array of the primitive type of its
 *        wire values
 * @param mixins the mixins that apply to this field
 * @param converters the optional converters of the whole value of the field, to and from an
 *        array of the primitive type of its wire values
 */
public record ProtobufArrayFieldType(
        ArrayType descriptorElementType,
        ProtobufSimpleFieldType valueType,
        boolean bulkCandidate,
        DaedalusMixins mixins,
        List<DaedalusConverterElement> converters
) implements ProtobufValueFieldType {
    /**
     * Creates an array field type without converters.
     *
     * @param descriptorElementType the declared array type of the field
     * @param valueType the simple field type describing each element of the array
     * @param bulkCandidate whether the field is packed
     * @param mixins the mixins that apply to this field
     */
    public ProtobufArrayFieldType(ArrayType descriptorElementType, ProtobufSimpleFieldType valueType, boolean bulkCandidate, DaedalusMixins mixins) {
        this(descriptorElementType, valueType, bulkCandidate, mixins, new ArrayList<>());
    }

    @Override
    public TypeMirror accessorType() {
        return descriptorElementType;
    }

    @Override
    public ProtobufType protobufType() {
        return valueType.protobufType();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty array, which is the value of a repeated field without elements.
     * For example, the default value of an {@code int[]} is {@code new int[0]}, and the one
     * of an {@code int[][]} is {@code new int[0][]}.
     *
     * @return the expression that creates an empty array
     */
    @Override
    public String descriptorDefaultValue() {
        TypeMirror innermostType = descriptorElementType;
        var dimensions = 0;
        while (innermostType instanceof ArrayType arrayType) {
            innermostType = arrayType.getComponentType();
            dimensions++;
        }
        return "new %s[0]%s".formatted(innermostType, "[]".repeat(dimensions - 1));
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

    /**
     * Returns the primitive type of the wire values of the given type, which is the component
     * type of an array that is written and read in bulk.
     *
     * @param protobufType the type of the elements of the repeated field
     * @return the name of the primitive type, or {@code null} if the wire values of the type
     *         are not primitives
     */
    public static String getBulkComponentType(ProtobufType protobufType) {
        return switch (protobufType) {
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case BOOL -> "boolean";
            case ENUM, INT32, SINT32, UINT32, FIXED32, SFIXED32 -> "int";
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> "long";
            default -> null;
        };
    }
}
