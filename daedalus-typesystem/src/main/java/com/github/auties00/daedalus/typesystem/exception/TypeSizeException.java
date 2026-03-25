package com.github.auties00.daedalus.typesystem.exception;

/**
 * A checked exception thrown when the serialized size of a value cannot be
 * computed.
 *
 * @see TypeSerializationException
 * @see TypeDeserializationException
 */
public final class TypeSizeException extends TypeException {
    /**
     * Constructs a new {@code TypeSizeException} with the specified detail
     * message.
     *
     * @param message the detail message
     */
    public TypeSizeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TypeSizeException} with the specified detail
     * message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public TypeSizeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code TypeSizeException} with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public TypeSizeException(Throwable cause) {
        super(cause);
    }
}
