package com.github.auties00.daedalus.protobuf.io.reader.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextChars.*;
import static com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextLimits.*;

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
     * @param bufferSize  the internal buffer size in bytes
     */
    public ProtobufTextStreamReader(InputStream inputStream, long limit, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        this.inputStream = inputStream;
        this.autoclose = autoclose;
        this.limit = limit;
        this.buffer = new byte[bufferSize];
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
     * @throws ProtobufDeserializationException if the next token is not
     *         {@code &#123;} or {@code <}
     */
    @Override
    public void readStartObject() {
        var b = skipWhitespaceAndComments();
        if (b != LBRACE && b != LANGLE) {
            throw new ProtobufDeserializationException("Expected '{' or '<' to start object");
        }
        readByte();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not
     *         {@code &#125;} or {@code >}
     */
    @Override
    public void readEndObject() {
        var b = skipWhitespaceAndComments();
        if (b != RBRACE && b != RANGLE) {
            throw new ProtobufDeserializationException("Expected '}' or '>' to end object");
        }
        readByte();
        skipOptionalSeparator();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not {@code [}
     */
    @Override
    public void readStartArray() {
        var b = skipWhitespaceAndComments();
        if (b != LBRACKET) {
            throw new ProtobufDeserializationException("Expected '[' to start array");
        }
        readByte();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not {@code ]}
     */
    @Override
    public void readEndArray() {
        var b = skipWhitespaceAndComments();
        if (b != RBRACKET) {
            throw new ProtobufDeserializationException("Expected ']' to end array");
        }
        readByte();
        skipOptionalSeparator();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code false} if the stream is exhausted or the next token
     * is not an identifier (e.g. a closing bracket, a value token, or a
     * numeric literal). If an identifier is found, the optional colon
     * separator after the field name is also consumed.
     */
    @Override
    public boolean readPropertyName() {
        var b = skipWhitespaceAndComments();
        if (b == -1 || !isWordByte(b) || (b >= '0' && b <= '9') || b == DOT) {
            return false;
        }
        var builder = new StringBuilder();
        while (true) {
            if (bufferPosition >= bufferLimit) {
                if (finished || !refill()) {
                    return false;
                }
            }
            var codepoint = buffer[bufferPosition] & 0xFF;
            if(!isWordByte(codepoint)) {
                break;
            }
            builder.appendCodePoint(codepoint);
            bufferPosition++;
            position++;
        }
        propertyName = builder.toString();
        b = skipWhitespaceAndComments();
        if (b == COLON) {
            readByte();
        }
        return true;
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
    public void skipUnknownProperty() {
        var b = skipWhitespaceAndComments();
        if (b == -1) {
            return;
        }

        switch (b) {
            case LBRACE -> skipMatchingBlock(LBRACE, RBRACE);
            case LANGLE -> skipMatchingBlock(LANGLE, RANGLE);
            case LBRACKET -> skipMatchingBlock(LBRACKET, RBRACKET);
            case MINUS -> {
                readByte();
                skipScalarValue();
            }
            case DQUOTE, SQUOTE -> {
                skipAdjacentStrings();
                skipOptionalSeparator();
            }
            default -> {
                skipWord();
                skipOptionalSeparator();
            }
        }
    }

    /**
     * Skips a bracketed block including all nested content at arbitrary
     * depth. Correctly handles strings inside the block (quotes inside
     * strings do not affect bracket matching).
     *
     * @param open  the opening bracket character
     * @param close the closing bracket character
     */
    private void skipMatchingBlock(int open, int close) {
        readByte();
        var depth = 1;
        while (depth > 0) {
            var b = skipWhitespaceAndComments();
            if (b == -1) {
                throw new ProtobufDeserializationException("Unterminated block");
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
     * Skips a single scalar value (word, string, or nested block).
     */
    private void skipScalarValue() {
        var b = skipWhitespaceAndComments();
        if (b == DQUOTE || b == SQUOTE) {
            skipAdjacentStrings();
            skipOptionalSeparator();
        } else if (b == LBRACE) {
            skipMatchingBlock(LBRACE, RBRACE);
        } else if (b == LANGLE) {
            skipMatchingBlock(LANGLE, RANGLE);
        } else if (b == LBRACKET) {
            skipMatchingBlock(LBRACKET, RBRACKET);
        } else {
            skipWord();
            skipOptionalSeparator();
        }
    }

    /**
     * Skips one word
     */
    private void skipWord() {
        while (true) {
            if (bufferPosition >= bufferLimit && (finished || !refill())) {
                break;
            }
            var start = bufferPosition;
            while (bufferPosition < bufferLimit && isWordByte(buffer[bufferPosition] & 0xFF)) {
                bufferPosition++;
            }
            position += bufferPosition - start;
            if (bufferPosition < bufferLimit) {
                break;
            }
        }
    }

    /**
     * Skips one or more adjacent quoted strings (textproto concatenation).
     */
    private void skipAdjacentStrings() {
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
    private void skipQuotedString(int quoteChar) {
        readByte();
        while (true) {
            var b = readByte();
            if (b == -1) {
                throw new ProtobufDeserializationException("Unterminated string");
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
    private long streamMagnitude() {
        var b = peekByte();
        if (b < '0' || b > '9') {
            throw new ProtobufDeserializationException("Expected numeric value");
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
    private long streamDecimal() {
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
                throw new ProtobufDeserializationException("Integer overflow");
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
    private long streamHex() {
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
                throw new ProtobufDeserializationException("Integer overflow");
            }
            value = (value << 4) | digit;
        }
        if (!hasDigits) {
            throw new ProtobufDeserializationException("Expected hex digits after 0x prefix");
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
    private long streamOctal() {
        var value = 0L;
        while (true) {
            var b = peekByte();
            if (b < '0' || b > '7') {
                break;
            }
            readByte();
            if ((value >>> 61) != 0) {
                throw new ProtobufDeserializationException("Integer overflow");
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
    public float readFloatProperty() {
        var negative = streamSign();
        var b = peekByte() | 0x20;
        float value;
        if (b == 'i') {
            readByte();
            if ((readByte() | 0x20) != 'n') {
                throw new ProtobufDeserializationException("Expected float value");
            }
            if ((readByte() | 0x20) != 'f') {
                throw new ProtobufDeserializationException("Expected float value");
            }
            if (isWordByte(peekByte())) {
                if ((readByte() | 0x20) != 'i') {
                    throw new ProtobufDeserializationException("Expected float value");
                }
                if ((readByte() | 0x20) != 'n') {
                    throw new ProtobufDeserializationException("Expected float value");
                }
                if ((readByte() | 0x20) != 'i') {
                    throw new ProtobufDeserializationException("Expected float value");
                }
                if ((readByte() | 0x20) != 't') {
                    throw new ProtobufDeserializationException("Expected float value");
                }
                if ((readByte() | 0x20) != 'y') {
                    throw new ProtobufDeserializationException("Expected float value");
                }
                if (isWordByte(peekByte())) {
                    throw new ProtobufDeserializationException("Expected float value");
                }
            }
            value = Float.POSITIVE_INFINITY;
        } else if (b == 'n') {
            readByte();
            if ((readByte() | 0x20) != 'a') {
                throw new ProtobufDeserializationException("Expected float value");
            }
            if ((readByte() | 0x20) != 'n') {
                throw new ProtobufDeserializationException("Expected float value");
            }
            if (isWordByte(peekByte())) {
                throw new ProtobufDeserializationException("Expected float value");
            }
            value = Float.NaN;
        } else {
            value = streamFloat();
        }
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * Parses a float from the stream
     *
     * @return the parsed float
     */
    private float streamFloat() {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDoubleProperty() {
        var negative = streamSign();
        var b = peekByte();
        double value;
        if (b == 'i' || b == 'I') {
            readByte();
            if ((readByte() | 0x20) != 'n') {
                throw new ProtobufDeserializationException("Expected double value");
            }
            if ((readByte() | 0x20) != 'f') {
                throw new ProtobufDeserializationException("Expected double value");
            }
            if (isWordByte(peekByte())) {
                if ((readByte() | 0x20) != 'i') {
                    throw new ProtobufDeserializationException("Expected double value");
                }
                if ((readByte() | 0x20) != 'n') {
                    throw new ProtobufDeserializationException("Expected double value");
                }
                if ((readByte() | 0x20) != 'i') {
                    throw new ProtobufDeserializationException("Expected double value");
                }
                if ((readByte() | 0x20) != 't') {
                    throw new ProtobufDeserializationException("Expected double value");
                }
                if ((readByte() | 0x20) != 'y') {
                    throw new ProtobufDeserializationException("Expected double value");
                }
                if (isWordByte(peekByte())) {
                    throw new ProtobufDeserializationException("Expected double value");
                }
            }
            value = Double.POSITIVE_INFINITY;
        } else if (b == 'n' || b == 'N') {
            readByte();
            if ((readByte() | 0x20) != 'a') {
                throw new ProtobufDeserializationException("Expected double value");
            }
            if ((readByte() | 0x20) != 'n') {
                throw new ProtobufDeserializationException("Expected double value");
            }
            if (isWordByte(peekByte())) {
                throw new ProtobufDeserializationException("Expected double value");
            }
            value = Double.NaN;
        } else {
            value = streamDouble();
        }
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * Parses a double from the stream
     *
     * @return the parsed double
     */
    private double streamDouble() {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt32Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt32(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSInt32Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt32(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSFixed32Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt32(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUInt32Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        var value = checkUInt32(magnitude);
        return negative ? -value : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readFixed32Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        var value = checkUInt32(magnitude);
        return negative ? -value : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readInt64Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt64(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSInt64Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt64(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSFixed64Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return applySignAndCheckInt64(magnitude, negative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readUInt64Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return negative ? -magnitude : magnitude;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readFixed64Property() {
        var negative = streamSign();
        var magnitude = streamMagnitude();
        skipOptionalSeparator();
        return negative ? -magnitude : magnitude;
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
    public boolean readBoolProperty() {
        var b = skipWhitespaceAndComments();
        boolean result;
        if (b == 't' || b == 'T') {
            // Match "t", "true", "True", or "TRUE" (case-insensitive after first byte)
            readByte();
            if (isWordByte(peekByte())) {
                if ((readByte() | 0x20) != 'r') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if ((readByte() | 0x20) != 'u') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if ((readByte() | 0x20) != 'e') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if (isWordByte(peekByte())) {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
            }
            result = true;
        } else if (b == 'f' || b == 'F') {
            // Match "f", "false", "False", or "FALSE" (case-insensitive after first byte)
            readByte();
            if (isWordByte(peekByte())) {
                if ((readByte() | 0x20) != 'a') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if ((readByte() | 0x20) != 'l') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if ((readByte() | 0x20) != 's') {
                        throw new ProtobufDeserializationException("Expected boolean value");
                }
                if ((readByte() | 0x20) != 'e') {
                    throw new ProtobufDeserializationException("Expected boolean value");
                }
                if (isWordByte(peekByte())) {
                    throw new ProtobufDeserializationException("Expected boolean value");
                }
            }
            result = false;
        } else if (b >= '0' && b <= '9') {
            result = streamMagnitude() != 0;
        } else {
            throw new ProtobufDeserializationException("Expected boolean value");
        }
        skipOptionalSeparator();
        return result;
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
    public String readStringProperty() {
        return new String(readBytesProperty(), StandardCharsets.UTF_8);
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
    public byte[] readBytesProperty() {
        var quote = skipWhitespaceAndComments();
        if (quote != DQUOTE && quote != SQUOTE) {
            throw new ProtobufDeserializationException("Expected string value for bytes field");
        }

        // Save some allocations by rolling our own linked list
        final class Node {
            final byte[] value;
            Node next;

            Node(byte[] value) {
                this.value = value;
            }
        }
        Node head = null, tail = null;
        var count = 0;
        var totalLength = 0;

        int b;
        do {
            readByte();

            var endOfString = false;
            while (!endOfString) {
                if (bufferPosition >= bufferLimit && (finished || !refill())) {
                    throw new ProtobufDeserializationException("Unterminated string");
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
                    // \UHHHHHHHH is the longest escape (10 source bytes)
                    if (bufferLimit - scanPos < 10) {
                        break;
                    }
                    hasEscapes = true;
                    var esc = buffer[scanPos + 1] & 0xFF;
                    scanPos += 2;
                    switch (esc) {
                        case 'a', 'b', 'f', 'n', 'r', 't', 'v', '\\', '\'', '"', '?' -> decodedLen++;
                        case 'x', 'X' -> {
                            decodedLen++;
                            if (hexDigit(buffer[scanPos] & 0xFF) >= 0) {
                                scanPos++;
                                if (hexDigit(buffer[scanPos] & 0xFF) >= 0) {
                                    scanPos++;
                                }
                            }
                        }
                        case 'u' -> {
                            var cp = 0;
                            for (var d = 0; d < 4; d++) {
                                var digit = hexDigit(buffer[scanPos] & 0xFF);
                                if (digit < 0) break;
                                cp = (cp << 4) | digit;
                                scanPos++;
                            }
                            decodedLen += utf8EncodedLength(cp);
                        }
                        case 'U' -> {
                            var cp = 0;
                            for (var d = 0; d < 8; d++) {
                                var digit = hexDigit(buffer[scanPos] & 0xFF);
                                if (digit < 0) break;
                                cp = (cp << 4) | digit;
                                scanPos++;
                            }
                            decodedLen += utf8EncodedLength(cp);
                        }
                        default -> {
                            if (esc >= '0' && esc <= '7') {
                                decodedLen++;
                                if ((buffer[scanPos] & 0xFF) >= '0' && (buffer[scanPos] & 0xFF) <= '7') {
                                    scanPos++;
                                    if ((buffer[scanPos] & 0xFF) >= '0' && (buffer[scanPos] & 0xFF) <= '7') {
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
                        throw new ProtobufDeserializationException("Unterminated string");
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
            while (head != tail) {
                System.arraycopy(head.value, 0, result, offset, head.value.length);
                offset += head.value.length;
                head = head.next;
            }
            return result;
        }
    }

    /**
     * Decodes escape sequences from a source byte range into a pre-allocated
     * destination array. The destination must be sized to exactly the decoded
     * length as computed by the first-pass scan in {@link #readBytesProperty()}.
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
}
