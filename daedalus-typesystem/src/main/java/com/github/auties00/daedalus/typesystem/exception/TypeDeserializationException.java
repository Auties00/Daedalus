package com.github.auties00.daedalus.typesystem.exception;

/**
 * A checked exception thrown when a value cannot be deserialized from its
 * encoded representation into the expected type.
 *
 * @see TypeSerializationException
 * @see TypeSizeException
 */
public final class TypeDeserializationException extends TypeException {
    /**
     * Constructs a new {@code TypeDeserializationException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public TypeDeserializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TypeDeserializationException} with the specified
     * detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public TypeDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code TypeDeserializationException} with the specified
     * cause.
     *
     * @param cause the cause of this exception
     */
    public TypeDeserializationException(Throwable cause) {
        super(cause);
    }
}
