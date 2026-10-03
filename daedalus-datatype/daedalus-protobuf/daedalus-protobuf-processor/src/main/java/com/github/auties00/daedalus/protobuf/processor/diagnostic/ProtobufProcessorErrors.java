package com.github.auties00.daedalus.protobuf.processor.diagnostic;

import com.github.auties00.daedalus.protobuf.model.ProtobufFieldPresence;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringJoiner;

/**
 * Static factory methods for the diagnostics reported by the Protocol Buffers annotation
 * processor when it validates messages, enums and their fields.
 *
 * <p>Every method returns the text of a diagnostic localized for the
 * {@linkplain Locale#getDefault() default locale}. The text is looked up in the
 * {@code diagnostics} resource bundle of this package using {@link MessageFormat}, and the
 * key of each entry is documented by the method that uses it.
 *
 * <p>This class is thread-safe.
 */
public final class ProtobufProcessorErrors {
    /**
     * The base name of the resource bundle containing the text of every diagnostic.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.protobuf.processor.diagnostic.diagnostics";

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufProcessorErrors() {
        throw new UnsupportedOperationException("ProtobufProcessorErrors is a utility class and cannot be instantiated");
    }

    /**
     * Returns the diagnostic reported when a field index is out of the range allowed by
     * Protocol Buffers.
     *
     * <p>The text is resolved from the {@code field.indexOutOfRange} key.
     *
     * @param index the index of the field
     * @param min the minimum allowed index
     * @param max the maximum allowed index
     * @return the localized diagnostic
     */
    public static String fieldIndexOutOfRange(int index, int min, int max) {
        return localize("field.indexOutOfRange", index, min, max);
    }

    /**
     * Returns the diagnostic reported when a field index is in the range that Protocol
     * Buffers reserves for its implementation.
     *
     * <p>The text is resolved from the {@code field.indexInImplementationRange} key.
     *
     * @param index the index of the field
     * @param min the first index of the reserved range
     * @param max the last index of the reserved range
     * @return the localized diagnostic
     */
    public static String fieldIndexInImplementationRange(int index, int min, int max) {
        return localize("field.indexInImplementationRange", index, min, max);
    }

    /**
     * Returns the diagnostic reported when the name of a field is not a valid Protocol
     * Buffers identifier.
     *
     * <p>The text is resolved from the {@code field.invalidName} key.
     *
     * @param name the name of the field
     * @return the localized diagnostic
     */
    public static String fieldInvalidName(String name) {
        return localize("field.invalidName", name);
    }

    /**
     * Returns the diagnostic reported when an element is annotated with more than one
     * field annotation.
     *
     * <p>The text is resolved from the {@code field.conflictingAnnotations} key.
     *
     * @param annotations the simple names of the field annotations of the element
     * @return the localized diagnostic
     */
    public static String fieldConflictingAnnotations(Collection<String> annotations) {
        var names = new StringJoiner(", ");
        for (var annotation : annotations) {
            names.add("@" + annotation);
        }
        return localize("field.conflictingAnnotations", names.toString());
    }

    /**
     * Returns the diagnostic reported when a field is required in a version that does
     * not support required fields.
     *
     * <p>The text is resolved from the {@code field.requiredNotSupported} key.
     *
     * @param version the version of the enclosing message
     * @return the localized diagnostic
     */
    public static String fieldRequiredNotSupported(ProtobufVersion version) {
        return localize("field.requiredNotSupported", version);
    }

    /**
     * Returns the diagnostic reported when a repeated or map field is required.
     *
     * <p>The text is resolved from the {@code field.requiredRepeated} key.
     *
     * @return the localized diagnostic
     */
    public static String fieldRequiredRepeated() {
        return localize("field.requiredRepeated");
    }

    /**
     * Returns the diagnostic reported when a required field has a primitive type.
     *
     * <p>The text is resolved from the {@code field.requiredPrimitive} key.
     *
     * @return the localized diagnostic
     */
    public static String fieldRequiredPrimitive() {
        return localize("field.requiredPrimitive");
    }

    /**
     * Returns the diagnostic reported when a field has implicit presence in a version
     * that does not support it.
     *
     * <p>The text is resolved from the {@code field.implicitNotSupported} key.
     *
     * @param version the version of the enclosing message
     * @return the localized diagnostic
     */
    public static String fieldImplicitNotSupported(ProtobufVersion version) {
        return localize("field.implicitNotSupported", version);
    }

