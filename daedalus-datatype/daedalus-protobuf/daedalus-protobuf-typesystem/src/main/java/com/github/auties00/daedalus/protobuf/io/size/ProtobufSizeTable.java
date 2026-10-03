package com.github.auties00.daedalus.protobuf.io.size;

import com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec;

/**
 * A table of the sizes of the length delimited parts of a message, computed once while the
 * message is measured and read back while it is written.
 *
 * <p>The binary format precedes every nested message, every map entry and every packed
 * field by the length of its payload, so a message must be measured before it is written.
 * Without this table, writing a message would measure each nested message again for every
 * message that encloses it. Instead, {@link ProtobufMessageSpec#sizeOfBinary(Object, ProtobufSizeTable)}
 * reserves a slot for each length delimited part before measuring it, and fills it once the
 * part is measured, so that the slots are ordered like the parts are written, and
 * {@link ProtobufMessageSpec#encodeBinary(Object, com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter, ProtobufSizeTable)}
 * reads them back in that order.
 *
 * <p>A table holds exactly the number of slots computed by
 * {@link ProtobufMessageSpec#sizeTableSlots(Object)}, so it never grows. The
 * {@linkplain #discarding() discarding table} has no slots: it is used to measure a message
 * that is not written afterwards.
 *
 * <p>This class is not thread safe: a table belongs to the encoding of a single message.
 */
public final class ProtobufSizeTable {
    /**
     * The table that discards every size, as it has no slots.
     */
    private static final ProtobufSizeTable DISCARDING = new ProtobufSizeTable(null);

    /**
     * The sizes, or {@code null} for the discarding table.
     */
    private final int[] slots;

    /**
     * The index of the next slot to reserve.
     */
    private int reserveIndex;

    /**
     * The index of the next slot to read.
     */
    private int readIndex;

    /**
     * Constructs a new table with the given slots.
     *
     * @param slots the slots, or {@code null} for the discarding table
     */
    private ProtobufSizeTable(int[] slots) {
        this.slots = slots;
    }

    /**
     * Returns a new table with the given number of slots, or the discarding table if it is
     * {@code 0}, as a table without slots is never read.
     *
     * @param slots the number of slots
     * @return the table
     * @throws NegativeArraySizeException if {@code slots} is negative
     */
    public static ProtobufSizeTable of(int slots) {
        return slots == 0 ? DISCARDING : new ProtobufSizeTable(new int[slots]);
    }

    /**
     * Returns the table that discards every size, which is used to measure a message that is
     * not written afterwards.
     *
     * <p>The discarding table is shared and immutable, so it is safe to use from any thread.
     *
     * @return the discarding table
     */
    public static ProtobufSizeTable discarding() {
        return DISCARDING;
    }

    /**
     * Reserves the next slot, before the length delimited part whose size it will hold is
     * measured.
     *
     * @return the index of the reserved slot, or {@code -1} if this is the discarding table
     * @throws ArrayIndexOutOfBoundsException if every slot of this table is already reserved
     */
    public int reserve() {
        if (slots == null) {
            return -1;
        }

        if (reserveIndex == slots.length) {
            throw new ArrayIndexOutOfBoundsException("Cannot reserve a slot, as all the " + slots.length + " slots of the table are reserved");
        }

        return reserveIndex++;
    }

    /**
     * Stores the given size in the given slot, once the length delimited part whose size it
     * holds is measured.
     *
     * @param slot the index returned by {@link #reserve()}
     * @param size the size of the part
     * @return {@code size}
     */
    public int set(int slot, int size) {
        if (slot >= 0) {
            slots[slot] = size;
        }
        return size;
    }

    /**
     * Returns the size held by the next slot, while the length delimited parts are written in
     * the same order as their slots were reserved.
     *
     * @return the size held by the next slot
     * @throws ArrayIndexOutOfBoundsException if every slot of this table was already read, or
     *         if this is the discarding table
     */
    public int next() {
        if (slots == null || readIndex == slots.length) {
            throw new ArrayIndexOutOfBoundsException("Cannot read a slot, as all the " + (slots == null ? 0 : slots.length) + " slots of the table were read");
        }

        return slots[readIndex++];
    }
}
