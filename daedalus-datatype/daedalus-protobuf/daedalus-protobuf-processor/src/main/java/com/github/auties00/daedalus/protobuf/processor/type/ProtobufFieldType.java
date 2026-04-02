package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

// A representation of a protobuf type
public sealed interface ProtobufFieldType
        extends DaedalusFieldType
        permits ProtobufCollectionFieldType, ProtobufMapFieldType, ProtobufSimpleFieldType {
    // The protobuf type of the field
    // For example: required string field = 1;
    // The protobuf type here is string
    ProtobufType protobufType();

    // List of converter used as middlewares between:
    // 1. The protobuf input -> the model
    // 2. The protobuf model -> the output
    List<DaedalusConverterElement> converters();

    // Adds a nullable converter to the type
    void addConverter(DaedalusConverterElement element);

    // Remove all converters
    void clearConverters();

    // The type of the Element that describes this property
    // This can be interpreted as the input type as this is used by the deserializer and builder
    //
    // This is also the parameter's type in the constructor of the enclosing ProtobufMessage
    // This is guaranteed by the ProtobufProcessorExtension#hasPropertiesConstructor check
    //
    // The associated descriptor can either be:
    // 1. VariableElement(class field/record component)
    // 2. ExecutableElement (@ProtobufGetter with no VariableElement associated by index)
    TypeMirror descriptorElementType();

    // The type returned by the accessor for the property
    // This can be interpreted as the output type as this is used by the serializer
    // Hierarchy for accessor resolution(from most important to least important):
    // 1. @ProtobufGetter with the same index
    // 2. Field (accessible if public, protected or package private)
    // 3. Getter/Accessor method
    TypeMirror accessorType();

    // The default valueType of the type
    // For a primitive type, the valueType is 0 (or false)
    // For an object, it's null, or the default valueType assigned by @ProtobufDefaultValue in a ProtobufMixin registered in a @ProtobufProperty
    String descriptorDefaultValue();

    // The mixins associated to this type
    List<TypeElement> mixins();

    // Default implementation to get the serializers for the converters
    default List<DaedalusConverterElement.Attributed.Serializer> serializers() {
        return converters()
                .stream()
                .filter(entry -> entry instanceof DaedalusConverterElement.Attributed.Serializer)
                .map(entry -> (DaedalusConverterElement.Attributed.Serializer) entry)
                .toList();
    }

    // Default implementation to get the deserializers for the converters
    default List<DaedalusConverterElement.Attributed.Deserializer> deserializers() {
        return converters()
                .stream()
                .filter(entry -> entry instanceof DaedalusConverterElement.Attributed.Deserializer)
                .map(entry -> (DaedalusConverterElement.Attributed.Deserializer) entry)
                .toList();
    }

    default TypeMirror serializedType() {
        var serializers = serializers();
        if (serializers.isEmpty()) {
            return descriptorElementType();
        }

        return serializers.getLast()
                .returnType();
    }

    default TypeMirror deserializedType() {
        var deserializers = deserializers();
        if (deserializers.isEmpty()) {
            return descriptorElementType();
        }

        return deserializers.getLast()
                .returnType();
    }
}
