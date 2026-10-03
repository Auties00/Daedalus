package com.github.auties00.daedalus.protobuf.io.writer;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeCalculator;
import com.github.auties00.daedalus.protobuf.io.writer.text.*;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * An abstract writer for the Protocol Buffers text format, also known as textproto.
 *
 * <p>Text writers produce the human readable representation of Protocol Buffers messages,
 * which identifies fields by their names instead of their numbers. Generated Spec classes
 * provide an {@code encode(T, ProtobufTextWriter)} overload that writes each field that is set
 * through this class. The output is the same as the one of the {@code TextFormat} printer of
 * the official protobuf library:
 *
 * <pre>{@code
 * name: "Alice"
 * id: 7
 * address {
 *   city: "Rome"
 * }
 * }</pre>
 *
 * <p>Every field is written on its own line, and the fields of a nested message are indented by
 * two spaces for each level of nesting. A writer {@linkplain #setSingleLine(boolean) set to
 * write a single line} separates the fields with a single space instead:
 *
 * <pre>{@code
 * name: "Alice" id: 7 address { city: "Rome" }
 * }</pre>
 *
 * <p>Like the official protobuf library, the bytes of a {@code string} field that are not
 * printable ASCII characters are written as octal escape sequences of their UTF-8 encoding,
 * unless the writer is {@linkplain #setEscapeNonAscii(boolean) set to write them as they are}.
 * The value of a {@code bytes} field is always escaped.
 *
 * <p>Concrete subclasses only implement the methods that append characters to their output,
 * like a byte array, a buffer, a stream or a {@link StringBuilder}: this class formats every
 * value.
 *
 * <p>This class is not thread safe.
 *
 * @param <OUTPUT> the type of the output produced by this writer
 * @see ProtobufBinaryWriter
 */
public abstract non-sealed class ProtobufTextWriter<OUTPUT> extends ProtobufWriter<OUTPUT> {
    /**
     * The characters used to write the hexadecimal digits of the unknown fixed width fields.
     */
    private static final byte[] HEX_DIGITS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    /**
     * The tens digit of every number from {@code 0} to {@code 99}, indexed by the number, so
     * that a number is formatted two digits at a time.
     */
    private static final byte[] TENS_DIGITS = createDigits(true);

    /**
     * The ones digit of every number from {@code 0} to {@code 99}, indexed by the number.
     */
    private static final byte[] ONES_DIGITS = createDigits(false);

    /**
     * The spaces that indent a line, which are written at once.
     */
    private static final byte[] SPACES = " ".repeat(64).getBytes(StandardCharsets.US_ASCII);

    /**
     * The size of every character below {@code 0x80} in the value of a {@code string} field,
     * indexed by the character: {@code 1} for a character that is written as it is, and more
     * for one that is escaped.
     */
    private static final byte[] ESCAPED_ASCII_SIZES = createEscapedAsciiSizes();

    /**
     * The characters of the formatted numbers, which are formatted into this array instead of a
     * new string. It holds the longest formatted number, which is a {@code double}.
     */
    private final byte[] digits;

    /**
     * The builder used to format floating point values, which is created the first time one is
     * written.
     */
    private StringBuilder decimal;

    /**
     * Whether every field is written on the same line, separated by a single space.
     */
    private boolean singleLine;

    /**
     * Whether the characters of a {@code string} field that are not ASCII are written as octal
     * escape sequences of their UTF-8 encoding.
     */
    private boolean escapeNonAscii;

    /**
     * The number of messages that enclose the next field.
     */
    private int depth;

    /**
     * Whether the next field starts a new line, which is preceded by the indentation of its
     * depth, or by a single space if the writer writes a single line.
     */
    private boolean atStartOfLine;

    /**
     * Constructs a new text writer that writes a field on each line and escapes the characters
     * of {@code string} fields that are not ASCII.
     */
    protected ProtobufTextWriter() {
        this.escapeNonAscii = true;
        this.digits = new byte[32];
    }

    /**
     * Constructs a new text writer with the given settings, which writes its first field at the
     * given depth.
     *
     * @param singleLine whether every field is written on the same line
     * @param escapeNonAscii whether the characters of {@code string} fields that are not ASCII
     *                       are escaped
     * @param depth the number of messages that enclose the first field
     * @param atStartOfLine whether the first field starts a new line, which is preceded by the
     *                      indentation of its depth, or by a single space if the writer writes
     *                      a single line
     */
    protected ProtobufTextWriter(boolean singleLine, boolean escapeNonAscii, int depth, boolean atStartOfLine) {
        this.singleLine = singleLine;
        this.escapeNonAscii = escapeNonAscii;
        this.depth = depth;
        this.atStartOfLine = atStartOfLine;
        this.digits = new byte[32];
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into a new byte array of
     * the given length.
     *
     * @param length the length of the byte array
     * @return a new text writer
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static ProtobufTextWriter<byte[]> toBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteArrayWriter(new byte[length], 0, length);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into the given byte
     * array, starting at the given offset.
     *
     * @param bytes  the byte array
     * @param offset the offset of the first byte to write
     * @return a new text writer
     * @throws NullPointerException      if {@code bytes} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} is not an index of {@code bytes}
     */
    public static ProtobufTextWriter<byte[]> toBytes(byte[] bytes, int offset) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        Objects.checkIndex(offset, bytes.length);
        return new ProtobufTextByteArrayWriter(bytes, offset, bytes.length);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into a new heap buffer
     * of the given capacity.
     *
     * @param length the capacity of the buffer
     * @return a new text writer
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static ProtobufTextWriter<ByteBuffer> toHeapBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteBufferWriter(ByteBuffer.allocate(length));
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into a new direct buffer
     * of the given capacity.
     *
     * @param length the capacity of the buffer
     * @return a new text writer
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static ProtobufTextWriter<ByteBuffer> toDirectBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufTextByteBufferWriter(ByteBuffer.allocateDirect(length));
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into the given buffer,
     * starting at its position.
     *
     * @param buffer the buffer
     * @return a new text writer
     * @throws NullPointerException     if {@code buffer} is {@code null}
     * @throws IllegalArgumentException if {@code buffer} is read only
     */
    public static ProtobufTextWriter<ByteBuffer> toBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer is read-only");
        }
        return new ProtobufTextByteBufferWriter(buffer);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text to the given stream,
     * closing it when the writer is closed.
     *
     * @param stream the stream
     * @return a new text writer
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, true);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text to the given stream.
     *
     * @param stream    the stream
     * @param autoclose whether the stream is closed when the writer is closed
     * @return a new text writer
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream, boolean autoclose) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, autoclose);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text to the given stream,
     * through a buffer of the given size.
     *
     * @param stream     the stream
     * @param autoclose  whether the stream is closed when the writer is closed
     * @param bufferSize the size of the buffer
     * @return a new text writer
     * @throws NullPointerException     if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public static ProtobufTextWriter<OutputStream> toStream(OutputStream stream, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        return new ProtobufTextStreamWriter(stream, autoclose, bufferSize);
    }

    /**
     * Creates a text writer that writes the UTF-8 encoding of the text into the given memory
     * segment.
     *
     * @param segment the memory segment
     * @return a new text writer
     * @throws NullPointerException if {@code segment} is {@code null}
     */
    public static ProtobufTextWriter<MemorySegment> toMemorySegment(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        return new ProtobufTextMemorySegmentWriter(segment);
    }

    /**
     * Creates a text writer that writes the text into a new character sequence.
     *
     * @param initialCapacity the initial capacity of the character sequence
     * @return a new text writer
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public static ProtobufTextWriter<? extends CharSequence> toCharSequence(int initialCapacity) {
        return toString(initialCapacity);
    }

    /**
     * Creates a text writer that writes the text into a new string.
     *
     * @param initialCapacity the initial capacity of the string
     * @return a new text writer
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public static ProtobufTextWriter<String> toString(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must not be negative");
        }
        return new ProtobufTextCharSequenceWriter(initialCapacity);
    }

    /**
     * Creates a text writer that writes the text into the given character buffer, starting at
     * its position.
     *
     * @param buffer the character buffer
     * @return a new text writer
     * @throws NullPointerException     if {@code buffer} is {@code null}
     * @throws IllegalArgumentException if {@code buffer} is read only
     */
    public static ProtobufTextWriter<CharBuffer> toCharBuffer(CharBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer is read-only");
        }
        return new ProtobufTextCharBufferWriter(buffer);
    }

    /**
     * Creates a text writer that writes nothing, but counts the bytes of the UTF-8 encoding of
     * the text it would write with the given settings, which its {@link #toOutput()} method
     * returns.
     *
     * <p>It computes the size of the text written by code that only writes it with a text
     * writer, like a converter, as part of the text written by another writer, so it writes its
     * first field at the given depth.
     *
     * @param singleLine whether every field is written on the same line
     * @param escapeNonAscii whether the characters of {@code string} fields that are not ASCII
     *                       are escaped
     * @param depth the number of messages that enclose the first field
     * @param atStartOfLine whether the first field starts a new line, as it follows other
     *                      fields
     * @return a new counting writer
     */
    public static ProtobufTextWriter<Long> toSize(boolean singleLine, boolean escapeNonAscii, int depth, boolean atStartOfLine) {
        return new ProtobufTextSizeWriter(singleLine, escapeNonAscii, depth, atStartOfLine);
    }

    /**
     * Returns whether this writer writes every field on the same line.
     *
     * @return {@code true} if the fields are separated by a single space, or {@code false} if
     *         every field is written on its own line, which is the default
     */
    public boolean isSingleLine() {
        return singleLine;
    }

    /**
     * Sets whether this writer writes every field on the same line, separated by a single
     * space, like the {@code shortDebugString} of the official protobuf library.
     *
     * @param singleLine {@code true} to write a single line, or {@code false} to write every
     *                   field on its own line
     * @return this writer
     */
    public ProtobufTextWriter<OUTPUT> setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
        return this;
    }

    /**
     * Returns whether this writer escapes the characters of {@code string} fields that are not
     * ASCII.
     *
     * @return {@code true} if they are written as octal escape sequences of their UTF-8
     *         encoding, which is the default, or {@code false} if they are written as they are
     */
    public boolean isEscapeNonAscii() {
        return escapeNonAscii;
    }

    /**
     * Sets whether this writer escapes the characters of {@code string} fields that are not
     * ASCII.
     *
     * <p>Like the official protobuf library, they are escaped by default, so that the output is
     * ASCII. Writing them as they are produces a more readable output, which is encoded as UTF-8
     * by writers that produce bytes. The values of {@code bytes} fields are always escaped.
     *
     * @param escapeNonAscii {@code true} to write the characters as octal escape sequences of
     *                       their UTF-8 encoding, or {@code false} to write them as they are
     * @return this writer
     */
    public ProtobufTextWriter<OUTPUT> setEscapeNonAscii(boolean escapeNonAscii) {
        this.escapeNonAscii = escapeNonAscii;
        return this;
    }

    /**
     * Writes the name of a field whose value is a message, or a group, and opens the message,
     * whose fields are written next.
     *
     * <p>For example, the name {@code address} is written as {@code address}, followed by a
     * space and an opening brace.
     *
     * @param fieldName the name of the field
     * @throws ProtobufSerializationException if the output cannot be written
     * @see #writeEndMessageField()
     */
    public void writeStartMessageField(String fieldName) throws ProtobufSerializationException {
        writeStartMessageField(fieldName, " {");
    }

    /**
     * Writes the name of a field whose value is a message, the given separator, which ends with
     * the opening brace of the message, and opens the message.
     *
     * @param fieldName the name of the field
     * @param separator the separator between the name and the fields of the message
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeStartMessageField(String fieldName, String separator) throws ProtobufSerializationException {
        writeFieldName(fieldName, separator);
        writeEndOfLine();
        depth++;
    }

    /**
     * Closes the message opened by the last call to {@link #writeStartMessageField(String)}
     * that was not closed yet.
     *
     * @throws ProtobufSerializationException if the output cannot be written, or if no message
     *                                        is open
     */
    public void writeEndMessageField() throws ProtobufSerializationException {
        if (depth == 0) {
            throw ProtobufSerializationException.unmatchedEndMessage();
        }
        depth--;
        writeLineStart();
        writeAscii('}');
        writeEndOfLine();
    }

    /**
     * Writes a {@code float} field.
     *
     * <p>The value is written like {@link Float#toString(float)} does, so infinite values are
     * written as {@code Infinity} and {@code -Infinity}, and not a number as {@code NaN}.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeFloatField(String fieldName, float value) throws ProtobufSerializationException {
        var builder = decimal();
        builder.append(value);
        writeDecimalField(fieldName, builder);
    }

    /**
     * Writes a {@code double} field.
     *
     * <p>The value is written like {@link Double#toString(double)} does, so infinite values are
     * written as {@code Infinity} and {@code -Infinity}, and not a number as {@code NaN}.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeDoubleField(String fieldName, double value) throws ProtobufSerializationException {
        var builder = decimal();
        builder.append(value);
        writeDecimalField(fieldName, builder);
    }

    /**
     * Writes a {@code bool} field as {@code true} or {@code false}.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeBoolField(String fieldName, boolean value) throws ProtobufSerializationException {
        writeScalarField(fieldName, value ? "true" : "false");
    }

    /**
     * Writes an {@code int32} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeInt32Field(String fieldName, int value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes a {@code uint32} field, whose value is interpreted as unsigned.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeUInt32Field(String fieldName, int value) throws ProtobufSerializationException {
        writeSignedField(fieldName, Integer.toUnsignedLong(value));
    }

    /**
     * Writes a {@code sint32} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeSInt32Field(String fieldName, int value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes a {@code fixed32} field, whose value is interpreted as unsigned.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeFixed32Field(String fieldName, int value) throws ProtobufSerializationException {
        writeSignedField(fieldName, Integer.toUnsignedLong(value));
    }

    /**
     * Writes a {@code sfixed32} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeSFixed32Field(String fieldName, int value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes an {@code int64} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeInt64Field(String fieldName, long value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes a {@code uint64} field, whose value is interpreted as unsigned.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeUInt64Field(String fieldName, long value) throws ProtobufSerializationException {
        writeUnsignedField(fieldName, value);
    }

    /**
     * Writes a {@code sint64} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeSInt64Field(String fieldName, long value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes a {@code fixed64} field, whose value is interpreted as unsigned.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeFixed64Field(String fieldName, long value) throws ProtobufSerializationException {
        writeUnsignedField(fieldName, value);
    }

    /**
     * Writes a {@code sfixed64} field.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeSFixed64Field(String fieldName, long value) throws ProtobufSerializationException {
        writeSignedField(fieldName, value);
    }

    /**
     * Writes an enum field.
     *
     * <p>The value is written by the name of its constant, or by its number if the enum has no
     * constant for it, which can happen for open enums, like the official protobuf library does.
     *
     * @param fieldName    the name of the field
     * @param value        the number of the value of the field
     * @param constantName the name of the constant whose number is {@code value}, or
     *                     {@code null} if the enum has no such constant
     * @throws ProtobufSerializationException if the output cannot be written
     */
    public void writeEnumField(String fieldName, int value, String constantName) throws ProtobufSerializationException {
        if (constantName != null) {
            writeFieldName(fieldName, ": ");
            writeUnicode(constantName, 0, constantName.length());
            writeEndOfLine();
        } else {
            writeSignedField(fieldName, value);
        }
    }

    /**
     * Writes a {@code string} field as a double quoted string.
     *
     * <p>Double quotes, single quotes, backslashes and the characters that are not printable are
     * escaped. The characters that are not ASCII are escaped too, unless the writer is
     * {@linkplain #setEscapeNonAscii(boolean) set to write them as they are}.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     * @throws NullPointerException           if {@code value} is {@code null}
     */
    public void writeStringField(String fieldName, String value) throws ProtobufSerializationException {
        Objects.requireNonNull(value, "value cannot be null");
        writeFieldName(fieldName, ": \"");
        writeEscapedString(value);
        writeAscii('"');
        writeEndOfLine();
    }

    /**
     * Writes a {@code bytes} field as a double quoted string.
     *
     * <p>Every byte that is not a printable ASCII character, and the bytes of double quotes,
     * single quotes and backslashes, are escaped.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     * @throws NullPointerException           if {@code value} is {@code null}
     */
    public void writeBytesField(String fieldName, byte[] value) throws ProtobufSerializationException {
        Objects.requireNonNull(value, "value cannot be null");
        writeFieldName(fieldName, ": \"");
        for (var entry : value) {
            writeEscapedByte(entry & 0xFF);
        }
        writeAscii('"');
        writeEndOfLine();
    }

    /**
     * Writes a value of an unknown field, read from the binary format, by the number of the
     * field.
     *
     * <p>Like the official protobuf library, a variable length integer is written as an
     * unsigned decimal number, and a fixed width integer as a hexadecimal number padded to its
     * width. A length delimited value is written as a message if it can be decoded as one, and as
     * a string of bytes otherwise, and a group as a message. Like the official protobuf library,
     * a colon separates the number of a length delimited value from its message, and not the one
     * of a group. For example:
     *
     * <pre>{@code
     * 1: 150
     * 2: 0x0000002a
     * 3: {
     *   1: 1
     * }
     * 4: "\377abc"
     * 5 {
     *   1: 1
     * }
     * }</pre>
     *
     * <p>The text format cannot be decoded back into unknown fields, as fields are decoded by
     * their names: this method writes a binary field to inspect it, like the official protobuf
     * library does, and {@link ProtobufUnknownFields#encodeText(ProtobufTextWriter)} does not
     * use it.
     *
     * @param fieldIndex the number of the field
     * @param value      the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     * @throws NullPointerException           if {@code value} is {@code null}
     */
    public void writeUnknownField(int fieldIndex, ProtobufUnknownValue.Binary value) throws ProtobufSerializationException {
        Objects.requireNonNull(value, "value cannot be null");
        var fieldName = Integer.toString(fieldIndex);
        switch (value) {
            case ProtobufUnknownValue.Binary.VarInt varInt -> writeUnsignedField(fieldName, varInt.value());
            case ProtobufUnknownValue.Binary.Fixed32 fixed32 -> writeHexField(fieldName, fixed32.value() & 0xFFFFFFFFL, Integer.BYTES * 2);
            case ProtobufUnknownValue.Binary.Fixed64 fixed64 -> writeHexField(fieldName, fixed64.value(), Long.BYTES * 2);
            case ProtobufUnknownValue.Binary.Group group -> {
                writeStartMessageField(fieldName);
                writeBinaryUnknownFields(group.value());
                writeEndMessageField();
            }
            case ProtobufUnknownValue.Binary.LengthDelimited lengthDelimited -> {
                var message = decodeUnknownMessage(lengthDelimited);
                if (message != null) {
                    writeStartMessageField(fieldName, ": {");
                    writeBinaryUnknownFields(message);
                    writeEndMessageField();
                } else {
                    writeBytesField(fieldName, toByteArray(lengthDelimited));
                }
            }
        }
    }

    /**
     * Writes a value of an unknown field, read from the text format, by the name of the field.
     *
     * <p>A scalar is written exactly as it was read, a string of bytes as a quoted string whose
     * bytes are escaped like the ones of a {@code bytes} field, and a message as its text fields,
     * enclosed in braces.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     * @throws NullPointerException           if {@code value} is {@code null}
     */
    public void writeUnknownField(String fieldName, ProtobufUnknownValue.Text value) throws ProtobufSerializationException {
        Objects.requireNonNull(value, "value cannot be null");
        switch (value) {
            case ProtobufUnknownValue.Text.Scalar scalar -> {
                writeFieldName(fieldName, ": ");
                writeUnicode(scalar.value(), 0, scalar.value().length());
                writeEndOfLine();
            }
            case ProtobufUnknownValue.Text.Bytes bytes -> writeBytesField(fieldName, bytes.value());
            case ProtobufUnknownValue.Text.Message message -> {
                writeStartMessageField(fieldName);
                message.value().encodeText(this);
                writeEndMessageField();
            }
        }
    }

    /**
     * Writes every value of the binary fields of the given unknown fields, which are the fields
     * of a group or of a length delimited message, by their numbers.
     *
     * @param fields the unknown fields
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeBinaryUnknownFields(ProtobufUnknownFields fields) throws ProtobufSerializationException {
        for (var field : fields.binaryFields()) {
            for (var value : field) {
                writeUnknownField(field.index(), value);
            }
        }
    }

    /**
     * Writes the start of the line of a field, its name, and the separator between the name and
     * the given value, the value, and the end of the line.
     *
     * @param fieldName the name of the field
     * @param value     the formatted value of the field, which is ASCII
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeScalarField(String fieldName, String value) throws ProtobufSerializationException {
        writeFieldName(fieldName, ": ");
        writeAscii(value);
        writeEndOfLine();
    }

    /**
     * Writes a field whose value is the given signed integer, formatted in decimal without
     * creating a string.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeSignedField(String fieldName, long value) throws ProtobufSerializationException {
        writeFieldName(fieldName, ": ");
        writeSigned(value);
        writeEndOfLine();
    }

    /**
     * Writes a field whose value is the given unsigned integer, formatted in decimal without
     * creating a string.
     *
     * @param fieldName the name of the field
     * @param value     the value of the field, interpreted as unsigned
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeUnsignedField(String fieldName, long value) throws ProtobufSerializationException {
        writeFieldName(fieldName, ": ");
        writeUnsigned(value);
        writeEndOfLine();
    }

    /**
     * Appends the given signed value, formatted in decimal, to the output.
     *
     * @implSpec
     * The default implementation formats the value into an array owned by this writer, as
     * described by {@link #formatSigned(long, byte[], int)}, and appends it with
     * {@link #writeAscii(byte[], int, int)}.
     *
     * @param value the value
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected void writeSigned(long value) throws ProtobufSerializationException {
        var position = formatSigned(value, digits, digits.length);
        writeAscii(digits, position, digits.length - position);
    }

    /**
     * Appends the given unsigned value, formatted in decimal, to the output.
     *
     * @implSpec
     * The default implementation formats the value into an array owned by this writer, as
     * described by {@link #formatUnsigned(long, byte[], int)}, and appends it with
     * {@link #writeAscii(byte[], int, int)}.
     *
     * @param value the value, interpreted as unsigned
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected void writeUnsigned(long value) throws ProtobufSerializationException {
        var position = formatUnsigned(value, digits, digits.length);
        writeAscii(digits, position, digits.length - position);
    }

    /**
     * Formats the given unsigned value in decimal into the given array, so that its last digit
     * precedes the given index.
     *
     * <p>The array must have room for every digit, whose number is returned by
     * {@link ProtobufTextSizeCalculator#getUnsignedSize(long)}.
     *
     * @param value the value, interpreted as unsigned
     * @param destination the array
     * @param end the index after the last digit
     * @return the index of the first digit
     */
    protected static int formatUnsigned(long value, byte[] destination, int end) {
        if (value >= 0) {
            return formatSigned(value, destination, end);
        }

        // The quotient of the division by ten of an unsigned value that does not fit in a long is positive
        var quotient = (value >>> 1) / 5;
        destination[end - 1] = (byte) ('0' + (int) (value - quotient * 10));
        return formatSigned(quotient, destination, end - 1);
    }

    /**
     * Formats the given signed value in decimal into the given array, so that its last
     * character precedes the given index.
     *
     * <p>The array must have room for every character, whose number is returned by
     * {@link ProtobufTextSizeCalculator#getSignedSize(long)}. The digits are computed two at a
     * time, and with {@code int} arithmetic once the value fits in an {@code int}. They are
     * computed on the negative value, as the negative range is larger.
     *
     * @param value the value
     * @param destination the array
     * @param end the index after the last character
     * @return the index of the first character of the formatted value
     */
    protected static int formatSigned(long value, byte[] destination, int end) {
        var position = end;
        var negative = value < 0;
        var remaining = negative ? value : -value;
        while (remaining < Integer.MIN_VALUE) {
            var quotient = remaining / 100;
            var pair = (int) (quotient * 100 - remaining);
            remaining = quotient;
            destination[--position] = ONES_DIGITS[pair];
            destination[--position] = TENS_DIGITS[pair];
        }

        var small = (int) remaining;
        while (small <= -100) {
            var quotient = small / 100;
            var pair = quotient * 100 - small;
            small = quotient;
            destination[--position] = ONES_DIGITS[pair];
            destination[--position] = TENS_DIGITS[pair];
        }

        destination[--position] = ONES_DIGITS[-small];
        if (small <= -10) {
            destination[--position] = TENS_DIGITS[-small];
        }
        if (negative) {
            destination[--position] = '-';
        }
        return position;
    }

    /**
     * Writes a field whose value is the given unsigned value, formatted as a hexadecimal number
     * with the {@code 0x} prefix, padded with zeros to the given number of digits.
     *
     * @param fieldName the name of the field
     * @param value     the value
     * @param count     the number of digits
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeHexField(String fieldName, long value, int count) throws ProtobufSerializationException {
        writeFieldName(fieldName, ": ");
        digits[0] = '0';
        digits[1] = 'x';
        for (var i = count + 1; i >= 2; i--) {
            digits[i] = HEX_DIGITS[(int) (value & 0xF)];
            value >>>= 4;
        }
        writeAscii(digits, 0, count + 2);
        writeEndOfLine();
    }

    /**
     * Writes a field whose value is the floating point value formatted by the given builder.
     *
     * @param fieldName the name of the field
     * @param builder   the builder that holds the formatted value
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeDecimalField(String fieldName, StringBuilder builder) throws ProtobufSerializationException {
        var length = builder.length();
        for (var i = 0; i < length; i++) {
            digits[i] = (byte) builder.charAt(i);
        }
        writeFieldName(fieldName, ": ");
        writeAscii(digits, 0, length);
        writeEndOfLine();
    }

    /**
     * Returns the builder used to format floating point values, emptied, which formats them
     * like {@link Double#toString(double)} without creating a string.
     *
     * @return the builder
     */
    private StringBuilder decimal() {
        if (decimal == null) {
            decimal = new StringBuilder(digits.length);
        } else {
            decimal.setLength(0);
        }
        return decimal;
    }

    /**
     * Writes the start of the line of a field, its name, and the given separator.
     *
     * @param fieldName the name of the field
     * @param separator the separator that follows the name, which is ASCII
     * @throws ProtobufSerializationException if the output cannot be written
     * @throws NullPointerException           if {@code fieldName} is {@code null}
     */
    private void writeFieldName(String fieldName, String separator) throws ProtobufSerializationException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        writeFieldPrefix(takeLineStart(), fieldName, separator);
    }

    /**
     * Returns the number of spaces that precede the next field, which are the indentation of
     * the current depth, or a single space if the writer writes a single line, if the next
     * field starts a new line, and marks the line as started.
     *
     * @return the number of spaces, which is {@code 0} if the field does not start a new line
     */
    private int takeLineStart() {
        if (!atStartOfLine) {
            return 0;
        }

        atStartOfLine = false;
        return singleLine ? 1 : depth * 2;
    }

    /**
     * Writes the indentation of the current depth, or a single space if the writer writes a
     * single line, if the next field starts a new line.
     *
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeLineStart() throws ProtobufSerializationException {
        var spaces = takeLineStart();
        if (spaces != 0) {
            writeSpaces(spaces);
        }
    }

    /**
     * Ends the line of the field that was written, which is a new line, unless the writer writes
     * a single line.
     *
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeEndOfLine() throws ProtobufSerializationException {
        if (!singleLine) {
            writeAscii('\n');
        }
        atStartOfLine = true;
    }

    /**
     * Writes the content of a {@code string} field, escaping the characters that cannot be
     * written as they are.
     *
     * <p>The runs of characters that need no escaping are written at once.
     *
     * @param value the value of the field
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeEscapedString(String value) throws ProtobufSerializationException {
        var length = value.length();
        var i = 0;
        while (i < length) {
            var next = CharsetUtils.indexOfEscaped(value, i, length, ESCAPED_ASCII_SIZES);
            if (next != i) {
                writeAscii(value, i, next);
            }
            if (next == length) {
                return;
            }

            var character = value.charAt(next);
            if (character < 0x80) {
                writeEscapedByte(character);
                i = next + 1;
            } else if (!escapeNonAscii) {
                // The characters that are not ASCII are written at once, and a surrogate pair is never split, as it ends before an ASCII character
                i = next + 1;
                while (i < length && value.charAt(i) >= 0x80) {
                    i++;
                }
                writeUnicode(value, next, i);
            } else {
                var codePoint = value.codePointAt(next);
                var codePointLength = Character.charCount(codePoint);
                if (Character.isSurrogate(character) && codePointLength == 1) {
                    // A lone surrogate is encoded as a question mark, like String.getBytes does
                    writeAscii('?');
                } else {
                    writeEscapedUtf8(codePoint);
                }
                i = next + codePointLength;
            }
        }
    }

    /**
     * Writes the octal escape sequences of the UTF-8 encoding of the given code point, which is
     * not ASCII.
     *
     * @param codePoint the code point
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeEscapedUtf8(int codePoint) throws ProtobufSerializationException {
        if (codePoint < 0x800) {
            writeEscapedByte(0xC0 | (codePoint >>> 6));
            writeEscapedByte(0x80 | (codePoint & 0x3F));
        } else if (codePoint < 0x10000) {
            writeEscapedByte(0xE0 | (codePoint >>> 12));
            writeEscapedByte(0x80 | ((codePoint >>> 6) & 0x3F));
            writeEscapedByte(0x80 | (codePoint & 0x3F));
        } else {
            writeEscapedByte(0xF0 | (codePoint >>> 18));
            writeEscapedByte(0x80 | ((codePoint >>> 12) & 0x3F));
            writeEscapedByte(0x80 | ((codePoint >>> 6) & 0x3F));
            writeEscapedByte(0x80 | (codePoint & 0x3F));
        }
    }

    /**
     * Writes a byte of a quoted string, escaping it if it is not a printable ASCII character,
     * or if it is a double quote, a single quote or a backslash.
     *
     * <p>The escape sequences are the same as the ones of the official protobuf library: the
     * control characters that have a C escape sequence use it, and any other byte that is not
     * printable is written as three octal digits.
     *
     * @param value the unsigned value of the byte
     * @throws ProtobufSerializationException if the output cannot be written
     */
    private void writeEscapedByte(int value) throws ProtobufSerializationException {
        switch (value) {
            case 0x07 -> writeAscii("\\a");
            case '\b' -> writeAscii("\\b");
            case '\f' -> writeAscii("\\f");
            case '\n' -> writeAscii("\\n");
            case '\r' -> writeAscii("\\r");
            case '\t' -> writeAscii("\\t");
            case 0x0B -> writeAscii("\\v");
            case '\\' -> writeAscii("\\\\");
            case '\'' -> writeAscii("\\'");
            case '"' -> writeAscii("\\\"");
            default -> {
                if (value >= 0x20 && value < 0x7F) {
                    writeAscii((char) value);
                } else {
                    writeAscii('\\');
                    writeAscii((char) ('0' + ((value >>> 6) & 0x3)));
                    writeAscii((char) ('0' + ((value >>> 3) & 0x7)));
                    writeAscii((char) ('0' + (value & 0x7)));
                }
            }
        }
    }

    /**
     * Decodes the given length delimited value as a message, like the official protobuf library
     * does to write unknown fields.
     *
     * @param value the length delimited value
     * @return the fields of the message, or {@code null} if the value is not a message
     */
    private static ProtobufUnknownFields decodeUnknownMessage(ProtobufUnknownValue.Binary.LengthDelimited value) {
        var reader = switch (value) {
            case ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked bytes -> ProtobufBinaryReader.fromBytes(bytes.value());
            case ProtobufUnknownValue.Binary.LengthDelimited.ByteBufferBacked buffer -> ProtobufBinaryReader.fromBuffer(buffer.value().duplicate());
            case ProtobufUnknownValue.Binary.LengthDelimited.MemorySegmentBacked segment -> ProtobufBinaryReader.fromMemorySegment(segment.value().asSlice(0, segment.length()));
        };
        // The readers of values held in memory need no closing
        try {
            return ProtobufUnknownFields.decodeBinary(reader);
        } catch (ProtobufDeserializationException _) {
            return null;
        }
    }

    /**
     * Copies the bytes of the given length delimited value into a new array.
     *
     * @param value the length delimited value
     * @return the bytes of the value
     */
    private static byte[] toByteArray(ProtobufUnknownValue.Binary.LengthDelimited value) {
        return switch (value) {
            case ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked bytes -> bytes.value();
            case ProtobufUnknownValue.Binary.LengthDelimited.ByteBufferBacked buffer -> {
                var source = buffer.value();
                var result = new byte[source.remaining()];
                source.get(source.position(), result);
                yield result;
            }
            case ProtobufUnknownValue.Binary.LengthDelimited.MemorySegmentBacked segment -> segment.value()
                    .asSlice(0, segment.length())
                    .toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
        };
    }

    /**
     * Appends an ASCII character to the output.
     *
     * @param value the character, which is ASCII
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected abstract void writeAscii(char value) throws ProtobufSerializationException;

    /**
     * Appends a string of ASCII characters to the output.
     *
     * @param value the string, whose characters are ASCII
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected abstract void writeAscii(String value) throws ProtobufSerializationException;

    /**
     * Appends a range of an array of ASCII characters, held as one byte each, to the output.
     *
     * @param value  the array, whose bytes are ASCII characters
     * @param offset the index of the first character to append
     * @param length the number of characters to append
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected abstract void writeAscii(byte[] value, int offset, int length) throws ProtobufSerializationException;

    /**
     * Appends a range of a string, whose characters are ASCII, to the output.
     *
     * @implSpec
     * The default implementation appends the range with
     * {@link #writeUnicode(String, int, int)}, as the UTF-8 encoding of an ASCII character is
     * the character itself.
     *
     * @param value the string
     * @param start the index of the first character to append
     * @param end   the index after the last character to append
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected void writeAscii(String value, int start, int end) throws ProtobufSerializationException {
        writeUnicode(value, start, end);
    }

    /**
     * Appends the given number of spaces to the output, which indent a line.
     *
     * @implSpec
     * The default implementation appends the spaces with
     * {@link #writeAscii(byte[], int, int)}, at most {@code 64} at a time.
     *
     * @param count the number of spaces, which is positive
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected void writeSpaces(int count) throws ProtobufSerializationException {
        while (count > 0) {
            var chunk = Math.min(count, SPACES.length);
            writeAscii(SPACES, 0, chunk);
            count -= chunk;
        }
    }

    /**
     * Appends the start of the line of a field to the output: the given number of spaces, the
     * name of the field, and the separator that follows it.
     *
     * @implSpec
     * The default implementation appends the spaces with {@link #writeSpaces(int)}, the name
     * with {@link #writeUnicode(String, int, int)}, and the separator with
     * {@link #writeAscii(String)}.
     *
     * @param spaces the number of spaces that precede the name, which indent the line or
     *               separate it from the previous field
     * @param fieldName the name of the field
     * @param separator the separator that follows the name, which is ASCII
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected void writeFieldPrefix(int spaces, String fieldName, String separator) throws ProtobufSerializationException {
        if (spaces != 0) {
            writeSpaces(spaces);
        }
        writeUnicode(fieldName, 0, fieldName.length());
        writeAscii(separator);
    }

    /**
     * Creates the table of the tens or of the ones digits of the numbers from {@code 0} to
     * {@code 99}.
     *
     * @param tens whether the table holds the tens digits
     * @return the table, indexed by the number
     */
    private static byte[] createDigits(boolean tens) {
        var digits = new byte[100];
        for (var number = 0; number < digits.length; number++) {
            digits[number] = (byte) ('0' + (tens ? number / 10 : number % 10));
        }
        return digits;
    }

    /**
     * Creates the table of the sizes of the characters below {@code 0x80} in the value of a
     * {@code string} field, which are the sizes of their escape sequences.
     *
     * @return the table, indexed by the character
     */
    private static byte[] createEscapedAsciiSizes() {
        var sizes = new byte[0x80];
        for (var character = 0; character < sizes.length; character++) {
            sizes[character] = (byte) ProtobufTextSizeCalculator.getEscapedByteSize(character);
        }
        return sizes;
    }

    /**
     * Appends a range of a string, whose characters can be any Unicode character, to the
     * output.
     *
     * <p>Writers that produce bytes encode the characters as UTF-8.
     *
     * @param value the string
     * @param start the index of the first character to append
     * @param end   the index after the last character to append
     * @throws ProtobufSerializationException if the output cannot be written
     */
    protected abstract void writeUnicode(String value, int start, int end) throws ProtobufSerializationException;
}
