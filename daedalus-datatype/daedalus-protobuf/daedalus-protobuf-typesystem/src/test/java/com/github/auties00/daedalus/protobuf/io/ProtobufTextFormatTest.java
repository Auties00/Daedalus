package com.github.auties00.daedalus.protobuf.io;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeAccumulator;
import com.github.auties00.daedalus.protobuf.io.size.ProtobufTextSizeCalculator;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields;
import com.github.auties00.daedalus.protobuf.model.ProtobufUnknownValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.TextFormat;
import com.google.protobuf.UnknownFieldSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the text readers and writers against {@code protobuf-java}, the reference
 * implementation of the text format.
 *
 * <p>The messages are {@link DynamicMessage} instances of a schema that has a field of every
 * type: the writers must print them exactly like the {@code TextFormat} printer does, and the
 * readers must parse the texts printed by it, and the hand written texts that use the rest of
 * the grammar, into the same messages that the {@code TextFormat} parser produces.
 */
public class ProtobufTextFormatTest {
    private static final Descriptors.Descriptor TYPE = createType();
    private static final Descriptors.EnumDescriptor COLOR = TYPE.getFile().findEnumTypeByName("Color");

    /**
     * Creates the schema of the test messages.
     *
     * @return the descriptor of the test message
     */
    private static Descriptors.Descriptor createType() {
        var message = DescriptorProto.newBuilder().setName("Sample");
        var types = List.of(
                FieldDescriptorProto.Type.TYPE_INT32, FieldDescriptorProto.Type.TYPE_UINT32, FieldDescriptorProto.Type.TYPE_SINT32,
                FieldDescriptorProto.Type.TYPE_FIXED32, FieldDescriptorProto.Type.TYPE_SFIXED32, FieldDescriptorProto.Type.TYPE_INT64,
                FieldDescriptorProto.Type.TYPE_UINT64, FieldDescriptorProto.Type.TYPE_SINT64, FieldDescriptorProto.Type.TYPE_FIXED64,
                FieldDescriptorProto.Type.TYPE_SFIXED64, FieldDescriptorProto.Type.TYPE_FLOAT, FieldDescriptorProto.Type.TYPE_DOUBLE,
                FieldDescriptorProto.Type.TYPE_BOOL, FieldDescriptorProto.Type.TYPE_STRING, FieldDescriptorProto.Type.TYPE_BYTES
        );
        var names = List.of("i32", "u32", "s32", "f32", "sf32", "i64", "u64", "s64", "f64", "sf64", "fl", "db", "b", "s", "by");
        for (var i = 0; i < types.size(); i++) {
            message.addField(FieldDescriptorProto.newBuilder()
                    .setName(names.get(i))
                    .setNumber(i + 1)
                    .setType(types.get(i))
                    .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                    .setProto3Optional(true)
                    .setOneofIndex(i));
            message.addOneofDecl(com.google.protobuf.DescriptorProtos.OneofDescriptorProto.newBuilder().setName("_" + names.get(i)));
        }
        message.addField(FieldDescriptorProto.newBuilder().setName("color").setNumber(16).setType(FieldDescriptorProto.Type.TYPE_ENUM).setTypeName(".test.Color").setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL));
        message.addField(FieldDescriptorProto.newBuilder().setName("child").setNumber(17).setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".test.Sample").setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL));
        message.addField(FieldDescriptorProto.newBuilder().setName("numbers").setNumber(18).setType(FieldDescriptorProto.Type.TYPE_INT32).setLabel(FieldDescriptorProto.Label.LABEL_REPEATED));
        message.addField(FieldDescriptorProto.newBuilder().setName("children").setNumber(19).setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".test.Sample").setLabel(FieldDescriptorProto.Label.LABEL_REPEATED));
        message.addField(FieldDescriptorProto.newBuilder().setName("colors").setNumber(20).setType(FieldDescriptorProto.Type.TYPE_ENUM).setTypeName(".test.Color").setLabel(FieldDescriptorProto.Label.LABEL_REPEATED));
        message.addField(FieldDescriptorProto.newBuilder().setName("texts").setNumber(21).setType(FieldDescriptorProto.Type.TYPE_STRING).setLabel(FieldDescriptorProto.Label.LABEL_REPEATED));
        var color = EnumDescriptorProto.newBuilder()
                .setName("Color")
                .addValue(EnumValueDescriptorProto.newBuilder().setName("RED").setNumber(0))
                .addValue(EnumValueDescriptorProto.newBuilder().setName("GREEN").setNumber(1))
                .addValue(EnumValueDescriptorProto.newBuilder().setName("BLUE").setNumber(2));
        var file = FileDescriptorProto.newBuilder()
                .setName("test.proto")
                .setPackage("test")
                .setSyntax("proto3")
                .addMessageType(message)
                .addEnumType(color)
                .build();
        try {
            return Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[0]).findMessageTypeByName("Sample");
        } catch (Descriptors.DescriptorValidationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Descriptors.FieldDescriptor field(String name) {
        return TYPE.findFieldByName(name);
    }

    /**
     * Parses a text with the reference implementation.
     *
     * @param text the text
     * @return the message
     * @throws TextFormat.ParseException if the text is malformed
     */
    private static DynamicMessage parse(String text) throws TextFormat.ParseException {
        var builder = DynamicMessage.newBuilder(TYPE);
        TextFormat.merge(text, builder);
        return builder.build();
    }

    /**
     * Returns the messages whose texts are compared with the ones printed by the reference
     * implementation.
     *
     * @return the messages
     */
    static Stream<Arguments> messages() {
        var empty = DynamicMessage.newBuilder(TYPE).build();
        var minimums = DynamicMessage.newBuilder(TYPE)
                .setField(field("i32"), Integer.MIN_VALUE)
                .setField(field("u32"), -1)
                .setField(field("s32"), Integer.MIN_VALUE)
                .setField(field("f32"), -1)
                .setField(field("sf32"), Integer.MIN_VALUE)
                .setField(field("i64"), Long.MIN_VALUE)
                .setField(field("u64"), -1L)
                .setField(field("s64"), Long.MIN_VALUE)
                .setField(field("f64"), -1L)
                .setField(field("sf64"), Long.MIN_VALUE)
                .setField(field("fl"), Float.NEGATIVE_INFINITY)
                .setField(field("db"), -0.0)
                .setField(field("b"), false)
                .setField(field("s"), "")
                .setField(field("by"), ByteString.EMPTY)
                .build();
        var allBytes = new byte[256];
        for (var i = 0; i < allBytes.length; i++) {
            allBytes[i] = (byte) i;
        }
        var values = DynamicMessage.newBuilder(TYPE)
                .setField(field("i32"), 42)
                .setField(field("u32"), Integer.MAX_VALUE)
                .setField(field("i64"), 1L << 40)
                .setField(field("fl"), 1.0e-10f)
                .setField(field("db"), Double.NaN)
                .setField(field("b"), true)
                .setField(field("s"), "quote \" apostrophe ' backslash \\ tab \t newline \n bell \u0007 del \u007f héllo 🌍 \0")
                .setField(field("by"), ByteString.copyFrom(allBytes))
                .setField(field("color"), COLOR.findValueByName("GREEN"))
                .build();
        var nested = DynamicMessage.newBuilder(TYPE)
                .setField(field("i32"), 1)
                .setField(field("child"), DynamicMessage.newBuilder(TYPE)
                        .setField(field("s"), "inner")
                        .setField(field("child"), DynamicMessage.newBuilder(TYPE).setField(field("db"), 1.5e300).build())
                        .build())
                .addRepeatedField(field("numbers"), 1)
                .addRepeatedField(field("numbers"), -2)
                .addRepeatedField(field("numbers"), 3)
                .addRepeatedField(field("children"), DynamicMessage.newBuilder(TYPE).build())
                .addRepeatedField(field("children"), DynamicMessage.newBuilder(TYPE).setField(field("b"), true).build())
                .addRepeatedField(field("colors"), COLOR.findValueByName("BLUE"))
                .addRepeatedField(field("colors"), COLOR.findValueByNumber(0))
                .addRepeatedField(field("texts"), "a")
                .addRepeatedField(field("texts"), "")
                .build();
        return Stream.of(
                Arguments.of("empty", empty),
                Arguments.of("minimums", minimums),
                Arguments.of("values", values),
                Arguments.of("nested", nested)
        );
    }

    /**
     * Writes the given message with the given writer, field by field, ordered by number.
     *
     * @param message the message
     * @param writer  the writer
     * @throws ProtobufSerializationException if the writer fails
     */
    private static void write(DynamicMessage message, ProtobufTextWriter<?> writer) throws ProtobufSerializationException {
        for (var entry : message.getAllFields().entrySet()) {
            var field = entry.getKey();
            if (field.isRepeated()) {
                for (var value : (List<?>) entry.getValue()) {
                    writeValue(field, value, writer);
                }
            } else {
                writeValue(field, entry.getValue(), writer);
            }
        }
        for (var entry : message.getUnknownFields().asMap().entrySet()) {
            var fields = new ProtobufUnknownFields();
            for (var value : entry.getValue().getVarintList()) {
                fields.add(entry.getKey(), new ProtobufUnknownValue.Binary.VarInt(value));
            }
            for (var value : entry.getValue().getFixed32List()) {
                fields.add(entry.getKey(), new ProtobufUnknownValue.Binary.Fixed32(value));
            }
            for (var value : entry.getValue().getFixed64List()) {
                fields.add(entry.getKey(), new ProtobufUnknownValue.Binary.Fixed64(value));
            }
            for (var value : entry.getValue().getLengthDelimitedList()) {
                fields.add(entry.getKey(), new ProtobufUnknownValue.Binary.LengthDelimited.ByteArrayBacked(value.toByteArray()));
            }
            writeBinaryUnknownFields(fields, writer);
        }
    }

    /**
     * Writes every value of the binary fields of the given unknown fields by their numbers, like
     * the reference printer writes unknown fields.
     *
     * @param fields the unknown fields
     * @param writer the writer
     * @throws ProtobufSerializationException if the writer fails
     */
    private static void writeBinaryUnknownFields(ProtobufUnknownFields fields, ProtobufTextWriter<?> writer) throws ProtobufSerializationException {
        for (var field : fields.binaryFields()) {
            for (var value : field) {
                writer.writeUnknownField(field.index(), value);
            }
        }
    }

    /**
     * Checks that the text size of the given unknown fields is the size of the text they write,
     * on multiple lines and on a single line.
     *
     * @param fields the unknown fields
     * @throws ProtobufSerializationException if the writer fails
     */
    private static void assertTextSize(ProtobufUnknownFields fields) throws ProtobufSerializationException {
        var multiLine = ProtobufTextWriter.toString(16);
        fields.encodeText(multiLine);
        assertEquals(multiLine.toOutput().getBytes(StandardCharsets.UTF_8).length, fields.textSize(false));
        var singleLine = ProtobufTextWriter.toString(16).setSingleLine(true);
        fields.encodeText(singleLine);
        assertEquals(singleLine.toOutput().getBytes(StandardCharsets.UTF_8).length, fields.textSize(true));
    }

    private static void writeValue(Descriptors.FieldDescriptor field, Object value, ProtobufTextWriter<?> writer) throws ProtobufSerializationException {
        var name = field.getName();
        switch (field.getType()) {
            case INT32 -> writer.writeInt32Field(name, (Integer) value);
            case UINT32 -> writer.writeUInt32Field(name, (Integer) value);
            case SINT32 -> writer.writeSInt32Field(name, (Integer) value);
            case FIXED32 -> writer.writeFixed32Field(name, (Integer) value);
            case SFIXED32 -> writer.writeSFixed32Field(name, (Integer) value);
            case INT64 -> writer.writeInt64Field(name, (Long) value);
            case UINT64 -> writer.writeUInt64Field(name, (Long) value);
            case SINT64 -> writer.writeSInt64Field(name, (Long) value);
            case FIXED64 -> writer.writeFixed64Field(name, (Long) value);
            case SFIXED64 -> writer.writeSFixed64Field(name, (Long) value);
            case FLOAT -> writer.writeFloatField(name, (Float) value);
            case DOUBLE -> writer.writeDoubleField(name, (Double) value);
            case BOOL -> writer.writeBoolField(name, (Boolean) value);
            case STRING -> writer.writeStringField(name, (String) value);
            case BYTES -> writer.writeBytesField(name, ((ByteString) value).toByteArray());
            case ENUM -> {
                var constant = (Descriptors.EnumValueDescriptor) value;
                var known = COLOR.findValueByNumber(constant.getNumber());
                writer.writeEnumField(name, constant.getNumber(), known != null ? known.getName() : null);
            }
            case MESSAGE -> {
                writer.writeStartMessageField(name);
                write((DynamicMessage) value, writer);
                writer.writeEndMessageField();
            }
            default -> throw new AssertionError(field.getType());
        }
    }

    /**
     * Reads a message of the test type with the given reader.
     *
     * @param reader the reader
     * @return the message
     * @throws ProtobufDeserializationException if the reader fails
     */
    private static DynamicMessage read(ProtobufTextReader reader, Descriptors.Descriptor type) throws ProtobufDeserializationException {
        var builder = DynamicMessage.newBuilder(type);
        reader.readStartMessage();
        while (reader.nextField()) {
            var field = type.findFieldByName(reader.fieldName());
            assertNotNull(field, () -> "Unknown field " + reader.fieldName());
            if (field.isRepeated()) {
                if (reader.readStartList()) {
                    while (reader.nextListElement()) {
                        builder.addRepeatedField(field, readValue(reader, field));
                    }
                } else {
                    builder.addRepeatedField(field, readValue(reader, field));
                }
            } else {
                builder.setField(field, readValue(reader, field));
            }
        }
        reader.readEndMessage();
        return builder.build();
    }

    private static Object readValue(ProtobufTextReader reader, Descriptors.FieldDescriptor field) throws ProtobufDeserializationException {
        return switch (field.getType()) {
            case INT32 -> reader.readInt32Field();
            case UINT32 -> reader.readUInt32Field();
            case SINT32 -> reader.readSInt32Field();
            case FIXED32 -> reader.readFixed32Field();
            case SFIXED32 -> reader.readSFixed32Field();
            case INT64 -> reader.readInt64Field();
            case UINT64 -> reader.readUInt64Field();
            case SINT64 -> reader.readSInt64Field();
            case FIXED64 -> reader.readFixed64Field();
            case SFIXED64 -> reader.readSFixed64Field();
            case FLOAT -> reader.readFloatField();
            case DOUBLE -> reader.readDoubleField();
            case BOOL -> reader.readBoolField();
            case STRING -> reader.readStringField();
            case BYTES -> ByteString.copyFrom(reader.readBytesField());
            case ENUM -> reader.isNextValueIdentifier()
                    ? field.getEnumType().findValueByName(reader.readIdentifierField())
                    : field.getEnumType().findValueByNumberCreatingIfUnknown(reader.readInt32Field());
            case MESSAGE -> read(reader, field.getMessageType());
            default -> throw new AssertionError(field.getType());
        };
    }

    record ReaderKind(String name, Function<String, ProtobufTextReader> factory) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<ReaderKind> readerKinds() {
        return List.of(
                new ReaderKind("string", ProtobufTextReader::fromString),
                new ReaderKind("bytes", text -> ProtobufTextReader.fromBytes(text.getBytes(StandardCharsets.UTF_8))),
                new ReaderKind("directBuffer", text -> {
                    var bytes = text.getBytes(StandardCharsets.UTF_8);
                    return ProtobufTextReader.fromBuffer(ByteBuffer.allocateDirect(bytes.length).put(bytes).flip());
                }),
                new ReaderKind("segment", text -> {
                    var bytes = text.getBytes(StandardCharsets.UTF_8);
                    var segment = Arena.ofAuto().allocate(Math.max(bytes.length, 1)).asSlice(0, bytes.length);
                    return ProtobufTextReader.fromMemorySegment(segment.copyFrom(MemorySegment.ofArray(bytes)));
                }),
                new ReaderKind("stream", text -> ProtobufTextReader.fromStream(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))),
                new ReaderKind("tinyStream", text -> ProtobufTextReader.fromStream(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, 11))
        );
    }

    static Stream<Arguments> messagesAndReaders() {
        return messages().flatMap(arguments -> readerKinds().stream().map(kind -> Arguments.of(arguments.get()[0], arguments.get()[1], kind)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("messages")
    public void writesLikeTheReferencePrinter(String name, DynamicMessage message) throws Exception {
        var multiLine = ProtobufTextWriter.toString(16);
        write(message, multiLine);
        assertEquals(TextFormat.printer().printToString(message), multiLine.toOutput());

        var singleLine = ProtobufTextWriter.toString(16).setSingleLine(true);
        write(message, singleLine);
        assertEquals(TextFormat.printer().emittingSingleLine(true).printToString(message).trim(), singleLine.toOutput());

        var unicode = ProtobufTextWriter.toString(16).setEscapeNonAscii(false);
        write(message, unicode);
        var expected = TextFormat.printer().escapingNonAscii(false).printToString(message);
        assertEquals(parse(expected), parse(unicode.toOutput()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("messages")
    public void countingWriterCountsTheWrittenBytes(String name, DynamicMessage message) throws Exception {
        for (var singleLine : new boolean[]{false, true}) {
            for (var escapeNonAscii : new boolean[]{false, true}) {
                var text = ProtobufTextWriter.toString(16).setSingleLine(singleLine).setEscapeNonAscii(escapeNonAscii);
                write(message, text);
                var size = ProtobufTextWriter.toSize(singleLine, escapeNonAscii, 0, false);
                write(message, size);
                assertEquals(text.toOutput().getBytes(StandardCharsets.UTF_8).length, (long) size.toOutput());
            }
        }
    }

    @Test
    public void calculatorMeasuresStringsLikeTheWriterWritesThem() throws Exception {
        var values = List.of("", "plain", "quote \" backslash \\ tab \t bell \u0007 del \u007f", "héllo € 🌍", "lone \uD83C high", "lone \uDF0D low", "\uDF0D\uD83C", "end \uD83C");
        for (var value : values) {
            for (var escapeNonAscii : new boolean[]{false, true}) {
                var text = ProtobufTextWriter.toString(16).setEscapeNonAscii(escapeNonAscii);
                text.writeStringField("s", value);
                var accumulator = new ProtobufTextSizeAccumulator(false, escapeNonAscii);
                accumulator.addLine(0, ProtobufTextSizeCalculator.getNameSize("s") + 4 + accumulator.getStringSize(value));
                assertEquals(text.toOutput().getBytes(StandardCharsets.UTF_8).length, accumulator.size());
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("messages")
    public void everyWriterProducesTheSameText(String name, DynamicMessage message) throws Exception {
        var expected = TextFormat.printer().printToString(message);
        var length = expected.getBytes(StandardCharsets.UTF_8).length;

        var bytes = ProtobufTextWriter.toBytes(length);
        write(message, bytes);
        assertEquals(expected, new String(bytes.toOutput(), StandardCharsets.UTF_8));

        var offsetBytes = ProtobufTextWriter.toBytes(new byte[length + 5], 3);
        write(message, offsetBytes);
        assertEquals(expected, new String(offsetBytes.toOutput(), StandardCharsets.UTF_8));

        var heap = ProtobufTextWriter.toHeapBuffer(length);
        write(message, heap);
        assertEquals(expected, StandardCharsets.UTF_8.decode(heap.toOutput()).toString());

        var direct = ProtobufTextWriter.toDirectBuffer(length + 10);
        write(message, direct);
        assertEquals(expected, StandardCharsets.UTF_8.decode(direct.toOutput()).toString());

        try (var arena = Arena.ofConfined()) {
            var segment = ProtobufTextWriter.toMemorySegment(arena.allocate(length + 1));
            write(message, segment);
            assertEquals(expected, new String(segment.toOutput().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
        }

        var stream = new ByteArrayOutputStream();
        try (var streamWriter = ProtobufTextWriter.toStream(stream, true, 3)) {
            write(message, streamWriter);
        }
        assertEquals(expected, stream.toString(StandardCharsets.UTF_8));

        var chars = ProtobufTextWriter.toCharBuffer(CharBuffer.allocate(expected.length() + 2));
        write(message, chars);
        assertEquals(expected, chars.toOutput().toString());
    }

    @Test
    public void fixedSizeWritersFailWhenFull() {
        var message = DynamicMessage.newBuilder(TYPE).setField(field("s"), "too long").build();
        assertThrows(ProtobufSerializationException.class, () -> write(message, ProtobufTextWriter.toBytes(4)));
        assertThrows(ProtobufSerializationException.class, () -> write(message, ProtobufTextWriter.toHeapBuffer(4)));
        assertThrows(ProtobufSerializationException.class, () -> write(message, ProtobufTextWriter.toCharBuffer(CharBuffer.allocate(4))));
    }

    @Test
    public void writesUnknownFieldsLikeTheReferencePrinter() throws Exception {
        var nested = UnknownFieldSet.newBuilder()
                .addField(1, UnknownFieldSet.Field.newBuilder().addVarint(5).build())
                .build();
        var unknown = UnknownFieldSet.newBuilder()
                .addField(100, UnknownFieldSet.Field.newBuilder().addVarint(-1).addVarint(150).build())
                .addField(101, UnknownFieldSet.Field.newBuilder().addFixed32(42).build())
                .addField(102, UnknownFieldSet.Field.newBuilder().addFixed64(-2L).build())
                .addField(103, UnknownFieldSet.Field.newBuilder().addLengthDelimited(nested.toByteString()).build())
                .addField(104, UnknownFieldSet.Field.newBuilder().addLengthDelimited(ByteString.copyFrom(new byte[]{(byte) 0xFF, 'a'})).build())
                .build();
        var message = DynamicMessage.newBuilder(TYPE).setField(field("i32"), 1).setUnknownFields(unknown).build();
        var writer = ProtobufTextWriter.toString(16);
        write(message, writer);
        assertEquals(TextFormat.printer().printToString(message), writer.toOutput());
    }

    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("messagesAndReaders")
    public void readsTheTextOfTheReferencePrinter(String name, DynamicMessage message, ReaderKind kind) throws Exception {
        var multiLine = TextFormat.printer().printToString(message);
        assertEquals(message, read(kind.factory().apply(multiLine), TYPE));
        var singleLine = TextFormat.printer().emittingSingleLine(true).printToString(message);
        assertEquals(message, read(kind.factory().apply(singleLine), TYPE));
        var unicode = TextFormat.printer().escapingNonAscii(false).printToString(message);
        assertEquals(message, read(kind.factory().apply(unicode), TYPE));
    }

    /**
     * Returns hand written texts that use the parts of the grammar that the reference printer
     * does not.
     *
     * @return the texts
     */
    static Stream<Arguments> grammar() {
        var texts = List.of(
                "i32: 0x7fffffff u32: 4294967295 s32: -0x80000000 f32: 0xFFFFFFFF sf32: -017",
                "i64: -9223372036854775808 u64: 18446744073709551615 s64: 0x7FFFFFFFFFFFFFFF f64: 01777777777777777777777 sf64: -1",
                "fl: 1e-5 db: -1.5E-300",
                "fl: -inf db: Infinity",
                "fl: NaN db: nan",
                "fl: 1.5f db: .5",
                "fl: 10 db: 10",
                "b: True",
                "b: t",
                "b: 0",
                "b: 1",
                "b: False",
                "s: 'single \"quoted\"' 'adjacent' \"double\"",
                "s: \"\\x41\\101\\u00e9\\U0001F30D\\303\\251\\n\\'\\\"\\?\"",
                "by: \"\\000\\377\\x7f\" 'more'",
                "color: BLUE",
                "color: 2",
                "colors: [RED, 1, BLUE] colors: GREEN",
                "numbers: [] numbers: [1, -2, 0x3] numbers: 4",
                "texts: [\"a\", 'b' \"c\"]",
                "child { i32: 1 } children: [{ b: true }, < i32: 2 >] children < >",
                "child: < child: { s: \"deep\" } >",
                "# comment\ni32: 1 # trailing comment\n# another\nu32: 2",
                "i32: 1, u32: 2; s32: 3",
                "child { i32: 1; }, children { },",
                "i32:1 child{s:\"x\"}"
        );
        return texts.stream().flatMap(text -> readerKinds().stream().map(kind -> Arguments.of(text, kind)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("grammar")
    public void readsTheWholeGrammar(String text, ReaderKind kind) throws Exception {
        var expected = parse(text);
        var actual = read(kind.factory().apply(text), TYPE);
        assertEquals(expected, actual);
    }

    @Test
    public void readsBracketedNames() throws Exception {
        for (var kind : readerKinds()) {
            var reader = kind.factory().apply("[com.example.ext]: 1 [ type.googleapis.com / pkg.Type ] { } [my-domain.com/x] < >");
            reader.readStartMessage();
            assertTrue(reader.nextField());
            assertEquals("[com.example.ext]", reader.fieldName());
            assertEquals(1, reader.readInt32Field());
            assertTrue(reader.nextField());
            assertEquals("[type.googleapis.com/pkg.Type]", reader.fieldName());
            reader.skipUnknownField();
            assertTrue(reader.nextField());
            assertEquals("[my-domain.com/x]", reader.fieldName());
            reader.skipUnknownField();
            assertFalse(reader.nextField());
            reader.readEndMessage();
        }
    }

    @Test
    public void skipsEveryKindOfValue() throws Exception {
        var text = "a: -1e-5 b: 'x' \"y\" c { d: [1, 2] e < f: g > } h: [{}, {}] i: IDENT j: -inf k: 0x1F z: 1";
        for (var kind : readerKinds()) {
            var reader = kind.factory().apply(text);
            reader.readStartMessage();
            var skipped = 0;
            while (reader.nextField()) {
                if (reader.fieldName().equals("z")) {
                    assertEquals(1, reader.readInt32Field());
                } else {
                    reader.skipUnknownField();
                    skipped++;
                }
            }
            reader.readEndMessage();
            assertEquals(7, skipped, kind.name());
        }
    }

    static Stream<Arguments> malformed() {
        var texts = List.of(
                "i32: 2147483648",
                "i32: -2147483649",
                "u32: -1",
                "u32: 4294967296",
                "u64: 18446744073709551616",
                "i32: 1.5",
                "i32: abc",
                "fl: abc",
                "b: 2",
                "b: yes",
                "child { i32: 1",
                "child { i32: 1 >",
                "child < i32: 1 }",
                "i32: 1 }",
                "s: \"unterminated",
                "numbers: [1, 2",
                ": 1",
                "child: 1"
        );
        return texts.stream().flatMap(text -> readerKinds().stream().map(kind -> Arguments.of(text, kind)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("malformed")
    public void rejectsMalformedTexts(String text, ReaderKind kind) {
        assertThrows(TextFormat.ParseException.class, () -> parse(text));
        assertThrows(ProtobufDeserializationException.class, () -> read(kind.factory().apply(text), TYPE));
    }

    /**
     * Reads every field of the given reader into unknown fields.
     *
     * @param reader the reader
     * @return the unknown fields
     * @throws ProtobufDeserializationException if the reader fails
     */
    private static ProtobufUnknownFields readUnknownFields(ProtobufTextReader reader) throws ProtobufDeserializationException {
        var fields = new ProtobufUnknownFields();
        reader.readStartMessage();
        while (reader.nextField()) {
            reader.readUnknownField(fields);
        }
        reader.readEndMessage();
        return fields;
    }

    @Test
    public void keepsUnknownFieldsInATextStore() throws Exception {
        var text = "a: -1e-5 b: 'x' \"y\" c { d: [1, 2] e < f: RED > } h: [{}, { i: 0x1F }] j: -inf [ext.name]: 1 k: \"\\377\" a: true";
        var nested = new ProtobufUnknownFields();
        nested.add("d", new ProtobufUnknownValue.Text.Scalar("1"));
        nested.add("d", new ProtobufUnknownValue.Text.Scalar("2"));
        var deepest = new ProtobufUnknownFields();
        deepest.add("f", new ProtobufUnknownValue.Text.Scalar("RED"));
        nested.add("e", new ProtobufUnknownValue.Text.Message(deepest));
        var listed = new ProtobufUnknownFields();
        listed.add("i", new ProtobufUnknownValue.Text.Scalar("0x1F"));
        var expected = new ProtobufUnknownFields();
        expected.add("a", new ProtobufUnknownValue.Text.Scalar("-1e-5"));
        expected.add("b", new ProtobufUnknownValue.Text.Bytes("xy".getBytes(StandardCharsets.UTF_8)));
        expected.add("c", new ProtobufUnknownValue.Text.Message(nested));
        expected.add("h", new ProtobufUnknownValue.Text.Message(new ProtobufUnknownFields()));
        expected.add("h", new ProtobufUnknownValue.Text.Message(listed));
        expected.add("j", new ProtobufUnknownValue.Text.Scalar("-inf"));
        expected.add("[ext.name]", new ProtobufUnknownValue.Text.Scalar("1"));
        expected.add("k", new ProtobufUnknownValue.Text.Bytes(new byte[]{(byte) 0xFF}));
        expected.add("a", new ProtobufUnknownValue.Text.Scalar("true"));
        for (var kind : readerKinds()) {
            var fields = readUnknownFields(kind.factory().apply(text));
            assertEquals(expected, fields, kind.name());

            var writer = ProtobufTextWriter.toString(16);
            fields.encodeText(writer);
            assertEquals(expected, readUnknownFields(kind.factory().apply(writer.toOutput())), kind.name());
        }

        assertTextSize(expected);
        var writer = ProtobufTextWriter.toString(16);
        expected.encodeText(writer);
        assertEquals("a: -1e-5\na: true\nb: \"xy\"\nc {\n  d: 1\n  d: 2\n  e {\n    f: RED\n  }\n}\nh {\n}\nh {\n  i: 0x1F\n}\nj: -inf\n[ext.name]: 1\nk: \"\\377\"\n", writer.toOutput());
    }

    @Test
    public void decodesAndEncodesTextWithoutSchema() throws Exception {
        var text = "name: \"x\" child { id: 1 tags: [\"a\", 'b'] } [pkg.ext]: RED id: -0x10";
        var expected = new ProtobufUnknownFields();
        expected.add("name", new ProtobufUnknownValue.Text.Bytes("x".getBytes(StandardCharsets.UTF_8)));
        var child = new ProtobufUnknownFields();
        child.add("id", new ProtobufUnknownValue.Text.Scalar("1"));
        child.add("tags", new ProtobufUnknownValue.Text.Bytes("a".getBytes(StandardCharsets.UTF_8)));
        child.add("tags", new ProtobufUnknownValue.Text.Bytes("b".getBytes(StandardCharsets.UTF_8)));
        expected.add("child", new ProtobufUnknownValue.Text.Message(child));
        expected.add("[pkg.ext]", new ProtobufUnknownValue.Text.Scalar("RED"));
        expected.add("id", new ProtobufUnknownValue.Text.Scalar("-0x10"));
        for (var kind : readerKinds()) {
            var decoded = ProtobufUnknownFields.decodeText(kind.factory().apply(text));
            assertEquals(expected, decoded, kind.name());

            var writer = ProtobufTextWriter.toString(16);
            decoded.encodeText(writer);
            assertEquals(expected, ProtobufUnknownFields.decodeText(kind.factory().apply(writer.toOutput())), kind.name());
        }
        assertTextSize(expected);
    }

    @Test
    public void writesBinaryUnknownFieldsOnlyInTheBinaryFormat() throws Exception {
        var message = DynamicMessage.newBuilder(TYPE)
                .setField(field("i32"), -5)
                .setField(field("f32"), 42)
                .setField(field("f64"), -2L)
                .setField(field("s"), "text \u00e9")
                .setField(field("child"), DynamicMessage.newBuilder(TYPE).setField(field("b"), true).build())
                .addRepeatedField(field("numbers"), 1)
                .addRepeatedField(field("numbers"), 2)
                .build();
        var bytes = message.toByteArray();
        var decoded = ProtobufUnknownFields.decodeBinary(ProtobufBinaryReader.fromBytes(bytes));

        var binary = ProtobufBinaryWriter.toBytes(Math.toIntExact(decoded.wireSize()));
        decoded.encodeBinary(binary);
        assertArrayEquals(bytes, binary.toOutput());

        var text = ProtobufTextWriter.toString(16);
        decoded.encodeText(text);
        assertEquals("", text.toOutput());
        assertEquals(0, decoded.textSize(false));

        var dump = ProtobufTextWriter.toString(16);
        writeBinaryUnknownFields(decoded, dump);
        assertEquals(TextFormat.printer().printToString(UnknownFieldSet.parseFrom(bytes)), dump.toOutput());
    }

    @Test
    public void writesTextUnknownFieldsOnlyInTheTextFormat() throws Exception {
        var fields = ProtobufUnknownFields.decodeText(ProtobufTextReader.fromString("name: \"x\" child { id: 1 }"));
        fields.add(1, new ProtobufUnknownValue.Binary.VarInt(150));
        var binary = ProtobufBinaryWriter.toBytes(Math.toIntExact(fields.wireSize()));
        fields.encodeBinary(binary);
        assertArrayEquals(new byte[]{0x08, (byte) 0x96, 0x01}, binary.toOutput());

        var text = ProtobufTextWriter.toString(16);
        fields.encodeText(text);
        assertEquals("name: \"x\"\nchild {\n  id: 1\n}\n", text.toOutput());
    }

    @Test
    public void computesTheTextSize() throws Exception {
        var allBytes = new byte[256];
        for (var i = 0; i < allBytes.length; i++) {
            allBytes[i] = (byte) i;
        }
        var deep = new ProtobufUnknownFields();
        deep.add("caf\u00e9", new ProtobufUnknownValue.Text.Scalar("\uD83C\uDF0D"));
        var nested = new ProtobufUnknownFields();
        nested.add("deep", new ProtobufUnknownValue.Text.Message(deep));
        nested.add("empty", new ProtobufUnknownValue.Text.Message(new ProtobufUnknownFields()));
        var fields = new ProtobufUnknownFields();
        fields.add("bytes", new ProtobufUnknownValue.Text.Bytes(allBytes));
        fields.add("nested", new ProtobufUnknownValue.Text.Message(nested));
        fields.add("[pkg.ext]", new ProtobufUnknownValue.Text.Scalar("-1e-5"));
        assertTextSize(fields);
        assertTextSize(new ProtobufUnknownFields());
    }

    @Test
    public void writesUnknownEnumValuesByNumber() throws Exception {
        var writer = ProtobufTextWriter.toString(16);
        writer.writeEnumField("color", 7, null);
        writer.writeEnumField("color", 1, "GREEN");
        assertEquals("color: 7\ncolor: GREEN\n", writer.toOutput());
    }

    @Test
    public void readsTheValuesThatOnlyTheReferenceImplementationInCppAccepts() throws Exception {
        for (var kind : readerKinds()) {
            var reader = kind.factory().apply("a: -nan b: 0x10 c: 010 d: -0x10");
            reader.readStartMessage();
            assertTrue(reader.nextField());
            assertTrue(Double.isNaN(reader.readDoubleField()));
            assertTrue(reader.nextField());
            assertEquals(16f, reader.readFloatField());
            assertTrue(reader.nextField());
            assertEquals(8d, reader.readDoubleField());
            assertTrue(reader.nextField());
            assertEquals(-16d, reader.readDoubleField());
            assertFalse(reader.nextField());
            reader.readEndMessage();
        }
    }

    @Test
    public void enforcesTheRecursionLimit() {
        var text = "child { ".repeat(5) + "} ".repeat(5);
        for (var kind : readerKinds()) {
            var reader = kind.factory().apply(text);
            reader.setRecursionLimit(4);
            assertThrows(ProtobufDeserializationException.class, () -> read(reader, TYPE));
            var unlimited = kind.factory().apply(text);
            assertDoesNotThrow(() -> read(unlimited, TYPE));
        }
    }
}
