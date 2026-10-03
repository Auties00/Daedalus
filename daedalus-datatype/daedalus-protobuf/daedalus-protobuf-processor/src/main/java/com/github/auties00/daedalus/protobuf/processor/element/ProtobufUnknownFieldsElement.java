package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * A processed element representing a field annotated with
 * {@link com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage.UnknownFields @ProtobufMessage.UnknownFields}.
 *
 * <p>The codegen always operates on a
 * {@link com.github.auties00.daedalus.protobuf.model.ProtobufUnknownFields}, which keeps the
 * unknown fields read from the binary format, identified by their indexes, and the ones read
 * from the text format, identified by their names, side by side.
 *
 * <p>A field whose type is not a store is converted by the static methods of one of its
 * mixins: {@code serializer} converts the value of the field to a store, and
 * {@code deserializer} converts a store to a value of the field. Both are {@code null}
 * if the field is a store.
 *
 * @param field        the annotated field on the enclosing message class
 * @param accessor     the element that reads the field, which is the field itself unless
 *                     it is private, or its getter otherwise
 * @param type         the declared type of the field
 * @param serializer   the mixin method that converts the field to a store, or {@code null}
 *                     if the field is a store
 * @param deserializer the mixin method that converts a store to the field, or {@code null}
 *                     if the field is a store
 */
public record ProtobufUnknownFieldsElement(
        VariableElement field,
        Element accessor,
        TypeMirror type,
        ExecutableElement serializer,
        ExecutableElement deserializer
) {
    /**
     * Returns the expression that converts the given value of the field to a store.
     *
     * @param value the expression that provides the value of the field
     * @return the expression, which is {@code value} itself if the field is a store
     */
    public String toStore(String value) {
        return invoke(serializer, value);
    }

    /**
     * Returns the expression that converts the given store to a value of the field.
     *
     * @param store the expression that provides the store
     * @return the expression, which is {@code store} itself if the field is a store
     */
    public String fromStore(String store) {
        return invoke(deserializer, store);
    }

    /**
     * Returns the mixin method that converts the field to a store, if the field is not
     * a store.
     *
     * @return the method, or an empty {@link Optional} if the field is a store
     */
    public Optional<ExecutableElement> serializerMethod() {
        return Optional.ofNullable(serializer);
    }

    /**
     * Returns the mixin method that converts a store to the field, if the field is not
     * a store.
     *
     * @return the method, or an empty {@link Optional} if the field is a store
     */
    public Optional<ExecutableElement> deserializerMethod() {
        return Optional.ofNullable(deserializer);
    }

    /**
     * Returns the invocation of the given mixin method with the given argument, which is
     * a static method, or an instance method invoked on the instance of its mixin.
     *
     * @param method the method, or {@code null} to return the argument unchanged
     * @param argument the expression that provides the argument
     * @return the invocation
     */
    private static String invoke(ExecutableElement method, String argument) {
        if (method == null) {
            return argument;
        }
        return DaedalusMethodElement.of(method, false, "").invoke(argument, List.of(), _ -> {
            throw new IllegalStateException("A conversion of unknown fields has no injected arguments");
        });
    }
}
