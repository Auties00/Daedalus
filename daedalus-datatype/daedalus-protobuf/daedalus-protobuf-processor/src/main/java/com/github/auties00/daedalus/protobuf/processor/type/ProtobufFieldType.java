package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.processor.model.DaedalusConverterArgument;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * A resolved type of a protobuf field, extending {@link DaedalusFieldType} with
 * protobuf-specific wire type information and converter chain management.
 *
 * <p>This sealed interface restricts its implementations to five variants that
 * model the possible shapes of a protobuf field, of which the first three are
 * {@linkplain ProtobufValueFieldType value field types}, whose values all have the same
 * wire type:
 * <ul>
 * <li>{@link ProtobufSimpleFieldType} for scalar types, strings, bytes, enums,
 *     and nested messages
 * <li>{@link ProtobufCollectionFieldType} for repeated (collection) fields
 * <li>{@link ProtobufArrayFieldType} for repeated scalar fields held by an array of
 *     primitives
 * <li>{@link ProtobufMapFieldType} for map fields with separate key and value types
 * <li>{@link ProtobufOneOfFieldType} for oneofs held by a sealed type, whose cases are
 *     described by simple field types
 * </ul>
 *
 * <p>Each variant carries the Java types used for declaration and access, a default value expression, associated mixins, and an
 * ordered converter chain that mediates between the wire format and the Java model.
 */
public sealed interface ProtobufFieldType
        extends DaedalusFieldType
        permits ProtobufMapFieldType, ProtobufOneOfFieldType, ProtobufValueFieldType {
    /**
     * Returns the converter chain used as middleware between the protobuf wire
     * format and the Java model.
     *
     * <p>Converters are applied in order during both serialization (model to
     * wire format) and deserialization (wire format to model).
     *
     * @return an unmodifiable list of converters
     */
    List<DaedalusConverterElement> converters();

    /**
     * Adds a converter to the end of this field's converter chain.
     *
     * @param element the converter element to add
     */
    void addConverter(DaedalusConverterElement element);

    /**
     * Removes all converters from this field's converter chain.
     */
    void clearConverters();

    /**
     * Returns the Java type of the element that describes this field.
     *
     * <p>This type represents the input type used by the deserializer and
     * builder. It is also the parameter type in the constructor of the
     * enclosing {@code @ProtobufMessage}, as guaranteed by
     * {@code ProtobufProcessorExtension#hasPropertiesConstructor}.
     *
     * <p>The associated descriptor can be either a {@code VariableElement}
     * (class field or record component) or an {@code ExecutableElement}
     * ({@code @ProtobufGetter} with no {@code VariableElement} associated
     * by index).
     *
     * @return the descriptor element type mirror
     */
    TypeMirror descriptorElementType();

    /**
     * Returns the Java type returned by the accessor for this field.
     *
     * <p>This type represents the output type used by the serializer. The
     * accessor is resolved according to the following priority:
     * <ul>
     * <li>{@code @ProtobufGetter} with the same index
     * <li>Field (accessible if public, protected, or package-private)
     * <li>Getter or accessor method
     * </ul>
     *
     * @return the accessor type mirror
     */
    TypeMirror accessorType();

    /**
     * Returns the default initialization expression for this field type.
     *
     * <p>For primitive types, the value is {@code 0} (or {@code false}). For
     * object types, it is {@code null} unless overridden by a
     * {@code @ProtobufDefaultValue} annotation in a {@code ProtobufMixin}
     * registered via {@code @ProtobufProperty}.
     *
     * @return the default value expression as a source code string
     */
    String descriptorDefaultValue();

    /**
     * Returns the serializer converters from this field's converter chain.
     *
     * <p>Filters the full converter list to include only elements that are
     * instances of {@link DaedalusConverterElement.Attributed.Serializer}.
     *
     * @return an unmodifiable list of serializer converters
     */
    default List<DaedalusConverterElement.Attributed.Serializer> serializers() {
        return converters()
                .stream()
                .filter(entry -> entry instanceof DaedalusConverterElement.Attributed.Serializer)
                .map(entry -> (DaedalusConverterElement.Attributed.Serializer) entry)
                .toList();
    }

    /**
     * Returns the deserializer converters from this field's converter chain.
     *
     * <p>Filters the full converter list to include only elements that are
     * instances of {@link DaedalusConverterElement.Attributed.Deserializer}.
     *
     * @return an unmodifiable list of deserializer converters
     */
    default List<DaedalusConverterElement.Attributed.Deserializer> deserializers() {
        return converters()
                .stream()
                .filter(entry -> entry instanceof DaedalusConverterElement.Attributed.Deserializer)
                .map(entry -> (DaedalusConverterElement.Attributed.Deserializer) entry)
                .toList();
    }

    /**
     * Returns the message whose builder holds the value of a field of this type, in the
     * builder of the message that declares the field.
     *
     * <p>A field holds the builder of a message if it is not repeated, and its first
     * deserializer is the decoder of the Spec class of a {@link ProtobufMessage}, so that an
     * occurrence of the message decoded more than once is merged into the same builder, like
     * the official protobuf library does. The other deserializers, which convert the built
     * message to the type of the field, are applied when the builder of the field is built,
     * so none of them can receive an injected argument, like a reader.
     *
     * @return the type of the message, or an empty optional if a field of this type holds its
     *         value
     */
    default Optional<DeclaredType> builderMessage() {
        if (!(this instanceof ProtobufSimpleFieldType simpleType) || simpleType.protobufType() != ProtobufType.MESSAGE) {
            return Optional.empty();
        }

        var deserializers = deserializers();
        if (deserializers.isEmpty()) {
            return Optional.empty();
        }

        var decoder = deserializers.getFirst();
        if (!(decoder.delegate() instanceof DaedalusMethodElement.Synthetic)
                || !decoder.arguments().isEmpty()
                || !(decoder.returnType() instanceof DeclaredType messageType)
                || messageType.asElement().getAnnotation(ProtobufMessage.class) == null) {
            return Optional.empty();
        }

        for (var i = 1; i < deserializers.size(); i++) {
            for (var argument : deserializers.get(i).arguments()) {
                if (argument instanceof DaedalusConverterArgument.Injected) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(messageType);
    }

    /**
     * Returns the type produced after applying all serializer converters in
     * the chain.
     *
     * <p>If no serializers are present, returns the
     * {@linkplain #descriptorElementType() descriptor element type} unchanged.
     * Otherwise, returns the return type of the last serializer in the chain.
     *
     * @return the final serialized type mirror
     */
    default TypeMirror serializedType() {
        var serializers = serializers();
        if (serializers.isEmpty()) {
            return descriptorElementType();
        }

        return serializers.getLast()
                .returnType();
    }

    /**
     * Returns the type produced after applying all deserializer converters in
     * the chain.
     *
     * <p>If no deserializers are present, returns the
     * {@linkplain #descriptorElementType() descriptor element type} unchanged.
     * Otherwise, returns the return type of the last deserializer in the chain.
     *
     * @return the final deserialized type mirror
     */
    default TypeMirror deserializedType() {
        var deserializers = deserializers();
        if (deserializers.isEmpty()) {
            return descriptorElementType();
        }

        return deserializers.getLast()
                .returnType();
    }
}
