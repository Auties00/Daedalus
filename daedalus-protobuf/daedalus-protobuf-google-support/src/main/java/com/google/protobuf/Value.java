package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

/**
 * Represents a JSON value.
 *
 * <p>{@code Value} represents a dynamically typed value which can be either
 * null, a number, a string, a boolean, a recursive struct value, or a
 * list of values. A producer of value is expected to set one of these
 * variants. Absence of any variant is an invalid state.
 *
 * <p>Protobuf type {@code google.protobuf.Value}
 */
@ProtobufMessage
public final class Value {

    /**
     * Represents a JSON {@code null}.
     *
     * <p><code>NullValue null_value = 1;</code>
     */
    @ProtobufMessage.EnumField(index = 1)
    NullValue nullValue;

    /**
     * Represents a JSON number. Must not be {@code NaN}, {@code Infinity} or
     * {@code -Infinity}, since those are not supported in JSON. This also cannot
     * represent large Int64 values, since JSON format generally does not
     * support them in its number type.
     *
     * <p><code>double number_value = 2;</code>
     */
    @ProtobufMessage.DoubleField(index = 2)
    double numberValue;

    /**
     * Represents a JSON string.
     *
     * <p><code>string string_value = 3;</code>
     */
    @ProtobufMessage.StringField(index = 3)
    String stringValue;

    /**
     * Represents a JSON boolean ({@code true} or {@code false} literal in JSON).
     *
     * <p><code>bool bool_value = 4;</code>
     */
    @ProtobufMessage.BoolField(index = 4)
    boolean boolValue;

    /**
     * Represents a JSON object.
     *
     * <p><code>Struct struct_value = 5;</code>
     */
    @ProtobufMessage.MessageField(index = 5)
    Struct structValue;

    /**
     * Represents a JSON array.
     *
     * <p><code>ListValue list_value = 6;</code>
     */
    @ProtobufMessage.MessageField(index = 6)
    ListValue listValue;

    Value(
            NullValue nullValue,
            double numberValue,
            String stringValue,
            boolean boolValue,
            Struct structValue,
            ListValue listValue
    ) {
        this.nullValue = nullValue;
        this.numberValue = numberValue;
        this.stringValue = stringValue;
        this.boolValue = boolValue;
        this.structValue = structValue;
        this.listValue = listValue;
    }

    public NullValue nullValue() {
        return nullValue;
    }

    public double numberValue() {
        return numberValue;
    }

    public String stringValue() {
        return stringValue;
    }

    public boolean boolValue() {
        return boolValue;
    }

    public Struct structValue() {
        return structValue;
    }

    public ListValue listValue() {
        return listValue;
    }
}
