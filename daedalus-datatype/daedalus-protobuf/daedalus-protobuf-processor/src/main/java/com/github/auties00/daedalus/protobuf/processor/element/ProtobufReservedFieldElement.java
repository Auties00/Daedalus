package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufReservedRange;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

public sealed interface ProtobufReservedFieldElement {
    static SequencedSet<? extends ProtobufReservedFieldElement> of(ProtobufMessage messageAnnotation) {
        Objects.requireNonNull(messageAnnotation, "Annotation cannot be null");
        return collect(
                messageAnnotation.reservedIndexes(),
                messageAnnotation.reservedNames(),
                messageAnnotation.reservedRanges()
        );
    }

    static SequencedSet<? extends ProtobufReservedFieldElement> of(ProtobufEnum enumAnnotation) {
        Objects.requireNonNull(enumAnnotation, "Annotation cannot be null");
        return collect(
                enumAnnotation.reservedIndexes(),
                enumAnnotation.reservedNames(),
                enumAnnotation.reservedRanges()
        );
    }

    private static SequencedSet<ProtobufReservedFieldElement> collect(int[] groupAnnotation, String[] groupAnnotation1, ProtobufReservedRange[] groupAnnotation2) {
        var reserved = new LinkedHashSet<ProtobufReservedFieldElement>();
        for (var reservedIndex : groupAnnotation) {
            reserved.add(new Index.Value(reservedIndex));
        }
        for (var reservedName : groupAnnotation1) {
            reserved.add(new Name(reservedName));
        }
        for (var reservedRange : groupAnnotation2) {
            reserved.add(new Index.Range(reservedRange.min(), reservedRange.max()));
        }
        return Collections.unmodifiableSequencedSet(reserved);
    }

    record Name(String name) implements ProtobufReservedFieldElement {
        public boolean allows(String name) {
            return !this.name.equals(name);
        }
    }

    sealed interface Index extends ProtobufReservedFieldElement {
        boolean allows(long index);

        record Range(long min, long max) implements Index {
            @Override
            public boolean allows(long index) {
                return index < min || index > max;
            }
        }

        record Value(long value) implements Index {
            @Override
            public boolean allows(long index) {
                return index != value;
            }
        }
    }
}
