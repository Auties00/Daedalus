package com.github.auties00.daedalus.processor.exception;

import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A checked exception that serves as the base for the errors of the annotation processor
 * that are not reported as diagnostics of the compilation, such as a failure to access
 * the internals of the compiler.
 *
 * <p>Subclasses create their instances through static factory methods, so that the text
 * of every message is kept in a localizable resource bundle instead of being built at the
 * call site. The detail message is resolved lazily from the {@code messages} resource
 * bundle of this package, using the {@linkplain Locale#getDefault() default locale}.
 */
public abstract class DaedalusProcessorException extends Exception {
    /**
     * The base name of the resource bundle containing the text of the messages of the
     * exceptions in this package.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.processor.exception.messages";

    /**
     * The resource bundle key of the message of this exception.
     */
    private final String key;

    /**
     * The arguments substituted into the message of this exception.
     */
    private final Object[] args;

    /**
     * Constructs a new {@code DaedalusProcessorException} whose message is resolved from the
     * resource bundle of this package.
     *
     * @param key the resource bundle key of the message, which must not be {@code null}
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     * @param args the arguments substituted into the message
     * @throws NullPointerException if {@code key} is {@code null}
     */
    protected DaedalusProcessorException(String key, Throwable cause, Object... args) {
        super(null, cause);
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.args = args;
    }

    /**
     * Returns a description of the given cause that can be embedded in a message.
     *
     * <p>The message of a {@code DaedalusProcessorException} or of a {@link TypeReflectionException}
     * is already a complete sentence, so it is returned as is. Any other throwable is described by its class and its message,
     * as the message of many exceptions of the JDK is meaningless without its class: for
     * example, the message of a {@link java.nio.file.NoSuchFileException} is only the path
     * of the missing file.
     *
     * @param cause the cause to describe, or {@code null}
     * @return the description of the cause, or {@code "unknown cause"} if it is {@code null}
     */
    public static String describe(Throwable cause) {
        if (cause == null) {
            return "unknown cause";
        }

        if (cause instanceof DaedalusProcessorException || cause instanceof TypeReflectionException) {
            return cause.getMessage();
        }

        return cause.toString();
    }

    /**
     * Returns the detail message of this exception localized for the given locale.
     *
     * <p>If no resource bundle exists for {@code locale}, the root bundle, which is
     * written in English, is used instead.
     *
     * @param locale the locale used to resolve the message and to format its arguments
     * @return the localized detail message
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public String format(Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }

    /**
     * Returns the detail message of this exception localized for the
     * {@linkplain Locale#getDefault() default locale}.
     *
     * @return the detail message
     */
    @Override
    public String getMessage() {
        return format(Locale.getDefault());
    }
}
