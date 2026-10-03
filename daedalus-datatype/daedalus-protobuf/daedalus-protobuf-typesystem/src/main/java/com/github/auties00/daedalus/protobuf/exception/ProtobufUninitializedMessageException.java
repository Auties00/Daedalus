package com.github.auties00.daedalus.protobuf.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * An unchecked exception thrown when a message is built from a builder that does not set one
 * of the required fields of the message.
 *
 * <p>The {@code build} method of a generated builder checks the required fields of its
 * message, and of every nested message built by it, and throws this exception for the first
 * one that is not set. The {@code buildPartial} method of the same builder skips the check.
 * When a message is decoded, the generated {@code decode} method converts this exception
 * into a {@link ProtobufDeserializationException}, as a missing required field means that the
 * input is not a valid encoding of the message.
 *
 * <p>Instances are created exclusively through the static factory methods of this class, each
 * of which documents the key of the {@code messages} resource bundle entry that holds its
 * text.
 *
 * @apiNote
 * This exception is unchecked, like the {@code UninitializedMessageException} of the official
 * protobuf library, because setting every required field before building a message is a
 * precondition that the caller controls.
 */
public final class ProtobufUninitializedMessageException extends RuntimeException {
    /**
     * The base name of the resource bundle containing the text of the messages of the
     * exceptions in this package.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.protobuf.exception.messages";

    /**
     * The resource bundle key of the message of this exception.
     */
    private final String key;

    /**
     * The name of the message whose required field is not set.
     */
    private final String messageName;

    /**
     * The name of the required field that is not set.
     */
    private final String fieldName;

    /**
     * Constructs a new {@code ProtobufUninitializedMessageException} with the given message key.
     *
     * @param key the resource bundle key of the message
     * @param messageName the name of the message whose required field is not set
     * @param fieldName the name of the required field that is not set
     */
    private ProtobufUninitializedMessageException(String key, String messageName, String fieldName) {
        super(null, null);
        this.key = key;
        this.messageName = messageName;
        this.fieldName = fieldName;
    }

    /**
     * Creates a new {@code ProtobufUninitializedMessageException} indicating that a message
     * cannot be built because one of its required fields is not set.
     *
     * <p>Bundle key: {@code uninitialized.missing.required.field}.
     *
     * @param messageName the name of the message whose required field is not set
     * @param fieldName the name of the required field that is not set
     * @return a {@code ProtobufUninitializedMessageException} with a detailed message
     *         specifying the message and the missing field
     * @throws NullPointerException if {@code messageName} or {@code fieldName} is {@code null}
     */
    @SuppressWarnings("unused") // Used by ProtobufBuilderTypeGenerator
    public static ProtobufUninitializedMessageException missingRequiredField(String messageName, String fieldName) {
        Objects.requireNonNull(messageName, "messageName cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        return new ProtobufUninitializedMessageException("uninitialized.missing.required.field", messageName, fieldName);
    }

    /**
     * Returns the name of the message whose required field is not set.
     *
     * @return the name of the message
     */
    public String messageName() {
        return messageName;
    }

    /**
     * Returns the name of the required field that is not set.
     *
     * @return the name of the field
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Returns the detail message of this exception localized for the given locale.
     *
     * <p>If no resource bundle exists for {@code locale}, the root bundle, which is written in
     * English, is used instead.
     *
     * @param locale the locale used to resolve the message and to format its arguments
     * @return the localized detail message
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public String format(Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(new Object[]{messageName, fieldName});
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
