package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;

import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * A text writer that writes the text into a {@link CharBuffer}, starting at its position.
 *
 * <p>The text cannot exceed the remaining characters of the buffer: a write that does not fit
 * throws a {@link ProtobufSerializationException}. The position of the buffer passed to the
 * constructor is not changed.
 */
public final class ProtobufTextCharBufferWriter extends ProtobufTextWriter<CharBuffer> {
    /**
     * A duplicate of the buffer passed to the constructor, whose position is the index of the
     * next character to write.
     */
    private final CharBuffer buffer;

    /**
     * The index of the first character of the text.
     */
    private final int start;

    /**
     * Constructs a new writer for the given buffer.
     *
     * @param buffer the buffer, which must not be read only
     * @throws NullPointerException if {@code buffer} is {@code null}
     */
    public ProtobufTextCharBufferWriter(CharBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        this.buffer = buffer.duplicate();
        this.start = buffer.position();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the buffer is full
     */
    @Override
    protected void writeAscii(char value) throws ProtobufSerializationException {
        try {
            buffer.put(value);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.outputFull(buffer.limit() - start);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the buffer is full
     */
    @Override
    protected void writeAscii(String value) throws ProtobufSerializationException {
        writeUnicode(value, 0, value.length());
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the buffer is full
     */
    @Override
    protected void writeAscii(byte[] value, int offset, int length) throws ProtobufSerializationException {
        if (length > buffer.remaining()) {
            throw ProtobufSerializationException.outputFull(buffer.limit() - start);
        }
        for (var i = offset; i < offset + length; i++) {
            buffer.put((char) value[i]);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the buffer is full
     */
    @Override
    protected void writeUnicode(String value, int start, int end) throws ProtobufSerializationException {
        try {
            buffer.put(value, start, end);
        } catch (BufferOverflowException _) {
            throw ProtobufSerializationException.outputFull(buffer.limit() - this.start);
        }
    }

    /**
     * Returns a buffer that holds the text that was written.
     *
     * <p>The returned buffer shares the content of the buffer passed to the constructor: its
     * position is the first character of the text, and its limit is after the last character
     * of the text.
     *
     * @return a buffer that holds the text
     */
    @Override
    public CharBuffer toOutput() {
        return buffer.duplicate()
                .limit(buffer.position())
                .position(start);
    }

    /**
     * Does nothing, as a buffer needs no closing.
     */
    @Override
    public void close() {

    }
}
