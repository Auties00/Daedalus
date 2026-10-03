package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * A text writer that writes the UTF-8 encoding of the text to an {@link OutputStream}, through
 * a buffer.
 *
 * <p>The buffer is written to the stream when it is full, when the output is
 * {@linkplain #toOutput() requested}, and when the writer is closed.
 */
public final class ProtobufTextStreamWriter extends ProtobufTextByteWriter<OutputStream> {
    /**
     * The size of the buffer used when none is specified.
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The stream the text is written to.
     */
    private final OutputStream outputStream;

    /**
     * Whether the stream is closed when the writer is closed.
     */
    private final boolean autoclose;

    /**
     * The bytes that were not written to the stream yet.
     */
    private final byte[] buffer;

    /**
     * The number of bytes held by the buffer.
     */
    private int bufferPosition;

    /**
     * Constructs a new writer for the given stream, with a buffer of the default size.
     *
     * @param outputStream the stream
     * @param autoclose    whether the stream is closed when the writer is closed
     * @throws NullPointerException if {@code outputStream} is {@code null}
     */
    public ProtobufTextStreamWriter(OutputStream outputStream, boolean autoclose) {
        this(outputStream, autoclose, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a new writer for the given stream, with a buffer of the given size.
     *
     * @param outputStream the stream
     * @param autoclose    whether the stream is closed when the writer is closed
     * @param bufferSize   the size of the buffer
     * @throws NullPointerException     if {@code outputStream} is {@code null}
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public ProtobufTextStreamWriter(OutputStream outputStream, boolean autoclose, int bufferSize) {
        Objects.requireNonNull(outputStream, "outputStream cannot be null");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
        this.outputStream = outputStream;
        this.autoclose = autoclose;
        this.buffer = new byte[bufferSize];
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the stream cannot be written
     */
    @Override
    void writeByte(byte value) throws ProtobufSerializationException {
        if (bufferPosition == buffer.length) {
            try {
                flushBuffer();
            } catch (IOException exception) {
                throw ProtobufSerializationException.outputStreamFailure(exception);
            }
        }
        buffer[bufferPosition++] = value;
    }

    /**
     * Writes the bytes held by the buffer to the stream, and empties the buffer.
     *
     * @throws IOException if the stream cannot be written
     */
    private void flushBuffer() throws IOException {
        if (bufferPosition != 0) {
            outputStream.write(buffer, 0, bufferPosition);
            bufferPosition = 0;
        }
    }

    /**
     * Writes the bytes held by the buffer to the stream, and returns the stream.
     *
     * @return the stream
     * @throws UncheckedIOException if the stream cannot be written
     */
    @Override
    public OutputStream toOutput() {
        try {
            flushBuffer();
            outputStream.flush();
            return outputStream;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Writes the bytes held by the buffer to the stream, and closes the stream if the writer
     * was created with {@code autoclose} set to {@code true}.
     *
     * @throws IOException if the stream cannot be written or closed
     */
    @Override
    public void close() throws IOException {
        try {
            flushBuffer();
            outputStream.flush();
        } finally {
            if (autoclose) {
                outputStream.close();
            }
        }
    }
}
