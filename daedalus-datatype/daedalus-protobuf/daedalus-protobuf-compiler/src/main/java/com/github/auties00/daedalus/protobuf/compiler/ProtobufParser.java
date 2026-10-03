package com.github.auties00.daedalus.protobuf.compiler;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufParserException;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufSemanticErrors;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufSyntaxErrors;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.protobuf.compiler.expression.*;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufFloatingPoint;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufIntegerRange;
import com.github.auties00.daedalus.protobuf.compiler.token.*;
import com.github.auties00.daedalus.protobuf.compiler.tree.*;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourcePosition;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourceRange;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses Protocol Buffer definition files (.proto) into an abstract syntax tree (AST).
 * <p>
 * The parser takes the token stream produced by {@link ProtobufLexer} and constructs a structured
 * representation of the Protocol Buffer definitions as a tree of {@link ProtobufTree} nodes.
 * It handles all Protocol Buffer language constructs including:
 * </p>
 * <ul>
 *   <li>Syntax declarations ({@code syntax = "proto2"} or {@code syntax = "proto3"})</li>
 *   <li>Package declarations ({@code package com.example})</li>
 *   <li>Import statements ({@code import "other.proto"})</li>
 *   <li>Message definitions with fields, nested messages, and enums</li>
 *   <li>Enum definitions with constants</li>
 *   <li>Service definitions with RPC methods</li>
 *   <li>Extensions and extension ranges</li>
 *   <li>Reserved field numbers and names</li>
 *   <li>Options at file, message, field, enum, and service levels</li>
 *   <li>Groups (deprecated Protocol Buffers 2 feature)</li>
 *   <li>Oneofs</li>
 *   <li>Maps</li>
 * </ul>
 * <p>
 * The parser provides both single-file and directory parsing capabilities. After parsing,
 * the {@link ProtobufAnalyzer} performs semantic analysis including type resolution,
 * import attribution, and validation.
 * </p>
 * <p>
 * The parser does not stop at the first problem it finds. When a declaration is malformed, the
 * problem is recorded and the parser skips ahead to the next statement boundary, so that a single
 * run reports every independent problem in the input. Files that contain lexical or syntax errors
 * are not semantically analyzed, and neither are the files that import them, to avoid reporting
 * problems that are only a consequence of the broken declarations. If any problem is found, the
 * entry points throw a {@link ProtobufParserException} carrying every diagnostic.
 * </p>
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Parse a single file
 * Path protoFile = Path.of("message.proto");
 * ProtobufDocumentTree tree = ProtobufParser.parseOnly(protoFile);
 *
 * // Parse a directory of files
 * Path protoDirectory = Path.of("proto/");
 * Map<String, ProtobufDocumentTree> trees = ProtobufParser.parse(protoDirectory);
 * }</pre>
 *
 * @see ProtobufLexer
 * @see ProtobufAnalyzer
 * @see ProtobufDocumentTree
 */
public final class ProtobufParser {
    private static final String STATEMENT_END = ";";
    private static final String BODY_START = "{";
    private static final String BODY_END = "}";
    private static final String ASSIGNMENT_OPERATOR = "=";
    private static final String ARRAY_START = "[";
    private static final String ARRAY_END = "]";
    private static final String ARRAY_SEPARATOR = ",";
    private static final String RANGE_OPERATOR = "to";
    private static final String TYPE_PARAMETERS_START = "<";
    private static final String TYPE_PARAMETERS_END = ">";
    private static final String NULL = "null";
    private static final String KEY_VALUE_SEPARATOR = ":";
    private static final String PARENS_START = "(";
    private static final String PARENS_END = ")";
    private static final String STREAM = "stream";
    private static final String MAX = "max";
    private static final String GROUP = "group";
    private static final String MAP = "map";
    private static final String RETURNS = "returns";

    /**
     * The keywords that start a statement, at which the parser resumes after an error.
     */
    private static final Set<String> STATEMENT_KEYWORDS = Set.of(
            "syntax", "edition", "package", "import", "option", "message", "enum", "service",
            "extend", "extensions", "reserved", "oneof", "rpc", "optional", "required", "repeated"
    );

    /**
     * The file being parsed, or {@code null} if the source was not read from a file.
     */
    private final Path location;

    /**
     * The lexer that produces the tokens of the file being parsed.
     */
    private final ProtobufLexer lexer;

    /**
     * The collector that receives the diagnostics reported while parsing.
     */
    private final ProtobufDiagnosticator diagnostics;

    /**
     * The document being built.
     */
    private final ProtobufDocumentTree document;

    /**
     * The text of the most recently consumed token, or {@code null} if it was a string literal, a
     * number or a boolean.
     */
    private String lastToken;

    /**
     * The number of tokens consumed so far, used to guarantee that error recovery always makes
     * progress.
     */
    private int tokenCount;

    /**
     * The end position of the token consumed before the most recently consumed one.
     */
    private ProtobufTreeSourcePosition previousTokenEnd;

    /**
     * The end position of the most recently consumed token.
     */
    private ProtobufTreeSourcePosition lastTokenEnd;

    /**
     * Whether the next call to {@link #synchronize(boolean, int)} should resume parsing at the
     * offending token instead of skipping it, because the malformed statement was only missing
     * its terminator.
     */
    private boolean resumeAtOffendingToken;

    /**
     * Constructs a new parser for a single file.
     *
     * @param location the file being parsed, or {@code null} if the source was not read from a file
     * @param lexer the lexer that produces the tokens of the file
     * @param diagnostics the collector that receives the diagnostics reported while parsing
     */
    private ProtobufParser(Path location, ProtobufLexer lexer, ProtobufDiagnosticator diagnostics) {
        this.location = location;
        this.lexer = lexer;
        this.diagnostics = diagnostics;
        this.document = new ProtobufDocumentTree(location);
        this.previousTokenEnd = ProtobufTreeSourcePosition.UNKNOWN;
        this.lastTokenEnd = ProtobufTreeSourcePosition.UNKNOWN;
    }

