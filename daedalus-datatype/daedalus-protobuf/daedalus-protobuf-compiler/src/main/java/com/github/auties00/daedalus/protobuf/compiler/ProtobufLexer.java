package com.github.auties00.daedalus.protobuf.compiler;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufLexicalErrors;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufFloatingPoint;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufFloatingPoint.Infinity.Signum;
import com.github.auties00.daedalus.protobuf.compiler.number.ProtobufInteger;
import com.github.auties00.daedalus.protobuf.compiler.token.ProtobufBoolToken;
import com.github.auties00.daedalus.protobuf.compiler.token.ProtobufLiteralToken;
import com.github.auties00.daedalus.protobuf.compiler.token.ProtobufNumberToken;
import com.github.auties00.daedalus.protobuf.compiler.token.ProtobufRawToken;
import com.github.auties00.daedalus.protobuf.compiler.token.ProtobufToken;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufCommentStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourcePosition;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourceRange;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Performs lexical analysis (tokenization) of Protocol Buffer definition files.
 *
 * <p>The lexer is a hand-written single-pass scanner that reads the entire input into a
 * {@code char[]} buffer up front. Position information is tracked as 1-indexed line and column
 * numbers; tabs are treated as a single column for compatibility with {@code protoc}.
 *
 * <p>It recognizes:
 * <ul>
 * <li>Keyword and identifier tokens</li>
 * <li>Numeric literals: decimal, hexadecimal ({@code 0x}), octal ({@code 0}), and floating-point
 *     including {@code inf}, {@code -inf}, and {@code nan}</li>
 * <li>String literals delimited by either {@code "} or {@code '}, with adjacent literals
 *     concatenated automatically</li>
 * <li>Boolean literals ({@code true}/{@code false})</li>
 * <li>Single-character punctuation: {@code { } ; = [ ] , ( ) < > :}</li>
 * <li>Comments: line comments ({@code //}) and block comments ({@code /* *}{@code /})</li>
 * </ul>
 *
 * <p>The lexer also captures comments and categorizes them as leading, trailing, or detached
 * according to {@code protoc}'s {@code Tokenizer::NextWithComments} algorithm. The parser drains
 * these via {@link #consumePendingTrailing()}, {@link #consumePendingDetached()}, and
 * {@link #consumePendingLeading()} around each statement it constructs.
 *
 * @see ProtobufToken
 * @see ProtobufParser
 */
public final class ProtobufLexer {
    private static final String POSITIVE_INFINITY_KEYWORD = "inf";
    private static final ProtobufToken POSITIVE_INFINITY = new ProtobufNumberToken(new ProtobufFloatingPoint.Infinity(Signum.POSITIVE));

    private static final String NEGATIVE_INFINITY_TOKEN = "-inf";
    private static final ProtobufToken NEGATIVE_INFINITY = new ProtobufNumberToken(new ProtobufFloatingPoint.Infinity(Signum.NEGATIVE));

    private static final String NOT_A_NUMBER_TOKEN = "nan";
    private static final ProtobufToken NOT_A_NUMBER = new ProtobufNumberToken(new ProtobufFloatingPoint.NaN());

    private static final String TRUE_TOKEN = "true";
    private static final ProtobufToken TRUE = new ProtobufBoolToken(true);

    private static final String FALSE_TOKEN = "false";
    private static final ProtobufToken FALSE = new ProtobufBoolToken(false);

    private static final String EMPTY_TOKEN = "";
    private static final ProtobufToken EMPTY = new ProtobufRawToken(EMPTY_TOKEN);

    private static final int INVALID_RADIX = -1;
    private static final int OCTAL_RADIX = 8;
    private static final int DECIMAL_RADIX = 10;
    private static final int HEXADECIMAL_RADIX = 16;

    private static final long DECIMAL_RADIX_LIMIT = Long.divideUnsigned(-1L, 10); // 1844674407370955161L
    private static final long DECIMAL_RADIX_MAX_DIGIT = Long.remainderUnsigned(-1L, 10); // 5

    // Branch-free classification table for the ASCII range. Index by char; the lookup is the
    // entire characterization of the char (whitespace, word, quote, comment-prefix, ...) so the
    // hot path can branch on a single load.
    private static final boolean[] WORD_CHAR = new boolean[256];

    static {
        for (var c = 'a'; c <= 'z'; c++) {
            WORD_CHAR[c] = true;
        }
        for (var c = 'A'; c <= 'Z'; c++) {
            WORD_CHAR[c] = true;
        }
        for (var c = '0'; c <= '9'; c++) {
            WORD_CHAR[c] = true;
        }
        WORD_CHAR['_'] = true;
        WORD_CHAR['.'] = true;
        WORD_CHAR['-'] = true;
        WORD_CHAR['+'] = true;
        for (var c = 160; c <= 255; c++) {
            WORD_CHAR[c] = true;
        }
    }

    private static boolean isWordChar(char c) {
        return c < 256 && WORD_CHAR[c];
    }

    private final char[] buf;
    private final int length;
    private final Path location;
    private final ProtobufDiagnosticator diagnostics;
    private int pos;
    private int line;
    private int column;

    // Position bracketing the most recently consumed token.
    private int tokenStartLine;
    private int tokenStartColumn;
    private int tokenEndLine;
    private int tokenEndColumn;

    // One-token pushback state.
    private boolean hasPushback;
    private int pushbackPos;
    private int pushbackLine;
    private int pushbackColumn;
    private int pushbackTokenStartLine;
    private int pushbackTokenStartColumn;
    private int pushbackTokenEndLine;
    private int pushbackTokenEndColumn;
    private int pushbackPendingCommentMark;
    private boolean pushbackHadTrailing;
    private ProtobufCommentStatement.Trailing pushbackTrailing;
    private int pushbackLastConsumedTokenEndLine;

    // Comment buffers populated by the comment-aware scan path and drained by the parser.
    private final List<ProtobufCommentStatement.LeadingDetached> pendingDetached;
    private final List<ProtobufCommentStatement.Leading> pendingLeading;
    private ProtobufCommentStatement.Trailing pendingTrailing;
    private int lastConsumedTokenEndLine;

    /**
     * Constructs a new Protocol Buffer lexer that fully reads the given character stream into
     * memory.
     *
     * <p>The reader is consumed eagerly so that the lexer can perform a single forward pass over
     * the resulting {@code char[]} buffer. The reader is not closed by the lexer.
     *
     * <p>Lexical errors, such as an unterminated string literal, are recovered from silently by a
     * lexer created with this constructor. Use {@link ProtobufParser} to obtain diagnostics for
     * them.
     *
     * @param reader the character stream to tokenize, must not be null
     * @throws IOException if an I/O error occurs while reading from {@code reader}
     */
    public ProtobufLexer(Reader reader) throws IOException {
        this(reader, null, new ProtobufDiagnosticator());
    }

    /**
     * Constructs a new Protocol Buffer lexer that fully reads the given character stream into
     * memory and reports lexical errors to the given collector.
     *
     * @param reader the character stream to tokenize, must not be null
     * @param location the file being tokenized, or {@code null} if the source was not read from a
     *                 file
     * @param diagnostics the collector that receives lexical errors, must not be null
     * @throws IOException if an I/O error occurs while reading from {@code reader}
     */
    ProtobufLexer(Reader reader, Path location, ProtobufDiagnosticator diagnostics) throws IOException {
        var initial = new char[8192];
        var collected = 0;
        while (true) {
            if (collected == initial.length) {
                var grown = new char[initial.length * 2];
                System.arraycopy(initial, 0, grown, 0, collected);
                initial = grown;
            }
            var read = reader.read(initial, collected, initial.length - collected);
            if (read < 0) {
                break;
            }
            collected += read;
        }
        this.buf = initial;
        this.length = collected;
        this.location = location;
        this.diagnostics = diagnostics;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
        this.tokenStartLine = 1;
        this.tokenStartColumn = 1;
        this.tokenEndLine = 1;
        this.tokenEndColumn = 1;
        this.lastConsumedTokenEndLine = 0;
        this.pendingDetached = new ArrayList<>();
        this.pendingLeading = new ArrayList<>();
        this.pendingTrailing = null;
    }

    /**
     * Returns the source range covering the most recently consumed token.
     *
     * @return the source range
     */
    public ProtobufTreeSourceRange tokenRange() {
        return new ProtobufTreeSourceRange(
                new ProtobufTreeSourcePosition(tokenStartLine, tokenStartColumn),
                new ProtobufTreeSourcePosition(tokenEndLine, tokenEndColumn)
        );
    }

    /**
     * Returns the position the lexer is currently about to read from.
     *
     * <p>This is useful for capturing the start position of a statement before any of its tokens
     * have been consumed: the parser calls this immediately before its first
     * {@link #nextRawToken()} or {@link #nextToken()} call.
     *
     * @return the current position
     */
    public ProtobufTreeSourcePosition currentPosition() {
        consumePushback();
        snapshotPushback();
        skipWhitespaceAndComments();
        return new ProtobufTreeSourcePosition(line, column);
    }

    /**
     * Returns and clears the trailing comment associated with the previously consumed token.
     *
     * <p>This method performs a lookahead through any whitespace and comments following the most
     * recently consumed token in order to detect a same-line trailing comment, so it should be
     * called immediately after the parser consumes a statement terminator.
     *
     * @return the trailing comment, or {@code null} if none is pending
     */
    public ProtobufCommentStatement.Trailing consumePendingTrailing() {
        consumePushback();
        snapshotPushback();
        skipWhitespaceAndComments();
        var result = pendingTrailing;
        pendingTrailing = null;
        return result;
    }

    /**
     * Returns and clears any detached leading comments associated with the next token.
     *
     * @return an unmodifiable sequenced collection of detached comments, possibly empty
     */
    public SequencedCollection<ProtobufCommentStatement> consumePendingDetached() {
        if (pendingDetached.isEmpty()) {
            return Collections.emptyList();
        }
        var copy = new ArrayList<ProtobufCommentStatement>(pendingDetached);
        pendingDetached.clear();
        return Collections.unmodifiableList(copy);
    }

    /**
     * Returns and clears the most recent leading comment associated with the next token.
     *
     * <p>{@code protoc} attaches at most a single leading comment block per statement. If multiple
     * blocks were collected, all but the last become detached comments and only the last one is
     * returned here.
     *
     * @return the leading comment, or {@code null} if none is pending
     */
    public ProtobufCommentStatement.Leading consumePendingLeading() {
        if (pendingLeading.isEmpty()) {
            return null;
        }
        var result = pendingLeading.getLast();
        pendingLeading.clear();
        return result;
    }

    /**
     * Moves back the tokenizer to the previous token.
     *
     * <p>The lexer supports a single token of pushback. Calling this method twice without an
     * intervening {@link #nextRawToken()} or {@link #nextToken()} is unsupported.
     */
    public void moveToPreviousToken() {
        hasPushback = true;
    }

    /**
     * Reads the next raw token as a string.
     *
     * <p>String literals are returned without their delimiters, so the caller cannot distinguish
     * them from other tokens; use {@link #nextToken()} when that distinction matters.
     *
     * @return the next raw token string, or {@code null} if the end of input has been reached
     */
    public String nextRawToken() {
        consumePushback();
        snapshotPushback();
        skipWhitespaceAndComments();
        if (pos >= length) {
            return null;
        }
        var c = buf[pos];
        if (c == '"' || c == '\'') {
            var literal = scanStringLiteral(c);
            recordTokenEnd();
            return concatAdjacentRawStrings(literal);
        }
        if (!isWordChar(c)) {
            captureTokenStart();
            consumeChar();
            recordTokenEnd();
            return String.valueOf(c);
        }
        captureTokenStart();
        var start = pos;
        readWordChars();
        recordTokenEnd();
        return new String(buf, start, pos - start);
    }

    private String concatAdjacentRawStrings(String head) {
        var headStartLine = tokenStartLine;
        var headStartColumn = tokenStartColumn;
        StringBuilder builder = null;
        while (true) {
            var savedPos = pos;
            var savedLine = line;
            var savedColumn = column;
            var savedStartLine = tokenStartLine;
            var savedStartColumn = tokenStartColumn;
            var savedEndLine = tokenEndLine;
            var savedEndColumn = tokenEndColumn;

            skipPlainWhitespace();
            if (pos >= length || (buf[pos] != '"' && buf[pos] != '\'')) {
                pos = savedPos;
                line = savedLine;
                column = savedColumn;
                tokenStartLine = savedStartLine;
                tokenStartColumn = savedStartColumn;
                tokenEndLine = savedEndLine;
                tokenEndColumn = savedEndColumn;
                return builder == null ? head : builder.toString();
            }
            var quote = buf[pos];
            var literal = scanStringLiteral(quote);
            recordTokenEnd();
            // The concatenated token starts at the first fragment, not at the one just scanned.
            tokenStartLine = headStartLine;
            tokenStartColumn = headStartColumn;
            if (builder == null) {
                builder = new StringBuilder();
                builder.append(head);
            }
            builder.append(literal);
        }
    }

    /**
     * Reads and returns the next typed token from the input stream.
     *
     * @return the next token, or {@code null} if the end of input has been reached
     */
    public ProtobufToken nextToken() {
        consumePushback();
        snapshotPushback();
        skipWhitespaceAndComments();
        if (pos >= length) {
            return null;
        }
        var c = buf[pos];
        if (c == '"') {
            var literal = scanStringLiteral('"');
            recordTokenEnd();
            var concatenated = concatAdjacentTypedStrings(literal);
            return new ProtobufLiteralToken(concatenated, '"');
        }
        if (c == '\'') {
            var literal = scanStringLiteral('\'');
            recordTokenEnd();
            var concatenated = concatAdjacentTypedStrings(literal);
            return new ProtobufLiteralToken(concatenated, '\'');
        }
        if (!isWordChar(c)) {
            captureTokenStart();
            consumeChar();
            recordTokenEnd();
            return new ProtobufRawToken(String.valueOf(c));
        }
        captureTokenStart();
        var start = pos;
        readWordChars();
        recordTokenEnd();
        var word = new String(buf, start, pos - start);
        return parseWord(word);
    }

    private String concatAdjacentTypedStrings(String head) {
        var headStartLine = tokenStartLine;
        var headStartColumn = tokenStartColumn;
        StringBuilder builder = null;
        while (true) {
            var savedPos = pos;
            var savedLine = line;
            var savedColumn = column;
            var savedStartLine = tokenStartLine;
            var savedStartColumn = tokenStartColumn;
            var savedEndLine = tokenEndLine;
            var savedEndColumn = tokenEndColumn;

            skipPlainWhitespace();
            if (pos >= length || (buf[pos] != '"' && buf[pos] != '\'')) {
                pos = savedPos;
                line = savedLine;
                column = savedColumn;
                tokenStartLine = savedStartLine;
                tokenStartColumn = savedStartColumn;
                tokenEndLine = savedEndLine;
                tokenEndColumn = savedEndColumn;
                return builder == null ? head : builder.toString();
            }
            var quote = buf[pos];
            var literal = scanStringLiteral(quote);
            recordTokenEnd();
            // The concatenated token starts at the first fragment, not at the one just scanned.
            tokenStartLine = headStartLine;
            tokenStartColumn = headStartColumn;
            if (builder == null) {
                builder = new StringBuilder();
                builder.append(head);
            }
            builder.append(literal);
        }
    }

    /**
     * Skips ASCII whitespace (spaces, tabs, newlines, form feeds, vertical tabs) without
     * collecting comments. Used by the speculative string-concatenation lookahead.
     */
    private void skipPlainWhitespace() {
        while (pos < length) {
            var c = buf[pos];
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B') {
                consumeChar();
            } else {
                break;
            }
        }
    }

    private void captureTokenStart() {
        tokenStartLine = line;
        tokenStartColumn = column;
    }

    private void recordTokenEnd() {
        tokenEndLine = line;
        tokenEndColumn = column;
        lastConsumedTokenEndLine = line;
    }

    private void readWordChars() {
        while (pos < length && isWordChar(buf[pos])) {
            consumeChar();
        }
    }

    private void consumeChar() {
        var c = buf[pos++];
        if (c == '\n') {
            line++;
            column = 1;
        } else if (c == '\r') {
            if (pos < length && buf[pos] == '\n') {
                pos++;
            }
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    /**
     * Scans a string literal starting at the current position. The opening quote is consumed; the
     * returned value is the unescaped (well, raw) literal contents excluding the delimiters.
     *
     * <p>Escape sequences are accepted but not decoded — they are passed through verbatim, matching
     * the previous {@link java.io.StreamTokenizer}-based behavior. The closing quote is consumed.
     *
     * <p>If the literal is not closed before the end of its line or of the input, an unterminated
     * string diagnostic is reported and the content scanned so far is returned; the line
     * terminator, if any, is left in the input.
     */
    private String scanStringLiteral(char delimiter) {
        captureTokenStart();
        consumeChar(); // opening quote
        var contentStart = pos;
        var hasEscape = false;
        while (pos < length) {
            var c = buf[pos];
            if (c == delimiter) {
                String value;
                if (hasEscape) {
                    value = decodeEscapes(buf, contentStart, pos);
                } else {
                    value = new String(buf, contentStart, pos - contentStart);
                }
                consumeChar(); // closing quote
                return value;
            }
            if (c == '\\') {
                hasEscape = true;
                consumeChar();
                if (pos < length) {
                    consumeChar();
                }
                continue;
            }
            if (c == '\n' || c == '\r') {
                break;
            }
            consumeChar();
        }
        diagnostics.report(location, new ProtobufTreeSourceRange(
                new ProtobufTreeSourcePosition(tokenStartLine, tokenStartColumn),
                new ProtobufTreeSourcePosition(line, column)
        ), ProtobufLexicalErrors.unterminatedString());
        return hasEscape ? decodeEscapes(buf, contentStart, pos) : new String(buf, contentStart, pos - contentStart);
    }

    private static String decodeEscapes(char[] src, int start, int end) {
        var sb = new StringBuilder(end - start);
        var i = start;
        while (i < end) {
            var c = src[i++];
            if (c != '\\' || i >= end) {
                sb.append(c);
                continue;
            }
            var esc = src[i++];
            switch (esc) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case '"' -> sb.append('"');
                case '\'' -> sb.append('\'');
                case '\\' -> sb.append('\\');
                case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    var value = esc - '0';
                    var consumed = 1;
                    while (consumed < 3 && i < end && src[i] >= '0' && src[i] <= '7') {
                        value = value * 8 + (src[i++] - '0');
                        consumed++;
                    }
                    sb.append((char) value);
                }
                case 'x', 'X' -> {
                    var value = 0;
                    var consumed = 0;
                    while (consumed < 2 && i < end) {
                        var hx = src[i];
                        int digit;
                        if (hx >= '0' && hx <= '9') {
                            digit = hx - '0';
                        } else if (hx >= 'a' && hx <= 'f') {
                            digit = hx - 'a' + 10;
                        } else if (hx >= 'A' && hx <= 'F') {
                            digit = hx - 'A' + 10;
                        } else {
                            break;
                        }
                        value = value * 16 + digit;
                        i++;
                        consumed++;
                    }
                    if (consumed == 0) {
                        // Bare \x - leave as-is rather than producing nothing.
                        sb.append('\\').append(esc);
                    } else {
                        sb.append((char) value);
                    }
                }
                default -> {
                    sb.append('\\').append(esc);
                }
            }
        }
        return sb.toString();
    }

    private void skipWhitespaceAndComments() {
        // Track whether we have already attempted to capture a trailing comment for the previously
        // consumed token. Only one trailing comment is recognized per token, matching protoc.
        var trailingChecked = pendingTrailing != null;

        var newlinesSinceLastEvent = 0;

        // Buffer the in-progress block of comments. They are flushed as detached or leading once
        // we know what's adjacent to the next real token.
        var currentBlock = new ArrayList<ProtobufCommentStatement>();

        while (true) {
            // Skip horizontal whitespace.
            while (pos < length) {
                var c = buf[pos];
                if (c == ' ' || c == '\t' || c == '\f' || c == '\u000B') {
                    consumeChar();
                } else if (c == '\n' || c == '\r') {
                    consumeChar();
                    newlinesSinceLastEvent++;
                } else {
                    break;
                }
            }

            if (pos >= length) {
                // EOF: anything we've collected becomes detached, since there's no following token.
                if (!currentBlock.isEmpty()) {
                    flushCurrentBlockDetached(currentBlock);
                }
                return;
            }

            var c = buf[pos];
            if (c == '/' && pos + 1 < length) {
                var next = buf[pos + 1];
                if (next == '/' || next == '*') {
                    // Decide whether this comment can be a trailing comment.
                    if (!trailingChecked && newlinesSinceLastEvent == 0 && lastConsumedTokenEndLine != 0 && lastConsumedTokenEndLine == line && pendingDetached.isEmpty() && pendingLeading.isEmpty()) {
                        pendingTrailing = readCommentAsTrailing(next == '*');
                        trailingChecked = true;
                        continue;
                    }
                    trailingChecked = true;
                    // Otherwise this comment is part of an in-progress block.
                    if (!currentBlock.isEmpty() && newlinesSinceLastEvent >= 2) {
                        // Blank line separator, flush previous block as detached.
                        flushCurrentBlockDetached(currentBlock);
                    } else if (currentBlock.isEmpty() && newlinesSinceLastEvent >= 2) {
                        // Blank line separates this block from the previous token; that's expected.
                    }
                    var comment = readCommentBlock(next == '*');
                    currentBlock.add(comment);
                    newlinesSinceLastEvent = 0;
                    continue;
                }
            }

            // Real token starts here. Decide what to do with the buffered block.
            if (!currentBlock.isEmpty()) {
                if (newlinesSinceLastEvent >= 2) {
                    // Blank line between block and next token, all blocks become detached.
                    flushCurrentBlockDetached(currentBlock);
                } else {
                    // Block is adjacent to the token, leading comment.
                    var leadingText = joinBlockText(currentBlock);
                    var leadingRange = new ProtobufTreeSourceRange(
                            currentBlock.getFirst().range().start(),
                            currentBlock.getLast().range().end()
                    );
                    pendingLeading.add(new ProtobufCommentStatement.Leading(leadingText, leadingRange));
                    currentBlock.clear();
                }
            }
            return;
        }
    }

    private void flushCurrentBlockDetached(List<ProtobufCommentStatement> currentBlock) {
        var text = joinBlockText(currentBlock);
        var range = new ProtobufTreeSourceRange(
                currentBlock.getFirst().range().start(),
                currentBlock.getLast().range().end()
        );
        pendingDetached.add(new ProtobufCommentStatement.LeadingDetached(text, range));
        currentBlock.clear();
    }

    private static String joinBlockText(List<ProtobufCommentStatement> block) {
        if (block.size() == 1) {
            return block.getFirst().text();
        }
        var sb = new StringBuilder();
        for (var i = 0; i < block.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(block.get(i).text());
        }
        return sb.toString();
    }

    private ProtobufCommentStatement.Trailing readCommentAsTrailing(boolean block) {
        var startLine = line;
        var startColumn = column;
        var rawText = block ? readBlockComment() : readLineComment();
        var range = new ProtobufTreeSourceRange(
                new ProtobufTreeSourcePosition(startLine, startColumn),
                new ProtobufTreeSourcePosition(line, column)
        );
        return new ProtobufCommentStatement.Trailing(rawText, range);
    }

    private ProtobufCommentStatement readCommentBlock(boolean block) {
        var startLine = line;
        var startColumn = column;
        var rawText = block ? readBlockComment() : readLineComment();
        var range = new ProtobufTreeSourceRange(
                new ProtobufTreeSourcePosition(startLine, startColumn),
                new ProtobufTreeSourcePosition(line, column)
        );
        return new ProtobufCommentStatement.LeadingDetached(rawText, range);
    }

    private String readLineComment() {
        // Consume the leading "//".
        consumeChar();
        consumeChar();
        var start = pos;
        while (pos < length) {
            var c = buf[pos];
            if (c == '\n' || c == '\r') {
                break;
            }
            consumeChar();
        }
        var raw = new String(buf, start, pos - start);
        // Strip at most one leading space, matching protoc's normalization.
        if (!raw.isEmpty() && raw.charAt(0) == ' ') {
            return raw.substring(1);
        }
        return raw;
    }

    private String readBlockComment() {
        // Consume the leading "/*".
        consumeChar();
        consumeChar();
        var start = pos;
        var endContent = -1;
        while (pos < length - 1) {
            if (buf[pos] == '*' && buf[pos + 1] == '/') {
                endContent = pos;
                consumeChar();
                consumeChar();
                break;
            }
            consumeChar();
        }
        if (endContent < 0) {
            // Unterminated; consume to EOF and use what we got.
            endContent = pos;
            while (pos < length) {
                consumeChar();
            }
        }
        return normalizeBlockComment(buf, start, endContent);
    }

    private static String normalizeBlockComment(char[] src, int start, int end) {
        // Split the contents into lines and normalize each according to protoc's rules: leading
        // whitespace stripped, optional leading '*' removed, optional single space after '*' removed.
        var raw = new String(src, start, end - start);
        var lines = raw.split("\\r\\n|\\r|\\n", -1);
        if (lines.length == 1) {
            return trimSingleLineBlockComment(lines[0]);
        }
        var sb = new StringBuilder();
        for (var i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            var lineContent = lines[i];
            if (i == 0) {
                // First line: strip a single leading space if present.
                if (!lineContent.isEmpty() && lineContent.charAt(0) == ' ') {
                    lineContent = lineContent.substring(1);
                }
                // Trim trailing whitespace because protoc does.
                lineContent = stripTrailingWhitespace(lineContent);
                sb.append(lineContent);
                continue;
            }
            // Strip leading whitespace.
            var idx = 0;
            while (idx < lineContent.length()
                    && (lineContent.charAt(idx) == ' ' || lineContent.charAt(idx) == '\t')) {
                idx++;
            }
            // Strip optional '*'.
            if (idx < lineContent.length() && lineContent.charAt(idx) == '*') {
                idx++;
                if (idx < lineContent.length() && lineContent.charAt(idx) == ' ') {
                    idx++;
                }
            }
            sb.append(stripTrailingWhitespace(lineContent.substring(idx)));
        }
        return sb.toString();
    }

    private static String trimSingleLineBlockComment(String content) {
        var s = content;
        if (!s.isEmpty() && s.charAt(0) == ' ') {
            s = s.substring(1);
        }
        return stripTrailingWhitespace(s);
    }

    private static String stripTrailingWhitespace(String s) {
        var i = s.length();
        while (i > 0) {
            var c = s.charAt(i - 1);
            if (c == ' ' || c == '\t') {
                i--;
            } else {
                break;
            }
        }
        return s.substring(0, i);
    }

    private static ProtobufToken parseWord(String token) {
        return switch (token) {
            case EMPTY_TOKEN -> EMPTY;
            case TRUE_TOKEN -> TRUE;
            case FALSE_TOKEN -> FALSE;
            case POSITIVE_INFINITY_KEYWORD -> POSITIVE_INFINITY;
            case NEGATIVE_INFINITY_TOKEN -> NEGATIVE_INFINITY;
            case NOT_A_NUMBER_TOKEN -> NOT_A_NUMBER;
            default -> parseNumber(token);
        };
    }

    private static ProtobufToken parseNumber(String token) {
        var length = token.length();
        if (length == 0) {
            return EMPTY;
        }

        var start = 0;

        boolean isNegative;
        switch (token.charAt(start)) {
            case '+' -> {
                if (++start >= length) {
                    return new ProtobufRawToken(token);
                }
                isNegative = false;
            }
            case '-' -> {
                if (++start >= length) {
                    return new ProtobufRawToken(token);
                }
                isNegative = true;
            }
            default -> isNegative = false;
        }

        int radix;
        var isFloat = false;
        if (token.charAt(start) == '.') {
            radix = INVALID_RADIX;
            isFloat = true;
        } else if (token.charAt(start) == '0' && start + 1 < length) {
            var nextChar = token.charAt(start + 1);
            if (nextChar == 'x' || nextChar == 'X') {
                radix = HEXADECIMAL_RADIX;
                start += 2;
                if (start >= length) {
                    return new ProtobufRawToken(token);
                }
            } else if (nextChar >= '0' && nextChar <= '7') {
                var pointer = start + 1;
                while (pointer < length) {
                    if (token.charAt(pointer++) == '.') {
                        isFloat = true;
                        break;
                    }
                }
                if (pointer != length) {
                    radix = DECIMAL_RADIX;
                } else {
                    radix = OCTAL_RADIX;
                    start++;
                }
            } else if (nextChar == '.' || nextChar == 'e' || nextChar == 'E') {
                radix = INVALID_RADIX;
                isFloat = true;
            } else {
                return new ProtobufRawToken(token);
            }
        } else {
            radix = DECIMAL_RADIX;
        }

        if (isFloat) {
            return parseFloat(token);
        }

        var whole = 0L;
        char character;
        int digit;
        while (start < length) {
            character = token.charAt(start++);
            switch (radix) {
                case OCTAL_RADIX -> {
                    if ((whole & 0xE000000000000000L) != 0) {
                        return new ProtobufRawToken(token);
                    }
                    if (character >= '0' && character <= '7') {
                        digit = character - '0';
                    } else {
                        return new ProtobufRawToken(token);
                    }
                }
                case DECIMAL_RADIX -> {
                    if (character >= '0' && character <= '9') {
                        digit = character - '0';
                    } else if (character == '.' || character == 'e' || character == 'E') {
                        return parseFloat(token);
                    } else {
                        return new ProtobufRawToken(token);
                    }
                    var atLimit = Long.compareUnsigned(whole, DECIMAL_RADIX_LIMIT);
                    if (atLimit > 0 || (atLimit == 0 && digit > DECIMAL_RADIX_MAX_DIGIT)) {
                        return new ProtobufRawToken(token);
                    }
                }
                case HEXADECIMAL_RADIX -> {
                    if ((whole & 0xF000000000000000L) != 0) {
                        return new ProtobufRawToken(token);
                    }
                    if (character >= '0' && character <= '9') {
                        digit = character - '0';
                    } else if (character >= 'a' && character <= 'f') {
                        digit = character - 'a' + 10;
                    } else if (character >= 'A' && character <= 'F') {
                        digit = character - 'A' + 10;
                    } else {
                        return new ProtobufRawToken(token);
                    }
                }
                default -> throw new InternalError("Invalid radix: " + radix);
            }
            whole = whole * radix + digit;
        }
        return new ProtobufNumberToken(new ProtobufInteger(isNegative ? -whole : whole));
    }

    private static ProtobufToken parseFloat(String token) {
        try {
            var value = Double.parseDouble(token);
            if (!Double.isFinite(value)) {
                return new ProtobufRawToken(token);
            }
            return new ProtobufNumberToken(new ProtobufFloatingPoint.Finite(value));
        } catch (NumberFormatException _) {
            return new ProtobufRawToken(token);
        }
    }

    /**
     * Returns the current line number in the input stream.
     *
     * @return the current line number
     */
    public int line() {
        return line;
    }

    private void snapshotPushback() {
        pushbackPos = pos;
        pushbackLine = line;
        pushbackColumn = column;
        pushbackTokenStartLine = tokenStartLine;
        pushbackTokenStartColumn = tokenStartColumn;
        pushbackTokenEndLine = tokenEndLine;
        pushbackTokenEndColumn = tokenEndColumn;
        pushbackPendingCommentMark = pendingDetached.size() + pendingLeading.size();
        pushbackHadTrailing = pendingTrailing != null;
        pushbackTrailing = pendingTrailing;
        pushbackLastConsumedTokenEndLine = lastConsumedTokenEndLine;
    }

    private boolean consumePushback() {
        if (!hasPushback) {
            return false;
        }
        hasPushback = false;
        pos = pushbackPos;
        line = pushbackLine;
        column = pushbackColumn;
        tokenStartLine = pushbackTokenStartLine;
        tokenStartColumn = pushbackTokenStartColumn;
        tokenEndLine = pushbackTokenEndLine;
        tokenEndColumn = pushbackTokenEndColumn;
        var detachedSize = pendingDetached.size();
        var leadingSize = pendingLeading.size();
        var markedTotal = pushbackPendingCommentMark;
        var totalNow = detachedSize + leadingSize;
        var toDrop = totalNow - markedTotal;
        while (toDrop > 0 && !pendingLeading.isEmpty()) {
            pendingLeading.removeLast();
            toDrop--;
        }
        while (toDrop > 0 && !pendingDetached.isEmpty()) {
            pendingDetached.removeLast();
            toDrop--;
        }
        pendingTrailing = pushbackHadTrailing ? pushbackTrailing : null;
        lastConsumedTokenEndLine = pushbackLastConsumedTokenEndLine;
        return true;
    }
}
