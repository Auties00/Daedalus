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
 * method per parameter and a terminal method (named {@code build()} by default,
 * configurable via {@link #buildMethodName()}) that delegates to the annotated
 * method or constructor.
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
     * A placeholder that resolves to the name of the builder field.
     */
    String FIELD_NAME = "$FIELD_NAME";

    /**
     * A placeholder that resolves to the simple name of the type being built.
     */
    String CLASS_NAME = "$CLASS_NAME";

    /**
     * A placeholder that resolves to the simple name of the builder field's type.
     */
    String FIELD_TYPE_NAME = "$FIELD_TYPE_NAME";

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
     * <p>The behavior depends on whether the enclosing type is managed by a
     * data format (e.g. {@code @ProtobufMessage}):
     *
     * <ul>
     * <li>If the enclosing type <em>is</em> managed by a data format and the
     *     name is empty (the default), the generated builder <em>replaces</em>
     *     the default builder that the format would otherwise generate. If
     *     non-empty, an additional builder with the given name is generated
     *     alongside the default one.
     * <li>If the enclosing type is <em>not</em> managed by a data format and
     *     the name is empty (the default), the generated builder is named
     *     {@code {EnclosingTypeName}Builder}, following the same naming
     *     convention used by format-managed default builders. If non-empty,
     *     the given name is used directly.
     * </ul>
     *
     * @return the name of the generated builder class, or an empty string
     *         for the default naming behavior
     */
    String name() default "";

    /**
     * Specifies the name of the terminal method that constructs the object.
     *
     * <p>If empty (the default), the terminal method is named {@code build}.
     *
     * @return the name of the terminal build method, or an empty string
     *         for the default {@code build} name
     */
    String buildMethodName() default "";

    /**
     * Specifies the segments that are camelCase-joined to form the name of
     * each generated setter method.
     *
     * <p>Each segment is either a literal string or one of the placeholder
     * constants defined on this annotation ({@link #FIELD_NAME},
     * {@link #CLASS_NAME}, {@link #FIELD_TYPE_NAME}).
     *
     * <p>If empty (the default), the setter name is {@code set} followed
     * by the field name (equivalent to {@code {"set", FIELD_NAME}}).
     *
     * @return the segments that compose the setter method name, or an
     *         empty array for the default behavior
     */
    String[] settersMethodName() default {};

    /**
     * Marks a static method as a mixin that gets mixed into generated builders.
     *
     * <p>The annotated method must be {@code static}, must have at least one
     * parameter, and must return the same type as its first parameter. When a
     * mixin matches, the processor generates a convenience method on the
     * builder that delegates to the annotated static method, passing the
     * matched value as the first argument and forwarding the remaining
     * parameters. The return value is assigned back, allowing the mixin to
     * handle {@code null} values by creating a new instance. The generated
     * method returns the builder instance for fluent chaining.
     *
     * <p>This annotation operates in two distinct matching modes depending on
     * where the annotated method is declared:
     *
     * <h2>Field-level mixin (external mixin class):</h2>
     *
     * <p>When the annotated method is declared in an external class, it
     * matches against individual builder <em>fields</em>. A mixin is applicable
     * to a field if its first parameter type, after substituting the method's
     * type variables against the field type, is exactly equal to the field
     * type. Strict type equality is required because the mixin both consumes
     * the field as its first parameter and assigns its return value back to
     * the same field — both directions must typecheck, which collapses to
     * type equality. As a consequence, a {@code Collection<T>} mixin is
     * <em>not</em> applied to a {@code List<String>} field even though
     * {@code List<String>} is assignable to {@code Collection<String>}; the
     * mixin's {@code Collection<String>} return value cannot be assigned back
     * to the {@code List<String>} field. To support a {@code List<String>}
     * field, declare the mixin method directly for {@code List<T>}.
     *
     * <pre>{@code
     * public final class ListMixin {
     *     @TypeBuilder.Mixin(builderMethodName = {"add", TypeBuilder.FIELD_NAME})
     *     public static <T> List<T> addElement(List<T> list, T value) {
     *         if (list == null) {
     *             var result = new ArrayList<T>();
     *             result.add(value);
     *             return result;
     *         } else {
     *             try {
     *                 list.add(value);
     *                 return list;
     *             } catch (UnsupportedOperationException _) {
     *                 var result = new ArrayList<>(list);
     *                 result.add(value);
     *                 return result;
     *             }
     *         }
     *     }
     * }
     * }</pre>
     *
     * <p>A builder with a {@code List<String>} field named {@code tags} would
     * receive a method named {@code addTags(String value)}. The generated
     * method assigns the return value back to the field:
     * {@code this.tags = ListMixin.addElement(this.tags, value)}.
     *
     * <h2>Type-level mixin (inside a {@link TypeBuilder} annotated type):</h2>
     *
     * <p>This annotation can also be used on static methods declared directly
     * within a type that is {@link TypeBuilder} annotated, either explicitly
     * or implicitly (for example, types managed by a specific data format may
     * be implicitly {@code @TypeBuilder} annotated). In this mode the matching
     * rule is different: the first parameter must equal the <em>enclosing
     * type</em> rather than any individual field. The generated convenience
     * method operates on the entire built instance rather than on a single
     * field, and the return value replaces the in-progress builder state.
     *
     * <pre>{@code
     * public class TagsMessage {
     *     private final List<String> tags;
     *
     *     @TypeBuilder
     *     public TagsMessage(List<String> tags) {
     *         this.tags = Objects.requireNonNull(tags);
     *     }
     *
     *     @TypeBuilder.Mixin(builderMethodName = {"withReversedTags"})
     *     public static TagsMessage reverseTags(TagsMessage message) {
     *         try {
     *             Collections.reverse(message.tags);
     *         } catch (UnsupportedOperationException _) {
     *             var reversed = new ArrayList<>(message.tags);
     *             Collections.reverse(reversed);
     *             message = new TagsMessage(reversed);
     *         }
     *         return message;
     *     }
     * }
     * }</pre>
     *
     * <p>Multiple mixins can match the same builder field (or enclosing type),
     * in which case a separate convenience method is generated for each match.
     *
     * <p>The name of the generated builder method is determined by
     * {@link #builderMethodName()}, which accepts an array of segments that
     * are camelCase-joined at generation time. Segments can be literal strings
     * or placeholder constants defined on {@link TypeBuilder} ({@link #FIELD_NAME},
     * {@link #CLASS_NAME}, {@link #FIELD_TYPE_NAME}).
     *
     * @see TypeBuilder
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Mixin {
        /**
         * Specifies the segments that are camelCase-joined to form the name of
         * the generated builder method.
         *
         * <p>Each segment is either a literal string or one of the placeholder
         * constants defined on {@link TypeBuilder} ({@link #FIELD_NAME},
         * {@link #CLASS_NAME}, {@link #FIELD_TYPE_NAME}).
         *
         * @return the segments that compose the generated method name
         */
        String[] builderMethodName();
    }
}
