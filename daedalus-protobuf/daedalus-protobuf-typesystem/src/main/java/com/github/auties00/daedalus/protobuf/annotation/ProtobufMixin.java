package com.github.auties00.daedalus.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to a type to represent a Protobuf Mixin.
 * Protobuf mixins are used to provide additional on existing types that cannot be modified, like built-in Java types.
 * This library provides a number of built-in mixins for common use cased under the {@link com.github.auties00.daedalus.protobuf.builtin} package.
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @ProtobufMixin
 * public final class ProtobufURIMixin {
 *     @ProtobufDeserializer
 *     public static URI ofNullable(Supplier<String> value) {
 *         return value == null ? null : URI.create(value.toString());
 *     }
 *
 *     @ProtobufSerializer
 *     public static Supplier<String> toValue(URI value) {
 *         return value == null ? null : ProtobufString.wrap(value.toString());
 *     }
 * }
 * }</pre>
 *
 * @see ProtobufSerializer
 * @see ProtobufDeserializer
 * @see ProtobufDefaultValue
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMixin {
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
     * A visibility scope that controls where a {@link ProtobufMixin} is automatically applied.
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
     * scope of the enclosing {@link ProtobufMixin}.
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Import {
        /**
         * Specifies the mixin classes to import.
         *
         * @return an array of classes annotated with {@link ProtobufMixin}
         */
        Class<?>[] value();
    }
}
