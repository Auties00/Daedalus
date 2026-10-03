package com.github.auties00.daedalus.protobuf.io.reader.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextChars.*;

/**
 * A streaming text reader for Protocol Buffers text format (textproto)
 * backed by an {@link InputStream}.
 *
 * <p>Unlike {@link ProtobufTextTokenizedReader ProtobufTextTokenizedReader},
 * which eagerly tokenizes the entire input using SIMD-accelerated lexing,
 * this reader processes bytes on demand through a refillable buffer.
 * This makes it suitable for large or unbounded inputs where loading everything
 * into memory is impractical.
 *
 * <p>The reader uses a simple byte-at-a-time lexer that skips whitespace
 * and comments inline, reading tokens directly into their target
 * representations (strings, numbers, identifiers) without an intermediate
 * token tape.
 */
public final class ProtobufTextStreamReader extends ProtobufTextReader {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The minimum size of the internal buffer, which must hold the longest escape sequence of a
     * string, {@code \UHHHHHHHH}, so that it can be decoded at once.
     */
    private static final int MIN_BUFFER_SIZE = 16;
    private static final long ULONG_MAX_DIV_10 = Long.divideUnsigned(-1L, 10);
    private static final int ULONG_MAX_MOD_10 = (int) Long.remainderUnsigned(-1L, 10);

    private final InputStream inputStream;
    private final boolean autoclose;
    private final long limit;
    private long position;
    private boolean finished;

    private final byte[] buffer;
    private int bufferPosition;
    private int bufferLimit;

