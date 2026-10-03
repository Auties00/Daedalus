package com.github.auties00.daedalus.protobuf.io;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufBinarySizeCalculator;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the binary readers, writers and size size against {@code protobuf-java}, the
 * reference implementation of the wire format.
 */
public class ProtobufBinaryWireFormatTest {
    private static final int GROUP_INDEX = 22;
    private static final int MESSAGE_INDEX = 23;
    private static final String TEXT = "héllo\0wörld 🌍";

    @FunctionalInterface
    interface ReferenceWriter {
        void write(CodedOutputStream output) throws IOException;
    }

    @FunctionalInterface
    interface Writer {
        void write(ProtobufBinaryWriter<?> writer) throws ProtobufSerializationException;
    }

    @FunctionalInterface
    interface Reader {
        void read(ProtobufBinaryReader reader) throws ProtobufDeserializationException;
    }

    @FunctionalInterface
    interface ReaderFactory {
        ProtobufBinaryReader create(byte[] bytes);
    }

    @FunctionalInterface
    interface WriterFactory {
        ProtobufBinaryWriter<?> create(int size);
    }

    @FunctionalInterface
    interface OutputExtractor {
        byte[] extract(ProtobufBinaryWriter<?> writer);
    }

    record Field(String name, int index, ReferenceWriter reference, int referenceSize, Writer writer, long size, Reader reader) {
        @Override
        public String toString() {
            return name;
        }
    }

    record WriterKind(String name, WriterFactory factory, OutputExtractor extractor) {
        @Override
        public String toString() {
            return name;
        }
    }

