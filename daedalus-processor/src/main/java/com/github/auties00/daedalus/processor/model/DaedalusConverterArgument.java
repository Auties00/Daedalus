package com.github.auties00.daedalus.processor.model;

import javax.lang.model.type.TypeMirror;
import java.util.Objects;

/**
 * An argument passed to an additional parameter of a converter method at a use site.
 *
 * <p>Converter methods annotated with {@code @TypeSerializer}, {@code @TypeDeserializer}
 * or {@code @TypeSize} may declare parameters after the mandatory one that receives the
 * value to convert. Each of these parameters receives one argument, in declaration order:
 * <ul>
 * <li>{@link Value} arguments are compile time constants supplied by the companion
 *     parameters annotation applied to the field that uses the converter.
 * <li>{@link Injected} arguments are the reader or writer types that a data format
 *     supplies itself, and that the generated code of that format passes to the method.
 * </ul>
 *
 * <p>Instances of this interface are immutable and thread-safe.
 */
public sealed interface DaedalusConverterArgument {
    /**
     * Returns the name of the parameter that receives this argument.
     *
     * @return the parameter name
     */
    String name();

    /**
     * An argument whose value is supplied by the companion parameters annotation of a field.
     *
     * @param name the name of the parameter that receives the argument
     * @param expression the Java source expression that evaluates to the value of the
     *        argument, which is always a compile time constant or an array of them
     */
    record Value(String name, String expression) implements DaedalusConverterArgument {
        /**
         * Constructs a new value argument.
         *
         * @param name the name of the parameter that receives the argument
         * @param expression the Java source expression of the argument
         * @throws NullPointerException if {@code name} or {@code expression} is {@code null}
         */
        public Value {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(expression, "expression cannot be null");
        }
    }

    /**
     * An argument whose value is supplied by the generated code of a data format, such as
     * the writer that a serializer writes to.
     *
     * @param name the name of the parameter that receives the argument
     * @param type the reader or writer type of the parameter
     */
    record Injected(String name, TypeMirror type) implements DaedalusConverterArgument {
        /**
         * Constructs a new injected argument.
         *
         * @param name the name of the parameter that receives the argument
         * @param type the reader or writer type of the parameter
         * @throws NullPointerException if {@code name} or {@code type} is {@code null}
         */
        public Injected {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }
    }
}
