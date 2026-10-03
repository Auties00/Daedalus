package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.protobuf.model.*;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;

import javax.lang.model.element.Element;
import java.util.List;

/**
 * A processed protobuf field element representing a property of a protobuf message.
 *
 * <p>Each subtype mirrors the corresponding {@link ParsedProtobufField}
 * subtype but carries resolved (non-VERSION_DEFAULT) feature values and the
 * fully attributed {@link ProtobufFieldType}.
 */
public sealed interface ProtobufFieldElement extends DaedalusFieldElement {

    /**
     * Returns the protobuf field index.
     *
     * @return the field index as declared in the {@code @ProtobufField} annotation
     */
    int index();

    /**
     * Returns the name of the Java element that declares this field, which is a valid Java
     * identifier and is used to name the variables and builder methods of the generated
     * code.
     *
     * @return the non-null Java name
     */
    java.lang.String name();

    /**
     * Returns the Protocol Buffers name of this field, which is used by the text formats.
     *
     * <p>This is the {@code name} of the field annotation, or the {@code snake_case} form
     * of the Java name if the annotation does not specify one, so that the fields generated
     * from a {@code .proto} file keep their original name.
     *
     * @return the non-null Protocol Buffers name
     */
    java.lang.String textName();

    /**
     * Returns the element used to read the field value during serialization.
     *
     * <p>This is typically a field element, a record component, or a getter method
     * declared via {@code @ProtobufAccessor}.
     *
     * @return the non-null accessor element
     */
    Element accessor();

    /**
     * Returns whether this field was declared via {@code @ProtobufAccessor} rather
     * than as a direct field or record component.
     *
     * @return {@code true} if the field is synthetic, {@code false} otherwise
     */
    boolean isSynthetic();

    /**
     * Returns the resolved protobuf field type.
     *
     * @return the non-null field type
     */
    ProtobufFieldType type();

    /**
     * {@inheritDoc}
     *
     * <p>Synthetic fields are excluded from the default builder because they are
     * not present in the constructor.
     */
    @Override
    default boolean includedInDefaultBuilder() {
        return !isSynthetic();
    }

    /**
     * Returns whether this field has legacy required presence semantics.
     *
     * @return {@code true} if the field presence is {@link ProtobufFieldPresence#LEGACY_REQUIRED}
     */
    boolean required();

    /**
     * Returns whether this field uses packed encoding for repeated values.
     *
     * <p>Only a repeated field whose type is packable can be packed, so this method
     * returns {@code false} for every other field, whatever its declared encoding.
     *
     * @return {@code true} if the repeated field encoding is {@link ProtobufRepeatedFieldEncoding#PACKED}
     */
    boolean packed();

    /**
     * Returns whether this field has implicit presence, so that its default value is not
     * written to the wire.
     *
     * @return {@code true} if the field presence is {@link ProtobufFieldPresence#IMPLICIT}
     */
    boolean implicit();

    /**
     * Returns whether this field is a message encoded as a group, which is delimited by a
     * start tag and an end tag instead of being prefixed by its length.
     *
     * @return {@code true} if the message encoding is {@link ProtobufMessageEncoding#DELIMITED}
     */
    default boolean delimited() {
        return false;
    }

    /**
     * A protobuf field element representing a scalar field such as an {@code int32},
     * {@code bool}, {@code double}, or an enum value.
     *
     * @param index the protobuf field index
     * @param name the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the element used to read the field value
     * @param isSynthetic whether this field was declared via {@code @ProtobufAccessor}
     * @param fieldPresence the resolved field presence strategy
     * @param repeatedFieldEncoding the resolved encoding for repeated fields
     * @param type the resolved protobuf field type
     */
    record Scalar(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufFieldType type
    ) implements ProtobufFieldElement {
        public Scalar {
            if (fieldPresence == ProtobufFieldPresence.VERSION_DEFAULT) {
                throw new IllegalArgumentException("fieldPresence must be resolved");
            }
            if (repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.VERSION_DEFAULT) {
                throw new IllegalArgumentException("repeatedFieldEncoding must be resolved");
            }
        }

        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }

