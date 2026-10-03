package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;

/**
 * A text writer that writes the UTF-8 encoding of the text, which is the shared base of the
 * writers whose output is made of bytes.
 *
 * <p>Concrete subclasses only append bytes to their output.
 *
 * @param <OUTPUT> the type of the output produced by this writer
 */
abstract sealed class ProtobufTextByteWriter<OUTPUT> extends ProtobufTextWriter<OUTPUT>
        permits ProtobufTextByteArrayWriter, ProtobufTextByteBufferWriter, ProtobufTextMemorySegmentWriter, ProtobufTextStreamWriter {
    /**
     * Constructs a new text writer whose output is made of bytes.
     */
    ProtobufTextByteWriter() {

    }

    /**
     * {@inheritDoc}
     *
     * <p>The character is written as a single byte.
     */
    @Override
    protected void writeAscii(char value) throws ProtobufSerializationException {
        writeByte((byte) value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every character is written as a single byte.
     */
    @Override
    protected void writeAscii(String value) throws ProtobufSerializationException {
        for (var i = 0; i < value.length(); i++) {
            writeByte((byte) value.charAt(i));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every character is written as a single byte.
     */
    @Override
    protected void writeAscii(byte[] value, int offset, int length) throws ProtobufSerializationException {
        for (var i = offset; i < offset + length; i++) {
            writeByte(value[i]);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A surrogate that is not part of a pair is encoded as a question mark, like
     * {@link String#getBytes(java.nio.charset.Charset)} does.
     */
    @Override
    protected void writeUnicode(String value, int start, int end) throws ProtobufSerializationException {
        var i = start;
        while (i < end) {
            var character = value.charAt(i);
            if (character < 0x80) {
                writeByte((byte) character);
                i++;
            } else if (character < 0x800) {
                writeByte((byte) (0xC0 | (character >>> 6)));
                writeByte((byte) (0x80 | (character & 0x3F)));
                i++;
            } else if (!Character.isSurrogate(character)) {
                writeByte((byte) (0xE0 | (character >>> 12)));
                writeByte((byte) (0x80 | ((character >>> 6) & 0x3F)));
                writeByte((byte) (0x80 | (character & 0x3F)));
                i++;
            } else if (Character.isHighSurrogate(character) && i + 1 < end && Character.isLowSurrogate(value.charAt(i + 1))) {
                var codePoint = Character.toCodePoint(character, value.charAt(i + 1));
                writeByte((byte) (0xF0 | (codePoint >>> 18)));
                writeByte((byte) (0x80 | ((codePoint >>> 12) & 0x3F)));
                writeByte((byte) (0x80 | ((codePoint >>> 6) & 0x3F)));
                writeByte((byte) (0x80 | (codePoint & 0x3F)));
                i += 2;
            } else {
                writeByte((byte) '?');
                i++;
            }
        }
    }

    /**
     * Appends a byte to the output.
     *
     * @param value the byte
     * @throws ProtobufSerializationException if the output cannot be written
     */
    abstract void writeByte(byte value) throws ProtobufSerializationException;
}