    /**
     * Returns the diagnostic reported when a message field has implicit presence.
     *
     * <p>The text is resolved from the {@code field.implicitMessage} key.
     *
     * @return the localized diagnostic
     */
    public static String fieldImplicitMessage() {
        return localize("field.implicitMessage");
    }

    /**
     * Returns the diagnostic reported when a field with explicit presence has a primitive
     * type, which cannot represent a field that is not set.
     *
     * <p>The text is resolved from the {@code field.explicitPrimitive} key.
     *
     * @param type the primitive type of the field
     * @param boxedType the boxed type that can represent a field that is not set
     * @param version the version of the enclosing message
     * @return the localized diagnostic
     */
    public static String fieldExplicitPrimitive(TypeMirror type, TypeMirror boxedType, ProtobufVersion version) {
        return localize("field.explicitPrimitive", type, boxedType, version);
    }

    /**
     * Returns the diagnostic reported when a repeated or map field declares a presence,
     * which only fields that are not repeated have.
     *
     * <p>The text is resolved from the {@code field.presenceRepeated} key.
     *
     * @param presence the declared presence
     * @return the localized diagnostic
     */
    public static String fieldPresenceRepeated(ProtobufFieldPresence presence) {
        return localize("field.presenceRepeated", presence);
    }

    /**
     * Returns the diagnostic reported when a field with implicit presence has the type of a
     * closed enum.
     *
     * <p>The text is resolved from the {@code field.implicitClosedEnum} key.
     *
     * @param enumType the type of the closed enum
     * @return the localized diagnostic
     */
    public static String fieldImplicitClosedEnum(TypeMirror enumType) {
        return localize("field.implicitClosedEnum", enumType);
    }

    /**
     * Returns the diagnostic reported when a repeated field is held by an array whose
     * component type is not reifiable, so that the array cannot be created.
     *
     * <p>The text is resolved from the {@code field.arrayNotReifiable} key.
     *
     * @param arrayType the array type of the field
     * @return the localized diagnostic
     */
    public static String fieldArrayNotReifiable(TypeMirror arrayType) {
        return localize("field.arrayNotReifiable", arrayType);
    }

    /**
     * Returns the warning reported when a packed field cannot be written and read in bulk,
     * because its type is not an array of the primitive type of its wire values.
     *
     * <p>The text is resolved from the {@code field.packedNotBulk} key.
     *
     * @param type the type of the field
     * @param protobufType the protobuf type of the elements of the field
     * @param bulkType the array type that would be written and read in bulk
     * @return the localized diagnostic
     */
    public static String fieldPackedNotBulk(TypeMirror type, ProtobufType protobufType, String bulkType) {
        return localize("field.packedNotBulk", type, protobufType, bulkType);
    }

    /**
     * Returns the diagnostic reported when a field that is not repeated is packed.
     *
     * <p>The text is resolved from the {@code field.packedNotRepeated} key.
     *
     * @return the localized diagnostic
     */
    public static String fieldPackedNotRepeated() {
        return localize("field.packedNotRepeated");
    }

    /**
     * Returns the diagnostic reported when a map field has a key type that Protocol
     * Buffers does not allow.
     *
     * <p>The text is resolved from the {@code field.invalidMapKeyType} key.
     *
     * @param type the key type
     * @return the localized diagnostic
     */
    public static String fieldInvalidMapKeyType(ProtobufType type) {
        return localize("field.invalidMapKeyType", type);
    }

    /**
     * Returns the diagnostic reported when a map field does not have a map type.
     *
     * <p>The text is resolved from the {@code field.mapNotMap} key.
     *
     * @param type the type of the field
     * @return the localized diagnostic
     */
    public static String fieldMapNotMap(TypeMirror type) {
        return localize("field.mapNotMap", type);
    }

    /**
     * Returns the diagnostic reported when a method annotated with a field annotation
     * cannot be read as a synthetic field.
     *
     * <p>The text is resolved from the {@code field.syntheticMethod} key.
     *
     * @return the localized diagnostic
     */
    public static String fieldSyntheticMethod() {
        return localize("field.syntheticMethod");
    }

    /**
     * Returns the diagnostic reported when the name of a field or constant is reserved.
     *
     * <p>The text is resolved from the {@code field.reservedName} key.
     *
     * @param name the reserved name
     * @return the localized diagnostic
     */
    public static String fieldReservedName(String name) {
        return localize("field.reservedName", name);
    }

