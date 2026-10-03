package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

/**
 * A reserved field declaration extracted from a
 * {@link ProtobufMessage @ProtobufMessage} or {@link ProtobufEnum @ProtobufEnum} annotation.
 *
 * <p>Reserved fields prevent specific field names or numeric indexes from being
 * reused in a protobuf message or enum definition. This sealed interface has two
 * categories of permitted subtypes:
 * <ul>
 * <li>{@link Name} for fields reserved by name
 * <li>{@link Index} for fields reserved by numeric index, which is itself sealed
 *     into {@link Index.Range} and {@link Index.Value}
 * </ul>
 */
public sealed interface ProtobufReservedFieldElement {
    /**
     * Extracts the reserved field declarations from a {@link ProtobufMessage} annotation.
     *
     * <p>This factory method collects reserved indexes, reserved names, and reserved
     * ranges from the annotation and returns them as an unmodifiable set.
     *
     * @param messageAnnotation the {@code @ProtobufMessage} annotation to extract from
     * @return an unmodifiable set of reserved field elements
     * @throws NullPointerException if {@code messageAnnotation} is {@code null}
     */
    static SequencedSet<? extends ProtobufReservedFieldElement> of(ProtobufMessage messageAnnotation) {
        Objects.requireNonNull(messageAnnotation, "Annotation cannot be null");
        return collect(
                messageAnnotation.reservedIndexes(),
                messageAnnotation.reservedNames(),
                messageAnnotation.reservedRanges()
        );
    }

    /**
     * Extracts the reserved field declarations from a {@link ProtobufEnum} annotation.
     *
     * <p>This factory method collects reserved indexes, reserved names, and reserved
     * ranges from the annotation and returns them as an unmodifiable set.
     *
     * @param enumAnnotation the {@code @ProtobufEnum} annotation to extract from
     * @return an unmodifiable set of reserved field elements
     * @throws NullPointerException if {@code enumAnnotation} is {@code null}
     */
    static SequencedSet<? extends ProtobufReservedFieldElement> of(ProtobufEnum enumAnnotation) {
        Objects.requireNonNull(enumAnnotation, "Annotation cannot be null");
        return collect(
                enumAnnotation.reservedIndexes(),
                enumAnnotation.reservedNames(),
                enumAnnotation.reservedRanges()
        );
    }

    private static SequencedSet<ProtobufReservedFieldElement> collect(int[] indexes, String[] names, ProtobufReservedRange[] ranges) {
        var reserved = new LinkedHashSet<ProtobufReservedFieldElement>();
        for (var reservedIndex : indexes) {
            reserved.add(new Index.Value(reservedIndex));
        }
        for (var reservedName : names) {
            reserved.add(new Name(reservedName));
        }
        for (var reservedRange : ranges) {
            reserved.add(new Index.Range(reservedRange.min(), reservedRange.max()));
        }
        return Collections.unmodifiableSequencedSet(reserved);
    }

    /**
     * A reserved field declaration that reserves a field by name.
     *
     * @param name the reserved field name
     */
    record Name(String name) implements ProtobufReservedFieldElement {
        /**
         * Returns whether the given field name is allowed by this reservation.
         *
         * <p>A name is allowed if it does not match the reserved name.
         *
         * @param name the field name to check
         * @return {@code true} if the name is not reserved, {@code false} otherwise
         */
        public boolean allows(String name) {
            return !this.name.equals(name);
        }
    }

    /**
     * A reserved field declaration that reserves a field by numeric index.
     *
     * <p>This sealed interface has two subtypes: {@link Range} for a contiguous
     * range of reserved indexes, and {@link Value} for a single reserved index.
     */
    sealed interface Index extends ProtobufReservedFieldElement {
        /**
         * Returns whether the given field index is allowed by this reservation.
         *
         * @param index the field index to check
         * @return {@code true} if the index is not reserved, {@code false} otherwise
         */
        boolean allows(int index);

        /**
         * A reserved index range defined by an inclusive minimum and maximum bound.
         *
         * @param min the minimum reserved index, inclusive
         * @param max the maximum reserved index, inclusive
         */
        record Range(int min, int max) implements Index {
            @Override
            public boolean allows(int index) {
                return index < min || index > max;
            }
        }

        /**
         * A single reserved index value.
         *
         * @param value the reserved index
         */
        record Value(int value) implements Index {
            @Override
            public boolean allows(int index) {
                return index != value;
            }
        }
    }
}
