package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.util.Optional;

/**
 * Represents a JSON value.
 *
 * <p>{@code Value} represents a dynamically typed value which can be either
 * null, a number, a string, a boolean, a recursive struct value, or a
 * list of values. A producer of value is expected to set one of these
 * variants. Absence of any variant is an invalid state.
 *
 * <p>Protobuf type {@code com.google.protobuf.Value}
 */
@ProtobufMessage(version = ProtobufVersion.PROTOBUF_3)
public final class Value {

    /**
     * The kind of value, which is the case of the {@code kind} oneof that is set, or
     * {@code null} if no case is set.
     *
     * <p><code>oneof kind { ... }</code>
     */
    @ProtobufMessage.OneOfField(
            enumCases = @ProtobufMessage.OneOfField.EnumCase(index = 1, name = "null_value", type = Kind.NullValueCase.class),
            doubleCases = @ProtobufMessage.OneOfField.DoubleCase(index = 2, type = Kind.NumberValue.class),
            stringCases = @ProtobufMessage.OneOfField.StringCase(index = 3, type = Kind.StringValue.class),
            boolCases = @ProtobufMessage.OneOfField.BoolCase(index = 4, type = Kind.BoolValue.class),
            messageCases = {
                    @ProtobufMessage.OneOfField.MessageCase(index = 5, name = "struct_value", type = Struct.class),
                    @ProtobufMessage.OneOfField.MessageCase(index = 6, type = ListValue.class)
            }
    )
    Kind kind;

    /**
     * Constructs a new {@code Value} with the specified field values.
     *
     * @param kind the case of the {@code kind} oneof that is set, or {@code null} if no
     *             case is set
     */
    Value(Kind kind) {
        this.kind = kind;
    }

    /**
     * Returns the case of the {@code kind} oneof that is set, if any.
     *
     * @return an {@code Optional} containing the case that is set, or an empty
     *         {@code Optional} if no case is set
     */
    public Optional<Kind> kind() {
        return Optional.ofNullable(kind);
    }

    /**
     * The cases of the {@code kind} oneof.
     *
     * <p>A JSON object is a {@link Struct} and a JSON array is a {@link ListValue}, which
     * implement this interface, while the other cases wrap their value in a record.
     */
    public sealed interface Kind permits Kind.NullValueCase, Kind.NumberValue, Kind.StringValue, Kind.BoolValue, Struct, ListValue {
        /**
         * Represents a JSON {@code null}.
         *
         * <p><code>NullValue null_value = 1;</code>
         *
         * @param value the null value
         */
        record NullValueCase(NullValue value) implements Kind {
            /**
             * Returns the null value of this case.
             *
             * @return the null value
             */
            @TypeSerializer
            public NullValue value() {
                return value;
            }

            /**
             * Returns the case that holds the given null value.
             *
             * @param value the null value
             * @return the case
             */
            @TypeDeserializer
            static NullValueCase of(NullValue value) {
                return new NullValueCase(value);
            }
        }

        /**
         * Represents a JSON number. Must not be {@code NaN}, {@code Infinity} or
         * {@code -Infinity}, since those are not supported in JSON. This also cannot
         * represent large Int64 values, since JSON format generally does not
         * support them in its number type.
         *
         * <p><code>double number_value = 2;</code>
         *
         * @param value the number
         */
        record NumberValue(double value) implements Kind {
            /**
             * Returns the number of this case.
             *
             * @return the number
             */
            @TypeSerializer
            public double value() {
                return value;
            }

            /**
             * Returns the case that holds the given number.
             *
             * @param value the number
             * @return the case
             */
            @TypeDeserializer
            static NumberValue of(double value) {
                return new NumberValue(value);
            }
        }

        /**
         * Represents a JSON string.
         *
         * <p><code>string string_value = 3;</code>
         *
         * @param value the string
         */
        record StringValue(String value) implements Kind {
            /**
             * Returns the string of this case.
             *
             * @return the string
             */
            @TypeSerializer
            public String value() {
                return value;
            }

            /**
             * Returns the case that holds the given string.
             *
             * @param value the string
             * @return the case
             */
            @TypeDeserializer
            static StringValue of(String value) {
                return new StringValue(value);
            }
        }

        /**
         * Represents a JSON boolean ({@code true} or {@code false} literal in JSON).
         *
         * <p><code>bool bool_value = 4;</code>
         *
         * @param value the boolean
         */
        record BoolValue(boolean value) implements Kind {
            /**
             * Returns the boolean of this case.
             *
             * @return the boolean
             */
            @TypeSerializer
            public boolean value() {
                return value;
            }

            /**
             * Returns the case that holds the given boolean.
             *
             * @param value the boolean
             * @return the case
             */
            @TypeDeserializer
            static BoolValue of(boolean value) {
                return new BoolValue(value);
            }
        }
    }
}
