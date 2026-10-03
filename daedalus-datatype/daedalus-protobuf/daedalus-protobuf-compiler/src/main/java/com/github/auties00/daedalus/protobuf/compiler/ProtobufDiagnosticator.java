package com.github.auties00.daedalus.protobuf.compiler;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnostic;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufParserException;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourceRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An accumulator of the {@link ProtobufDiagnostic}s reported by the lexer, the parser and the
 * analyzer during a single compilation.
 *
 * <p>Reporting a diagnostic never interrupts the phase that reported it, and every reported
 * diagnostic is kept: the number of diagnostics collected by a single compilation is unbounded.
 *
 * <p>This class is not thread safe.
 */
final class ProtobufDiagnosticator {
    /**
     * The diagnostics reported so far, in the order in which they were reported.
     */
    private final List<ProtobufDiagnostic> diagnostics;

    /**
     * Constructs a new collector that holds no diagnostics.
     */
    ProtobufDiagnosticator() {
        this.diagnostics = new ArrayList<>();
    }

    /**
     * Records a diagnostic.
     *
     * @param location the file that contains the problem, or {@code null} if the source was not
     *                 read from a file
     * @param range the source range that contains the problem
     * @param message the description of the problem
     */
    void report(Path location, ProtobufTreeSourceRange range, ProtobufDiagnosticMessage message) {
        diagnostics.add(new ProtobufDiagnostic(message, location, range));
    }

    /**
     * Returns whether at least one diagnostic has been reported.
     *
     * @return {@code true} if at least one diagnostic has been reported, otherwise {@code false}
     */
    boolean hasErrors() {
        return !diagnostics.isEmpty();
    }

    /**
     * Returns a marker identifying the diagnostics reported so far, to be passed later to
     * {@link #hasSyntaxErrorsSince(int)}.
     *
     * @return the number of diagnostics reported so far
     */
    int mark() {
        return diagnostics.size();
    }

    /**
     * Returns whether a lexical or syntax diagnostic has been reported after the given marker.
     *
     * @param mark a marker previously returned by {@link #mark()}
     * @return {@code true} if a lexical or syntax diagnostic was reported after {@code mark},
     *         otherwise {@code false}
     */
    boolean hasSyntaxErrorsSince(int mark) {
        for (var i = mark; i < diagnostics.size(); i++) {
            var kind = diagnostics.get(i).message().kind();
            if (kind != ProtobufDiagnosticMessage.Kind.SEMANTIC) {
                return true;
            }
        }
        return false;
    }

    /**
     * Throws a {@link ProtobufParserException} carrying every diagnostic reported so far, if any.
     *
     * @throws ProtobufParserException if at least one diagnostic has been reported
     */
    void throwIfErrors() throws ProtobufParserException {
        if (!diagnostics.isEmpty()) {
            throw new ProtobufParserException(diagnostics);
        }
    }
}
