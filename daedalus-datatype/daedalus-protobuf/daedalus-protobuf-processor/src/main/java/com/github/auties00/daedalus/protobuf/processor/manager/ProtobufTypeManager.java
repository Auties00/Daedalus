package com.github.auties00.daedalus.protobuf.processor.manager;

import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
        return hasAnnotation(mirror, ProtobufMessage.class);
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
        return hasAnnotation(mirror, ProtobufEnum.class);
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
        return hasAnnotation(mirror, ProtobufMessage.class)
                || hasAnnotation(mirror, ProtobufEnum.class);
    }

    /**
     * Returns the mixin type elements referenced by the given unknown fields annotation.
     *
     * @param property the unknown fields annotation
     * @return a set of resolved mixin type elements
     */
    public Set<TypeElement> getMixins(ProtobufMessage.UnknownFields property) {
        return getMirroredTypes(property::mixins);
    }

    /**
     * Returns the type of the class returned by the given annotation attribute.
     *
     * <p>During annotation processing, the class of an attribute is usually not loaded, so
     * reading it throws a {@link MirroredTypeException} that carries its type.
     *
     * @param supplier the annotation attribute that returns a class
     * @return the type of the class
     */
    public TypeMirror getMirroredType(Supplier<Class<?>> supplier) {
        try {
            return getType(supplier.get());
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }
}
