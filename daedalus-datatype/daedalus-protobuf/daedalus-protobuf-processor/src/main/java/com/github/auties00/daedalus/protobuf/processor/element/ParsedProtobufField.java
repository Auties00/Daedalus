package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.*;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A normalized representation of a protobuf field annotation.
 *
 * <p>The Protocol Buffer annotation model uses separate annotation types for each
 * field type (e.g. {@link ProtobufMessage.FloatField}, {@link ProtobufMessage.Int32Field}).
 * This sealed interface unifies them into a common model with implementations for
 * each category of field annotation.
 */
public sealed interface ParsedProtobufField {
    /**
     * The field annotations of {@link ProtobufMessage}, in declaration order.
     */
    List<Class<? extends Annotation>> FIELD_ANNOTATIONS = List.of(
            ProtobufMessage.FloatField.class,
            ProtobufMessage.DoubleField.class,
            ProtobufMessage.BoolField.class,
            ProtobufMessage.Int32Field.class,
            ProtobufMessage.Sint32Field.class,
            ProtobufMessage.Uint32Field.class,
            ProtobufMessage.Fixed32Field.class,
            ProtobufMessage.Sfixed32Field.class,
            ProtobufMessage.Int64Field.class,
            ProtobufMessage.Sint64Field.class,
            ProtobufMessage.Uint64Field.class,
            ProtobufMessage.Fixed64Field.class,
            ProtobufMessage.Sfixed64Field.class,
            ProtobufMessage.EnumField.class,
            ProtobufMessage.StringField.class,
            ProtobufMessage.BytesField.class,
            ProtobufMessage.MessageField.class,
            ProtobufMessage.MapField.class,
            ProtobufMessage.OneOfField.class
    );

    /**
     * A normalized field annotation whose values all have the same {@linkplain #type() type},
     * which is any field annotation but the one of a map.
     */
    sealed interface Value extends ParsedProtobufField permits Scalar, String, Bytes, Message {
        /**
         * Returns the type of the values of this field.
         *
         * @return the type of the values
         */
        ProtobufType type();
    }

    /**
     * Returns the field index.
     *
     * @return the field index
     */
    int index();

    /**
     * Returns the custom name for text serialization, or an empty string for the default.
     *
     * @return the field name
     */
    java.lang.String name();

    /**
     * Returns a supplier for the mixin classes associated with this field.
     *
     * @return the mixin classes supplier
     */
    Supplier<Class<?>[]> mixins();


    /**
     * Returns whether this field has legacy required presence semantics.
     *
     * <p>The default implementation returns {@code false}. Variants that carry a
     * {@link ProtobufFieldPresence} attribute override this method.
     *
     * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
     */
    boolean required();

    /**
     * Returns whether this field uses packed encoding for repeated values.
     *
     * <p>The default implementation returns {@code false}. Variants that carry a
     * {@link ProtobufRepeatedFieldEncoding} attribute override this method.
     *
     * @return {@code true} if the repeated field encoding is {@link ProtobufRepeatedFieldEncoding#PACKED}
     */
    boolean packed();

    /**
     * Returns the declared presence of this field.
     *
     * <p>A map field has no presence attribute, so its declared presence is always
     * {@link ProtobufFieldPresence#VERSION_DEFAULT}.
     *
     * @return the declared field presence, which may be {@link ProtobufFieldPresence#VERSION_DEFAULT}
     */
    ProtobufFieldPresence fieldPresence();

    /**
     * Returns the simple names of the field annotations present on the given element.
     *
     * <p>An element may be annotated with at most one field annotation, so a result with
     * more than one name identifies a conflict.
     *
     * @param element the element to inspect
     * @return the simple names of the field annotations, in declaration order of
     *         {@link ProtobufMessage}
     */
    static List<java.lang.String> annotationNames(Element element) {
        var result = new ArrayList<java.lang.String>();
        for (var annotation : FIELD_ANNOTATIONS) {
            if (element.getAnnotation(annotation) != null) {
                result.add(annotation.getSimpleName());
            }
        }
        return result;
    }

