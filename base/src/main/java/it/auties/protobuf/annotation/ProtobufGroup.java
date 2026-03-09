package it.auties.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to a type to represent a Protobuf2 Group.
 * A group is a legacy Protobuf2 wire format that encodes nested messages using start and end group tags
 * instead of length-delimited encoding. The annotated type can be a class, a record, or an interface.
 *
 * <h2>As a class:</h2>
 *
 * <h3>Concrete:</h3>
 * <pre>{@code
 * @ProtobufGroup
 * public final class Group {
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     private final Supplier<String> string;
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
 *     private final int value;
 *
 *     public Group(Supplier<String> string, int value) {
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
 * public class GroupSpec {
 *     @ProtobufSerializer
 *     public static void encode(Group protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Group decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
 *     public static int sizeOf(Group protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated:
 * <pre>{@code
 * public class GroupBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public GroupBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public GroupBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public GroupBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Group build() {
 *         return new Group(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed abstract:</h3>
 * <p>Only sealed abstract classes are supported. Non-sealed abstract classes cannot be used
 * because the set of permitted subtypes must be known at compile time.
 * <pre>{@code
 * @ProtobufGroup
 * public sealed abstract class Group {
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     private final Supplier<String> string;
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
 *     private final int value;
 *
 *     protected Group(Supplier<String> string, int value) {
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
 *     @ProtobufGroup
 *     static final class TextGroup extends Group {
 *         TextGroup(Supplier<String> string, int value) {
 *             super(string, value);
 *         }
 *     }
 *
 *     @ProtobufGroup
 *     static final class DataGroup extends Group {
 *         DataGroup(Supplier<String> string, int value) {
 *             super(string, value);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @ProtobufMixin
 * public class GroupSpec {
 *     @ProtobufSerializer
 *     public static void encode(Group protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Group decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
 *     public static int sizeOf(Group protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated for each permitted subtype:
 * <pre>{@code
 * public class TextGroupBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public TextGroupBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public TextGroupBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public TextGroupBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Group.TextGroup build() {
 *         return new Group.TextGroup(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h2>As a record:</h2>
 * <pre>{@code
 * @ProtobufGroup
 * record Group(
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
 * public class GroupSpec {
 *     @ProtobufSerializer
 *     public static void encode(Group protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Group decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
 *     public static int sizeOf(Group protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated:
 * <pre>{@code
 * public class GroupBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public GroupBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public GroupBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public GroupBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Group build() {
 *         return new Group(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h2>As an interface:</h2>
 * <pre>{@code
 * @ProtobufGroup
 * public interface Group {
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
 * final class GroupImpl implements Group {
 *     private final Supplier<String> string;
 *     private final int value;
 *
 *     GroupImpl(Supplier<String> string, int value) {
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
 * public class GroupSpec {
 *     @ProtobufSerializer
 *     public static void encode(Group protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Group decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
 *     public static int sizeOf(Group protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated, returning the generated implementation:
 * <pre>{@code
 * public class GroupBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public GroupBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public GroupBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public GroupBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Group build() {
 *         return new GroupImpl(string, value);
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed:</h3>
 * <p>Sealed interfaces can also be used as protobuf groups. Unlike plain interfaces,
 * no implementation class is generated; the permitted subtypes serve as the concrete
 * implementations. Each permitted subtype must be annotated with {@link ProtobufGroup}.
 * <pre>{@code
 * @ProtobufGroup
 * public sealed interface Group {
 *     @ProtobufProperty(index = 1, type = ProtobufType.STRING)
 *     Supplier<String> string();
 *
 *     @ProtobufProperty(index = 2, type = ProtobufType.INT32)
 *     int value();
 *
 *     @ProtobufGroup
 *     record TextGroup(Supplier<String> string, int value) implements Group {
 *     }
 *
 *     @ProtobufGroup
 *     record DataGroup(Supplier<String> string, int value) implements Group {
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated:
 * <pre>{@code
 * @ProtobufMixin
 * public class GroupSpec {
 *     @ProtobufSerializer
 *     public static void encode(Group protoInputObject, ProtobufWriter protoWriter) { ... }
 *     @ProtobufDeserializer
 *     public static Group decode(ProtobufReader protoReader) { ... }
 *     @ProtobufSize
 *     public static int sizeOf(Group protoInputObject) { ... }
 * }
 * }</pre>
 *
 * <p>The following default builder class is generated for each permitted subtype:
 * <pre>{@code
 * public class TextGroupBuilder {
 *     private Supplier<String> string;
 *     private int value;
 *
 *     public TextGroupBuilder() {
 *         this.string = null;
 *         this.value = 0;
 *     }
 *
 *     public TextGroupBuilder string(Supplier<String> string) {
 *         this.string = string;
 *         return this;
 *     }
 *
 *     public TextGroupBuilder value(int value) {
 *         this.value = value;
 *         return this;
 *     }
 *
 *     public Group.TextGroup build() {
 *         return new Group.TextGroup(string, value);
 *     }
 * }
 * }</pre>
 *
 * @see ProtobufMessage
 * @see ProtobufProperty
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufGroup {
    /**
     * Specifies the fully qualified name of the referenced Protobuf Group schema.
     * This is used by the CLI to update schemas.
     *
     * @return the fully qualified name of the Protobuf Group schema, or empty if it should be detected automatically
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
