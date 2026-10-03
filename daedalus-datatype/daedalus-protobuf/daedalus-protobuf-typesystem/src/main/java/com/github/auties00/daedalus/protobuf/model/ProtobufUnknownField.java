package com.github.auties00.daedalus.protobuf.model;

import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.SequencedCollection;

/**
 * A field that a message does not declare, which is the sequence of every value it had in the
 * input, in the order in which they were read.
 *
 * <p>A field may appear more than once in the input, for example because it is repeated in
 * the schema that wrote it, and like the {@code UnknownFieldSet} of the official protobuf
 * library, every value is kept. A field read from the binary format is identified by its
 * index, and one read from the text format by its name.
 *
 * <p>A field is a mutable {@link SequencedCollection} of its values, which cannot hold
 * {@code null}. Its {@link #reversed()} view cannot be modified. Two fields are equal if they
 * have the same identifier and equal values in the same order.
 *
 * <p>The implementations are not thread-safe.
 *
 * @param <V> the type of the values, which is {@link ProtobufUnknownValue.Binary} for a field
 *            read from the binary format and {@link ProtobufUnknownValue.Text} for a field
 *            read from the text format
 */
public sealed interface ProtobufUnknownField<V extends ProtobufUnknownValue> extends SequencedCollection<V>
        permits ProtobufUnknownField.Binary, ProtobufUnknownField.Text {
    /**
     * An unknown field read from the binary format, identified by its index.
     *
     * <p>The field keeps the wire size of its values, including their tags, up to date as
     * they are added and removed, so that the size of a message can be computed without
     * walking its unknown values.
     */
    final class Binary extends AbstractCollection<ProtobufUnknownValue.Binary> implements ProtobufUnknownField<ProtobufUnknownValue.Binary> {
        /**
         * The index of the field.
         */
        private final int index;

        /**
         * The values of the field, in order.
         */
        private final ArrayList<ProtobufUnknownValue.Binary> values;

        /**
         * The total wire size, in bytes, of the values, including their tags.
         */
        private long wireSize;

        /**
         * Creates a field without values.
         *
         * @param index the index of the field
         */
        public Binary(int index) {
            this.index = index;
            this.values = new ArrayList<>(1);
        }

        /**
         * Creates a field with the given values, in order.
         *
         * @param index the index of the field
         * @param values the values of the field
         * @throws NullPointerException if {@code values} or any of its elements is {@code null}
         */
        public Binary(int index, Collection<? extends ProtobufUnknownValue.Binary> values) {
            this.index = index;
            this.values = new ArrayList<>(Math.max(1, values.size()));
            addAll(values);
        }

        /**
         * Returns the index of this field.
         *
         * @return the index
         */
        public int index() {
            return index;
        }

        /**
         * Returns the total wire size, in bytes, of the values of this field, including the
         * tag of each value.
         *
         * @return the wire size
         */
        public long wireSize() {
            return wireSize;
        }

        /**
         * Returns the wire size of the given value of this field, including its tag.
         *
         * @param value the value
         * @return the wire size
         */
        private long sizeOf(ProtobufUnknownValue.Binary value) {
            return ProtobufBinarySizeCalculator.getUnknownFieldSize(index, value);
        }

        /**
         * Returns the number of values of this field.
         *
         * @return the number of values
         */
        @Override
        public int size() {
            return values.size();
        }

        /**
         * Appends a value to this field.
         *
         * @param value the value
         * @return {@code true}
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public boolean add(ProtobufUnknownValue.Binary value) {
            Objects.requireNonNull(value, "value cannot be null");
            values.add(value);
            wireSize += sizeOf(value);
            return true;
        }

        /**
         * Inserts a value before the other values of this field.
         *
         * @param value the value
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void addFirst(ProtobufUnknownValue.Binary value) {
            Objects.requireNonNull(value, "value cannot be null");
            values.addFirst(value);
            wireSize += sizeOf(value);
        }

        /**
         * Appends a value to this field.
         *
         * @param value the value
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void addLast(ProtobufUnknownValue.Binary value) {
            add(value);
        }

        /**
         * Returns the first value of this field.
         *
         * @return the first value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Binary getFirst() {
            return values.getFirst();
        }

        /**
         * Returns the last value of this field.
         *
         * @return the last value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Binary getLast() {
            return values.getLast();
        }

        /**
         * Removes and returns the first value of this field.
         *
         * @return the first value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Binary removeFirst() {
            var value = values.removeFirst();
            wireSize -= sizeOf(value);
            return value;
        }

        /**
         * Removes and returns the last value of this field.
         *
         * @return the last value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Binary removeLast() {
            var value = values.removeLast();
            wireSize -= sizeOf(value);
            return value;
        }

        /**
         * Removes every value of this field.
         */
        @Override
        public void clear() {
            values.clear();
            wireSize = 0L;
        }

        /**
         * Returns an iterator over the values of this field, in order, whose removals update
         * the wire size of the field.
         *
         * @return an iterator over the values
         */
        @Override
        public Iterator<ProtobufUnknownValue.Binary> iterator() {
            var delegate = values.iterator();
            return new Iterator<>() {
                /**
                 * The value returned by the last call to {@link #next()}.
                 */
                private ProtobufUnknownValue.Binary last;

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public ProtobufUnknownValue.Binary next() {
                    return last = delegate.next();
                }

                /**
                 * Removes the value returned by the last call to {@link #next()}, and
                 * subtracts its wire size from the one of the field.
                 */
                @Override
                public void remove() {
                    delegate.remove();
                    wireSize -= sizeOf(last);
                }
            };
        }

        /**
         * Returns an unmodifiable reverse-ordered view of the values of this field.
         *
         * @return the values of this field in reverse order
         */
        @Override
        public SequencedCollection<ProtobufUnknownValue.Binary> reversed() {
            return Collections.unmodifiableSequencedCollection(values).reversed();
        }

        /**
         * Returns whether the given object is a binary field with the same index and equal
         * values in the same order.
         *
         * @param object the object to compare with
         * @return {@code true} if the object is an equal field
         */
        @Override
        public boolean equals(Object object) {
            return object instanceof Binary that
                    && index == that.index
                    && values.equals(that.values);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code of this field
         */
        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(index) + values.hashCode();
        }

        /**
         * Returns a string representation of this field, with its index and values.
         *
         * @return a string representation of this field
         */
        @Override
        public String toString() {
            return "ProtobufUnknownField.Binary[index=" + index + ", values=" + values + "]";
        }
    }

    /**
     * An unknown field read from the text format, identified by its name.
     */
    final class Text extends AbstractCollection<ProtobufUnknownValue.Text> implements ProtobufUnknownField<ProtobufUnknownValue.Text> {
        /**
         * The name of the field.
         */
        private final String name;

        /**
         * The values of the field, in order.
         */
        private final ArrayList<ProtobufUnknownValue.Text> values;

        /**
         * Creates a field without values.
         *
         * @param name the name of the field
         * @throws NullPointerException if {@code name} is {@code null}
         */
        public Text(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.values = new ArrayList<>(1);
        }

        /**
         * Creates a field with the given values, in order.
         *
         * @param name the name of the field
         * @param values the values of the field
         * @throws NullPointerException if {@code name}, {@code values} or any of its elements
         *                              is {@code null}
         */
        public Text(String name, Collection<? extends ProtobufUnknownValue.Text> values) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.values = new ArrayList<>(Math.max(1, values.size()));
            addAll(values);
        }

        /**
         * Returns the name of this field.
         *
         * @return the name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the number of values of this field.
         *
         * @return the number of values
         */
        @Override
        public int size() {
            return values.size();
        }

        /**
         * Appends a value to this field.
         *
         * @param value the value
         * @return {@code true}
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public boolean add(ProtobufUnknownValue.Text value) {
            values.add(Objects.requireNonNull(value, "value cannot be null"));
            return true;
        }

        /**
         * Inserts a value before the other values of this field.
         *
         * @param value the value
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void addFirst(ProtobufUnknownValue.Text value) {
            values.addFirst(Objects.requireNonNull(value, "value cannot be null"));
        }

        /**
         * Appends a value to this field.
         *
         * @param value the value
         * @throws NullPointerException if {@code value} is {@code null}
         */
        @Override
        public void addLast(ProtobufUnknownValue.Text value) {
            add(value);
        }

        /**
         * Returns the first value of this field.
         *
         * @return the first value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Text getFirst() {
            return values.getFirst();
        }

        /**
         * Returns the last value of this field.
         *
         * @return the last value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Text getLast() {
            return values.getLast();
        }

        /**
         * Removes and returns the first value of this field.
         *
         * @return the first value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Text removeFirst() {
            return values.removeFirst();
        }

        /**
         * Removes and returns the last value of this field.
         *
         * @return the last value
         * @throws java.util.NoSuchElementException if this field has no value
         */
        @Override
        public ProtobufUnknownValue.Text removeLast() {
            return values.removeLast();
        }

        /**
         * Removes every value of this field.
         */
        @Override
        public void clear() {
            values.clear();
        }

        /**
         * Returns an iterator over the values of this field, in order.
         *
         * @return an iterator over the values
         */
        @Override
        public Iterator<ProtobufUnknownValue.Text> iterator() {
            return values.iterator();
        }

        /**
         * Returns an unmodifiable reverse-ordered view of the values of this field.
         *
         * @return the values of this field in reverse order
         */
        @Override
        public SequencedCollection<ProtobufUnknownValue.Text> reversed() {
            return Collections.unmodifiableSequencedCollection(values).reversed();
        }

        /**
         * Returns whether the given object is a text field with the same name and equal
         * values in the same order.
         *
         * @param object the object to compare with
         * @return {@code true} if the object is an equal field
         */
        @Override
        public boolean equals(Object object) {
            return object instanceof Text that
                    && name.equals(that.name)
                    && values.equals(that.values);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code of this field
         */
        @Override
        public int hashCode() {
            return 31 * name.hashCode() + values.hashCode();
        }

        /**
         * Returns a string representation of this field, with its name and values.
         *
         * @return a string representation of this field
         */
        @Override
        public String toString() {
            return "ProtobufUnknownField.Text[name=" + name + ", values=" + values + "]";
        }
    }
}
