package com.github.auties00.daedalus.protobuf.spec;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.exception.ProtobufSerializationException;

/**
 * A specification of a Protocol Buffers enum, which converts the constants of a type
 * annotated with {@link com.github.auties00.daedalus.protobuf.annotation.ProtobufEnum} to and
 * from their index, which is how the binary format encodes them, and their index to and from
 * their name, which is how the text format encodes them.
 *
 * <p>The annotation processor generates a subclass of this class for every enum, named after
 * the enum with the {@code Spec} suffix, whose only instance is returned by its static
 * {@code global()} method. A Spec is stateless, so it is thread safe and can be shared freely.
 * Code that works with any enum can hold a Spec through this class:
 *
 * <pre>{@code
 * ProtobufEnumSpec<Color> spec = ColorSpec.global();
 * Integer index = spec.encode(Color.GREEN);
 * Color color = spec.decode(index);
 * }</pre>
 *
 * <p>The checked exceptions thrown by the converters of an enum, which convert its constants
 * to and from their index if it declares them, are wrapped in a
 * {@link ProtobufSerializationException} or in a {@link ProtobufDeserializationException},
 * whose cause is the exception thrown by the converter. A generated subclass declares only the
 * exceptions that its methods can throw.
 *
 * @param <E> the type of the enum
 */
public abstract class ProtobufEnumSpec<E> {
    /**
     * Constructs a new Spec, which is only invoked by the generated subclass.
     */
    protected ProtobufEnumSpec() {

    }

    /**
     * Returns the index of the given constant.
     *
     * @param constant the constant, or {@code null}
     * @return the index of the constant, or {@code null} if it is {@code null}
     * @throws ProtobufSerializationException if a converter of the enum fails
     */
    public abstract Integer encode(E constant) throws ProtobufSerializationException;

    /**
     * Returns the constant with the given index.
     *
     * @param index the index, or {@code null}
     * @return the constant with the index, or {@code null} if the index is {@code null} or no
     *         constant has it
     * @throws ProtobufDeserializationException if a converter of the enum fails
     */
    public abstract E decode(Integer index) throws ProtobufDeserializationException;

    /**
     * Returns the size of the binary encoding of the index of the given constant.
     *
     * @param constant the constant, or {@code null}
     * @return the size of the binary encoding of its index, or {@code 0} if it is {@code null}
     * @throws ProtobufSerializationException if a converter of the enum fails
     */
    public abstract int sizeOfBinary(E constant) throws ProtobufSerializationException;

    /**
     * Returns the name of the constant with the given index, which is how the text format
     * writes it.
     *
     * @param index the index
     * @return the name of the constant with the index, or {@code null} if no constant has it,
     *         in which case the text format writes the index
     */
    public abstract String nameOf(int index);

    /**
     * Returns the index of the constant with the given name, which is how the text format
     * reads it.
     *
     * @param name the name
     * @return the index of the constant with the name
     * @throws ProtobufDeserializationException if no constant has the name
     */
    public abstract int indexOf(String name) throws ProtobufDeserializationException;
}
