package com.github.auties00.daedalus.protobuf.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A checked exception that serves as the base for errors related to Protocol Buffer processing,
 * such as serialization, deserialization, or the compilation of Protocol Buffer definitions.
 *
 * <p>This exception is designed to be extended by more specific exception classes that provide
 * detailed context about each kind of problem. Subclasses are expected to create their instances
 * through static factory methods, so that the text of every message is kept in a localizable
 * resource bundle instead of being built at the call site.
 *
 * <p>The detail message of a {@code ProtobufException} created by
 * {@link #ProtobufException(String, Throwable, Object...)} is resolved lazily from the
 * {@code messages} resource bundle of this package, using the
 * {@linkplain Locale#getDefault() default locale}.
 */
public abstract class ProtobufException extends Exception {
    /**
     * The base name of the resource bundle containing the text of the messages of the exceptions
     * in this package.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.protobuf.exception.messages";

    /**
     * The resource bundle key of the message of this exception, or {@code null} if the subclass
     * computes its own message.
     */
    private final String key;

    /**
     * The arguments substituted into the message of this exception.
     */
    private final Object[] args;

    /**
     * Constructs a new {@code ProtobufException} whose message is computed by the subclass.
     *
     * <p>Subclasses using this constructor must override {@link #getMessage()}.
     */
    protected ProtobufException() {
        super(null, null);
        this.key = null;
        this.args = null;
    }

    /**
     * Constructs a new {@code ProtobufException} whose message is resolved from the resource bundle
     * of this package.
     *
     * @param key the resource bundle key of the message, which must not be {@code null}
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     * @param args the arguments substituted into the message
     * @throws NullPointerException if {@code key} is {@code null}
     */
    protected ProtobufException(String key, Throwable cause, Object... args) {
        super(null, cause);
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.args = args;
    }

    /**
     * Returns the detail message of this exception localized for the given locale.
     *
     * <p>If no resource bundle exists for {@code locale}, the root bundle, which is written in
     * English, is used instead.
     *
     * @param locale the locale used to resolve the message and to format its arguments
     * @return the localized detail message, or {@code null} if this exception has no message key
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public String format(Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        if (key == null) {
            return null;
        }

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