    /**
     * Returns the cases of the given oneof, normalized like the field annotations of the
     * same kind.
     *
     * <p>Every case has {@link ProtobufFieldPresence#EXPLICIT explicit} presence and the
     * {@link ProtobufRepeatedFieldEncoding#EXPANDED expanded} encoding, as a case is never
     * repeated. The cases are returned grouped by kind, in the order of the attributes of
     * {@link ProtobufMessage.OneOfField}.
     *
     * @param annotation the oneof annotation
     * @return the cases of the oneof
     */
    static List<OneOfCase> cases(ProtobufMessage.OneOfField annotation) {
        var result = new ArrayList<OneOfCase>();
        for (var entry : annotation.floatCases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.FLOAT, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.doubleCases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.DOUBLE, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.boolCases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.BOOL, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.int32Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.INT32, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.sint32Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.SINT32, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.uint32Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.UINT32, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.fixed32Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.FIXED32, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.sfixed32Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.SFIXED32, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.int64Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.INT64, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.sint64Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.SINT64, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.uint64Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.UINT64, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.fixed64Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.FIXED64, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.sfixed64Cases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.SFIXED64, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.enumCases()) {
            result.add(new OneOfCase(new Scalar(ProtobufType.ENUM, entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED), entry::type));
        }
        for (var entry : annotation.stringCases()) {
            result.add(new OneOfCase(new String(entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED, entry.utf8Validation()), entry::type));
        }
        for (var entry : annotation.bytesCases()) {
            result.add(new OneOfCase(new Bytes(entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT), entry::type));
        }
        for (var entry : annotation.messageCases()) {
            result.add(new OneOfCase(new Message(entry.index(), entry.name(), entry::mixins, ProtobufFieldPresence.EXPLICIT, ProtobufRepeatedFieldEncoding.EXPANDED, entry.messageEncoding()), entry::type));
        }
        return result;
    }

    /**
     * A case of a {@link ProtobufMessage.OneOfField}.
     *
     * @param field the case, normalized like the field annotation of the same kind
     * @param type the supplier of the permitted subtype that holds the case, which throws a
     *        {@link javax.lang.model.type.MirroredTypeException} during annotation processing
     */
    record OneOfCase(Value field, Supplier<Class<?>> type) {

    }