    /**
     * Returns the diagnostic reported when the index of a field or constant is reserved.
     *
     * <p>The text is resolved from the {@code field.reservedIndex} key.
     *
     * @param index the reserved index
     * @return the localized diagnostic
     */
    public static String fieldReservedIndex(int index) {
        return localize("field.reservedIndex", index);
    }

    /**
     * Returns the diagnostic reported when a reserved range is out of the bounds of the
     * indexes it reserves.
     *
     * <p>The text is resolved from the {@code reserved.rangeOutOfBounds} key.
     *
     * @param min the minimum of the range
     * @param max the maximum of the range
     * @param lower the lowest allowed index
     * @param upper the highest allowed index
     * @return the localized diagnostic
     */
    public static String reservedRangeOutOfBounds(int min, int max, int lower, int upper) {
        return localize("reserved.rangeOutOfBounds", min, max, lower, upper);
    }

    /**
     * Returns the diagnostic reported when the minimum of a reserved range is greater than
     * its maximum.
     *
     * <p>The text is resolved from the {@code reserved.rangeInverted} key.
     *
     * @param min the minimum of the range
     * @param max the maximum of the range
     * @return the localized diagnostic
     */
    public static String reservedRangeInverted(int min, int max) {
        return localize("reserved.rangeInverted", min, max);
    }

    /**
     * Returns the diagnostic reported when the first constant of an open enum does not
     * have index {@code 0}.
     *
     * <p>The text is resolved from the {@code enum.openWithoutZero} key.
     *
     * @param constant the name of the first constant
     * @param index the index of the first constant
     * @return the localized diagnostic
     */
    public static String enumOpenWithoutZero(String constant, int index) {
        return localize("enum.openWithoutZero", constant, index);
    }

    /**
     * Returns the error reported when a oneof is held by a field whose type is not sealed.
     *
     * <p>The text is resolved from the {@code oneof.notSealed} key.
     *
     * @param type the type of the field
     * @return the localized diagnostic
     */
    public static String oneOfNotSealed(TypeMirror type) {
        return localize("oneof.notSealed", type);
    }

    /**
     * Returns the error reported when a oneof declares no case.
     *
     * <p>The text is resolved from the {@code oneof.empty} key.
     *
     * @param type the sealed type of the oneof
     * @return the localized diagnostic
     */
    public static String oneOfEmpty(TypeMirror type) {
        return localize("oneof.empty", type);
    }

    /**
     * Returns the error reported when a case of a oneof holds a type that the sealed type of
     * the oneof does not permit.
     *
     * <p>The text is resolved from the {@code oneof.caseNotPermitted} key.
     *
     * @param index the index of the case
     * @param caseType the type held by the case
     * @param type the sealed type of the oneof
     * @return the localized diagnostic
     */
    public static String oneOfCaseNotPermitted(int index, TypeMirror caseType, TypeMirror type) {
        return localize("oneof.caseNotPermitted", index, caseType, type);
    }

    /**
     * Returns the error reported when two cases of a oneof hold the same type.
     *
     * <p>The text is resolved from the {@code oneof.duplicatedCaseType} key.
     *
     * @param caseType the type held by both cases
     * @param firstIndex the index of the first case
     * @param secondIndex the index of the second case
     * @return the localized diagnostic
     */
    public static String oneOfDuplicatedCaseType(TypeMirror caseType, int firstIndex, int secondIndex) {
        return localize("oneof.duplicatedCaseType", caseType, firstIndex, secondIndex);
    }

    /**
     * Returns the error reported when two cases of a oneof have the same index.
     *
     * <p>The text is resolved from the {@code oneof.duplicatedCaseIndex} key.
     *
     * @param index the index of both cases
     * @return the localized diagnostic
     */
    public static String oneOfDuplicatedCaseIndex(int index) {
        return localize("oneof.duplicatedCaseIndex", index);
    }

    /**
     * Returns the error reported when a case of a oneof holds a type that is converted to a
     * repeated or a map field.
     *
     * <p>The text is resolved from the {@code oneof.repeatedCase} key.
     *
     * @param index the index of the case
     * @param caseType the type held by the case
     * @return the localized diagnostic
     */
    public static String oneOfRepeatedCase(int index, TypeMirror caseType) {
        return localize("oneof.repeatedCase", index, caseType);
    }

