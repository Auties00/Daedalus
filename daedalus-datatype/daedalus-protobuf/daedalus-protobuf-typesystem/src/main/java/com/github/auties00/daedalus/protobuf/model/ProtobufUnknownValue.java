package com.github.auties00.daedalus.protobuf.model;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public sealed interface ProtobufUnknownValue {
    /**
     * A sealed interface of all possible values for an unknown field encountered during Protobuf parsing.
     *
     * @see ProtobufWireType
     */
    @SuppressWarnings("preview")
    sealed interface Binary extends ProtobufUnknownValue {
        /**
         * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_VAR_INT} wire type.
         *
         * @param value the decoded {@code long} value.
         */
        record VarInt(long value) implements Binary {

        }

        /**
         * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_FIXED64} wire type.
         *
         * @param value the decoded {@code long} value.
         */
        record Fixed64(long value) implements Binary {
            public static final Fixed64 POSITIVE_INFINITY = new Fixed64(Double.doubleToRawLongBits(Double.POSITIVE_INFINITY));
            public static final Fixed64 NEGATIVE_INFINITY = new Fixed64(Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));
            public static final Fixed64 NaN = new Fixed64(Double.doubleToRawLongBits(Double.NaN));
        }

        /**
         * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_LENGTH_DELIMITED} wire type.
         */
        sealed interface LengthDelimited extends Binary {
            /**
             * Returns a lazily decoded representation of the underlying byte data as a string.
             * The returned {@code StableValue} is not set until its value is first accessed.
             *
             * @return a {@code StableValue} containing the decoded {@code String}.
             */
            Supplier<String> asString();

            /**
             * Represents length-delimited data stored as a raw byte array.
             *
             * @param value the raw byte array data.
             */
            record ByteArrayBacked(byte[] value) implements LengthDelimited {
                /**
                 * Constructs a new {@code Bytes} record, ensuring the value is not null.
                 *
                 * @param value the raw byte array data.
                 */
                public ByteArrayBacked {
                    Objects.requireNonNull(value, "value cannot be null");
                }

                /**
                 * Returns a lazily decoded string representation wrapping the byte array.
                 *
                 * @return a {@code StableValue} containing the decoded {@code String}.
                 */
                @Override
                public Supplier<String> asString() {
                    return LazyConstant.of(() -> new String(value, StandardCharsets.UTF_8));
                }
            }

            /**
             * Represents length-delimited data stored as a {@link ByteBuffer}.
             *
             * @param value the {@code ByteBuffer} containing the data.
             */
            record ByteBufferBacked(ByteBuffer value) implements LengthDelimited {
                /**
                 * Constructs a new {@code Buffer} record, ensuring the value is not null.
                 *
                 * @param value the {@code ByteBuffer} containing the data.
                 */
                public ByteBufferBacked {
                    Objects.requireNonNull(value, "value cannot be null");
                }

                /**
                 * Returns a lazily decoded string representation wrapping the ByteBuffer.
                 *
                 * @return a {@code StableValue} containing the decoded {@code String}.
                 */
                @Override
                public Supplier<String> asString() {
                    return LazyConstant.of(() -> {
                        if (value.hasArray()) {
                            return new String(value.array(), value.arrayOffset() + value.position(), value.remaining(), StandardCharsets.UTF_8);
                        } else {
                            var copy = new byte[value.remaining()];
                            value.get(copy);
                            return new String(copy, StandardCharsets.UTF_8);
                        }
                    });
                }
            }

            /**
             * Represents length-delimited data stored as a {@link MemorySegment}.
             *
             * @param value the {@code MemorySegment} containing the data.
             * @param length the length of the {@code MemorySegment} containing the data.
             */
            record MemorySegmentBacked(MemorySegment value, int length) implements LengthDelimited {
                /**
                 * Constructs a new {@code AsMemorySegment} record, ensuring the value is not null.
                 *
                 * @param value the {@code MemorySegment} containing the data.
                 */
                public MemorySegmentBacked {
                    Objects.requireNonNull(value, "value cannot be null");
                }

                public MemorySegmentBacked(MemorySegment value) {
                    Objects.requireNonNull(value, "value cannot be null");
                    int length;
                    try {
                        length = Math.toIntExact(value.byteSize());
                    } catch (ArithmeticException _) {
                        throw new IllegalArgumentException("MemorySegment is too big to fit into a Protobuf message");
                    }
                    this(value, length);
                }

                /**
                 * Returns a lazily decoded string representation wrapping the MemorySegment.
                 *
                 * @return a {@code StableValue} containing the decoded {@code String}.
                 */
                @Override
                public Supplier<String> asString() {
                    return LazyConstant.of(() -> {
                        var heapBase = value.heapBase();
                        return heapBase.isPresent() && heapBase.get() instanceof byte[] array
                                ? new String(array, StandardCharsets.UTF_8)
                                : value.getString(0, StandardCharsets.UTF_8);
                    });
                }
            }
        }

        /**
         * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_START_OBJECT} wire type.
         *
         * <p>Like in the official protobuf library, the fields of a group are an unknown field
         * set of their own, which keeps every value of each field.
         *
         * @param value the unknown fields of the group
         */
        record Group(ProtobufUnknownFields value) implements Binary {
            /**
             * Constructs a new {@code Group} record, ensuring the store is not null.
             *
             * @param value the unknown fields of the group
             * @throws NullPointerException if {@code value} is {@code null}
             */
            public Group {
                Objects.requireNonNull(value, "value cannot be null");
            }
        }

        /**
         * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_FIXED32} wire type.
         *
         * @param value the decoded {@code int} value.
         */
        record Fixed32(int value) implements Binary {
            public static final Fixed32 POSITIVE_INFINITY = new Fixed32(Float.floatToRawIntBits(Float.POSITIVE_INFINITY));
            public static final Fixed32 NEGATIVE_INFINITY = new Fixed32(Float.floatToRawIntBits(Float.NEGATIVE_INFINITY));
            public static final Fixed32 NaN = new Fixed32(Float.floatToRawIntBits(Float.NaN));
        }
    }

    /**
     * A sealed interface of all possible values for an unknown field encountered while parsing
     * the text format of Protocol Buffers.
     *
     * <p>Unlike {@link Binary}, which models the wire types of the binary format, this interface
     * models the values of the text format, which cannot be interpreted without the schema of the
     * field: an unquoted scalar, which can be a number, a boolean or the name of an enum constant,
     * a quoted string of bytes, which can be a {@code string} or a {@code bytes} value, or a
     * message. The values of a list are separate values of their field.
     *
     * @see ProtobufUnknownFields
     */
    sealed interface Text extends ProtobufUnknownValue {
        /**
         * Represents an unknown value that is an unquoted scalar, kept exactly as it was written,
         * including its sign, like {@code 42}, {@code -0x1F}, {@code 1e-5}, {@code -inf},
         * {@code true} or {@code RED}.
         *
         * @param value the text of the scalar
         */
        record Scalar(String value) implements Text {
            /**
             * Constructs a new scalar, ensuring the value is not null.
             *
             * @param value the text of the scalar
             * @throws NullPointerException if {@code value} is {@code null}
             */
            public Scalar {
                Objects.requireNonNull(value, "value cannot be null");
            }
        }

        /**
         * Represents an unknown value that is a quoted string, whose escape sequences are decoded
         * as bytes, and whose adjacent strings are concatenated.
         *
         * <p>Two values are equal if they hold the same bytes.
         *
         * @param value the bytes of the string
         */
        record Bytes(byte[] value) implements Text {
            /**
             * Constructs a new string of bytes, ensuring the value is not null.
             *
             * @param value the bytes of the string
             * @throws NullPointerException if {@code value} is {@code null}
             */
            public Bytes {
                Objects.requireNonNull(value, "value cannot be null");
            }

            /**
             * Returns the bytes of the string decoded as UTF-8, which is the value of a
             * {@code string} field.
             *
             * @return the decoded string
             */
            public String asString() {
                return new String(value, StandardCharsets.UTF_8);
            }

            /**
             * Returns whether the given object is a string of the same bytes.
             *
             * @param object the object to compare with
             * @return {@code true} if the object holds the same bytes
             */
            @Override
            public boolean equals(Object object) {
                return object instanceof Bytes that && Arrays.equals(value, that.value);
            }

            /**
             * Returns a hash code consistent with {@link #equals(Object)}.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Arrays.hashCode(value);
            }

            /**
             * Returns a string representation of the bytes.
             *
             * @return a string representation of the bytes
             */
            @Override
            public String toString() {
                return "Bytes[value=" + HexFormat.of().formatHex(value) + "]";
            }
        }

        /**
         * Represents an unknown value that is a message, enclosed in braces or angle brackets,
         * whose fields are unknown fields of their own.
         *
         * @param value the fields of the message
         */
        record Message(ProtobufUnknownFields value) implements Text {
            /**
             * Constructs a new message, ensuring the store is not null.
             *
             * @param value the fields of the message
             * @throws NullPointerException if {@code value} is {@code null}
             */
            public Message {
                Objects.requireNonNull(value, "value cannot be null");
            }
        }
    }
}
