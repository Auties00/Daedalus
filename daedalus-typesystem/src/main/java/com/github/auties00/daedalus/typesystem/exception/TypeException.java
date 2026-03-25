package com.github.auties00.daedalus.typesystem.exception;

/**
 * An abstract checked exception that serves as the base for all exceptions
 * thrown by the Daedalus type system.
 *
 * <p>Concrete subclasses represent specific failure modes:
 * <ul>
 * <li>{@link TypeDeserializationException} — failure during deserialization
 * <li>{@link TypeSerializationException} — failure during serialization
 * <li>{@link TypeSizeException} — failure during size computation
 * </ul>
 *
 * @see TypeDeserializationException
 * @see TypeSerializationException
 * @see TypeSizeException
 */
public sealed abstract class TypeException extends Exception permits TypeDeserializationException, TypeSerializationException, TypeSizeException {
    /**
     * Constructs a new {@code TypeException} with the specified detail message.
     *
     * @param message the detail message
     */
    public TypeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TypeException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code TypeException} with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public TypeException(Throwable cause) {
        super(cause);
    }
}
