package com.github.auties00.daedalus.typesystem.exception;

/**
 * A checked exception thrown when a value cannot be serialized into its
 * encoded representation.
 *
 * @see TypeDeserializationException
 * @see TypeSizeException
 */
public final class TypeSerializationException extends TypeException {
    /**
     * Constructs a new {@code TypeSerializationException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public TypeSerializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TypeSerializationException} with the specified
     * detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public TypeSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code TypeSerializationException} with the specified
     * cause.
     *
     * @param cause the cause of this exception
     */
    public TypeSerializationException(Throwable cause) {
        super(cause);
    }
}
