package com.github.auties00.daedalus.protobuf.compiler.exception;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A localizable description of a problem found while compiling a Protocol Buffer definition,
 * without any information about where the problem is located.
 *
 * <p>A message is a template made of a {@linkplain Kind kind}, a resource bundle key and the raw
 * arguments that are substituted into the localized text. Instances are never created directly by
 * the compiler: every message is obtained from one of the factory classes
 * {@link ProtobufLexicalErrors}, {@link ProtobufSyntaxErrors} or {@link ProtobufSemanticErrors}, each
 * of which documents the bundle key used by its methods.
 *
 * <p>The text is resolved lazily by {@link #format(Locale)}, so the same message can be rendered in
 * any locale. Keys are looked up in the {@code diagnostics} resource bundle of this package using
 * {@link MessageFormat}; when the bundle also defines an entry named after the key followed by
 * {@value #HELP_SUFFIX}, that entry is appended as a help section.
 *
 * <p>This record is immutable and thread safe, provided that its arguments are.
 *
 * @param kind the phase of the compiler that reported the message, never {@code null}
 * @param key the resource bundle key of the message text, never {@code null}
 * @param args the unmodifiable arguments substituted into the message text, never {@code null}
 * @see ProtobufDiagnostic
 */
public record ProtobufDiagnosticMessage(Kind kind, String key, List<Object> args) {
    /**
     * The base name of the resource bundle containing the text of every message.
     */
    static final String BUNDLE_NAME = "com.github.auties00.daedalus.protobuf.compiler.exception.diagnostics";

    /**
     * The suffix appended to a message key to obtain the key of its optional help text.
     */
    static final String HELP_SUFFIX = ".help";

    /**
     * The bundle key used to render the help section of a message.
     */
    private static final String HELP_KEY = "diagnostic.help";

    /**
     * Constructs a new message, validating its components.
     *
     * @param kind the phase of the compiler that reported the message
     * @param key the resource bundle key of the message text
     * @param args the arguments substituted into the message text
     * @throws NullPointerException if {@code kind}, {@code key} or {@code args} is {@code null}
     */
    public ProtobufDiagnosticMessage {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(args, "args cannot be null");
    }

    /**
     * Creates a new message from the given kind, key and arguments.
     *
     * <p>The arguments may contain {@code null} elements, which are rendered as the string
     * {@code "null"}.
     *
     * @param kind the phase of the compiler that reported the message
     * @param key the resource bundle key of the message text
     * @param args the arguments substituted into the message text
     * @return a new message
     */
    static ProtobufDiagnosticMessage of(Kind kind, String key, Object... args) {
        return new ProtobufDiagnosticMessage(kind, key, Collections.unmodifiableList(Arrays.asList(args)));
    }

    /**
     * Returns the text of this message localized for the given locale, followed by its help section
     * if the resource bundle defines one.
     *
     * <p>If no bundle exists for {@code locale}, the root bundle, which is written in English, is
     * used instead.
     *
     * @param locale the locale used to resolve the text and to format the arguments
     * @return the localized text of this message
     * @throws NullPointerException if {@code locale} is {@code null}
     * @throws java.util.MissingResourceException if the bundle does not define {@link #key()}
     */
    public String format(Locale locale) {
        var bundle = bundle(locale);
        var arguments = args.toArray();
        var text = new MessageFormat(bundle.getString(key), locale).format(arguments);
        var helpKey = key + HELP_SUFFIX;
        if (!bundle.containsKey(helpKey)) {
            return text;
        }

        var help = new MessageFormat(bundle.getString(helpKey), locale).format(arguments);
        return text + "\n\n" + new MessageFormat(bundle.getString(HELP_KEY), locale).format(new Object[]{help});
    }

    /**
     * Returns the text of this message localized for the {@linkplain Locale#getDefault() default
     * locale}.
     *
     * @return the localized text of this message
     */
    @Override
    public String toString() {
        return format(Locale.getDefault());
    }

    /**
     * Returns the diagnostics resource bundle for the given locale.
     *
     * @param locale the locale of the bundle
     * @return the resource bundle
     */
    static ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    /**
     * Formats the bundle entry with the given key for the given locale.
     *
     * @param locale the locale used to resolve the text and to format the arguments
     * @param key the key of the bundle entry
     * @param args the arguments substituted into the entry
     * @return the formatted entry
     */
    static String localize(Locale locale, String key, Object... args) {
        return new MessageFormat(bundle(locale).getString(key), locale).format(args);
    }

    /**
     * A phase of the compiler, such as 'syntax', that can report a {@link ProtobufDiagnosticMessage}.
     *
     * <p>The kind of a message determines how the compiler recovers from it: documents with
     * {@link #LEXICAL} or {@link #SYNTAX} problems are not semantically analyzed, while
     * {@link #SEMANTIC} problems never prevent the analysis of other declarations.
     */
    public enum Kind {
        /**
         * A problem found while splitting the source text into tokens, such as an unterminated
         * string literal.
         */
        LEXICAL("diagnostic.kind.lexical"),

        /**
         * A problem found while matching tokens against the grammar of the language, such as a
         * missing semicolon.
         */
        SYNTAX("diagnostic.kind.syntax"),

        /**
         * A problem found in a grammatically valid definition, such as a duplicate field number or
         * an unresolved type.
         */
        SEMANTIC("diagnostic.kind.semantic");

        /**
         * The bundle key of the localized name of this kind.
         */
        private final String key;

        /**
         * Constructs a new kind with the given bundle key.
         *
         * @param key the bundle key of the localized name of this kind
         */
        Kind(String key) {
            this.key = key;
        }

        /**
         * Returns the name of this kind localized for the given locale.
         *
         * @param locale the locale of the name
         * @return the localized name of this kind
         */
        public String format(Locale locale) {
            return localize(locale, key);
        }
    }
}
