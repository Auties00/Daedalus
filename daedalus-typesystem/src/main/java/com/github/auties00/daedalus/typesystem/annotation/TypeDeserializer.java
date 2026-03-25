package com.github.auties00.daedalus.typesystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method or constructor as a deserializer that converts from a
 * wire-level representation into an instance of the enclosing type.
 *
 * <p>This annotation can be applied to static methods or constructors in a type
 * annotated with {@link TypeMixin}, or directly in a type managed by a specific
 * data format. The annotated method must accept one mandatory parameter whose
 * type is the wire-level representation to convert from, and must return an
 * instance of the target type (or {@code null}).
 *
 * <h2>Additional parameters:</h2>
 *
 * <p>The deserializer method may declare additional parameters after the
 * mandatory wire-level parameter. These parameters act as configuration
 * values that are supplied at the use-site through a generated annotation.
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
 * <p>The corresponding {@link TypeSerializer} and {@link TypeSize} methods
 * must accept the same additional parameters in the same order.
 *
 * <h2>In a {@link TypeMixin}:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class AtomicMixin {
 *     @TypeDeserializer
 *     public static AtomicInteger ofNullable(Integer value) {
 *         return value == null ? new AtomicInteger() : new AtomicInteger(value);
 *     }
 * }
 * }</pre>
 *
 * <h2>With additional parameters:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class InstantMixin {
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
 *
 *     @TypeSerializer
 *     public static long toNullable(Instant value, TimeUnit unit) {
 *         return switch (unit) {
 *             case SECONDS -> value.getEpochSecond();
 *             case MILLISECONDS -> value.toEpochMilli();
 *             case NANOSECONDS -> value.getEpochSecond() * 1_000_000_000L + value.getNano();
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
 * @see TypeSerializer
 * @see TypeSize
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeDeserializer {
    /**
     * Provides an optional warning message that should be printed by the compiler when this deserializer is used.
     * By default, no warning message is specified.
     *
     * @return a string containing the warning message; if not specified, an empty string is returned
     */
    String warning() default "";
}
