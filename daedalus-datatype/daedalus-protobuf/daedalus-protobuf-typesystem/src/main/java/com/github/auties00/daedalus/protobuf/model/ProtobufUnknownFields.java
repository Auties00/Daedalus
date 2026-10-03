package com.github.auties00.daedalus.protobuf.model;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeAccumulator;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeCalculator;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.typesystem.util.CharsetUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The fields that a message does not declare, encountered while parsing it from any format.
 *
 * <p>Like the {@code UnknownFieldSet} of the official protobuf library, the unknown fields
 * keep every value of each field, as an unknown field may be repeated: adding a value to a
 * field that already has values appends it, and the values of a field keep the order in
 * which they were added. The fields read from each format are kept side by side:
 * <ul>
 * <li>the {@linkplain #binaryFields() binary fields}, read from the binary format, are
 *     identified by index and ordered by index, which is the order in which the official
 *     library writes unknown fields;
 * <li>the {@linkplain #textFields() text fields}, read from the text format, are identified by
 *     name and ordered by the first time their name was added.
 * </ul>
 *
 * <p>Each format only writes the fields read from it: {@link #encodeBinary(ProtobufBinaryWriter)}
 * writes the binary fields, as a text field has no index, and
 * {@link #encodeText(ProtobufTextWriter)} writes the text fields, as a binary field has no name.
 * This way, every field that a format writes can be read back from it.
 *
 * <p>The unknown fields can also encode and decode a whole message without a schema: no
 * generated Spec classes, annotations, or reflection are required.
 * {@link #decodeBinary(ProtobufBinaryReader)} keeps every field of a message read from the
 * binary format, which is useful to inspect a message without its {@code .proto} definition,
 * or to re-encode it after changing it, and {@link #decodeText(ProtobufTextReader)} does the same
 * for the text format. The payload of a length delimited field is kept as raw bytes, as the wire
 * format cannot tell an embedded message apart from a string or a packed field: decode it again
 * if it is known to be a message. These methods are slow compared to the generated Spec
 * classes, so use them only when the generated Spec classes are not available.
 *
 * <p>Iterating over the unknown fields iterates over the binary fields, and then over the text
 * fields. The fields are live: a value added to the unknown fields is visible through the
 * field it belongs to, and a value added to a field belongs to the unknown fields.
 *
 * <p>Two unknown fields are equal if they hold equal fields, each with equal values in the
 * same order.
 *
 * <p>This class is not thread-safe.
 */
public final class ProtobufUnknownFields implements Iterable<ProtobufUnknownField<?>> {
    /**
     * The binary fields, keyed by index in ascending order.
     */
    private final TreeMap<Integer, ProtobufUnknownField.Binary> binaryFields;

    /**
     * The text fields, keyed by name in insertion order.
     */
    private final SequencedMap<String, ProtobufUnknownField.Text> textFields;

    /**
     * Creates empty unknown fields.
     */
    public ProtobufUnknownFields() {
        this.binaryFields = new TreeMap<>();
        this.textFields = new LinkedHashMap<>();
    }

    /**
     * Decodes every field of a message from the given binary reader, without a schema.
     *
     * <p>Every field is read according to its wire type, and stored under its number, keeping
     * every value of a repeated field.
     *
     * <p><span style="color: red;"><strong>Warning:</strong> This method is slow compared to the
     * generated Spec classes. Use it only when the generated Spec classes are
     * not available.</span></p>
     *
     * @param reader the reader of the message
     * @return the fields of the message
     * @throws ProtobufDeserializationException if the message cannot be decoded
     * @throws NullPointerException if {@code reader} is {@code null}
     * @see ProtobufBinaryReader#readUnknownField()
     */
    public static ProtobufUnknownFields decodeBinary(ProtobufBinaryReader reader) throws ProtobufDeserializationException {
        Objects.requireNonNull(reader, "reader cannot be null");
        var result = new ProtobufUnknownFields();
        while (reader.nextFieldTag()) {
            result.add(reader.fieldIndex(), reader.readUnknownField());
        }
        return result;
    }

    /**
     * Decodes every field of a message from the given text reader, without a schema.
     *
     * <p>Every value of each field is stored under the name of the field, keeping every value
     * of a repeated field. The values of a list are stored as separate values, and a message is
     * decoded as fields of its own. The name of an extension, or the type URL of an expanded
     * {@code Any}, is stored with its enclosing square brackets. The method decodes both a top
     * level message and a nested one, enclosed in braces or angle brackets.
     *
     * <p><span style="color: red;"><strong>Warning:</strong> This method is slow compared to the
     * generated Spec classes. Use it only when the generated Spec classes are
     * not available.</span></p>
     *
     * @param reader the reader of the message
     * @return the fields of the message
     * @throws ProtobufDeserializationException if the message cannot be decoded
     * @throws NullPointerException if {@code reader} is {@code null}
     * @see ProtobufTextReader#readUnknownField(ProtobufUnknownFields)
     */
    public static ProtobufUnknownFields decodeText(ProtobufTextReader reader) throws ProtobufDeserializationException {
        Objects.requireNonNull(reader, "reader cannot be null");
        var result = new ProtobufUnknownFields();
        reader.readStartMessage();
        while (reader.nextField()) {
            reader.readUnknownField(result);
        }
        reader.readEndMessage();
        return result;
    }

    /**
     * Returns the number of fields, binary and text.
     *
     * @return the number of fields
     */
    public int size() {
        return binaryFields.size() + textFields.size();
    }

    /**
     * Returns whether there are no fields.
     *
     * @return {@code true} if there are no fields
     */
    public boolean isEmpty() {
        return binaryFields.isEmpty() && textFields.isEmpty();
    }

    /**
     * Removes every field.
     */
    public void clear() {
        binaryFields.clear();
        textFields.clear();
    }

    /**
     * Returns an unmodifiable view of the binary fields, ordered by index.
     *
     * <p>The view does not copy the unknown fields, so it reflects later changes.
     *
     * @return a sequenced view of the binary fields
     */
    public SequencedCollection<ProtobufUnknownField.Binary> binaryFields() {
        return Collections.unmodifiableSequencedCollection(binaryFields.sequencedValues());
    }

    /**
     * Returns an unmodifiable view of the text fields, in insertion order.
     *
     * <p>The view does not copy the unknown fields, so it reflects later changes.
     *
     * @return a sequenced view of the text fields
     */
    public SequencedCollection<ProtobufUnknownField.Text> textFields() {
        return Collections.unmodifiableSequencedCollection(textFields.sequencedValues());
    }

    /**
     * Returns a sequential {@link Stream} of the binary fields, followed by the text fields.
     *
     * @return a stream of the fields
     */
    public Stream<ProtobufUnknownField<?>> stream() {
        return Stream.concat(binaryFields.values().stream(), textFields.values().stream());
    }

    /**
     * Returns an iterator over the binary fields, followed by the text fields, which does not
     * support removal.
     *
     * @return an iterator over the fields
     */
    @Override
    public Iterator<ProtobufUnknownField<?>> iterator() {
        var binaryIterator = binaryFields.values().iterator();
        var textIterator = textFields.values().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return binaryIterator.hasNext() || textIterator.hasNext();
            }

            @Override
            public ProtobufUnknownField<?> next() {
                return binaryIterator.hasNext() ? binaryIterator.next() : textIterator.next();
            }
        };
    }

    /**
     * Performs the given action for each binary field, ordered by index, and then for each text
     * field, in insertion order.
     *
     * <p>Overrides {@link Iterable#forEach(Consumer)} to avoid allocating an iterator.
     *
     * @param action the consumer to apply to each field
     * @throws NullPointerException if {@code action} is {@code null}
     */
    @Override
    public void forEach(Consumer<? super ProtobufUnknownField<?>> action) {
        Objects.requireNonNull(action, "action cannot be null");
        binaryFields.values().forEach(action);
        textFields.values().forEach(action);
    }

    /**
     * Appends a value to the binary field with the given index, creating the field if it does
     * not exist yet.
     *
     * @param fieldIndex the field index encountered on the wire
     * @param value the value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public void add(int fieldIndex, ProtobufUnknownValue.Binary value) {
        Objects.requireNonNull(value, "value cannot be null");
        binaryFields.computeIfAbsent(fieldIndex, ProtobufUnknownField.Binary::new).add(value);
    }

    /**
     * Appends a value to the text field with the given name, creating the field if it does not
     * exist yet.
     *
     * @param fieldName the field name as it appeared in the text input
     * @param value the value
     * @throws NullPointerException if {@code fieldName} or {@code value} is {@code null}
     */
    public void add(String fieldName, ProtobufUnknownValue.Text value) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        textFields.computeIfAbsent(fieldName, ProtobufUnknownField.Text::new).add(value);
    }

    /**
     * Returns the binary field with the given index.
     *
     * @param fieldIndex the field index to look up
     * @return an {@link Optional} holding the field, or empty if it does not exist
     */
    public Optional<ProtobufUnknownField.Binary> get(int fieldIndex) {
        return Optional.ofNullable(binaryFields.get(fieldIndex));
    }

    /**
     * Returns the text field with the given name.
     *
     * @param fieldName the field name to look up
     * @return an {@link Optional} holding the field, or empty if it does not exist
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public Optional<ProtobufUnknownField.Text> get(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        return Optional.ofNullable(textFields.get(fieldName));
    }

    /**
     * Returns whether a binary field with the given index exists.
     *
     * @param fieldIndex the field index to look up
     * @return {@code true} if the field exists
     */
    public boolean containsKey(int fieldIndex) {
        return binaryFields.containsKey(fieldIndex);
    }

    /**
     * Returns whether a text field with the given name exists.
     *
     * @param fieldName the field name to look up
     * @return {@code true} if the field exists
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public boolean containsKey(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        return textFields.containsKey(fieldName);
    }

    /**
     * Removes the binary field with the given index, and all of its values, if it exists.
     *
     * @param fieldIndex the field index to remove
     * @return {@code true} if the field existed
     */
    public boolean remove(int fieldIndex) {
        return binaryFields.remove(fieldIndex) != null;
    }

    /**
     * Removes the text field with the given name, and all of its values, if it exists.
     *
     * @param fieldName the field name to remove
     * @return {@code true} if the field existed
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public boolean remove(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        return textFields.remove(fieldName) != null;
    }

    /**
     * Appends every value of the given unknown fields, as if each one was added in order.
     *
     * <p>This is how the unknown fields of two occurrences of the same message are merged,
     * like {@code UnknownFieldSet.Builder.mergeFrom} does in the official protobuf library: a
     * field that both instances hold keeps its values, followed by the ones of the other
     * instance, and a field that only the other instance holds is copied, so that the two
     * instances never share a field.
     *
     * @param other the unknown fields whose values are appended
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public void addAll(ProtobufUnknownFields other) {
        Objects.requireNonNull(other, "other cannot be null");
        for (var field : other.binaryFields.values()) {
            var existing = binaryFields.get(field.index());
            if (existing == null) {
                binaryFields.put(field.index(), new ProtobufUnknownField.Binary(field.index(), field));
            } else {
                existing.addAll(field);
            }
        }
        for (var field : other.textFields.values()) {
            var existing = textFields.get(field.name());
            if (existing == null) {
                textFields.put(field.name(), new ProtobufUnknownField.Text(field.name(), field));
            } else {
                existing.addAll(field);
            }
        }
    }

    /**
     * Returns the total wire size, in bytes, of every value of the binary fields, including
     * their tags.
     *
     * <p>This is the number of bytes written by {@link #encodeBinary(ProtobufBinaryWriter)}.
     * Each field keeps its own wire size up to date, so this method only adds them up.
     * The codegen {@code sizeOfBinary} path uses it to add the unknown fields to the
     * serialized size of the enclosing message. The text fields are not counted, as the
     * binary format does not write them.
     *
     * @return the wire size
     */
    public long wireSize() {
        var wireSize = 0L;
        for (var field : binaryFields.values()) {
            wireSize += field.wireSize();
        }
        return wireSize;
    }

    /**
     * Returns the size, in bytes, of the UTF-8 encoding of the text written by
     * {@link #encodeText(ProtobufTextWriter)} to a new text writer that writes a field on each
     * line, or every field on the same line.
     *
     * <p>The binary fields are not counted, as the text format does not write them. The size
     * of the text written to a writer that already wrote other fields can differ, as it depends
     * on the separators and on the indentation of the fields that precede it.
     *
     * @param singleLine whether the writer writes every field on the same line, like a writer
     *                   {@linkplain ProtobufTextWriter#setSingleLine(boolean) set to write a
     *                   single line}
     * @return the text size
     */
    public long textSize(boolean singleLine) {
        var accumulator = new ProtobufTextSizeAccumulator(singleLine, true);
        addTextSize(accumulator, 0);
        return accumulator.size();
    }

    /**
     * Adds the size of the lines written by {@link #encodeText(ProtobufTextWriter)} at the
     * given depth to the given accumulator.
     *
     * <p>A scalar is counted as it was read, and a string of bytes as its escaped bytes,
     * enclosed in double quotes. A message is counted as the line that opens it, its fields one
     * level deeper, and the line that closes it. The binary fields are not counted, as the text
     * format does not write them. The size does not depend on whether the characters that are
     * not ASCII are escaped, as unknown values are not {@code string} fields.
     *
     * @param accumulator the accumulator of the size of the text that encloses the fields
     * @param depth the number of messages that enclose the fields
     * @throws NullPointerException if {@code accumulator} is {@code null}
     */
    public void addTextSize(ProtobufTextSizeAccumulator accumulator, int depth) {
        for (var field : textFields.values()) {
            var nameSize = ProtobufTextSizeCalculator.getNameSize(field.name());
            for (var value : field) {
                switch (value) {
                    case ProtobufUnknownValue.Text.Scalar scalar -> accumulator.addLine(depth, nameSize + 2L + CharsetUtils.utf8Length(scalar.value()));
                    case ProtobufUnknownValue.Text.Bytes bytes -> accumulator.addLine(depth, nameSize + 4L + ProtobufTextSizeCalculator.getEscapedBytesSize(bytes.value()));
                    case ProtobufUnknownValue.Text.Message message -> {
                        accumulator.addMessageLines(depth, nameSize + 2L);
                        message.value().addTextSize(accumulator, depth + 1);
                    }
                }
            }
        }
    }

    /**
     * Writes every value of the binary fields to the given writer, ordered by index, and by
     * insertion order within a field.
     *
     * <p>The text fields are not written, as they have no index.
     *
     * @apiNote
     * This is the method the generated serializers use to write the unknown fields of a
     * message after its known fields, as it writes without allocating.
     *
     * @param writer the writer
     * @throws ProtobufSerializationException if the writer cannot write a value
     * @throws NullPointerException if {@code writer} is {@code null}
     */
    public void encodeBinary(ProtobufBinaryWriter<?> writer) throws ProtobufSerializationException {
        Objects.requireNonNull(writer, "writer cannot be null");
        for (var field : binaryFields.values()) {
            for (var value : field) {
                writer.writeUnknownField(field.index(), value);
            }
        }
    }

    /**
     * Writes every value of the text fields to the given writer, by the names of the fields,
     * in the order in which the fields were first added, and by insertion order within a field.
     *
     * <p>The binary fields are not written, as they have no name, so the text format could not
     * read them back: {@link ProtobufTextWriter#writeUnknownField(int, ProtobufUnknownValue.Binary)}
     * writes a binary field by its number, like the official protobuf library does, to inspect
     * it.
     *
     * @apiNote
     * This is the method the generated text serializers use to write the unknown fields of a
     * message after its known fields.
     *
     * @param writer the writer
     * @throws ProtobufSerializationException if the writer cannot write a value
     * @throws NullPointerException if {@code writer} is {@code null}
     * @see ProtobufTextWriter#writeUnknownField(String, ProtobufUnknownValue.Text)
     */
    public void encodeText(ProtobufTextWriter<?> writer) throws ProtobufSerializationException {
        Objects.requireNonNull(writer, "writer cannot be null");
        for (var field : textFields.values()) {
            for (var value : field) {
                writer.writeUnknownField(field.name(), value);
            }
        }
    }

    /**
     * Returns whether the given object holds equal fields in the same order.
     *
     * @param object the object to compare with
     * @return {@code true} if the object are equal unknown fields
     */
    @Override
    public boolean equals(Object object) {
        return object instanceof ProtobufUnknownFields that
                && binaryFields.equals(that.binaryFields)
                && textFields.equals(that.textFields);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return 31 * binaryFields.hashCode() + textFields.hashCode();
    }

    /**
     * Returns a string representation of the fields.
     *
     * @return a string representation of the fields
     */
    @Override
    public String toString() {
        return "ProtobufUnknownFields[binary=" + binaryFields.values() + ", text=" + textFields.values() + "]";
    }
}
