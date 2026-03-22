package it.auties.protobuf.model;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A sealed interface of all possible values for an unknown field encountered during Protobuf parsing.
 *
 * @see ProtobufWireType
 */
@SuppressWarnings("preview")
public sealed interface ProtobufUnknownValue {
    /**
     * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_VAR_INT} wire type.
     *
     * @param value the decoded {@code long} value.
     */
    record VarInt(long value) implements ProtobufUnknownValue {

    }

    /**
     * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_FIXED64} wire type.
     *
     * @param value the decoded {@code long} value.
     */
    record Fixed64(long value) implements ProtobufUnknownValue {

    }

    /**
     * Represents an unknown field value that has {@link ProtobufWireType#WIRE_TYPE_LENGTH_DELIMITED} wire type.
     */
    sealed interface LengthDelimited extends ProtobufUnknownValue {
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
     */
    record Group(Map<Long, ProtobufUnknownValue> value) implements ProtobufUnknownValue {
        /**
         * Constructs a new {@code Group} record, ensuring the map is not null.
         *
         * @param value the map of field numbers to inner unknown values.
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
    record Fixed32(int value) implements ProtobufUnknownValue {

    }
}