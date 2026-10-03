package com.github.auties00.daedalus.protobuf.processor.type;

import com.github.auties00.daedalus.protobuf.model.ProtobufType;

/**
 * A protobuf field type whose values all have the same {@linkplain #protobufType() wire type},
 * such as a field that holds a single value, or a repeated field held by a collection or an
 * array.
 *
 * <p>A map field and a oneof are not value field types: the keys and the values of a map have
 * their own types, and each case of a oneof has its own type.
 */
public sealed interface ProtobufValueFieldType
        extends ProtobufFieldType
        permits ProtobufArrayFieldType, ProtobufCollectionFieldType, ProtobufSimpleFieldType {
    /**
     * Returns the type of the values of this field.
     *
     * <p>For example, given the protobuf declaration
     * {@code repeated string names = 1;}, the type of the values is
     * {@link ProtobufType#STRING STRING}.
     *
     * @return the type of the values
     */
    ProtobufType protobufType();
}
