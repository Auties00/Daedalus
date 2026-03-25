package com.github.auties00.daedalus.protobuf.io.reader.text.streamed;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.reader.text.tokenized.ProtobufTextTokenizedReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

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
public final class ProtobufTextStreamedReader extends ProtobufTextReader {
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
    public ProtobufTextStreamedReader(InputStream inputStream, long limit, boolean autoclose) {
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
    public ProtobufTextStreamedReader(InputStream inputStream, long limit, boolean autoclose, int bufferSize) {
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
     * Skips whitespace and {@code #}-style line comments, returning the
     * first meaningful byte without consuming it. Returns {@code -1} at EOF.
     *
     * @return the first non-whitespace, non-comment byte, or {@code -1}
     */
    private int skipWhitespaceAndComments() {
        while (true) {
            var b = peekByte();
            if (b == -1) return -1;
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                readByte();
                continue;
            }
            if (b == '#') {
                readByte();
                while (true) {
                    var c = readByte();
                    if (c == -1 || c == '\n') break;
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
                && b != ' ' && b != '\t' && b != '\r' && b != '\n'
                && b != '{' && b != '}' && b != '<' && b != '>'
                && b != '[' && b != ']' && b != ':' && b != ','
                && b != ';' && b != '-' && b != '#'
                && b != '"' && b != '\'';
    }

    /**
     * Reads a word (identifier or numeric literal) from the current
     * position. Consumes all consecutive content bytes.
     *
     * @return the word text
     */
    private String readWord() {
        var sb = new StringBuilder();
        while (true) {
            var b = peekByte();
            if (!isWordByte(b)) break;
            sb.append((char) readByte());
        }
        return sb.toString();
    }

    /**
     * Consumes an optional separator ({@code ,} or {@code ;}) if one
     * appears after skipping whitespace and comments.
     */
    private void skipOptionalSeparator() {
        var b = skipWhitespaceAndComments();
        if (b == ',' || b == ';') {
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
        if (b != '{' && b != '<') {
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
        if (b != '}' && b != '>') {
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
        if (b != '[') {
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
        if (b != ']') {
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
        if (b == -1 || !isWordByte(b) || (b >= '0' && b <= '9') || b == '.') {
            return false;
        }
        propertyName = readWord();
        b = skipWhitespaceAndComments();
        if (b == ':') {
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
        if (b == -1) return;

        switch (b) {
            case '{' -> skipMatchingBlock('{', '}');
            case '<' -> skipMatchingBlock('<', '>');
            case '[' -> skipMatchingBlock('[', ']');
            case '-' -> {
                readByte();
                skipScalarValue();
            }
            case '"', '\'' -> {
                skipAdjacentStrings();
                skipOptionalSeparator();
            }
            default -> {
                readWord();
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
            if (b == -1) break;
            if (b == open) {
                depth++;
                readByte();
            } else if (b == close) {
                depth--;
                readByte();
            } else if (b == '"' || b == '\'') {
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
        if (b == '"' || b == '\'') {
            skipAdjacentStrings();
            skipOptionalSeparator();
        } else if (b == '{') {
            skipMatchingBlock('{', '}');
        } else if (b == '<') {
            skipMatchingBlock('<', '>');
        } else if (b == '[') {
            skipMatchingBlock('[', ']');
        } else {
            readWord();
            skipOptionalSeparator();
        }
    }

    /**
     * Skips one or more adjacent quoted strings (textproto concatenation).
     */
    private void skipAdjacentStrings() {
        var b = peekByte();
        while (b == '"' || b == '\'') {
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
            if (b == -1 || b == quoteChar) break;
            if (b == '\\') readByte();
        }
    }

    /**
     * Reads an integer value, optionally preceded by a minus sign.
     * Supports decimal, hexadecimal ({@code 0x} prefix), and octal
     * (leading zero) formats.
     *
     * @return the parsed integer value
     * @throws ProtobufDeserializationException if no numeric value is found
     */
    private long readRawInteger() {
        var b = skipWhitespaceAndComments();
        var negative = false;
        if (b == '-') {
            negative = true;
            readByte();
        }
        var text = readWord();
        if (text.isEmpty()) {
            throw new ProtobufDeserializationException("Expected numeric value");
        }
        var value = parseInteger(text);
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * Parses an integer string in decimal, hexadecimal, or octal format.
     *
     * @param text the integer text to parse
     * @return the parsed value
     */
    private static long parseInteger(String text) {
        if (text.length() > 2 && (text.startsWith("0x") || text.startsWith("0X"))) {
            return Long.parseUnsignedLong(text.substring(2), 16);
        } else if (text.length() > 1 && text.charAt(0) == '0' && text.charAt(1) >= '0' && text.charAt(1) <= '7') {
            return Long.parseUnsignedLong(text, 8);
        } else {
            return Long.parseLong(text);
        }
    }

    /**
     * Reads a floating-point value, handling special literals
     * ({@code inf}, {@code infinity}, {@code nan}) and numeric text.
     *
     * @return the parsed floating-point value
     * @throws ProtobufDeserializationException if no valid float is found
     */
    private double readRawFloat() {
        var b = skipWhitespaceAndComments();
        var negative = false;
        if (b == '-') {
            negative = true;
            readByte();
        }
        var text = readWord();
        if (text.isEmpty()) {
            throw new ProtobufDeserializationException("Expected numeric value or special float literal");
        }
        double value = switch (text) {
            case "inf", "Inf", "infinity", "Infinity" -> Double.POSITIVE_INFINITY;
            case "nan", "NaN" -> Double.NaN;
            default -> Double.parseDouble(text);
        };
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloatProperty() {
        return (float) readRawFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDoubleProperty() {
        return readRawFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readFixed32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSFixed32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readUInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readFixed64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSFixed64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts identifier literals ({@code true}, {@code false} and
     * common variants) as well as numeric values ({@code 0} for false,
     * non-zero for true).
     *
     * @throws ProtobufDeserializationException if the next token is not a
     *         valid boolean representation
     */
    @Override
    public boolean readBoolProperty() {
        skipWhitespaceAndComments();
        var text = readWord();
        var result = switch (text) {
            case "true", "True", "TRUE", "t" -> true;
            case "false", "False", "FALSE", "f" -> false;
            case "0" -> false;
            default -> {
                try {
                    yield Long.parseLong(text) != 0;
                } catch (NumberFormatException _) {
                    throw new ProtobufDeserializationException("Expected boolean value, got: " + text);
                }
            }
        };
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
        var b = skipWhitespaceAndComments();
        if (b != '"' && b != '\'') {
            throw new ProtobufDeserializationException("Expected string value");
        }
        var sb = new StringBuilder();
        readQuotedStringInto(sb);
        // Adjacent string concatenation
        while (true) {
            b = skipWhitespaceAndComments();
            if (b != '"' && b != '\'') break;
            readQuotedStringInto(sb);
        }
        skipOptionalSeparator();
        return sb.toString();
    }

    /**
     * Reads a single quoted string and appends the unescaped content
     * to the given {@link StringBuilder}. Consumes the opening and
     * closing quotes. Handles C-style escape sequences and multi-byte
     * UTF-8 sequences.
     *
     * @param sb the builder to append unescaped characters to
     */
    private void readQuotedStringInto(StringBuilder sb) {
        var quoteChar = readByte();
        while (true) {
            var b = readByte();
            if (b == -1 || b == quoteChar) break;
            if (b != '\\') {
                if (b < 0x80) {
                    sb.append((char) b);
                } else {
                    int codePoint;
                    int seqLen;
                    if ((b & 0xE0) == 0xC0) {
                        seqLen = 2;
                        codePoint = b & 0x1F;
                    } else if ((b & 0xF0) == 0xE0) {
                        seqLen = 3;
                        codePoint = b & 0x0F;
                    } else {
                        seqLen = 4;
                        codePoint = b & 0x07;
                    }
                    for (var j = 1; j < seqLen; j++) {
                        var cb = readByte();
                        if (cb == -1) break;
                        codePoint = (codePoint << 6) | (cb & 0x3F);
                    }
                    sb.appendCodePoint(codePoint);
                }
                continue;
            }
            unescapeCharInto(sb);
        }
    }

    /**
     * Reads a single escape sequence (the byte after the backslash has
     * not yet been consumed) and appends the decoded character to the
     * given {@link StringBuilder}.
     *
     * @param sb the builder to append the unescaped character to
     */
    private void unescapeCharInto(StringBuilder sb) {
        var esc = readByte();
        if (esc == -1) return;
        switch (esc) {
            case 'a' -> sb.append('\u0007');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'v' -> sb.append('\u000B');
            case '\\' -> sb.append('\\');
            case '\'' -> sb.append('\'');
            case '"' -> sb.append('"');
            case '?' -> sb.append('?');
            case 'x', 'X' -> {
                var hex = 0;
                for (var d = 0; d < 2; d++) {
                    var digit = hexDigit(peekByte());
                    if (digit < 0) break;
                    readByte();
                    hex = (hex << 4) | digit;
                }
                sb.append((char) hex);
            }
            case 'u' -> {
                var cp = 0;
                for (var d = 0; d < 4; d++) {
                    var digit = hexDigit(peekByte());
                    if (digit < 0) break;
                    readByte();
                    cp = (cp << 4) | digit;
                }
                sb.append((char) cp);
            }
            case 'U' -> {
                var cp = 0;
                for (var d = 0; d < 8; d++) {
                    var digit = hexDigit(peekByte());
                    if (digit < 0) break;
                    readByte();
                    cp = (cp << 4) | digit;
                }
                sb.appendCodePoint(cp);
            }
            default -> {
                if (esc >= '0' && esc <= '7') {
                    var oct = esc - '0';
                    for (var d = 1; d < 3; d++) {
                        var b = peekByte();
                        if (b < '0' || b > '7') break;
                        readByte();
                        oct = (oct << 3) | (b - '0');
                    }
                    sb.append((char) oct);
                } else {
                    sb.append('\\');
                    sb.append((char) esc);
                }
            }
        }
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
        var b = skipWhitespaceAndComments();
        if (b != '"' && b != '\'') {
            throw new ProtobufDeserializationException("Expected string value for bytes field");
        }
        var out = new ByteArrayOutputStream();
        readQuotedBytesInto(out);
        while (true) {
            b = skipWhitespaceAndComments();
            if (b != '"' && b != '\'') break;
            readQuotedBytesInto(out);
        }
        skipOptionalSeparator();
        return out.toByteArray();
    }

    /**
     * Reads a single quoted string and writes the unescaped raw bytes
     * to the given output stream. Used for protobuf {@code bytes} fields.
     *
     * @param out the output stream to write raw bytes to
     */
    private void readQuotedBytesInto(ByteArrayOutputStream out) {
        var quoteChar = readByte();
        while (true) {
            var b = readByte();
            if (b == -1 || b == quoteChar) break;
            if (b != '\\') {
                out.write(b);
                continue;
            }
            unescapeByteInto(out);
        }
    }

    /**
     * Reads a single byte escape sequence (the byte after the backslash
     * has not yet been consumed) and writes the decoded byte to the
     * given output stream.
     *
     * @param out the output stream to write the unescaped byte to
     */
    private void unescapeByteInto(ByteArrayOutputStream out) {
        var esc = readByte();
        if (esc == -1) return;
        switch (esc) {
            case 'a' -> out.write(0x07);
            case 'b' -> out.write(0x08);
            case 'f' -> out.write(0x0C);
            case 'n' -> out.write(0x0A);
            case 'r' -> out.write(0x0D);
            case 't' -> out.write(0x09);
            case 'v' -> out.write(0x0B);
            case '\\' -> out.write('\\');
            case '\'' -> out.write('\'');
            case '"' -> out.write('"');
            case '?' -> out.write('?');
            case 'x', 'X' -> {
                var hex = 0;
                for (var d = 0; d < 2; d++) {
                    var digit = hexDigit(peekByte());
                    if (digit < 0) break;
                    readByte();
                    hex = (hex << 4) | digit;
                }
                out.write(hex);
            }
            default -> {
                if (esc >= '0' && esc <= '7') {
                    var oct = esc - '0';
                    for (var d = 1; d < 3; d++) {
                        var b = peekByte();
                        if (b < '0' || b > '7') break;
                        readByte();
                        oct = (oct << 3) | (b - '0');
                    }
                    out.write(oct);
                } else {
                    out.write('\\');
                    out.write(esc);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not an identifier
     */
    @Override
    public String readEnumNameProperty() {
        skipWhitespaceAndComments();
        var text = readWord();
        if (text.isEmpty()) {
            throw new ProtobufDeserializationException("Expected enum name");
        }
        skipOptionalSeparator();
        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readEnumNumberProperty() {
        return readInt32Property();
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
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