    record ReaderKind(String name, ReaderFactory factory) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static List<Field> fields() {
        var fields = new ArrayList<Field>();
        fields.add(new Field("float", 1,
                output -> output.writeFloat(1, 3.5f), CodedOutputStream.computeFloatSize(1, 3.5f),
                writer -> writer.writeFloatField(1, 3.5f), ProtobufBinarySizeCalculator.getFloatFieldSize(1, 3.5f),
                reader -> assertEquals(3.5f, reader.readFloatField())));
        fields.add(new Field("double", 2,
                output -> output.writeDouble(2, -1.25), CodedOutputStream.computeDoubleSize(2, -1.25),
                writer -> writer.writeDoubleField(2, -1.25), ProtobufBinarySizeCalculator.getDoubleFieldSize(2, -1.25),
                reader -> assertEquals(-1.25, reader.readDoubleField())));
        fields.add(new Field("bool", 3,
                output -> output.writeBool(3, true), CodedOutputStream.computeBoolSize(3, true),
                writer -> writer.writeBoolField(3, true), ProtobufBinarySizeCalculator.getBoolFieldSize(3, true),
                reader -> assertTrue(reader.readBoolField())));
        fields.add(new Field("int32", 4,
                output -> output.writeInt32(4, -5), CodedOutputStream.computeInt32Size(4, -5),
                writer -> writer.writeInt32Field(4, -5), ProtobufBinarySizeCalculator.getInt32FieldSize(4, -5),
                reader -> assertEquals(-5, reader.readInt32Field())));
        fields.add(new Field("uint32", 5,
                output -> output.writeUInt32(5, -1), CodedOutputStream.computeUInt32Size(5, -1),
                writer -> writer.writeUInt32Field(5, -1), ProtobufBinarySizeCalculator.getUInt32FieldSize(5, -1),
                reader -> assertEquals(-1, reader.readUInt32Field())));
        fields.add(new Field("sint32", 6,
                output -> output.writeSInt32(6, -123456), CodedOutputStream.computeSInt32Size(6, -123456),
                writer -> writer.writeSInt32Field(6, -123456), ProtobufBinarySizeCalculator.getSInt32FieldSize(6, -123456),
                reader -> assertEquals(-123456, reader.readSInt32Field())));
        fields.add(new Field("int64", 7,
                output -> output.writeInt64(7, Long.MIN_VALUE), CodedOutputStream.computeInt64Size(7, Long.MIN_VALUE),
                writer -> writer.writeInt64Field(7, Long.MIN_VALUE), ProtobufBinarySizeCalculator.getInt64FieldSize(7, Long.MIN_VALUE),
                reader -> assertEquals(Long.MIN_VALUE, reader.readInt64Field())));
        fields.add(new Field("uint64", 8,
                output -> output.writeUInt64(8, -1L), CodedOutputStream.computeUInt64Size(8, -1L),
                writer -> writer.writeUInt64Field(8, -1L), ProtobufBinarySizeCalculator.getUInt64FieldSize(8, -1L),
                reader -> assertEquals(-1L, reader.readUInt64Field())));
        fields.add(new Field("sint64", 9,
                output -> output.writeSInt64(9, -987654321987L), CodedOutputStream.computeSInt64Size(9, -987654321987L),
                writer -> writer.writeSInt64Field(9, -987654321987L), ProtobufBinarySizeCalculator.getSInt64FieldSize(9, -987654321987L),
                reader -> assertEquals(-987654321987L, reader.readSInt64Field())));
        fields.add(new Field("fixed32", 10,
                output -> output.writeFixed32(10, 0xDEADBEEF), CodedOutputStream.computeFixed32Size(10, 0xDEADBEEF),
                writer -> writer.writeFixed32Field(10, 0xDEADBEEF), ProtobufBinarySizeCalculator.getFixed32FieldSize(10, 0xDEADBEEF),
                reader -> assertEquals(0xDEADBEEF, reader.readFixed32Field())));
        fields.add(new Field("sfixed32", 11,
                output -> output.writeSFixed32(11, -1), CodedOutputStream.computeSFixed32Size(11, -1),
                writer -> writer.writeSFixed32Field(11, -1), ProtobufBinarySizeCalculator.getSFixed32FieldSize(11, -1),
                reader -> assertEquals(-1, reader.readSFixed32Field())));
        fields.add(new Field("fixed64", 12,
                output -> output.writeFixed64(12, 0x0123456789ABCDEFL), CodedOutputStream.computeFixed64Size(12, 0x0123456789ABCDEFL),
                writer -> writer.writeFixed64Field(12, 0x0123456789ABCDEFL), ProtobufBinarySizeCalculator.getFixed64FieldSize(12, 0x0123456789ABCDEFL),
                reader -> assertEquals(0x0123456789ABCDEFL, reader.readFixed64Field())));
        fields.add(new Field("sfixed64", 13,
                output -> output.writeSFixed64(13, Long.MIN_VALUE), CodedOutputStream.computeSFixed64Size(13, Long.MIN_VALUE),
                writer -> writer.writeSFixed64Field(13, Long.MIN_VALUE), ProtobufBinarySizeCalculator.getSFixed64FieldSize(13, Long.MIN_VALUE),
                reader -> assertEquals(Long.MIN_VALUE, reader.readSFixed64Field())));
        fields.add(new Field("string", 14,
                output -> output.writeString(14, TEXT), CodedOutputStream.computeStringSize(14, TEXT),
                writer -> writer.writeStringField(14, TEXT), ProtobufBinarySizeCalculator.getStringFieldSize(14, TEXT),
                reader -> assertEquals(TEXT, reader.readStringField(true))));
        var bytes = new byte[]{0, 1, 2, (byte) 255};
        fields.add(new Field("bytes", 15,
                output -> output.writeByteArray(15, bytes), CodedOutputStream.computeByteArraySize(15, bytes),
                writer -> writer.writeBytesField(15, bytes), ProtobufBinarySizeCalculator.getBytesFieldSize(15, bytes),
                reader -> assertArrayEquals(bytes, reader.readBytesField())));
        fields.add(new Field("empty string", 16,
                output -> output.writeString(16, ""), CodedOutputStream.computeStringSize(16, ""),
                writer -> writer.writeStringField(16, ""), ProtobufBinarySizeCalculator.getStringFieldSize(16, ""),
                reader -> assertEquals("", reader.readStringField(false))));
        fields.add(new Field("max field number", 536870911,
                output -> output.writeString(536870911, "max"), CodedOutputStream.computeStringSize(536870911, "max"),
                writer -> writer.writeStringField(536870911, "max"), ProtobufBinarySizeCalculator.getStringFieldSize(536870911, "max"),
                reader -> assertEquals("max", reader.readStringField(false))));

