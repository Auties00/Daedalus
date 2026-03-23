package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Represents a JSON {@code null}.
 *
 * <p>{@code NullValue} is a sentinel, using an enum with only one value to represent
 * the null value for the {@code Value} type union.
 *
 * <p>A field of type {@code NullValue} with any value other than {@code 0} is considered
 * invalid. Most ProtoJSON serializers will emit a Value with a {@code null_value} set
 * as a JSON {@code null} regardless of the integer value, and so will round trip to
 * a {@code 0} value.
 *
 * <p>Protobuf enum {@code google.protobuf.NullValue}
 */
@ProtobufEnum
public enum NullValue {
    /**
     * Null value.
     */
    @ProtobufEnum.Constant(index = 0)
    NULL_VALUE;
}
