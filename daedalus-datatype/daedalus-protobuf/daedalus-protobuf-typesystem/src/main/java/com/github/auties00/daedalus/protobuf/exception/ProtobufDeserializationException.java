package com.github.auties00.daedalus.protobuf.exception;

import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;

/**
 * A checked exception thrown when a Protocol Buffers message cannot be deserialized, for example
 * because its binary or text encoding is malformed.
 *
 * <p>Instances are created exclusively through the static factory methods of this class, each of
 * which documents the key of the {@code messages} resource bundle entry that holds its text.
 */
public final class ProtobufDeserializationException extends ProtobufException {
    /**
     * Constructs a new {@code ProtobufDeserializationException} with the given message key.
     *
     * @param key the resource bundle key of the message
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     * @param args the arguments substituted into the message
     */
    private ProtobufDeserializationException(String key, Throwable cause, Object... args) {
        super(key, cause, args);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message ended unexpectedly.
     *
     * <p>Bundle key: {@code deserialization.truncated.message}.
     *
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the message
     *         ended unexpectedly during deserialization
     */
    public static ProtobufDeserializationException truncatedMessage() {
        return new ProtobufDeserializationException("deserialization.truncated.message", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message ended unexpectedly.
     *
     * <p>Bundle key: {@code deserialization.truncated.message}.
     *
     * @param cause the cause
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the message
     *         ended unexpectedly during deserialization
     */
    public static ProtobufDeserializationException truncatedMessage(Throwable cause) {
        return new ProtobufDeserializationException("deserialization.truncated.message", cause);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message contains a malformed var int.
     *
     * <p>Bundle key: {@code deserialization.malformed.var.int}.
     *
     * @return a {@code ProtobufDeserializationException} with a predefined message stating that the var int is malformed
     */
    public static ProtobufDeserializationException malformedVarInt() {
        return new ProtobufDeserializationException("deserialization.malformed.var.int", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that the wire type in the serialized
     * message is invalid.
     *
     * <p>Bundle key: {@code deserialization.invalid.wire.type}.
     *
     * @param wireType the invalid wire type that was encountered during deserialization
     * @return a {@code ProtobufDeserializationException} with a predefined message specifying the invalid wire type
     */
    public static ProtobufDeserializationException invalidWireType(int wireType) {
        return new ProtobufDeserializationException("deserialization.invalid.wire.type", null, wireType);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a group in a serialized protobuf
     * message was improperly terminated. This occurs when the group was closed with a field index that
     * does not match the field index of the previously opened group.
     *
     * <p>Bundle key: {@code deserialization.invalid.end.object}.
     *
     * @param actualFieldIndex   the actual field index encountered when the group was closed
     * @param expectedFieldIndex the expected field index corresponding to the previously opened group
     * @return a {@code ProtobufDeserializationException} with a detailed message explaining the mismatch
     */
    public static ProtobufDeserializationException invalidEndObject(int actualFieldIndex, int expectedFieldIndex) {
        return new ProtobufDeserializationException("deserialization.invalid.end.object", null, actualFieldIndex, expectedFieldIndex);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that the input nests
     * messages or groups deeper than the recursion limit of the reader.
     *
     * <p>Decoding a nested message or group recurses, so the limit protects the decoder from
     * inputs that would overflow the stack. Like in the official protobuf library, every
     * nested message, map entry and group counts, including groups that are skipped or kept
     * as unknown fields.
     *
     * <p>Bundle key: {@code deserialization.recursion.limit.exceeded}.
     *
     * @param recursionLimit the recursion limit of the reader
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the limit
     */
    public static ProtobufDeserializationException recursionLimitExceeded(int recursionLimit) {
        return new ProtobufDeserializationException("deserialization.recursion.limit.exceeded", null, recursionLimit);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message closed
     * a group that was never opened.
     *
     * <p>An end group tag is only valid inside a group, so finding one outside of any group,
     * for example at the top level of a message or inside a length prefixed message, means
     * that the input is malformed, truncated, or not a message of the expected type.
     *
     * <p>Bundle key: {@code deserialization.unmatched.end.group}.
     *
     * @param fieldIndex the field index of the end group tag
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the field index
     */
    public static ProtobufDeserializationException unmatchedEndGroup(int fieldIndex) {
        return new ProtobufDeserializationException("deserialization.unmatched.end.group", null, fieldIndex);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message
     * expected a group to begin but did not encounter a valid start object at the specified field index.
     *
     * <p>Bundle key: {@code deserialization.invalid.start.object}.
     *
     * @param fieldIndex the index of the field where the start of the group was expected
     * @return a {@code ProtobufDeserializationException} with a predefined message identifying the problem at the specified field index
     */
    public static ProtobufDeserializationException invalidStartObject(int fieldIndex) {
        return new ProtobufDeserializationException("deserialization.invalid.start.object", null, fieldIndex);
    }

    /**
     * Creates a new ProtobufDeserializationException indicating that a message opened a group but failed
     * to close it. This typically happens when a well-formed group is not properly terminated in the serialized
     * protobuf data.
     *
     * <p>Bundle key: {@code deserialization.malformed.group}.
     *
     * @return a ProtobufDeserializationException with a predefined message describing the error
     */
    public static ProtobufDeserializationException malformedGroup() {
        return new ProtobufDeserializationException("deserialization.malformed.group", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message specified
     * a negative length for a length-delimited field during deserialization.
     *
     * <p>Bundle key: {@code deserialization.negative.length}.
     *
     * @param size the negative length value that was encountered
     * @return a {@code ProtobufDeserializationException} with a message explaining the error
     */
    public static ProtobufDeserializationException negativeLength(long size) {
        return new ProtobufDeserializationException("deserialization.negative.length", null, size);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a message specified
     * an invalid field index during deserialization.
     *
     * <p>Bundle key: {@code deserialization.invalid.field.index}.
     *
     * @param index the invalid field index that was encountered
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the invalid index
     */
    public static ProtobufDeserializationException invalidFieldIndex(int index) {
        return new ProtobufDeserializationException("deserialization.invalid.field.index", null, index);
    }

    /**
     * Returns the {@code ProtobufDeserializationException} that reports the given exception,
     * caught while a message was being decoded, which is the exception itself if it is already a
     * {@code ProtobufDeserializationException}, or one indicating that a converter of a field of
     * the message, or its builder, threw the given checked exception otherwise.
     *
     * <p>An unchecked exception is not wrapped: it is thrown by this method.
     *
     * <p>Bundle key: {@code deserialization.converter.failure}.
     *
     * @param messageName the name of the message
     * @param cause the exception caught while the message was being decoded
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the error
     * @throws RuntimeException if {@code cause} is an unchecked exception, which is thrown as it is
     */
    @SuppressWarnings("unused") // Used by the generated Spec classes
    public static ProtobufDeserializationException converterFailure(String messageName, Exception cause) {
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }

        if (cause instanceof ProtobufDeserializationException deserializationException) {
            return deserializationException;
        }

        return new ProtobufDeserializationException("deserialization.converter.failure", cause, messageName, cause);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a decoded message,
     * or one of the messages nested in it, ended without a value for one of its required
     * fields.
     *
     * <p>Bundle key: {@code deserialization.missing.required.field}.
     *
     * @param cause the exception thrown by the builder of the message whose required field is
     *              not set, which must not be {@code null}
     * @return a {@code ProtobufDeserializationException} with a detailed message specifying the
     *         message and the missing field
     * @throws NullPointerException if {@code cause} is {@code null}
     */
    @SuppressWarnings("unused") // Used by ProtobufMessageDeserializationGenerator
    public static ProtobufDeserializationException missingRequiredField(ProtobufUninitializedMessageException cause) {
        return new ProtobufDeserializationException("deserialization.missing.required.field", cause, cause.messageName(), cause.fieldName());
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a string
     * field contains bytes that are not valid UTF-8.
     *
     * <p>Bundle key: {@code deserialization.invalid.utf8}.
     *
     * @return a {@code ProtobufDeserializationException} for invalid UTF-8 data
     */
    public static ProtobufDeserializationException invalidUtf8() {
        return new ProtobufDeserializationException("deserialization.invalid.utf8", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a field tag was read
     * while the value of the previous field had not been consumed yet.
     *
     * <p>Bundle key: {@code deserialization.field.tag.already.read}.
     *
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the invalid state
     */
    public static ProtobufDeserializationException fieldTagAlreadyRead() {
        return new ProtobufDeserializationException("deserialization.field.tag.already.read", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that the value of a field
     * was read before its tag.
     *
     * <p>Bundle key: {@code deserialization.missing.field.tag}.
     *
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the invalid state
     */
    public static ProtobufDeserializationException missingFieldTag() {
        return new ProtobufDeserializationException("deserialization.missing.field.tag", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a length delimited field was too large to be deserialized.
     * <p>
     * This can happen, for example, if a caller attempts to read a length delimited field with a {@code length > Integer.MAX_VALUE} from a {@link ProtobufBinaryReader} backed by a {@code byte[]}.
     *
     * <p>Bundle key: {@code deserialization.length.delimited.field.overflow}.
     *
     * @param length the length of the length delimited property that was too large to be deserialized
     * @return a {@code ProtobufDeserializationException} with a detailed message describing the invalid state
     */
    public static ProtobufDeserializationException lengthDelimitedFieldOverflow(long length) {
        return new ProtobufDeserializationException("deserialization.length.delimited.field.overflow", null, length);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * does not open a nested message where one was expected.
     *
     * <p>Bundle key: {@code deserialization.expected.message.start}.
     *
     * @param found the text that was found instead, which is empty at the end of the text
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedMessageStart(String found) {
        return new ProtobufDeserializationException("deserialization.expected.message.start", null, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * closes a nested message with a delimiter that does not match the one that opened it.
     *
     * <p>Bundle key: {@code deserialization.expected.message.end}.
     *
     * @param expected the delimiter that closes the message
     * @param found the text that was found instead
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedMessageEnd(String expected, String found) {
        return new ProtobufDeserializationException("deserialization.expected.message.end", null, expected, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * continues after the end of its top level message, like a delimiter that closes a message
     * that was never opened.
     *
     * <p>Bundle key: {@code deserialization.expected.end.of.text}.
     *
     * @param found the text that was found instead of the end of the text
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedEndOfText(String found) {
        return new ProtobufDeserializationException("deserialization.expected.end.of.text", null, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * does not contain the name of a field where one was expected.
     *
     * <p>Bundle key: {@code deserialization.expected.field.name}.
     *
     * @param found the text that was found instead
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedFieldName(String found) {
        return new ProtobufDeserializationException("deserialization.expected.field.name", null, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * ends before a nested message, a list or a value is complete.
     *
     * <p>Bundle key: {@code deserialization.unexpected.end.of.text}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unexpectedEndOfText() {
        return new ProtobufDeserializationException("deserialization.unexpected.end.of.text", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text reader was
     * asked to close a message while no message was open, which is a bug of the caller.
     *
     * <p>Bundle key: {@code deserialization.unmatched.end.message}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unmatchedEndMessage() {
        return new ProtobufDeserializationException("deserialization.unmatched.end.message", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * sets a field that its type does not declare.
     *
     * <p>Like the official protobuf library, a message decoded from the text format rejects the
     * names it does not know, as the text format cannot tell a misspelled name apart from a
     * field added by a newer version of the schema.
     *
     * <p>Bundle key: {@code deserialization.unknown.field}.
     *
     * @param messageName the name of the message
     * @param fieldName the name of the field
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unknownField(String messageName, String fieldName) {
        return new ProtobufDeserializationException("deserialization.unknown.field", null, messageName, fieldName);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * sets a field that cannot be read from the text format, because its value is converted by
     * a converter that reads the binary format directly.
     *
     * <p>Bundle key: {@code deserialization.binary.only.field}.
     *
     * @param messageName the name of the message
     * @param fieldName the name of the field
     * @param converter the name of the converter
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    @SuppressWarnings("unused") // Used by ProtobufMessageTextPartialDeserializationGenerator
    public static ProtobufDeserializationException binaryOnlyField(String messageName, String fieldName, String converter) {
        return new ProtobufDeserializationException("deserialization.binary.only.field", null, messageName, fieldName, converter);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * sets an enum field to a name that the enum does not declare.
     *
     * <p>Bundle key: {@code deserialization.unknown.enum.constant}.
     *
     * @param enumName the name of the enum
     * @param constantName the name that was found
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unknownEnumConstant(String enumName, String constantName) {
        return new ProtobufDeserializationException("deserialization.unknown.enum.constant", null, enumName, constantName);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * ends in the middle of a block that must be skipped.
     *
     * <p>Bundle key: {@code deserialization.unterminated.block}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unterminatedBlock() {
        return new ProtobufDeserializationException("deserialization.unterminated.block", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * contains a string that is not closed.
     *
     * <p>Bundle key: {@code deserialization.unterminated.string}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException unterminatedString() {
        return new ProtobufDeserializationException("deserialization.unterminated.string", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * contains something other than a number where a numeric value was expected.
     *
     * <p>Bundle key: {@code deserialization.expected.numeric.value}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedNumericValue() {
        return new ProtobufDeserializationException("deserialization.expected.numeric.value", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that an integer in a text
     * encoded message does not fit in 64 bits.
     *
     * <p>Bundle key: {@code deserialization.integer.overflow}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException integerOverflow() {
        return new ProtobufDeserializationException("deserialization.integer.overflow", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a hexadecimal integer in
     * a text encoded message has no digits after its {@code 0x} prefix.
     *
     * <p>Bundle key: {@code deserialization.expected.hex.digits}.
     *
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedHexDigits() {
        return new ProtobufDeserializationException("deserialization.expected.hex.digits", null);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a text encoded message
     * does not contain a value of the expected type.
     *
     * <p>Bundle key: {@code deserialization.expected.value}, or
     * {@code deserialization.expected.value.found} when {@code found} is not {@code null}.
     *
     * @param type the name of the expected type, such as {@code float}
     * @param found the text that was found instead, or {@code null} if it is not available
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedValue(String type, String found) {
        return found == null
                ? new ProtobufDeserializationException("deserialization.expected.value", null, type)
                : new ProtobufDeserializationException("deserialization.expected.value.found", null, type, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a boolean field of a
     * text encoded message contains a number other than {@code 0} or {@code 1}.
     *
     * <p>Bundle key: {@code deserialization.expected.boolean.number}, or
     * {@code deserialization.expected.boolean.number.found} when {@code found} is not {@code null}.
     *
     * @param found the text that was found instead, or {@code null} if it is not available
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException expectedBooleanNumber(String found) {
        return found == null
                ? new ProtobufDeserializationException("deserialization.expected.boolean.number", null)
                : new ProtobufDeserializationException("deserialization.expected.boolean.number.found", null, found);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a floating point field
     * of a text encoded message contains an octal literal.
     *
     * <p>Bundle key: {@code deserialization.octal.floating.point}.
     *
     * @param type the name of the field type, such as {@code double}
     * @param text the octal literal
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException octalFloatingPoint(String type, String text) {
        return new ProtobufDeserializationException("deserialization.octal.floating.point", null, type, text);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that a floating point field
     * of a text encoded message contains text that is not a valid number.
     *
     * <p>Bundle key: {@code deserialization.invalid.floating.point}.
     *
     * @param type the name of the field type, such as {@code double}
     * @param text the invalid text
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException invalidFloatingPoint(String type, String text) {
        return new ProtobufDeserializationException("deserialization.invalid.floating.point", null, type, text);
    }

    /**
     * Creates a new {@code ProtobufDeserializationException} indicating that an integer in a text
     * encoded message is out of the range of its field type.
     *
     * <p>Bundle key: {@code deserialization.value.out.of.range}.
     *
     * @param type the name of the field type, such as {@code int32}
     * @return a {@code ProtobufDeserializationException} describing the error
     */
    public static ProtobufDeserializationException valueOutOfRange(String type) {
        return new ProtobufDeserializationException("deserialization.value.out.of.range", null, type);
    }
}