        @Override
        public boolean implicit() {
            return fieldPresence == ProtobufFieldPresence.IMPLICIT;
        }
    }

    /**
     * A protobuf field element representing a {@code string} field.
     *
     * <p>In addition to the common field attributes, this record carries the
     * resolved UTF-8 validation strategy.
     *
     * @param index the protobuf field index
     * @param name the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the element used to read the field value
     * @param isSynthetic whether this field was declared via {@code @ProtobufAccessor}
     * @param fieldPresence the resolved field presence strategy
     * @param repeatedFieldEncoding the resolved encoding for repeated fields
     * @param utf8Validation the resolved UTF-8 validation strategy
     * @param type the resolved protobuf field type
     */
    record String(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufUtf8Validation utf8Validation,
            ProtobufFieldType type
    ) implements ProtobufFieldElement {
        public String {
            if (fieldPresence == ProtobufFieldPresence.VERSION_DEFAULT) {
                throw new IllegalArgumentException("fieldPresence must be resolved");
            }
            if (repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.VERSION_DEFAULT) {
                throw new IllegalArgumentException("repeatedFieldEncoding must be resolved");
            }
            if (utf8Validation == ProtobufUtf8Validation.VERSION_DEFAULT) {
                throw new IllegalArgumentException("utf8Validation must be resolved");
            }
        }

        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }

        @Override
        public boolean implicit() {
            return fieldPresence == ProtobufFieldPresence.IMPLICIT;
        }
    }

    /**
     * A protobuf field element representing a {@code bytes} field.
     *
     * @param index the protobuf field index
     * @param name the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the element used to read the field value
     * @param isSynthetic whether this field was declared via {@code @ProtobufAccessor}
     * @param fieldPresence the resolved field presence strategy
     * @param type the resolved protobuf field type
     */
    record Bytes(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            ProtobufFieldPresence fieldPresence,
            ProtobufFieldType type
    ) implements ProtobufFieldElement {
        public Bytes {
            if (fieldPresence == ProtobufFieldPresence.VERSION_DEFAULT) {
                throw new IllegalArgumentException("fieldPresence must be resolved");
            }
        }

        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        @Override
        public boolean packed() {
            return false;
        }

        @Override
        public boolean implicit() {
            return fieldPresence == ProtobufFieldPresence.IMPLICIT;
        }
    }

    /**
     * A protobuf field element representing a nested {@code message} field.
     *
     * <p>In addition to the common field attributes, this record carries the
     * resolved message encoding strategy (length-delimited or group).
     *
     * @param index the protobuf field index
     * @param name the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the element used to read the field value
     * @param isSynthetic whether this field was declared via {@code @ProtobufAccessor}
     * @param fieldPresence the resolved field presence strategy
     * @param repeatedFieldEncoding the resolved encoding for repeated fields
     * @param messageEncoding the resolved encoding for the nested message
     * @param type the resolved protobuf field type
     */
    record Message(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            ProtobufFieldPresence fieldPresence,
            ProtobufRepeatedFieldEncoding repeatedFieldEncoding,
            ProtobufMessageEncoding messageEncoding,
            ProtobufFieldType type
    ) implements ProtobufFieldElement {
        public Message {
            if (fieldPresence == ProtobufFieldPresence.VERSION_DEFAULT) {
                throw new IllegalArgumentException("fieldPresence must be resolved");
            }
            if (repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.VERSION_DEFAULT) {
                throw new IllegalArgumentException("repeatedFieldEncoding must be resolved");
            }
            if (messageEncoding == ProtobufMessageEncoding.VERSION_DEFAULT) {
                throw new IllegalArgumentException("messageEncoding must be resolved");
            }
        }

        @Override
        public boolean required() {
            return fieldPresence == ProtobufFieldPresence.LEGACY_REQUIRED;
        }

        @Override
        public boolean packed() {
            return repeatedFieldEncoding == ProtobufRepeatedFieldEncoding.PACKED;
        }

        @Override
        public boolean implicit() {
            return fieldPresence == ProtobufFieldPresence.IMPLICIT;
        }

        @Override
        public boolean delimited() {
            return messageEncoding == ProtobufMessageEncoding.DELIMITED;
        }
    }

    /**
     * A protobuf field element representing a {@code map} field.
     *
     * <p>Map fields are encoded as repeated key-value pair messages on the wire.
     * In addition to the common field attributes, this record carries the
     * resolved UTF-8 validation strategy for string-typed map keys or values.
     *
     * @param index the protobuf field index
     * @param name the name of the Java element that declares the field
     * @param textName the Protocol Buffers name of the field
     * @param accessor the element used to read the field value
     * @param isSynthetic whether this field was declared via {@code @ProtobufAccessor}
     * @param utf8Validation the resolved UTF-8 validation strategy
     * @param type the resolved protobuf field type
     */
    record Map(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            ProtobufUtf8Validation utf8Validation,
            ProtobufFieldType type
    ) implements ProtobufFieldElement {
        public Map {
            if (utf8Validation == ProtobufUtf8Validation.VERSION_DEFAULT) {
                throw new IllegalArgumentException("utf8Validation must be resolved");
            }
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public boolean packed() {
            return false;
        }

        @Override
        public boolean implicit() {
            return false;
        }
    }

    /**
     * A oneof of a protobuf message, held by a field whose type is sealed.
     *
     * <p>A oneof has no index of its own: each of its cases is a field of the message,
     * described by an element whose type is the permitted subtype that holds the case, and
     * whose name and accessor are the ones of the oneof. The cases always have explicit
     * presence, as at most one of them is set. The {@linkplain #index() index} of the oneof
     * is the lowest index of its cases, which orders it among the fields of the message.
     *
     * @param index the lowest index of the cases of the oneof
     * @param name the name of the Java element that declares the oneof
     * @param textName the Protocol Buffers name of the oneof
     * @param accessor the element used to read the value of the oneof
     * @param isSynthetic whether the oneof is declared by a method, so that it is only written
     * @param cases the cases of the oneof, ordered like the
     *        {@linkplain ProtobufOneOfFieldType#caseTypes() types of the cases}
     * @param type the resolved type of the oneof
     */
    record OneOf(
            int index,
            java.lang.String name,
            java.lang.String textName,
            Element accessor,
            boolean isSynthetic,
            List<ProtobufFieldElement> cases,
            ProtobufOneOfFieldType type
    ) implements ProtobufFieldElement {
        /**
         * {@inheritDoc}
         *
         * @return {@code false}, as a oneof may have no case set
         */
        @Override
        public boolean required() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code false}, as a oneof is never repeated
         */
        @Override
        public boolean packed() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code false}, as the cases of a oneof have explicit presence
         */
        @Override
        public boolean implicit() {
            return false;
        }
    }
}
