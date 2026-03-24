package com.github.auties00.daedalus.protobuf.exception;

import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;

/**
 * Represents an exception that occurs during the deserialization of a Protocol Buffers message.
 * This class extends {@link ProtobufException} and provides various static methods to create
 * specific exceptions related to deserialization errors.
 */
public final class ProtobufDeserializationException extends ProtobufException {
    /**
     * Constructs a new ProtobufDeserializationException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ProtobufDeserializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ProtobufDeserializationException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the cause of the exception
     */
    public ProtobufDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message ended unexpectedly.
     *
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the message
     *         ended unexpectedly during deserialization
     */
    public static ProtobufDeserializationException truncatedMessage() {
        return new ProtobufDeserializationException("A message ended unexpectedly");
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message ended unexpectedly.
     *
     * @param cause the cause
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the message
     *         ended unexpectedly during deserialization
     */
    public static ProtobufDeserializationException truncatedMessage(Throwable cause) {
        return new ProtobufDeserializationException("A message ended unexpectedly", cause);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message contains a malformed var int.
     *
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the var int is malformed
     */
    public static ProtobufDeserializationException malformedVarInt() {
        return new ProtobufDeserializationException("A message contained a malformed var int");
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that the wire type in the serialized
     * message is invalid.
     *
     * @param wireType the invalid wire type that was encountered during deserialization
     * @return a {@code ProtobufDeserializationException} with a predefined message specifying the invalid wire type
     */
    public static ProtobufDeserializationException invalidWireType(int wireType) {
        return new ProtobufDeserializationException("A message contained an invalid wire type: %s".formatted(wireType));
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a group in a serialized protobuf
     * message was improperly terminated. This occurs when the group was closed with a field index that
     * does not match the field index of the previously opened group.
     *
     * @param actualFieldIndex   the actual field index encountered when the group was closed
     * @param expectedFieldIndex the expected field index corresponding to the previously opened group
     * @return a {@code ProtobufDeserializationException} with a detailed message explaining the mismatch
     */
    public static ProtobufDeserializationException invalidEndObject(long actualFieldIndex, long expectedFieldIndex) {
        return new ProtobufDeserializationException("A message closed a group with index %s, but the previously opened group had index %s".formatted(actualFieldIndex, expectedFieldIndex));
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message
     * expected a group to begin but did not encounter a valid start object at the specified field index.
     *
     * @param fieldIndex the index of the field where the start of the group was expected
     * @return a {@code ProtobufDeserializationException} with a predefined message identifying the problem at the specified field index
     */
    public static ProtobufDeserializationException invalidStartObject(long fieldIndex) {
        return new ProtobufDeserializationException("A message expected a group to open at index " + fieldIndex);
    }

    /**
     * Creates a new ProtobufDeserializationException indicating that a message opened a group but failed
     * to close it. This typically happens when a well-formed group is not properly terminated in the serialized
     * protobuf data.
     *
     * @return a ProtobufDeserializationException with a predefined message describing the error
     */
    public static ProtobufDeserializationException malformedGroup() {
        return new ProtobufDeserializationException("A message opened a group but didn't close it");
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message specified
     * a negative length for a length-delimited field during deserialization.
     *
     * @param size the negative length value that was encountered
     * @return a {@code ProtobufDeserializationException} with a message explaining the error
     */
    public static ProtobufDeserializationException negativeLength(long size) {
        return new ProtobufDeserializationException("A message specified a negative block length for a length-delimited field: " + size);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message specified
     * an invalid field index during deserialization.
     *
     * @param index the invalid field index that was encountered
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the invalid index
     */
    public static ProtobufDeserializationException invalidFieldIndex(long index) {
        return new ProtobufDeserializationException("A message specified an invalid field index: " + index);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that the specified field index
     * is marked as reserved in the protobuf schema and cannot be used.
     *
     * @param index the reserved index that was encountered during deserialization
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the reserved index
     */
    @SuppressWarnings("unused") // Used by ProtobufObjectDeserializationGenerator
    public static ProtobufDeserializationException reservedIndex(long index) {
        return new ProtobufDeserializationException(index + " is marked as reserved");
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a deserialized
     * property is in an invalid state.
     *
     * @param reason the description of the invalid property state
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the invalid state
     */
    public static ProtobufDeserializationException invalidPropertyState(String reason) {
        return new ProtobufDeserializationException("Invalid property state: " + reason);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a length delimited property was too large to be deserialized.
     * <p>
     * This can happen, for example, if a caller attempts to read a length delimited property with a {@code length > Integer.MAX_VALUE} from a {@link ProtobufBinaryReader} backed by a {@code byte[]}.
     *
     * @param length the length of the length delimited property that was too large to be deserialized
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the invalid state
     */
    public static ProtobufDeserializationException lengthDelimitedPropertyOverflow(long length) {
        return new ProtobufDeserializationException("Message size(" + length + ") is too large to be deserialized");
    }
}
