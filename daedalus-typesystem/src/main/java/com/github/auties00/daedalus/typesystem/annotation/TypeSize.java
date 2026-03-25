package com.github.auties00.daedalus.typesystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that computes the serialized size, in bytes, of the value
 * produced by a corresponding {@link TypeSerializer}.
 *
 * <p>This annotation can be applied to instance methods or static methods in a
 * type annotated with {@link TypeMixin}, or directly in a type managed by a
 * specific data format. The method must return an {@code int} representing the
 * byte count.
 *
 * <p>Implementing a {@code TypeSize} method is optional when the serialized
 * size can be inferred from the serializer's return type, but may be required
 * by certain data formats when writing directly to a low-level writer.
 *
 * <h2>Additional parameters:</h2>
 *
 * <p>If the corresponding {@link TypeSerializer} and {@link TypeDeserializer}
 * methods declare additional parameters, the {@code TypeSize} method must
 * accept the same additional parameters in the same order. See
 * {@link TypeSerializer} for a full description of this feature and the
 * constraints on parameter types.
 *
 * @see TypeSerializer
 * @see TypeDeserializer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeSize {

}
