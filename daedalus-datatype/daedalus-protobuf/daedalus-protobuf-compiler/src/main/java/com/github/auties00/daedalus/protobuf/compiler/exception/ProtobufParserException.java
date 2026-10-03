package com.github.auties00.daedalus.protobuf.compiler.exception;

import com.github.auties00.daedalus.protobuf.exception.ProtobufException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A checked exception thrown when the compilation of one or more Protocol Buffer definitions
 * produces at least one {@link ProtobufDiagnostic}.
 *
 * <p>The compiler does not stop at the first problem it finds. The lexer, the parser and the
 * analyzer each report problems and recover from them, so a single exception carries every
 * diagnostic found during a compilation, in the order in which they were reported. The
 * {@linkplain ProtobufDiagnosticMessage.Kind kind} of each diagnostic identifies the phase that
 * reported it.
 *
 * <p>The detail message of this exception is computed lazily from its diagnostics using the
 * {@linkplain Locale#getDefault() default locale}. Callers that need a different locale should
 * render each diagnostic with {@link ProtobufDiagnostic#format(Locale)}.
 *
 * @see ProtobufDiagnostic
 */
public final class ProtobufParserException extends ProtobufException {
    /**
     * The diagnostics reported during the compilation.
     */
    private final List<ProtobufDiagnostic> diagnostics;

    /**
     * Constructs a new {@code ProtobufParserException} carrying the given diagnostics.
     *
     * <p>The list is copied defensively.
     *
     * @param diagnostics the diagnostics reported during the compilation, which must not be
     *                    {@code null} or empty
     * @throws NullPointerException if {@code diagnostics} is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code diagnostics} is empty
     */
    public ProtobufParserException(List<ProtobufDiagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics cannot be null");
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("diagnostics cannot be empty");
        }
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Returns the diagnostics reported during the compilation, in the order in which they were
     * reported.
     *
     * @return an unmodifiable, non empty list of diagnostics
     */
    public List<ProtobufDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns every diagnostic rendered for the given locale, followed by the number of
     * diagnostics.
     *
     * <p>A diagnostic can span several lines, for example when it includes a help section, so
     * consecutive diagnostics, and the final count, are separated by an empty line.
     *
     * @param locale the locale used to render the diagnostics
     * @return the rendered diagnostics
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    @Override
    public String format(Locale locale) {
        var result = new StringBuilder();
        for (var diagnostic : diagnostics) {
            result.append(diagnostic.format(locale))
                    .append("\n\n");
        }
        result.append(ProtobufDiagnosticMessage.localize(locale, "diagnostic.count", diagnostics.size()));
        return result.toString();
    }
}
