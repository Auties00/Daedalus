package com.github.auties00.daedalus.protobuf.spec;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufSizeTable;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeAccumulator;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;


/**
 * A specification of a Protocol Buffers message, which encodes, decodes and measures the
 * values of a type annotated with {@link com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage}
 * in the binary format and in the text format.
 *
 * <p>The annotation processor generates a subclass of this class for every message, named
 * after the message with the {@code Spec} suffix, whose only instance is returned by its
 * static {@code global()} method. A Spec is stateless, so it is thread safe and can be
 * shared freely. Code that works with any message, such as a registry that maps a type to
 * the Spec that decodes it, can hold a Spec through this class:
 *
 * <pre>{@code
 * ProtobufMessageSpec<Person, PersonBuilder> spec = PersonSpec.global();
 * byte[] encoded = spec.encodeBinary(person);
 * Person decoded = spec.decodeBinary(encoded);
 * }</pre>
 *
 * <p>Every method of this class accepts a {@code null} message or input, and returns
 * {@code null}, or writes nothing, for it. The checked exceptions thrown by the converters
 * of the fields of a message are wrapped in a {@link ProtobufSerializationException} or in a
 * {@link ProtobufDeserializationException}, whose cause is the exception thrown by the
 * converter. A generated subclass declares only the exceptions that its methods can throw.
 *
 * @apiNote
 * A message is encoded in two passes: the first one measures it, recording the sizes of its
 * nested messages, of its map entries and of its packed fields in a {@link ProtobufSizeTable},
 * and the second one writes it, reading those sizes back, so that no part of the message is
 * measured twice. The methods that take a size table expose these two passes, and are meant
 * to be invoked by the generated Spec of an enclosing message.
 *
 * @param <M> the type of the message
 * @param <B> the type of the builder of the message
 */
public abstract class ProtobufMessageSpec<M, B> {
    /**
     * The largest size of the text encoding returned by {@link #encodeText(Object)}, which is
     * the largest length of an array that every virtual machine can allocate.
     */
    private static final int TEXT_MAX_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Constructs a new Spec, which is only invoked by the generated subclass.
     */
    protected ProtobufMessageSpec() {

    }

    /**
     * Encodes the given message in the binary format.
     *
     * <p>The message is measured once, as described by {@link #sizeOfBinary(Object, ProtobufSizeTable)},
     * so that the returned array has exactly its size, and then written.
     *
     * @param message the message, or {@code null}
     * @return the binary encoding of the message, or {@code null} if it is {@code null}
     * @throws ProtobufSerializationException if a required field of the message is not set, if
     *         a repeated field holds a {@code null} element, or if a converter of a field fails
     */
    public byte[] encodeBinary(M message) throws ProtobufSerializationException {
        if (message == null) {
            return null;
        }

        var table = ProtobufSizeTable.of(sizeTableSlots(message));
        var writer = ProtobufBinaryWriter.toBytes(sizeOfBinary(message, table));
        encodeBinary(message, writer, table);
        return writer.toOutput();
    }

    /**
     * Encodes the given message in the binary format with the given writer.
     *
     * <p>The message is measured once, as described by {@link #sizeOfBinary(Object, ProtobufSizeTable)},
     * and then written.
     *
     * @param message the message, or {@code null}
     * @param writer the writer
     * @throws ProtobufSerializationException if the writer fails, if a required field of the
     *         message is not set, if a repeated field holds a {@code null} element, or if a
     *         converter of a field fails
     */
    public void encodeBinary(M message, ProtobufBinaryWriter<?> writer) throws ProtobufSerializationException {
        if (message == null) {
            return;
        }

        var table = ProtobufSizeTable.of(sizeTableSlots(message));
        sizeOfBinary(message, table);
        encodeBinary(message, writer, table);
    }

    /**
     * Writes the given message in the binary format with the given writer, reading the sizes
     * of its length delimited parts from the given table.
     *
     * <p>The table must have been filled by {@link #sizeOfBinary(Object, ProtobufSizeTable)}
     * for the same message, and every size that precedes the ones of this message must have
     * been read.
     *
     * @param message the message, or {@code null}
     * @param writer the writer
     * @param table the size table filled by measuring the message
     * @throws ProtobufSerializationException if the writer fails, if a required field of the
     *         message is not set, if a repeated field holds a {@code null} element, or if a
     *         converter of a field fails
     */
    public abstract void encodeBinary(M message, ProtobufBinaryWriter<?> writer, ProtobufSizeTable table) throws ProtobufSerializationException;

