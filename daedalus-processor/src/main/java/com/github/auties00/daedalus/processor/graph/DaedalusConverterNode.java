package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in the converter graph representing a type transformation.
 *
 * <p>Nodes are categorized into two kinds:
 * <ul>
 * <li>{@link Serializer} — a transformation used during serialization, from a
 *     value type to a wire-format type (including {@code void} for
 *     length-prefixed message bodies). A serializer may optionally carry an
 *     associated size calculation method that can be invoked to compute the
 *     serialized byte size of the value type, which is required when writing
 *     {@code MESSAGE} and {@code GROUP} length prefixes.
 * <li>{@link Deserializer} — a transformation used during deserialization, from
 *     a wire-format reader type to a value type.
 * </ul>
 */
sealed interface DaedalusConverterNode {

    /**
     * Returns the source type of the transformation.
     *
     * @return the source type
     */
    TypeMirror from();

    /**
     * Returns the target type of the transformation.
     *
     * @return the target type
     */
    TypeMirror to();

    /**
     * Returns the converter method that performs the transformation.
     *
     * @return the converter method
     */
    DaedalusMethodElement arc();

    /**
     * Returns the companion parameters annotations that a use
     * site must supply to perform the transformation.
     *
     * @return the annotation names, empty if the transformation declares no annotated
     *         parameter
     */
    List<DaedalusConverterParametersElement> parametersAnnotations();

    /**
     * A serializer node representing a value-to-wire transformation, with an
     * optional size calculation method.
     *
     * <p>The {@code sizer}, when present, is the method that computes the
     * serialized byte size of a value of type {@link #from()}. It is used during
     * serialization of {@code MESSAGE} and {@code GROUP} fields to write the
     * length prefix before the field bytes.
     *
     * <p>The sizer field is mutable so that
     * {@code DaedalusConverterGraph#attachSizer(DaedalusMethodElement)} can
     * decorate an existing serializer in place without rebuilding the node set.
     */
    final class Serializer implements DaedalusConverterNode {
        private final TypeMirror from;
        private final TypeMirror to;
        private final DaedalusMethodElement arc;
        private final List<DaedalusConverterParametersElement> arcParametersAnnotations;
        private DaedalusMethodElement sizer;
        private List<DaedalusConverterParametersElement> parametersAnnotations;

        /**
         * Constructs a new serializer node with the given types and converter method.
         *
         * @param from the value type being serialized
         * @param to the wire-format target type
         * @param arc the serializer method
         * @param arcParametersAnnotations the companion parameters annotations
         *        required to invoke {@code arc}
         */
        Serializer(TypeMirror from, TypeMirror to, DaedalusMethodElement arc, List<DaedalusConverterParametersElement> arcParametersAnnotations) {
            this.from = from;
            this.to = to;
            this.arc = arc;
            this.arcParametersAnnotations = arcParametersAnnotations;
            this.parametersAnnotations = arcParametersAnnotations;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TypeMirror from() {
            return from;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TypeMirror to() {
            return to;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DaedalusMethodElement arc() {
            return arc;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The returned annotations are required either by the serializer method or
         * by its sizer.
         */
        @Override
        public List<DaedalusConverterParametersElement> parametersAnnotations() {
            return parametersAnnotations;
        }

        /**
         * Returns the size calculation method registered for this serializer's
         * source type, or {@code null} if none has been attached yet.
         *
         * @return the sizer, or {@code null}
         */
        public DaedalusMethodElement sizer() {
            return sizer;
        }

        /**
         * Sets the size calculation method for this serializer's source type.
         *
         * @param sizer the sizer to attach
         * @param sizerParametersAnnotations the companion
         *        parameters annotations required to invoke {@code sizer}
         */
        public void setSizer(DaedalusMethodElement sizer, List<DaedalusConverterParametersElement> sizerParametersAnnotations) {
            this.sizer = sizer;
            var annotations = new ArrayList<>(arcParametersAnnotations);
            for (var annotation : sizerParametersAnnotations) {
                if (!annotations.contains(annotation)) {
                    annotations.add(annotation);
                }
            }
            this.parametersAnnotations = List.copyOf(annotations);
        }
    }

    /**
     * A deserializer node representing a wire-to-value transformation.
     *
     * @param from the wire-format source type
     * @param to the value type being deserialized
     * @param arc the deserializer method
     * @param parametersAnnotations the companion parameters annotations
     *        required to invoke {@code arc}
     */
    record Deserializer(
            TypeMirror from,
            TypeMirror to,
            DaedalusMethodElement arc,
            List<DaedalusConverterParametersElement> parametersAnnotations
    ) implements DaedalusConverterNode {

    }
}
