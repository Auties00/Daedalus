package com.github.auties00.daedalus.typesystem.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

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
     * The base name of the resource bundle containing the text of the messages created
     * by the static factory methods of the exceptions in this package.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.typesystem.exception.messages";

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

    /**
     * Formats the entry with the given key of the {@code messages} resource bundle of
     * this package for the {@linkplain Locale#getDefault() default locale}.
     *
     * <p>This method is used by the static factory methods of the subclasses, which
     * document the key they use.
     *
     * @param key the key of the bundle entry
     * @param args the arguments substituted into the entry
     * @return the formatted entry
     */
    static String localize(String key, Object... args) {
        var locale = Locale.getDefault();
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }
}
