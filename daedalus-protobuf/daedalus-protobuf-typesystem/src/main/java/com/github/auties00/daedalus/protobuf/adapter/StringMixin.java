package com.github.auties00.daedalus.protobuf.adapter;

import com.github.auties00.daedalus.protobuf.io.ProtobufIODataType;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "preview"})
@TypeMixin
public final class StringMixin {
    private StringMixin() {
        throw new UnsupportedOperationException("StringMixin is a mixin and cannot be instantiated");
    }

    @TypeDeserializer
    public static String ofNullable(ProtobufBinaryReader reader) {
        var length = reader.readLengthDelimitedPropertyLength();
        return switch (reader.rawDataTypePreference()) {
            case ProtobufIODataType.BYTE_ARRAY -> {
                var source = reader.readRawBytes(length);
                yield new String(source, StandardCharsets.UTF_8);
            }

            case ProtobufIODataType.BYTE_BUFFER -> {
                var source = reader.readRawBuffer(length);
                if(source.hasArray()) {
                    yield new String(source.array(), source.arrayOffset() + source.position(), source.remaining(), StandardCharsets.UTF_8);
                }else {
                    // FIXME: Potentially large alloc
                    var copy = new byte[length];
                    source.get(copy);
                    yield new String(copy, StandardCharsets.UTF_8);
                }
            }

            case ProtobufIODataType.MEMORY_SEGMENT -> {
                var source = reader.readRawMemorySegment(length);
                yield source.getString(0, StandardCharsets.UTF_8);
            }
        };
    }

    @TypeSerializer
    public static void write(String value, ProtobufBinaryWriter<?> writer) {
        if (value != null) {
            var source = value.getBytes(StandardCharsets.UTF_8);
            writer.writeLengthDelimitedPropertyLength(source.length);
            writer.writeRawBytes(source);
        }
    }

    @TypeDeserializer
    public static Supplier<String> read(ProtobufBinaryReader reader) {
        var length = reader.readLengthDelimitedPropertyLength();
        return switch (reader.rawDataTypePreference()) {
            case ProtobufIODataType.BYTE_ARRAY -> {
                var source = reader.readRawBytes(length);
                yield LazyConstant.of(() -> new String(source, StandardCharsets.UTF_8));
            }

            case ProtobufIODataType.BYTE_BUFFER -> {
                var source = reader.readRawBuffer(length);
                yield LazyConstant.of(() -> {
                    if (source.hasArray()) {
                        return new String(source.array(), source.arrayOffset() + source.position(), source.remaining(), StandardCharsets.UTF_8);
                    } else {
                        // FIXME: Potentially large alloc
                        var copy = new byte[source.remaining()];
                        source.get(copy);
                        return new String(copy, StandardCharsets.UTF_8);
                    }
                });
            }

            case ProtobufIODataType.MEMORY_SEGMENT -> {
                var source = reader.readRawMemorySegment(length);
                yield LazyConstant.of(() -> source.getString(0, StandardCharsets.UTF_8));
            }
        };
    }

    @TypeSerializer
    public static void write(Supplier<String> value, ProtobufBinaryWriter<?> writer) {
        if (value != null) {
            write(value.get(), writer);
        }
    }
}