        // Long enough for the SIMD pair decoder and for a payload larger than the smallest stream buffer,
        // mixing lengths from one to ten bytes so pairs fall back to the scalar decoder
        var int32Values = new int[64];
        for (var i = 0; i < int32Values.length; i++) {
            int32Values[i] = switch (i % 6) {
                case 0 -> i;
                case 1 -> -i;
                case 2 -> 300 * i;
                case 3 -> Integer.MAX_VALUE - i;
                case 4 -> Integer.MIN_VALUE + i;
                default -> 1 << (i % 31);
            };
        }
        var int32DataSize = 0;
        for (var value : int32Values) {
            int32DataSize += CodedOutputStream.computeInt32SizeNoTag(value);
        }
        var packedInt32DataSize = int32DataSize;
        fields.add(new Field("packed int32", 17,
                output -> {
                    output.writeTag(17, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(packedInt32DataSize);
                    for (var value : int32Values) {
                        output.writeInt32NoTag(value);
                    }
                }, CodedOutputStream.computeTagSize(17) + CodedOutputStream.computeUInt32SizeNoTag(packedInt32DataSize) + packedInt32DataSize,
                writer -> writer.writePackedInt32Field(17, int32Values), ProtobufBinarySizeCalculator.getPackedInt32FieldSize(17, int32Values),
                reader -> assertArrayEquals(int32Values, reader.readPackedInt32Field())));

        var sint64Values = new long[]{-2, 5, Long.MAX_VALUE};
        var sint64DataSize = 0;
        for (var value : sint64Values) {
            sint64DataSize += CodedOutputStream.computeSInt64SizeNoTag(value);
        }
        var packedSInt64DataSize = sint64DataSize;
        fields.add(new Field("packed sint64", 18,
                output -> {
                    output.writeTag(18, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(packedSInt64DataSize);
                    for (var value : sint64Values) {
                        output.writeSInt64NoTag(value);
                    }
                }, CodedOutputStream.computeTagSize(18) + CodedOutputStream.computeUInt32SizeNoTag(packedSInt64DataSize) + packedSInt64DataSize,
                writer -> writer.writePackedSInt64Field(18, sint64Values), ProtobufBinarySizeCalculator.getPackedSInt64FieldSize(18, sint64Values),
                reader -> assertArrayEquals(sint64Values, reader.readPackedSInt64Field())));

        var doubleValues = new double[]{1.5, -0.0};
        fields.add(new Field("packed double", 19,
                output -> {
                    output.writeTag(19, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(doubleValues.length * Double.BYTES);
                    for (var value : doubleValues) {
                        output.writeDoubleNoTag(value);
                    }
                }, CodedOutputStream.computeTagSize(19) + CodedOutputStream.computeUInt32SizeNoTag(doubleValues.length * Double.BYTES) + doubleValues.length * Double.BYTES,
                writer -> writer.writePackedDoubleField(19, doubleValues), ProtobufBinarySizeCalculator.getPackedDoubleFieldSize(19, doubleValues),
                reader -> assertArrayEquals(doubleValues, reader.readPackedDoubleField())));

        var boolValues = new boolean[]{true, false, true};
        fields.add(new Field("packed bool", 20,
                output -> {
                    output.writeTag(20, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(boolValues.length);
                    for (var value : boolValues) {
                        output.writeBoolNoTag(value);
                    }
                }, CodedOutputStream.computeTagSize(20) + CodedOutputStream.computeUInt32SizeNoTag(boolValues.length) + boolValues.length,
                writer -> writer.writePackedBoolField(20, boolValues), ProtobufBinarySizeCalculator.getPackedBoolFieldSize(20, boolValues),
                reader -> assertArrayEquals(boolValues, reader.readPackedBoolField())));

        var fixed32Values = new int[]{1, -2};
        fields.add(new Field("packed fixed32", 21,
                output -> {
                    output.writeTag(21, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(fixed32Values.length * Integer.BYTES);
                    for (var value : fixed32Values) {
                        output.writeFixed32NoTag(value);
                    }
                }, CodedOutputStream.computeTagSize(21) + CodedOutputStream.computeUInt32SizeNoTag(fixed32Values.length * Integer.BYTES) + fixed32Values.length * Integer.BYTES,
                writer -> writer.writePackedFixed32Field(21, fixed32Values), ProtobufBinarySizeCalculator.getPackedFixed32FieldSize(21, fixed32Values),
                reader -> assertArrayEquals(fixed32Values, reader.readPackedFixed32Field())));

        fields.add(new Field("group", GROUP_INDEX,
                output -> {
                    output.writeTag(GROUP_INDEX, WireFormat.WIRETYPE_START_GROUP);
                    output.writeInt32(1, 42);
                    output.writeTag(GROUP_INDEX, WireFormat.WIRETYPE_END_GROUP);
                }, 2 * CodedOutputStream.computeTagSize(GROUP_INDEX) + CodedOutputStream.computeInt32Size(1, 42),
                writer -> {
                    writer.writeStartGroupField(GROUP_INDEX);
                    writer.writeInt32Field(1, 42);
                    writer.writeEndGroupField(GROUP_INDEX);
                }, ProtobufBinarySizeCalculator.getStartGroupFieldSize(GROUP_INDEX) + ProtobufBinarySizeCalculator.getInt32FieldSize(1, 42) + ProtobufBinarySizeCalculator.getEndGroupFieldSize(GROUP_INDEX),
                reader -> {
                    reader.readStartGroupField(GROUP_INDEX);
                    assertTrue(reader.nextFieldTag());
                    assertEquals(1, reader.fieldIndex());
                    assertEquals(42, reader.readInt32Field());
                    assertFalse(reader.nextFieldTag());
                    reader.readEndGroupField(GROUP_INDEX);
                }));

        var messageContentSize = CodedOutputStream.computeInt32Size(1, 7) + CodedOutputStream.computeStringSize(2, "x");
        fields.add(new Field("message", MESSAGE_INDEX,
                output -> {
                    output.writeTag(MESSAGE_INDEX, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                    output.writeUInt32NoTag(messageContentSize);
                    output.writeInt32(1, 7);
                    output.writeString(2, "x");
                }, CodedOutputStream.computeTagSize(MESSAGE_INDEX) + CodedOutputStream.computeUInt32SizeNoTag(messageContentSize) + messageContentSize,
                writer -> {
                    writer.writeMessageField(MESSAGE_INDEX, messageContentSize);
                    writer.writeInt32Field(1, 7);
                    writer.writeStringField(2, "x");
                }, ProtobufBinarySizeCalculator.getMessageFieldSize(MESSAGE_INDEX, messageContentSize),
                reader -> {
                    var message = reader.readLengthDelimitedField();
                    assertTrue(message.nextFieldTag());
                    assertEquals(7, message.readInt32Field());
                    assertTrue(message.nextFieldTag());
                    assertEquals("x", message.readStringField(false));
                    assertFalse(message.nextFieldTag());
                }));
        return fields;
    }

    private static byte[] referenceBytes(List<Field> fields) throws IOException {
        var size = 0;
        for (var field : fields) {
            size += field.referenceSize();
        }
        var result = new byte[size];
        var output = CodedOutputStream.newInstance(result);
        for (var field : fields) {
            field.reference().write(output);
        }
        output.checkNoSpaceLeft();
        return result;
    }

    private static byte[] segmentBytes(MemorySegment segment) {
        return segment.toArray(ValueLayout.JAVA_BYTE);
    }

    private static byte[] bufferBytes(ByteBuffer buffer) {
        var result = new byte[buffer.capacity()];
        buffer.get(0, result);
        return result;
    }

    static Stream<Arguments> writers() {
        var streams = new LinkedHashMap<ProtobufBinaryWriter<?>, ByteArrayOutputStream>();
        return Stream.of(
                new WriterKind("byte[]", ProtobufBinaryWriter::toBytes, writer -> (byte[]) writer.toOutput()),
                new WriterKind("heap ByteBuffer", ProtobufBinaryWriter::toBuffer, writer -> bufferBytes((ByteBuffer) writer.toOutput())),
                new WriterKind("direct ByteBuffer", ProtobufBinaryWriter::toDirectBuffer, writer -> bufferBytes((ByteBuffer) writer.toOutput())),
                new WriterKind("heap MemorySegment", size -> ProtobufBinaryWriter.toMemorySegment(MemorySegment.ofArray(new byte[size])), writer -> segmentBytes((MemorySegment) writer.toOutput())),
                new WriterKind("native MemorySegment", size -> ProtobufBinaryWriter.toMemorySegment(Arena.ofAuto().allocate(size)), writer -> segmentBytes((MemorySegment) writer.toOutput())),
                new WriterKind("stream", _ -> {
                    var output = new ByteArrayOutputStream();
                    var writer = ProtobufBinaryWriter.toStream(output);
                    streams.put(writer, output);
                    return writer;
                }, writer -> streams.get(writer).toByteArray()),
                new WriterKind("stream with small buffer", _ -> {
                    var output = new ByteArrayOutputStream();
                    var writer = ProtobufBinaryWriter.toStream(output, 3);
                    streams.put(writer, output);
                    return writer;
                }, writer -> streams.get(writer).toByteArray())
        ).map(Arguments::of);
    }

    static Stream<Arguments> readers() {
        return Stream.of(
                new ReaderKind("byte[]", ProtobufBinaryReader::fromBytes),
                new ReaderKind("byte[] with offset", bytes -> {
                    var padded = new byte[bytes.length + 6];
                    padded[0] = padded[1] = padded[2] = (byte) 0xFF;
                    System.arraycopy(bytes, 0, padded, 3, bytes.length);
                    padded[padded.length - 1] = padded[padded.length - 2] = padded[padded.length - 3] = (byte) 0xFF;
                    return ProtobufBinaryReader.fromBytes(padded, 3, bytes.length);
                }),
                new ReaderKind("heap ByteBuffer", bytes -> ProtobufBinaryReader.fromBuffer(ByteBuffer.wrap(bytes))),
                new ReaderKind("direct ByteBuffer", bytes -> ProtobufBinaryReader.fromBuffer(ByteBuffer.allocateDirect(bytes.length).put(bytes).flip())),
                new ReaderKind("heap MemorySegment", bytes -> ProtobufBinaryReader.fromMemorySegment(MemorySegment.ofArray(bytes))),
                new ReaderKind("native MemorySegment", bytes -> {
                    var segment = Arena.ofAuto().allocate(bytes.length);
                    MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
                    return ProtobufBinaryReader.fromMemorySegment(segment);
                }),
                new ReaderKind("heap MemorySegment slice", bytes -> {
                    var padded = new byte[bytes.length + 6];
                    System.arraycopy(bytes, 0, padded, 3, bytes.length);
                    return ProtobufBinaryReader.fromMemorySegment(MemorySegment.ofArray(padded).asSlice(3, bytes.length));
                }),
                new ReaderKind("stream", bytes -> ProtobufBinaryReader.fromStream(new ByteArrayInputStream(bytes))),
                // The smallest buffer fed one byte at a time forces a refill on almost every read
                new ReaderKind("stream with small buffer", bytes -> ProtobufBinaryReader.fromStream(new TrickleInputStream(bytes), true, 16))
        ).map(Arguments::of);
    }

    private static final class TrickleInputStream extends ByteArrayInputStream {
        private TrickleInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(len, 1));
        }
    }

    @Test
    public void testCalculatorMatchesReference() {
        for (var field : fields()) {
            assertEquals(field.referenceSize(), field.size(), field.name());
        }
    }

    @ParameterizedTest
    @MethodSource("writers")
    public void testWriterMatchesReference(WriterKind kind) throws Exception {
        var fields = fields();
        var expected = referenceBytes(fields);
        var writer = kind.factory().create(expected.length);
        for (var field : fields) {
            field.writer().write(writer);
        }
        writer.close();
        assertArrayEquals(expected, kind.extractor().extract(writer));
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderReadsReference(ReaderKind kind) throws Exception {
        var fields = fields();
        var reader = kind.factory().create(referenceBytes(fields));
        for (var field : fields) {
            assertTrue(reader.nextFieldTag(), field.name());
            assertEquals(field.index(), reader.fieldIndex(), field.name());
            field.reader().read(reader);
        }
        assertFalse(reader.nextFieldTag());
    }

    // Every length of a varint64 is followed by enough bytes for the fast path of the readers,
    // which decodes sixteen bytes at once, and must still stop at the end of the varint
    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderReadsVarInt64OfEveryLengthBeforeMoreFields(ReaderKind kind) throws Exception {
        var values = new long[]{1L, 300L, 1L << 21, 1L << 28, 1L << 35, 1706695530123L, 1L << 49, 1L << 56, 1L << 63 >>> 1, -1L};
        var trailer = "trailing field long enough for the fast path";
        var bytes = new ByteArrayOutputStream();
        var output = CodedOutputStream.newInstance(bytes);
        for (var i = 0; i < values.length; i++) {
            output.writeInt64(2 * i + 1, values[i]);
            output.writeString(2 * i + 2, trailer);
        }
        output.flush();

        var reader = kind.factory().create(bytes.toByteArray());
        for (var i = 0; i < values.length; i++) {
            assertTrue(reader.nextFieldTag());
            assertEquals(2 * i + 1, reader.fieldIndex());
            assertEquals(values[i], reader.readInt64Field(), "value " + values[i]);
            assertTrue(reader.nextFieldTag());
            assertEquals(2 * i + 2, reader.fieldIndex());
            assertEquals(trailer, reader.readStringField(true));
        }
        assertFalse(reader.nextFieldTag());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderRejectsEndGroupOutsideOfAnyGroup(ReaderKind kind) throws Exception {
        // Field 1 varint 1, then an end group tag for field 2 that no start group tag opened, then field 3 varint 3
        var reader = kind.factory().create(new byte[]{0x08, 0x01, 0x14, 0x18, 0x03});
        assertTrue(reader.nextFieldTag());
        assertEquals(1, reader.readInt32Field());
        assertThrows(ProtobufDeserializationException.class, reader::nextFieldTag);
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderEndsGroupOnlyInsideIt(ReaderKind kind) throws Exception {
        // Group 2 containing field 1 varint 5, then field 3 varint 3 after the group
        var reader = kind.factory().create(new byte[]{0x13, 0x08, 0x05, 0x14, 0x18, 0x03});
        reader.readStartGroupField(2);
        assertTrue(reader.nextFieldTag());
        assertEquals(5, reader.readInt32Field());
        assertFalse(reader.nextFieldTag());
        reader.readEndGroupField(2);
        assertTrue(reader.nextFieldTag());
        assertEquals(3, reader.readInt32Field());
        assertFalse(reader.nextFieldTag());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderLimitsNestedMessages(ReaderKind kind) throws Exception {
        // Field 1 holds a message whose field 1 holds an empty message: two levels of nesting
        var bytes = new byte[]{0x0A, 0x02, 0x0A, 0x00};
        var reader = kind.factory().create(bytes);
        reader.setRecursionLimit(1);
        assertTrue(reader.nextFieldTag());
        var nested = reader.readLengthDelimitedField();
        assertEquals(1, nested.recursionLimit());
        assertTrue(nested.nextFieldTag());
        assertThrows(ProtobufDeserializationException.class, nested::readLengthDelimitedField);

        var unlimited = kind.factory().create(bytes);
        assertTrue(unlimited.nextFieldTag());
        var first = unlimited.readLengthDelimitedField();
        assertTrue(first.nextFieldTag());
        var second = first.readLengthDelimitedField();
        assertFalse(second.nextFieldTag());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderLimitsNestedUnknownGroups(ReaderKind kind) throws Exception {
        // 150 nested groups with index 1, deeper than the default recursion limit of 100
        var depth = 150;
        var bytes = new byte[depth * 2];
        for (var i = 0; i < depth; i++) {
            bytes[i] = 0x0B;
            bytes[bytes.length - 1 - i] = 0x0C;
        }
        var skipped = kind.factory().create(bytes);
        assertTrue(skipped.nextFieldTag());
        assertThrows(ProtobufDeserializationException.class, skipped::skipUnknownField);

        var read = kind.factory().create(bytes);
        assertTrue(read.nextFieldTag());
        assertThrows(ProtobufDeserializationException.class, read::readUnknownField);

        var raised = kind.factory().create(bytes);
        raised.setRecursionLimit(depth);
        assertTrue(raised.nextFieldTag());
        raised.skipUnknownField();
        assertFalse(raised.nextFieldTag());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testReaderSkipsUnknownFields(ReaderKind kind) throws Exception {
        var fields = fields();
        var reader = kind.factory().create(referenceBytes(fields));
        for (var field : fields) {
            assertTrue(reader.nextFieldTag(), field.name());
            assertEquals(field.index(), reader.fieldIndex(), field.name());
            reader.skipUnknownField();
        }
        assertFalse(reader.nextFieldTag());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testUnknownFieldsRoundTrip(ReaderKind kind) throws Exception {
        var fields = fields();
        var expected = referenceBytes(fields);
        var reader = kind.factory().create(expected);
        var unknownFields = new LinkedHashMap<Integer, ProtobufUnknownValue.Binary>();
        for (var field : fields) {
            assertTrue(reader.nextFieldTag(), field.name());
            var index = reader.fieldIndex();
            unknownFields.put(index, reader.readUnknownField());
        }
        assertFalse(reader.nextFieldTag());

        var size = 0L;
        for (var entry : unknownFields.entrySet()) {
            size += ProtobufBinarySizeCalculator.getUnknownFieldSize(entry.getKey(), entry.getValue());
        }
        assertEquals(expected.length, size);

        var writer = ProtobufBinaryWriter.toBytes(expected.length);
        for (var entry : unknownFields.entrySet()) {
            writer.writeUnknownField(entry.getKey(), entry.getValue());
        }
        assertArrayEquals(expected, writer.toOutput());
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testStrings(ReaderKind kind) throws Exception {
        var values = List.of("", "a", "a\0b", TEXT, "x".repeat(10_000), "é".repeat(5_000) + "🌍".repeat(100));
        var size = 0;
        for (var i = 0; i < values.size(); i++) {
            size += CodedOutputStream.computeStringSize(i + 1, values.get(i));
        }
        var bytes = new byte[size];
        var output = CodedOutputStream.newInstance(bytes);
        for (var i = 0; i < values.size(); i++) {
            output.writeString(i + 1, values.get(i));
        }

        for (var validate : new boolean[]{false, true}) {
            var reader = kind.factory().create(bytes);
            for (var value : values) {
                assertTrue(reader.nextFieldTag());
                assertEquals(value, reader.readStringField(validate));
            }
            assertFalse(reader.nextFieldTag());

            var lazyReader = kind.factory().create(bytes);
            for (var value : values) {
                assertTrue(lazyReader.nextFieldTag());
                assertEquals(value, lazyReader.readLazyStringField(validate).get());
            }
            assertFalse(lazyReader.nextFieldTag());
        }
    }

    @ParameterizedTest
    @MethodSource("readers")
    public void testInvalidUtf8(ReaderKind kind) throws Exception {
        var invalid = new byte[]{(byte) 0xC3, 0x28};
        var bytes = new byte[]{0x0A, 2, invalid[0], invalid[1]};

        var validating = kind.factory().create(bytes);
        assertTrue(validating.nextFieldTag());
        assertThrows(ProtobufDeserializationException.class, () -> validating.readStringField(true));

        var lenient = kind.factory().create(bytes);
        assertTrue(lenient.nextFieldTag());
        assertEquals(new String(invalid, StandardCharsets.UTF_8), lenient.readStringField(false));
    }

    @Test
    public void computesTheUtf8LengthOfStringsLikeTheJdk() {
        var random = new java.util.SplittableRandom(7);
        var samples = new ArrayList<String>(List.of("", "a", "\u00e9", "\u4e2d", "\uD83D\uDE00", "a\uD800b", "\uDC00", "x\uD83D", "\uD83D\uDE00\uD800\uD800\uDC00", TEXT));
        var alphabet = new char[]{'a', '\u00e9', '\u07ff', '\u0800', '\u4e2d', '\uffff', '\uD83D', '\uDE00', '\uD800', '\uDC00'};
        for (var length : new int[]{1, 7, 31, 32, 33, 63, 64, 65, 511, 512, 513, 1500}) {
            for (var round = 0; round < 20; round++) {
                var builder = new StringBuilder(length);
                var asciiPrefix = random.nextInt(length + 1);
                for (var i = 0; i < length; i++) {
                    builder.append(i < asciiPrefix ? (char) ('a' + random.nextInt(26)) : alphabet[random.nextInt(alphabet.length)]);
                }
                samples.add(builder.toString());
            }
            for (var round = 0; round < 20; round++) {
                var builder = new StringBuilder(length + 1);
                while (builder.length() < length) {
                    if (random.nextInt(3) == 0) {
                        builder.append((char) ('a' + random.nextInt(26)));
                    } else {
                        builder.appendCodePoint(0x10000 + random.nextInt(0x100000));
                    }
                }
                samples.add(builder.toString());
                var corrupted = builder.toString().toCharArray();
                corrupted[random.nextInt(corrupted.length)] = alphabet[6 + random.nextInt(4)];
                samples.add(new String(corrupted));
                samples.add(builder.substring(0, builder.length() - 1));
                samples.add(builder.substring(1));
            }
            var latin1 = new char[]{'a', '\u0080', 'é', 'ÿ'};
            for (var round = 0; round < 20; round++) {
                var builder = new StringBuilder(length);
                var asciiPrefix = random.nextInt(length + 1);
                for (var i = 0; i < length; i++) {
                    builder.append(i < asciiPrefix ? (char) ('a' + random.nextInt(26)) : latin1[random.nextInt(latin1.length)]);
                }
                samples.add(builder.toString());
            }
            for (var round = 0; round < 20; round++) {
                var ascii = new StringBuilder(length);
                for (var i = 0; i < length; i++) {
                    ascii.append((char) ('a' + random.nextInt(26)));
                }
                var position = random.nextInt(length);
                samples.add(new StringBuilder(ascii).replace(position, position + 1, "€").toString());
                samples.add(new StringBuilder(ascii).replace(position, position + 1, "😀").toString());
                samples.add(new StringBuilder(ascii).replace(position, position + 1, "\uD83D").toString());
            }
        }
        for (var sample : samples) {
            assertEquals(sample.getBytes(StandardCharsets.UTF_8).length, com.github.auties00.daedalus.typesystem.util.CharsetUtils.utf8Length(sample), sample);
        }
    }
}