    /**
     * Inspects the given element for any known protobuf field annotation and returns
     * a normalized {@link ParsedProtobufField} if one is found.
     *
     * <p>The following annotations are recognized:
     * {@link ProtobufMessage.FloatField}, {@link ProtobufMessage.DoubleField},
     * {@link ProtobufMessage.BoolField}, {@link ProtobufMessage.Int32Field},
     * {@link ProtobufMessage.Sint32Field}, {@link ProtobufMessage.Uint32Field},
     * {@link ProtobufMessage.Fixed32Field}, {@link ProtobufMessage.Sfixed32Field},
     * {@link ProtobufMessage.Int64Field}, {@link ProtobufMessage.Sint64Field},
     * {@link ProtobufMessage.Uint64Field}, {@link ProtobufMessage.Fixed64Field},
     * {@link ProtobufMessage.Sfixed64Field}, {@link ProtobufMessage.EnumField},
     * {@link ProtobufMessage.StringField}, {@link ProtobufMessage.BytesField},
     * {@link ProtobufMessage.MessageField}, and {@link ProtobufMessage.MapField}.
     *
     * @param element the element to inspect
     * @return an {@link Optional} containing the normalized annotation if one is found,
     *         or an empty {@code Optional} otherwise
     */
    static Optional<ParsedProtobufField> of(Element element) {
        var floatField = element.getAnnotation(ProtobufMessage.FloatField.class);
        if (floatField != null) {
            return Optional.of(new Scalar(ProtobufType.FLOAT, floatField.index(), floatField.name(), floatField::mixins, floatField.fieldPresence(), floatField.repeatedFieldEncoding()));
        }

        var doubleField = element.getAnnotation(ProtobufMessage.DoubleField.class);
        if (doubleField != null) {
            return Optional.of(new Scalar(ProtobufType.DOUBLE, doubleField.index(), doubleField.name(), doubleField::mixins, doubleField.fieldPresence(), doubleField.repeatedFieldEncoding()));
        }

        var boolField = element.getAnnotation(ProtobufMessage.BoolField.class);
        if (boolField != null) {
            return Optional.of(new Scalar(ProtobufType.BOOL, boolField.index(), boolField.name(), boolField::mixins, boolField.fieldPresence(), boolField.repeatedFieldEncoding()));
        }

        var int32Field = element.getAnnotation(ProtobufMessage.Int32Field.class);
        if (int32Field != null) {
            return Optional.of(new Scalar(ProtobufType.INT32, int32Field.index(), int32Field.name(), int32Field::mixins, int32Field.fieldPresence(), int32Field.repeatedFieldEncoding()));
        }

        var sint32Field = element.getAnnotation(ProtobufMessage.Sint32Field.class);
        if (sint32Field != null) {
            return Optional.of(new Scalar(ProtobufType.SINT32, sint32Field.index(), sint32Field.name(), sint32Field::mixins, sint32Field.fieldPresence(), sint32Field.repeatedFieldEncoding()));
        }

        var uint32Field = element.getAnnotation(ProtobufMessage.Uint32Field.class);
        if (uint32Field != null) {
            return Optional.of(new Scalar(ProtobufType.UINT32, uint32Field.index(), uint32Field.name(), uint32Field::mixins, uint32Field.fieldPresence(), uint32Field.repeatedFieldEncoding()));
        }

        var fixed32Field = element.getAnnotation(ProtobufMessage.Fixed32Field.class);
        if (fixed32Field != null) {
            return Optional.of(new Scalar(ProtobufType.FIXED32, fixed32Field.index(), fixed32Field.name(), fixed32Field::mixins, fixed32Field.fieldPresence(), fixed32Field.repeatedFieldEncoding()));
        }

        var sfixed32Field = element.getAnnotation(ProtobufMessage.Sfixed32Field.class);
        if (sfixed32Field != null) {
            return Optional.of(new Scalar(ProtobufType.SFIXED32, sfixed32Field.index(), sfixed32Field.name(), sfixed32Field::mixins, sfixed32Field.fieldPresence(), sfixed32Field.repeatedFieldEncoding()));
        }

        var int64Field = element.getAnnotation(ProtobufMessage.Int64Field.class);
        if (int64Field != null) {
            return Optional.of(new Scalar(ProtobufType.INT64, int64Field.index(), int64Field.name(), int64Field::mixins, int64Field.fieldPresence(), int64Field.repeatedFieldEncoding()));
        }

        var sint64Field = element.getAnnotation(ProtobufMessage.Sint64Field.class);
        if (sint64Field != null) {
            return Optional.of(new Scalar(ProtobufType.SINT64, sint64Field.index(), sint64Field.name(), sint64Field::mixins, sint64Field.fieldPresence(), sint64Field.repeatedFieldEncoding()));
        }

        var uint64Field = element.getAnnotation(ProtobufMessage.Uint64Field.class);
        if (uint64Field != null) {
            return Optional.of(new Scalar(ProtobufType.UINT64, uint64Field.index(), uint64Field.name(), uint64Field::mixins, uint64Field.fieldPresence(), uint64Field.repeatedFieldEncoding()));
        }

        var fixed64Field = element.getAnnotation(ProtobufMessage.Fixed64Field.class);
        if (fixed64Field != null) {
            return Optional.of(new Scalar(ProtobufType.FIXED64, fixed64Field.index(), fixed64Field.name(), fixed64Field::mixins, fixed64Field.fieldPresence(), fixed64Field.repeatedFieldEncoding()));
        }

        var sfixed64Field = element.getAnnotation(ProtobufMessage.Sfixed64Field.class);
        if (sfixed64Field != null) {
            return Optional.of(new Scalar(ProtobufType.SFIXED64, sfixed64Field.index(), sfixed64Field.name(), sfixed64Field::mixins, sfixed64Field.fieldPresence(), sfixed64Field.repeatedFieldEncoding()));
        }

        var enumField = element.getAnnotation(ProtobufMessage.EnumField.class);
        if (enumField != null) {
            return Optional.of(new Scalar(ProtobufType.ENUM, enumField.index(), enumField.name(), enumField::mixins, enumField.fieldPresence(), enumField.repeatedFieldEncoding()));
        }

        var stringField = element.getAnnotation(ProtobufMessage.StringField.class);
        if (stringField != null) {
            return Optional.of(new String(stringField.index(), stringField.name(), stringField::mixins, stringField.fieldPresence(), ProtobufRepeatedFieldEncoding.VERSION_DEFAULT, stringField.utf8Validation()));
        }

        var bytesField = element.getAnnotation(ProtobufMessage.BytesField.class);
        if (bytesField != null) {
            return Optional.of(new Bytes(bytesField.index(), bytesField.name(), bytesField::mixins, bytesField.fieldPresence()));
        }

        var messageField = element.getAnnotation(ProtobufMessage.MessageField.class);
        if (messageField != null) {
            return Optional.of(new Message(messageField.index(), messageField.name(), messageField::mixins, messageField.fieldPresence(), ProtobufRepeatedFieldEncoding.VERSION_DEFAULT, messageField.messageEncoding()));
        }

        var mapField = element.getAnnotation(ProtobufMessage.MapField.class);
        if (mapField != null) {
            return Optional.of(new Map(mapField.index(), mapField.name(), mapField::mixins, mapField.mapKeyType(), mapField.mapValueType(), mapField.utf8Validation()));
        }

        return Optional.empty();
    }

