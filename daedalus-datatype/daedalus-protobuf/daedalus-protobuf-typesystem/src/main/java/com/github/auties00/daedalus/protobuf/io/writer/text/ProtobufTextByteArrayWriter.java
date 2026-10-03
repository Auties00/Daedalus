package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeCalculator;

import java.util.Arrays;
import java.util.Objects;

/**
 * A text writer that writes the UTF-8 encoding of the text into a range of a byte array.
 *
 * <p>The text cannot exceed the range: a write that does not fit throws a
 * {@link ProtobufSerializationException}.
 */
public final class ProtobufTextByteArrayWriter extends ProtobufTextByteWriter<byte[]> {
    /**
     * The length of the longest range that is copied one byte at a time, as a short range, like
     * the separator between the name and the value of a field, or the indentation of a line, is
     * copied faster by a loop than by a bulk copy, whose fixed cost is higher.
     */
    private static final int SHORT_RANGE = 16;

    /**
     * The byte array the text is written into.
     */
    private final byte[] buffer;

    /**
     * The index of the first byte of the text.
     */
    private final int start;

    /**
     * The index after the last byte that can be written.
     */
    private final int limit;

    /**
     * The index of the next byte to write.
     */
    private int offset;

    /**
     * Constructs a new writer for the given range of a byte array.
     *
     * @param buffer the byte array
     * @param offset the index of the first byte to write
     * @param limit  the index after the last byte that can be written
     * @throws NullPointerException      if {@code buffer} is {@code null}
     * @throws IndexOutOfBoundsException if the range is not within {@code buffer}
     */
    public ProtobufTextByteArrayWriter(byte[] buffer, int offset, int limit) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.checkFromToIndex(offset, limit, buffer.length);
        this.buffer = buffer;
        this.start = offset;
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    void writeByte(byte value) throws ProtobufSerializationException {
        if (offset >= limit) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        buffer[offset++] = value;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeAscii(char value) throws ProtobufSerializationException {
        if (offset >= limit) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        buffer[offset++] = (byte) value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The characters are stored into the array directly while they are ASCII, and the
     * remaining ones are encoded one byte at a time.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeUnicode(String value, int start, int end) throws ProtobufSerializationException {
        // Every character takes at least one byte, so a range that has more characters than the bytes left cannot fit
        if (end - start > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - this.start);
        }

        var position = offset;
        var i = start;
        while (i < end) {
            var character = value.charAt(i);
            if (character >= 0x80) {
                break;
            }
            buffer[position++] = (byte) character;
            i++;
        }
        offset = position;
        if (i < end) {
            super.writeUnicode(value, i, end);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A short string is copied one character at a time, and a longer one at once, as the low
     * byte of an ASCII character is its UTF-8 encoding.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    @SuppressWarnings("deprecation") // The characters are ASCII, so the bytes that this method copies are their UTF-8 encoding
    protected void writeAscii(String value) throws ProtobufSerializationException {
        var length = value.length();
        if (length > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        if (length <= SHORT_RANGE) {
            for (var i = 0; i < length; i++) {
                buffer[offset++] = (byte) value.charAt(i);
            }
        } else {
            value.getBytes(0, length, buffer, offset);
            offset += length;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeAscii(byte[] value, int offset, int length) throws ProtobufSerializationException {
        if (length > limit - this.offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        System.arraycopy(value, offset, buffer, this.offset, length);
        this.offset += length;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The capacity of the array is checked once, for the whole prefix, and its characters are
     * stored into the array directly, unless the name is not ASCII, in which case the name and
     * the separator are appended like the default implementation appends them.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeFieldPrefix(int spaces, String fieldName, String separator) throws ProtobufSerializationException {
        var nameLength = fieldName.length();
        var separatorLength = separator.length();
        // Every character takes at least one byte, so a prefix that has more characters than the bytes left cannot fit
        if (spaces + nameLength + separatorLength > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }

        var position = offset;
        for (var i = 0; i < spaces; i++) {
            buffer[position++] = ' ';
        }
        for (var i = 0; i < nameLength; i++) {
            var character = fieldName.charAt(i);
            if (character >= 0x80) {
                offset = position;
                writeUnicode(fieldName, i, nameLength);
                writeAscii(separator);
                return;
            }
            buffer[position++] = (byte) character;
        }
        for (var i = 0; i < separatorLength; i++) {
            buffer[position++] = (byte) separator.charAt(i);
        }
        offset = position;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value is formatted directly into the array, after its number of characters is
     * computed, so that the capacity of the array is checked once.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeSigned(long value) throws ProtobufSerializationException {
        var length = ProtobufTextSizeCalculator.getSignedSize(value);
        if (length > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        offset += length;
        formatSigned(value, buffer, offset);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value is formatted directly into the array, after its number of digits is
     * computed, so that the capacity of the array is checked once.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeUnsigned(long value) throws ProtobufSerializationException {
        var length = ProtobufTextSizeCalculator.getUnsignedSize(value);
        if (length > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        offset += length;
        formatUnsigned(value, buffer, offset);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The range is copied into the array at once, as the low byte of an ASCII character is
     * its UTF-8 encoding.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    @SuppressWarnings("deprecation") // The characters are ASCII, so the bytes that this method copies are their UTF-8 encoding
    protected void writeAscii(String value, int start, int end) throws ProtobufSerializationException {
        var length = end - start;
        if (length > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - this.start);
        }
        value.getBytes(start, end, buffer, offset);
        offset += length;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A few spaces are stored one at a time, and more are filled into the array at once.
     *
     * @throws ProtobufSerializationException if the range of the byte array is full
     */
    @Override
    protected void writeSpaces(int count) throws ProtobufSerializationException {
        if (count > limit - offset) {
            throw ProtobufSerializationException.outputFull(limit - start);
        }
        if (count <= SHORT_RANGE) {
            for (var i = 0; i < count; i++) {
                buffer[offset++] = ' ';
            }
        } else {
            Arrays.fill(buffer, offset, offset + count, (byte) ' ');
            offset += count;
        }
    }

    /**
     * Returns the bytes of the text that was written.
     *
     * <p>The byte array is returned as it is if the text fills it, and the bytes of the text are
     * copied into a new array otherwise.
     *
     * @return the UTF-8 encoding of the text
     */
    @Override
    public byte[] toOutput() {
        if (start == 0 && offset == buffer.length) {
            return buffer;
        }
        return Arrays.copyOfRange(buffer, start, offset);
    }

    /**
     * Does nothing, as a byte array needs no closing.
     */
    @Override
    public void close() {

    }
}
