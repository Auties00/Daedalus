package com.github.auties00.daedalus.protobuf.compiler.typeReference;

import com.github.auties00.daedalus.protobuf.model.ProtobufType;

/**
 * A reference to a type whose values have a single {@linkplain #protobufType() protobuf type}:
 * a scalar type, {@code string}, {@code bytes}, an enum, a message or a group.
 *
 * <p>A group is a message whose fields are delimited by start and end tags, so its values have
 * the {@link ProtobufType#MESSAGE MESSAGE} type. A map is not a value type, as its keys and its
 * values have their own types, and a reference that is not resolved yet has no type.
 */
public sealed interface ProtobufValueTypeReference
        extends ProtobufTypeReference
        permits ProtobufPrimitiveTypeReference, ProtobufMessageTypeReference, ProtobufEnumTypeReference, ProtobufGroupTypeReference {
    /**
     * Returns the protobuf type of the values of this type.
     *
     * @return the protobuf type
     */
    ProtobufType protobufType();
}
