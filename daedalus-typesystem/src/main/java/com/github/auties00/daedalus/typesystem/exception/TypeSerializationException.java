package com.github.auties00.daedalus.typesystem.exception;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    /**
     * Returns an exception reporting that a value cannot be expressed as a
     * {@code long} count of the given unit without overflowing.
     *
     * <p>The message is resolved from the {@code serialization.overflow} key.
     *
     * @param value the value that was serialized
     * @param unit the unit of the count
     * @return a new exception
     */
    public static TypeSerializationException overflow(Object value, TimeUnit unit) {
        return new TypeSerializationException(localize("serialization.overflow", String.valueOf(value), unit));
    }

    /**
     * Returns an exception reporting that a value of the given type cannot be encoded,
     * for example because it does not expose its encoded form.
     *
     * <p>The message is resolved from the {@code serialization.unencodableValue} key.
     *
     * @param type the type of the value that was serialized
     * @param cause the error that prevented the encoding, or {@code null} if unknown
     * @return a new exception
     */
    public static TypeSerializationException unencodableValue(Class<?> type, Throwable cause) {
        return new TypeSerializationException(localize("serialization.unencodableValue", type.getName()), cause);
    }

    /**
     * Returns an exception reporting that the flags of a pattern cannot be
     * represented by the string form it is serialized to.
     *
     * <p>The message is resolved from the {@code serialization.patternFlags} key.
     *
     * @param pattern the pattern that was serialized
     * @return a new exception
     */
    public static TypeSerializationException patternFlags(Pattern pattern) {
        return new TypeSerializationException(localize("serialization.patternFlags", pattern.pattern(), String.valueOf(pattern.flags())));
    }
}
