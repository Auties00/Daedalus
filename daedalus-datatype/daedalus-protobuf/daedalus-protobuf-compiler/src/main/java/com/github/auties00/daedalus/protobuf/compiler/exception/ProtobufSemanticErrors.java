package com.github.auties00.daedalus.protobuf.compiler.exception;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufValueTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufUnresolvedTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufGroupTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage.Kind;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;

import java.util.Locale;

/**
 * Static factory methods for the {@linkplain Kind#SEMANTIC semantic} diagnostic messages reported
 * for grammatically valid Protocol Buffer definitions that violate the rules of the language.
 *
 * @see ProtobufLexicalErrors
 * @see ProtobufSyntaxErrors
 */
public final class ProtobufSemanticErrors {
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufSemanticErrors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a message stating that two files with the same name were found in a directory tree.
     *
     * <p>Bundle key: {@code semantic.duplicate.file}.
     *
     * @param file the path of the second file
     * @param previous the path of the file with the same name that was found first
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateFile(String file, String previous) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.file", file, previous);
    }

    /**
     * Returns a message stating that a file declares its package more than once.
     *
     * <p>Bundle key: {@code semantic.duplicate.package}.
     *
     * @param previous the package declared by the first package declaration
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicatePackage(String previous) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.package", previous);
    }

    /**
     * Returns a message stating that a syntax declaration is not the first statement of a file.
     *
     * <p>Bundle key: {@code semantic.syntax.not.first}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage syntaxNotFirst() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.syntax.not.first");
    }

    /**
     * Returns a message stating that an edition declaration is not the first statement of a file.
     *
     * <p>Bundle key: {@code semantic.edition.not.first}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage editionNotFirst() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.edition.not.first");
    }

    /**
     * Returns a message stating that a visibility modifier is used before edition 2024.
     *
     * <p>Bundle key: {@code semantic.visibility.requires.edition.2024}.
     *
     * @param modifier the visibility modifier, such as {@code export}
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage visibilityRequiresEdition2024(String modifier) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.visibility.requires.edition.2024", modifier);
    }

    /**
     * Returns a message stating that the name of a group does not start with a capital letter.
     *
     * <p>Bundle key: {@code semantic.group.name.not.capitalized}.
     *
     * @param name the name of the group
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage groupNameNotCapitalized(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.group.name.not.capitalized", name);
    }

    /**
     * Returns a message stating that a reserved name is written as a string literal in a file that
     * uses editions.
     *
     * <p>Bundle key: {@code semantic.reserved.name.must.be.identifier}.
     *
     * @param name the reserved name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedNameMustBeIdentifier(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.name.must.be.identifier", name);
    }

    /**
     * Returns a message stating that a reserved name is written as an identifier in a proto2 or
     * proto3 file.
     *
     * <p>Bundle key: {@code semantic.reserved.name.must.be.literal}.
     *
     * @param name the reserved name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedNameMustBeLiteral(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.name.must.be.literal", name);
    }

    /**
     * Returns a message stating that a weak import is used in edition 2024.
     *
     * <p>Bundle key: {@code semantic.weak.import.in.edition.2024}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage weakImportInEdition2024() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.weak.import.in.edition.2024");
    }

    /**
     * Returns a message stating that an option import is used before edition 2024.
     *
     * <p>Bundle key: {@code semantic.option.import.requires.edition.2024}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionImportRequiresEdition2024() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.option.import.requires.edition.2024");
    }

    /**
     * Returns a message stating that an imported file cannot be found.
     *
     * <p>Bundle key: {@code semantic.unresolved.import}.
     *
     * @param location the location of the imported file
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedImport(String location) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.import", location);
    }

    /**
     * Returns a message stating that two top level types of a file have the same name.
     *
     * <p>Bundle key: {@code semantic.duplicate.type.name}.
     *
     * @param name the duplicated name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateTypeName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.type.name", name);
    }

    /**
     * Returns a message stating that two fields of a message have the same name.
     *
     * <p>Bundle key: {@code semantic.duplicate.field.name}.
     *
     * @param name the duplicated name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateFieldName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.field.name", name);
    }

    /**
     * Returns a message stating that the JSON name of a field conflicts with the JSON name of
     * another field of the same message.
     *
     * <p>Bundle key: {@code semantic.duplicate.json.field.name}.
     *
     * @param name the name of the field
     * @param jsonName the conflicting JSON name
     * @param previousField the name of the field that already uses the JSON name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateJsonFieldName(String name, String jsonName, String previousField) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.json.field.name", name, jsonName, previousField);
    }

    /**
     * Returns a message stating that two fields of a message have the same number.
     *
     * <p>Bundle key: {@code semantic.duplicate.field.index}.
     *
     * @param name the name of the second field
     * @param index the duplicated field number
     * @param previousField the name of the field that already uses the number
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateFieldIndex(String name, long index, String previousField) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.field.index", name, index, previousField);
    }

    /**
     * Returns a message stating that a nested declaration has the same name as another member of
     * its enclosing message.
     *
     * <p>Bundle key: {@code semantic.duplicate.name}.
     *
     * @param name the duplicated name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.name", name);
    }

    /**
     * Returns a message stating that a reserved range overlaps with an extension range declared
     * earlier in the same message.
     *
     * <p>Bundle key: {@code semantic.extension.range.overlaps.reserved.range}.
     *
     * @param reservedRange the overlapping reserved number or range, rendered as text
     * @param extensionRange the extension range it overlaps, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionRangeOverlapsReservedRange(String reservedRange, String extensionRange) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.range.overlaps.reserved.range", reservedRange, extensionRange);
    }

    /**
     * Returns a message stating that an extension range overlaps with a reserved range declared
     * earlier in the same message.
     *
     * <p>Bundle key: {@code semantic.reserved.range.overlaps.extension.range}.
     *
     * @param extensionRange the overlapping extension number or range, rendered as text
     * @param reservedRange the reserved range it overlaps, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedRangeOverlapsExtensionRange(String extensionRange, String reservedRange) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.range.overlaps.extension.range", extensionRange, reservedRange);
    }

    /**
     * Returns a message stating that the name of a field is reserved.
     *
     * <p>Bundle key: {@code semantic.reserved.field.name}.
     *
     * @param name the reserved name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedFieldName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.field.name", name);
    }

    /**
     * Returns a message stating that the number of a field is reserved.
     *
     * <p>Bundle key: {@code semantic.reserved.field.index}.
     *
     * @param index the reserved field number
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedFieldIndex(long index, String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.field.index", index, field);
    }

    /**
     * Returns a message stating that the number of a field falls within an extension range.
     *
     * <p>Bundle key: {@code semantic.extensible.field.index}.
     *
     * @param index the field number
     * @param field the name of the field
     * @param extensionRange the extension range that contains the number, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensibleFieldIndex(long index, String field, String extensionRange) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extensible.field.index", index, field, extensionRange);
    }

    /**
     * Returns a message stating that the type targeted by an extend declaration cannot be resolved.
     *
     * <p>Bundle key: {@code semantic.unresolved.extend.target}.
     *
     * @param name the name of the extended type
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedExtendTarget(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.extend.target", name);
    }

    /**
     * Returns a message stating that the type targeted by an extend declaration is neither a
     * message nor a group.
     *
     * <p>Bundle key: {@code semantic.invalid.extend.target}.
     *
     * @param name the name of the extended type
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidExtendTarget(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.invalid.extend.target", name);
    }

    /**
     * Returns a message stating that an extension field uses the {@code required} modifier.
     *
     * <p>Bundle key: {@code semantic.required.extension.field}.
     *
     * @param name the name of the extension field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage requiredExtensionField(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.required.extension.field", name);
    }

    /**
     * Returns a message stating that an extension field has a map type.
     *
     * <p>Bundle key: {@code semantic.map.extension.field}.
     *
     * @param name the name of the extension field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage mapExtensionField(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.map.extension.field", name);
    }

    /**
     * Returns a message stating that the number of an extension field is outside the extension
     * ranges of the extended message.
     *
     * <p>Bundle key: {@code semantic.extension.field.outside.ranges}, or
     * {@code semantic.extension.field.outside.ranges.none} when {@code ranges} is {@code null}.
     *
     * @param name the name of the extension field
     * @param index the number of the extension field
     * @param message the name of the extended message
     * @param ranges the extension ranges of the extended message, rendered as text, or {@code null}
     *               if it declares none
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionFieldOutsideRanges(String name, long index, String message, String ranges) {
        return ranges == null
                ? ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.field.outside.ranges.none", name, index, message)
                : ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.field.outside.ranges", name, index, message, ranges);
    }

    /**
     * Returns a message stating that the {@code features} option is used outside editions.
     *
     * <p>Bundle key: {@code semantic.features.require.editions}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage featuresRequireEditions() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.features.require.editions");
    }

    /**
     * Returns a message stating that a repeated field specifies a field presence feature.
     *
     * <p>Bundle key: {@code semantic.repeated.field.presence}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage repeatedFieldPresence(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.repeated.field.presence", field);
    }

    /**
     * Returns a message stating that the {@code ctype} option is used in edition 2024 or later.
     *
     * <p>Bundle key: {@code semantic.ctype.in.edition.2024}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage ctypeInEdition2024(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.ctype.in.edition.2024", field);
    }

    /**
     * Returns a message stating that a default value is used in a proto3 file.
     *
     * <p>Bundle key: {@code semantic.default.value.in.proto3}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueInProto3(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.in.proto3", field);
    }

    /**
     * Returns a message stating that a default value is used on a field with implicit presence.
     *
     * <p>Bundle key: {@code semantic.default.value.with.implicit.presence}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueWithImplicitPresence(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.with.implicit.presence", field);
    }

    /**
     * Returns a message stating that a default value is used on a repeated field.
     *
     * <p>Bundle key: {@code semantic.default.value.on.repeated.field}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueOnRepeatedField(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.on.repeated.field", field);
    }

    /**
     * Returns a message stating that a default value is used on a field whose type cannot have
     * one, such as a message, a group or a map.
     *
     * <p>Bundle key: {@code semantic.default.value.on.invalid.type}.
     *
     * @param field the name of the field
     * @param type the type of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueOnInvalidType(String field, ProtobufTypeReference type) {
        var typeName = switch (type) {
            case ProtobufMapTypeReference _ -> "map";
            case ProtobufGroupTypeReference _ -> "group";
            case ProtobufUnresolvedTypeReference unresolved -> unresolved.name();
            case ProtobufValueTypeReference valueType -> typeName(valueType.protobufType());
        };
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.on.invalid.type", field, typeName);
    }

    /**
     * Returns a message stating that the default value of a field does not match its type.
     *
     * <p>Bundle key: {@code semantic.default.value.type.mismatch}.
     *
     * @param field the name of the field
     * @param type the type of the field
     * @param value the default value, as written in the source
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueTypeMismatch(String field, ProtobufType type, String value) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.type.mismatch", field, typeName(type), value);
    }

    /**
     * Returns a message stating that the default value of an enum field references a constant that
     * does not exist.
     *
     * <p>Bundle key: {@code semantic.default.value.unknown.enum.constant}.
     *
     * @param constant the name of the referenced constant
     * @param enumName the name of the enum
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueUnknownEnumConstant(String constant, String enumName) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.unknown.enum.constant", constant, enumName);
    }

    /**
     * Returns a message stating that the default value of a field is out of the range of its type.
     *
     * <p>Bundle key: {@code semantic.default.value.out.of.range}.
     *
     * @param value the default value
     * @param field the name of the field
     * @param type the type of the field
     * @param min the smallest valid value, rendered as text
     * @param max the largest valid value, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage defaultValueOutOfRange(long value, String field, ProtobufType type, String min, String max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.default.value.out.of.range", value, field, typeName(type), min, max);
    }

    /**
     * Returns a message stating that the {@code packed} option is used in a file that uses
     * editions.
     *
     * <p>Bundle key: {@code semantic.packed.in.editions}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage packedInEditions(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.packed.in.editions", field);
    }

    /**
     * Returns a message stating that the {@code packed} option is used on a field that is not
     * repeated.
     *
     * <p>Bundle key: {@code semantic.packed.on.non.repeated.field}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage packedOnNonRepeatedField(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.packed.on.non.repeated.field", field);
    }

    /**
     * Returns a message stating that the {@code packed} option is used on a field whose type cannot
     * be packed.
     *
     * <p>Bundle key: {@code semantic.packed.on.non.packable.field}.
     *
     * @param field the name of the field
     * @param type the type of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage packedOnNonPackableField(String field, ProtobufType type) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.packed.on.non.packable.field", field, typeName(type));
    }

    /**
     * Returns a message stating that the value of the {@code json_name} option is not a string.
     *
     * <p>Bundle key: {@code semantic.json.name.not.string}.
     *
     * @param field the name of the field
     * @param value the value of the option, as written in the source
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage jsonNameNotString(String field, String value) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.json.name.not.string", field, value);
    }

    /**
     * Returns a message stating that the type of a field cannot be resolved.
     *
     * <p>Bundle key: {@code semantic.unresolved.field.type}, or
     * {@code semantic.unresolved.field.type.nested} when {@code parent} is not {@code null}.
     *
     * @param type the name of the unresolved type
     * @param field the name of the field
     * @param parent the name of the declaration that contains the field, or {@code null}
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedFieldType(String type, String field, String parent) {
        return parent == null
                ? ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.field.type", type, field)
                : ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.field.type.nested", type, field, parent);
    }

    /**
     * Returns a message stating that the key type of a map field cannot be resolved.
     *
     * <p>Bundle key: {@code semantic.unresolved.map.key.type}.
     *
     * @param type the name of the unresolved type
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedMapKeyType(String type, String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.map.key.type", type, field);
    }

    /**
     * Returns a message stating that the value type of a map field cannot be resolved.
     *
     * <p>Bundle key: {@code semantic.unresolved.map.value.type}.
     *
     * @param type the name of the unresolved type
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedMapValueType(String type, String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.map.value.type", type, field);
    }

    /**
     * Returns a message stating that a custom option is not declared by any visible extension.
     *
     * <p>Bundle key: {@code semantic.unknown.custom.option}.
     *
     * @param name the name of the custom option
     * @param options the qualified name of the options message being extended
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unknownCustomOption(String name, String options) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unknown.custom.option", name, options);
    }

    /**
     * Returns a message stating that an option is not defined for its declaration.
     *
     * <p>Bundle key: {@code semantic.unknown.option}.
     *
     * @param name the name of the option
     * @param options the qualified name of the options message of the declaration
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unknownOption(String name, String options) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unknown.option", name, options);
    }

    /**
     * Returns a message stating that a segment of an option name does not reference a field of the
     * preceding segment.
     *
     * <p>Bundle key: {@code semantic.unresolved.option.field}.
     *
     * @param field the name of the unresolved segment
     * @param option the full name of the option
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedOptionField(String field, String option) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.option.field", field, option);
    }

    /**
     * Returns a message stating that the value of an option does not match the type of the option.
     *
     * <p>Bundle key: {@code semantic.option.value.type.mismatch}.
     *
     * @param option the name of the option
     * @param type the type of the option
     * @param value the value of the option, as written in the source
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionValueTypeMismatch(String option, ProtobufType type, String value) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.option.value.type.mismatch", option, typeName(type), value);
    }

    /**
     * Returns a message stating that the value of an enum option references a constant that does
     * not exist.
     *
     * <p>Bundle key: {@code semantic.option.value.unknown.enum.constant}.
     *
     * @param constant the name of the referenced constant
     * @param enumName the name of the enum
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionValueUnknownEnumConstant(String constant, String enumName) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.option.value.unknown.enum.constant", constant, enumName);
    }

    /**
     * Returns a message stating that the value of an option is out of the range of its type.
     *
     * <p>Bundle key: {@code semantic.option.value.out.of.range}.
     *
     * @param value the value of the option
     * @param option the name of the option
     * @param type the type of the option
     * @param min the smallest valid value, rendered as text
     * @param max the largest valid value, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionValueOutOfRange(long value, String option, ProtobufType type, String min, String max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.option.value.out.of.range", value, option, typeName(type), min, max);
    }

    /**
     * Returns a message stating that an option has a map type, which cannot be given a value.
     *
     * <p>Bundle key: {@code semantic.option.value.on.map.field}.
     *
     * @param option the name of the option
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionValueOnMapField(String option) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.option.value.on.map.field", option);
    }

    /**
     * Returns a message stating that a field of a proto2 message has no label.
     *
     * <p>Bundle key: {@code semantic.missing.label.in.proto2}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage missingLabelInProto2(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.missing.label.in.proto2", field);
    }

    /**
     * Returns a message stating that a field uses the {@code required} modifier in proto3.
     *
     * <p>Bundle key: {@code semantic.required.in.proto3}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage requiredInProto3(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.required.in.proto3", field);
    }

    /**
     * Returns a message stating that a field uses the {@code required} modifier in a file that uses
     * editions.
     *
     * <p>Bundle key: {@code semantic.required.in.editions}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage requiredInEditions(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.required.in.editions", field);
    }

    /**
     * Returns a message stating that a field uses the {@code optional} modifier in a file that uses
     * editions.
     *
     * <p>Bundle key: {@code semantic.optional.in.editions}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage optionalInEditions(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.optional.in.editions", field);
    }

    /**
     * Returns a message stating that a map field is declared inside a oneof.
     *
     * <p>Bundle key: {@code semantic.map.field.in.oneof}.
     *
     * @param field the name of the field
     * @param oneof the name of the oneof
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage mapFieldInOneof(String field, String oneof) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.map.field.in.oneof", field, oneof);
    }

    /**
     * Returns a message stating that a map field uses a modifier.
     *
     * <p>Bundle key: {@code semantic.map.field.with.modifier}.
     *
     * @param field the name of the field
     * @param modifier the modifier used by the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage mapFieldWithModifier(String field, String modifier) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.map.field.with.modifier", field, modifier);
    }

    /**
     * Returns a message stating that the key type of a map field is not an integral or string type.
     *
     * <p>Bundle key: {@code semantic.invalid.map.key.type}.
     *
     * @param field the name of the field
     * @param type the name of the key type
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMapKeyType(String field, String type) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.invalid.map.key.type", field, type);
    }

    /**
     * Returns a message stating that the value type of a map field is another map.
     *
     * <p>Bundle key: {@code semantic.nested.map.value}.
     *
     * @param field the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage nestedMapValue(String field) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.nested.map.value", field);
    }

    /**
     * Returns a message stating that the number of a field is smaller than the smallest valid field
     * number.
     *
     * <p>Bundle key: {@code semantic.field.number.too.small}.
     *
     * @param field the name of the field
     * @param number the number of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage fieldNumberTooSmall(String field, long number) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.field.number.too.small", field, number);
    }

    /**
     * Returns a message stating that the number of a field is larger than the largest valid field
     * number.
     *
     * <p>Bundle key: {@code semantic.field.number.too.large}.
     *
     * @param field the name of the field
     * @param number the number of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage fieldNumberTooLarge(String field, long number) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.field.number.too.large", field, number);
    }

    /**
     * Returns a message stating that the number of a field falls within the range reserved for the
     * Protocol Buffers implementation.
     *
     * <p>Bundle key: {@code semantic.field.number.in.implementation.range}.
     *
     * @param field the name of the field
     * @param number the number of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage fieldNumberInImplementationRange(String field, long number) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.field.number.in.implementation.range", field, number);
    }

    /**
     * Returns a message stating that the value of an enum constant is outside the {@code int32}
     * range.
     *
     * <p>Bundle key: {@code semantic.enum.value.out.of.range}.
     *
     * @param value the value of the constant
     * @param min the smallest valid value
     * @param max the largest valid value
     * @param enumName the name of the enum
     * @param constant the name of the constant
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage enumValueOutOfRange(long value, long min, long max, String enumName, String constant) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.enum.value.out.of.range", value, min, max, enumName, constant);
    }

    /**
     * Returns a message stating that a group is declared in a proto3 file.
     *
     * <p>Bundle key: {@code semantic.group.in.proto3}.
     *
     * @param name the name of the group
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage groupInProto3(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.group.in.proto3", name);
    }

    /**
     * Returns a message stating that a group is declared in a file that uses editions.
     *
     * <p>Bundle key: {@code semantic.group.in.editions}.
     *
     * @param name the name of the group
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage groupInEditions(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.group.in.editions", name);
    }

    /**
     * Returns a message stating that a oneof does not contain any field.
     *
     * <p>Bundle key: {@code semantic.empty.oneof}.
     *
     * @param name the name of the oneof
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage emptyOneof(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.empty.oneof", name);
    }

    /**
     * Returns a message stating that an enum does not contain any constant.
     *
     * <p>Bundle key: {@code semantic.empty.enum}.
     *
     * @param name the name of the enum
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage emptyEnum(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.empty.enum", name);
    }

    /**
     * Returns a message stating that the first constant of an open enum does not have the value
     * {@code 0}.
     *
     * <p>Bundle key: {@code semantic.open.enum.first.value.not.zero}.
     *
     * @param enumName the name of the enum
     * @param value the value of the first constant
     * @param constant the name of the first constant
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage openEnumFirstValueNotZero(String enumName, long value, String constant) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.open.enum.first.value.not.zero", enumName, value, constant);
    }

    /**
     * Returns a message stating that two constants of an enum have the same name.
     *
     * <p>Bundle key: {@code semantic.duplicate.enum.constant.name}.
     *
     * @param name the duplicated name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateEnumConstantName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.enum.constant.name", name);
    }

    /**
     * Returns a message stating that the name of an enum constant is reserved.
     *
     * <p>Bundle key: {@code semantic.reserved.enum.constant.name}.
     *
     * @param name the reserved name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedEnumConstantName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.enum.constant.name", name);
    }

    /**
     * Returns a message stating that the value of an enum constant is reserved.
     *
     * <p>Bundle key: {@code semantic.reserved.enum.constant.index}.
     *
     * @param value the reserved value
     * @param constant the name of the constant
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedEnumConstantIndex(long value, String constant) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.enum.constant.index", value, constant);
    }

    /**
     * Returns a message stating that two constants of an enum that does not allow aliases have the
     * same value.
     *
     * <p>Bundle key: {@code semantic.duplicate.enum.value}.
     *
     * @param enumName the name of the enum
     * @param value the duplicated value
     * @param constants the names of the constants that share the value, separated by commas
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateEnumValue(String enumName, long value, String constants) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.enum.value", enumName, value, constants);
    }

    /**
     * Returns a message stating that a name is reserved more than once.
     *
     * <p>Bundle key: {@code semantic.duplicate.reserved.name}.
     *
     * @param name the duplicated name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateReservedName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.reserved.name", name);
    }

    /**
     * Returns a message stating that a reserved number is smaller than the smallest valid number.
     *
     * <p>Bundle key: {@code semantic.reserved.number.too.small}.
     *
     * @param value the reserved number
     * @param min the smallest valid number
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedNumberTooSmall(long value, long min) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.number.too.small", value, min);
    }

    /**
     * Returns a message stating that a reserved number is larger than the largest valid number.
     *
     * <p>Bundle key: {@code semantic.reserved.number.too.large}.
     *
     * @param value the reserved number
     * @param max the largest valid number
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedNumberTooLarge(long value, long max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.number.too.large", value, max);
    }

    /**
     * Returns a message stating that the start of a reserved range is greater than its end.
     *
     * <p>Bundle key: {@code semantic.invalid.reserved.range}.
     *
     * @param min the start of the range
     * @param max the end of the range
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidReservedRange(long min, long max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.invalid.reserved.range", min, max);
    }

    /**
     * Returns a message stating that two reserved ranges overlap.
     *
     * <p>Bundle key: {@code semantic.reserved.ranges.overlap}.
     *
     * @param range the overlapping reserved number or range, rendered as text
     * @param previousRange the range reserved earlier that it overlaps, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage reservedRangesOverlap(String range, String previousRange) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.reserved.ranges.overlap", range, previousRange);
    }

    /**
     * Returns a message stating that a reserved declaration contains both numbers and names.
     *
     * <p>Bundle key: {@code semantic.mixed.reserved.numbers.and.names}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage mixedReservedNumbersAndNames() {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.mixed.reserved.numbers.and.names");
    }

    /**
     * Returns a message stating that an extension range is declared in a proto3 file.
     *
     * <p>Bundle key: {@code semantic.extensions.in.proto3}.
     *
     * @param message the name of the message that declares the extension range
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionsInProto3(String message) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extensions.in.proto3", message);
    }

    /**
     * Returns a message stating that an extension number is smaller than the smallest valid field
     * number.
     *
     * <p>Bundle key: {@code semantic.extension.number.too.small}.
     *
     * @param value the extension number
     * @param min the smallest valid field number
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionNumberTooSmall(long value, long min) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.number.too.small", value, min);
    }

    /**
     * Returns a message stating that an extension number is larger than the largest valid field
     * number.
     *
     * <p>Bundle key: {@code semantic.extension.number.too.large}.
     *
     * @param value the extension number
     * @param max the largest valid field number
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionNumberTooLarge(long value, long max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.number.too.large", value, max);
    }

    /**
     * Returns a message stating that the start of an extension range is greater than its end.
     *
     * <p>Bundle key: {@code semantic.invalid.extension.range}.
     *
     * @param min the start of the range
     * @param max the end of the range
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidExtensionRange(long min, long max) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.invalid.extension.range", min, max);
    }

    /**
     * Returns a message stating that two extension ranges overlap.
     *
     * <p>Bundle key: {@code semantic.extension.ranges.overlap}.
     *
     * @param range the overlapping extension number or range, rendered as text
     * @param previousRange the extension range declared earlier that it overlaps, rendered as text
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage extensionRangesOverlap(String range, String previousRange) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.extension.ranges.overlap", range, previousRange);
    }

    /**
     * Returns a message stating that two methods of a service have the same name.
     *
     * <p>Bundle key: {@code semantic.duplicate.method.name}.
     *
     * @param method the duplicated name
     * @param service the name of the service
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage duplicateMethodName(String method, String service) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.duplicate.method.name", method, service);
    }

    /**
     * Returns a message stating that the input or output type of an rpc method cannot be
     * resolved.
     *
     * <p>Bundle key: {@code semantic.unresolved.method.type}.
     *
     * @param type the name of the unresolved type
     * @param method the name of the method
     * @param service the name of the service
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unresolvedMethodType(String type, String method, String service) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.unresolved.method.type", type, method, service);
    }

    /**
     * Returns a message stating that the input type of an rpc method is an enum.
     *
     * <p>Bundle key: {@code semantic.enum.method.input}.
     *
     * @param method the name of the method
     * @param service the name of the service
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage enumMethodInput(String method, String service) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.enum.method.input", method, service);
    }

    /**
     * Returns a message stating that the output type of an rpc method is an enum.
     *
     * <p>Bundle key: {@code semantic.enum.method.output}.
     *
     * @param method the name of the method
     * @param service the name of the service
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage enumMethodOutput(String method, String service) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.enum.method.output", method, service);
    }

    /**
     * Returns a message stating that a segment of a package name is not written in
     * lower_snake_case.
     *
     * <p>Bundle key: {@code semantic.package.segment.not.snake.case}.
     *
     * @param segment the segment of the package name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage packageSegmentNotSnakeCase(String segment) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.package.segment.not.snake.case", segment);
    }

    /**
     * Returns a message stating that the name of a message is not written in TitleCase.
     *
     * <p>Bundle key: {@code semantic.message.name.not.title.case}.
     *
     * @param name the name of the message
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage messageNameNotTitleCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.message.name.not.title.case", name);
    }

    /**
     * Returns a message stating that the name of an enum is not written in TitleCase.
     *
     * <p>Bundle key: {@code semantic.enum.name.not.title.case}.
     *
     * @param name the name of the enum
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage enumNameNotTitleCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.enum.name.not.title.case", name);
    }

    /**
     * Returns a message stating that the name of a service is not written in TitleCase.
     *
     * <p>Bundle key: {@code semantic.service.name.not.title.case}.
     *
     * @param name the name of the service
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage serviceNameNotTitleCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.service.name.not.title.case", name);
    }

    /**
     * Returns a message stating that the name of an rpc method is not written in TitleCase.
     *
     * <p>Bundle key: {@code semantic.method.name.not.title.case}.
     *
     * @param name the name of the method
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage methodNameNotTitleCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.method.name.not.title.case", name);
    }

    /**
     * Returns a message stating that the name of a field is not written in lower_snake_case.
     *
     * <p>Bundle key: {@code semantic.field.name.not.snake.case}.
     *
     * @param name the name of the field
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage fieldNameNotSnakeCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.field.name.not.snake.case", name);
    }

    /**
     * Returns a message stating that the name of a oneof is not written in lower_snake_case.
     *
     * <p>Bundle key: {@code semantic.oneof.name.not.snake.case}.
     *
     * @param name the name of the oneof
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage oneofNameNotSnakeCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.oneof.name.not.snake.case", name);
    }

    /**
     * Returns a message stating that the name of an enum constant is not written in
     * UPPER_SNAKE_CASE.
     *
     * <p>Bundle key: {@code semantic.enum.value.not.upper.snake.case}.
     *
     * @param name the name of the enum constant
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage enumValueNotUpperSnakeCase(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SEMANTIC, "semantic.enum.value.not.upper.snake.case", name);
    }

    /**
     * Returns the name of the given type as it is written in a Protocol Buffer definition.
     *
     * @param type the type
     * @return the lowercase name of the type
     */
    private static String typeName(ProtobufType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
