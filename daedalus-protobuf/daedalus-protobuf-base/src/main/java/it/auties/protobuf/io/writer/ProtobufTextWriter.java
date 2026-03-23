package it.auties.protobuf.io.writer;

/**
 * An abstract writer for text-based Protocol Buffer formats (JSON, textproto).
 * <p>
 * Text writers produce human-readable representations of Protocol Buffer messages
 * using field names instead of field numbers. Generated Spec classes provide separate
 * {@code encode(T, ProtobufTextWriter)} overloads that pass field names for each property.
 *
 * @see ProtobufBinaryWriter
 */
public abstract non-sealed class ProtobufTextWriter<OUTPUT> implements ProtobufWriter<OUTPUT> {
    public abstract void writeStartObject();

    public abstract void writeStartObjectProperty(String fieldName);

    public abstract void writeStartObjectElement();

    public abstract void writeEndObject();

    public abstract void writeStartRepeatedProperty(String fieldName);

    public abstract void writeEndRepeatedProperty();

    public abstract void writeFloatProperty(String fieldName, float value);

    public abstract void writeDoubleProperty(String fieldName, double value);

    public abstract void writeBoolProperty(String fieldName, boolean value);

    public abstract void writeInt32Property(String fieldName, int value);

    public abstract void writeUInt32Property(String fieldName, int value);

    public abstract void writeSInt32Property(String fieldName, int value);

    public abstract void writeFixed32Property(String fieldName, int value);

    public abstract void writeSFixed32Property(String fieldName, int value);

    public abstract void writeInt64Property(String fieldName, long value);

    public abstract void writeUInt64Property(String fieldName, long value);

    public abstract void writeSInt64Property(String fieldName, long value);

    public abstract void writeFixed64Property(String fieldName, long value);

    public abstract void writeSFixed64Property(String fieldName, long value);

    public abstract void writeStringProperty(String fieldName, String value);

    public abstract void writeBytesProperty(String fieldName, byte[] value);

    public abstract void writeEnumProperty(String fieldName, int number, String enumName);

    public abstract void writeFloatElement(float value);

    public abstract void writeDoubleElement(double value);

    public abstract void writeBoolElement(boolean value);

    public abstract void writeInt32Element(int value);

    public abstract void writeUInt32Element(int value);

    public abstract void writeSInt32Element(int value);

    public abstract void writeFixed32Element(int value);

    public abstract void writeSFixed32Element(int value);

    public abstract void writeInt64Element(long value);

    public abstract void writeUInt64Element(long value);

    public abstract void writeSInt64Element(long value);

    public abstract void writeFixed64Element(long value);

    public abstract void writeSFixed64Element(long value);

    public abstract void writeStringElement(String value);

    public abstract void writeBytesElement(byte[] value);

    public abstract void writeEnumElement(int number, String enumName);

    public abstract void writeNullElement();
}
