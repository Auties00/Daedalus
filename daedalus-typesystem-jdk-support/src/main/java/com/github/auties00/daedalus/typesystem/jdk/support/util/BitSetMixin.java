package com.github.auties00.daedalus.typesystem.jdk.support.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.util.BitSet;

/**
 * A {@link TypeMixin} that provides a default value, a serializer and a deserializer
 * for {@link BitSet}.
 *
 * <p>A bit set is encoded as the {@code byte[]} returned by {@link BitSet#toByteArray()},
 * which stores the bits in little endian order.
 *
 * <p>Every serializer returns {@code null} for a {@code null} value, and every
 * deserializer returns {@code null} for a {@code null} encoded value.
 */
@SuppressWarnings("unused")
@TypeMixin(scope = TypeMixin.Scope.GLOBAL)
public final class BitSetMixin {
    private BitSetMixin() {
        throw new UnsupportedOperationException("BitSetMixin is a mixin and cannot be instantiated");
    }

    /**
     * Returns a new empty bit set.
     *
     * @return the bit set
     */
    @TypeDefaultValue
    public static BitSet newBitSet() {
        return new BitSet();
    }

    /**
     * Returns the bits of the given bit set, in little endian order.
     *
     * @param value the bit set, or {@code null}
     * @return the bits, or {@code null} if {@code value} is {@code null}
     */
    @TypeSerializer
    public static byte[] toBytes(BitSet value) {
        return value == null ? null : value.toByteArray();
    }

    /**
     * Returns the bit set with the given bits, in little endian order.
     *
     * @param value the bits, or {@code null}
     * @return the bit set, or {@code null} if {@code value} is {@code null}
     */
    @TypeDeserializer
    public static BitSet ofBitSet(byte[] value) {
        return value == null ? null : BitSet.valueOf(value);
    }
}
