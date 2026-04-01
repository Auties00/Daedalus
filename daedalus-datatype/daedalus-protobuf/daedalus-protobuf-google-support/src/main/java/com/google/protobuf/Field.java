package com.google.protobuf;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * A single field of a message type.
 *
 * <p>New usages of this message as an alternative to FieldDescriptorProto are
 * strongly discouraged. This message does not reliability preserve all
 * information necessary to model the schema and preserve semantics. Instead
 * make use of FileDescriptorSet which preserves the necessary information.
 *
 * <p>Protobuf type {@code com.google.protobuf.Field}
 */
@ProtobufMessage
public final class Field {

    /**
     * Basic field types.
     *
     * <p>Protobuf enum {@code com.google.protobuf.Field.Kind}
     */
    @ProtobufEnum
    public enum Kind {
        /**
         * Field type unknown.
         */
        @ProtobufEnum.Constant(index = 0)
        TYPE_UNKNOWN,

        /**
         * Field type double.
         */
        @ProtobufEnum.Constant(index = 1)
        TYPE_DOUBLE,

        /**
         * Field type float.
         */
        @ProtobufEnum.Constant(index = 2)
        TYPE_FLOAT,

        /**
         * Field type int64.
         */
        @ProtobufEnum.Constant(index = 3)
        TYPE_INT64,

        /**
         * Field type uint64.
         */
        @ProtobufEnum.Constant(index = 4)
        TYPE_UINT64,

        /**
         * Field type int32.
         */
        @ProtobufEnum.Constant(index = 5)
        TYPE_INT32,

        /**
         * Field type fixed64.
         */
        @ProtobufEnum.Constant(index = 6)
        TYPE_FIXED64,

        /**
         * Field type fixed32.
         */
        @ProtobufEnum.Constant(index = 7)
        TYPE_FIXED32,

        /**
         * Field type bool.
         */
        @ProtobufEnum.Constant(index = 8)
        TYPE_BOOL,

        /**
         * Field type string.
         */
        @ProtobufEnum.Constant(index = 9)
        TYPE_STRING,

        /**
         * Field type group. Proto2 syntax only, and deprecated.
         */
        @ProtobufEnum.Constant(index = 10)
        TYPE_GROUP,

        /**
         * Field type message.
         */
        @ProtobufEnum.Constant(index = 11)
        TYPE_MESSAGE,

        /**
         * Field type bytes.
         */
        @ProtobufEnum.Constant(index = 12)
        TYPE_BYTES,

        /**
         * Field type uint32.
         */
        @ProtobufEnum.Constant(index = 13)
        TYPE_UINT32,

        /**
         * Field type enum.
         */
        @ProtobufEnum.Constant(index = 14)
        TYPE_ENUM,

        /**
         * Field type sfixed32.
         */
        @ProtobufEnum.Constant(index = 15)
        TYPE_SFIXED32,

        /**
         * Field type sfixed64.
         */
        @ProtobufEnum.Constant(index = 16)
        TYPE_SFIXED64,

        /**
         * Field type sint32.
         */
        @ProtobufEnum.Constant(index = 17)
        TYPE_SINT32,

        /**
         * Field type sint64.
         */
        @ProtobufEnum.Constant(index = 18)
        TYPE_SINT64;
    }

    /**
     * Whether a field is optional, required, or repeated.
     *
     * <p>Protobuf enum {@code com.google.protobuf.Field.Cardinality}
     */
    @ProtobufEnum
    public enum Cardinality {
        /**
         * For fields with unknown cardinality.
         */
        @ProtobufEnum.Constant(index = 0)
        CARDINALITY_UNKNOWN,

        /**
         * For optional fields.
         */
        @ProtobufEnum.Constant(index = 1)
        CARDINALITY_OPTIONAL,

        /**
         * For required fields. Proto2 syntax only.
         */
        @ProtobufEnum.Constant(index = 2)
        CARDINALITY_REQUIRED,

        /**
         * For repeated fields.
         */
        @ProtobufEnum.Constant(index = 3)
        CARDINALITY_REPEATED;
    }

    /**
     * The field type.
     *
     * <p><code>Kind kind = 1;</code>
     */
    @ProtobufMessage.EnumField(index = 1)
    Field.Kind kind;

    /**
     * The field cardinality.
     *
     * <p><code>Cardinality cardinality = 2;</code>
     */
    @ProtobufMessage.EnumField(index = 2)
    Field.Cardinality cardinality;

    /**
     * The field number.
     *
     * <p><code>int32 number = 3;</code>
     */
    @ProtobufMessage.Int32Field(index = 3)
    int number;

    /**
     * The field name.
     *
     * <p><code>string name = 4;</code>
     */
    @ProtobufMessage.StringField(index = 4)
    String name;

    /**
     * The field type URL, without the scheme, for message or enumeration
     * types. Example: {@code "type.googleapis.com/com.google.protobuf.Timestamp"}.
     *
     * <p><code>string type_url = 6;</code>
     */
    @ProtobufMessage.StringField(index = 6)
    String typeUrl;

    /**
     * The index of the field type in {@code Type.oneofs}, for message or enumeration
     * types. The first type has index 1; zero means the type is not in the list.
     *
     * <p><code>int32 oneof_index = 7;</code>
     */
    @ProtobufMessage.Int32Field(index = 7)
    int oneofIndex;

    /**
     * Whether to use alternative packed wire representation.
     *
     * <p><code>bool packed = 8;</code>
     */
    @ProtobufMessage.BoolField(index = 8)
    boolean packed;

    /**
     * The protocol buffer options.
     *
     * <p><code>repeated Option options = 9;</code>
     */
    @ProtobufMessage.MessageField(index = 9)
    List<Option> options;

    /**
     * The field JSON name.
     *
     * <p><code>string json_name = 10;</code>
     */
    @ProtobufMessage.StringField(index = 10)
    String jsonName;

    /**
     * The string value of the default value of this field. Proto2 syntax only.
     *
     * <p><code>string default_value = 11;</code>
     */
    @ProtobufMessage.StringField(index = 11)
    String defaultValue;

    Field(
            Field.Kind kind,
            Field.Cardinality cardinality,
            int number,
            String name,
            String typeUrl,
            int oneofIndex,
            boolean packed,
            List<Option> options,
            String jsonName,
            String defaultValue
    ) {
        this.kind = kind;
        this.cardinality = cardinality;
        this.number = number;
        this.name = name;
        this.typeUrl = typeUrl;
        this.oneofIndex = oneofIndex;
        this.packed = packed;
        this.options = options;
        this.jsonName = jsonName;
        this.defaultValue = defaultValue;
    }

    public Field.Kind kind() {
        return kind;
    }

    public Field.Cardinality cardinality() {
        return cardinality;
    }

    public int number() {
        return number;
    }

    public String name() {
        return name;
    }

    public String typeUrl() {
        return typeUrl;
    }

    public int oneofIndex() {
        return oneofIndex;
    }

    public boolean packed() {
        return packed;
    }

    public SequencedCollection<Option> options() {
        return Collections.unmodifiableSequencedCollection(options);
    }

    public String jsonName() {
        return jsonName;
    }

    public String defaultValue() {
        return defaultValue;
    }
}