    /**
     * Returns the error reported when some of the types that the sealed type of a oneof
     * permits are held by no case.
     *
     * <p>The text is resolved from the {@code oneof.missingCases} key.
     *
     * @param type the sealed type of the oneof
     * @param missingTypes the types that no case holds
     * @return the localized diagnostic
     */
    public static String oneOfMissingCases(TypeMirror type, Collection<?> missingTypes) {
        var joiner = new StringJoiner(", ");
        for (var missingType : missingTypes) {
            joiner.add(String.valueOf(missingType));
        }
        return localize("oneof.missingCases", type, joiner.toString());
    }

    /**
     * Returns the error reported when a message field holds a message whose builder is not
     * public and is in another package, so that the builder of the message that declares the
     * field cannot hold its builder.
     *
     * <p>The text is resolved from the {@code field.builderNotAccessible} key.
     *
     * @param field the name of the field
     * @param messageType the type of the message held by the field
     * @param builderType the name of the builder of that message
     * @return the localized diagnostic
     */
    public static String fieldBuilderNotAccessible(String field, TypeMirror messageType, String builderType) {
        return localize("field.builderNotAccessible", field, messageType, builderType);
    }

    /**
     * Returns the diagnostic reported when a type that cannot hold a known set of values is
     * annotated with {@code @ProtobufEnum}.
     *
     * <p>The text is resolved from the {@code enum.illegalType} key.
     *
     * @param type the simple name of the type
     * @return the localized diagnostic
     */
    public static String enumIllegalType(String type) {
        return localize("enum.illegalType", type);
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @ProtobufEnum.Constant} is not a constant of its protobuf enum.
     *
     * <p>The text is resolved from the {@code enum.illegalConstant} key.
     *
     * @param constant the name of the field
     * @param type the simple name of the protobuf enum
     * @return the localized diagnostic
     */
    public static String enumIllegalConstant(String constant, String type) {
        return localize("enum.illegalConstant", constant, type);
    }

    /**
     * Returns the diagnostic reported when a protobuf enum that is not a Java enum declares
     * neither constants nor the methods that convert its values.
     *
     * <p>The text is resolved from the {@code enum.withoutValues} key.
     *
     * @param type the simple name of the protobuf enum
     * @return the localized diagnostic
     */
    public static String enumWithoutValues(String type) {
        return localize("enum.withoutValues", type);
    }

    /**
     * Returns the diagnostic reported when the serializer of a protobuf enum does not return
     * the index of the value it is invoked on.
     *
     * <p>The text is resolved from the {@code enum.illegalSerializer} key.
     *
     * @param method the name of the method
     * @return the localized diagnostic
     */
    public static String enumIllegalSerializer(String method) {
        return localize("enum.illegalSerializer", method);
    }

    /**
     * Returns the diagnostic reported when the deserializer of a protobuf enum does not
     * return the value of an index.
     *
     * <p>The text is resolved from the {@code enum.illegalDeserializer} key.
     *
     * @param method the name of the method
     * @param type the simple name of the protobuf enum
     * @return the localized diagnostic
     */
    public static String enumIllegalDeserializer(String method, String type) {
        return localize("enum.illegalDeserializer", method, type);
    }

    /**
     * Returns the diagnostic reported when a protobuf enum declares only one of the methods
     * that convert its values to and from their index.
     *
     * <p>The text is resolved from the {@code enum.missingConverter} key.
     *
     * @param type the simple name of the protobuf enum
     * @return the localized diagnostic
     */
    public static String enumMissingConverter(String type) {
        return localize("enum.missingConverter", type);
    }

    /**
     * Returns the warning reported when a field of an open protobuf enum is typed as a Java
     * enum, which cannot hold the values that have no constant.
     *
     * <p>The text is resolved from the {@code field.openJavaEnum} key.
     *
     * @param enumType the Java enum type of the field
     * @return the localized diagnostic
     */
    public static String fieldOpenJavaEnum(TypeMirror enumType) {
        return localize("field.openJavaEnum", enumType);
    }

    /**
     * Returns the diagnostic reported when a builder parameter maps to an index that no
     * field of the message has.
     *
     * <p>The text is resolved from the {@code builder.unknownFieldIndex} key.
     *
     * @param index the index of the builder parameter
     * @return the localized diagnostic
     */
    public static String builderUnknownFieldIndex(int index) {
        return localize("builder.unknownFieldIndex", index);
    }

