package com.github.auties00.daedalus.protobuf.compiler.number;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents an integer value in a Protocol Buffer definition.
 * <p>
 * This record encapsulates integer literals parsed from Protocol Buffer files, supporting
 * various integer formats:
 * </p>
 * <ul>
 *   <li>Decimal integers: {@code 42}, {@code -123}, {@code 0}</li>
 *   <li>Hexadecimal integers: {@code 0x2A}, {@code 0xFF}, {@code 0X10}</li>
 *   <li>Octal integers: {@code 052}, {@code 0177}, {@code 01}</li>
 * </ul>
 *
 * @param value the arbitrary-precision integer value
 */
public record ProtobufInteger(long value) implements ProtobufNumber {
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public OptionalLong toFieldIndex() {
        return value >= ProtobufMessage.MIN_FIELD_INDEX && value <= ProtobufMessage.MAX_FIELD_INDEX
                ? OptionalLong.of(value)
                : OptionalLong.empty();
    }

    @Override
    public OptionalInt toEnumConstant() {
        return value >= ProtobufEnum.MIN_CONSTANT_INDEX && value <= ProtobufEnum.MAX_CONSTANT_INDEX
                ? OptionalInt.of((int) value)
                : OptionalInt.empty();
    }

    @Override
    public int compareTo(ProtobufNumber other) {
        return switch (other) {
            case ProtobufFloatingPoint otherFloatingPoint -> switch (otherFloatingPoint) {
                // TODO: Maybe optimize me
                case ProtobufFloatingPoint.Finite(var otherValue) -> Double.compare(value, otherValue);
                case ProtobufFloatingPoint.Infinity(var signum) -> switch (signum) {
                    case POSITIVE -> -1;
                    case NEGATIVE -> 1;
                };
                case ProtobufFloatingPoint.NaN _ -> 1;
            };
            case ProtobufInteger(var otherValue) -> Long.compare(value, otherValue);
        };
    }
}
