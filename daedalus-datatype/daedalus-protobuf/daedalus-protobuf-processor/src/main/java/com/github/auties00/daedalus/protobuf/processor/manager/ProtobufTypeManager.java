package com.github.auties00.daedalus.protobuf.processor.manager;

import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * A protobuf specific extension of the common type utility that adds predicates
 * for protobuf messages, enums, and mixins.
 *
 * <p>This class inherits all general type operations from the common
 * {@link DaedalusTypeManager} class and adds
 * methods for identifying types annotated with protobuf annotations such as
 * {@link ProtobufMessage} and {@link ProtobufEnum}.
 */
public final class ProtobufTypeManager extends DaedalusTypeManager {

    /**
     * Constructs a new protobuf type utility with the given processing environment.
     *
     * @param processingEnv the annotation processing environment
     */
    public ProtobufTypeManager(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    /**
     * Returns whether the given type mirror represents a protobuf message.
     *
     * <p>A type is considered a protobuf message if its erased form is a declared
     * type whose element is annotated with {@link ProtobufMessage}.
     *
     * @param mirror the type mirror to check
     * @return {@code true} if the type is a protobuf message
     */
    public boolean isMessage(TypeMirror mirror) {
        return erase(mirror) instanceof DeclaredType declaredType
                && declaredType.asElement().getAnnotation(ProtobufMessage.class) != null;
    }

    /**
     * Returns whether the given type mirror represents a protobuf enum.
     *
     * <p>A type is considered a protobuf enum if its erased form is a declared
     * type whose element is annotated with {@link ProtobufEnum}.
     *
     * @param mirror the type mirror to check
     * @return {@code true} if the type is a protobuf enum
     */
    public boolean isEnum(TypeMirror mirror) {
        return erase(mirror) instanceof DeclaredType declaredType
                && declaredType.asElement().getAnnotation(ProtobufEnum.class) != null;
    }

    /**
     * Returns whether the given type mirror represents a protobuf object.
     *
     * <p>A type is considered a protobuf object if it is either a protobuf
     * message or a protobuf enum.
     *
     * @param mirror the type mirror to check
     * @return {@code true} if the type is a protobuf message or protobuf enum
     */
    public boolean isObject(TypeMirror mirror) {
        return erase(mirror) instanceof DeclaredType declaredType
                && (declaredType.asElement().getAnnotation(ProtobufMessage.class) != null
                        || declaredType.asElement().getAnnotation(ProtobufEnum.class) != null);
    }

    /**
     * Returns the mixin type elements referenced by the given unknown fields annotation.
     *
     * @param property the unknown fields annotation
     * @return a list of resolved mixin type elements
     */
    public List<TypeElement> getMixins(ProtobufMessage.UnknownFields property) {
        return getMirroredTypes(property::mixins);
    }
}
