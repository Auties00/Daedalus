package com.github.auties00.daedalus.protobuf.io.writer;

import com.github.auties00.daedalus.typesystem.util.BinaryUtils;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.typesystem.model.BinaryDataType;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryByteArrayWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryByteBufferWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryMemorySegmentWriter;
import com.github.auties00.daedalus.protobuf.io.writer.binary.ProtobufBinaryStreamWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.github.auties00.daedalus.protobuf.model.ProtobufWireType;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
public abstract non-sealed class ProtobufBinaryWriter<OUTPUT> extends ProtobufWriter<OUTPUT> {



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
            // A shift by 64 is a shift by 0 in Java, so the full mask must be special cased
            var loMask = loContBytes <= 0 ? 0L
                    : loContBytes >= Long.BYTES ? VARINT64_LO_CONT_BITS
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

    protected int fieldIndex;

    protected ProtobufBinaryWriter() {
        resetFieldTag();
    }

    protected static int writeRawVarInt32(byte[] arr, int offset, int value) throws ProtobufSerializationException {
        if (value >= 0) {
            if (arr.length - offset >= 8) {
                var nlz = Integer.numberOfLeadingZeros(value | 1) & 0x3F;
                var size = VARINT32_SIZE_TABLE[nlz];
                var loCont = VARINT32_LO_CONT_TABLE[nlz];
                var scattered = Long.expand(value & 0xFFFFFFFFL, VARINT32_PAYLOAD_BITS);
                BinaryUtils.writeLongLE(arr, offset, scattered | loCont);
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

    protected static int writeRawVarInt64(byte[] arr, int offset, long value) throws ProtobufSerializationException {
        if (arr.length - offset >= 16) {
            var nlz = Long.numberOfLeadingZeros(value | 1) & 0x7F;
            var size = VARINT64_SIZE_TABLE[nlz];
            var loCont = VARINT64_LO_CONT_TABLE[nlz];
            var hiCont = VARINT64_HI_CONT_TABLE[nlz];
            var lo = Long.expand(value, INT64_PEXT_MASK_LOW);
            var hi = Long.expand(value >>> 56, VARINT64_HI_PAYLOAD_BITS);
            BinaryUtils.writeLongLE(arr, offset, lo | loCont);
            BinaryUtils.writeLongLE(arr, offset + 8, hi | hiCont);
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

    public boolean hasFieldIndex() {
        return fieldIndex != Integer.MIN_VALUE;
    }

    public void prepareFieldTag(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public void writeFieldTag(int wireType) throws ProtobufSerializationException {
        if (fieldIndex == Integer.MIN_VALUE) {
            throw new IllegalStateException("No field index was set");
        } else {
            writeRawVarInt64(ProtobufWireType.makeTag(fieldIndex, wireType));
            resetFieldTag();
        }
    }

    public void writeFieldTag(int fieldIndex, int wireType) throws ProtobufSerializationException {
        // Tags are unsigned 32 bit values, so they are written as 64 bit varints to avoid the sign extension of int varints
        writeRawVarInt64(ProtobufWireType.makeTag(fieldIndex, wireType));
        resetFieldTag();
    }

    private void resetFieldTag() {
        this.fieldIndex = Integer.MIN_VALUE;
    }

    public void writeLengthDelimitedFieldLength(int length) throws ProtobufSerializationException {
        if (length < 0) {
            throw ProtobufSerializationException.negativeLength();
        } else {
            writeRawVarInt32(length);
        }
    }

    public void writeStartGroupField(int fieldIndex) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_START_OBJECT);
    }

    public void writeEndGroupField(int fieldIndex) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_END_OBJECT);
    }

    public void writeFloatField(int fieldIndex, Float value) throws ProtobufSerializationException {
        if (value != null) {
            writeFloatField(fieldIndex, (float) value);
        }
    }

    public void writeFloatField(int fieldIndex, float value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32);
        writeRawFloat(value);
    }

    public void writeDoubleField(int fieldIndex, Double value) throws ProtobufSerializationException {
        if (value != null) {
            writeDoubleField(fieldIndex, (double) value);
        }
    }

    public void writeDoubleField(int fieldIndex, double value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64);
        writeRawDouble(value);
    }

    public void writeBoolField(int fieldIndex, Boolean value) throws ProtobufSerializationException {
        if (value != null) {
            writeBoolField(fieldIndex, (boolean) value);
        }
    }

    public void writeBoolField(int fieldIndex, boolean value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawByte((byte) (value ? 1 : 0));
    }

    public void writeInt32Field(int fieldIndex, Integer value) throws ProtobufSerializationException {
        if (value != null) {
            writeInt32Field(fieldIndex, (int) value);
        }
    }

    public void writeInt32Field(int fieldIndex, int value) throws ProtobufSerializationException {
        writeVarInt32(fieldIndex, value);
    }

    public void writeUInt32Field(int fieldIndex, Integer value) throws ProtobufSerializationException {
        if (value != null) {
            writeUInt32Field(fieldIndex, (int) value);
        }
    }

    public void writeUInt32Field(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawUnsignedVarInt32(value);
    }

    private void writeVarInt32(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawVarInt32(value);
    }

    public void writeSInt32Field(int fieldIndex, Integer value) throws ProtobufSerializationException {
        if (value != null) {
            writeSInt32Field(fieldIndex, (int) value);
        }
    }

    public void writeSInt32Field(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawZigZagVarInt32(value);
    }


    public void writeInt64Field(int fieldIndex, Long value) throws ProtobufSerializationException {
        if (value != null) {
            writeInt64Field(fieldIndex, (long) value);
        }
    }

    public void writeInt64Field(int fieldIndex, long value) throws ProtobufSerializationException {
        writeVarInt64(fieldIndex, value);
    }

    public void writeUInt64Field(int fieldIndex, Long value) throws ProtobufSerializationException {
        if (value != null) {
            writeUInt64Field(fieldIndex, (long) value);
        }
    }

    public void writeUInt64Field(int fieldIndex, long value) throws ProtobufSerializationException {
        writeVarInt64(fieldIndex, value);
    }

    private void writeVarInt64(int fieldIndex, long value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawVarInt64(value);
    }


    public void writeSInt64Field(int fieldIndex, Long value) throws ProtobufSerializationException {
        if (value != null) {
            writeSInt64Field(fieldIndex, (long) value);
        }
    }

    public void writeSInt64Field(int fieldIndex, long value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_VAR_INT);
        writeRawZigZagVarInt64(value);
    }

    public void writeFixed32Field(int fieldIndex, Integer value) throws ProtobufSerializationException {
        if (value != null) {
            writeFixed32(fieldIndex, value);
        }
    }

    public void writeFixed32Field(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFixed32(fieldIndex, value);
    }

    public void writeSFixed32Field(int fieldIndex, Integer value) throws ProtobufSerializationException {
        if (value != null) {
            writeFixed32(fieldIndex, value);
        }
    }

    public void writeSFixed32Field(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFixed32(fieldIndex, value);
    }

    private void writeFixed32(int fieldIndex, int value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED32);
        writeRawFixedInt32(value);
    }

    public void writeFixed64Field(int fieldIndex, Long value) throws ProtobufSerializationException {
        if (value != null) {
            writeFixed64(fieldIndex, value);
        }
    }

    public void writeFixed64Field(int fieldIndex, long value) throws ProtobufSerializationException {
        writeFixed64(fieldIndex, value);
    }

    public void writeSFixed64Field(int fieldIndex, Long value) throws ProtobufSerializationException {
        if (value != null) {
            writeFixed64(fieldIndex, value);
        }
    }

    public void writeSFixed64Field(int fieldIndex, long value) throws ProtobufSerializationException {
        writeFixed64(fieldIndex, value);
    }

    private void writeFixed64(int fieldIndex, long value) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_FIXED64);
        writeRawFixedInt64(value);
    }

    public void writeUnknownField(int fieldIndex, ProtobufUnknownValue.Binary value) throws ProtobufSerializationException {
        switch (value) {
            case ProtobufUnknownValue.Binary.Fixed32(var fixed32Value) -> writeFixed32(fieldIndex, fixed32Value);

            case ProtobufUnknownValue.Binary.Fixed64(var fixed64Value) -> writeFixed64(fieldIndex, fixed64Value);

            case ProtobufUnknownValue.Binary.Group(var groupValue) -> {
                writeStartGroupField(fieldIndex);
                groupValue.encodeBinary(this);
                writeEndGroupField(fieldIndex);
            }

            case ProtobufUnknownValue.Binary.LengthDelimited lengthDelimited -> {
                writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
                switch (lengthDelimited) {
                    case ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(var array) -> {
                        writeLengthDelimitedFieldLength(array.length);
                        writeRawBytes(array);
                    }
                    case ProtobufUnknownValue.Binary.LengthDelimited.ByteBufferBacked(var buffer) -> {
                        writeLengthDelimitedFieldLength(buffer.remaining());
                        writeRawBuffer(buffer);
                    }
                    case ProtobufUnknownValue.Binary.LengthDelimited.MemorySegmentBacked(var memory, var length) -> {
                        writeLengthDelimitedFieldLength(length);
                        writeRawMemorySegment(memory);
                    }
                }
            }

            case ProtobufUnknownValue.Binary.VarInt(var varIntValue) -> writeInt64Field(fieldIndex, varIntValue);

            case null -> { /* Having this branch prevents a NPE */ }
        }
    }

    public void writePackedFloatField(int fieldIndex, float[] values) throws ProtobufSerializationException {
        if (values != null) {
            var size = values.length * Float.BYTES;
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(size);
            writeRawPackedFloat(values);
        }
    }

    public void writePackedDoubleField(int fieldIndex, double[] values) throws ProtobufSerializationException {
        if (values != null) {
            var size = values.length * Double.BYTES;
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(size);
            writeRawPackedDouble(values);
        }
    }

    public void writePackedInt32Field(int fieldIndex, int[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedInt32Size(values));
            writeRawPackedVarInt32(values);
        }
    }

    public void writePackedUInt32Field(int fieldIndex, int[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedUInt32Size(values));
            for (var value : values) {
                writeRawUnsignedVarInt32(value);
            }
        }
    }

    public void writePackedSInt32Field(int fieldIndex, int[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedSInt32Size(values));
            for (var value : values) {
                writeRawZigZagVarInt32(value);
            }
        }
    }

    public void writePackedInt64Field(int fieldIndex, long[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedInt64Size(values));
            writeRawPackedVarInt64(values);
        }
    }

    public void writePackedUInt64Field(int fieldIndex, long[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedUInt64Size(values));
            writeRawPackedVarInt64(values);
        }
    }

    public void writePackedSInt64Field(int fieldIndex, long[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedSInt64Size(values));
            writeRawPackedZigZagVarInt64(values);
        }
    }

    public void writePackedFixed32Field(int fieldIndex, int[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedFixed32Size(values));
            writeRawPackedFixedInt32(values);
        }
    }

    public void writePackedSFixed32Field(int fieldIndex, int[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedSFixed32Size(values));
            writeRawPackedFixedInt32(values);
        }
    }

    public void writePackedFixed64Field(int fieldIndex, long[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedFixed64Size(values));
            writeRawPackedFixedInt64(values);
        }
    }

    public void writePackedSFixed64Field(int fieldIndex, long[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedSFixed64Size(values));
            writeRawPackedFixedInt64(values);
        }
    }

    public void writePackedBoolField(int fieldIndex, boolean[] values) throws ProtobufSerializationException {
        if (values != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(ProtobufBinarySizeCalculator.getPackedBoolSize(values));
            writeRawPackedBool(values);
        }
    }

    public void writeStringField(int fieldIndex, String value) throws ProtobufSerializationException {
        if (value != null) {
            writeBytesField(fieldIndex, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void writeBytesField(int fieldIndex, byte[] value) throws ProtobufSerializationException {
        if (value != null) {
            writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
            writeLengthDelimitedFieldLength(value.length);
            writeRawBytes(value);
        }
    }

    public void writeMessageField(int fieldIndex, int size) throws ProtobufSerializationException {
        writeFieldTag(fieldIndex, ProtobufWireType.WIRE_TYPE_LENGTH_DELIMITED);
        writeLengthDelimitedFieldLength(size);
    }

    public void writeRawBytes(byte[] entry) throws ProtobufSerializationException {
        writeRawBytes(entry, 0, entry.length);
    }

    public void writeRawZigZagVarInt32(int value) throws ProtobufSerializationException {
        writeRawUnsignedVarInt32((value << 1) ^ (value >> 31));
    }

    // uint32 values, and the zigzag encoding of sint32 values, are unsigned: they are zero extended, never sign extended
    public void writeRawUnsignedVarInt32(int value) throws ProtobufSerializationException {
        if (value >= 0) {
            writeRawVarInt32(value);
        } else {
            writeRawVarInt64(Integer.toUnsignedLong(value));
        }
    }

    public void writeRawZigZagVarInt64(long value) throws ProtobufSerializationException {
        var zigzag = (value << 1) ^ (value >> 63);
        writeRawVarInt64(zigzag);
    }

    public abstract void writeRawByte(byte entry) throws ProtobufSerializationException;

    public abstract void writeRawBytes(byte[] entry, int offset, int length) throws ProtobufSerializationException;

    public abstract void writeRawBuffer(ByteBuffer entry) throws ProtobufSerializationException;

    public abstract void writeRawMemorySegment(MemorySegment entry) throws ProtobufSerializationException;

    public abstract void writeRawFixedInt32(int entry) throws ProtobufSerializationException;

    public abstract void writeRawFixedInt64(long entry) throws ProtobufSerializationException;

    public abstract void writeRawFloat(float entry) throws ProtobufSerializationException;

    public abstract void writeRawDouble(double entry) throws ProtobufSerializationException;

    public abstract void writeRawVarInt32(int entry) throws ProtobufSerializationException;

    public abstract void writeRawVarInt64(long entry) throws ProtobufSerializationException;
    // no writeRawBool because the fastest path is always writeRawByte(0 or 1)

    public abstract void writeRawPackedFixedInt32(int[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedFixedInt64(long[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedFloat(float[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedDouble(double[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedVarInt32(int[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedZigZagVarInt32(int[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedVarInt64(long[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedZigZagVarInt64(long[] input) throws ProtobufSerializationException;

    public abstract void writeRawPackedBool(boolean[] input) throws ProtobufSerializationException;

    public abstract BinaryDataType rawDataTypePreference();

    public abstract OUTPUT toOutput();
}