package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;

/**
 * A text writer that writes nothing, but counts the bytes of the UTF-8 encoding of the text it
 * would write.
 *
 * <p>It measures the text written by code that can only write it with a text writer, like a
 * converter, so that the size of the text of a message can be computed before it is written.
 * The count is returned by {@link #toOutput()}.
 */
public final class ProtobufTextSizeWriter extends ProtobufTextWriter<Long> {
    /**
     * The number of bytes counted so far.
     */
    private long size;

    /**
     * Constructs a new counting writer with the given settings, which writes its first field
     * at the given depth.
     *
     * @param singleLine whether every field is written on the same line
     * @param escapeNonAscii whether the characters of {@code string} fields that are not ASCII
     *                       are escaped
     * @param depth the number of messages that enclose the first field
     * @param atStartOfLine whether the first field starts a new line, as it follows other
     *                      fields
     */
    public ProtobufTextSizeWriter(boolean singleLine, boolean escapeNonAscii, int depth, boolean atStartOfLine) {
        super(singleLine, escapeNonAscii, depth, atStartOfLine);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The character is counted as a single byte.
     */
    @Override
    protected void writeAscii(char value) {
        size++;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every character is counted as a single byte.
     */
    @Override
    protected void writeAscii(String value) {
        size += value.length();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every character is counted as a single byte.
     */
    @Override
    protected void writeAscii(byte[] value, int offset, int length) {
        size += length;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every space is counted as a single byte.
     */
    @Override
    protected void writeSpaces(int count) {
        size += count;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The characters are counted as the bytes of their UTF-8 encoding. A surrogate that is
     * not part of a pair is counted as a single byte, as it is encoded as a question mark.
     */
    @Override
    protected void writeUnicode(String value, int start, int end) {
        if (start == 0 && end == value.length()) {
            size += CharsetUtils.utf8Length(value);
            return;
        }

        var i = start;
        while (i < end) {
            var character = value.charAt(i);
            if (character < 0x80) {
                size++;
                i++;
            } else if (character < 0x800) {
                size += 2;
                i++;
            } else if (!Character.isSurrogate(character)) {
                size += 3;
                i++;
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(value.charAt(i + 1))) {
                size += 4;
                i += 2;
            } else {
                size++;
                i++;
            }
        }
    }

    /**
     * Returns the number of bytes counted so far.
     *
     * @return the size, in bytes, of the UTF-8 encoding of the text that was written
     */
    @Override
    public Long toOutput() {
        return size;
    }

    /**
     * Does nothing, as a counting writer holds no resource.
     */
    @Override
    public void close() {

    }
}
