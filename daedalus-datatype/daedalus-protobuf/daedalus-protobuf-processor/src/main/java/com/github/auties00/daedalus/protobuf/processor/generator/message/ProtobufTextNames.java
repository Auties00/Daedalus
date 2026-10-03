package com.github.auties00.daedalus.protobuf.processor.generator.message;

import com.github.auties00.daedalus.protobuf.processor.element.ProtobufFieldElement;
import com.github.auties00.daedalus.protobuf.processor.element.ProtobufTypeElement;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufValueFieldType;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Static utility methods for resolving the names that identify the fields of a message in the
 * text format.
 *
 * <p>A field is identified by its Protocol Buffers name, except for a group, which the official
 * protobuf library identifies by the name of its message type: a message field encoded as a
 * group whose name is the name of its message type in lower case is written by the name of the
 * type, and read by either name.
 */
final class ProtobufTextNames {
    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufTextNames() {
        throw new UnsupportedOperationException("ProtobufTextNames is a utility class and cannot be instantiated");
    }

    /**
     * Returns the name that the text format writes for the given field.
     *
     * @param field the field, or the case of a oneof
     * @return the name of the message type of a group whose name is the name of the type in
     *         lower case, and the Protocol Buffers name of the field otherwise
     */
    static String getWrittenName(ProtobufFieldElement field) {
        return getGroupName(field).orElse(field.textName());
    }

    /**
     * Returns the names that the text format accepts for the given field.
     *
     * @param field the field, or the case of a oneof
     * @return the Protocol Buffers name of the field, followed by the name of the message type of
     *         a group whose name is the name of the type in lower case
     */
    static List<String> getReadNames(ProtobufFieldElement field) {
        return getGroupName(field)
                .map(groupName -> List.of(field.textName(), groupName))
                .orElseGet(() -> List.of(field.textName()));
    }

    /**
     * Returns the name of the message type of the given field, if the field is a group whose
     * Protocol Buffers name is the name of the type in lower case.
     *
     * @param field the field, or the case of a oneof
     * @return the name of the message type, if the field is such a group
     */
    private static Optional<String> getGroupName(ProtobufFieldElement field) {
        if (!field.delimited()) {
            return Optional.empty();
        }

        var valueType = switch (field.type()) {
            case ProtobufCollectionFieldType collectionType -> collectionType.valueType();
            case ProtobufArrayFieldType arrayType -> arrayType.valueType();
            case ProtobufValueFieldType simpleType -> simpleType;
            default -> null;
        };
        if (valueType == null
                || !(valueType.descriptorElementType() instanceof DeclaredType declaredType)
                || !(declaredType.asElement() instanceof TypeElement typeElement)) {
            return Optional.empty();
        }

        var typeName = ProtobufTypeElement.getSimpleProtobufName(typeElement);
        if (typeName.equals(field.textName()) || !typeName.toLowerCase(Locale.ROOT).equals(field.textName())) {
            return Optional.empty();
        }

        return Optional.of(typeName);
    }
}