    /**
     * Parses all Protocol Buffer files in the specified path and performs full semantic analysis.
     * <p>
     * If the path is a file, parses that single file. If it's a directory, recursively parses all
     * regular files in the directory tree. After parsing, all documents are analyzed together,
     * resolving imports and type references between files.
     * </p>
     *
     * @param path the file or directory path to parse, must not be null
     * @return an unmodifiable map from file names to their parsed document trees
     * @throws IOException if an I/O error occurs while reading the files
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found in any file
     */
    public static Map<String, ProtobufDocumentTree> parse(Path path) throws IOException, ProtobufParserException {
        Objects.requireNonNull(path, "Path must not be null");
        if (!Files.isDirectory(path)) {
            return Map.of(path.getFileName().toString(), parseOnly(path));
        }

        List<Path> files;
        try (var walker = Files.walk(path)) {
            files = walker.filter(Files::isRegularFile).toList();
        }

        var diagnostics = new ProtobufDiagnosticator();
        Map<String, ProtobufDocumentTree> results = HashMap.newHashMap(files.size());
        Set<ProtobufDocumentTree> broken = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var file : files) {
            var mark = diagnostics.mark();
            ProtobufDocumentTree parsed;
            try (var reader = Files.newBufferedReader(file)) {
                parsed = doParse(file, reader, diagnostics);
            }
            if (diagnostics.hasSyntaxErrorsSince(mark)) {
                broken.add(parsed);
            }
            var previous = results.putIfAbsent(file.getFileName().toString(), parsed);
            if (previous != null) {
                var previousFile = previous.location()
                        .map(Path::toString)
                        .orElse("");
                diagnostics.report(file, ProtobufTreeSourceRange.UNKNOWN, ProtobufSemanticErrors.duplicateFile(file.toString(), previousFile));
            }
        }
        ProtobufAnalyzer.attribute(withoutBrokenDocuments(results.values(), broken), diagnostics);
        diagnostics.throwIfErrors();
        return Collections.unmodifiableMap(results);
    }

    /**
     * Parses a Protocol Buffer definition from a string and performs semantic analysis.
     * <p>
     * This method is useful for parsing inline Protocol Buffer definitions or testing.
     * The parsed document is analyzed in isolation without any additional context.
     * </p>
     *
     * @param input the Protocol Buffer definition string to parse, must not be null
     * @return the parsed and analyzed document tree
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(String input) throws ProtobufParserException {
        return parseOnly(input, (Collection<ProtobufDocumentTree>) null);
    }

    /**
     * Parses a Protocol Buffer definition from a string with additional context documents.
     * <p>
     * The provided context documents allow type references in the parsed document to be resolved
     * against types defined in other documents. This is useful when parsing documents that import
     * or reference types from other files.
     * </p>
     *
     * @param input the Protocol Buffer definition string to parse, must not be null
     * @param documentTrees additional context documents for type resolution
     * @return the parsed and analyzed document tree
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(String input, ProtobufDocumentTree... documentTrees) throws ProtobufParserException {
        return parseOnly(input, Arrays.asList(documentTrees));
    }

    /**
     * Parses a Protocol Buffer definition from a string with additional context documents.
     *
     * @param input the Protocol Buffer definition string to parse, must not be null
     * @param documents additional context documents for type resolution, may be null
     * @return the parsed and analyzed document tree
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(String input, Collection<? extends ProtobufDocumentTree> documents) throws ProtobufParserException {
        try {
            return parseAndAttribute(null, new StringReader(input), documents);
        } catch (IOException exception) {
            // A StringReader never fails
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Parses a single Protocol Buffer file and performs semantic analysis.
     * <p>
     * The parsed document is analyzed in isolation without any additional context.
     * </p>
     *
     * @param input the file path to parse, must not be null
     * @return the parsed and analyzed document tree
     * @throws IOException if an I/O error occurs while reading the files
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(Path input) throws IOException, ProtobufParserException {
        return parseOnly(input, (Collection<ProtobufDocumentTree>) null);
    }

    /**
     * Parses a single Protocol Buffer file with additional context documents.
     *
     * @param input the file path to parse, must not be null
     * @param documentTrees additional context documents for type resolution
     * @return the parsed and analyzed document tree
     * @throws IOException if an I/O error occurs while reading the files
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(Path input, ProtobufDocumentTree... documentTrees) throws IOException, ProtobufParserException {
        return parseOnly(input, Arrays.asList(documentTrees));
    }

    /**
     * Parses a single Protocol Buffer file with additional context documents.
     *
     * @param input the file path to parse, must not be null
     * @param documents additional context documents for type resolution, may be null
     * @return the parsed and analyzed document tree
     * @throws IOException if an I/O error occurs while reading the files
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    public static ProtobufDocumentTree parseOnly(Path input, Collection<? extends ProtobufDocumentTree> documents) throws IOException, ProtobufParserException {
        try (var reader = Files.newBufferedReader(input)) {
            return parseAndAttribute(input, reader, documents);
        }
    }

    /**
     * Parses a single source and, if it has no lexical or syntax errors, analyzes it together with
     * the given context documents.
     *
     * @param location the file being parsed, or {@code null} if the source was not read from a file
     * @param input the source to parse
     * @param documents additional context documents for type resolution, may be null
     * @return the parsed and analyzed document tree
     * @throws IOException if an I/O error occurs while reading {@code input}
     * @throws ProtobufParserException if any lexical, syntax or semantic error is found
     */
    private static ProtobufDocumentTree parseAndAttribute(Path location, Reader input, Collection<? extends ProtobufDocumentTree> documents) throws IOException, ProtobufParserException {
        var diagnostics = new ProtobufDiagnosticator();
        var result = doParse(location, input, diagnostics);
        if (!diagnostics.hasSyntaxErrorsSince(0)) {
            var results = new ArrayList<ProtobufDocumentTree>();
            if (documents != null) {
                results.addAll(documents);
            }
            results.add(result);
            ProtobufAnalyzer.attribute(results, diagnostics);
        }
        diagnostics.throwIfErrors();
        return result;
    }

    /**
     * Returns the documents that can be semantically analyzed: those without lexical or syntax
     * errors that do not import, directly or transitively, a document with such errors.
     *
     * @param documents the parsed documents
     * @param broken the documents with lexical or syntax errors
     * @return the documents to analyze
     */
    private static List<ProtobufDocumentTree> withoutBrokenDocuments(Collection<ProtobufDocumentTree> documents, Set<ProtobufDocumentTree> broken) {
        if (broken.isEmpty()) {
            return new ArrayList<>(documents);
        }

        var brokenPaths = new HashSet<String>();
        for (var document : broken) {
            brokenPaths.add(document.qualifiedPath());
        }

        var remaining = new ArrayList<ProtobufDocumentTree>();
        for (var document : documents) {
            if (!broken.contains(document)) {
                remaining.add(document);
            }
        }

        var changed = true;
        while (changed) {
            changed = false;
            var iterator = remaining.iterator();
            while (iterator.hasNext()) {
                var document = iterator.next();
                var importsBroken = document.getDirectChildrenByType(ProtobufImportStatement.class)
                        .anyMatch(importStatement -> brokenPaths.contains(importStatement.location()));
                if (importsBroken) {
                    brokenPaths.add(document.qualifiedPath());
                    iterator.remove();
                    changed = true;
                }
            }
        }
        return remaining;
    }

    /**
     * Parses a single source into a document tree, reporting every problem to the given collector
     * instead of throwing.
     *
     * @param location the file being parsed, or {@code null} if the source was not read from a file
     * @param input the source to parse
     * @param diagnostics the collector that receives the diagnostics reported while parsing
     * @return the parsed document, which only contains the declarations that were parsed
     *         successfully
     * @throws IOException if an I/O error occurs while reading {@code input}
     */
    static ProtobufDocumentTree doParse(Path location, Reader input, ProtobufDiagnosticator diagnostics) throws IOException {
        var lexer = new ProtobufLexer(input, location, diagnostics);
        var parser = new ProtobufParser(location, lexer, diagnostics);
        return parser.parseDocument();
    }

    /**
     * Parses every statement of the source into {@link #document}.
     *
     * @return the parsed document
     */
    private ProtobufDocumentTree parseDocument() {
        try {
            String token;
            while ((token = lexer.nextRawToken()) != null) {
                consumed(token);
                var start = lexer.tokenRange().start();
                var dispatch = tokenCount;
                try {
                    parseDocumentChild(token, start);
                } catch (Sync _) {
                    synchronize(false, dispatch);
                }
            }
        } catch (Abort _) {
            // The input ended in the middle of a declaration
        }
        // Any comments still pending after the last token belong to the document itself.
        for (var comment : lexer.consumePendingDetached()) {
            document.addComment(comment);
        }
        var trailingLeading = lexer.consumePendingLeading();
        if (trailingLeading != null) {
            document.addComment(trailingLeading);
        }
        document.setRange(new ProtobufTreeSourceRange(new ProtobufTreeSourcePosition(1, 1), lexer.currentPosition()));
        return document;
    }

    /**
     * Parses a top level statement introduced by the given token and adds it to {@link #document}.
     *
     * @param token the first token of the statement
     * @param start the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseDocumentChild(String token, ProtobufTreeSourcePosition start) {
        switch (token) {
            case STATEMENT_END -> {
                var statement = parseEmpty(start);
                document.addChild(statement);
            }
            case "package" -> {
                var statement = parsePackage(start);
                document.addChild(statement);
            }
            case "syntax" -> {
                var statement = parseSyntax(start);
                document.addChild(statement);
            }
            case "edition" -> {
                var statement = parseEdition(start);
                document.addChild(statement);
            }
            case "option" -> {
                var statement = parseOptionStatement(start);
                document.addChild(statement);
            }
            case "export", "local" -> {
                if (document.version() != ProtobufVersion.EDITION_2024) {
                    report(ProtobufSemanticErrors.visibilityRequiresEdition2024(token));
                }
                var visibility = ProtobufTreeVisibility.of(token).orElseThrow();
                var keyword = nextRawToken();
                switch (keyword) {
                    case "message" -> {
                        var statement = parseMessage(start);
                        statement.setVisibility(visibility);
                        document.addChild(statement);
                    }
                    case "enum" -> {
                        var statement = parseEnum(start);
                        statement.setVisibility(visibility);
                        document.addChild(statement);
                    }
                    default -> throw error(ProtobufSyntaxErrors.expectedMessageOrEnumAfterVisibility(token, keyword));
                }
            }
            case "message" -> {
                var statement = parseMessage(start);
                document.addChild(statement);
            }
            case "enum" -> {
                var statement = parseEnum(start);
                document.addChild(statement);
            }
            case "service" -> {
                var statement = parseService(start);
                document.addChild(statement);
            }
            case "import" -> {
                var statement = parseImport(start);
                document.addChild(statement);
            }
            case "extend" -> {
                var statement = parseExtend(start);
                document.addChild(statement);
            }
            default -> throw error(ProtobufSyntaxErrors.unexpectedTopLevelToken(token));
        }
    }

    /**
     * Reads the next raw token, reporting an error if the input has ended.
     *
     * @return the next raw token
     * @throws Abort if the input has ended
     */
    private String nextRawToken() {
        var token = lexer.nextRawToken();
        if (token == null) {
            throw endOfInput();
        }
        consumed(token);
        return token;
    }

    /**
     * Reads the next typed token, reporting an error if the input has ended.
     *
     * @return the next typed token
     * @throws Abort if the input has ended
     */
    private ProtobufToken nextToken() {
        var token = lexer.nextToken();
        if (token == null) {
            throw endOfInput();
        }
        consumed(token instanceof ProtobufRawToken(var value) ? value : null);
        return token;
    }

    /**
     * Records that a token was consumed.
     *
     * @param token the text of the token, or {@code null} if it was a string literal, a number or
     *              a boolean
     */
    private void consumed(String token) {
        lastToken = token;
        tokenCount++;
        previousTokenEnd = lastTokenEnd;
        lastTokenEnd = lexer.tokenRange().end();
    }

    /**
     * Moves the lexer back to the previous token, so that it is read again by the next call to
     * {@link #nextRawToken()} or {@link #nextToken()}.
     */
    private void moveToPreviousToken() {
        lexer.moveToPreviousToken();
        tokenCount--;
    }

    /**
     * Reports that the input ended in the middle of a declaration.
     *
     * @return the signal that unwinds the parser, to be thrown by the caller
     */
    private Abort endOfInput() {
        var position = lexer.currentPosition();
        diagnostics.report(location, new ProtobufTreeSourceRange(position, position), ProtobufSyntaxErrors.unexpectedEndOfInput());
        return Abort.INSTANCE;
    }

    /**
     * Reports the given error at the most recently consumed token if {@code condition} is
     * {@code false}.
     *
     * @param condition the condition that must hold
     * @param message the error to report if the condition does not hold
     * @throws Sync if the condition does not hold
     */
    private void expect(boolean condition, ProtobufDiagnosticMessage message) {
        if (!condition) {
            throw error(message);
        }
    }

    /**
     * Reports the given error if {@code token} is not the {@code ;} that terminates a statement.
     *
     * <p>A statement that is not terminated is usually followed by the next statement on a new
     * line. In that case the error is reported right after the last token of the statement, and
     * parsing resumes at the offending token instead of skipping it, so that the next statement
     * is not lost.
     *
     * @param token the token that should terminate the statement
     * @param message the error to report if {@code token} is not a {@code ;}
     * @throws Sync if {@code token} is not a {@code ;}
     */
    private void expectStatementEnd(String token, ProtobufDiagnosticMessage message) {
        if (isStatementEnd(token)) {
            return;
        }

        var offendingStart = lexer.tokenRange().start();
        if (previousTokenEnd.line() > 0 && offendingStart.line() > previousTokenEnd.line()) {
            diagnostics.report(location, new ProtobufTreeSourceRange(previousTokenEnd, previousTokenEnd), message);
            resumeAtOffendingToken = true;
            throw Sync.INSTANCE;
        }

        throw error(message);
    }

    /**
     * Reports the given error at the most recently consumed token.
     *
     * @param message the error to report
     * @return the signal that unwinds the parser to the nearest statement boundary, to be thrown by
     *         the caller
     */
    private Sync error(ProtobufDiagnosticMessage message) {
        report(message);
        return Sync.INSTANCE;
    }

    /**
     * Reports the given problem at the most recently consumed token without interrupting parsing.
     *
     * @param message the problem to report
     */
    private void report(ProtobufDiagnosticMessage message) {
        diagnostics.report(location, lexer.tokenRange(), message);
    }

    /**
     * Skips tokens until the end of the malformed statement, so that parsing can resume with the
     * next one.
     *
     * <p>Recovery starts from the offending token, which is the most recently consumed one. A
     * {@code ;} ends the statement. A {@code }} ends the enclosing body, so it is left in the input
     * when {@code inBody} is {@code true}. A {@code {} starts a block that is skipped entirely.
     * A statement keyword starts the next statement and is left in the input, unless it is the
     * token that introduced the malformed statement, in which case leaving it would make the
     * parser fail on it again.
     *
     * @param inBody whether the malformed statement is inside the body of a declaration
     * @param dispatch the value of {@link #tokenCount} after the first token of the malformed
     *                 statement was consumed
     */
    private void synchronize(boolean inBody, int dispatch) {
        if (resumeAtOffendingToken) {
            resumeAtOffendingToken = false;
            if (tokenCount > dispatch && !(isBodyEnd(lastToken) && !inBody)) {
                moveToPreviousToken();
                return;
            }
        }

        var token = lastToken;
        if (token != null) {
            switch (token) {
                case STATEMENT_END -> {
                    return;
                }
                case BODY_END -> {
                    if (inBody) {
                        moveToPreviousToken();
                    }
                    return;
                }
                case BODY_START -> {
                    skipBlock();
                    return;
                }
                default -> {
                    if (tokenCount > dispatch && STATEMENT_KEYWORDS.contains(token)) {
                        moveToPreviousToken();
                        return;
                    }
                }
            }
        }

        while ((token = lexer.nextRawToken()) != null) {
            consumed(token);
            switch (token) {
                case STATEMENT_END -> {
                    return;
                }
                case BODY_END -> {
                    if (inBody) {
                        moveToPreviousToken();
                    }
                    return;
                }
                case BODY_START -> {
                    skipBlock();
                    return;
                }
                default -> {
                    if (STATEMENT_KEYWORDS.contains(token)) {
                        moveToPreviousToken();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Skips the tokens of a block whose opening brace was just consumed, up to and including its
     * matching closing brace or the end of input.
     */
    private void skipBlock() {
        var depth = 1;
        String token;
        while ((token = lexer.nextRawToken()) != null) {
            consumed(token);
            if (isBodyStart(token)) {
                depth++;
            } else if (isBodyEnd(token) && --depth == 0) {
                return;
            }
        }
    }

    /**
     * Drains any leading and detached comments collected by the lexer for the upcoming statement
     * and attaches them to the given statement.
     */
    private void attachLeadingComments(ProtobufStatement statement) {
        for (var comment : lexer.consumePendingDetached()) {
            statement.addComment(comment);
        }
        var leading = lexer.consumePendingLeading();
        if (leading != null) {
            statement.addComment(leading);
        }
    }

    /**
     * Finalizes a statement's source range and attaches any same-line trailing comment that
     * follows the most recently consumed token.
     */
    private <T extends ProtobufStatement> T finishStatement(T statement, ProtobufTreeSourcePosition start) {
        statement.setRange(new ProtobufTreeSourceRange(start, lexer.tokenRange().end()));
        var trailing = lexer.consumePendingTrailing();
        if (trailing != null) {
            statement.addComment(trailing);
        }
        return statement;
    }

    private ProtobufEmptyStatement parseEmpty(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufEmptyStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        return finishStatement(statement, start);
    }

    private ProtobufPackageStatement parsePackage(ProtobufTreeSourcePosition start) {
        if (document.packageName().isPresent()) {
            report(ProtobufSemanticErrors.duplicatePackage(document.packageName().get()));
        }
        var statement = new ProtobufPackageStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidPackage(name), ProtobufSyntaxErrors.invalidPackageName(name));
        statement.setName(name);
        var end = nextRawToken();
        expectStatementEnd(end, ProtobufSyntaxErrors.expectedSemicolonAfterPackage(end));
        return finishStatement(statement, start);
    }

    private ProtobufSyntaxStatement parseSyntax(ProtobufTreeSourcePosition start) {
        if (!document.children().isEmpty()) {
            report(ProtobufSemanticErrors.syntaxNotFirst());
        }
        var statement = new ProtobufSyntaxStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var assignment = nextRawToken();
        expect(isAssignmentOperator(assignment), ProtobufSyntaxErrors.expectedAssignmentAfterSyntax(assignment));
        var versionCodeToken = nextToken();
        if (!(versionCodeToken instanceof ProtobufLiteralToken(var versionCode, _))) {
            throw error(ProtobufSyntaxErrors.expectedSyntaxLiteral(versionCodeToken.toString()));
        }
        var version = ProtobufVersion.of(versionCode)
                .filter(v -> !v.isEdition());
        if (version.isPresent()) {
            statement.setVersion(version.get());
        } else {
            // Keep parsing the rest of the file as proto2, the default version
            report(ProtobufSyntaxErrors.unknownSyntaxVersion(versionCode));
            statement.setVersion(ProtobufVersion.PROTOBUF_2);
        }
        var end = nextRawToken();
        expectStatementEnd(end, ProtobufSyntaxErrors.expectedSemicolonAfterSyntax(end));
        return finishStatement(statement, start);
    }

    private ProtobufEditionStatement parseEdition(ProtobufTreeSourcePosition start) {
        if (!document.children().isEmpty()) {
            report(ProtobufSemanticErrors.editionNotFirst());
        }
        var statement = new ProtobufEditionStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var assignment = nextRawToken();
        expect(isAssignmentOperator(assignment), ProtobufSyntaxErrors.expectedAssignmentAfterEdition(assignment));
        var editionCodeToken = nextToken();
        if (!(editionCodeToken instanceof ProtobufLiteralToken(var editionCode, _))) {
            throw error(ProtobufSyntaxErrors.expectedEditionLiteral(editionCodeToken.toString()));
        }
        var version = ProtobufVersion.of(editionCode)
                .filter(ProtobufVersion::isEdition);
        if (version.isPresent()) {
            statement.setVersion(version.get());
        } else {
            // Keep parsing the rest of the file as the latest known edition
            report(ProtobufSyntaxErrors.unknownEdition(editionCode));
            statement.setVersion(ProtobufVersion.EDITION_2024);
        }
        var end = nextRawToken();
        expectStatementEnd(end, ProtobufSyntaxErrors.expectedSemicolonAfterEdition(end));
        return finishStatement(statement, start);
    }

    private ProtobufOptionStatement parseOptionStatement(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufOptionStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var optionName = parseOptionName();
        var optionValue = readExpression();
        var end = nextRawToken();
        expectStatementEnd(end, ProtobufSyntaxErrors.expectedSemicolonAfterOption(end));
        statement.setName(optionName);
        statement.setValue(optionValue);
        return finishStatement(statement, start);
    }

    private ProtobufOptionExpression parseOptionExpression() {
        var optionName = parseOptionName();
        var optionValue = readExpression();
        return new ProtobufOptionExpression(optionName, optionValue);
    }

    private ProtobufOptionName parseOptionName() {
        var segments = new ArrayList<ProtobufOptionNameSegment>();
        while(true) {
            var token = nextRawToken();
            if(isAssignmentOperator(token)) {
                moveToPreviousToken();
                break;
            }else if(isParensStart(token)) {
                var extName = nextRawToken();
                var parensEnd = nextRawToken();
                expect(isParensEnd(parensEnd), ProtobufSyntaxErrors.expectedExtensionNameEnd(parensEnd));
                segments.add(new ProtobufOptionNameSegment(extName, true));
            }else {
                var start = token.charAt(0) == '.' ? 1 : 0;
                var end = token.charAt(token.length() - 1) == '.' ? token.length() - 1 : token.length();
                var pos = start;
                while(pos < end) {
                    var dot = token.indexOf('.', pos);
                    if(dot < 0 || dot >= end) {
                        segments.add(new ProtobufOptionNameSegment(token.substring(pos, end), false));
                        break;
                    } else {
                        segments.add(new ProtobufOptionNameSegment(token.substring(pos, dot), false));
                        pos = dot + 1;
                    }
                }
                if(end == token.length()) {
                    break;
                }
            }
        }

        var assignment = nextRawToken();
        expect(isAssignmentOperator(assignment), ProtobufSyntaxErrors.expectedAssignmentAfterOptionName(assignment));

        return new ProtobufOptionName(segments);
    }

    private ProtobufMessageStatement parseMessage(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufMessageStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidMessageName(name));
        statement.setName(name);
        var objectStart = nextRawToken();
        expect(isBodyStart(objectStart), ProtobufSyntaxErrors.expectedMessageBodyStart(objectStart));
        String token;
        while (!isBodyEnd(token = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseMessageChild(statement, token, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of a message body introduced by the given token and adds it to the
     * message.
     *
     * @param statement the message that contains the statement
     * @param token the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseMessageChild(ProtobufMessageStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        switch (token) {
            case STATEMENT_END -> {
                var child = parseEmpty(childStart);
                statement.addChild(child);
            }
            case "option" -> {
                var child = parseOptionStatement(childStart);
                statement.addChild(child);
            }
            case "message" -> {
                var child = parseMessage(childStart);
                statement.addChild(child);
            }
            case "enum" -> {
                var child = parseEnum(childStart);
                statement.addChild(child);
            }
            case "extend" -> {
                var child = parseExtend(childStart);
                statement.addChild(child);
            }
            case "extensions" -> {
                var child = parseExtensions(childStart);
                statement.addChild(child);
            }
            case "reserved"  -> {
                var child = parseReserved(childStart);
                statement.addChild(child);
            }
            case "oneof" -> {
                var child = parseOneof(childStart);
                statement.addChild(child);
            }
            default -> {
                var modifier = ProtobufModifier.of(token);
                if(modifier.isPresent()) {
                    var typeToken = nextRawToken();
                    if (isGroupType(typeToken)) {
                        var groupField = parseGroupField(modifier.get(), childStart);
                        statement.addChild(groupField);
                    } else {
                        var field = parseField(modifier.get(), typeToken, childStart);
                        statement.addChild(field);
                    }
                } else {
                    if (isGroupType(token)) {
                        var groupField = parseGroupField(ProtobufModifier.NONE, childStart);
                        statement.addChild(groupField);
                    } else {
                        var field = parseField(ProtobufModifier.NONE, token, childStart);
                        statement.addChild(field);
                    }
                }
            }
        }
    }

    private ProtobufExtendStatement parseExtend(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufExtendStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var qualifiedName = nextRawToken();
        expect(isValidType(qualifiedName), ProtobufSyntaxErrors.invalidExtendTarget(qualifiedName));

        // In proto3, extends are only allowed for Options messages
        // Validation happens in the attribute phase, not here during parsing
        var reference = new ProtobufUnresolvedTypeReference(qualifiedName);
        statement.setDeclaration(reference);
        var objectStart = nextRawToken();
        expect(isBodyStart(objectStart), ProtobufSyntaxErrors.expectedExtendBodyStart(objectStart));
        String token;
        while (!isBodyEnd(token = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseExtendChild(statement, token, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a field of an extend body introduced by the given token and adds it to the extend
     * declaration.
     *
     * @param statement the extend declaration that contains the field
     * @param token the first token of the field
     * @param childStart the position of the first token of the field
     * @throws Sync if the field is malformed
     */
    private void parseExtendChild(ProtobufExtendStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        var modifier = ProtobufModifier.of(token);
        if(modifier.isPresent()) {
            var typeToken = nextRawToken();
            if (isGroupType(typeToken)) {
                var groupField = parseGroupField(modifier.get(), childStart);
                statement.addChild(groupField);
            } else {
                var field = parseField(modifier.get(), typeToken, childStart);
                statement.addChild(field);
            }
        } else {
            if (isGroupType(token)) {
                var groupField = parseGroupField(ProtobufModifier.NONE, childStart);
                statement.addChild(groupField);
            } else {
                var field = parseField(ProtobufModifier.NONE, token, childStart);
                statement.addChild(field);
            }
        }
    }

    private ProtobufGroupStatement parseGroupField(ProtobufModifier modifier, ProtobufTreeSourcePosition start) {
        var statement = new ProtobufGroupStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);

        statement.setModifier(modifier);

        var name = nextRawToken();
        statement.setName(validateGroupFieldName(name));

        validateFieldIndexAssignment();

        var index = nextToken();
        statement.setIndex(validateFieldIndex(index));

        var bodyStartToken = parseOptions(nextRawToken(), statement);
        expect(isBodyStart(bodyStartToken), ProtobufSyntaxErrors.expectedToken(BODY_START, bodyStartToken));

        String groupToken;
        while(!isBodyEnd(groupToken = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseGroupChild(statement, groupToken, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }

        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of a group body introduced by the given token and adds it to the group.
     *
     * @param statement the group that contains the statement
     * @param groupToken the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseGroupChild(ProtobufGroupStatement statement, String groupToken, ProtobufTreeSourcePosition childStart) {
        switch (groupToken) {
            case STATEMENT_END -> {
                var groupChild = parseEmpty(childStart);
                statement.addChild(groupChild);
            }

            case "message" -> {
                var groupChild = parseMessage(childStart);
                statement.addChild(groupChild);
            }

            case "enum" -> {
                var groupChild = parseEnum(childStart);
                statement.addChild(groupChild);
            }

            case "extend" -> {
                var groupChild = parseExtend(childStart);
                statement.addChild(groupChild);
            }

            case "extensions" -> {
                var groupChild = parseExtensions(childStart);
                statement.addChild(groupChild);
            }

            case "reserved" -> {
                var groupChild = parseReserved(childStart);
                statement.addChild(groupChild);
            }

            case "oneof" -> {
                var groupChild = parseOneof(childStart);
                statement.addChild(groupChild);
            }

            default -> {
                var groupModifier = ProtobufModifier.of(groupToken);
                if(groupModifier.isPresent()) {
                    var typeToken = nextRawToken();
                    if (isGroupType(typeToken)) {
                        var groupField = parseGroupField(groupModifier.get(), childStart);
                        statement.addChild(groupField);
                    } else {
                        var field = parseField(groupModifier.get(), typeToken, childStart);
                        statement.addChild(field);
                    }
                } else {
                    if (isGroupType(groupToken)) {
                        var groupField = parseGroupField(ProtobufModifier.NONE, childStart);
                        statement.addChild(groupField);
                    } else {
                        var field = parseField(ProtobufModifier.NONE, groupToken, childStart);
                        statement.addChild(field);
                    }
                }
            }
        }
    }

    private String validateGroupFieldName(String name) {
        validateFieldName(name);
        if (!Character.isUpperCase(name.charAt(0))) {
            report(ProtobufSemanticErrors.groupNameNotCapitalized(name));
        }
        return name;
    }

    private ProtobufFieldStatement parseField(ProtobufModifier modifier, String typeToken, ProtobufTreeSourcePosition start) {
        var statement = new ProtobufFieldStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);

        statement.setModifier(modifier);

        var type = parseFieldType(typeToken);
        statement.setType(type);

        var name = nextRawToken();
        statement.setName(validateFieldName(name));

        validateFieldIndexAssignment();

        var index = nextToken();
        statement.setIndex(validateFieldIndex(index));

        var statementEnd = parseOptions(nextRawToken(), statement);
        expectStatementEnd(statementEnd, ProtobufSyntaxErrors.expectedToken(STATEMENT_END, statementEnd));

        return finishStatement(statement, start);
    }

    private ProtobufTypeReference parseFieldType(String typeToken) {
        var primitiveType = ProtobufType.ofPrimitive(typeToken);
        if(primitiveType.isPresent()) {
            return new ProtobufPrimitiveTypeReference(primitiveType.get());
        }

        if(isMapType(typeToken)) {
            var mapType = parseFieldMapType();
            if(mapType.isPresent()) {
                return mapType.get();
            }
        }

        return new ProtobufUnresolvedTypeReference(typeToken);
    }

    private Optional<ProtobufTypeReference> parseFieldMapType() {
        var nameOrTypeArgs = nextRawToken();
        if (!isTypeParametersStart(nameOrTypeArgs)) {
            moveToPreviousToken();
            return Optional.empty();
        }

        var keyTypeToken = nextRawToken();
        expect(isValidType(keyTypeToken), ProtobufSyntaxErrors.invalidMapKeyType(keyTypeToken));
        var keyType = parseFieldType(keyTypeToken);

        var separator = nextRawToken();
        expect(isArraySeparator(separator), ProtobufSyntaxErrors.expectedMapTypeSeparator(separator));

        var valueTypeToken = nextRawToken();
        expect(isValidType(valueTypeToken), ProtobufSyntaxErrors.invalidMapValueType(valueTypeToken));
        var valueType = parseFieldType(valueTypeToken);

        var end = nextRawToken();
        expect(isTypeParametersEnd(end), ProtobufSyntaxErrors.expectedMapTypeEnd(end));

        var result = new ProtobufMapTypeReference(keyType, valueType);
        return Optional.of(result);
    }

    private String validateFieldName(String name) {
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidFieldName(name));
        return name;
    }

    private void validateFieldIndexAssignment() {
        var operator = nextRawToken();
        expect(isAssignmentOperator(operator), ProtobufSyntaxErrors.expectedAssignmentAfterFieldName(operator));
    }

    private ProtobufInteger validateFieldIndex(ProtobufToken index) {
        if (index instanceof ProtobufNumberToken(var number) && number instanceof ProtobufInteger integer) {
            return integer;
        } else {
            throw error(ProtobufSyntaxErrors.expectedFieldNumber(index.toString()));
        }
    }

    private ProtobufEnumStatement parseEnum(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufEnumStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidEnumName(name));
        statement.setName(name);
        var objectStart = nextRawToken();
        expect(isBodyStart(objectStart), ProtobufSyntaxErrors.expectedToken(BODY_START, objectStart));
        String token;
        while (!isBodyEnd(token = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseEnumChild(statement, token, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of an enum body introduced by the given token and adds it to the enum.
     *
     * @param statement the enum that contains the statement
     * @param token the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseEnumChild(ProtobufEnumStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        switch (token) {
            case STATEMENT_END -> {
                var child = parseEmpty(childStart);
                statement.addChild(child);
            }
            case "option" -> {
                var child = parseOptionStatement(childStart);
                statement.addChild(child);
            }
            case "reserved"  -> {
                var child = parseReserved(childStart);
                statement.addChild(child);
            }
            default -> {
                var child = parseEnumConstant(token, childStart);
                statement.addChild(child);
            }
        }
    }

    private ProtobufEnumConstantStatement parseEnumConstant(String token, ProtobufTreeSourcePosition start) {
        var statement = new ProtobufEnumConstantStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        statement.setName(token);

        var operator = nextRawToken();
        expect(isAssignmentOperator(operator), ProtobufSyntaxErrors.expectedToken(ASSIGNMENT_OPERATOR, operator));

        var index = nextToken();
        if(index instanceof ProtobufNumberToken(var number) && number instanceof ProtobufInteger integer) {
            statement.setIndex(integer);
        }else {
            throw error(ProtobufSyntaxErrors.expectedEnumConstantNumber(index.toString()));
        }

        var statementEndToken = parseOptions(nextRawToken(), statement);
        expectStatementEnd(statementEndToken, ProtobufSyntaxErrors.expectedToken(STATEMENT_END, statementEndToken));
        return finishStatement(statement, start);
    }

    private ProtobufExtensionsStatement parseExtensions(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufExtensionsStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);

        while(true) {
            var valueOrMinToken = nextToken();
            if (!(valueOrMinToken instanceof ProtobufNumberToken(var valueOrMinNumber)) || !(valueOrMinNumber instanceof ProtobufInteger valueOrMinInt)) {
                throw error(ProtobufSyntaxErrors.expectedIntegerValue(valueOrMinToken.toString()));
            }

            var operator = nextRawToken();
            if(isRangeOperator(operator)) {
                var maxToken = nextToken();
                var range = switch (maxToken) {
                    case ProtobufNumberToken(var maxNumber) when maxNumber instanceof ProtobufInteger maxInt -> new ProtobufIntegerRange.Bounded(valueOrMinInt, maxInt);
                    case ProtobufRawToken(var token) when isMax(token) -> new ProtobufIntegerRange.LowerBounded(valueOrMinInt);
                    default -> throw error(ProtobufSyntaxErrors.expectedRangeEnd(maxToken.toString()));
                };

                var expression = new ProtobufIntegerRangeExpression(range);
                statement.addExpression(expression);

                operator = nextRawToken();
            }else {
                var expression = new ProtobufIntegerExpression(valueOrMinInt);
                statement.addExpression(expression);
            }

            var statementEndOrSeparator = parseOptions(operator, statement);
            if (isStatementEnd(statementEndOrSeparator)) {
                break;
            } else if (!isArraySeparator(statementEndOrSeparator)) {
                throw error(ProtobufSyntaxErrors.expectedSeparatorOrSemicolon(statementEndOrSeparator));
            }
        }

        return finishStatement(statement, start);
    }

    private ProtobufReservedStatement parseReserved(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufReservedStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var isEdition = document.version().isEdition();

        bodyLoop: {
            while (true) {
                var valueOrMinToken = nextToken();
                switch (valueOrMinToken) {
                    case ProtobufLiteralToken(var value, _) -> {
                        if (isEdition) {
                            report(ProtobufSemanticErrors.reservedNameMustBeIdentifier(value));
                        }
                        statement.addExpression(new ProtobufLiteralExpression(value));
                        var operator = nextRawToken();
                        if(isStatementEnd(operator)) {
                            break bodyLoop;
                        }else if(!isArraySeparator(operator)) {
                            throw error(ProtobufSyntaxErrors.expectedSeparatorOrSemicolon(operator));
                        }
                    }

                    case ProtobufNumberToken(var number) when number instanceof ProtobufInteger valueOrMinInt -> {
                        var operator = nextRawToken();
                        if(isRangeOperator(operator)) {
                            var maxToken = nextToken();
                            var range = switch (maxToken) {
                                case ProtobufNumberToken(var maxNumber) when maxNumber instanceof ProtobufInteger maxInt -> new ProtobufIntegerRange.Bounded(valueOrMinInt, maxInt);
                                case ProtobufRawToken(var token) when isMax(token) -> new ProtobufIntegerRange.LowerBounded(valueOrMinInt);
                                default -> throw error(ProtobufSyntaxErrors.expectedRangeEnd(maxToken.toString()));
                            };

                            statement.addExpression(new ProtobufIntegerRangeExpression(range));

                            operator = nextRawToken();
                        }else {
                            statement.addExpression(new ProtobufIntegerExpression(valueOrMinInt));
                        }

                        if(isStatementEnd(operator)) {
                            break bodyLoop;
                        }else if(!isArraySeparator(operator)) {
                            throw error(ProtobufSyntaxErrors.expectedSeparatorOrSemicolon(operator));
                        }
                    }

                    case ProtobufRawToken(var rawValue) when isValidIdent(rawValue) -> {
                        if (!isEdition) {
                            report(ProtobufSemanticErrors.reservedNameMustBeLiteral(rawValue));
                        }
                        statement.addExpression(new ProtobufLiteralExpression(rawValue));
                        var operator = nextRawToken();
                        if(isStatementEnd(operator)) {
                            break bodyLoop;
                        }else if(!isArraySeparator(operator)) {
                            throw error(ProtobufSyntaxErrors.expectedSeparatorOrSemicolon(operator));
                        }
                    }

                    default -> throw error(ProtobufSyntaxErrors.expectedReservedValue(valueOrMinToken.toString()));
                }
            }
        }

        return finishStatement(statement, start);
    }

    private ProtobufServiceStatement parseService(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufServiceStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidServiceName(name));
        statement.setName(name);
        var objectStart = nextRawToken();
        expect(isBodyStart(objectStart), ProtobufSyntaxErrors.expectedToken(BODY_START, objectStart));
        String token;
        while (!isBodyEnd(token = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseServiceChild(statement, token, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of a service body introduced by the given token and adds it to the
     * service.
     *
     * @param statement the service that contains the statement
     * @param token the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseServiceChild(ProtobufServiceStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        switch (token) {
            case STATEMENT_END -> {
                var child = parseEmpty(childStart);
                statement.addChild(child);
            }
            case "option" -> {
                var child = parseOptionStatement(childStart);
                statement.addChild(child);
            }
            case "rpc" -> {
                var child = parseMethod(childStart);
                statement.addChild(child);
            }
            default -> throw error(ProtobufSyntaxErrors.unexpectedServiceToken(token));
        }
    }

    private ProtobufMethodStatement parseMethod(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufMethodStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidMethodName(name));
        statement.setName(name);
        var inputType = parseMethodType();
        statement.setInputType(inputType);
        var returnsToken = nextRawToken();
        expect(Objects.equals(returnsToken, RETURNS), ProtobufSyntaxErrors.expectedReturns(returnsToken));
        var outputType = parseMethodType();
        statement.setOutputType(outputType);
        var objectStartOrStatementEnd = nextRawToken();
        if(isBodyStart(objectStartOrStatementEnd)){
            String token;
            while (!isBodyEnd(token = nextRawToken())) {
                var childStart = lexer.tokenRange().start();
                var dispatch = tokenCount;
                try {
                    parseMethodChild(statement, token, childStart);
                } catch (Sync _) {
                    synchronize(true, dispatch);
                }
            }
        }else if(!isStatementEnd(objectStartOrStatementEnd)) {
            throw error(ProtobufSyntaxErrors.expectedMethodBodyOrSemicolon(objectStartOrStatementEnd));
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of an rpc method body introduced by the given token and adds it to the
     * method.
     *
     * @param statement the method that contains the statement
     * @param token the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseMethodChild(ProtobufMethodStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        switch (token) {
            case STATEMENT_END -> {
                var child = parseEmpty(childStart);
                statement.addChild(child);
            }
            case "option" -> {
                var child = parseOptionStatement(childStart);
                statement.addChild(child);
            }
            default -> throw error(ProtobufSyntaxErrors.unexpectedMethodBodyToken(token));
        }
    }

    private ProtobufMethodStatement.Type parseMethodType() {
        var typeStart = nextRawToken();
        expect(isParensStart(typeStart), ProtobufSyntaxErrors.expectedToken(PARENS_START, typeStart));
        var typeOrModifier = nextRawToken();
        ProtobufTypeReference typeReference;
        boolean stream;
        if(isStreamModifier(typeOrModifier)) {
            var type = nextRawToken();
            expect(isValidType(type), ProtobufSyntaxErrors.invalidMethodType(type));
            typeReference = new ProtobufUnresolvedTypeReference(type);
            stream = true;
        }else {
            expect(isValidType(typeOrModifier), ProtobufSyntaxErrors.invalidMethodType(typeOrModifier));
            typeReference = new ProtobufUnresolvedTypeReference(typeOrModifier);
            stream = false;
        }

        var typeEnd = nextRawToken();
        expect(isParensEnd(typeEnd), ProtobufSyntaxErrors.expectedToken(PARENS_END, typeEnd));
        return new ProtobufMethodStatement.Type(typeReference, stream);
    }

    private ProtobufOneofStatement parseOneof(ProtobufTreeSourcePosition start) {
        var statement = new ProtobufOneofStatement(document, ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(statement);
        var name = nextRawToken();
        expect(isValidIdent(name), ProtobufSyntaxErrors.invalidOneofName(name));
        statement.setName(name);
        var objectStart = nextRawToken();
        expect(isBodyStart(objectStart), ProtobufSyntaxErrors.expectedToken(BODY_START, objectStart));
        String token;
        while (!isBodyEnd(token = nextRawToken())) {
            var childStart = lexer.tokenRange().start();
            var dispatch = tokenCount;
            try {
                parseOneofChild(statement, token, childStart);
            } catch (Sync _) {
                synchronize(true, dispatch);
            }
        }
        return finishStatement(statement, start);
    }

    /**
     * Parses a statement of a oneof body introduced by the given token and adds it to the oneof.
     *
     * @param statement the oneof that contains the statement
     * @param token the first token of the statement
     * @param childStart the position of the first token of the statement
     * @throws Sync if the statement is malformed
     */
    private void parseOneofChild(ProtobufOneofStatement statement, String token, ProtobufTreeSourcePosition childStart) {
        switch (token) {
            case "option" -> {
                var child = parseOptionStatement(childStart);
                statement.addChild(child);
            }
            default -> {
                var child = parseField(ProtobufModifier.NONE, token, childStart);
                statement.addChild(child);
            }
        }
    }

    private ProtobufImportStatement parseImport(ProtobufTreeSourcePosition start) {
        var importStatement = new ProtobufImportStatement(ProtobufTreeSourceRange.UNKNOWN);
        attachLeadingComments(importStatement);
        var token = nextToken();
        switch (token) {
            case ProtobufLiteralToken literal -> {
                importStatement.setModifier(ProtobufImportStatement.Modifier.NONE);
                importStatement.setLocation(literal.value());
                var end = nextRawToken();
                expectStatementEnd(end, ProtobufSyntaxErrors.expectedToken(STATEMENT_END, end));
            }
            case ProtobufRawToken raw -> {
                var modifier = ProtobufImportStatement.Modifier.of(raw.value());
                if (modifier.isEmpty()) {
                    throw error(ProtobufSyntaxErrors.unknownImportModifier(raw.value()));
                }
                var version = document.version();
                switch (modifier.get()) {
                    case WEAK -> {
                        if (version == ProtobufVersion.EDITION_2024) {
                            report(ProtobufSemanticErrors.weakImportInEdition2024());
                        }
                    }
                    case OPTION -> {
                        if (version != ProtobufVersion.EDITION_2024) {
                            report(ProtobufSemanticErrors.optionImportRequiresEdition2024());
                        }
                    }
                    case PUBLIC, NONE -> {}
                }
                importStatement.setModifier(modifier.get());
                var locationToken = nextToken();
                if(!(locationToken instanceof ProtobufLiteralToken(var location, _))) {
                    throw error(ProtobufSyntaxErrors.expectedImportLocation(locationToken.toString()));
                }
                importStatement.setLocation(location);
                var end = nextRawToken();
                expectStatementEnd(end, ProtobufSyntaxErrors.expectedToken(STATEMENT_END, end));
            }
            default -> throw error(ProtobufSyntaxErrors.expectedImportLocation(token.toString()));
        }
        return finishStatement(importStatement, start);
    }

    private ProtobufExpression readExpression() {
        return switch (nextToken()) {
            case ProtobufBoolToken(var value) -> new ProtobufBoolExpression(value);
            case ProtobufLiteralToken literal -> new ProtobufLiteralExpression(literal.value());
            case ProtobufNumberToken(var number) -> switch (number) {
                case ProtobufFloatingPoint floatingPoint -> new ProtobufFloatingPointExpression(floatingPoint);
                case ProtobufInteger integer -> new ProtobufIntegerExpression(integer);
            };
            case ProtobufRawToken raw when isValidIdent(raw.value()) -> new ProtobufEnumConstantExpression(raw.value());
            case ProtobufRawToken raw when isNullExpression(raw.value()) -> new ProtobufNullExpression();
            case ProtobufRawToken raw when isBodyStart(raw.value()) -> {
                var data = new LinkedHashMap<String, ProtobufExpression>();
                var keyOrEndToken = nextToken();
                while (true) {
                    if (!(keyOrEndToken instanceof ProtobufRawToken(var keyOrEndValue))) {
                        throw error(ProtobufSyntaxErrors.expectedMessageLiteralKey(keyOrEndToken.toString()));
                    }

                    if (isBodyEnd(keyOrEndValue)) {
                        break;
                    }

                    var keyValueSeparatorToken = nextRawToken();
                    expect(isKeyValueSeparatorOperator(keyValueSeparatorToken), ProtobufSyntaxErrors.expectedToken(KEY_VALUE_SEPARATOR, keyValueSeparatorToken));

                    var valueToken = readExpression();
                    data.put(keyOrEndValue, valueToken);

                    var nextToken = nextToken();
                    keyOrEndToken = nextToken instanceof ProtobufRawToken(var nextValue) && isArraySeparator(nextValue)
                            ? nextToken()
                            : nextToken;
                }
                yield new ProtobufJsonExpression(data);
            }
            case ProtobufRawToken raw -> throw error(ProtobufSyntaxErrors.invalidOptionValue(raw.value()));
        };
    }

    private String parseOptions(String currentToken, ProtobufTree.WithOptions child) {
        if(!isArrayStart(currentToken)) {
            return currentToken;
        }

        String optionSeparatorOrOptionEnd;
        do {
            var expression = parseOptionExpression();
            child.addOption(expression);
        }while (isArraySeparator(optionSeparatorOrOptionEnd = nextRawToken()));

        expect(isArrayEnd(optionSeparatorOrOptionEnd), ProtobufSyntaxErrors.expectedToken(ARRAY_END, optionSeparatorOrOptionEnd));

        return nextRawToken();
    }

    private static boolean isKeyValueSeparatorOperator(String keyValueSeparatorToken) {
        return Objects.equals(keyValueSeparatorToken, KEY_VALUE_SEPARATOR);
    }

    private static boolean isAssignmentOperator(String operator) {
        return Objects.equals(operator, ASSIGNMENT_OPERATOR);
    }

    private static boolean isBodyStart(String operator) {
        return Objects.equals(operator, BODY_START);
    }

    private static boolean isBodyEnd(String operator) {
        return Objects.equals(operator, BODY_END);
    }

    private static boolean isArrayStart(String operator) {
        return Objects.equals(operator, ARRAY_START);
    }

    private static boolean isArrayEnd(String operator) {
        return Objects.equals(operator, ARRAY_END);
    }

    private static boolean isTypeParametersStart(String operator) {
        return Objects.equals(operator, TYPE_PARAMETERS_START);
    }

    private static boolean isTypeParametersEnd(String operator) {
        return Objects.equals(operator, TYPE_PARAMETERS_END);
    }

    private static boolean isStatementEnd(String operator) {
        return Objects.equals(operator, STATEMENT_END);
    }

    private static boolean isArraySeparator(String operator) {
        return Objects.equals(operator, ARRAY_SEPARATOR);
    }

    private static boolean isRangeOperator(String operator) {
        return Objects.equals(operator, RANGE_OPERATOR);
    }

    private static boolean isValidPackage(String token) {
        var length = token.length();
        if(length == 0) {
            return false;
        }

        var start = 0;
        for(var end = start + 1; end < length; end++) {
            if (token.charAt(end) != '.') {
                continue;
            }

            if(!isValidIdent(token.substring(start, end))) {
                return false;
            }

            start = end + 1;
        }

        return isValidIdent(token.substring(start, length));
    }

    private static boolean isValidType(String token) {
        var length = token.length();
        if(length == 0) {
            return false;
        }

        if(token.charAt(length - 1) == '.') {
            return false;
        }

        int start;
        if(token.charAt(0) == '.') {
            if(length == 1) {
                return false;
            }
            start = 1;
        }else {
            start = 0;
        }

        for(var end = start + 1; end < length; end++) {
            if (token.charAt(end) != '.') {
                continue;
            }

            if(!isValidIdent(token.substring(start, end))) {
                return false;
            }

            start = end + 1;
        }

        return isValidIdent(token.substring(start, length));
    }

    private static boolean isValidIdent(String token) {
        var length = token.length();
        if(length == 0) {
            return false;
        }

        var head = token.charAt(0);
        if(!Character.isLetter(head) && token.charAt(0) != '_') {
            return false;
        }

        for(var i = 1; i < length; i++) {
            var entry = token.charAt(i);
            if(!Character.isLetterOrDigit(entry) && entry != '_') {
                return false;
            }
        }

        return true;
    }

    private static boolean isNullExpression(String token) {
        return Objects.equals(token, NULL);
    }

    private static boolean isParensStart(String typeStart) {
        return Objects.equals(typeStart, PARENS_START);
    }

    private static boolean isParensEnd(String typeEnd) {
        return Objects.equals(typeEnd, PARENS_END);
    }

    private static boolean isStreamModifier(String typeOrModifier) {
        return Objects.equals(typeOrModifier, STREAM);
    }

    private static boolean isMax(String token) {
        return Objects.equals(token, MAX);
    }

    private static boolean isGroupType(String token) {
        return Objects.equals(token, GROUP);
    }

    private static boolean isMapType(String token) {
        return Objects.equals(token, MAP);
    }

    /**
     * An unchecked signal that unwinds the parser from a malformed statement to the body loop that
     * contains it, where {@link #synchronize(boolean, int)} skips the rest of the statement.
     *
     * <p>It never escapes the parser. The singleton instance carries no stack trace.
     */
    private static final class Sync extends RuntimeException {
        /**
         * The singleton instance of this signal.
         */
        private static final Sync INSTANCE = new Sync();

        /**
         * Constructs the singleton instance, without a message, a cause or a stack trace.
         */
        private Sync() {
            super(null, null, false, false);
        }
    }

    /**
     * An unchecked signal that unwinds the parser when the input ends in the middle of a
     * declaration.
     *
     * <p>It never escapes the parser: {@link #parseDocument()} catches it and returns the
     * declarations parsed so far. The singleton instance carries no stack trace.
     */
    private static final class Abort extends RuntimeException {
        /**
         * The singleton instance of this signal.
         */
        private static final Abort INSTANCE = new Abort();

        /**
         * Constructs the singleton instance, without a message, a cause or a stack trace.
         */
        private Abort() {
            super(null, null, false, false);
        }
    }
}
