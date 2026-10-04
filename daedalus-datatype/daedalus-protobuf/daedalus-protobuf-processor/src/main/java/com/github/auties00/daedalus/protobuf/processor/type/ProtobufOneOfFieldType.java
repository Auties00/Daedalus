package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldType;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A protobuf field type whose Java type is a sealed type that holds a oneof, such as a
 * sealed interface {@code Shape} whose permitted subtypes {@code Circle} and {@code Square}
 * are the cases of the oneof.
 *
 * <p>The oneof has no type and no converters of its own: each case is a field whose
 * type is described by one of the {@link #caseTypes() case types}, which converts the
 * permitted subtype of the case to and from its wire value. The case types are the
 * {@linkplain #nestedTypes() nested types} of this type, so that their converters are
 * attributed with the ones of the other fields.
 *
 * @param descriptorElementType the sealed type of the field
 * @param caseTypes the types of the cases of the oneof, whose types are the permitted
 *        subtypes of the sealed type, ordered so that a subtype precedes its supertypes
 * @param converters the converters of the field, which are always empty
 */
public record ProtobufOneOfFieldType(
        DeclaredType descriptorElementType,
        List<ProtobufSimpleFieldType> caseTypes,
        List<DaedalusConverterElement> converters
) implements ProtobufFieldType {
    /**
     * Creates a oneof field type without converters.
     *
     * @param descriptorElementType the sealed type of the field
     * @param caseTypes the types of the cases of the oneof
     */
    public ProtobufOneOfFieldType(DeclaredType descriptorElementType, List<ProtobufSimpleFieldType> caseTypes) {
        this(descriptorElementType, List.copyOf(caseTypes), new ArrayList<>());
    }

    @Override
    public TypeMirror accessorType() {
        return descriptorElementType;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "null"}, which is the value of a oneof that has no case set
     */
    @Override
    public String descriptorDefaultValue() {
        return "null";
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
     * {@inheritDoc}
     *
     * @return the empty mixins, as the mixins of a oneof are declared by each of its cases
     */
    @Override
    public DaedalusMixins mixins() {
        return DaedalusMixins.empty();
    }

    /**
     * Returns the types of the cases of this oneof, whose converters are attributed with the
     * ones of the other fields.
     *
     * @return the {@link #caseTypes() case types}
     */
    @Override
    public Collection<? extends DaedalusFieldType> nestedTypes() {
        return caseTypes;
    }
}
