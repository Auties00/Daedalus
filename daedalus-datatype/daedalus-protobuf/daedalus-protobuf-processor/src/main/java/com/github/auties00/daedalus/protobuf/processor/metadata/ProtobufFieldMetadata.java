package com.github.auties00.daedalus.protobuf.processor.metadata;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.*;

import javax.lang.model.element.Element;
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
public sealed interface ProtobufFieldMetadata {
    /**
     * Returns the protobuf type of this field.
     *
     * @return the protobuf type
     */
    ProtobufType type();

    /**
     * Returns the field index.
     *
     * @return the field index
     */
    long index();

    /**
     * Returns the custom name for text serialization, or an empty string for the default.
     *
     * @return the field name
     */
    String name();

    /**
     * Returns a supplier for the mixin classes associated with this field.
     *
     * @return the mixin classes supplier
     */
    Supplier<Class<?>[]> mixins();

    /**
     * Returns whether this field should be skipped during serialization and deserialization.
     *
     * @return {@code true} if the field is ignored
     */
    boolean ignored();

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
     * Inspects the given element for any known protobuf field annotation and returns
     * a normalized {@link ProtobufFieldMetadata} if one is found.
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
    static Optional<ProtobufFieldMetadata> of(Element element) {
        var floatField = element.getAnnotation(ProtobufMessage.FloatField.class);
        if (floatField != null) {
            return Optional.of(new NormalField(ProtobufType.FLOAT, floatField.index(), floatField.name(), floatField::mixins, floatField.ignored(), floatField.fieldPresence(), floatField.repeatedFieldEncoding()));
        }

        var doubleField = element.getAnnotation(ProtobufMessage.DoubleField.class);
        if (doubleField != null) {
            return Optional.of(new NormalField(ProtobufType.DOUBLE, doubleField.index(), doubleField.name(), doubleField::mixins, doubleField.ignored(), doubleField.fieldPresence(), doubleField.repeatedFieldEncoding()));
        }

        var boolField = element.getAnnotation(ProtobufMessage.BoolField.class);
        if (boolField != null) {
            return Optional.of(new NormalField(ProtobufType.BOOL, boolField.index(), boolField.name(), boolField::mixins, boolField.ignored(), boolField.fieldPresence(), boolField.repeatedFieldEncoding()));
        }

        var int32Field = element.getAnnotation(ProtobufMessage.Int32Field.class);
        if (int32Field != null) {
            return Optional.of(new NormalField(ProtobufType.INT32, int32Field.index(), int32Field.name(), int32Field::mixins, int32Field.ignored(), int32Field.fieldPresence(), int32Field.repeatedFieldEncoding()));
        }

        var sint32Field = element.getAnnotation(ProtobufMessage.Sint32Field.class);
        if (sint32Field != null) {
            return Optional.of(new NormalField(ProtobufType.SINT32, sint32Field.index(), sint32Field.name(), sint32Field::mixins, sint32Field.ignored(), sint32Field.fieldPresence(), sint32Field.repeatedFieldEncoding()));
        }

        var uint32Field = element.getAnnotation(ProtobufMessage.Uint32Field.class);
        if (uint32Field != null) {
            return Optional.of(new NormalField(ProtobufType.UINT32, uint32Field.index(), uint32Field.name(), uint32Field::mixins, uint32Field.ignored(), uint32Field.fieldPresence(), uint32Field.repeatedFieldEncoding()));
        }

        var fixed32Field = element.getAnnotation(ProtobufMessage.Fixed32Field.class);
        if (fixed32Field != null) {
            return Optional.of(new NormalField(ProtobufType.FIXED32, fixed32Field.index(), fixed32Field.name(), fixed32Field::mixins, fixed32Field.ignored(), fixed32Field.fieldPresence(), fixed32Field.repeatedFieldEncoding()));
        }

        var sfixed32Field = element.getAnnotation(ProtobufMessage.Sfixed32Field.class);
        if (sfixed32Field != null) {
            return Optional.of(new NormalField(ProtobufType.SFIXED32, sfixed32Field.index(), sfixed32Field.name(), sfixed32Field::mixins, sfixed32Field.ignored(), sfixed32Field.fieldPresence(), sfixed32Field.repeatedFieldEncoding()));
        }

        var int64Field = element.getAnnotation(ProtobufMessage.Int64Field.class);
        if (int64Field != null) {
            return Optional.of(new NormalField(ProtobufType.INT64, int64Field.index(), int64Field.name(), int64Field::mixins, int64Field.ignored(), int64Field.fieldPresence(), int64Field.repeatedFieldEncoding()));
        }

        var sint64Field = element.getAnnotation(ProtobufMessage.Sint64Field.class);
        if (sint64Field != null) {
            return Optional.of(new NormalField(ProtobufType.SINT64, sint64Field.index(), sint64Field.name(), sint64Field::mixins, sint64Field.ignored(), sint64Field.fieldPresence(), sint64Field.repeatedFieldEncoding()));
        }

        var uint64Field = element.getAnnotation(ProtobufMessage.Uint64Field.class);
        if (uint64Field != null) {
            return Optional.of(new NormalField(ProtobufType.UINT64, uint64Field.index(), uint64Field.name(), uint64Field::mixins, uint64Field.ignored(), uint64Field.fieldPresence(), uint64Field.repeatedFieldEncoding()));
        }

        var fixed64Field = element.getAnnotation(ProtobufMessage.Fixed64Field.class);
        if (fixed64Field != null) {
            return Optional.of(new NormalField(ProtobufType.FIXED64, fixed64Field.index(), fixed64Field.name(), fixed64Field::mixins, fixed64Field.ignored(), fixed64Field.fieldPresence(), fixed64Field.repeatedFieldEncoding()));
        }

        var sfixed64Field = element.getAnnotation(ProtobufMessage.Sfixed64Field.class);
        if (sfixed64Field != null) {
            return Optional.of(new NormalField(ProtobufType.SFIXED64, sfixed64Field.index(), sfixed64Field.name(), sfixed64Field::mixins, sfixed64Field.ignored(), sfixed64Field.fieldPresence(), sfixed64Field.repeatedFieldEncoding()));
        }

        var enumField = element.getAnnotation(ProtobufMessage.EnumField.class);
        if (enumField != null) {
            return Optional.of(new NormalField(ProtobufType.ENUM, enumField.index(), enumField.name(), enumField::mixins, enumField.ignored(), enumField.fieldPresence(), enumField.repeatedFieldEncoding()));
        }

        var stringField = element.getAnnotation(ProtobufMessage.StringField.class);
        if (stringField != null) {
            return Optional.of(new StringField(stringField.index(), stringField.name(), stringField::mixins, stringField.ignored(), stringField.fieldPresence(), ProtobufRepeatedFieldEncoding.VERSION_DEFAULT, stringField.utf8Validation()));
        }

        var bytesField = element.getAnnotation(ProtobufMessage.BytesField.class);
        if (bytesField != null) {
            return Optional.of(new BytesField(bytesField.index(), bytesField.name(), bytesField::mixins, bytesField.ignored(), bytesField.fieldPresence()));
        }

        var messageField = element.getAnnotation(ProtobufMessage.MessageField.class);
        if (messageField != null) {
            return Optional.of(new MessageField(messageField.index(), messageField.name(), messageField::mixins, messageField.ignored(), messageField.fieldPresence(), ProtobufRepeatedFieldEncoding.VERSION_DEFAULT, messageField.messageEncoding()));
        }

        var mapField = element.getAnnotation(ProtobufMessage.MapField.class);
        if (mapField != null) {
            return Optional.of(new MapField(mapField.index(), mapField.name(), mapField::mixins, mapField.ignored(), mapField.mapKeyType(), mapField.mapValueType()));
        }

        return Optional.empty();
    }