    /**
     * Returns the error reported when the builder of a message is given a modifier that a top
     * level class instantiated by the Spec class of the message cannot have.
     *
     * <p>The text is resolved from the {@code builder.invalidModifier} key.
     *
     * @param modifier the invalid modifier
     * @return the localized diagnostic
     */
    public static String builderInvalidModifier(ClassModifier modifier) {
        return localize("builder.invalidModifier", modifier);
    }

    /**
     * Returns the error reported when a builder declared on a message with
     * {@code @TypeBuilder} has no name, which would clash with the builder that is always
     * generated for the message.
     *
     * <p>The text is resolved from the {@code builder.missingName} key.
     *
     * @param builderType the name of the builder that is always generated for the message
     * @return the localized diagnostic
     */
    public static String builderMissingName(String builderType) {
        return localize("builder.missingName", builderType);
    }

    /**
     * Returns the diagnostic reported when the classes generated for a protobuf type cannot
     * be written to the source output of the compilation.
     *
     * <p>The text is resolved from the {@code generation.ioFailure} key. The cause is the
     * message of {@code exception}, or the name of its class if it has no message.
     *
     * @param type the protobuf type whose classes were being generated
     * @param exception the I/O error that stopped the generation
     * @return the localized diagnostic
     */
    public static String generationFailed(TypeElement type, IOException exception) {
        String cause = Objects.requireNonNullElse(exception.getMessage(), exception.getClass().getName());
        return localize("generation.ioFailure", describe(type), cause);
    }

    /**
     * Returns the diagnostic reported when a message or group declares more than one method
     * annotated with {@code @TypeDeserializer}.
     *
     * <p>The text is resolved from the {@code message.duplicatedDeserializer} key.
     *
     * @param method the method that declares the second deserializer
     * @param previous the name of the method that declares the first deserializer
     * @return the localized diagnostic
     */
    public static String messageDuplicatedDeserializer(ExecutableElement method, String previous) {
        return localize("message.duplicatedDeserializer", describe(method), describeEnclosingType(method), previous);
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @ProtobufAccessor} refers to an index that no field of its message has.
     *
     * <p>The text is resolved from the {@code accessor.unknownIndex} key.
     *
     * @param accessor the method annotated with {@code @ProtobufAccessor}
     * @param index the index that the annotation refers to
     * @param message the message that encloses the accessor
     * @return the localized diagnostic
     */
    public static String accessorUnknownIndex(ExecutableElement accessor, int index, TypeElement message) {
        return localize("accessor.unknownIndex", describe(accessor), index, describe(message));
    }

    /**
     * Returns the diagnostic reported when a message declares no constructor that takes the
     * values of its fields, followed by its unknown fields if it declares them.
     *
     * <p>The text is resolved from the {@code message.missingConstructor} key, and every
     * constructor that the message declares is listed after it using the
     * {@code message.missingConstructor.found} key.
     *
     * @param message the message that has no matching constructor
     * @param expectedParameters the parameters of the expected constructor, each rendered as
     *        its type followed by its name
     * @param constructors the constructors that the message declares
     * @return the localized diagnostic
     */
    public static String messageMissingConstructor(TypeElement message, List<String> expectedParameters, List<ExecutableElement> constructors) {
        StringBuilder found = new StringBuilder();
        for (ExecutableElement constructor : constructors) {
            StringJoiner parameters = new StringJoiner(", ");
            for (VariableElement parameter : constructor.getParameters()) {
                parameters.add(parameter.asType() + " " + parameter.getSimpleName());
            }
            found.append(localize("message.missingConstructor.found", message.getSimpleName(), parameters.toString()));
        }
        return localize("message.missingConstructor", describe(message), message.getSimpleName(), String.join(", ", expectedParameters), found.toString());
    }

    /**
     * Returns the diagnostic reported when a message declares more than one field annotated
     * with {@code @ProtobufMessage.UnknownFields}.
     *
     * <p>The text is resolved from the {@code unknownFields.duplicated} key.
     *
     * @param field the field that declares the second unknown fields
     * @param previous the field that declares the first unknown fields
     * @return the localized diagnostic
     */
    public static String unknownFieldsDuplicated(VariableElement field, VariableElement previous) {
        return localize("unknownFields.duplicated", describe(field), describeEnclosingType(field), describe(previous));
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @ProtobufMessage.UnknownFields} does not have the type of a class or of an
     * interface.
     *
     * <p>The text is resolved from the {@code unknownFields.notDeclaredType} key.
     *
     * @param field the illegal field
     * @return the localized diagnostic
     */
    public static String unknownFieldsNotDeclaredType(VariableElement field) {
        return localize("unknownFields.notDeclaredType", describe(field), field.asType());
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @ProtobufMessage.UnknownFields} is private and has no getter that the generated
     * code can invoke.
     *
     * <p>The text is resolved from the {@code unknownFields.missingAccessor} key.
     *
     * @param field the private field
     * @return the localized diagnostic
     */
    public static String unknownFieldsMissingAccessor(VariableElement field) {
        return localize("unknownFields.missingAccessor", describe(field), describeEnclosingType(field), field.getSimpleName());
    }

