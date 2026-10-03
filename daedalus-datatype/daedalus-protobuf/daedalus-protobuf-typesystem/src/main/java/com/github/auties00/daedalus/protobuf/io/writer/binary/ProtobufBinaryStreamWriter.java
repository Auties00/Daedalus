package com.github.auties00.daedalus.protobuf.io.writer.binary;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public final class ProtobufBinaryStreamWriter extends ProtobufBinaryWriter<OutputStream> {
    private static final int BUFFER_LENGTH = 8192;
    private static final int MAX_PRIMITIVE_LENGTH = 16;

    private final OutputStream outputStream;
    private final byte[] buffer;
    // Holds a single encoded primitive: the fast varint encoders write up to 16 bytes, which could
    // exceed the length of the user provided buffer
    private final byte[] scratch;

    public ProtobufBinaryStreamWriter(OutputStream outputStream) {
        this(outputStream, BUFFER_LENGTH);
    }

    public ProtobufBinaryStreamWriter(OutputStream outputStream, int tempBufferLength) {
        this.outputStream = outputStream;
        this.buffer = new byte[Math.max(tempBufferLength, 1)];
        this.scratch = new byte[MAX_PRIMITIVE_LENGTH];
    }

    @Override
    public void writeRawByte(byte entry) throws ProtobufSerializationException {
        try {
            outputStream.write(entry);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) throws ProtobufSerializationException {
        try {
            outputStream.write(entry, offset, length);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) throws ProtobufSerializationException {
        try {
            if (entry.hasArray()) {
                outputStream.write(entry.array(), entry.arrayOffset() + entry.position(), entry.remaining());
                return;
            }

            var position = entry.position();
            var limit = entry.limit();
            while (position < limit) {
                var readable = Math.min(limit - position, buffer.length);
                entry.get(position, buffer, 0, readable);
                outputStream.write(buffer, 0, readable);
                position += readable;
            }
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) throws ProtobufSerializationException {
        try {
            var heapBase = entry.heapBase();
            if(heapBase.isPresent() && heapBase.get() instanceof byte[] array) {
                // The address of a heap segment is its offset in the backing array
                outputStream.write(array, (int) entry.address(), (int) entry.byteSize());
                return;
            }

            var offset = 0L;
            var limit = entry.byteSize();
            while (offset < limit) {
                var readable = (int) Math.min(limit - offset, buffer.length);
                MemorySegment.copy(
                        entry,
                        ValueLayout.JAVA_BYTE,
                        offset,
                        buffer,
                        0,
                        readable
                );
                outputStream.write(buffer, 0, readable);
                offset += readable;
            }
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawFixedInt32(int entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeIntLE(scratch, 0, entry);
            outputStream.write(scratch, 0, Integer.BYTES);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawFixedInt64(long entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeLongLE(scratch, 0, entry);
            outputStream.write(scratch, 0, Long.BYTES);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawFloat(float entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeFloatLE(scratch, 0, entry);
            outputStream.write(scratch, 0, Float.BYTES);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawDouble(double entry) throws ProtobufSerializationException {
        try {
            BinaryUtils.writeDoubleLE(scratch, 0, entry);
            outputStream.write(scratch, 0, Double.BYTES);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawVarInt32(int entry) throws ProtobufSerializationException {
        try {
            var len = writeRawVarInt32(scratch, 0, entry);
            outputStream.write(scratch, 0, len);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawVarInt64(long entry) throws ProtobufSerializationException {
        try {
            var len = writeRawVarInt64(scratch, 0, entry);
            outputStream.write(scratch, 0, len);
        } catch (IOException e) {
            throw ProtobufSerializationException.outputStreamFailure(e);
        }
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFixedInt32(value);
        }
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFixedInt64(value);
        }
    }

    @Override
    public void writeRawPackedFloat(float[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawFloat(value);
        }
    }

    @Override
    public void writeRawPackedDouble(double[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawDouble(value);
        }
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt32(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawZigZagVarInt32(value);
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt64(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawVarInt64((value << 1) ^ (value >> 63));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) throws ProtobufSerializationException {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
    }

    @Override
    public OutputStream toOutput() {
        return outputStream;
    }

    @Override
    public BinaryDataType rawDataTypePreference() {
        return BinaryDataType.BYTE_ARRAY;
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
