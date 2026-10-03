package com.github.auties00.daedalus.typesystem.model;

/**
 * A kind of storage that holds binary data, such as a {@code byte[]}.
 *
 * <p>A reader or a writer of a binary format reports the kind of storage it reads from or
 * writes to, so that the binary data it reads, like a value whose type is not known, can be
 * kept in the same kind of storage, which can be filled without converting the data.
 */
public enum BinaryDataType {
    /**
     * Binary data held by a {@code byte[]}.
     */
    BYTE_ARRAY,

    /**
     * Binary data held by a {@link java.nio.ByteBuffer}, which can be a heap or a direct buffer.
     */
    BYTE_BUFFER,

    /**
     * Binary data held by a {@link java.lang.foreign.MemorySegment}, which can be a heap or a
     * native segment.
     */
    MEMORY_SEGMENT
}