    /**
     * A normal scalar or enum field with standard field presence and repeated encoding attributes.
     *
     * <p>Covers: float, double, bool, int32, sint32, uint32, fixed32, sfixed32,
     * int64, sint64, uint64, fixed64, sfixed64, and enum fields.
     *
     * @param type the protobuf type of the field
     * @param index the field index
     * @param name the overridden field name, or empty for the default
     * @param mixins a supplier that returns the mixin classes associated with the field
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     */
    record Scalar(
            ProtobufType type,
            int index,
            java.lang.String name,
            Supplier<Class<?>[]> mixins,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding
    ) implements Value {
        /**
         * Returns whether this field has legacy required presence semantics.
         *
         * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
         */
        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        /**
         * Returns whether this field uses packed encoding for repeated values.
         *
         * @return {@code true} if the repeated field encoding is {@link ProtobufRepeatedFieldEncoding#PACKED}
         */
        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }
    }

    /**
     * A string field with UTF-8 validation control.
     *
     * @param index the field index
     * @param name the overridden field name, or empty for the default
     * @param mixins a supplier that returns the mixin classes associated with the field
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     * @param utf8Validation the UTF-8 validation strategy
     */
    record String(
            int index,
            java.lang.String name,
            Supplier<Class<?>[]> mixins,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufUtf8Validation utf8Validation
    ) implements Value {
        /**
         * Returns the protobuf type of this field, which is always {@link ProtobufType#STRING}.
         *
         * @return {@link ProtobufType#STRING}
         */
        @Override
        public ProtobufType type() {
            return ProtobufType.STRING;
        }

        /**
         * Returns whether this field has legacy required presence semantics.
         *
         * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
         */
        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        /**
         * Returns whether this field uses packed encoding for repeated values.
         *
         * @return {@code true} if the repeated field encoding is {@link ProtobufRepeatedFieldEncoding#PACKED}
         */
        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }
    }

    /**
     * A bytes field without repeated field encoding control.
     *
     * @param index the field index
     * @param name the overridden field name, or empty for the default
     * @param mixins a supplier that returns the mixin classes associated with the field
     * @param fieldPresence the field presence strategy
     */
    record Bytes(
            int index,
            java.lang.String name,
            Supplier<Class<?>[]> mixins,
            ProtobufFieldPresence fieldPresence
    ) implements Value {
        /**
         * Returns the protobuf type of this field, which is always {@link ProtobufType#BYTES}.
         *
         * @return {@link ProtobufType#BYTES}
         */
        @Override
        public ProtobufType type() {
            return ProtobufType.BYTES;
        }

        /**
         * Returns whether this field has legacy required presence semantics.
         *
         * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
         */
        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean packed() {
            return false;
        }
    }

    /**
     * A nested message field with message encoding control.
     *
     * @param index the field index
     * @param name the overridden field name, or empty for the default
     * @param mixins a supplier that returns the mixin classes associated with the field
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     * @param messageEncoding the message encoding strategy
     */
    record Message(
            int index,
            java.lang.String name,
            Supplier<Class<?>[]> mixins,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufMessageEncoding messageEncoding
    ) implements Value {
        /**
         * Returns the protobuf type of this field, which is always {@link ProtobufType#MESSAGE}.
         *
         * @return {@link ProtobufType#MESSAGE}
         */
        @Override
        public ProtobufType type() {
            return ProtobufType.MESSAGE;
        }

        /**
         * Returns whether this field has legacy required presence semantics.
         *
         * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
         */
        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        /**
         * Returns whether this field uses packed encoding for repeated values.
         *
         * @return {@code true} if the repeated field encoding is {@link ProtobufRepeatedFieldEncoding#PACKED}
         */
        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }
    }

    /**
     * A map field with key and value type specifications.
     *
     * @param index the field index
     * @param name the overridden field name, or empty for the default
     * @param mixins a supplier that returns the mixin classes associated with the field
     * @param mapKeyType the protobuf type of the map key
     * @param mapValueType the protobuf type of the map value
     * @param utf8Validation the UTF-8 validation strategy for string keys and values
     */
    record Map(
            int index,
            java.lang.String name,
            Supplier<Class<?>[]> mixins,
            ProtobufType mapKeyType,
            ProtobufType mapValueType,
            ProtobufUtf8Validation utf8Validation
    ) implements ParsedProtobufField {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean required() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean packed() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ProtobufFieldPresence fieldPresence() {
            return ProtobufFieldPresence.VERSION_DEFAULT;
        }
    }
}
