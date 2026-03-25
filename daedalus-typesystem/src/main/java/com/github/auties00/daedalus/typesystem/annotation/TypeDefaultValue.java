package com.github.auties00.daedalus.typesystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the default value of a type.
 *
 * <p>This annotation can be applied to:
 * <ul>
 * <li>A static method that takes no arguments and returns an instance of the enclosing type.
 * <li>A static field, which includes enum constants, which is an instance of the enclosing type.
 * </ul>
 *
 * <h2>In a {@link TypeMixin}:</h2>
 * <pre>{@code
 * @TypeMixin
 * public final class AtomicMixin {
 *     @TypeDefaultValue
 *     public static AtomicInteger newAtomicInt() {
 *         return new AtomicInteger();
 *     }
 * }
 * }</pre>
 *
 * @see TypeMixin
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeDefaultValue {

}