    /**
     * Constructs a new streamed reader with the default buffer size.
     *
     * @param inputStream the input stream to read from
     * @param limit       the maximum number of bytes to read, or {@code -1} for unlimited
     * @param autoclose   whether to close the stream when {@link #close()} is called
     */
    public ProtobufTextStreamReader(InputStream inputStream, long limit, boolean autoclose) {
        this(inputStream, limit, autoclose, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a new streamed reader with the specified buffer size.
     *
     * @param inputStream the input stream to read from
     * @param limit       the maximum number of bytes to read, or {@code -1} for unlimited
     * @param autoclose   whether to close the stream when {@link #close()} is called
     * @param bufferSize  the internal buffer size in bytes, which is raised to
     *                    {@value #MIN_BUFFER_SIZE} if it is smaller
     */
    public ProtobufTextStreamReader(InputStream inputStream, long limit, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        this.inputStream = inputStream;
        this.autoclose = autoclose;
        this.limit = limit;
        this.buffer = new byte[Math.max(bufferSize, MIN_BUFFER_SIZE)];
    }

    /**
     * Returns the next byte without consuming it, or {@code -1} if the
     * stream is exhausted.
     *
     * @return the next byte (0-255), or {@code -1} at EOF
     */
    private int peekByte() {
        if (bufferPosition < bufferLimit) {
            return buffer[bufferPosition] & 0xFF;
        }
        if (finished || !refill()) {
            return -1;
        }
        return buffer[bufferPosition] & 0xFF;
    }

    /**
     * Consumes and returns the next byte, or {@code -1} if the stream
     * is exhausted.
     *
     * @return the consumed byte (0-255), or {@code -1} at EOF
     */
    private int readByte() {
        if (bufferPosition < bufferLimit) {
            position++;
            return buffer[bufferPosition++] & 0xFF;
        }
        if (finished || !refill()) {
            return -1;
        }
        position++;
        return buffer[bufferPosition++] & 0xFF;
    }

    /**
     * Refills the internal buffer from the input stream.
     *
     * @return {@code true} if at least one byte was read
     */
    private boolean refill() {
        try {
            var maxRead = buffer.length;
            if (limit >= 0) {
                var remaining = limit - position;
                if (remaining <= 0) {
                    finished = true;
                    return false;
                }
                maxRead = (int) Math.min(maxRead, remaining);
            }
            var read = inputStream.read(buffer, 0, maxRead);
            if (read <= 0) {
                finished = true;
                return false;
            }
            bufferPosition = 0;
            bufferLimit = read;
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Shifts the unprocessed bytes starting at {@code keepFrom} to the front
     * of the buffer, then fills the remaining space from the input stream.
     *
     * <p>This is used when an escape sequence straddles the buffer boundary:
     * the partial escape is preserved and new data is appended so that the
     * scan loop can resolve it without special-case code.
     *
     * @param keepFrom the start index of bytes to preserve
     * @return {@code true} if new data was read from the stream
     */
    private boolean compactAndRefill(int keepFrom) {
        var remaining = bufferLimit - keepFrom;
        System.arraycopy(buffer, keepFrom, buffer, 0, remaining);
        bufferPosition = 0;
        bufferLimit = remaining;
        try {
            var maxRead = buffer.length - remaining;
            if (limit >= 0) {
                var streamBudget = limit - position - remaining;
                if (streamBudget <= 0) {
                    return false;
                }
                maxRead = (int) Math.min(maxRead, streamBudget);
            }
            if (maxRead <= 0) {
                return false;
            }
            var read = inputStream.read(buffer, remaining, maxRead);
            if (read <= 0) {
                return false;
            }
            bufferLimit = remaining + read;
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Skips whitespace and {@code #}-style line comments, returning the
     * first meaningful byte without consuming it. Returns {@code -1} at EOF.
     *
     * @return the first non-whitespace, non-comment byte, or {@code -1}
     */
    private int skipWhitespaceAndComments() {
        while (true) {
            var b = peekByte();
            if (b == -1) {
                return -1;
            }
            if (b == SPACE || b == TAB || b == CR || b == LF) {
                readByte();
                continue;
            }
            if (b == HASH) {
                readByte();
                while (true) {
                    var c = readByte();
                    if (c == -1 || c == LF) {
                        break;
                    }
                }
                continue;
            }
            return b;
        }
    }

    /**
     * Returns {@code true} if the byte is a valid content byte (part of
     * an identifier or numeric literal). Content bytes are everything
     * except whitespace, structural characters, quotes, and comments.
     *
     * @param b the byte to test, or {@code -1} for EOF
     * @return {@code true} if the byte is a content byte
     */
    private static boolean isWordByte(int b) {
        return b != -1
                && b != SPACE && b != TAB && b != CR && b != LF
                && b != LBRACE && b != RBRACE && b != LANGLE && b != RANGLE
                && b != LBRACKET && b != RBRACKET && b != COLON && b != COMMA
                && b != SEMICOLON && b != MINUS && b != HASH
                && b != DQUOTE && b != SQUOTE;
    }

    /**
     * Consumes an optional separator ({@code ,} or {@code ;}) if one
     * appears after skipping whitespace and comments.
     */
    private void skipOptionalSeparator() {
        var b = skipWhitespaceAndComments();
        if (b == COMMA || b == SEMICOLON) {
            readByte();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if a message is open and the next token is
     *         neither {@code &#123;} nor {@code <}, or if the message exceeds the recursion limit
     */
    @Override
    public void readStartMessage() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        if (b == LBRACE || b == LANGLE) {
            readByte();
            pushMessage(b == LBRACE ? RBRACE : RANGLE);
            return;
        }

        if (messageDepth() != 0) {
            throw ProtobufDeserializationException.expectedMessageStart(describeByte(b));
        }

        pushMessage(END_OF_TEXT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextField() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        if (b == -1) {
            if (messageDelimiter() != END_OF_TEXT) {
                throw ProtobufDeserializationException.unexpectedEndOfText();
            }
            return false;
        }

        if (b == RBRACE || b == RANGLE) {
            var delimiter = messageDelimiter();
            if (delimiter == END_OF_TEXT) {
                throw ProtobufDeserializationException.expectedEndOfText(describeByte(b));
            }
            if (b != delimiter) {
                throw ProtobufDeserializationException.expectedMessageEnd(Character.toString(delimiter), describeByte(b));
            }
            return false;
        }

        if (b == LBRACKET) {
            readByte();
            fieldName = readBracketedName();
        } else if (isIdentifierStart(b)) {
            fieldName = readWordAsString();
        } else {
            throw ProtobufDeserializationException.expectedFieldName(describeByte(b));
        }

        if (skipWhitespaceAndComments() == COLON) {
            readByte();
        }
        return true;
    }

    /**
     * Reads the name of an extension, or the type URL of an expanded {@code Any}, whose opening
     * square bracket was consumed, and the closing square bracket that follows it.
     *
     * <p>The words of the name are concatenated, dropping the whitespace and the comments
     * between them.
     *
     * @return the name, enclosed in square brackets
     * @throws ProtobufDeserializationException if the name is empty, or if the text ends before
     *                                          the closing square bracket
     */
    private String readBracketedName() throws ProtobufDeserializationException {
        var result = new StringBuilder();
        result.append('[');
        while (true) {
            var b = skipWhitespaceAndComments();
            if (b == -1) {
                throw ProtobufDeserializationException.unexpectedEndOfText();
            } else if (b == RBRACKET) {
                readByte();
                break;
            } else if (b == MINUS) {
                readByte();
                result.append('-');
            } else if (isWordByte(b)) {
                result.append(readWordAsString());
            } else {
                throw ProtobufDeserializationException.expectedFieldName(describeByte(b));
            }
        }
        if (result.length() == 1) {
            throw ProtobufDeserializationException.expectedFieldName("]");
        }
        return result.append(']').toString();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not the delimiter that
     *         closes the message, or if the message is the top level message and the text does
     *         not end
     */
    @Override
    public void readEndMessage() throws ProtobufDeserializationException {
        var delimiter = popMessage();
        var b = skipWhitespaceAndComments();
        if (delimiter == END_OF_TEXT) {
            if (b != -1) {
                throw ProtobufDeserializationException.expectedEndOfText(describeByte(b));
            }
            return;
        }

        if (b == -1) {
            throw ProtobufDeserializationException.unexpectedEndOfText();
        }
        if (b != delimiter) {
            throw ProtobufDeserializationException.expectedMessageEnd(Character.toString(delimiter), describeByte(b));
        }
        readByte();
        skipOptionalSeparator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readStartList() {
        if (skipWhitespaceAndComments() == LBRACKET) {
            readByte();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nextListElement() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        if (b == -1) {
            throw ProtobufDeserializationException.unexpectedEndOfText();
        }
        if (b == RBRACKET) {
            readByte();
            skipOptionalSeparator();
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNextValueIdentifier() {
        return isIdentifierStart(skipWhitespaceAndComments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readIdentifierField() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        if (!isIdentifierStart(b)) {
            throw ProtobufDeserializationException.expectedValue("identifier", describeByte(b));
        }
        var result = readWordAsString();
        skipOptionalSeparator();
        return result;
    }

    /**
     * Returns whether the given byte starts an identifier, which is a word that does not start
     * with a digit or a dot.
     *
     * @param b the byte, or {@code -1} for the end of the text
     * @return {@code true} if the byte starts an identifier
     */
    private static boolean isIdentifierStart(int b) {
        return isWordByte(b) && (b < '0' || b > '9') && b != DOT;
    }

    /**
     * Describes the given byte in an error message.
     *
     * @param b the byte, or {@code -1} for the end of the text
     * @return the character of the byte, or an empty string for the end of the text
     */
    private static String describeByte(int b) {
        return b == -1 ? "" : Character.toString(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished() {
        return skipWhitespaceAndComments() == -1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles scalar values, signed numerics, adjacent string
     * concatenation, and nested objects or arrays at arbitrary depth.
     */
    @Override
    public void skipUnknownField() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        if (b == -1) {
            throw ProtobufDeserializationException.unexpectedEndOfText();
        }

        switch (b) {
            case LBRACE -> skipMatchingBlock(LBRACE, RBRACE);
            case LANGLE -> skipMatchingBlock(LANGLE, RANGLE);
            case LBRACKET -> skipMatchingBlock(LBRACKET, RBRACKET);
            case DQUOTE, SQUOTE -> {
                skipAdjacentStrings();
                skipOptionalSeparator();
            }
            default -> {
                streamSign();
                if (readNumberWord().isEmpty()) {
                    throw ProtobufDeserializationException.expectedValue("scalar", describeByte(b));
                }
                skipOptionalSeparator();
            }
        }
    }

    /**
     * Reads a word that is a number or an identifier, merging it with the negative exponent
     * that follows it, if any.
     *
     * <p>The minus sign is not a word byte, so a number like {@code 1e-5} is read as the word
     * {@code 1e}, which is followed by the minus sign and the digits of its exponent.
     *
     * @return the word
     */
    private String readNumberWord() {
        var word = readWordAsString();
        if (word.isEmpty() || peekByte() != MINUS) {
            return word;
        }

        var last = word.charAt(word.length() - 1);
        var hex = word.length() > 1 && word.charAt(0) == '0' && (word.charAt(1) | 0x20) == 'x';
        var digit = word.charAt(0) >= '0' && word.charAt(0) <= '9' || word.charAt(0) == '.';
        if ((last != 'e' && last != 'E') || hex || !digit) {
            return word;
        }

        readByte();
        return word + "-" + readWordAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNextValueMessage() {
        var b = skipWhitespaceAndComments();
        return b == LBRACE || b == LANGLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNextValueString() {
        var b = skipWhitespaceAndComments();
        return b == DQUOTE || b == SQUOTE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readScalarField() throws ProtobufDeserializationException {
        var negative = streamSign();
        var b = skipWhitespaceAndComments();
        var text = readNumberWord();
        if (text.isEmpty()) {
            throw ProtobufDeserializationException.expectedValue("scalar", describeByte(b));
        }
        skipOptionalSeparator();
        return negative ? "-" + text : text;
    }

    /**
     * Reads a contiguous unquoted word and returns it as a string.
     *
     * <p>The word is accumulated across buffer refills in a linked list of chunks.
     *
     * @return the word text
     */
    private String readWordAsString() {
        Node head = null, tail = null;
        var totalLength = 0;

        while (true) {
            if (bufferPosition >= bufferLimit && (finished || !refill())) {
                break;
            }

            var scanStart = bufferPosition;
            var scanPos = scanStart;
            while (scanPos < bufferLimit && isWordByte(buffer[scanPos] & 0xFF)) {
                scanPos++;
            }

            var chunkLen = scanPos - scanStart;
            if (chunkLen > 0) {
                var chunk = new byte[chunkLen];
                System.arraycopy(buffer, scanStart, chunk, 0, chunkLen);

                var current = new Node(chunk);
                if (head == null) {
                    head = current;
                    tail = head;
                } else {
                    tail.next = current;
                    tail = current;
                }
                totalLength += chunkLen;
            }

            position += chunkLen;
            bufferPosition = scanPos;

            if (bufferPosition < bufferLimit) {
                break;
            }
        }

        return Node.join(head, totalLength);
    }

    /**
     * Skips a bracketed block including all nested content at arbitrary
     * depth. Correctly handles strings inside the block (quotes inside
     * strings do not affect bracket matching).
     *
     * @param open  the opening bracket character
     * @param close the closing bracket character
     */
    private void skipMatchingBlock(int open, int close) throws ProtobufDeserializationException {
        readByte();
        var depth = 1;
        while (depth > 0) {
            var b = skipWhitespaceAndComments();
            if (b == -1) {
                throw ProtobufDeserializationException.unterminatedBlock();
            }
            if (b == open) {
                depth++;
                readByte();
            } else if (b == close) {
                depth--;
                readByte();
            } else if (b == DQUOTE || b == SQUOTE) {
                skipQuotedString(b);
            } else {
                readByte();
            }
        }
        skipOptionalSeparator();
    }

    /**
     * Skips one or more adjacent quoted strings (textproto concatenation).
     */
    private void skipAdjacentStrings() throws ProtobufDeserializationException {
        var b = peekByte();
        while (b == DQUOTE || b == SQUOTE) {
            skipQuotedString(b);
            b = skipWhitespaceAndComments();
        }
    }

    /**
     * Skips a single quoted string, handling backslash escapes.
     *
     * @param quoteChar the quote character ({@code "} or {@code '})
     */
    private void skipQuotedString(int quoteChar) throws ProtobufDeserializationException {
        readByte();
        while (true) {
            var b = readByte();
            if (b == -1) {
                throw ProtobufDeserializationException.unterminatedString();
            }
            if (b == quoteChar) {
                break;
            }
            if (b == BACKSLASH) {
                readByte();
            }
        }
    }

    /**
     * Parses an unsigned integer magnitude directly from the byte stream.
     * Dispatches to decimal, hexadecimal, or octal based on prefix.
     * No intermediate String is allocated.
     *
     * @return the parsed unsigned magnitude as a {@code long}
     * @throws ProtobufDeserializationException if no digits are found or
     *         the value exceeds 64 bits
     */
    private long streamMagnitude() throws ProtobufDeserializationException {
        var b = peekByte();
        if (b < '0' || b > '9') {
            throw ProtobufDeserializationException.expectedNumericValue();
        } else if (b == '0') {
            readByte();
            b = peekByte();
            if (b == 'x' || b == 'X') {
                readByte();
                return streamHex();
            } else if (b >= '0' && b <= '7') {
                return streamOctal();
            } else {
                return 0L;
            }
        } else {
            return streamDecimal();
        }
    }

    /**
     * Parses decimal digits from the stream into an unsigned {@code long}.
     * Throws on overflow beyond 2^64 - 1.
     *
     * @return the parsed decimal value
     * @throws ProtobufDeserializationException on overflow
     */
    private long streamDecimal() throws ProtobufDeserializationException {
        var value = 0L;
        while (true) {
            var b = peekByte();
            if (b < '0' || b > '9') {
                break;
            }
            readByte();
            var digit = b - '0';
            if (Long.compareUnsigned(value, ULONG_MAX_DIV_10) > 0
                    || (value == ULONG_MAX_DIV_10 && digit > ULONG_MAX_MOD_10)) {
                throw ProtobufDeserializationException.integerOverflow();
            }
            value = value * 10 + digit;
        }
        return value;
    }

    /**
     * Parses hexadecimal digits from the stream into an unsigned {@code long}.
     * The {@code 0x} prefix must have already been consumed. Throws on
     * overflow beyond 16 hex digits.
     *
     * @return the parsed hex value
     * @throws ProtobufDeserializationException on overflow
     */
    private long streamHex() throws ProtobufDeserializationException {
        var value = 0L;
        var hasDigits = false;
        while (true) {
            var digit = hexDigit(peekByte());
            if (digit < 0) {
                break;
            }
            readByte();
            hasDigits = true;
            if ((value >>> 60) != 0) {
                throw ProtobufDeserializationException.integerOverflow();
            }
            value = (value << 4) | digit;
        }
        if (!hasDigits) {
            throw ProtobufDeserializationException.expectedHexDigits();
        }
        return value;
    }

    /**
     * Parses octal digits from the stream into an unsigned {@code long}.
     * The leading {@code 0} must have already been consumed. Throws on
     * overflow beyond 2^64 - 1.
     *
     * @return the parsed octal value
     * @throws ProtobufDeserializationException on overflow
     */
    private long streamOctal() throws ProtobufDeserializationException {
        var value = 0L;
        while (true) {
            var b = peekByte();
            if (b < '0' || b > '7') {
                break;
            }
            readByte();
            if ((value >>> 61) != 0) {
                throw ProtobufDeserializationException.integerOverflow();
            }
            value = (value << 3) | (b - '0');
        }
        return value;
    }

    /**
     * Reads an optional minus sign from the stream.
     *
     * @return {@code true} if a minus was consumed
     */
    private boolean streamSign() {
        var b = skipWhitespaceAndComments();
        if (b == MINUS) {
            readByte();
            return true;
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloatField() throws ProtobufDeserializationException {
        var negative = streamSign();
        var value = parseFloat(readNumberWord());
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDoubleField() throws ProtobufDeserializationException {
        var negative = streamSign();
        var value = parseDouble(readNumberWord());
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt32Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt32(readMagnitude("int32"), negative, "int32");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSInt32Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt32(readMagnitude("sint32"), negative, "sint32");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSFixed32Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt32(readMagnitude("sfixed32"), negative, "sfixed32");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUInt32Field() throws ProtobufDeserializationException {
        skipWhitespaceAndComments();
        return toUnsignedInt32(readMagnitude("uint32"), "uint32");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readFixed32Field() throws ProtobufDeserializationException {
        skipWhitespaceAndComments();
        return toUnsignedInt32(readMagnitude("fixed32"), "fixed32");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readInt64Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt64(readMagnitude("int64"), negative, "int64");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSInt64Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt64(readMagnitude("sint64"), negative, "sint64");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSFixed64Field() throws ProtobufDeserializationException {
        var negative = streamSign();
        return toSignedInt64(readMagnitude("sfixed64"), negative, "sfixed64");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readUInt64Field() throws ProtobufDeserializationException {
        skipWhitespaceAndComments();
        return readMagnitude("uint64");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readFixed64Field() throws ProtobufDeserializationException {
        skipWhitespaceAndComments();
        return readMagnitude("fixed64");
    }

    /**
     * Reads the magnitude of an integer directly from the stream, checking that it is not
     * followed by other bytes of the same word, and the separator that follows it, if any.
     *
     * @param type the name of the type of the field, used in the error message
     * @return the magnitude, interpreted as unsigned
     * @throws ProtobufDeserializationException if the next word is not an integer, or if the
     *                                          integer does not fit in 64 bits
     */
    private long readMagnitude(String type) throws ProtobufDeserializationException {
        var magnitude = streamMagnitude();
        if (isWordByte(peekByte())) {
            throw ProtobufDeserializationException.expectedValue(type, readWordAsString());
        }
        skipOptionalSeparator();
        return magnitude;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Matches the first byte of the identifier to determine the boolean
     * value without allocating a String in the common case. {@code t}/{@code T}
     * prefix yields {@code true}, {@code f}/{@code F} yields {@code false},
     * and digit prefixes are parsed as integers (non-zero = {@code true}).
     *
     * @throws ProtobufDeserializationException if the next token is not a
     *         valid boolean representation
     */
    @Override
    public boolean readBoolField() throws ProtobufDeserializationException {
        var b = skipWhitespaceAndComments();
        boolean result;
        if (b == 't') {
            readByte();
            if (isWordByte(peekByte())) {
                expectByte('r');
                expectByte('u');
                expectByte('e');
                rejectTrailingWordBytes();
            }
            result = true;
        } else if (b == 'T') {
            readByte();
            if (isWordByte(peekByte())) {
                expectByte('r');
                expectByte('u');
                expectByte('e');
                rejectTrailingWordBytes();
            } else {
                throw ProtobufDeserializationException.expectedValue("boolean", null);
            }
            result = true;
        } else if (b == 'f') {
            readByte();
            if (isWordByte(peekByte())) {
                expectByte('a');
                expectByte('l');
                expectByte('s');
                expectByte('e');
                rejectTrailingWordBytes();
            }
            result = false;
        } else if (b == 'F') {
            readByte();
            if (isWordByte(peekByte())) {
                expectByte('a');
                expectByte('l');
                expectByte('s');
                expectByte('e');
                rejectTrailingWordBytes();
            } else {
                throw ProtobufDeserializationException.expectedValue("boolean", null);
            }
            result = false;
        } else if (b >= '0' && b <= '9') {
            var value = streamMagnitude();
            result = switch ((int) value) {
                case 0 -> false;
                case 1 -> true;
                default -> throw ProtobufDeserializationException.expectedBooleanNumber(null);
            };
        } else {
            throw ProtobufDeserializationException.expectedValue("boolean", null);
        }
        skipOptionalSeparator();
        return result;
    }

    private void expectByte(int expected) throws ProtobufDeserializationException {
        if (readByte() != expected) {
            throw ProtobufDeserializationException.expectedValue("boolean", null);
        }
    }

    private void rejectTrailingWordBytes() throws ProtobufDeserializationException {
        if (isWordByte(peekByte())) {
            throw ProtobufDeserializationException.expectedValue("boolean", null);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles C-style escape sequences and adjacent string concatenation
     * (consecutive string tokens are merged into one value).
     *
     * @throws ProtobufDeserializationException if the next token is not a string
     */
    @Override
    public String readStringField() throws ProtobufDeserializationException {
        return new String(readBytesField(), StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads one or more adjacent string tokens and decodes their
     * content as raw bytes with C-style escape sequences (octal, hex).
     *
     * @throws ProtobufDeserializationException if the next token is not a string
     */
    @Override
    public byte[] readBytesField() throws ProtobufDeserializationException {
        var quote = skipWhitespaceAndComments();
        if (quote != DQUOTE && quote != SQUOTE) {
            throw ProtobufDeserializationException.expectedValue("bytes", null);
        }

        Node head = null, tail = null;
        var count = 0;
        var totalLength = 0;

        var b = quote;
        do {
            // Adjacent strings can use different quotes
            quote = b;
            readByte();

            var endOfString = false;
            var inputExhausted = false;
            while (!endOfString) {
                if (bufferPosition >= bufferLimit && (finished || !refill())) {
                    throw ProtobufDeserializationException.unterminatedString();
                }

                // First pass: scan buffer for closing quote, count decoded length
                var scanStart = bufferPosition;
                var scanPos = scanStart;
                var decodedLen = 0;
                var hasEscapes = false;

                while (scanPos < bufferLimit) {
                    var c = buffer[scanPos] & 0xFF;
                    if (c == quote) {
                        endOfString = true;
                        break;
                    }
                    if (c != BACKSLASH) {
                        scanPos++;
                        decodedLen++;
                        continue;
                    }
                    // \UHHHHHHHH is the longest escape (10 source bytes): refill unless the input has no more bytes
                    if (bufferLimit - scanPos < 10 && !inputExhausted) {
                        break;
                    }
                    if (scanPos + 1 >= bufferLimit) {
                        throw ProtobufDeserializationException.unterminatedString();
                    }
                    hasEscapes = true;
                    var esc = buffer[scanPos + 1] & 0xFF;
                    scanPos += 2;
                    switch (esc) {
                        case 'a', 'b', 'f', 'n', 'r', 't', 'v', '\\', '\'', '"', '?' -> decodedLen++;
                        case 'x', 'X' -> {
                            decodedLen++;
                            if (hexDigit(peekBuffer(scanPos)) >= 0) {
                                scanPos++;
                                if (hexDigit(peekBuffer(scanPos)) >= 0) {
                                    scanPos++;
                                }
                            }
                        }
                        case 'u' -> {
                            var cp = 0;
                            for (var d = 0; d < 4; d++) {
                                var digit = hexDigit(peekBuffer(scanPos));
                                if (digit < 0) break;
                                cp = (cp << 4) | digit;
                                scanPos++;
                            }
                            decodedLen += utf8EncodedLength(cp);
                        }
                        case 'U' -> {
                            var cp = 0;
                            for (var d = 0; d < 8; d++) {
                                var digit = hexDigit(peekBuffer(scanPos));
                                if (digit < 0) break;
                                cp = (cp << 4) | digit;
                                scanPos++;
                            }
                            decodedLen += utf8EncodedLength(cp);
                        }
                        default -> {
                            if (esc >= '0' && esc <= '7') {
                                decodedLen++;
                                if (peekBuffer(scanPos) >= '0' && peekBuffer(scanPos) <= '7') {
                                    scanPos++;
                                    if (peekBuffer(scanPos) >= '0' && peekBuffer(scanPos) <= '7') {
                                        scanPos++;
                                    }
                                }
                            } else {
                                decodedLen += 2;
                            }
                        }
                    }
                }

                // Second pass: allocate exact-size chunk and decode
                var sourceLen = scanPos - scanStart;
                if (sourceLen > 0) {
                    var chunk = new byte[decodedLen];
                    if (!hasEscapes) {
                        System.arraycopy(buffer, scanStart, chunk, 0, decodedLen);
                    } else {
                        decodeEscapedBytes(buffer, scanStart, scanPos, chunk);
                    }

                    var current = new Node(chunk);
                    if(head == null) {
                        head = current;
                        tail = head;
                    } else {
                        tail.next = current;
                        tail = current;
                    }
                    count++;
                    totalLength += decodedLen;
                }

                // Advance buffer past the scanned region
                position += sourceLen;
                bufferPosition = scanPos;

                if (endOfString) {
                    bufferPosition++;
                    position++;
                } else if (scanPos < bufferLimit) {
                    // Escape straddles buffer boundary: compact remaining
                    // bytes to front and refill so the scan can resolve it
                    if (!compactAndRefill(scanPos)) {
                        inputExhausted = true;
                    }
                }
            }

            b = skipWhitespaceAndComments();
        } while (b == DQUOTE || b == SQUOTE);

        if (b == COMMA || b == SEMICOLON) {
            readByte();
        }

        if (count == 1) {
            return head.value;
        } else {
            var result = new byte[totalLength];
            var offset = 0;
            while (head != null) {
                System.arraycopy(head.value, 0, result, offset, head.value.length);
                offset += head.value.length;
                head = head.next;
            }
            return result;
        }
    }

    /**
     * Returns the byte of the buffer at the given index, if it holds one.
     *
     * @param index the index
     * @return the byte (0-255), or {@code -1} if the index is not before the limit of the buffer
     */
    private int peekBuffer(int index) {
        return index < bufferLimit ? buffer[index] & 0xFF : -1;
    }

    /**
     * Decodes escape sequences from a source byte range into a pre-allocated
     * destination array. The destination must be sized to exactly the decoded
     * length as computed by the first-pass scan in {@link #readBytesField()}.
     *
     * @param src   the source buffer
     * @param start the start offset in the source (inclusive)
     * @param end   the end offset in the source (exclusive)
     * @param dst   the destination array to decode into
     */
    private static void decodeEscapedBytes(byte[] src, int start, int end, byte[] dst) {
        var srcPos = start;
        var dstPos = 0;
        while (srcPos < end) {
            var b = src[srcPos] & 0xFF;
            if (b != '\\') {
                dst[dstPos++] = (byte) b;
                srcPos++;
                continue;
            }
            srcPos++;
            var esc = src[srcPos] & 0xFF;
            srcPos++;
            switch (esc) {
                case 'a' -> dst[dstPos++] = 0x07;
                case 'b' -> dst[dstPos++] = 0x08;
                case 'f' -> dst[dstPos++] = 0x0C;
                case 'n' -> dst[dstPos++] = 0x0A;
                case 'r' -> dst[dstPos++] = 0x0D;
                case 't' -> dst[dstPos++] = 0x09;
                case 'v' -> dst[dstPos++] = 0x0B;
                case '\\' -> dst[dstPos++] = (byte) '\\';
                case '\'' -> dst[dstPos++] = (byte) '\'';
                case '"' -> dst[dstPos++] = (byte) '"';
                case '?' -> dst[dstPos++] = (byte) '?';
                case 'x', 'X' -> {
                    var hex = 0;
                    if (srcPos < end) {
                        var d = hexDigit(src[srcPos] & 0xFF);
                        if (d >= 0) {
                            hex = d;
                            srcPos++;
                            if (srcPos < end) {
                                d = hexDigit(src[srcPos] & 0xFF);
                                if (d >= 0) {
                                    hex = (hex << 4) | d;
                                    srcPos++;
                                }
                            }
                        }
                    }
                    dst[dstPos++] = (byte) hex;
                }
                case 'u' -> {
                    var cp = 0;
                    for (var d = 0; d < 4 && srcPos < end; d++) {
                        var digit = hexDigit(src[srcPos] & 0xFF);
                        if (digit < 0) break;
                        cp = (cp << 4) | digit;
                        srcPos++;
                    }
                    dstPos = encodeUtf8(dst, dstPos, cp);
                }
                case 'U' -> {
                    var cp = 0;
                    for (var d = 0; d < 8 && srcPos < end; d++) {
                        var digit = hexDigit(src[srcPos] & 0xFF);
                        if (digit < 0) break;
                        cp = (cp << 4) | digit;
                        srcPos++;
                    }
                    dstPos = encodeUtf8(dst, dstPos, cp);
                }
                default -> {
                    if (esc >= '0' && esc <= '7') {
                        var oct = esc - '0';
                        if (srcPos < end) {
                            var od = src[srcPos] & 0xFF;
                            if (od >= '0' && od <= '7') {
                                oct = (oct << 3) | (od - '0');
                                srcPos++;
                                if (srcPos < end) {
                                    od = src[srcPos] & 0xFF;
                                    if (od >= '0' && od <= '7') {
                                        oct = (oct << 3) | (od - '0');
                                        srcPos++;
                                    }
                                }
                            }
                        }
                        dst[dstPos++] = (byte) oct;
                    } else {
                        dst[dstPos++] = (byte) '\\';
                        dst[dstPos++] = (byte) esc;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>If this reader was created with {@code autoclose=true}, the
     * underlying {@link InputStream} is closed.
     */
    @Override
    public void close() {
        if (autoclose) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Converts an ASCII hex digit character to its numeric value.
     *
     * @param c the ASCII character code
     * @return the hex value (0-15), or {@code -1} if not a valid hex digit
     */
    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /**
     * Returns the number of bytes needed to UTF-8 encode the given code point.
     *
     * @param codePoint the Unicode code point
     * @return the UTF-8 encoded length (1–4)
     */
    private static int utf8EncodedLength(int codePoint) {
        if (codePoint <= 0x7F) return 1;
        if (codePoint <= 0x7FF) return 2;
        if (codePoint <= 0xFFFF) return 3;
        return 4;
    }

    /**
     * Writes the UTF-8 encoding of a Unicode code point into the destination
     * array at the given position.
     *
     * @param dst       the destination byte array
     * @param pos       the write position
     * @param codePoint the Unicode code point to encode
     * @return the position after the last written byte
     */
    private static int encodeUtf8(byte[] dst, int pos, int codePoint) {
        if (codePoint <= 0x7F) {
            dst[pos++] = (byte) codePoint;
        } else if (codePoint <= 0x7FF) {
            dst[pos++] = (byte) (0xC0 | (codePoint >> 6));
            dst[pos++] = (byte) (0x80 | (codePoint & 0x3F));
        } else if (codePoint <= 0xFFFF) {
            dst[pos++] = (byte) (0xE0 | (codePoint >> 12));
            dst[pos++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            dst[pos++] = (byte) (0x80 | (codePoint & 0x3F));
        } else {
            dst[pos++] = (byte) (0xF0 | (codePoint >> 18));
            dst[pos++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
            dst[pos++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
            dst[pos++] = (byte) (0x80 | (codePoint & 0x3F));
        }
        return pos;
    }

    private final static class Node {
        final byte[] value;
        Node next;

        /**
         * Merges a linked list of byte array nodes into a single string.
         *
         * @param head the first node in the chain
         * @param totalLength the total byte length across all nodes
         * @return the merged string
         */
        private static String join(Node head, int totalLength) {
            if(head == null) {
                return "";
            } else if(head.value.length == totalLength) {
                return new String(head.value, StandardCharsets.UTF_8);
            } else {
                var result = new byte[totalLength];
                var offset = 0;
                while (head != null) {
                    System.arraycopy(head.value, 0, result, offset, head.value.length);
                    offset += head.value.length;
                    head = head.next;
                }
                return new String(result, StandardCharsets.UTF_8);
            }
        }

        Node(byte[] value) {
            this.value = value;
        }
    }
}
