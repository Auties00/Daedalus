package com.github.auties00.daedalus.typesystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a mixin that provides serialization, deserialization,
 * and default-value logic for existing types that cannot be modified
 * directly, such as built-in Java types.
 *
 * <p>A mixin class is typically {@code final} with a private constructor and
 * contains only {@code static} methods annotated with {@link TypeSerializer},
 * {@link TypeDeserializer}, and/or {@link TypeDefaultValue}. Built-in mixins
 * for common Java types are provided under the
 * {@link com.github.auties00.daedalus.typesystem.adapter} package.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class URIMixin {
 *     @TypeDeserializer
 *     public static URI ofNullable(String value) {
 *         return value == null ? null : URI.create(value);
 *     }
 *
 *     @TypeSerializer
 *     public static String toNullable(URI value) {
 *         return value == null ? null : value.toString();
 *     }
 * }
 * }</pre>
 *
 * @see TypeSerializer
 * @see TypeDeserializer
 * @see TypeDefaultValue
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeMixin {
    /**
     * Specifies the visibility scope of this mixin, controlling where it is automatically
     * applied during serialization and deserialization.
     *
     * @return the scope of this mixin, defaulting to {@link Scope#MANUAL}
     */
    Scope scope() default Scope.MANUAL;

    /**
     * Specifies additional mixins to import into the scope of this mixin.
     * This allows a mixin to compose and reuse serialization and deserialization
     * logic defined in other mixin classes.
     *
     * @return an array of {@link Import} annotations referencing other mixins
     */
    Import[] imports() default {};

    /**
     * A visibility scope that controls where a {@link TypeMixin} is automatically applied.
     * <p>
     * Scopes form a hierarchy from most restrictive to least restrictive:
     * <pre>
     * MANUAL &gt; PACKAGE &gt; MODULE &gt; GLOBAL
     * </pre>
     * A broader scope includes all the types visible to narrower scopes.
     * For example, {@link #MODULE} applies everywhere {@link #PACKAGE} does, and more.
     */
    enum Scope {
        /**
         * The mixin must be explicitly referenced where it is necessary.
         * This is the most restrictive scope and the default.
         */
        MANUAL,

        /**
         * The mixin is automatically applied to all types within the same package.
         * This is broader than {@link #MANUAL} but narrower than {@link #MODULE}.
         */
        PACKAGE,

        /**
         * The mixin is automatically applied to all types within the same module.
         * This is broader than {@link #PACKAGE} but narrower than {@link #GLOBAL}.
         */
        MODULE,

        /**
         * The mixin is automatically applied to all types.
         * This is the least restrictive scope.
         */
        GLOBAL
    }

    /**
     * An annotation that references a set of mixin classes to import into the
     * scope of the enclosing {@link TypeMixin}.
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Import {
        /**
         * Specifies the mixin classes to import.
         *
         * @return an array of classes annotated with {@link TypeMixin}
         */
        Class<?>[] value();
    }
}
