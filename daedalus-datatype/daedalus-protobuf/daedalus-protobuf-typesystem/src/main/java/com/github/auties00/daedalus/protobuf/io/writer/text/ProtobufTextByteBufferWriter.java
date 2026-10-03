package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A text writer that writes the UTF-8 encoding of the text into a {@link ByteBuffer}, starting
 * at its position.
 *
 * <p>The text cannot exceed the remaining bytes of the buffer: a write that does not fit throws
 * a {@link ProtobufSerializationException}. The position of the buffer passed to the
 * constructor is not changed.
 */
public final class ProtobufTextByteBufferWriter extends ProtobufTextByteWriter<ByteBuffer> {
    /**
     * A duplicate of the buffer passed to the constructor, whose position is the index of the
     * next byte to write.
     */
    private final ByteBuffer buffer;

    /**
     * The index of the first byte of the text.
     */
    private final int start;

    /**
     * Constructs a new writer for the given buffer.
     *
     * @param buffer the buffer, which must not be read only
     * @throws NullPointerException if {@code buffer} is {@code null}
     */
    public ProtobufTextByteBufferWriter(ByteBuffer buffer) {
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
    void writeByte(byte value) throws ProtobufSerializationException {
        if (!buffer.hasRemaining()) {
            throw ProtobufSerializationException.outputFull(buffer.limit() - start);
        }
        buffer.put(value);
    }

    /**
     * Returns a buffer that holds the text that was written.
     *
     * <p>The returned buffer shares the content of the buffer passed to the constructor: its
     * position is the first byte of the text, and its limit is after the last byte of the text.
     *
     * @return a buffer that holds the UTF-8 encoding of the text
     */
    @Override
    public ByteBuffer toOutput() {
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
