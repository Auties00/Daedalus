package com.github.auties00.daedalus.protobuf.exception;

/**
 * Represents an exception that occurs during the serialization of a Protocol Buffers message.
 * This class extends {@link ProtobufException} and provides static methods for creating
 * specific exceptions related to serialization errors.
 */
public final class ProtobufSerializationException extends ProtobufException {
    /**
     * Constructs a new {@code ProtobufSerializationException} with the specified detail message.
     *
     * @param message the detail message explaining the reason for the serialization exception
     */
    public ProtobufSerializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ProtobufSerializationException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the serialization exception
     * @param cause the cause of the exception
     */
    public ProtobufSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a size calculation error
     * occurred because there is not enough space remaining for the message after serialization.
     * This exception is intended to report underlying issues in the library, so it should hopefully never be thrown.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException underflow() {
        return new ProtobufSerializationException("Underflow");
    }
    /**
     * Creates a {@code ProtobufSerializationException} indicating that a size calculation error
     * occurred because there is not enough space remaining for the message after serialization.
     * This exception is intended to report underlying issues in the library, so it should hopefully never be thrown.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException overflow() {
        return new ProtobufSerializationException("Overflow");
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that an attempt was made
     * to write a length-delimited block with a negative length.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException negativeLength() {
        return new ProtobufSerializationException("Cannot write a length delimited block with a negative length");
    }
}
