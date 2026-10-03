package com.github.auties00.daedalus.protobuf.exception;

import java.io.IOException;

/**
 * A checked exception thrown when a Protocol Buffers message cannot be serialized, for example
 * because the underlying output cannot be written.
 *
 * <p>Instances are created exclusively through the static factory methods of this class, each of
 * which documents the key of the {@code messages} resource bundle entry that holds its text.
 */
public final class ProtobufSerializationException extends ProtobufException {
    /**
     * Constructs a new {@code ProtobufSerializationException} with the given message key.
     *
     * @param key the resource bundle key of the message
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     * @param args the arguments substituted into the message
     */
    private ProtobufSerializationException(String key, Throwable cause, Object... args) {
        super(key, cause, args);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a size calculation error
     * occurred because there is not enough space remaining for the message after serialization.
     * This exception is intended to report underlying issues in the library, so it should hopefully never be thrown.
     *
     * <p>Bundle key: {@code serialization.underflow}.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException underflow() {
        return new ProtobufSerializationException("serialization.underflow", null);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a size calculation error
     * occurred because the message is larger than the space that was reserved for it.
     * This exception is intended to report underlying issues in the library, so it should hopefully never be thrown.
     *
     * <p>Bundle key: {@code serialization.overflow}.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException overflow() {
        return new ProtobufSerializationException("serialization.overflow", null);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a message cannot be
     * serialized because one of its required fields has no value.
     *
     * <p>Bundle key: {@code serialization.missing.required.field}.
     *
     * @param fieldName the name of the required field
     * @return a {@code ProtobufSerializationException} with a detailed message specifying the missing field
     */
    @SuppressWarnings("unused") // Used by ProtobufMessageSerializationGenerator
    public static ProtobufSerializationException missingRequiredField(String fieldName) {
        return new ProtobufSerializationException("serialization.missing.required.field", null, fieldName);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a repeated field contains
     * a {@code null} element, or an element converted to {@code null}.
     *
     * <p>Like in the official protobuf library, a repeated field cannot hold {@code null}, as
     * the wire format has no way to represent it.
     *
     * <p>Bundle key: {@code serialization.null.repeated.element}.
     *
     * @param fieldName the name of the repeated field
     * @return a {@code ProtobufSerializationException} with a detailed message specifying the field
     */
    public static ProtobufSerializationException nullRepeatedElement(String fieldName) {
        return new ProtobufSerializationException("serialization.null.repeated.element", null, fieldName);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a map field contains a
     * {@code null} key, or a key converted to {@code null}.
     *
     * <p>Like in the official protobuf library, a map field cannot hold a {@code null} key.
     *
     * <p>Bundle key: {@code serialization.null.map.key}.
     *
     * @param fieldName the name of the map field
     * @return a {@code ProtobufSerializationException} with a detailed message specifying the field
     */
    public static ProtobufSerializationException nullMapKey(String fieldName) {
        return new ProtobufSerializationException("serialization.null.map.key", null, fieldName);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a map field contains a
     * {@code null} value, or a value converted to {@code null}.
     *
     * <p>Like in the official protobuf library, a map field cannot hold a {@code null} value:
     * writing its key alone would decode as the default value of the value type.
     *
     * <p>Bundle key: {@code serialization.null.map.value}.
     *
     * @param fieldName the name of the map field
     * @return a {@code ProtobufSerializationException} with a detailed message specifying the field
     */
    public static ProtobufSerializationException nullMapValue(String fieldName) {
        return new ProtobufSerializationException("serialization.null.map.value", null, fieldName);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that an attempt was made
     * to write a length-delimited block with a negative length.
     *
     * <p>Bundle key: {@code serialization.negative.length}.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException negativeLength() {
        return new ProtobufSerializationException("serialization.negative.length", null);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that the text format of a
     * message cannot be returned as a string, because its UTF-8 encoding is larger than an
     * array can hold.
     *
     * <p>Bundle key: {@code serialization.text.too.large}.
     *
     * @param size the size of the UTF-8 encoding of the text
     * @param maxSize the largest size that an array can hold
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException textTooLarge(long size, int maxSize) {
        return new ProtobufSerializationException("serialization.text.too.large", null, size, maxSize);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that the text format of a
     * message does not fit in the output of a text writer, whose capacity is fixed.
     *
     * <p>Bundle key: {@code serialization.output.full}.
     *
     * @param capacity the capacity of the output
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException outputFull(long capacity) {
        return new ProtobufSerializationException("serialization.output.full", null, capacity);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a field cannot be written
     * in the text format, because its value is converted by a converter that writes the binary
     * format directly.
     *
     * <p>Bundle key: {@code serialization.binary.only.field}.
     *
     * @param fieldName the name of the field
     * @param converter the name of the converter
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    @SuppressWarnings("unused") // Used by ProtobufMessageTextSerializationGenerator
    public static ProtobufSerializationException binaryOnlyField(String fieldName, String converter) {
        return new ProtobufSerializationException("serialization.binary.only.field", null, fieldName, converter);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that a text writer was asked to
     * close a message while no message was open, which is a bug of the caller.
     *
     * <p>Bundle key: {@code serialization.unmatched.end.message}.
     *
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException unmatchedEndMessage() {
        return new ProtobufSerializationException("serialization.unmatched.end.message", null);
    }

    /**
     * Creates a {@code ProtobufSerializationException} indicating that the output stream a message
     * is being written to failed.
     *
     * <p>Bundle key: {@code serialization.output.stream.failure}.
     *
     * @param cause the exception thrown by the output stream
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     */
    public static ProtobufSerializationException outputStreamFailure(IOException cause) {
        return new ProtobufSerializationException("serialization.output.stream.failure", cause);
    }

    /**
     * Returns the {@code ProtobufSerializationException} that reports the given exception,
     * caught while a message was being serialized or sized, which is the exception itself if it
     * is already a {@code ProtobufSerializationException}, or one indicating that a converter of
     * a field of the message threw the given checked exception otherwise.
     *
     * <p>An unchecked exception is not wrapped: it is thrown by this method.
     *
     * <p>Bundle key: {@code serialization.converter.failure}.
     *
     * @param messageName the name of the message
     * @param cause the exception caught while the message was being serialized or sized
     * @return a {@code ProtobufSerializationException} with a detailed message describing the error
     * @throws RuntimeException if {@code cause} is an unchecked exception, which is thrown as it is
     */
    @SuppressWarnings("unused") // Used by the generated Spec classes
    public static ProtobufSerializationException converterFailure(String messageName, Exception cause) {
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        if (cause instanceof ProtobufSerializationException serializationException) {
            return serializationException;
        }

        return new ProtobufSerializationException("serialization.converter.failure", cause, messageName, cause);
    }
}
