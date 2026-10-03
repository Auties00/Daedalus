package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusFieldElement;
import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

/**
 * A processed protobuf enum constant element.
 *
 * <p>Each constant maps a numeric index to a constant name within a
 * {@code @ProtobufEnum} annotated type. Enum constants do not participate
 * in converter attribution: converters are linked at the enum type level
 * (enum type to {@code int} and vice versa), not at the constant level.
 *
 * @param index the protobuf index of this constant
 * @param name the Java name of this constant
 * @param textName the Protocol Buffers name of this constant, which identifies it in the text
 *                 format
 * @param element the field that declares this constant
 * @param type the type of this constant
 */
public record ProtobufEnumConstantElement(
        int index,
        String name,
        String textName,
        VariableElement element,
        ProtobufFieldType type
) implements DaedalusFieldElement {
    /**
     * {@inheritDoc}
     *
     * <p>Enum constants are not included in any builder.
     */
    @Override
    public boolean includedInDefaultBuilder() {
        return false;
    }
}