    /**
     * Returns the diagnostic reported when the type of a field annotated with
     * {@code @ProtobufMessage.UnknownFields} is not a {@code ProtobufUnknownFields}, and its
     * mixins do not convert it to and from one.
     *
     * <p>The text is resolved from the {@code unknownFields.notConvertible} key.
     *
     * @param field the illegal field
     * @param storeType the type that holds the unknown fields of a message
     * @return the localized diagnostic
     */
    public static String unknownFieldsNotConvertible(VariableElement field, TypeMirror storeType) {
        return localize("unknownFields.notConvertible", describe(field), field.asType(), storeType);
    }

    /**
     * Returns the diagnostic reported when a private field of a message has no accessor that
     * the generated code can invoke.
     *
     * <p>The text is resolved from the {@code field.missingAccessor} key.
     *
     * @param field the private field
     * @param index the index of the field
     * @return the localized diagnostic
     */
    public static String fieldMissingAccessor(Element field, int index) {
        return localize("field.missingAccessor", describe(field), describeEnclosingType(field), field.getSimpleName(), index);
    }

    /**
     * Returns the diagnostic reported when two fields of a message have the same index.
     *
     * <p>The text is resolved from the {@code field.duplicatedIndex} key.
     *
     * @param field the field that was declared last
     * @param previous the name of the field that already has the index
     * @param index the shared index
     * @return the localized diagnostic
     */
    public static String fieldDuplicatedIndex(Element field, String previous, int index) {
        return localize("field.duplicatedIndex", describe(field), previous, index, describeEnclosingType(field));
    }

    /**
     * Returns the warning reported when a field cannot be written to, or read from, the text
     * format, because its value is converted by a converter that uses the binary format
     * directly and has no text overload.
     *
     * <p>The text is resolved from the {@code field.binaryOnlyConverter} key.
     *
     * @param field the name of the field
     * @param converter the name of the converter
     * @return the localized diagnostic
     */
    public static String fieldBinaryOnlyConverter(String field, String converter) {
        return localize("field.binaryOnlyConverter", field, converter);
    }

    /**
     * Returns the diagnostic reported when two fields of a message have the same Protocol
     * Buffers name.
     *
     * <p>The text is resolved from the {@code field.duplicatedName} key.
     *
     * @param field the field that was declared last
     * @param previous the name of the field that already has the Protocol Buffers name
     * @param name the shared Protocol Buffers name
     * @return the localized diagnostic
     */
    public static String fieldDuplicatedName(Element field, String previous, String name) {
        return localize("field.duplicatedName", describe(field), previous, name, describeEnclosingType(field));
    }

    /**
     * Returns the diagnostic reported when a type declares more than one element annotated
     * with {@code @TypeDefaultValue} that provides a default value of the same type.
     *
     * <p>The text is resolved from the {@code defaultValue.duplicated} key.
     *
     * @param candidate the element that provides the second default value
     * @param previous the element that provides the first default value
     * @param type the type of the two default values
     * @param invoker the element whose default value was being resolved
     * @return the localized diagnostic
     */
    public static String defaultValueDuplicated(Element candidate, Element previous, TypeMirror type, Element invoker) {
        return localize("defaultValue.duplicated", describe(candidate), describe(previous), type, describe(invoker), describeEnclosingType(candidate));
    }

    /**
     * Returns the diagnostic reported when the type of the elements of a repeated field held
     * by a collection cannot be determined.
     *
     * <p>The text is resolved from the {@code field.collectionTypeUnknown} key.
     *
     * @param field the repeated field
     * @param type the collection type of the field
     * @return the localized diagnostic
     */
    public static String fieldCollectionTypeUnknown(Element field, TypeMirror type) {
        return localize("field.collectionTypeUnknown", describe(field), type);
    }

