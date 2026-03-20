package it.auties.protobuf.io.writer;

import it.auties.protobuf.exception.ProtobufSerializationException;
import it.auties.protobuf.io.ProtobufDataType;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

final class ProtobufMemorySegmentWriter extends ProtobufWriter<MemorySegment> {
    private final MemorySegment memorySegment;
    private long position;

    ProtobufMemorySegmentWriter(MemorySegment memorySegment) {
        this.memorySegment = memorySegment;
    }

    @Override
    public void writeRawByte(byte entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_BYTE, position, entry);
            position++;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) {
        try {
            MemorySegment.copy(
                    entry,
                    offset,
                    memorySegment,
                    ValueLayout.JAVA_BYTE,
                    position,
                    length
            );
            position += length;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) {
        writeRawMemorySegment(MemorySegment.ofBuffer(entry));
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) {
        try {
            var length = entry.byteSize();
            MemorySegment.copy(
                    entry,
                    0,
                    memorySegment,
                    position,
                    length
            );
            position += length;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt32(int entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_INT_UNALIGNED, position, entry);
            position += Integer.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFixedInt64(long entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_LONG_UNALIGNED, position, entry);
            position += Long.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawFloat(float entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, position, entry);
            position += Float.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawDouble(double entry) {
        try {
            memorySegment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, position, entry);
            position += Double.BYTES;
        } catch (IndexOutOfBoundsException _) {
            throw ProtobufSerializationException.underflow();
        }
    }

    @Override
    public void writeRawVarInt32(int entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawVarInt64(long entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedFloat(float[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedDouble(double[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MemorySegment toOutput() {
        return memorySegment;
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.MEMORY_SEGMENT;
    }

    @Override
    public void close() {

    }
}
