package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import javax.lang.model.element.TypeElement;
import java.util.SequencedSet;

/**
 * A processed protobuf type element, either a message/group or an enum.
 *
 * <p>This sealed interface extends {@link DaedalusTypeElement} with the
 * reserved-elements API common to both {@code @ProtobufMessage} and
 * {@code @ProtobufEnum} annotated types.
 */
public sealed interface ProtobufTypeElement
        extends DaedalusTypeElement
        permits ProtobufMessageElement, ProtobufEnumElement {

    /**
     * Returns the reserved field elements declared on this type.
     *
     * @return an unmodifiable sequenced set of reserved field elements
     */
    SequencedSet<? extends ProtobufReservedFieldElement> reservedElements();

    /**
     * Returns whether the given field index is allowed (not reserved).
     *
     * @param value the field index to check
     * @return {@code true} if the index is allowed
     */
    boolean isIndexAllowed(int value);

    /**
     * Returns whether the given field name is allowed (not reserved).
     *
     * @param name the field name to check
     * @return {@code true} if the name is allowed
     */
    boolean isNameAllowed(String name);

    /**
     * Returns the Protocol Buffers name of the given message or enum.
     *
     * <p>The name is the one declared by the {@code protoName} attribute of the
     * {@link ProtobufMessage} or {@link ProtobufEnum} annotation of the type, which is fully
     * qualified, or the simple name of the type if the annotation declares none. The type can
     * be declared in a dependency, as only its annotation is read.
     *
     * @param type the message or the enum
     * @return the Protocol Buffers name of the type
     */
    static String getProtobufName(TypeElement type) {
        var message = type.getAnnotation(ProtobufMessage.class);
        if (message != null && !message.protoName().isEmpty()) {
            return message.protoName();
        }

        var enumeration = type.getAnnotation(ProtobufEnum.class);
        if (enumeration != null && !enumeration.protoName().isEmpty()) {
            return enumeration.protoName();
        }

        return type.getSimpleName().toString();
    }

    /**
     * Returns the simple Protocol Buffers name of the given message or enum, which is its
     * {@linkplain #getProtobufName(TypeElement) Protocol Buffers name} without the package and
     * the enclosing messages.
     *
     * @param type the message or the enum
     * @return the simple Protocol Buffers name of the type
     */
    static String getSimpleProtobufName(TypeElement type) {
        var name = getProtobufName(type);
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
