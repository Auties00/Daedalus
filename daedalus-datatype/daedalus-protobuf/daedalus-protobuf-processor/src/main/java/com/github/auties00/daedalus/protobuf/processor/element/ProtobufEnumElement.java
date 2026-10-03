package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusBuilderElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
 * A processed protobuf enum element.
 *
 * <p>This class models types annotated with {@code @ProtobufEnum}, collecting
 * their constants and metadata. Constants are accessible both
 * through {@link #fields()} (index to element map) and through
 * {@link #fields()} (for the {@link DaedalusTypeElement}
 * contract, though they do not participate in converter attribution).
 */
public final class ProtobufEnumElement implements ProtobufTypeElement {
    private final TypeElement typeElement;
    private final ProtobufEnumType enumType;
    private final TreeMap<Integer, ProtobufEnumConstantElement> fields;
    private final SequencedSet<? extends ProtobufReservedFieldElement> reservedElements;

    /**
     * The instance method annotated with {@code TypeSerializer} that returns the index of a
     * value, or {@code null} if the values are the constants of the type.
     */
    private ExecutableElement serializer;

    /**
     * The static method annotated with {@code TypeDeserializer} that returns the value of an
     * index, or {@code null} if the values are the constants of the type.
     */
    private ExecutableElement deserializer;

    /**
     * Constructs a new protobuf enum element.
     *
     * @param typeElement the source enum type element
     * @param version the protobuf version for resolving VERSION_DEFAULT values
     * @param enumType the enum type (OPEN, CLOSED, or VERSION_DEFAULT)
     * @param reserved the reserved field elements
     */
    public ProtobufEnumElement(TypeElement typeElement, ProtobufVersion version, ProtobufEnumType enumType, SequencedSet<? extends ProtobufReservedFieldElement> reserved) {
        this.typeElement = typeElement;
        this.enumType = enumType.resolve(version);
        this.fields = new TreeMap<>();
        this.reservedElements = reserved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeElement typeElement() {
        return typeElement;
    }

    /**
     * Returns how out-of-range enum values are handled during deserialization.
     *
     * @return the enum type (OPEN, CLOSED, or VERSION_DEFAULT)
     */
    public ProtobufEnumType enumType() {
        return enumType;
    }

    /**
     * Returns whether this element is a Java enum, whose values are its enum constants.
     *
     * @return {@code true} if the type of this element is a Java enum
     */
    public boolean javaEnum() {
        return typeElement.getKind() == ElementKind.ENUM;
    }

    /**
     * Returns the instance method that returns the index of a value, if the indexes of the
     * values are computed by the type instead of being the ones of its constants.
     *
     * @return the method annotated with {@code TypeSerializer}, if any
     */
    public Optional<ExecutableElement> serializer() {
        return Optional.ofNullable(serializer);
    }

    /**
     * Returns the static method that returns the value of an index, if the values are
     * created by the type instead of being its constants.
     *
     * @return the method annotated with {@code TypeDeserializer}, if any
     */
    public Optional<ExecutableElement> deserializer() {
        return Optional.ofNullable(deserializer);
    }

    /**
     * Sets the methods that convert the values of this type to and from their index, which
     * replace its constants.
     *
     * @param serializer the instance method that returns the index of a value
     * @param deserializer the static method that returns the value of an index
     */
    public void setConverters(ExecutableElement serializer, ExecutableElement deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    /**
     * Returns the enum constant elements
     *
     * @return an unmodifiable collection of enum constant elements
     */
    @Override
    public SequencedCollection<ProtobufEnumConstantElement> fields() {
        return Collections.unmodifiableSequencedCollection(fields.sequencedValues());
    }

    /**
     * Returns the enum constant element with the given protobuf index.
     *
     * @param index the protobuf index to look up
     * @return an {@link Optional} containing the constant element if found, or empty otherwise
     */
    public Optional<ProtobufEnumConstantElement> getField(int index) {
        return Optional.ofNullable(fields.get(index));
    }

    /**
     * Registers an enum constant at the given index, returning any displaced constant.
     *
     * @return the displaced constant element, if a duplicate index was found
     */
    public Optional<ProtobufEnumConstantElement> addField(ProtobufEnumConstantElement element) {
        return Optional.ofNullable(fields.put(element.index(), element));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enums have no builders; returns an empty collection.
     */
    @Override
    public SequencedCollection<? extends DaedalusBuilderElement> builders() {
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enums do not support default builders.
     */
    @Override
    public boolean supportsDefaultBuilder() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enums have no constructor; returns empty.
     */
    @Override
    public Optional<? extends DaedalusMethodElement> constructor() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequencedSet<? extends ProtobufReservedFieldElement> reservedElements() {
        return Collections.unmodifiableSequencedSet(reservedElements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIndexAllowed(int value) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Index)
                .allMatch(element -> ((ProtobufReservedFieldElement.Index) element).allows(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNameAllowed(String name) {
        return reservedElements.stream()
                .filter(element -> element instanceof ProtobufReservedFieldElement.Name)
                .allMatch(element -> ((ProtobufReservedFieldElement.Name) element).allows(name));
    }
}
