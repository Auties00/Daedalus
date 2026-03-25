package com.github.auties00.daedalus.protobuf.annotation;

import com.github.auties00.daedalus.protobuf.adapter.StringMixin;
import com.github.auties00.daedalus.protobuf.model.*;
import com.github.auties00.daedalus.typesystem.adapter.*;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to a type to represent a Protobuf Message.
 * A message is the primary unit of data in Protobuf, encoded using length-delimited wire format.
 * The annotated type can be a class, a record, or an interface.
 *
 * <h2>As a class:</h2>
 *
 * <h3>Concrete:</h3>
 * <pre>{@code
 * @ProtobufMessage
 * public final class Message {
 *     @ProtobufMessage.StringField(index = 1)
 *     private final Supplier<String> string;
 *
 *     @ProtobufMessage.Int32Field(index = 2)
 *     private final int value;
 *
 *     public Message(Supplier<String> string, int value) {
 *         this.string = string;
 *         this.value = value;
 *     }
 *
 *     @ProtobufAccessor(index = 1)
 *     public Supplier<String> string() {
 *         return string;
 *     }
 *
 *     @ProtobufAccessor(index = 2)
 *     public int value() {
 *         return value;
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @TypeMixin(scope = TypeMixin.Scope.GLOBAL)
 * public class MessageSpec {
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufBinaryWriter protoWriter) { ... }
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufTextWriter protoWriter) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufBinaryReader protoReader) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufTextReader protoReader) { ... }
 *     @TypeSize
 *     public static int sizeOf(Message protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated:
 * <pre>{@code
 * public class MessageBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public MessageBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public MessageBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public MessageBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Message build() {
 *         return new Message(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed abstract:</h3>
 * <p>Only sealed abstract classes are supported. Non-sealed abstract classes cannot be used
 * because the set of permitted subtypes must be known at compile time.
 * <pre>{@code
 * @ProtobufMessage
 * public sealed abstract class Message {
 *     @ProtobufMessage.StringField(index = 1)
 *     private final Supplier<String> string;
 *
 *     @ProtobufMessage.Int32Field(index = 2)
 *     private final int value;
 *
 *     protected Message(Supplier<String> string, int value) {
 *         this.string = string;
 *         this.value = value;
 *     }
 *
 *     @ProtobufAccessor(index = 1)
 *     public Supplier<String> string() {
 *         return string;
 *     }
 *
 *     @ProtobufAccessor(index = 2)
 *     public int value() {
 *         return value;
 *     }
 *
 *     @ProtobufMessage
 *     static final class TextMessage extends Message {
 *         TextMessage(Supplier<String> string, int value) {
 *             super(string, value);
 *         }
 *     }
 *
 *     @ProtobufMessage
 *     static final class DataMessage extends Message {
 *         DataMessage(Supplier<String> string, int value) {
 *             super(string, value);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @TypeMixin(scope = TypeMixin.Scope.GLOBAL)
 * public class MessageSpec {
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufBinaryWriter protoWriter) { ... }
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufTextWriter protoWriter) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufBinaryReader protoReader) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufTextReader protoReader) { ... }
 *     @TypeSize
 *     public static int sizeOf(Message protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated for each permitted subtype:
 * <pre>{@code
 * public class TextMessageBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public TextMessageBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public TextMessageBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public TextMessageBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Message.TextMessage build() {
 *         return new Message.TextMessage(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h2>As a record:</h2>
 * <pre>{@code
 * @ProtobufMessage
 * record Message(
 *     @ProtobufMessage.StringField(index = 1)
 *     Supplier<String> string,
 *     @ProtobufMessage.Int32Field(index = 2)
 *     int value
 * ) {
 *
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @TypeMixin(scope = TypeMixin.Scope.GLOBAL)
 * public class MessageSpec {
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufBinaryWriter protoWriter) { ... }
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufTextWriter protoWriter) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufBinaryReader protoReader) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufTextReader protoReader) { ... }
 *     @TypeSize
 *     public static int sizeOf(Message protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated:
 * <pre>{@code
 * public class MessageBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public MessageBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public MessageBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public MessageBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Message build() {
 *         return new Message(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h2>As an interface:</h2>
 * <pre>{@code
 * @ProtobufMessage
 * public interface Message {
 *     @ProtobufMessage.StringField(index = 1)
 *     Supplier<String> string();
 *
 *     @ProtobufMessage.Int32Field(index = 2)
 *     int value();
 * }
 * }</pre>
 *
 * <p>The following package-private implementation class is generated:
 * <pre>{@code
 * final class MessageImpl implements Message {
 *     private final Supplier<String> string;
 *     private final int value;
 *
 *     MessageImpl(Supplier<String> string, int value) {
 *         this.string = string;
 *         this.value = value;
 *     }
 *
 *     @Override
 *     public Supplier<String> string() {
 *         return string;
 *     }
 *
 *     @Override
 *     public int value() {
 *         return value;
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated, returning the generated implementation:
 * <pre>{@code
 * @TypeMixin(scope = TypeMixin.Scope.GLOBAL)
 * public class MessageSpec {
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufBinaryWriter protoWriter) { ... }
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufTextWriter protoWriter) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufBinaryReader protoReader) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufTextReader protoReader) { ... }
 *     @TypeSize
 *     public static int sizeOf(Message protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated, returning the generated implementation:
 * <pre>{@code
 * public class MessageBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public MessageBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public MessageBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public MessageBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Message build() {
 *         return new MessageImpl(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed:</h3>
 * <p>Sealed interfaces can also be used as protobuf messages. Unlike plain interfaces,
 * no implementation class is generated; the permitted subtypes serve as the concrete
 * implementations. Each permitted subtype must be annotated with {@link ProtobufMessage}.
 * <pre>{@code
 * @ProtobufMessage
 * public sealed interface Message {
 *     @ProtobufMessage.StringField(index = 1)
 *     Supplier<String> string();
 *
 *     @ProtobufMessage.Int32Field(index = 2)
 *     int value();
 *
 *     @ProtobufMessage
 *     record TextMessage(Supplier<String> string, int value) implements Message {
 *     }
 *
 *     @ProtobufMessage
 *     record DataMessage(Supplier<String> string, int value) implements Message {
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @TypeMixin(scope = TypeMixin.Scope.GLOBAL)
 * public class MessageSpec {
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufBinaryWriter protoWriter) { ... }
 *     @TypeSerializer
 *     public static void encode(Message protoInputObject, ProtobufTextWriter protoWriter) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufBinaryReader protoReader) { ... }
 *     @TypeDeserializer
 *     public static Message decode(ProtobufTextReader protoReader) { ... }
 *     @TypeSize
 *     public static int sizeOf(Message protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated for each permitted subtype:
 * <pre>{@code
 * public class TextMessageBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public TextMessageBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public TextMessageBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public TextMessageBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Message.TextMessage build() {
 *         return new Message.TextMessage(string, value);
 *     }
 * }
 * }</pre>
 *
 * @see FloatField
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {
    /**
     * The minimum valid index for a Protobuf field.
     */
    long MIN_FIELD_INDEX = 1;

    /**
     * Represents the maximum allowable index for a Protobuf field.
     */
    long MAX_FIELD_INDEX = 536_870_911; // 2^29 - 1

    /**
     * Specifies the fully qualified name of the referenced Protobuf Message schema.
     * This is used by the CLI to update schemas.
     *
     * @return the fully qualified name of the Protobuf Message schema, or empty if it should be detected automatically
     */
    String protoName() default "";

    /**
     * Specifies the names that are reserved and cannot be used in the context
     * where this annotation is applied. Reserved names are typically used
     * to ensure compatibility or avoid conflicts in Protobuf definitions.
     *
     * @return an array of strings representing the reserved names
     */
    String[] reservedNames() default {};

    /**
     * Specifies the numeric indexes that are reserved and cannot be used
     * in the context where this annotation is applied. Reserved indexes
     * are typically used to ensure compatibility or avoid conflicts
     * in Protobuf definitions.
     *
     * @return an array of integers representing the reserved indexes
     */
    int[] reservedIndexes() default {};

    /**
     * Specifies the numeric ranges that are reserved and cannot be used
     * in the context where this annotation is applied. Reserved ranges
     * are typically used to ensure compatibility or avoid conflicts
     * in Protobuf definitions.
     *
     * @return an array of {@code ProtobufReservedRange} representing the reserved ranges
     */
    ProtobufReservedRange[] reservedRanges() default {};

    /**
     * Controls whether JSON serialization/deserialization code should be generated for this message.
     * <p>
     * When set to {@link ProtobufJsonCompatibility#ENABLED}, the JSON processor (daedalus-json module)
     * must be active; if it is not, the compiler will issue a warning.
     *
     * <ul>
     *     <li><strong>proto2:</strong> defaults to {@link ProtobufJsonCompatibility#DISABLED}</li>
     *     <li><strong>proto3:</strong> defaults to {@link ProtobufJsonCompatibility#ENABLED}</li>
     *     <li><strong>edition 2023+:</strong> defaults to {@link ProtobufJsonCompatibility#ENABLED}</li>
     * </ul>
     *
     * @return the JSON compatibility strategy, defaulting to {@link ProtobufJsonCompatibility#EDITION_DEFAULT}
     */
    ProtobufJsonCompatibility generateJson() default ProtobufJsonCompatibility.EDITION_DEFAULT;

    /**
     * Describes a {@link ProtobufType#FLOAT} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface FloatField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#DOUBLE} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface DoubleField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#BOOL} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface BoolField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes an {@link ProtobufType#INT32} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Int32Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#SINT32} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sint32Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#UINT32} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Uint32Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#FIXED32} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Fixed32Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#SFIXED32} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sfixed32Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes an {@link ProtobufType#INT64} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Int64Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#SINT64} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sint64Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#UINT64} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Uint64Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#FIXED64} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Fixed64Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#SFIXED64} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sfixed64Field {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes an {@link ProtobufType#ENUM} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface EnumField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the encoding strategy for repeated fields.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufRepeatedFieldEncoding#EXPANDED}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufRepeatedFieldEncoding#PACKED}</li>
         * </ul>
         *
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#EDITION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#STRING} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface StringField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class,
                StringMixin.class,
                URIMixin.class,
                URLMixin.class,
                UUIDMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the UTF-8 validation strategy for this string field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufUtf8Validation#NONE}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufUtf8Validation#VERIFY}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufUtf8Validation#VERIFY}</li>
         * </ul>
         *
         * @return the UTF-8 validation strategy, defaulting to {@link ProtobufUtf8Validation#EDITION_DEFAULT}
         */
        ProtobufUtf8Validation utf8Validation() default ProtobufUtf8Validation.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#BYTES} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface BytesField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#MESSAGE} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MessageField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;

        /**
         * Specifies the field presence behaviour for this field.
         * <p>
         * The default behaviour depends on the protobuf version or edition:
         * <ul>
         *     <li><strong>proto2:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         *     <li><strong>proto3:</strong> {@link ProtobufFieldPresence#IMPLICIT}</li>
         *     <li><strong>edition 2023/2024:</strong> {@link ProtobufFieldPresence#EXPLICIT}</li>
         * </ul>
         *
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#EDITION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.EDITION_DEFAULT;

        /**
         * Specifies the wire encoding for this nested message field.
         * <p>
         * This replaces the proto2 group syntax. In editions, any message field can use
         * {@link ProtobufMessageEncoding#DELIMITED} encoding (wire types 3/4) instead of the
         * standard {@link ProtobufMessageEncoding#LENGTH_PREFIXED} encoding (wire type 2).
         * <p>
         * The default is {@link ProtobufMessageEncoding#LENGTH_PREFIXED} for all versions and editions.
         *
         * @return the message encoding strategy, defaulting to {@link ProtobufMessageEncoding#EDITION_DEFAULT}
         */
        ProtobufMessageEncoding messageEncoding() default ProtobufMessageEncoding.EDITION_DEFAULT;
    }

    /**
     * Describes a {@link ProtobufType#MAP} field in a {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MapField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        long index();

        /**
         * Overrides the field name used during text serialization and deserialization (JSON, textproto).
         * If empty, the field name is automatically derived from the Java field name.
         *
         * @return the custom field name, or empty for the default
         */
        String name() default "";

        /**
         * Specifies the key type for this map field.
         *
         * @return the {@link ProtobufType} representing the key type of the map
         */
        ProtobufType mapKeyType();

        /**
         * Specifies the value type for this map field.
         *
         * @return the {@link ProtobufType} representing the value type of the map
         */
        ProtobufType mapValueType();

        /**
         * Returns the list of mixin classes associated with this field.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {
                AtomicMixin.class,
                CollectionMixin.class,
                FutureMixin.class,
                MapMixin.class,
                OptionalMixin.class
        };

        /**
         * Indicates whether this field should be ignored during serialization and deserialization.
         *
         * @return true if the field is ignored; false otherwise
         */
        boolean ignored() default false;
    }

    /**
     * This annotation can be applied to a non-static field to store unknown fields encountered while when deserializing the enclosing {@link ProtobufMessage}.
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface UnknownFields {
        /**
         * Returns the array of mixin classes associated with this configuration.
         * These can be used to specify using {@link Setter} how an existing data structure can be used by Protobuf
         * when an unknown field is encountered.
         *
         * @return an array of {@code Class<?>} representing the default mixins
         */
        Class<?>[] mixins() default {
                MapMixin.class
        };

        /**
         * This annotation can be applied to non-static methods in a type that is used as an unknown fields store
         * or to static methods in a {@link TypeMixin} for an existing data structure.
         * <h2>Usage Example:</h2>
         * <h3>In a custom type:</h3>
         * <pre>{@code
         * final class UnknownFeatures {
         *     private final Set<Integer> unknownFeatures;
         *
         *     UnknownFeatures() {
         *         this.unknownFeatures = new HashSet<>();
         *     }
         *
         *     @ProtobufUnknownFields.Setter
         *     public void addFeature(long index, ProtobufUnknownValue value) {
         *         if (value instanceof ProtobufUnknownValue.VarInt(var data) && data == 1) {
         *             unknownFeatures.add(index);
         *         }
         *     }
         *
         *     public boolean hasFeature(long index) {
         *         return unknownFeatures.contains(index);
         *     }
         * }
         * }</pre>
         * <h3>In a {@link TypeMixin}:</h3>
         * <pre>{@code
         * @TypeMixin
         * final class ProtobufMapMixin {
         *     @ProtobufUnknownFields.Setter
         *     public static void addUnknownField(Map<Long, ProtobufUnknownValue> map, long index, ProtobufUnknownValue value) {
         *         map.put(index, value);
         *     }
         * }
         *}</pre>
         **/
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @interface Setter {

        }
    }
}
