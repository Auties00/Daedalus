package it.auties.protobuf.io.writer;

import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * A sealed interface representing a Protocol Buffer writer.
 * <p>
 * Two families of writers are supported:
 * <ul>
 *     <li>{@link ProtobufBinaryWriter} — writes the binary wire format (byte arrays, ByteBuffers, MemorySegments, OutputStreams)</li>
 *     <li>{@link ProtobufTextWriter} — writes text-based formats (JSON, textproto)</li>
 * </ul>
 * <p>
 * Generated Spec classes provide separate overloaded {@code encode} methods for each writer type.
 */
public sealed interface ProtobufWriter<OUTPUT> extends AutoCloseable permits ProtobufBinaryWriter, ProtobufTextWriter {
    static ProtobufBinaryWriter<byte[]> toBytes(int length) {
        return ProtobufBinaryWriter.toBytes(length);
    }

    static ProtobufBinaryWriter<byte[]> toBytes(byte[] bytes, int offset) {
        return ProtobufBinaryWriter.toBytes(bytes, offset);
    }

    static ProtobufBinaryWriter<ByteBuffer> toHeapBuffer(int length) {
        return ProtobufBinaryWriter.toHeapBuffer(length);
    }

    static ProtobufBinaryWriter<ByteBuffer> toDirectBuffer(int length) {
        return ProtobufBinaryWriter.toDirectBuffer(length);
    }

    static ProtobufBinaryWriter<ByteBuffer> toBuffer(ByteBuffer buffer) {
        return ProtobufBinaryWriter.toBuffer(buffer);
    }

    static ProtobufBinaryWriter<MemorySegment> toMemorySegment(MemorySegment segment) {
        return ProtobufBinaryWriter.toMemorySegment(segment);
    }

    static ProtobufBinaryWriter<OutputStream> toStream(OutputStream stream) {
        return ProtobufBinaryWriter.toStream(stream);
    }

    static ProtobufBinaryWriter<OutputStream> toStream(OutputStream stream, int tempBufferLength) {
        return ProtobufBinaryWriter.toStream(stream, tempBufferLength);
    }

    static ProtobufJsonStringWriter toJsonString() {
        return new ProtobufJsonStringWriter();
    }

    static ProtobufJsonByteArrayWriter toJsonBytes() {
        return new ProtobufJsonByteArrayWriter();
    }

    static ProtobufTextFormatStringWriter toTextFormatString() {
        return new ProtobufTextFormatStringWriter();
    }

    static ProtobufTextFormatByteArrayWriter toTextFormatBytes() {
        return new ProtobufTextFormatByteArrayWriter();
    }

    OUTPUT toOutput();
}
