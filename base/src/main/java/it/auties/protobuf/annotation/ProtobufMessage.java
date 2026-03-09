package it.auties.protobuf.annotation;

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
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     private final Supplier<String> string;
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
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
 * @ProtobufMixin
 * public class MessageSpec {
 *     @ProtobufSerializer
 *     public static void encode(Message protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Message decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
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
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     private final Supplier<String> string;
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
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
 * @ProtobufMixin
 * public class MessageSpec {
 *     @ProtobufSerializer
 *     public static void encode(Message protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Message decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
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
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     Supplier<String> string,
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
 *     int value
 * ) {
 *
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @ProtobufMixin
 * public class MessageSpec {
 *     @ProtobufSerializer
 *     public static void encode(Message protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Message decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
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
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     Supplier<String> string();
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
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
 * @ProtobufMixin
 * public class MessageSpec {
 *     @ProtobufSerializer
 *     public static void encode(Message protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Message decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
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
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     Supplier<String> string();
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
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
 * @ProtobufMixin
 * public class MessageSpec {
 *     @ProtobufSerializer
 *     public static void encode(Message protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Message decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
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
 * @see ProtobufGroup
 * @see ProtobufProperty
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {
    /**
     * Specifies the fully qualified name of the referenced Protobuf Message schema.
     * This is used by the CLI to update schemas.
     *
     * @return the fully qualified name of the Protobuf Message schema, or empty if it should be detected automatically
     */
    String name() default "";

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
}
