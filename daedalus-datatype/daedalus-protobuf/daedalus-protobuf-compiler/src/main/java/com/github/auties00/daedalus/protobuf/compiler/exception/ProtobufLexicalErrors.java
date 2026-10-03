package com.github.auties00.daedalus.protobuf.compiler.exception;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage.Kind;

/**
 * Static factory methods for the {@linkplain Kind#LEXICAL lexical} diagnostic messages reported
 * while splitting a Protocol Buffer definition into tokens.
 *
 * <p>Each method documents the key of the {@code diagnostics} resource bundle entry that holds its
 * text.
 *
 * @see ProtobufSyntaxErrors
 * @see ProtobufSemanticErrors
 */
public final class ProtobufLexicalErrors {
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufLexicalErrors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a message stating that a string literal is not closed before the end of its line or
     * of the input.
     *
     * <p>Bundle key: {@code lexical.unterminated.string}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unterminatedString() {
        return ProtobufDiagnosticMessage.of(Kind.LEXICAL, "lexical.unterminated.string");
    }
}
