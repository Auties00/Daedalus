package com.github.auties00.daedalus.protobuf.annotation;

import com.github.auties00.daedalus.protobuf.model.ProtobufEnumType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to enums, classes, records, sealed interfaces,
 * and sealed abstract classes to represent a Protobuf enum.
 * If no constant is annotated with {@link ProtobufDefaultValue}, {@code null} will be used implicitly.
 *
 * <p>There are two ways to define the protobuf index for each constant:
 * <ul>
 * <li>Annotate each constant ({@code static final} field or enum constant) with {@link Constant} to assign an explicit index.
 * <li>Use {@link ProtobufSerializer} and {@link ProtobufDeserializer} to define custom conversion logic between the type and its {@code int} index.
 * </ul>
 *
 * <p>{@link Constant} and {@link ProtobufSerializer}/{@link ProtobufDeserializer} cannot be mixed
 * in the same type. Use one approach or the other, never both.
 *
 * <p>When using {@link Constant}, all known instances are declared as {@code static final}
 * fields (or enum constants), so no deserializer is needed — the decoder
 * resolves instances by looking them up in the generated values map.
 * When using {@link ProtobufSerializer}/{@link ProtobufDeserializer}, the serializer
 * converts an instance to its {@code int} index and the deserializer creates an instance
 * from an {@code int} index.
 * For sealed interfaces and sealed abstract classes, the permitted subtypes must each
 * be annotated with {@link ProtobufEnum} individually.
 *
 * <p>Protobuf does not allow {@code ENUM} as a map key type in {@link ProtobufMessage.MapField}.
 * To use an enum as a map key, declare the key type as {@code ProtobufType.INT32}
 * (or another type that converts to {@code int}), and the enum will be automatically
 * converted using the generated Spec mixin.
 *
 * <h2>As an enum:</h2>
 *
 * <h3>With {@link Constant}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public enum Status {
 *     @ProtobufEnum.Constant(index = 0)
 *     ACTIVE,
 *     @ProtobufEnum.Constant(index = 1)
 *     INACTIVE,
 *     @ProtobufEnum.Constant(index = 2)
 *     SUSPENDED
 * }
 * }</pre>
 *
 * <h3>With {@link ProtobufSerializer}/{@link ProtobufDeserializer}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public enum Status {
 *     ACTIVE(0),
 *     INACTIVE(1),
 *     SUSPENDED(2);
 *
 *     private static final Map<Integer, Status> VALUES = Arrays.stream(values())
 *             .collect(Collectors.toUnmodifiableMap(Status::index, Function.identity()));
 *
 *     private final int index;
 *
 *     Status(int index) {
 *         this.index = index;
 *     }
 *
 *     @ProtobufSerializer
 *     int index() {
 *         return index;
 *     }
 *
 *     @ProtobufDeserializer
 *     static Status of(int index) {
 *         return VALUES.get(index);
 *     }
 * }
 * }</pre>
 *
 * <p>The following Spec class is generated for both approaches:
 * <pre>{@code
 * @ProtobufMixin
 * public class StatusSpec {
 *     @ProtobufSerializer
 *     public static int encode(Status protoInputObject) { ... }
 *
 *     @ProtobufDeserializer
 *     public static Status decode(int protoEnumIndex) { ... }
 * }
 * }</pre>
 *
 * <h2>As a class:</h2>
 *
 * <h3>Concrete with {@link Constant}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public final class Status {
 *     @ProtobufEnum.Constant(index = 0)
 *     static final Status ACTIVE = new Status(0);
 *     @ProtobufEnum.Constant(index = 1)
 *     static final Status INACTIVE = new Status(1);
 *     @ProtobufEnum.Constant(index = 2)
 *     static final Status SUSPENDED = new Status(2);
 *
 *     private final int index;
 *
 *     Status(int index) {
 *         this.index = index;
 *     }
 * }
 * }</pre>
 *
 * <h3>Concrete with {@link ProtobufSerializer}/{@link ProtobufDeserializer}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public final class Status {
 *     private final int index;
 *
 *     Status(int index) {
 *         this.index = index;
 *     }
 *
 *     @ProtobufSerializer
 *     int index() {
 *         return index;
 *     }
 *
 *     @ProtobufDeserializer
 *     static Status of(int index) {
 *         return new Status(index);
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed abstract with {@link Constant}:</h3>
 * <p>Only sealed abstract classes are supported. Non-sealed abstract classes cannot be used
 * because the set of permitted subtypes must be known at compile time.
 * <pre>{@code
 * @ProtobufEnum
 * public sealed abstract class Status {
 *     @ProtobufEnum.Constant(index = 0)
 *     static final Status ACTIVE = new Active();
 *     @ProtobufEnum.Constant(index = 1)
 *     static final Status INACTIVE = new Inactive();
 *
 *     @ProtobufEnum
 *     static final class Active extends Status {
 *     }
 *
 *     @ProtobufEnum
 *     static final class Inactive extends Status {
 *     }
 * }
 * }</pre>
 *
 * <h3>Sealed abstract with {@link ProtobufSerializer}/{@link ProtobufDeserializer}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public sealed abstract class Status {
 *     private final int index;
 *
 *     protected Status(int index) {
 *         this.index = index;
 *     }
 *
 *     @ProtobufSerializer
 *     int index() {
 *         return index;
 *     }
 *
 *     @ProtobufDeserializer
 *     static Status of(int index) {
 *         return switch (index) {
 *             case 0 -> new Active(0);
 *             case 1 -> new Inactive(1);
 *             default -> null;
 *         };
 *     }
 *
 *     @ProtobufEnum
 *     static final class Active extends Status {
 *         Active(int index) {
 *             super(index);
 *         }
 *     }
 *
 *     @ProtobufEnum
 *     static final class Inactive extends Status {
 *         Inactive(int index) {
 *             super(index);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>As a record:</h2>
 *
 * <h3>With {@link Constant}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public record Status(int index) {
 *     @ProtobufEnum.Constant(index = 0)
 *     static final Status ACTIVE = new Status(0);
 *     @ProtobufEnum.Constant(index = 1)
 *     static final Status INACTIVE = new Status(1);
 *     @ProtobufEnum.Constant(index = 2)
 *     static final Status SUSPENDED = new Status(2);
 * }
 * }</pre>
 *
 * <h3>With {@link ProtobufSerializer}/{@link ProtobufDeserializer}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public record Status(int index) {
 *     @ProtobufSerializer
 *     int index() {
 *         return index;
 *     }
 *
 *     @ProtobufDeserializer
 *     static Status of(int index) {
 *         return new Status(index);
 *     }
 * }
 * }</pre>
 *
 * <h2>As an interface:</h2>
 * <p>Only sealed interfaces are supported. Non-sealed interfaces cannot be used
 * because the set of permitted subtypes must be known at compile time.
 *
 * <h3>With {@link Constant}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public sealed interface Status {
 *     @ProtobufEnum.Constant(index = 0)
 *     Status ACTIVE = new Active();
 *     @ProtobufEnum.Constant(index = 1)
 *     Status INACTIVE = new Inactive();
 *
 *     @ProtobufEnum
 *     record Active() implements Status {
 *     }
 *
 *     @ProtobufEnum
 *     record Inactive() implements Status {
 *     }
 * }
 * }</pre>
 *
 * <h3>With {@link ProtobufSerializer}/{@link ProtobufDeserializer}:</h3>
 * <pre>{@code
 * @ProtobufEnum
 * public sealed interface Status {
 *     @ProtobufSerializer
 *     int index();
 *
 *     @ProtobufEnum
 *     record Active(int index) implements Status {
 *     }
 *
 *     @ProtobufEnum
 *     record Inactive(int index) implements Status {
 *     }
 *
 *     @ProtobufDeserializer
 *     static Status of(int index) {
 *         return switch (index) {
 *             case 0 -> new Active(0);
 *             case 1 -> new Inactive(1);
 *             default -> null;
 *         };
 *     }
 * }
 * }</pre>
 *
 * @see Constant
 * @see ProtobufSerializer
 * @see ProtobufDeserializer
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufEnum {
    /**
     * The minimum valid index for a Protobuf enum constant.
     */
    int MIN_CONSTANT_INDEX = -2147483648; // -2^31

    /**
     * Represents the maximum allowable index for a Protobuf enum constant.
     */
    int MAX_CONSTANT_INDEX = 2147483647; // 2^31 - 1

    /**
     * Specifies the fully qualified name of the referenced Protobuf Enum schema.
     * This is used by the CLI to update schemas.
     *
     * @return the fully qualified name of the Protobuf Enum schema, or empty if it should be detected automatically
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
     * Specifies how out-of-range enum values are handled during deserialization.
     * <p>
     * The default behaviour depends on the protobuf version or edition:
     * <ul>
     *     <li><strong>proto2:</strong> {@link ProtobufEnumType#CLOSED}</li>
     *     <li><strong>proto3:</strong> {@link ProtobufEnumType#OPEN}</li>
     *     <li><strong>edition 2023/2024:</strong> {@link ProtobufEnumType#OPEN}</li>
     * </ul>
     *
     * @return the enum type strategy, defaulting to {@link ProtobufEnumType#EDITION_DEFAULT}
     * @see ProtobufEnumType
     */
    ProtobufEnumType type() default ProtobufEnumType.EDITION_DEFAULT;

    /**
     * This annotation can be applied to enum constants or {@code static final} fields
     * in a type annotated with {@link ProtobufEnum} to assign an explicit protobuf index.
     * The annotated field must be at least package-private (not {@code private}).
     * For enum constants, this visibility requirement is implicitly satisfied.
     *
     * <p>This annotation cannot be used together with
     * {@link ProtobufSerializer}/{@link ProtobufDeserializer} in the same type.
     *
     * @see ProtobufEnum
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Constant {

        /**
         * Returns the index associated with the Protobuf enum constant.
         *
         * @return the index of the constant, between {@link ProtobufEnum#MIN_CONSTANT_INDEX} and {@link ProtobufEnum#MAX_CONSTANT_INDEX}
         */
        int index();
    }
}
