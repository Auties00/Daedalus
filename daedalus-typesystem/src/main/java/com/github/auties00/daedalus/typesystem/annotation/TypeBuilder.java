package com.github.auties00.daedalus.typesystem.annotation;

import com.github.auties00.daedalus.typesystem.model.ClassModifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a fluent builder class with customizable construction logic.
 *
 * <p>This annotation can be applied to static methods or constructors to
 * auto-generate a builder class that provides a fluent API for constructing
 * instances of the enclosing type. The generated builder exposes one setter
 * method per parameter and a terminal {@code build()} method that delegates
 * to the annotated method or constructor.
 *
 * <p>When used inside a type managed by a specific data format, the parameters
 * of the annotated method or constructor may need to be annotated with format-specific
 * annotations.
 *
 * @see ClassModifier
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeBuilder {
    /**
     * Specifies the visibility and mutability modifiers for the generated builder class.
     *
     * @return an array of modifiers to apply to the generated builder class
     */
    ClassModifier[] modifiers() default {
            ClassModifier.PUBLIC,
            ClassModifier.FINAL
    };

    /**
     * Specifies the name of the generated builder class.
     *
     * <p>If empty (the default), the generated builder replaces the default
     * builder that would otherwise be created by the enclosing type's data
     * format. If non-empty, an additional builder with the given name is
     * generated alongside the default one.
     *
     * @return the name of the generated builder class, or an empty string
     *         to override the default builder
     */
    String name() default "";
}
