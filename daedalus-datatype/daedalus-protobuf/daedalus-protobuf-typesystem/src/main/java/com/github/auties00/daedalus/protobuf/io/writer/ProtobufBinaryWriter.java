package com.github.auties00.daedalus.protobuf.io.writer;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.ProtobufIODataType;
import com.github.auties00.daedalus.protobuf.io.calculator.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryByteArrayWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryByteBufferWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryMemorySegmentWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryStreamWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * An abstract writer for Protocol Buffer binary wire format.
 * <p>
 * This class provides a comprehensive API for serializing Protocol Buffer messages to various
 * binary output destinations, including byte arrays, ByteBuffers, MemorySegments, and OutputStreams.
 *
 * <p>This class performs no checks on whether the data it's serializing is correct(ex. you can write an illegal field index/wire type).
 * This choice was made because the annotation processor is expected to perform these checks at compile time.
 * If you are using this class manually for any particular reason, keep this in mind.
 *
 * @param <OUTPUT> the type of output this stream produces (byte[], ByteBuffer, OutputStream, ...)
 * @see ProtobufBinaryReader
 */
public abstract non-sealed class ProtobufBinaryWriter<OUTPUT> implements ProtobufWriter<OUTPUT> {
    protected static final VarHandle ARRAY_AS_INT16_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_INT32_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_INT64_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_FLOAT_LE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle ARRAY_AS_DOUBLE_LE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    protected static final VarHandle BUFFER_AS_INT16_LE = MethodHandles.byteBufferViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_INT32_LE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_INT64_LE = MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_FLOAT_LE = MethodHandles.byteBufferViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    protected static final VarHandle BUFFER_AS_DOUBLE_LE = MethodHandles.byteBufferViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    protected static final ValueLayout.OfShort INT16_LE_LAYOUT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    protected static final ValueLayout.OfInt INT32_LE_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    protected static final ValueLayout.OfLong INT64_LE_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    protected static final ValueLayout.OfFloat FLOAT_LE_LAYOUT = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    protected static final ValueLayout.OfDouble DOUBLE_LE_LAYOUT = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    protected static final long INT64_PEXT_MASK_LOW = 0x7f7f7f7f7f7f7f7fL;
    protected static final long INT64_PEXT_MASK_HIGH = 0x000000000000017fL;

    protected static final long VARINT32_CONT_BITS = 0x00000080_80808080L;
    protected static final long VARINT32_PAYLOAD_BITS = 0x0000007F_7F7F7F7FL;
    protected static final long VARINT64_LO_CONT_BITS = 0x80808080_80808080L;
    protected static final long VARINT64_HI_CONT_BITS = 0x8080L;
    protected static final long VARINT64_HI_PAYLOAD_BITS = 0x7F7FL;

    protected static final int[] VARINT64_SIZE_TABLE = new int[128];
    protected static final long[] VARINT64_LO_CONT_TABLE = new long[128];
    protected static final long[] VARINT64_HI_CONT_TABLE = new long[128];
    protected static final int[] VARINT32_SIZE_TABLE = new int[64];
    protected static final long[] VARINT32_LO_CONT_TABLE = new long[64];

    static {
        for (var nlz = 0; nlz <= 64; nlz++) {
            var size = (70 - nlz) / 7;
            VARINT64_SIZE_TABLE[nlz] = size;
            var loContBytes = Math.min(size, 9) - 1;
            var loMask = loContBytes <= 0 ? 0L
                    : ((1L << (loContBytes * 8)) - 1) & VARINT64_LO_CONT_BITS;
            VARINT64_LO_CONT_TABLE[nlz] = loMask;
            VARINT64_HI_CONT_TABLE[nlz] = size == 10 ? 0x80L : 0L;
        }
        for (var nlz = 1; nlz <= 32; nlz++) {
            var size = (38 - nlz) / 7;
            VARINT32_SIZE_TABLE[nlz] = size;
            var loMask = size <= 1 ? 0L
                    : ((1L << ((size - 1) * 8)) - 1) & VARINT64_LO_CONT_BITS;
            VARINT32_LO_CONT_TABLE[nlz] = loMask;
        }
        VARINT32_SIZE_TABLE[0] = 10;
    }

    protected long propertyIndex;

    protected ProtobufBinaryWriter() {
        resetPropertyTag();
    }

    protected static int writeRawVarInt32(byte[] arr, int offset, int value) {
        if (value >= 0) {
            if (arr.length - offset >= 8) {
                var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
                var size = VARINT32_SIZE_TABLE[nlz];
                var loCont = VARINT32_LO_CONT_TABLE[nlz];
                var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
                ARRAY_AS_INT64_LE.set(arr, offset, scattered | loCont);
                return size;
            }

            return writeRawVarInt32Slow(arr, offset, value);
        }

        return writeRawVarInt64(arr, offset, value);
    }

    private static int writeRawVarInt32Slow(byte[] arr, int offset, int value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                arr[pos++] = (byte) value;
                return pos - offset;
            }
            arr[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    protected static int writeRawVarInt64(byte[] arr, int offset, long value) {
        if (arr.length - offset >= 16) {
            var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
            var size = VARINT64_SIZE_TABLE[nlz];
            var loCont = VARINT64_LO_CONT_TABLE[nlz];
            var hiCont = VARINT64_HI_CONT_TABLE[nlz];
            var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
            var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
            ARRAY_AS_INT64_LE.set(arr, offset, lo | loCont);
            ARRAY_AS_INT64_LE.set(arr, offset + 8, hi | hiCont);
            return size;
        }

        return writeRawVarInt64Slow(arr, offset, value);
    }

    private static int writeRawVarInt64Slow(byte[] arr, int offset, long value) {
        var pos = offset;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                arr[pos++] = (byte) value;
                return pos - offset;
            }
            arr[pos++] = (byte) (((int) value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public static ProtobufBinaryWriter<byte[]> toBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufBinaryByteArrayWriter(new byte[length], 0);
    }

    public static ProtobufBinaryWriter<byte[]> toBytes(byte[] bytes, int offset) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        Objects.checkIndex(offset, bytes.length);
        return new ProtobufBinaryByteArrayWriter(bytes, offset);
    }

    public static ProtobufBinaryWriter<ByteBuffer> toBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufBinaryByteBufferWriter(ByteBuffer.allocate(length));
    }

    public static ProtobufBinaryWriter<ByteBuffer> toDirectBuffer(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative");
        }
        return new ProtobufBinaryByteBufferWriter(ByteBuffer.allocateDirect(length));
    }

    public static ProtobufBinaryWriter<ByteBuffer> toBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("buffer is read-only");
        }
        return new ProtobufBinaryByteBufferWriter(buffer);
    }

    public static ProtobufBinaryWriter<MemorySegment> toMemorySegment(MemorySegment segment) {
        Objects.requireNonNull(segment, "segment must not be null");
        return new ProtobufBinaryMemorySegmentWriter(segment);
    }

    public static ProtobufBinaryWriter<OutputStream> toStream(OutputStream buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        return new ProtobufBinaryStreamWriter(buffer);
    }

    public static ProtobufBinaryWriter<OutputStream> toStream(OutputStream buffer, int tempBufferLength) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (tempBufferLength < 0) {
            throw new IllegalArgumentException("tempBufferLength must not be negative");
        }
        return new ProtobufBinaryStreamWriter(buffer, tempBufferLength);
    }

    public boolean hasPropertyIndex() {
        return propertyIndex != Long.MIN_VALUE;
    }

    public void preparePropertyTag(long propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public void writePropertyTag(int wireType) {
        if (propertyIndex == Long.MIN_VALUE) {
            throw new IllegalStateException("No field index was set");
        } else {
            writeRawFixedInt64(ProtobufWireType.makeTag(propertyIndex, wireType));
            resetPropertyTag();
        }
    }

    public void writePropertyTag(long propertyIndex, int wireType) {
        writeRawFixedInt64(ProtobufWireType.makeTag(propertyIndex, wireType));
        resetPropertyTag();
    }

    private void resetPropertyTag() {
        this.propertyIndex = Long.MIN_VALUE;
    }

    public void writeLengthDelimitedPropertyLength(int length) {
        if (length < 0) {
            throw ProtobufDeserializationException.negativeLength(length);
        } else {
            writeRawFixedInt32(length);
        }
    }

    public void writeStartGroupProperty(long propertyIndex) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_START_OBJECT);
    }

    public void writeEndGroupProperty(long propertyIndex) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_END_OBJECT);
    }

    public void writeFloatProperty(long propertyIndex, Float value) {
        if (value != null) {
            writeFloatProperty(propertyIndex, (float) value);
        }
    }

    public void writeFloatProperty(long propertyIndex, float value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_FIXED32);
        writeRawFloat(value);
    }

    public void writeDoubleProperty(long propertyIndex, Double value) {
        if (value != null) {
            writeDoubleProperty(propertyIndex, (double) value);
        }
    }

    public void writeDoubleProperty(long propertyIndex, double value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_FIXED64);
        writeRawDouble(value);
    }

    public void writeBoolProperty(long propertyIndex, Boolean value) {
        if (value != null) {
            writeBoolProperty(propertyIndex, (boolean) value);
        }
    }

    public void writeBoolProperty(long propertyIndex, boolean value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawByte((byte) (value ? 1 : 0));
    }

    public void writeInt32Property(long propertyIndex, Integer value) {
        if (value != null) {
            writeInt32Property(propertyIndex, (int) value);
        }
    }

    public void writeInt32Property(long propertyIndex, int value) {
        writeVarInt32(propertyIndex, value);
    }

    public void writeUInt32Property(long propertyIndex, Integer value) {
        if (value != null) {
            writeUInt32Property(propertyIndex, (int) value);
        }
    }

    public void writeUInt32Property(long propertyIndex, int value) {
        writeVarInt32(propertyIndex, value);
    }

    private void writeVarInt32(long propertyIndex, int value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawVarInt32(value);
    }

    public void writeSInt32Property(long propertyIndex, Integer value) {
        if (value != null) {
            writeSInt32Property(propertyIndex, (int) value);
        }
    }

    public void writeSInt32Property(long propertyIndex, int value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawZigZagVarInt32(value);
    }


    public void writeInt64Property(long propertyIndex, Long value) {
        if (value != null) {
            writeInt64Property(propertyIndex, (long) value);
        }
    }

    public void writeInt64Property(long propertyIndex, long value) {
        writeVarInt64(propertyIndex, value);
    }

    public void writeUInt64Property(long propertyIndex, Long value) {
        if (value != null) {
            writeUInt64Property(propertyIndex, (long) value);
        }
    }

    public void writeUInt64Property(long propertyIndex, long value) {
        writeVarInt64(propertyIndex, value);
    }

    private void writeVarInt64(long propertyIndex, long value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawVarInt64(value);
    }


    public void writeSInt64Property(long propertyIndex, Long value) {
        if (value != null) {
            writeSInt64Property(propertyIndex, (long) value);
        }
    }

    public void writeSInt64Property(long propertyIndex, long value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawZigZagVarInt64(value);
    }

    public void writeFixed32Property(long propertyIndex, Integer value) {
        if (value != null) {
            writeFixed32(propertyIndex, value);
        }
    }

    public void writeFixed32Property(long propertyIndex, int value) {
        writeFixed32(propertyIndex, value);
    }

    public void writeSFixed32Property(long propertyIndex, Integer value) {
        if (value != null) {
            writeFixed32(propertyIndex, value);
        }
    }

    public void writeSFixed32Property(long propertyIndex, int value) {
        writeFixed32(propertyIndex, value);
    }

    private void writeFixed32(long propertyIndex, int value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_FIXED32);
        writeRawFixedInt32(value);
    }

    public void writeFixed64Property(long propertyIndex, Long value) {
        if (value != null) {
            writeFixed64(propertyIndex, value);
        }
    }

    public void writeFixed64Property(long propertyIndex, long value) {
        writeFixed64(propertyIndex, value);
    }

    public void writeSFixed64Property(long propertyIndex, Long value) {
        if (value != null) {
            writeFixed64(propertyIndex, value);
        }
    }

    public void writeSFixed64Property(long propertyIndex, long value) {
        writeFixed64(propertyIndex, value);
    }

    private void writeFixed64(long propertyIndex, long value) {
        writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_FIXED32);
        writeRawFixedInt64(value);
    }

    private void writeUnknownProperty(long propertyIndex, ProtobufUnknownValue value) {
        switch (value) {
            case ProtobufUnknownValue.Fixed32(var fixed32Value) -> writeFixed32(propertyIndex, fixed32Value);

            case ProtobufUnknownValue.Fixed64(var fixed64Value) -> writeFixed64(propertyIndex, fixed64Value);

            case ProtobufUnknownValue.Group(var groupValue) -> {
                writeStartGroupProperty(propertyIndex);
                for (var entry : groupValue.entrySet()) {
                    writeUnknownProperty(entry.getKey(), entry.getValue());
                }
                writeEndGroupProperty(propertyIndex);
            }

            case ProtobufUnknownValue.LengthDelimited lengthDelimited -> {
                switch (lengthDelimited) {
                    case ProtobufUnknownValue.LengthDelimited.ByteArrayBacked(var array) -> {
                        writeLengthDelimitedPropertyLength(array.length);
                        writeRawBytes(array);
                    }
                    case ProtobufUnknownValue.LengthDelimited.ByteBufferBacked(var buffer) -> {
                        writeLengthDelimitedPropertyLength(buffer.remaining());
                        writeRawBuffer(buffer);
                    }
                    case ProtobufUnknownValue.LengthDelimited.MemorySegmentBacked(var memory, var length) -> {
                        writeLengthDelimitedPropertyLength(length);
                        writeRawMemorySegment(memory);
                    }
                }
            }

            case ProtobufUnknownValue.VarInt(var varIntValue) -> writeInt64Property(propertyIndex, varIntValue);

            case null -> { /* Having this branch prevents a NPE */ }
        }
    }

    public void writePackedFloatProperty(long propertyIndex, float[] values) {
        if (values != null) {
            var size = values.length * Float.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedFloat(values);
        }
    }

    public void writePackedDoubleProperty(long propertyIndex, double[] values) {
        if (values != null) {
            var size = values.length * Double.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedDouble(values);
        }
    }

    public void writePackedInt32Property(long propertyIndex, int[] values) {
        if (values != null) {
            var size = 0;
            for (var value : values) {
                size += ProtobufBinarySizeCalculator.getVarInt32Size(value);
            }
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedVarInt32(values);
        }
    }

    public void writePackedUInt32Property(long propertyIndex, int[] values) {
        if (values != null) {
            var size = 0;
            for (var value : values) {
                size += ProtobufBinarySizeCalculator.getVarInt32Size(value);
            }
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedVarInt32(values);
        }
    }

    public void writePackedSInt32Property(long propertyIndex, int[] values) {
        if (values != null) {
            var size = 0;
            for (var value : values) {
                size += ProtobufBinarySizeCalculator.getVarInt32Size(value);
            }
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedZigZagVarInt32(values);
        }
    }

    public void writePackedInt64Property(long propertyIndex, long[] values) {
        if (values != null) {
            var size = ProtobufBinarySizeCalculator.getVarInt64PackedSize(propertyIndex, values);
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedVarInt64(values);
        }
    }

    public void writePackedUInt64Property(long propertyIndex, long[] values) {
        if (values != null) {
            var size = ProtobufBinarySizeCalculator.getVarInt64PackedSize(propertyIndex, values);
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedVarInt64(values);
        }
    }

    public void writePackedSInt64Property(long propertyIndex, long[] values) {
        if (values != null) {
            var size = 0;
            for (var value : values) {
                size += ProtobufBinarySizeCalculator.getVarInt64Size(value);
            }
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedZigZagVarInt64(values);
        }
    }

    public void writePackedFixed32Property(long propertyIndex, int[] values) {
        if (values != null) {
            var size = values.length * Integer.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedFixedInt32(values);
        }
    }

    public void writePackedSFixed32Property(long propertyIndex, int[] values) {
        if (values != null) {
            var size = values.length * Integer.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedFixedInt32(values);
        }
    }

    public void writePackedFixed64Property(long propertyIndex, long[] values) {
        if (values != null) {
            var size = values.length * Long.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedFixedInt64(values);
        }
    }

    public void writePackedSFixed64Property(long propertyIndex, long[] values) {
        if (values != null) {
            var size = values.length * Long.BYTES;
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedFixedInt64(values);
        }
    }

    public void writePackedSFixed64Property(long propertyIndex, boolean[] values) {
        if (values != null) {
            var size = ProtobufBinarySizeCalculator.getBoolPackedSize(propertyIndex, values);
            writePropertyTag(propertyIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedPropertyLength(size);
            writeRawPackedBool(values);
        }
    }

    public void writeRawBytes(byte[] entry) {
        writeRawBytes(entry, 0, entry.length);
    }

    public void writeRawZigZagVarInt32(int value) {
        var zigzag = (value << 1) ^ (value >> 31);
        writeRawVarInt32(zigzag);
    }

    public void writeRawZigZagVarInt64(long value) {
        var zigzag = (value << 1) ^ (value >> 63);
        writeRawVarInt64(zigzag);
    }

    public abstract void writeRawByte(byte entry);

    public abstract void writeRawBytes(byte[] entry, int offset, int length);

    public abstract void writeRawBuffer(ByteBuffer entry);

    public abstract void writeRawMemorySegment(MemorySegment entry);

    public abstract void writeRawFixedInt32(int entry);

    public abstract void writeRawFixedInt64(long entry);

    public abstract void writeRawFloat(float entry);

    public abstract void writeRawDouble(double entry);

    public abstract void writeRawVarInt32(int entry);

    public abstract void writeRawVarInt64(long entry);
    // no writeRawBool because the fastest path is always writeRawByte(0 or 1)

    public abstract void writeRawPackedFixedInt32(int[] input);

    public abstract void writeRawPackedFixedInt64(long[] input);

    public abstract void writeRawPackedFloat(float[] input);

    public abstract void writeRawPackedDouble(double[] input);

    public abstract void writeRawPackedVarInt32(int[] input);

    public abstract void writeRawPackedZigZagVarInt32(int[] input);

    public abstract void writeRawPackedVarInt64(long[] input);

    public abstract void writeRawPackedZigZagVarInt64(long[] input);

    public abstract void writeRawPackedBool(boolean[] input);

    public abstract ProtobufIODataType rawDataTypePreference();

    public abstract OUTPUT toOutput();
}