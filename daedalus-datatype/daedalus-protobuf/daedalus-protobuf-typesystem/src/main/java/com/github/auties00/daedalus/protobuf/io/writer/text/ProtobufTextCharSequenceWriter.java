package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;

/**
 * A text writer that writes the text into a {@link StringBuilder}, which grows as needed.
 */
public final class ProtobufTextCharSequenceWriter extends ProtobufTextWriter<String> {
    /**
     * The text that was written.
     */
    private final StringBuilder output;

    /**
     * Constructs a new writer whose text has the given initial capacity.
     *
     * @param initialCapacity the initial capacity of the text
     * @throws NegativeArraySizeException if {@code initialCapacity} is negative
     */
    public ProtobufTextCharSequenceWriter(int initialCapacity) {
        this.output = new StringBuilder(initialCapacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAscii(char value) {
        output.append(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAscii(String value) {
        output.append(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAscii(byte[] value, int offset, int length) {
        for (var i = offset; i < offset + length; i++) {
            output.append((char) value[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeUnicode(String value, int start, int end) {
        output.append(value, start, end);
    }

    /**
     * Returns the text that was written.
     *
     * @return the text
     */
    @Override
    public String toOutput() {
        return output.toString();
    }

    /**
     * Does nothing, as a {@code StringBuilder} needs no closing.
     */
    @Override
    public void close() {

    }
}
