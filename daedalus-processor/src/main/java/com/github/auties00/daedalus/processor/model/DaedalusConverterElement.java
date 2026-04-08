package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;
import java.util.Set;

/**
 * A converter element that represents a type conversion step in a serialization
 * or deserialization chain.
 *
 * <p>Converter elements exist in two states:
 * <ul>
 * <li>{@link Unattributed} elements have been discovered but not yet resolved
 *     against the converter graph. They carry the source and target types along
 *     with a textual description of the expected target format.
 * <li>{@link Attributed} elements have been fully resolved and carry a reference
 *     to the {@link DaedalusMethodElement} that performs the conversion. Attributed
 *     elements are further divided into {@link Attributed.Serializer} and
 *     {@link Attributed.Deserializer} variants.
 * </ul>
 */
public sealed interface DaedalusConverterElement {

    /**
     * An attributed converter element that has been fully resolved against the
     * converter graph.
     *
     * <p>Attributed elements carry a reference to the {@link DaedalusMethodElement}
     * that performs the actual type conversion.
     */
    sealed interface Attributed extends DaedalusConverterElement {

        /**
         * An attributed converter element for serialization.
         *
         * @param delegate the converter method that performs the serialization
         * @param parameterType the input type of the conversion
         * @param returnType the output type of the conversion
         * @param sizer the size calculation method for {@code parameterType}, or
         *        {@code null} if no sizer is registered for this step
         */
        record Serializer(
                DaedalusMethodElement delegate,
                TypeMirror parameterType,
                TypeMirror returnType,
                DaedalusMethodElement sizer
        ) implements Attributed {
            public Serializer {
                Objects.requireNonNull(delegate, "delegate cannot be null");
                Objects.requireNonNull(parameterType, "parameterType cannot be null");
                Objects.requireNonNull(returnType, "returnType cannot be null");
            }
        }

        /**
         * An attributed converter element for deserialization.
         *
         * @param delegate the converter method that performs the deserialization
         * @param parameterType the input type of the conversion
         * @param returnType the output type of the conversion
         */
        record Deserializer(
                DaedalusMethodElement delegate,
                TypeMirror parameterType,
                TypeMirror returnType
        ) implements Attributed {
            public Deserializer {
                Objects.requireNonNull(delegate, "delegate cannot be null");
                Objects.requireNonNull(parameterType, "parameterType cannot be null");
                Objects.requireNonNull(returnType, "returnType cannot be null");
            }
        }
    }

    /**
     * An unattributed converter element that has been discovered but not yet
     * resolved against the converter graph.
     *
     * @param invoker the element that triggered this conversion requirement
     * @param from the source type of the conversion
     * @param to the target type of the conversion
     * @param mixins the mixin type elements to consider during path resolution
     * @param type whether this element represents a serializer or deserializer
     */
    record Unattributed(
            Element invoker,
            TypeMirror from,
            TypeMirror to,
            Set<TypeElement> mixins,
            Type type
    ) implements DaedalusConverterElement {
        public Unattributed {
            Objects.requireNonNull(invoker, "invoker cannot be null");
            Objects.requireNonNull(from, "from cannot be null");
            Objects.requireNonNull(to, "to cannot be null");
            Objects.requireNonNull(mixins, "mixins cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }

        /**
         * The direction of a converter element.
         */
        public enum Type {
            /**
             * The converter element transforms a value for serialization.
             */
            SERIALIZER,

            /**
             * The converter element transforms a value for deserialization.
             */
            DESERIALIZER
        }
    }
}
