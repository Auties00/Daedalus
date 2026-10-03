package com.github.auties00.daedalus.protobuf.io.size;

import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;

/**
 * An accumulator of the size, in bytes, of the UTF-8 encoding of the text written by a
 * {@link ProtobufTextWriter} with the given settings, which is computed without writing it.
 *
 * <p>A text writer writes every field on a line of its own, preceded by two spaces for each
 * message that encloses it and followed by a new line, or, if it writes a single line, writes
 * every field after the previous one, separated by a single space. The line of a field holds
 * its name and its value, or the name of a message field followed by an opening brace, or the
 * closing brace of a message. Every line is added by {@link #addLine(int, long)}, or by
 * {@link #addMessageLines(int, long)} for the lines that open and close a message, which add
 * their separators, so that the size of the content of a line does not depend on the settings,
 * except for the size of the value of a {@code string} field, which depends on whether the
 * characters that are not ASCII are escaped.
 *
 * <p>The text written by a converter that writes it with a text writer cannot be computed
 * without writing it: {@link #newWriter(int)} returns a writer that only counts the bytes it
 * would write, which {@link #addWritten(ProtobufTextWriter)} adds to this accumulator.
 *
 * <p>This class is not thread safe: an accumulator belongs to the computation of the size of
 * a single message.
 */
public final class ProtobufTextSizeAccumulator {
    /**
     * Whether every field is written on the same line.
     */
    private final boolean singleLine;

    /**
     * Whether the characters of a {@code string} field that are not ASCII are escaped.
     */
    private final boolean escapeNonAscii;

    /**
     * The number of spaces that indent a line for each message that encloses it, which is
     * {@code 0} if the writer writes a single line.
     */
    private final int indentation;

    /**
     * The size accumulated so far, which counts a separator before every line, so a space
     * before the first line if the writer writes a single line.
     */
    private long size;

    /**
     * The builder used to format floating point values, which is created the first time one is
     * measured.
     */
    private StringBuilder decimal;

    /**
     * Constructs an empty accumulator for a writer with the given settings.
     *
     * @param singleLine whether the writer writes every field on the same line, like a writer
     *                   {@linkplain ProtobufTextWriter#setSingleLine(boolean) set to write a
     *                   single line}
     * @param escapeNonAscii whether the writer escapes the characters of {@code string} fields
     *                       that are not ASCII, like a writer
     *                       {@linkplain ProtobufTextWriter#setEscapeNonAscii(boolean) set to
     *                       escape them}
     */
    public ProtobufTextSizeAccumulator(boolean singleLine, boolean escapeNonAscii) {
        this.singleLine = singleLine;
        this.escapeNonAscii = escapeNonAscii;
        this.indentation = singleLine ? 0 : 2;
    }

    /**
     * Returns whether the writer writes every field on the same line.
     *
     * @return {@code true} if the fields are separated by a single space
     */
    public boolean isSingleLine() {
        return singleLine;
    }

    /**
     * Returns whether the writer escapes the characters of {@code string} fields that are not
     * ASCII.
     *
     * @return {@code true} if they are written as octal escape sequences
     */
    public boolean isEscapeNonAscii() {
        return escapeNonAscii;
    }

    /**
     * Adds a line with the given content, written at the given depth, together with its
     * separators: its indentation and the new line that ends it, or the space that precedes
     * it if the writer writes a single line.
     *
     * @implNote
     * Every line is counted with a separator, so that the size of a line does not depend on
     * the lines that precede it: the space that precedes the first line of a single line text,
     * which is not written, is removed by {@link #size()}.
     *
     * @param depth the number of messages that enclose the line
     * @param contentSize the size of the content of the line, without its separators
     */
    public void addLine(int depth, long contentSize) {
        size += contentSize + (long) indentation * depth + 1;
    }

    /**
     * Adds the line that opens a message, with the given content, and the line that closes it,
     * which holds a closing brace, both written at the given depth together with their
     * separators.
     *
     * <p>The fields of the message are added separately, one level deeper, as the order in
     * which lines are added does not change their size.
     *
     * @param depth the number of messages that enclose the message
     * @param headerSize the size of the content of the line that opens the message, which is
     *                   the name of its field followed by a space and an opening brace
     */
    public void addMessageLines(int depth, long headerSize) {
        size += headerSize + 1 + 2 * ((long) indentation * depth + 1);
    }

    /**
     * Returns a writer with the settings of this accumulator, which only counts the bytes it
     * would write, and which writes its first field on a new line at the given depth.
     *
     * <p>The writer is used to compute the size of the text written by a converter with a text
     * writer, and must be passed to {@link #addWritten(ProtobufTextWriter)} afterwards.
     *
     * @param depth the number of messages that enclose the fields written to the writer
     * @return a new counting writer
     */
    public ProtobufTextWriter<Long> newWriter(int depth) {
        // Like every line added to this accumulator, the first line of the writer is counted with its separator
        return ProtobufTextWriter.toSize(singleLine, escapeNonAscii, depth, true);
    }

    /**
     * Adds the size of the text counted by the given writer, returned by {@link #newWriter(int)}.
     *
     * @param writer the counting writer
     */
    public void addWritten(ProtobufTextWriter<Long> writer) {
        size += writer.toOutput();
    }

    /**
     * Returns the size of the given {@code float}, formatted like {@link Float#toString(float)}
     * does.
     *
     * @param value the value
     * @return the number of characters of the formatted value
     */
    public int getFloatSize(float value) {
        var builder = decimal();
        builder.append(value);
        return builder.length();
    }

    /**
     * Returns the size of the given {@code double}, formatted like
     * {@link Double#toString(double)} does.
     *
     * @param value the value
     * @return the number of characters of the formatted value
     */
    public int getDoubleSize(double value) {
        var builder = decimal();
        builder.append(value);
        return builder.length();
    }

    /**
     * Returns the size of the quoted value of a {@code string} field, without its quotes,
     * escaped as the writer escapes it.
     *
     * @param value the value
     * @return the size of the escaped value
     */
    public long getStringSize(String value) {
        return ProtobufTextSizeCalculator.getEscapedStringSize(value, escapeNonAscii);
    }

    /**
     * Returns the size accumulated so far.
     *
     * @return the size, in bytes, of the UTF-8 encoding of the text
     */
    public long size() {
        // The first line of a single line text is not preceded by a space
        return singleLine && size != 0 ? size - 1 : size;
    }

    /**
     * Returns the builder used to format floating point values, emptied.
     *
     * @return the builder
     */
    private StringBuilder decimal() {
        if (decimal == null) {
            decimal = new StringBuilder(32);
        } else {
            decimal.setLength(0);
        }
        return decimal;
    }
}
