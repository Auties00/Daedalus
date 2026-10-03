package com.github.auties00.daedalus.protobuf.annotation;

import com.github.auties00.daedalus.protobuf.model.*;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;

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
 * public final class MessageSpec extends ProtobufMessageSpec<Message, MessageBuilder> {
 *     @TypeMixin.Instance
 *     public static MessageSpec global() { ... }
 *
 *     @TypeSerializer
 *     public void encodeBinary(Message protoInputObject, ProtobufBinaryWriter<?> protoOutputStream) { ... }
 *     @TypeDeserializer
 *     public Message decodeBinary(ProtobufBinaryReader protoInputStream) { ... }
 *     @TypeSize
 *     public int sizeOfBinary(Message protoInputObject) { ... }
 *     public void encodeText(Message protoInputObject, ProtobufTextWriter<?> protoOutputStream) { ... }
 *     public Message decodeText(ProtobufTextReader protoInputStream) { ... }
 *     ...
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
 * public final class MessageSpec extends ProtobufMessageSpec<Message, MessageBuilder> {
 *     @TypeMixin.Instance
 *     public static MessageSpec global() { ... }
 *
 *     @TypeSerializer
 *     public void encodeBinary(Message protoInputObject, ProtobufBinaryWriter<?> protoOutputStream) { ... }
 *     @TypeDeserializer
 *     public Message decodeBinary(ProtobufBinaryReader protoInputStream) { ... }
 *     @TypeSize
 *     public int sizeOfBinary(Message protoInputObject) { ... }
 *     public void encodeText(Message protoInputObject, ProtobufTextWriter<?> protoOutputStream) { ... }
 *     public Message decodeText(ProtobufTextReader protoInputStream) { ... }
 *     ...
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
 * public final class MessageSpec extends ProtobufMessageSpec<Message, MessageBuilder> {
 *     @TypeMixin.Instance
 *     public static MessageSpec global() { ... }
 *
 *     @TypeSerializer
 *     public void encodeBinary(Message protoInputObject, ProtobufBinaryWriter<?> protoOutputStream) { ... }
 *     @TypeDeserializer
 *     public Message decodeBinary(ProtobufBinaryReader protoInputStream) { ... }
 *     @TypeSize
 *     public int sizeOfBinary(Message protoInputObject) { ... }
 *     public void encodeText(Message protoInputObject, ProtobufTextWriter<?> protoOutputStream) { ... }
 *     public Message decodeText(ProtobufTextReader protoInputStream) { ... }
 *     ...
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
 * public final class MessageSpec extends ProtobufMessageSpec<Message, MessageBuilder> {
 *     @TypeMixin.Instance
 *     public static MessageSpec global() { ... }
 *
 *     @TypeSerializer
 *     public void encodeBinary(Message protoInputObject, ProtobufBinaryWriter<?> protoOutputStream) { ... }
 *     @TypeDeserializer
 *     public Message decodeBinary(ProtobufBinaryReader protoInputStream) { ... }
 *     @TypeSize
 *     public int sizeOfBinary(Message protoInputObject) { ... }
 *     public void encodeText(Message protoInputObject, ProtobufTextWriter<?> protoOutputStream) { ... }
 *     public Message decodeText(ProtobufTextReader protoInputStream) { ... }
 *     ...
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
 * public final class MessageSpec extends ProtobufMessageSpec<Message, MessageBuilder> {
 *     @TypeMixin.Instance
 *     public static MessageSpec global() { ... }
 *
 *     @TypeSerializer
 *     public void encodeBinary(Message protoInputObject, ProtobufBinaryWriter<?> protoOutputStream) { ... }
 *     @TypeDeserializer
 *     public Message decodeBinary(ProtobufBinaryReader protoInputStream) { ... }
 *     @TypeSize
 *     public int sizeOfBinary(Message protoInputObject) { ... }
 *     public void encodeText(Message protoInputObject, ProtobufTextWriter<?> protoOutputStream) { ... }
 *     public Message decodeText(ProtobufTextReader protoInputStream) { ... }
 *     ...
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
    int MIN_FIELD_INDEX = 1;

    /**
     * Represents the maximum allowable index for a Protobuf field.
     */
    int MAX_FIELD_INDEX = 536_870_911; // 2^29 - 1

    /**
     * Specifies the Protocol Buffer version or edition that this message conforms to.
     *
     * <p>This is used to resolve {@code VERSION_DEFAULT} values in field-level
     * feature attributes such as {@link ProtobufFieldPresence}, {@link ProtobufUtf8Validation},
     * and {@link ProtobufRepeatedFieldEncoding}.
     *
     * @return the protobuf version, defaulting to {@link ProtobufVersion#DEFAULT_VERSION}
     */
    ProtobufVersion version() default ProtobufVersion.PROTOBUF_2; // I really want to know who decided that DEFAULT_VERSION cannot be a constant even though it's a static final field

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
     * @return the JSON compatibility strategy, defaulting to {@link ProtobufJsonCompatibility#VERSION_DEFAULT}
     */
    ProtobufJsonCompatibility generateJson() default ProtobufJsonCompatibility.VERSION_DEFAULT;

    /**
     * Specifies the modifiers of the builder generated for this message.
     *
     * <p>A builder class named {@code {MessageName}Builder} is always generated, in the
     * package of the message. It holds the value of every field that is not synthetic,
     * starting from the value of a field that is not set: an empty collection, map or array
     * for a repeated field, the default value of its type for a field with implicit presence,
     * and {@code null} otherwise. A field whose value is a message holds the builder of that
     * message instead, so that a message read more than once is merged like the official
     * protobuf library does. A repeated or map field is appended to, by adding to its
     * collection or map if it can be modified, or to a copy of it otherwise. A field that
     * has no presence, which is a field with implicit presence, or a repeated or map field,
     * cannot be unset, so a {@code null} value set through its setter is built as the value
     * of a field that is not set. A field with explicit presence, which includes every
     * singular field of proto2, keeps {@code null}, which means that it is not set.
     *
     * <p>The builder has two terminal methods, which build the message through its
     * constructor, or through its {@code @TypeDeserializer} factory method if one is declared:
     * <ul>
     * <li>{@code build()} throws a
     *     {@link com.github.auties00.daedalus.protobuf.exception.ProtobufUninitializedMessageException}
     *     if a required field of the message, or of a nested message, is not set;
     * <li>{@code buildPartial()} builds the message without checking its required fields.
     * </ul>
     *
     * <p>The Spec class of the message decodes into the builder: {@code decodePartial}
     * returns the builder of the decoded fields, or merges them into a given builder, and
     * {@code decodeBinary} and {@code decodeText} build the message from {@code decodePartial}.
     *
     * <p>The builder is a top level class that is instantiated by the Spec class of the
     * message, so only {@link ClassModifier#PUBLIC} and {@link ClassModifier#FINAL} can be
     * specified; any other modifier is a compile time error. Leave out {@link ClassModifier#PUBLIC} to make the builder package
     * private, for example when the message declares its own builder through
     * {@code @TypeBuilder}: the {@code decodePartial} methods of the Spec class are public, as
     * they implement {@link com.github.auties00.daedalus.protobuf.spec.ProtobufMessageSpec},
     * but code in another package cannot use the builder they take or return. A message whose builder is not public
     * cannot be the type of a field of a message in another package, as the builder of that
     * message could not hold its builder.
     *
     * @return the modifiers of the builder, {@code public final} by default
     */
    ClassModifier[] defaultBuilderModifiers() default {
            ClassModifier.PUBLIC,
            ClassModifier.FINAL
    };

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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the repeated field encoding strategy, defaulting to {@link ProtobufRepeatedFieldEncoding#VERSION_DEFAULT}
         */
        ProtobufRepeatedFieldEncoding repeatedFieldEncoding() default ProtobufRepeatedFieldEncoding.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

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
         * @return the UTF-8 validation strategy, defaulting to {@link ProtobufUtf8Validation#VERSION_DEFAULT}
         */
        ProtobufUtf8Validation utf8Validation() default ProtobufUtf8Validation.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;
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
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


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
         * @return the field presence strategy, defaulting to {@link ProtobufFieldPresence#VERSION_DEFAULT}
         */
        ProtobufFieldPresence fieldPresence() default ProtobufFieldPresence.VERSION_DEFAULT;

        /**
         * Specifies the wire encoding for this nested message field.
         * <p>
         * This replaces the proto2 group syntax. In editions, any message field can use
         * {@link ProtobufMessageEncoding#DELIMITED} encoding (wire types 3/4) instead of the
         * standard {@link ProtobufMessageEncoding#LENGTH_PREFIXED} encoding (wire type 2).
         * <p>
         * The default is {@link ProtobufMessageEncoding#LENGTH_PREFIXED} for all versions and editions.
         *
         * @return the message encoding strategy, defaulting to {@link ProtobufMessageEncoding#VERSION_DEFAULT}
         */
        ProtobufMessageEncoding messageEncoding() default ProtobufMessageEncoding.VERSION_DEFAULT;
    }

    /**
     * Describes a oneof in a {@link ProtobufMessage}: a group of fields of which at most one
     * is set.
     *
     * <p>The annotated field must have a sealed type, such as a sealed interface or a sealed
     * class, and every case of the oneof is held by one of its permitted subtypes. The cases
     * are grouped by the kind of their value, in one attribute for each kind of field that a
     * oneof can hold: a oneof cannot hold a repeated or a map field. Every type that the
     * sealed type permits must be the {@code type} of exactly one case, which a permitted
     * type that is itself sealed delegates to the types that it permits; otherwise, a
     * compile time error is reported. For example:
     *
     * <pre>{@code
     * @ProtobufMessage
     * record Drawing(
     *         @ProtobufMessage.OneOfField(
     *                 messageCases = {
     *                         @ProtobufMessage.OneOfField.MessageCase(index = 1, type = Circle.class),
     *                         @ProtobufMessage.OneOfField.MessageCase(index = 2, type = Square.class)
     *                 },
     *                 stringCases = @ProtobufMessage.OneOfField.StringCase(index = 3, type = Label.class)
     *         )
     *         Shape shape
     * ) {}
     *
     * sealed interface Shape permits Circle, Square, Label {}
     *
     * @ProtobufMessage
     * record Circle(@ProtobufMessage.DoubleField(index = 1) Double radius) implements Shape {}
     *
     * @ProtobufMessage
     * record Square(@ProtobufMessage.DoubleField(index = 1) Double side) implements Shape {}
     *
     * record Label(String text) implements Shape {
     *     @TypeSerializer
     *     public String text() {
     *         return text;
     *     }
     *
     *     @TypeDeserializer
     *     static Label of(String text) {
     *         return new Label(text);
     *     }
     * }
     * }</pre>
     *
     * <p>The value of the field is the value of the case that is set, or {@code null} if no
     * case is set, as every case has explicit presence. When a message is decoded, the last
     * case on the wire is the one that is set, and a message case that appears more than once
     * is merged, like the official protobuf library does. A case index that the oneof does
     * not know is an unknown field of the enclosing message, so the oneof is not set.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface OneOfField {
        /**
         * Overrides the name of the oneof used during text serialization and deserialization.
         * If empty, the name is automatically derived from the Java field name.
         *
         * @return the custom oneof name, or empty for the default
         */
        String name() default "";

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#FLOAT}.
         *
         * @return the {@code float} cases, empty by default
         */
        FloatCase[] floatCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#DOUBLE}.
         *
         * @return the {@code double} cases, empty by default
         */
        DoubleCase[] doubleCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#BOOL}.
         *
         * @return the {@code bool} cases, empty by default
         */
        BoolCase[] boolCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#INT32}.
         *
         * @return the {@code int32} cases, empty by default
         */
        Int32Case[] int32Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#SINT32}.
         *
         * @return the {@code sint32} cases, empty by default
         */
        Sint32Case[] sint32Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#UINT32}.
         *
         * @return the {@code uint32} cases, empty by default
         */
        Uint32Case[] uint32Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#FIXED32}.
         *
         * @return the {@code fixed32} cases, empty by default
         */
        Fixed32Case[] fixed32Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#SFIXED32}.
         *
         * @return the {@code sfixed32} cases, empty by default
         */
        Sfixed32Case[] sfixed32Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#INT64}.
         *
         * @return the {@code int64} cases, empty by default
         */
        Int64Case[] int64Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#SINT64}.
         *
         * @return the {@code sint64} cases, empty by default
         */
        Sint64Case[] sint64Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#UINT64}.
         *
         * @return the {@code uint64} cases, empty by default
         */
        Uint64Case[] uint64Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#FIXED64}.
         *
         * @return the {@code fixed64} cases, empty by default
         */
        Fixed64Case[] fixed64Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#SFIXED64}.
         *
         * @return the {@code sfixed64} cases, empty by default
         */
        Sfixed64Case[] sfixed64Cases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#ENUM}.
         *
         * @return the {@code enum} cases, empty by default
         */
        EnumCase[] enumCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#STRING}.
         *
         * @return the {@code string} cases, empty by default
         */
        StringCase[] stringCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#BYTES}.
         *
         * @return the {@code bytes} cases, empty by default
         */
        BytesCase[] bytesCases() default {};

        /**
         * Returns the cases of this oneof whose value is a {@link ProtobufType#MESSAGE}.
         *
         * @return the {@code message} cases, empty by default
         */
        MessageCase[] messageCases() default {};

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#FLOAT}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface FloatCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#DOUBLE}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface DoubleCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#BOOL}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface BoolCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#INT32}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Int32Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#SINT32}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Sint32Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#UINT32}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Uint32Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#FIXED32}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Fixed32Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#SFIXED32}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Sfixed32Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#INT64}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Int64Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#SINT64}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Sint64Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#UINT64}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Uint64Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#FIXED64}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Fixed64Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#SFIXED64}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface Sfixed64Case {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#ENUM}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface EnumCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#STRING}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface StringCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};

            /**
             * Specifies the UTF-8 validation strategy for this case.
             * <p>
             * The default behaviour depends on the protobuf version or edition:
             * <ul>
             *     <li><strong>proto2:</strong> {@link ProtobufUtf8Validation#NONE}</li>
             *     <li><strong>proto3:</strong> {@link ProtobufUtf8Validation#VERIFY}</li>
             *     <li><strong>edition 2023/2024:</strong> {@link ProtobufUtf8Validation#VERIFY}</li>
             * </ul>
             *
             * @return the UTF-8 validation strategy, defaulting to {@link ProtobufUtf8Validation#VERSION_DEFAULT}
             */
            ProtobufUtf8Validation utf8Validation() default ProtobufUtf8Validation.VERSION_DEFAULT;
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#BYTES}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface BytesCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};
        }

        /**
         * Describes a case of a {@link OneOfField} whose value is a
         * {@link ProtobufType#MESSAGE}, held by the permitted subtype {@link #type()}.
         *
         * <p>Like the field of a message, the case is converted from {@link #type()} to
         * its wire value, and back, by the converters of that type, which can be declared
         * by the type itself with {@code @TypeSerializer} and {@code @TypeDeserializer}, or
         * by its mixins.
         */
        @Target({})
        @Retention(RetentionPolicy.RUNTIME)
        @interface MessageCase {
            /**
             * Returns the index of this case, which is the index of a field of the
             * enclosing message.
             *
             * @return the numeric index of the case
             */
            int index();

            /**
             * Overrides the name of this case used during text serialization and
             * deserialization (JSON, textproto).
             * If empty, the name is derived from the simple name of {@link #type()}.
             *
             * @return the custom case name, or empty for the default
             */
            String name() default "";

            /**
             * Returns the permitted subtype of the type of the oneof that holds the value of
             * this case.
             *
             * @return the type of the case
             */
            Class<?> type();

            /**
             * Returns the list of mixin classes associated with this case.
             *
             * <p>Global mixins, such as the ones that support the types of the JDK standard
             * library, apply without being listed here. A listed mixin takes precedence over
             * them.
             *
             * @return an array of mixin classes
             */
            Class<?>[] mixins() default {};

            /**
             * Specifies the wire encoding of the message of this case.
             * <p>
             * A message can be encoded as a group, delimited by start and end tags
             * ({@link ProtobufMessageEncoding#DELIMITED}), instead of being preceded by its
             * length ({@link ProtobufMessageEncoding#LENGTH_PREFIXED}), which is the default
             * for all versions and editions.
             *
             * @return the message encoding strategy, defaulting to {@link ProtobufMessageEncoding#VERSION_DEFAULT}
             */
            ProtobufMessageEncoding messageEncoding() default ProtobufMessageEncoding.VERSION_DEFAULT;
        }
    }

    /**
     * Describes a map field in a {@link ProtobufMessage}, whose keys and values have the
     * types {@link #mapKeyType()} and {@link #mapValueType()}.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MapField {
        /**
         * Returns the index associated with this field.
         *
         * @return the numeric index of the field
         */
        int index();

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
         * <p>Global mixins, such as the ones that support the types of the JDK standard
         * library, apply without being listed here. A listed mixin takes precedence over
         * them.
         *
         * @return an array of mixin classes
         */
        Class<?>[] mixins() default {};


        /**
         * Controls UTF-8 validation for string keys and values in this map field.
         *
         * <p>When set to {@link ProtobufUtf8Validation#VERIFY}, both the key (if it is a
         * {@code string}) and the value (if it is a {@code string}) are validated as
         * well-formed UTF-8 during deserialization. The same validation mode applies to
         * both key and value; they cannot be configured independently.
         *
         * <ul>
         *     <li><strong>proto2:</strong> defaults to {@link ProtobufUtf8Validation#NONE}</li>
         *     <li><strong>proto3:</strong> defaults to {@link ProtobufUtf8Validation#VERIFY}</li>
         *     <li><strong>edition 2023+:</strong> defaults to {@link ProtobufUtf8Validation#VERIFY}</li>
         * </ul>
         *
         * @return the UTF-8 validation strategy, defaulting to {@link ProtobufUtf8Validation#VERSION_DEFAULT}
         */
        ProtobufUtf8Validation utf8Validation() default ProtobufUtf8Validation.VERSION_DEFAULT;
    }

    /**
     * Marks a field that holds the unknown fields of the enclosing {@link ProtobufMessage}.
     *
     * <p>The codegen always operates on a
     * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields}, which keeps the
     * unknown fields read from the binary format, by their indexes, and the ones read from the
     * text format, by their names, side by side. A message that declares no such field rejects
     * the names it does not know when it is read from the text format, like the official
     * protobuf library does.
     *
     * <p>The annotated field's declared type may either be that store, or any other type for
     * which one of the {@link #mixins()} declares a static
     * {@link com.github.auties00.daedalus.typesystem.annotation.TypeSerializer} that converts it
     * to the store and a static
     * {@link com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer} that converts
     * the store to it.
     *
     * <p>The field is read directly unless it is private, in which case the message
     * must declare a non-private getter named after it, like the accessor of a record
     * component. The message receives the store as the last argument of its
     * constructor.
     *
     * <p>During deserialization the codegen routes any wire field whose
     * index does not match a declared field to
     * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields#add(int, ProtobufUnknownValue.Binary)},
     * together with the value of an enum field that has no constant, so that it is
     * not lost, and any text field whose name does not match a declared field to
     * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields#add(String, ProtobufUnknownValue.Text)}.
     * A reserved index or name is treated like any other undeclared one. During
     * serialization the codegen iterates the stores after the typed fields and
     * re-emits each entry verbatim. Each format only writes the unknown fields read from it,
     * as a field read from the text format has no index, and a field read from the binary
     * format has no name, so every unknown field that a format writes can be read back from
     * it.
     *
     * <h2>Usage example</h2>
     * <pre>{@code
     * @ProtobufMessage
     * public record MyMessage(
     *     @ProtobufMessage.StringField(index = 1)
     *     String name,
     *
     *     @ProtobufMessage.UnknownFields
     *     ProtobufUnknownFields unknownFields
     * ) {}
     * }</pre>
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface UnknownFields {
        /**
         * Returns the array of mixin classes that adapt the field's declared
         * type to and from a
         * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields}.
         *
         * <p>This follows the same convention as the other {@code mixins()}
         * attributes on protobuf field annotations: each listed class is a
         * {@link TypeMixin} whose static
         * {@link com.github.auties00.daedalus.typesystem.annotation.TypeSerializer}
         * and {@link com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer}
         * methods, each taking a single parameter, convert between the user's
         * chosen field type (for example an immutable holder, or a
         * project-specific store) and the {@code ProtobufUnknownFields} that the
         * codegen iterates over.
         *
         * <p>When the field's declared type already is a
         * {@code ProtobufUnknownFields} no adapter is needed and this attribute can
         * be left at its empty default.
         *
         * @return the registered conversion mixins
         */
        Class<?>[] mixins() default {};
    }
}
