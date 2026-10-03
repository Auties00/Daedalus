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
 * {@link TypeDeserializer}, {@link TypeSize} and/or {@link TypeDefaultValue}. The
 * methods annotated with {@link TypeSerializer}, {@link TypeDeserializer} or
 * {@link TypeSize} can also be instance methods, which are invoked on the instance of
 * the mixin provided by its member annotated with {@link Instance} or {@link Factory}.
 * Built-in global mixins
 * for the value types of the JDK standard library are provided by the
 * {@code com.github.auties00.daedalus.typesystem.jdk.support} package, and its
 * subpackages, of the {@code daedalus-typesystem-jdk-support} module.
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
     * A static field or a static method annotated {@code @TypeMixin.Instance} is one that
     * provides the instance of its enclosing mixin on which the instance methods of the
     * mixin annotated with {@link TypeSerializer}, {@link TypeDeserializer} or
     * {@link TypeSize} are invoked.
     *
     * <p>The generated code reads the field, or invokes the method, every time it invokes
     * one of those instance methods, so the field or the method must always provide the
     * same instance: the developer guarantees it, as it is not checked. A method must take
     * no parameters. The type of the field, or the return type of the method, must be
     * assignable to the mixin.
     *
     * <p>A mixin that declares at least one non-static converter must have exactly one
     * member annotated with either {@code @TypeMixin.Instance} or {@link Factory}, and a
     * mixin whose converters are all static needs neither. For example:
     *
     * <pre>{@code
     * @TypeMixin
     * public final class URIMixin {
     *     @TypeMixin.Instance
     *     public static final URIMixin INSTANCE = new URIMixin();
     *
     *     @TypeSerializer
     *     public String toNullable(URI value) {
     *         return value == null ? null : value.toString();
     *     }
     * }
     * }</pre>
     *
     * @see Factory
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Instance {

    }

    /**
     * A constructor or a static method annotated {@code @TypeMixin.Factory} is one that
     * creates the instance of its enclosing mixin on which the instance methods of the
     * mixin annotated with {@link TypeSerializer}, {@link TypeDeserializer} or
     * {@link TypeSize} are invoked.
     *
     * <p>The constructor or the method is invoked once, the first time one of those
     * instance methods is invoked: the processor generates a holder class named
     * {@code {MixinName}InstanceHolder}, in the package of the mixin, whose static field
     * {@code INSTANCE} keeps the created instance. The constructor or the method must take
     * no parameters, must not be private, and the return type of the method must be
     * assignable to the mixin. A constructor cannot belong to an abstract class.
     *
     * <p>A mixin that declares at least one non-static converter must have exactly one
     * member annotated with either {@link Instance} or {@code @TypeMixin.Factory}, and a
     * mixin whose converters are all static needs neither.
     *
     * @see Instance
     */
    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Factory {

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
