package it.auties.protobuf.io.writer.binary;

import it.auties.protobuf.exception.ProtobufSerializationException;
import it.auties.protobuf.io.ProtobufDataType;
import it.auties.protobuf.io.writer.ProtobufBinaryWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public final class ProtobufBinaryStreamWriter extends ProtobufBinaryWriter<OutputStream> {
    private static final int BUFFER_LENGTH = 8192;

    private final OutputStream outputStream;
    private final byte[] buffer;

    public ProtobufBinaryStreamWriter(OutputStream outputStream) {
        this(outputStream, BUFFER_LENGTH);
    }

    public ProtobufBinaryStreamWriter(OutputStream outputStream, int tempBufferLength) {
        this.outputStream = outputStream;
        this.buffer = new byte[tempBufferLength];
    }

    @Override
    public void writeRawByte(byte entry) {
        try {
            outputStream.write(entry);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawBytes(byte[] entry, int offset, int length) {
        try {
            outputStream.write(entry, offset, length);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawBuffer(ByteBuffer entry) {
        try {
            if (entry.hasArray()) {
                outputStream.write(entry.array(), entry.arrayOffset() + entry.position(), entry.remaining());
                return;
            }

            while (entry.hasRemaining()) {
                var readable = Math.min(entry.remaining(), buffer.length);
                entry.put(buffer, entry.position(), readable);
                outputStream.write(buffer, 0, readable);
            }
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawMemorySegment(MemorySegment entry) {
        try {
            var heapBase = entry.heapBase();
            if(heapBase.isPresent() && heapBase.get() instanceof byte[] array) {
                writeRawBytes(array);
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
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawFixedInt32(int entry) {
        try {
            ARRAY_AS_INT32_LE.set(buffer, 0, entry);
            outputStream.write(buffer, 0, Integer.BYTES);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawFixedInt64(long entry) {
        try {
            ARRAY_AS_INT64_LE.set(buffer, 0, entry);
            outputStream.write(buffer, 0, Long.BYTES);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawFloat(float entry) {
        try {
            ARRAY_AS_FLOAT_LE.set(buffer, 0, entry);
            outputStream.write(buffer, 0, Float.BYTES);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawDouble(double entry) {
        try {
            ARRAY_AS_DOUBLE_LE.set(buffer, 0, entry);
            outputStream.write(buffer, 0, Double.BYTES);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawVarInt32(int entry) {
        try {
            var len = writeRawVarInt32(buffer, 0, entry);
            outputStream.write(buffer, 0, len);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawVarInt64(long entry) {
        try {
            var len = writeRawVarInt64(buffer, 0, entry);
            outputStream.write(buffer, 0, len);
        } catch (IOException e) {
            throw new ProtobufSerializationException("Cannot write to output stream", e);
        }
    }

    @Override
    public void writeRawPackedFixedInt32(int[] input) {
        for (var value : input) {
            writeRawFixedInt32(value);
        }
    }

    @Override
    public void writeRawPackedFixedInt64(long[] input) {
        for (var value : input) {
            writeRawFixedInt64(value);
        }
    }

    @Override
    public void writeRawPackedFloat(float[] input) {
        for (var value : input) {
            writeRawFloat(value);
        }
    }

    @Override
    public void writeRawPackedDouble(double[] input) {
        for (var value : input) {
            writeRawDouble(value);
        }
    }

    @Override
    public void writeRawPackedVarInt32(int[] input) {
        for (var value : input) {
            writeRawVarInt32(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt32(int[] input) {
        for (var value : input) {
            writeRawVarInt32((value << 1) ^ (value >> 31));
        }
    }

    @Override
    public void writeRawPackedVarInt64(long[] input) {
        for (var value : input) {
            writeRawVarInt64(value);
        }
    }

    @Override
    public void writeRawPackedZigZagVarInt64(long[] input) {
        for (var value : input) {
            writeRawVarInt64((value << 1) ^ (value >> 63));
        }
    }

    @Override
    public void writeRawPackedBool(boolean[] input) {
        for (var value : input) {
            writeRawByte(value ? (byte) 1 : (byte) 0);
        }
    }

    @Override
    public OutputStream toOutput() {
        return outputStream;
    }

    @Override
    public ProtobufDataType rawDataTypePreference() {
        return ProtobufDataType.BYTE_ARRAY;
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
