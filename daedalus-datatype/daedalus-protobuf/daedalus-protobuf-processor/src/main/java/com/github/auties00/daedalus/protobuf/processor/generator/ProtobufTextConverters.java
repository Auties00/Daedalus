package com.github.auties00.daedalus.protobuf.processor.generator;

import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufBinaryReader;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufBinaryWriter;
import com.github.auties00.daedalus.protobuf.io.writer.ProtobufTextWriter;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufArrayFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufCollectionFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufMapFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufOneOfFieldType;
import com.github.auties00.daedalus.protobuf.processor.type.ProtobufSimpleFieldType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * Static utility methods for resolving the text counterparts of the converters that use the
 * binary format directly.
 *
 * <p>The text methods of a Spec class convert the value of each field with the same converters
 * as the binary methods. A converter that has a parameter whose type is a binary writer or
 * reader is replaced by its text counterpart, which takes a text writer or reader in its place,
 * and has either the same name, or the name of the converter with the {@code Binary} suffix
 * replaced by {@code Text}: for example, the {@code encodeBinary} and {@code decodeBinary}
 * methods of the Spec of a message are replaced by its {@code encodeText} and
 * {@code decodeText} methods. The Spec classes declare these counterparts, but a mixin that
 * reads or writes the binary format directly may not: the value of a field converted by such a
 * mixin cannot be written to, or read from, the text format.
 */
public final class ProtobufTextConverters {
    /**
     * The suffix of the name of a converter that uses the binary format.
     */
    private static final String BINARY_SUFFIX = "Binary";

    /**
     * The suffix of the name of the text counterpart of a converter that uses the binary format.
     */
    private static final String TEXT_SUFFIX = "Text";
    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufTextConverters() {
        throw new UnsupportedOperationException("ProtobufTextConverters is a utility class and cannot be instantiated");
    }

    /**
     * Returns the first serializer of the given field type, or of the types it holds, that
     * writes the binary format directly and has no overload that writes the text format.
     *
     * @param type the type of the field
     * @return the serializer, if any
     */
    public static Optional<DaedalusMethodElement> findBinaryOnlySerializer(ProtobufFieldType type) {
        return findBinaryOnly(type, true);
    }

    /**
     * Returns the first deserializer of the given field type, or of the types it holds, that
     * reads the binary format directly and has no overload that reads the text format.
     *
     * @param type the type of the field
     * @return the deserializer, if any
     */
    public static Optional<DaedalusMethodElement> findBinaryOnlyDeserializer(ProtobufFieldType type) {
        return findBinaryOnly(type, false);
    }

    /**
     * Returns the first converter of the given field type, or of the types it holds, that only
     * supports the binary format.
     *
     * @param type the type of the field
     * @param serializers whether the serializers are searched, or the deserializers
     * @return the converter, if any
     */
    private static Optional<DaedalusMethodElement> findBinaryOnly(ProtobufFieldType type, boolean serializers) {
        List<? extends DaedalusConverterElement.Attributed> converters = serializers ? type.serializers() : type.deserializers();
        for (var converter : converters) {
            var delegate = converter.delegate();
            if (findTextName(delegate).isEmpty()) {
                return Optional.of(delegate);
            }
        }

        return switch (type) {
            case ProtobufCollectionFieldType collectionType -> findBinaryOnly(collectionType.valueType(), serializers);
            case ProtobufArrayFieldType arrayType -> findBinaryOnly(arrayType.valueType(), serializers);
            case ProtobufMapFieldType mapType -> findBinaryOnly(mapType.keyType(), serializers)
                    .or(() -> findBinaryOnly(mapType.valueType(), serializers));
            case ProtobufSimpleFieldType _, ProtobufOneOfFieldType _ -> Optional.empty();
        };
    }