    /**
     * Returns the Java class that corresponds to the deserialized form of the given
     * protobuf type.
     *
     * <p>This mapping is used when a cast to the wire type is needed during
     * serialization code generation. For object types such as {@link ProtobufType#MESSAGE},
     * {@link ProtobufType#ENUM}, {@link ProtobufType#GROUP}, and {@link ProtobufType#MAP},
     * this method returns {@code null} because those types do not have a single
     * primitive wire representation.
     *
     * @param protobufType the protobuf type
     * @return the corresponding Java class, or {@code null} for object types
     */
    static Class<?> deserializableType(ProtobufType protobufType) {
        return switch (protobufType) {
            case FLOAT -> float.class;
            case DOUBLE -> double.class;
            case BOOL -> boolean.class;
            case INT32, SINT32, UINT32, FIXED32, SFIXED32, ENUM -> int.class;
            case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> long.class;
            case STRING -> String.class;
            case BYTES -> byte[].class;
            case MESSAGE, GROUP, MAP, UNKNOWN -> null;
        };
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
     * @param ignored whether the field is ignored during serialization and deserialization
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     */
    record NormalField(
            ProtobufType type,
            long index,
            String name,
            Supplier<Class<?>[]> mixins,
            boolean ignored,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding
    ) implements ProtobufFieldMetadata {
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
     * @param ignored whether the field is ignored during serialization and deserialization
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     * @param utf8Validation the UTF-8 validation strategy
     */
    record StringField(
            long index,
            String name,
            Supplier<Class<?>[]> mixins,
            boolean ignored,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufUtf8Validation utf8Validation
    ) implements ProtobufFieldMetadata {
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
     * @param ignored whether the field is ignored during serialization and deserialization
     * @param fieldPresence the field presence strategy
     */
    record BytesField(
            long index,
            String name,
            Supplier<Class<?>[]> mixins,
            boolean ignored,
            ProtobufFieldPresence fieldPresence
    ) implements ProtobufFieldMetadata {
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
         * @inheritDoc
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
     * @param ignored whether the field is ignored during serialization and deserialization
     * @param fieldPresence the field presence strategy
     * @param repeatedFieldEncoding the repeated field encoding strategy
     * @param messageEncoding the message encoding strategy
     */
    record MessageField(
            long index,
            String name,
            Supplier<Class<?>[]> mixins,
            boolean ignored,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufMessageEncoding messageEncoding
    ) implements ProtobufFieldMetadata {
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
     * @param ignored whether the field is ignored during serialization and deserialization
     * @param mapKeyType the protobuf type of the map key
     * @param mapValueType the protobuf type of the map value
     */
    record MapField(
            long index,
            String name,
            Supplier<Class<?>[]> mixins,
            boolean ignored,
            ProtobufType mapKeyType,
            ProtobufType mapValueType
    ) implements ProtobufFieldMetadata {
        /**
         * Returns the protobuf type of this field, which is always {@link ProtobufType#MAP}.
         *
         * @return {@link ProtobufType#MAP}
         */
        @Override
        public ProtobufType type() {
            return ProtobufType.MAP;
        }

        /**
         * @inheritDoc
         */
        @Override
        public boolean required() {
            return false;
        }

        /**
         * @inheritDoc
         */
        @Override
        public boolean packed() {
            return false;
        }
    }
}
