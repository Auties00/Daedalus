package com.github.auties00.daedalus.protobuf.io.reader;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextStreamReader;
import com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextTokenizedReader;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;

import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * An abstract reader for the Protocol Buffers text format, also known as textproto.
 *
 * <p>The text format identifies fields by their names instead of their numbers. Generated Spec
 * classes provide a {@code decode(ProtobufTextReader)} overload that reads a message through
 * this class, switching on the name of each field:
 *
 * <pre>{@code
 * reader.readStartMessage();
 * while (reader.nextField()) {
 *     switch (reader.fieldName()) {
 *         case "name" -> name = reader.readStringField();
 *         case "address" -> address = AddressSpec.global().decodeText(reader);
 *         default -> reader.skipUnknownField();
 *     }
 * }
 * reader.readEndMessage();
 * }</pre>
 *
 * <p>The whole grammar of the text format is supported:
 * <ul>
 * <li>the fields of a nested message are enclosed in braces or in angle brackets, and the
 *     colon between the name of the field and the message is optional;
 * <li>the name of an extension, or the type URL of an expanded {@code Any}, is enclosed in
 *     square brackets;
 * <li>the values of a repeated field can be written as a list enclosed in square brackets;
 * <li>fields can be separated by commas or semicolons;
 * <li>adjacent strings are concatenated, and strings can contain C escape sequences;
 * <li>integers can be written in decimal, hexadecimal or octal;
 * <li>floating point values can be written as {@code inf}, {@code infinity} or {@code nan},
 *     ignoring case, and can have an {@code f} suffix;
 * <li>booleans can be written as {@code true}, {@code True}, {@code t}, {@code 1},
 *     {@code false}, {@code False}, {@code f} or {@code 0};
 * <li>enum values can be written by the name of their constant or by their number;
 * <li>comments start with {@code #} and end at the end of the line.
 * </ul>
 *
 * <p>The top level message of the text is not enclosed in delimiters: its fields end at the end
 * of the text. The reader tracks the messages that are open, so that each one is closed by the
 * delimiter that matches the one that opened it, and fails if they are nested more deeply than
 * its {@linkplain #recursionLimit() recursion limit}.
 *
 * <p>This class is not thread safe.
 *
 * @see ProtobufBinaryReader
 */
public abstract non-sealed class ProtobufTextReader extends ProtobufReader {
    /**
     * The default maximum number of messages that can be nested inside each other, which is
     * the same as the default recursion limit of the official protobuf library.
     */
    public static final int DEFAULT_RECURSION_LIMIT = 100;

    /**
     * The delimiter that closes the top level message, which is the end of the text.
     */
    protected static final int END_OF_TEXT = -1;

    /**
     * The name of the last field read by {@link #nextField()}.
     */
    protected String fieldName;

    /**
     * The maximum number of messages that can be nested inside each other.
     */
    private int recursionLimit;

    /**
     * The delimiters that close the messages that are open, from the outermost to the
     * innermost: the closing brace, the closing angle bracket, or {@link #END_OF_TEXT}.
     */
    private int[] messageDelimiters;

    /**
     * The number of messages that are open.
     */
    private int messageDepth;

    /**
     * Constructs a new text reader with the default recursion limit.
     */
    protected ProtobufTextReader() {
        this.recursionLimit = DEFAULT_RECURSION_LIMIT;
        this.messageDelimiters = new int[8];
    }

    /**
     * Creates a text reader that reads the given string.
     *
     * @param sequence the string
     * @return a new text reader
     */
    public static ProtobufTextReader fromString(String sequence) {
        return new ProtobufTextTokenizedReader(sequence);
    }

    /**
     * Creates a text reader that reads the given character sequence.
     *
     * @param sequence the character sequence
     * @return a new text reader
     */
    public static ProtobufTextReader fromCharSequence(CharSequence sequence) {
        return new ProtobufTextTokenizedReader(sequence);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text held by the given byte array.
     *
     * @param bytes the byte array
     * @return a new text reader
     */
    public static ProtobufTextReader fromBytes(byte[] bytes) {
        return new ProtobufTextTokenizedReader(bytes);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text held by a range of the given byte
     * array.
     *
     * @param bytes  the byte array
     * @param offset the index of the first byte of the text
     * @param length the number of bytes of the text
     * @return a new text reader
     */
    public static ProtobufTextReader fromBytes(byte[] bytes, int offset, int length) {
        return new ProtobufTextTokenizedReader(bytes, offset, offset + length);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text held by the remaining bytes of the
     * given buffer.
     *
     * @param buffer the buffer
     * @return a new text reader
     */
    public static ProtobufTextReader fromBuffer(ByteBuffer buffer) {
        return new ProtobufTextTokenizedReader(buffer);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text of the given stream, closing it
     * when the reader is closed.
     *
     * @param stream the stream
     * @return a new text reader
     */
    public static ProtobufTextReader fromStream(InputStream stream) {
        return fromStream(stream, true);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text of the given stream.
     *
     * @param stream    the stream
     * @param autoclose whether the stream is closed when the reader is closed
     * @return a new text reader
     */
    public static ProtobufTextReader fromStream(InputStream stream, boolean autoclose) {
        return new ProtobufTextStreamReader(stream, -1, autoclose);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text of the given stream, through a
     * buffer of the given size.
     *
     * @param stream     the stream
     * @param autoclose  whether the stream is closed when the reader is closed
     * @param bufferSize the size of the buffer
     * @return a new text reader
     */
    public static ProtobufTextReader fromStream(InputStream stream, boolean autoclose, int bufferSize) {
        return new ProtobufTextStreamReader(stream, -1, autoclose, bufferSize);
    }

    /**
     * Creates a text reader that reads at most the given number of bytes of UTF-8 encoded text
     * from the given stream, closing it when the reader is closed.
     *
     * @param stream the stream
     * @param limit  the maximum number of bytes to read
     * @return a new text reader
     */
    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit) {
        return fromLimitedStream(stream, limit, true);
    }

    /**
     * Creates a text reader that reads at most the given number of bytes of UTF-8 encoded text
     * from the given stream.
     *
     * @param stream    the stream
     * @param limit     the maximum number of bytes to read
     * @param autoclose whether the stream is closed when the reader is closed
     * @return a new text reader
     */
    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit, boolean autoclose) {
        return new ProtobufTextStreamReader(stream, limit, autoclose);
    }

    /**
     * Creates a text reader that reads at most the given number of bytes of UTF-8 encoded text
     * from the given stream, through a buffer of the given size.
     *
     * @param stream     the stream
     * @param limit      the maximum number of bytes to read
     * @param autoclose  whether the stream is closed when the reader is closed
     * @param bufferSize the size of the buffer
     * @return a new text reader
     */
    public static ProtobufTextReader fromLimitedStream(InputStream stream, long limit, boolean autoclose, int bufferSize) {
        return new ProtobufTextStreamReader(stream, limit, autoclose, bufferSize);
    }

    /**
     * Creates a text reader that reads the UTF-8 encoded text held by the given memory segment.
     *
     * @param segment the memory segment
     * @return a new text reader
     */
    public static ProtobufTextReader fromMemorySegment(MemorySegment segment) {
        return new ProtobufTextTokenizedReader(segment);
    }

    /**
     * Returns the maximum number of messages that can be nested inside each other.
     *
     * @return the recursion limit, which is {@value #DEFAULT_RECURSION_LIMIT} unless changed
     */
    public int recursionLimit() {
        return recursionLimit;
    }

    /**
     * Sets the maximum number of messages that can be nested inside each other.
     *
     * <p>Like the recursion limit of the official protobuf library, it protects the decoder from
     * inputs that nest messages deeply enough to overflow the stack, so it should only be raised
     * for trusted inputs.
     *
     * @param recursionLimit the new recursion limit
     * @throws IllegalArgumentException if {@code recursionLimit} is negative
     */
    public void setRecursionLimit(int recursionLimit) {
        if (recursionLimit < 0) {
            throw new IllegalArgumentException("recursionLimit cannot be negative: " + recursionLimit);
        }
        this.recursionLimit = recursionLimit;
    }

    /**
     * Opens a message, whose fields are read next.
     *
     * <p>A nested message starts with an opening brace or an opening angle bracket, which is
     * consumed. If no message is open, and the text does not start with one of them, the
     * message is the top level message of the text, whose fields end at the end of the text.
     *
     * @throws ProtobufDeserializationException if a message is open and the next token does not
     *                                          open a message, or if the message exceeds the
     *                                          recursion limit
     * @see #readEndMessage()
     */
    public abstract void readStartMessage() throws ProtobufDeserializationException;

    /**
     * Reads the name of the next field of the message that is open, which is then available
     * through {@link #fieldName()}, and the colon that follows it, if any.
     *
     * <p>The name of an extension, or the type URL of an expanded {@code Any}, is returned with
     * its enclosing square brackets and without whitespace, like {@code [com.example.ext]}.
     *
     * @return {@code true} if a field was read, or {@code false} if the message has no more
     *         fields, as the next token closes it
     * @throws ProtobufDeserializationException if the next token is neither the name of a field
     *                                          nor the delimiter that closes the message
     */
    public abstract boolean nextField() throws ProtobufDeserializationException;

    /**
     * Returns the name of the last field read by {@link #nextField()}.
     *
     * @return the name of the field
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Closes the message opened by the last call to {@link #readStartMessage()}, consuming the
     * delimiter that closes it and the comma or the semicolon that follows it, if any.
     *
     * @throws ProtobufDeserializationException if the next token is not the delimiter that
     *                                          closes the message, or if the message is the top
     *                                          level message and the text does not end
     */
    public abstract void readEndMessage() throws ProtobufDeserializationException;

    /**
     * Opens the list of values of a repeated field, if the next token is an opening square
     * bracket.
     *
     * @return {@code true} if a list was opened, so its values are read through
     *         {@link #nextListElement()}, or {@code false} if the next token is a single value
     * @throws ProtobufDeserializationException if the text cannot be read
     */
    public abstract boolean readStartList() throws ProtobufDeserializationException;

    /**
     * Returns whether the list opened by {@link #readStartList()} has another value, consuming
     * the closing square bracket that ends the list, and the comma or the semicolon that
     * follows it, if it does not.
     *
     * @return {@code true} if a value follows, or {@code false} if the list ended
     * @throws ProtobufDeserializationException if the text ends before the list
     */
    public abstract boolean nextListElement() throws ProtobufDeserializationException;

    /**
     * Returns whether the next value is an identifier, like the name of an enum constant.
     *
     * @return {@code true} if the next value is an identifier, or {@code false} if it is a
     *         number, a string, a message, or a list
     * @throws ProtobufDeserializationException if the text cannot be read
     */
    public abstract boolean isNextValueIdentifier() throws ProtobufDeserializationException;

    /**
     * Reads a value that is an identifier, like the name of an enum constant.
     *
     * @return the identifier
     * @throws ProtobufDeserializationException if the next value is not an identifier
     */
    public abstract String readIdentifierField() throws ProtobufDeserializationException;

    /**
     * Returns whether the whole text was read.
     *
     * @return {@code true} if the text has no more tokens
     */
    public abstract boolean isFinished();

    /**
     * Skips the value of the last field read by {@link #nextField()}, whatever its type: a
     * scalar, a string, a message or a list.
     *
     * @throws ProtobufDeserializationException if the value is malformed
     */
    public abstract void skipUnknownField() throws ProtobufDeserializationException;

    /**
     * Reads the values of the last field read by {@link #nextField()}, whatever their type, and
     * adds them to the given unknown fields under the name of the field.
     *
     * <p>Every value of a list is added as a value of its own. A message is read as unknown
     * fields of its own, so that every field it holds is kept.
     *
     * @param fields the unknown fields that the values are added to
     * @throws ProtobufDeserializationException if a value is malformed
     * @throws NullPointerException if {@code fields} is {@code null}
     */
    public void readUnknownField(ProtobufUnknownFields fields) throws ProtobufDeserializationException {
        Objects.requireNonNull(fields, "fields cannot be null");
        var name = fieldName;
        if (readStartList()) {
            while (nextListElement()) {
                fields.add(name, readUnknownValue());
            }
        } else {
            fields.add(name, readUnknownValue());
        }
    }

    /**
     * Reads a single value, whatever its type.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the value is malformed
     */
    private ProtobufUnknownValue.Text readUnknownValue() throws ProtobufDeserializationException {
        if (isNextValueMessage()) {
            var fields = new ProtobufUnknownFields();
            readStartMessage();
            while (nextField()) {
                readUnknownField(fields);
            }
            readEndMessage();
            return new ProtobufUnknownValue.Text.Message(fields);
        }

        if (isNextValueString()) {
            return new ProtobufUnknownValue.Text.Bytes(readBytesField());
        }

        return new ProtobufUnknownValue.Text.Scalar(readScalarField());
    }

    /**
     * Returns whether the next value is a message, which starts with an opening brace or an
     * opening angle bracket.
     *
     * @return {@code true} if the next value is a message
     * @throws ProtobufDeserializationException if the text cannot be read
     */
    public abstract boolean isNextValueMessage() throws ProtobufDeserializationException;

    /**
     * Returns whether the next value is a quoted string.
     *
     * @return {@code true} if the next value is a string
     * @throws ProtobufDeserializationException if the text cannot be read
     */
    public abstract boolean isNextValueString() throws ProtobufDeserializationException;

    /**
     * Reads an unquoted scalar value exactly as it is written, including its sign, like
     * {@code 42}, {@code -0x1F}, {@code 1e-5}, {@code -inf}, {@code true} or {@code RED}, and the
     * separator that follows it, if any.
     *
     * @return the text of the scalar
     * @throws ProtobufDeserializationException if the next value is not an unquoted scalar
     */
    public abstract String readScalarField() throws ProtobufDeserializationException;

    /**
     * Reads a {@code float} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not a number
     */
    public abstract float readFloatField() throws ProtobufDeserializationException;

    /**
     * Reads a {@code double} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not a number
     */
    public abstract double readDoubleField() throws ProtobufDeserializationException;

    /**
     * Reads a {@code bool} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not a boolean
     */
    public abstract boolean readBoolField() throws ProtobufDeserializationException;

    /**
     * Reads an {@code int32} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          an {@code int32}
     */
    public abstract int readInt32Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code uint32} value.
     *
     * @return the value, interpreted as unsigned
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code uint32}
     */
    public abstract int readUInt32Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code sint32} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code sint32}
     */
    public abstract int readSInt32Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code fixed32} value.
     *
     * @return the value, interpreted as unsigned
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code fixed32}
     */
    public abstract int readFixed32Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code sfixed32} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code sfixed32}
     */
    public abstract int readSFixed32Field() throws ProtobufDeserializationException;

    /**
     * Reads an {@code int64} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          an {@code int64}
     */
    public abstract long readInt64Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code uint64} value.
     *
     * @return the value, interpreted as unsigned
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code uint64}
     */
    public abstract long readUInt64Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code sint64} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code sint64}
     */
    public abstract long readSInt64Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code fixed64} value.
     *
     * @return the value, interpreted as unsigned
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code fixed64}
     */
    public abstract long readFixed64Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code sfixed64} value.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not an integer that fits in
     *                                          a {@code sfixed64}
     */
    public abstract long readSFixed64Field() throws ProtobufDeserializationException;

    /**
     * Reads a {@code string} value, which is made of one or more adjacent quoted strings.
     *
     * <p>The escape sequences of the strings are decoded as bytes, which are then decoded as
     * UTF-8, like the official protobuf library does.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not a string
     */
    public abstract String readStringField() throws ProtobufDeserializationException;

    /**
     * Reads a {@code bytes} value, which is made of one or more adjacent quoted strings.
     *
     * @return the value
     * @throws ProtobufDeserializationException if the next value is not a string
     */
    public abstract byte[] readBytesField() throws ProtobufDeserializationException;

    /**
     * Records that a message was opened, which is closed by the given delimiter.
     *
     * @param delimiter the delimiter that closes the message: a closing brace, a closing angle
     *                  bracket, or {@link #END_OF_TEXT}
     * @throws ProtobufDeserializationException if the message exceeds the recursion limit
     */
    protected final void pushMessage(int delimiter) throws ProtobufDeserializationException {
        if (messageDepth >= recursionLimit) {
            throw ProtobufDeserializationException.recursionLimitExceeded(recursionLimit);
        }
        if (messageDepth == messageDelimiters.length) {
            messageDelimiters = Arrays.copyOf(messageDelimiters, messageDepth * 2);
        }
        messageDelimiters[messageDepth++] = delimiter;
    }

    /**
     * Records that the innermost message that is open was closed.
     *
     * @return the delimiter that closes the message
     * @throws ProtobufDeserializationException if no message is open
     */
    protected final int popMessage() throws ProtobufDeserializationException {
        if (messageDepth == 0) {
            throw ProtobufDeserializationException.unmatchedEndMessage();
        }
        return messageDelimiters[--messageDepth];
    }

    /**
     * Returns the delimiter that closes the innermost message that is open.
     *
     * @return the delimiter: a closing brace, a closing angle bracket, or {@link #END_OF_TEXT}
     *         if the message is the top level message or if no message is open
     */
    protected final int messageDelimiter() {
        return messageDepth == 0 ? END_OF_TEXT : messageDelimiters[messageDepth - 1];
    }

    /**
     * Returns the number of messages that are open.
     *
     * @return the number of messages that are open
     */
    protected final int messageDepth() {
        return messageDepth;
    }

    /**
     * Applies a sign to the magnitude of an integer, checking that the result fits in a signed
     * 32 bit integer.
     *
     * @param magnitude the magnitude, interpreted as unsigned
     * @param negative  whether the integer is preceded by a minus sign
     * @param type      the name of the type of the field, used in the error message
     * @return the integer
     * @throws ProtobufDeserializationException if the integer does not fit
     */
    protected static int toSignedInt32(long magnitude, boolean negative, String type) throws ProtobufDeserializationException {
        var maxMagnitude = negative ? (long) Integer.MAX_VALUE + 1 : Integer.MAX_VALUE;
        if (Long.compareUnsigned(magnitude, maxMagnitude) > 0) {
            throw ProtobufDeserializationException.valueOutOfRange(type);
        }
        return (int) (negative ? -magnitude : magnitude);
    }

    /**
     * Checks that the magnitude of an integer fits in an unsigned 32 bit integer.
     *
     * @param magnitude the magnitude, interpreted as unsigned
     * @param type      the name of the type of the field, used in the error message
     * @return the integer, interpreted as unsigned
     * @throws ProtobufDeserializationException if the integer does not fit
     */
    protected static int toUnsignedInt32(long magnitude, String type) throws ProtobufDeserializationException {
        if ((magnitude & ~0xFFFFFFFFL) != 0) {
            throw ProtobufDeserializationException.valueOutOfRange(type);
        }
        return (int) magnitude;
    }

    /**
     * Applies a sign to the magnitude of an integer, checking that the result fits in a signed
     * 64 bit integer.
     *
     * @param magnitude the magnitude, interpreted as unsigned
     * @param negative  whether the integer is preceded by a minus sign
     * @param type      the name of the type of the field, used in the error message
     * @return the integer
     * @throws ProtobufDeserializationException if the integer does not fit
     */
    protected static long toSignedInt64(long magnitude, boolean negative, String type) throws ProtobufDeserializationException {
        if (negative) {
            if (Long.compareUnsigned(magnitude, Long.MIN_VALUE) > 0) {
                throw ProtobufDeserializationException.valueOutOfRange(type);
            }
            return -magnitude;
        } else {
            if (magnitude < 0) {
                throw ProtobufDeserializationException.valueOutOfRange(type);
            }
            return magnitude;
        }
    }

    /**
     * Parses the magnitude of an integer literal, which is decimal, hexadecimal if it starts
     * with {@code 0x} or {@code 0X}, or octal if it starts with {@code 0}.
     *
     * @param text the literal, without its sign
     * @param type the name of the type of the field, used in the error message
     * @return the magnitude of the literal, interpreted as unsigned
     * @throws ProtobufDeserializationException if the literal is not a valid integer, or if it
     *                                          does not fit in 64 bits
     */
    protected static long parseUnsignedMagnitude(String text, String type) throws ProtobufDeserializationException {
        var radix = 10;
        var start = 0;
        if (text.length() > 1 && text.charAt(0) == '0') {
            if ((text.charAt(1) | 0x20) == 'x') {
                radix = 16;
                start = 2;
                if (text.length() == 2) {
                    throw ProtobufDeserializationException.expectedHexDigits();
                }
            } else {
                radix = 8;
                start = 1;
            }
        }
        for (var i = start; i < text.length(); i++) {
            if (Character.digit(text.charAt(i), radix) < 0) {
                throw ProtobufDeserializationException.expectedValue(type, text);
            }
        }
        try {
            return Long.parseUnsignedLong(text, start, text.length(), radix);
        } catch (NumberFormatException _) {
            throw ProtobufDeserializationException.integerOverflow();
        }
    }

    /**
     * Parses the magnitude of a {@code double} literal.
     *
     * <p>The literal is decimal, and can have an {@code f} or {@code F} suffix, or is one of the
     * names of the infinite values and of not a number, which are {@code inf},
     * {@code infinity} and {@code nan}, ignoring case. Like the reference implementation of
     * Protocol Buffers, an integer literal, which can be hexadecimal or octal, is accepted too.
     *
     * @param text the literal, without its sign
     * @return the value of the literal
     * @throws ProtobufDeserializationException if the literal is not valid
     */
    protected static double parseDouble(String text) throws ProtobufDeserializationException {
        var special = parseSpecialFloatingPoint(text, "double");
        if (special != null) {
            return special;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException _) {
            throw ProtobufDeserializationException.invalidFloatingPoint("double", text);
        }
    }

    /**
     * Parses the magnitude of a {@code float} literal.
     *
     * <p>The literal has the same syntax as the one of {@link #parseDouble(String)}, and is
     * rounded to the nearest {@code float} directly, instead of being rounded to a
     * {@code double} first.
     *
     * @param text the literal, without its sign
     * @return the value of the literal
     * @throws ProtobufDeserializationException if the literal is not valid
     */
    protected static float parseFloat(String text) throws ProtobufDeserializationException {
        var special = parseSpecialFloatingPoint(text, "float");
        if (special != null) {
            return special.floatValue();
        }
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException _) {
            throw ProtobufDeserializationException.invalidFloatingPoint("float", text);
        }
    }

    /**
     * Parses a floating point literal that is not a decimal number, and validates one that is.
     *
     * @param text the literal, without its sign
     * @param type the name of the type of the field, used in the error message
     * @return the value of an infinite value, of not a number, or of an integer literal that is
     *         hexadecimal or octal, or {@code null} if the literal is a valid decimal number,
     *         which the caller parses
     * @throws ProtobufDeserializationException if the literal is not valid
     */
    private static Double parseSpecialFloatingPoint(String text, String type) throws ProtobufDeserializationException {
        if (text.equalsIgnoreCase("inf") || text.equalsIgnoreCase("infinity")) {
            return Double.POSITIVE_INFINITY;
        }
        if (text.equalsIgnoreCase("nan")) {
            return Double.NaN;
        }
        if (text.isEmpty() || (text.charAt(0) != '.' && (text.charAt(0) < '0' || text.charAt(0) > '9'))) {
            throw ProtobufDeserializationException.expectedValue(type, text);
        }
        if (text.length() > 1 && text.charAt(0) == '0' && (text.charAt(1) | 0x20) == 'x') {
            return parseHexOrOctalAsFloatingPoint(text, 2, 16, type);
        }
        if (text.length() > 1 && text.charAt(0) == '0' && text.charAt(1) >= '0' && text.charAt(1) <= '9') {
            if (isOctal(text)) {
                return parseHexOrOctalAsFloatingPoint(text, 1, 8, type);
            }
            throw ProtobufDeserializationException.octalFloatingPoint(type, text);
        }
        var last = text.charAt(text.length() - 1);
        if (last == 'd' || last == 'D') {
            throw ProtobufDeserializationException.invalidFloatingPoint(type, text);
        }
        return null;
    }

    /**
     * Returns whether the given literal, which starts with {@code 0}, only has octal digits.
     *
     * @param text the literal
     * @return {@code true} if every character is an octal digit
     */
    private static boolean isOctal(String text) {
        for (var i = 0; i < text.length(); i++) {
            var character = text.charAt(i);
            if (character < '0' || character > '7') {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a hexadecimal or octal integer literal used as the value of a floating point field,
     * which the official protobuf library accepts.
     *
     * @param text  the literal
     * @param start the index of the first digit
     * @param radix the radix of the digits
     * @param type  the name of the type of the field, used in the error message
     * @return the value of the literal
     * @throws ProtobufDeserializationException if the literal is not valid
     */
    private static double parseHexOrOctalAsFloatingPoint(String text, int start, int radix, String type) throws ProtobufDeserializationException {
        try {
            var value = Long.parseUnsignedLong(text, start, text.length(), radix);
            return value >= 0 ? value : (double) (value >>> 1) * 2 + (value & 1);
        } catch (NumberFormatException _) {
            throw ProtobufDeserializationException.invalidFloatingPoint(type, text);
        }
    }
}
