package it.auties.protobuf.io.reader;

/**
 * An abstract reader for text-based Protocol Buffer formats (JSON, textproto).
 * <p>
 * Text readers parse human-readable representations of Protocol Buffer messages
 * using field names instead of field numbers. Generated Spec classes provide separate
 * {@code decode(ProtobufTextReader)} overloads that switch on field names.
 *
 * @see ProtobufBinaryReader
 */
public abstract non-sealed class ProtobufTextReader implements ProtobufReader {

    protected String propertyName;

    public abstract void readStartObject();

    public abstract void readEndObject();

    public abstract void readStartArray();

    public abstract void readEndArray();

    /**
     * Reads the next property name in the current object.
     * Returns {@code true} if a property was read, {@code false} if the object is finished.
     * The name is accessible via {@link #propertyName()}.
     */
    public abstract boolean readPropertyName();

    /**
     * Returns the last property name read by {@link #readPropertyName()}.
     */
    public String propertyName() {
        return propertyName;
    }

    public abstract boolean isFinished();

    public abstract void skipUnknownProperty();

    public abstract float readFloatProperty();

    public abstract double readDoubleProperty();

    public abstract boolean readBoolProperty();

    public abstract int readInt32Property();

    public abstract int readUInt32Property();

    public abstract int readSInt32Property();

    public abstract int readFixed32Property();

    public abstract int readSFixed32Property();

    public abstract long readInt64Property();

    public abstract long readUInt64Property();

    public abstract long readSInt64Property();

    public abstract long readFixed64Property();

    public abstract long readSFixed64Property();

    public abstract String readStringProperty();

    public abstract byte[] readBytesProperty();

    public abstract String readEnumNameProperty();

    public abstract int readEnumNumberProperty();
}