    /**
     * Returns the diagnostic reported when the empty collection of a repeated field cannot be
     * created, because its type has no default value.
     *
     * <p>The text is resolved from the {@code field.collectionDefaultUnknown} key.
     *
     * @param field the repeated field
     * @param type the collection type of the field
     * @return the localized diagnostic
     */
    public static String fieldCollectionDefaultUnknown(Element field, TypeMirror type) {
        return localize("field.collectionDefaultUnknown", describe(field), type);
    }

    /**
     * Returns the diagnostic reported when the type of the keys of a map field cannot be
     * determined.
     *
     * <p>The text is resolved from the {@code field.mapKeyTypeUnknown} key.
     *
     * @param field the map field
     * @param type the map type of the field
     * @return the localized diagnostic
     */
    public static String fieldMapKeyTypeUnknown(Element field, TypeMirror type) {
        return localize("field.mapKeyTypeUnknown", describe(field), type);
    }

    /**
     * Returns the diagnostic reported when the type of the values of a map field cannot be
     * determined.
     *
     * <p>The text is resolved from the {@code field.mapValueTypeUnknown} key.
     *
     * @param field the map field
     * @param type the map type of the field
     * @return the localized diagnostic
     */
    public static String fieldMapValueTypeUnknown(Element field, TypeMirror type) {
        return localize("field.mapValueTypeUnknown", describe(field), type);
    }

    /**
     * Returns the diagnostic reported when the empty map of a map field cannot be created,
     * because its type has no default value.
     *
     * <p>The text is resolved from the {@code field.mapDefaultUnknown} key.
     *
     * @param field the map field
     * @param type the map type of the field
     * @return the localized diagnostic
     */
    public static String fieldMapDefaultUnknown(Element field, TypeMirror type) {
        return localize("field.mapDefaultUnknown", describe(field), type);
    }

    /**
     * Returns the diagnostic reported when more than one constant of a protobuf enum has
     * the same index.
     *
     * <p>The text is resolved from the {@code enum.duplicatedIndex} key.
     *
     * @param enumType the protobuf enum
     * @param index the shared index
     * @param constants the names of the constants that share the index, in declaration order
     * @return the localized diagnostic
     */
    public static String enumDuplicatedIndex(TypeElement enumType, int index, Collection<String> constants) {
        return localize("enum.duplicatedIndex", describe(enumType), index, String.join(", ", constants));
    }

    /**
     * Returns the diagnostic reported when two constants of a protobuf enum have the same
     * Protocol Buffers name.
     *
     * <p>The text is resolved from the {@code enum.duplicatedName} key.
     *
     * @param constant the constant that was declared last
     * @param previous the Java name of the constant that already has the Protocol Buffers name
     * @param name the shared Protocol Buffers name
     * @return the localized diagnostic
     */
    public static String enumDuplicatedName(Element constant, String previous, String name) {
        return localize("enum.duplicatedName", describe(constant), previous, name, describeEnclosingType(constant));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @ProtobufMessage} is neither a class nor a record.
     *
     * <p>The text is resolved from the {@code message.illegalType} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String messageIllegalType(Element element) {
        return localize("message.illegalType", describe(element), describeKind(element));
    }

    /**
     * Returns the diagnostic reported when an element annotated with a field annotation is
     * not enclosed by a type annotated with {@code @ProtobufMessage}.
     *
     * <p>The text is resolved from the {@code field.illegalEnclosing} key.
     *
     * @param field the illegal field or method
     * @param annotation the simple name of the field annotation, such as {@code Int32Field}
     * @return the localized diagnostic
     */
    public static String fieldIllegalEnclosing(Element field, String annotation) {
        return localize("field.illegalEnclosing", describe(field), annotation, describeEnclosingType(field));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @ProtobufEnum.Constant} is not enclosed by a type annotated with
     * {@code @ProtobufEnum}.
     *
     * <p>The text is resolved from the {@code enum.constantIllegalEnclosing} key.
     *
     * @param constant the illegal element
     * @return the localized diagnostic
     */
    public static String enumConstantIllegalEnclosing(Element constant) {
        return localize("enum.constantIllegalEnclosing", describe(constant), describeEnclosingType(constant));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @ProtobufAccessor} is not enclosed by a type annotated with
     * {@code @ProtobufMessage}.
     *
     * <p>The text is resolved from the {@code accessor.illegalEnclosing} key.
     *
     * @param accessor the illegal method
     * @return the localized diagnostic
     */
    public static String accessorIllegalEnclosing(Element accessor) {
        return localize("accessor.illegalEnclosing", describe(accessor), describeEnclosingType(accessor));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @ProtobufMessage.UnknownFields} is not enclosed by a type annotated with
     * {@code @ProtobufMessage}.
     *
     * <p>The text is resolved from the {@code unknownFields.illegalEnclosing} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String unknownFieldsIllegalEnclosing(Element element) {
        return localize("unknownFields.illegalEnclosing", describe(element), describeEnclosingType(element));
    }

