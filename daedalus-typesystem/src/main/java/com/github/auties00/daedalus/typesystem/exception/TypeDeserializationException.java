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

    /**
     * Returns an exception reporting that an encoded value is not a valid
     * representation of the given type.
     *
     * <p>The message is resolved from the {@code deserialization.malformedValue} key.
     *
     * @param type the type that the value was deserialized into
     * @param value the encoded value
     * @param cause the error that rejected the value, or {@code null} if unknown
     * @return a new exception
     */
    public static TypeDeserializationException malformedValue(Class<?> type, Object value, Throwable cause) {
        return new TypeDeserializationException(localize("deserialization.malformedValue", type.getName(), String.valueOf(value)), cause);
    }

    /**
     * Returns an exception reporting that an encoded number does not fit in the
     * range of the given type.
     *
     * <p>The message is resolved from the {@code deserialization.outOfRange} key.
     *
     * @param type the type that the value was deserialized into
     * @param value the encoded number
     * @return a new exception
     */
    public static TypeDeserializationException outOfRange(Class<?> type, long value) {
        return new TypeDeserializationException(localize("deserialization.outOfRange", type.getName(), String.valueOf(value)));
    }
}
