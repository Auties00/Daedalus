package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDeserializer;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufSerializer;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "preview"})
@ProtobufMixin
public final class StringMixin {
    @ProtobufDeserializer
    public static String ofNullable(ProtobufBinaryReader reader) {
        var length = reader.readLengthDelimitedPropertyLength();
        return switch (reader.rawDataTypePreference()) {
            case BYTE_ARRAY -> {
                var source = reader.readRawBytes(length);
                yield new String(source, StandardCharsets.UTF_8);
            }

            case BYTE_BUFFER -> {
                var source = reader.readRawBuffer(length);
                if(source.hasArray()) {
                    yield new String(source.array(), source.arrayOffset() + source.position(), source.remaining(), StandardCharsets.UTF_8);
                }else {
                    var copy = new byte[length];
                    source.get(copy);
                    yield new String(copy, StandardCharsets.UTF_8);
                }
            }

            case MEMORY_SEGMENT -> {
                var source = reader.readRawMemorySegment(length);
                yield source.getString(0, StandardCharsets.UTF_8);
            }
        };
    }

    @ProtobufSerializer
    public static void write(String value, ProtobufBinaryWriter<?> writer) {
        if (value != null) {
            var source = value.getBytes(StandardCharsets.UTF_8);
            writer.writeLengthDelimitedPropertyLength(source.length);
            writer.writeRawBytes(source);
        }
    }

    @ProtobufDeserializer
    public static Supplier<String> read(ProtobufBinaryReader reader) {
        var length = reader.readLengthDelimitedPropertyLength();
        return switch (reader.rawDataTypePreference()) {
            case BYTE_ARRAY -> {
                var source = reader.readRawBytes(length);
                yield LazyConstant.of(() -> new String(source, StandardCharsets.UTF_8));
            }

            case BYTE_BUFFER -> {
                var source = reader.readRawBuffer(length);
                yield LazyConstant.of(() -> {
                    if (source.hasArray()) {
                        return new String(source.array(), source.arrayOffset() + source.position(), source.remaining(), StandardCharsets.UTF_8);
                    } else {
                        var copy = new byte[source.remaining()];
                        source.get(copy);
                        return new String(copy, StandardCharsets.UTF_8);
                    }
                });
            }

            case MEMORY_SEGMENT -> {
                var source = reader.readRawMemorySegment(length);
                yield LazyConstant.of(() -> source.getString(0, StandardCharsets.UTF_8));
            }
        };
    }

    @ProtobufSerializer
    public static void write(Supplier<String> value, ProtobufBinaryWriter<?> writer) {
        if (value != null) {
            write(value.get(), writer);
        }
    }
}