    /**
     * Returns the size of the binary encoding of the given message.
     *
     * @param message the message, or {@code null}
     * @return the size of the binary encoding of the message, or {@code 0} if it is
     *         {@code null}
     * @throws ProtobufSerializationException if a converter of a field fails
     */
    public int sizeOfBinary(M message) throws ProtobufSerializationException {
        return sizeOfBinary(message, ProtobufSizeTable.discarding());
    }

    /**
     * Returns the size of the binary encoding of the given message, and records the sizes of
     * its length delimited parts in the given table, in the order in which they are written.
     *
     * @param message the message, or {@code null}
     * @param table the size table to fill, or the {@linkplain ProtobufSizeTable#discarding()
     *        discarding table} to only measure the message
     * @return the size of the binary encoding of the message, or {@code 0} if it is
     *         {@code null}
     * @throws ProtobufSerializationException if a converter of a field fails
     */
    public abstract int sizeOfBinary(M message, ProtobufSizeTable table) throws ProtobufSerializationException;

    /**
     * Returns the number of slots of the size table needed to encode the given message, which
     * is the number of its nested messages, map entries and packed fields that have a length,
     * including the ones of its nested messages.
     *
     * @param message the message, or {@code null}
     * @return the number of slots, or {@code 0} if the message is {@code null}
     * @throws ProtobufSerializationException if a converter of a field fails
     */
    public abstract int sizeTableSlots(M message) throws ProtobufSerializationException;

    /**
     * Decodes a message from the given binary encoding.
     *
     * @param bytes the binary encoding, or {@code null}
     * @return the message, or {@code null} if {@code bytes} is {@code null}
     * @throws ProtobufDeserializationException if the input is malformed, if a required field of
     *         the message is not set, or if a converter of a field fails
     */
    public M decodeBinary(byte[] bytes) throws ProtobufDeserializationException {
        if (bytes == null) {
            return null;
        }

        return decodeBinary(ProtobufBinaryReader.fromBytes(bytes, 0, bytes.length));
    }

    /**
     * Decodes a message from the binary encoding read by the given reader.
     *
     * @param reader the reader
     * @return the message
     * @throws ProtobufDeserializationException if the input is malformed, if a required field of
     *         the message is not set, or if a converter of a field fails
     */
    public abstract M decodeBinary(ProtobufBinaryReader reader) throws ProtobufDeserializationException;

    /**
     * Decodes the fields of a message from the binary encoding read by the given reader into a
     * new builder, which is not built, so that a message whose required fields are not set can
     * be inspected.
     *
     * @param reader the reader
     * @return the builder that holds the decoded fields
     * @throws ProtobufDeserializationException if the input is malformed, or if a converter of a
     *         field fails
     */
    public B decodePartial(ProtobufBinaryReader reader) throws ProtobufDeserializationException {
        var builder = newBuilder();
        decodePartial(reader, builder);
        return builder;
    }

    /**
     * Decodes the fields of a message from the binary encoding read by the given reader into
     * the given builder, replacing the fields it already holds that are decoded again, and
     * adding to its repeated and map fields.
     *
     * @param reader the reader
     * @param builder the builder
     * @throws ProtobufDeserializationException if the input is malformed, or if a converter of a
     *         field fails
     */
    public abstract void decodePartial(ProtobufBinaryReader reader, B builder) throws ProtobufDeserializationException;

    /**
     * Encodes the given message in the text format, writing a field on each line and escaping
     * the characters of its {@code string} fields that are not ASCII.
     *
     * <p>The message is measured once, as described by
     * {@link #sizeOfText(Object, ProtobufTextSizeAccumulator, int)}, so that its UTF-8 encoding
     * is written into an array that has exactly its size, which becomes the internal array of
     * the returned string, as described by {@link CharsetUtils#adoptUtf8(byte[])}, so that the
     * text is not copied.
     *
     * @param message the message, or {@code null}
     * @return the text encoding of the message, or {@code null} if it is {@code null}
     * @throws ProtobufSerializationException if a required field of the message is not set, if
     *         a repeated field holds a {@code null} element, if a converter of a field fails, or
     *         if the text encoding is larger than an array can hold
     */
    public String encodeText(M message) throws ProtobufSerializationException {
        if (message == null) {
            return null;
        }

        var size = sizeOfText(message);
        if (size > TEXT_MAX_SIZE) {
            throw ProtobufSerializationException.textTooLarge(size, TEXT_MAX_SIZE);
        }

        var writer = ProtobufTextWriter.toBytes((int) size);
        encodeText(message, writer);
        // The array is only referenced by the writer, which is discarded, so the string can take ownership of it
        return CharsetUtils.adoptUtf8(writer.toOutput());
    }