    /**
     * Returns the invocation of the text counterpart of the given converter, given the
     * invocation of the converter.
     *
     * <p>The invocation of a converter whose text counterpart has the same name, or that has no
     * text counterpart, is returned as it is.
     *
     * @param method the converter
     * @param invocation the invocation of the converter
     * @return the invocation of the text counterpart of the converter
     */
    public static String toTextInvocation(DaedalusMethodElement method, String invocation) {
        var textName = findTextName(method).orElse(method.name());
        var target = method.receiver().orElse(method.ownerName());
        var prefix = target + "." + method.name() + "(";
        if (textName.equals(method.name()) || !invocation.startsWith(prefix)) {
            return invocation;
        }

        return target + "." + textName + "(" + invocation.substring(prefix.length());
    }

    /**
     * Returns the name of the text counterpart of the given converter.
     *
     * <p>A converter that has no parameter whose type is a binary writer or reader does not
     * depend on the format, so it is its own counterpart. A synthetic converter, like a method
     * of a Spec class, has a counterpart whose name replaces the {@code Binary} suffix of its
     * name with {@code Text}. Any other converter has a counterpart if its owner declares a
     * method, named like the converter or like the converter with that suffix replaced, whose
     * parameters are the ones of the converter, except that each binary writer or reader is
     * replaced by a text writer or reader.
     *
     * @param method the converter
     * @return the name of the text counterpart, if the converter has one
     */
    private static Optional<String> findTextName(DaedalusMethodElement method) {
        var parameters = method.parameters();
        if (parameters.stream().noneMatch(parameter -> getTextCounterpart(parameter) != null)) {
            return Optional.of(method.name());
        }

        var swappedName = method.name().endsWith(BINARY_SUFFIX)
                ? method.name().substring(0, method.name().length() - BINARY_SUFFIX.length()) + TEXT_SUFFIX
                : null;
        var element = method.element();
        if (element.isEmpty()) {
            return Optional.of(swappedName != null ? swappedName : method.name());
        }

        if (!(element.get().getEnclosingElement() instanceof TypeElement owner)) {
            return Optional.empty();
        }

        for (var name : swappedName != null ? List.of(method.name(), swappedName) : List.of(method.name())) {
            for (var candidate : owner.getEnclosedElements()) {
                if (candidate instanceof ExecutableElement executable
                        && executable.getSimpleName().contentEquals(name)
                        && isTextOverload(parameters, executable)) {
                    return Optional.of(name);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the parameters of the given method match the given parameters of a
     * converter, with each binary writer or reader replaced by a text writer or reader.
     *
     * @param parameters the parameters of the converter
     * @param candidate the method
     * @return {@code true} if the method is the text overload of the converter
     */
    private static boolean isTextOverload(List<TypeMirror> parameters, ExecutableElement candidate) {
        var candidateParameters = candidate.getParameters();
        if (candidateParameters.size() != parameters.size()) {
            return false;
        }

        for (var i = 0; i < parameters.size(); i++) {
            var expected = getTextCounterpart(parameters.get(i));
            var actual = getQualifiedName(candidateParameters.get(i).asType());
            if (!actual.equals(expected != null ? expected : getQualifiedName(parameters.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the qualified name of the text writer or reader that replaces the given binary
     * writer or reader.
     *
     * @param type the type of a parameter
     * @return the qualified name of the text counterpart, or {@code null} if the type is
     *         neither a binary writer nor a binary reader
     */
    private static String getTextCounterpart(TypeMirror type) {
        var name = getQualifiedName(type);
        if (name.equals(ProtobufBinaryWriter.class.getName())) {
            return ProtobufTextWriter.class.getName();
        } else if (name.equals(ProtobufBinaryReader.class.getName())) {
            return ProtobufTextReader.class.getName();
        } else {
            return null;
        }
    }

    /**
     * Returns the qualified name of the given type, without its type arguments.
     *
     * @param type the type
     * @return the qualified name of the type
     */
    private static String getQualifiedName(TypeMirror type) {
        if (type instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement typeElement) {
            return typeElement.getQualifiedName().toString();
        }
        return type.toString();
    }
}