    /**
     * Returns the diagnostic reported when a parameter of a {@code @TypeBuilder} method of a
     * message is annotated with neither {@code @ProtobufBuilderParameter.Field} nor
     * {@code @ProtobufBuilderParameter.Synthetic}.
     *
     * <p>The text is resolved from the {@code builder.parameterMissingAnnotation} key.
     *
     * @param parameter the parameter that is not annotated
     * @return the localized diagnostic
     */
    public static String builderParameterMissingAnnotation(VariableElement parameter) {
        return localize("builder.parameterMissingAnnotation", parameter.getSimpleName(), describeEnclosing(parameter), describeEnclosingType(parameter));
    }

    /**
     * Returns the diagnostic reported when a parameter of a {@code @TypeBuilder} method of a
     * message is annotated with both {@code @ProtobufBuilderParameter.Field} and
     * {@code @ProtobufBuilderParameter.Synthetic}.
     *
     * <p>The text is resolved from the {@code builder.parameterConflictingAnnotations} key.
     *
     * @param parameter the parameter that is annotated with both annotations
     * @return the localized diagnostic
     */
    public static String builderParameterConflictingAnnotations(VariableElement parameter) {
        return localize("builder.parameterConflictingAnnotations", parameter.getSimpleName(), describeEnclosing(parameter));
    }

    /**
     * Returns the diagnostic reported when a protobuf enum declares both constants annotated
     * with {@code @ProtobufEnum.Constant} and methods annotated with
     * {@code @TypeSerializer} or {@code @TypeDeserializer}.
     *
     * <p>The text is resolved from the {@code enum.mixedConstantsAndConverters} key.
     *
     * @param enumType the protobuf enum
     * @return the localized diagnostic
     */
    public static String enumMixedConstantsAndConverters(TypeElement enumType) {
        return localize("enum.mixedConstantsAndConverters", describe(enumType));
    }

    /**
     * Renders the given element as it should be named by a diagnostic.
     *
     * <p>A type or a package is rendered as its qualified name, while any other element is
     * rendered as its name qualified by the element that encloses it. A method or a
     * constructor also lists the types of its parameters.
     *
     * @param element the element
     * @return the rendered element
     */
    private static String describe(Element element) {
        if (element instanceof TypeElement type) {
            return type.getQualifiedName().toString();
        }

        if (element instanceof PackageElement packageElement) {
            return packageElement.getQualifiedName().toString();
        }

        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) {
            return element.toString();
        }

        if (enclosing instanceof TypeElement type) {
            return type.getQualifiedName() + "." + element;
        }

        return describe(enclosing) + "." + element.getSimpleName();
    }

    /**
     * Renders the element that directly encloses the given element as it should be named
     * by a diagnostic.
     *
     * @param element the element
     * @return the rendered enclosing element
     * @see #describe(Element)
     */
    private static String describeEnclosing(Element element) {
        Element enclosing = element.getEnclosingElement();
        return enclosing == null ? describe(element) : describe(enclosing);
    }

    /**
     * Renders the innermost type that encloses the given element as it should be named by
     * a diagnostic.
     *
     * <p>A parameter is enclosed by a method, so the type that encloses the method is
     * rendered instead. If no type encloses the element, the element itself is rendered.
     *
     * @param element the element
     * @return the rendered enclosing type
     * @see #describe(Element)
     */
    private static String describeEnclosingType(Element element) {
        Element enclosing = element.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof TypeElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return enclosing == null ? describe(element) : describe(enclosing);
    }

    /**
     * Renders the kind of the given element in lowercase words, such as
     * {@code interface} or {@code annotation type}.
     *
     * @param element the element
     * @return the rendered kind
     */
    private static String describeKind(Element element) {
        return element.getKind()
                .name()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');
    }

    /**
     * Formats the bundle entry with the given key for the default locale.
     *
     * @param key the key of the bundle entry
     * @param args the arguments substituted into the entry
     * @return the formatted entry
     */
    private static String localize(String key, Object... args) {
        var locale = Locale.getDefault();
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }
}