    /**
     * Encodes the given message in the text format with the given writer.
     *
     * @param message the message, or {@code null}
     * @param writer the writer
     * @throws ProtobufSerializationException if the writer fails, if a required field of the
     *         message is not set, if a repeated field holds a {@code null} element, or if a
     *         converter of a field fails
     */
    public abstract void encodeText(M message, ProtobufTextWriter<?> writer) throws ProtobufSerializationException;

    /**
     * Returns the size, in bytes, of the UTF-8 encoding of the text written by
     * {@link #encodeText(Object)}, which writes a field on each line and escapes the characters
     * of the {@code string} fields that are not ASCII.
     *
     * @param message the message, or {@code null}
     * @return the size of the text encoding of the message, or {@code 0} if it is {@code null}
     * @throws ProtobufSerializationException if a repeated or map field holds a {@code null}
     *         element, key or value, or if a converter of a field fails
     */
    public long sizeOfText(M message) throws ProtobufSerializationException {
        return sizeOfText(message, false, true);
    }

    /**
     * Returns the size, in bytes, of the UTF-8 encoding of the text written by
     * {@link #encodeText(Object, ProtobufTextWriter)} to a new writer with the given settings.
     *
     * @param message the message, or {@code null}
     * @param singleLine whether the writer writes every field on the same line, like a writer
     *                   {@linkplain ProtobufTextWriter#setSingleLine(boolean) set to write a
     *                   single line}
     * @param escapeNonAscii whether the writer escapes the characters of {@code string} fields
     *                       that are not ASCII, like a writer
     *                       {@linkplain ProtobufTextWriter#setEscapeNonAscii(boolean) set to
     *                       escape them}
     * @return the size of the text encoding of the message, or {@code 0} if it is {@code null}
     * @throws ProtobufSerializationException if a repeated or map field holds a {@code null}
     *         element, key or value, or if a converter of a field fails
     */
    public long sizeOfText(M message, boolean singleLine, boolean escapeNonAscii) throws ProtobufSerializationException {
        var accumulator = new ProtobufTextSizeAccumulator(singleLine, escapeNonAscii);
        sizeOfText(message, accumulator, 0);
        return accumulator.size();
    }

    /**
     * Adds the size of the lines of the fields of the given message, written at the given
     * depth, to the given accumulator.
     *
     * <p>The required fields are not checked, as the size of a message that cannot be written
     * is never used.
     *
     * @param message the message, or {@code null}, which adds nothing
     * @param accumulator the accumulator of the size of the text that encloses the message
     * @param depth the number of messages that enclose the fields of the message
     * @throws ProtobufSerializationException if a repeated or map field holds a {@code null}
     *         element, key or value, or if a converter of a field fails
     */
    public abstract void sizeOfText(M message, ProtobufTextSizeAccumulator accumulator, int depth) throws ProtobufSerializationException;

    /**
     * Decodes a message from the given text encoding.
     *
     * @param text the text encoding, or {@code null}
     * @return the message, or {@code null} if {@code text} is {@code null}
     * @throws ProtobufDeserializationException if the input is malformed, if a required field of
     *         the message is not set, or if a converter of a field fails
     */
    public M decodeText(CharSequence text) throws ProtobufDeserializationException {
        if (text == null) {
            return null;
        }

        return decodeText(ProtobufTextReader.fromCharSequence(text));
    }

    /**
     * Decodes a message from the text encoding read by the given reader.
     *
     * @param reader the reader
     * @return the message
     * @throws ProtobufDeserializationException if the input is malformed, if a required field of
     *         the message is not set, or if a converter of a field fails
     */
    public abstract M decodeText(ProtobufTextReader reader) throws ProtobufDeserializationException;

    /**
     * Decodes the fields of a message from the text encoding read by the given reader into a
     * new builder, which is not built, so that a message whose required fields are not set can
     * be inspected.
     *
     * @param reader the reader
     * @return the builder that holds the decoded fields
     * @throws ProtobufDeserializationException if the input is malformed, or if a converter of a
     *         field fails
     */
    public B decodePartial(ProtobufTextReader reader) throws ProtobufDeserializationException {
        var builder = newBuilder();
        decodePartial(reader, builder);
        return builder;
    }

    /**
     * Decodes the fields of a message from the text encoding read by the given reader into the
     * given builder, replacing the fields it already holds that are decoded again, and adding
     * to its repeated and map fields.
     *
     * @param reader the reader
     * @param builder the builder
     * @throws ProtobufDeserializationException if the input is malformed, or if a converter of a
     *         field fails
     */
    public abstract void decodePartial(ProtobufTextReader reader, B builder) throws ProtobufDeserializationException;

    /**
     * Returns a new builder of the message, which holds no field.
     *
     * @return the builder
     */
    public abstract B newBuilder();
}
