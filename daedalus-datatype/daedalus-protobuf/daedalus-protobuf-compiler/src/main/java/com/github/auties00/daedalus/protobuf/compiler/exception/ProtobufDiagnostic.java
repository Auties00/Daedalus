package com.github.auties00.daedalus.protobuf.compiler.exception;

import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourceRange;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * A problem found while compiling a Protocol Buffer definition, together with the location of the
 * source text that caused it.
 *
 * <p>Diagnostics are collected by the compiler instead of being thrown one at a time, so that a
 * single compilation can report every problem it finds. They are delivered to callers through
 * {@link ProtobufParserException#diagnostics()}.
 *
 * <p>This record is immutable and thread safe.
 *
 * @param message the localizable description of the problem, never {@code null}
 * @param location the file that contains the problem, or {@code null} if the source was not read
 *                 from a file
 * @param range the source range that contains the problem, never {@code null}; it is
 *              {@link ProtobufTreeSourceRange#UNKNOWN} when the problem is not tied to a specific
 *              part of the source
 * @see ProtobufDiagnosticMessage
 */
public record ProtobufDiagnostic(ProtobufDiagnosticMessage message, Path location, ProtobufTreeSourceRange range) {
    /**
     * Constructs a new diagnostic, validating its components.
     *
     * @param message the localizable description of the problem
     * @param location the file that contains the problem, or {@code null}
     * @param range the source range that contains the problem
     * @throws NullPointerException if {@code message} or {@code range} is {@code null}
     */
    public ProtobufDiagnostic {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(range, "range cannot be null");
    }

    /**
     * Returns this diagnostic rendered for the given locale in the conventional
     * {@code file:line:column: kind: message} format used by {@code javac} and {@code protoc}.
     *
     * <p>The line and column are omitted when {@link #range()} is unknown.
     *
     * @param locale the locale used to resolve the text of the diagnostic
     * @return the rendered diagnostic
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public String format(Locale locale) {
        var file = location == null
                ? ProtobufDiagnosticMessage.localize(locale, "diagnostic.location.input")
                : location.toString();
        var kind = message.kind().format(locale);
        var text = message.format(locale);
        var start = range.start();
        if (start.line() <= 0) {
            return ProtobufDiagnosticMessage.localize(locale, "diagnostic.format.nopos", file, kind, text);
        }

        return ProtobufDiagnosticMessage.localize(locale, "diagnostic.format", file, String.valueOf(start.line()), String.valueOf(start.column()), kind, text);
    }

    /**
     * Returns this diagnostic rendered for the {@linkplain Locale#getDefault() default locale}.
     *
     * @return the rendered diagnostic
     * @see #format(Locale)
     */
    @Override
    public String toString() {
        return format(Locale.getDefault());
    }
}
