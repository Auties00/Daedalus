package com.github.auties00.daedalus.protobuf.io.writer.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * A text writer that writes the UTF-8 encoding of the text into a {@link MemorySegment},
 * starting at its first byte.
 *
 * <p>The text cannot exceed the size of the segment: a write that does not fit throws a
 * {@link ProtobufSerializationException}.
 */
public final class ProtobufTextMemorySegmentWriter extends ProtobufTextByteWriter<MemorySegment> {
    /**
     * The segment the text is written into.
     */
    private final MemorySegment segment;

    /**
     * The offset of the next byte to write.
     */
    private long position;

    /**
     * Constructs a new writer for the given segment.
     *
     * @param segment the segment
     * @throws NullPointerException if {@code segment} is {@code null}
     */
    public ProtobufTextMemorySegmentWriter(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment cannot be null");
        this.segment = segment;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufSerializationException if the segment is full
     */
    @Override
    void writeByte(byte value) throws ProtobufSerializationException {
        if (position >= segment.byteSize()) {
            throw ProtobufSerializationException.outputFull(segment.byteSize());
        }
        segment.set(ValueLayout.JAVA_BYTE, position++, value);
    }

    /**
     * Returns the slice of the segment that holds the text that was written.
     *
     * @return a segment that holds the UTF-8 encoding of the text
     */
    @Override
    public MemorySegment toOutput() {
        return segment.asSlice(0, position);
    }

    /**
     * Does nothing, as the segment is owned by the caller.
     */
    @Override
    public void close() {

    }
}
