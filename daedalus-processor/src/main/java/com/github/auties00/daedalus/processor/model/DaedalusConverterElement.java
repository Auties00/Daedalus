package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Objects;

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
     * that performs the actual type conversion, together with the arguments that the
     * use site passes to its additional parameters.
     */
    sealed interface Attributed extends DaedalusConverterElement {
        /**
         * Returns the converter method that performs the conversion.
         *
         * @return the converter method
         */
        DaedalusMethodElement delegate();

        /**
         * Returns the arguments passed to the additional parameters of
         * {@link #delegate()}, in declaration order.
         *
         * @return the arguments, empty if the method declares no additional parameters
         */
        List<DaedalusConverterArgument> arguments();

        /**
         * Returns the input type of the conversion.
         *
         * @return the input type
         */
        TypeMirror parameterType();

        /**
         * Returns the output type of the conversion.
         *
         * @return the output type
         */
        TypeMirror returnType();

        /**
         * An attributed converter element for serialization.
         *
         * @param delegate the converter method that performs the serialization
         * @param arguments the arguments passed to the additional parameters of
         *        {@code delegate}, in declaration order
         * @param parameterType the input type of the conversion
         * @param returnType the output type of the conversion
         * @param sizer the size calculation method for {@code parameterType}, or
         *        {@code null} if no sizer is registered for this step
         * @param sizerArguments the arguments passed to the additional parameters of
         *        {@code sizer}, in declaration order
         */
        record Serializer(
                DaedalusMethodElement delegate,
                List<DaedalusConverterArgument> arguments,
                TypeMirror parameterType,
                TypeMirror returnType,
                DaedalusMethodElement sizer,
                List<DaedalusConverterArgument> sizerArguments
        ) implements Attributed {
            public Serializer {
                Objects.requireNonNull(delegate, "delegate cannot be null");
                Objects.requireNonNull(arguments, "arguments cannot be null");
                Objects.requireNonNull(parameterType, "parameterType cannot be null");
                Objects.requireNonNull(returnType, "returnType cannot be null");
                Objects.requireNonNull(sizerArguments, "sizerArguments cannot be null");
            }
        }

        /**
         * An attributed converter element for deserialization.
         *
         * @param delegate the converter method that performs the deserialization
         * @param arguments the arguments passed to the additional parameters of
         *        {@code delegate}, in declaration order
         * @param parameterType the input type of the conversion
         * @param returnType the output type of the conversion
         */
        record Deserializer(
                DaedalusMethodElement delegate,
                List<DaedalusConverterArgument> arguments,
                TypeMirror parameterType,
                TypeMirror returnType
        ) implements Attributed {
            public Deserializer {
                Objects.requireNonNull(delegate, "delegate cannot be null");
                Objects.requireNonNull(arguments, "arguments cannot be null");
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
     * @param mixins the mixins to consider during path resolution: a lower precedence tier
     *        is only considered when no path exists in the previous ones
     * @param type whether this element represents a serializer or deserializer
     * @param optional whether the conversion may not exist: an optional element that cannot
     *        be resolved is left unattributed, so that its field type can choose another way
     *        to convert its value, instead of reporting an error
     */
    record Unattributed(
            Element invoker,
            TypeMirror from,
            TypeMirror to,
            DaedalusMixins mixins,
            Type type,
            boolean optional
    ) implements DaedalusConverterElement {
        public Unattributed {
            Objects.requireNonNull(invoker, "invoker cannot be null");
            Objects.requireNonNull(from, "from cannot be null");
            Objects.requireNonNull(to, "to cannot be null");
            Objects.requireNonNull(mixins, "mixins cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }

        /**
         * Creates a required unattributed converter element, whose resolution failure is
         * reported as an error.
         *
         * @param invoker the element that triggered this conversion requirement
         * @param from the source type of the conversion
         * @param to the target type of the conversion
         * @param mixins the mixins to consider during path resolution
         * @param type whether this element represents a serializer or deserializer
         */
        public Unattributed(Element invoker, TypeMirror from, TypeMirror to, DaedalusMixins mixins, Type type) {
            this(invoker, from, to, mixins, type, false);
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
