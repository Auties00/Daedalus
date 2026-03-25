package com.github.auties00.daedalus.typesystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a serializer that converts an instance of the enclosing
 * type into a wire-level representation.
 *
 * <p>This annotation can be applied to instance methods or static methods in a
 * type annotated with {@link TypeMixin}, or directly in a type managed by a
 * specific data format. In a mixin, the method must be {@code static} and
 * accept the source type as its first parameter. In a non-mixin type, the
 * method is typically an instance method with no mandatory parameters.
 *
 * <p>Implementing a corresponding {@link TypeSize} method is optional when the
 * serialized size can be inferred from the return type, but may be required by
 * certain data formats when writing directly to a low-level writer.
 *
 * <h2>Additional parameters:</h2>
 *
 * <p>The serializer method may declare additional parameters after the
 * mandatory source-type parameter (in a mixin) or with no preceding mandatory
 * parameter (in a non-mixin instance method). These parameters act as
 * configuration values that are supplied at the use-site through a generated
 * annotation.
 *
 * <p>Every additional parameter must be of a type that is legal in a Java
 * annotation element, that is: a primitive type ({@code boolean}, {@code byte},
 * {@code char}, {@code short}, {@code int}, {@code long}, {@code float},
 * {@code double}), {@link String}, {@link Class}, an {@code enum} type,
 * an annotation type, or a one-dimensional array of any of these.
 *
 * <p>When additional parameters are present, the annotation processor generates
 * a companion annotation named {@code {TypeName}Parameters} whose methods
 * mirror those parameters. This annotation is then applied at the use-site to
 * supply the values.
 *
 * <p>The corresponding {@link TypeDeserializer} and {@link TypeSize} methods
 * must accept the same additional parameters in the same order.
 *
 * <h2>In a {@link TypeMixin}:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class AtomicMixin {
 *     @TypeSerializer
 *     public static int toNullable(AtomicInteger value) {
 *         return value.get();
 *     }
 * }
 * }</pre>
 *
 * <h2>With additional parameters:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class InstantMixin {
 *     @TypeSerializer
 *     public static long toNullable(Instant value, TimeUnit unit) {
 *         return switch (unit) {
 *             case SECONDS -> value.getEpochSecond();
 *             case MILLISECONDS -> value.toEpochMilli();
 *             case NANOSECONDS -> value.getEpochSecond() * 1_000_000_000L + value.getNano();
 *             default -> throw new UnsupportedOperationException();
 *         };
 *     }
 *
 *     @TypeDeserializer
 *     public static Instant ofNullable(Long value, TimeUnit unit) {
 *         if (value == null) {
 *             return null;
 *         }
 *
 *         return switch (unit) {
 *             case SECONDS -> Instant.ofEpochSecond(value);
 *             case MILLISECONDS -> Instant.ofEpochMilli(value);
 *             case NANOSECONDS -> Instant.ofEpochSecond(0, value);
 *             default -> throw new UnsupportedOperationException();
 *         };
 *     }
 * }
 * }</pre>
 *
 * <p>The annotation processor generates the following companion annotation:
 * <pre><code>
 * &#64;Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public &#64;interface InstantMixinParameters {
 *     TimeUnit unit();
 * }
 * </code></pre>
 *
 * @see TypeDeserializer
 * @see TypeSize
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeSerializer {
    /**
     * Provides an optional warning message that should be printed by the compiler when this serializer is used.
     * By default, no warning message is specified.
     *
     * @return a string containing the warning message; if not specified, an empty string is returned
     */
    String warning() default "";
}
