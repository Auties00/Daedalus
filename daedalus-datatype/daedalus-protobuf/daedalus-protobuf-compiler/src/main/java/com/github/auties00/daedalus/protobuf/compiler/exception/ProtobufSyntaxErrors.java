package com.github.auties00.daedalus.protobuf.compiler.exception;

import com.github.auties00.daedalus.protobuf.compiler.exception.ProtobufDiagnosticMessage.Kind;

/**
 * Static factory methods for the {@linkplain Kind#SYNTAX syntax} diagnostic messages reported
 * while matching the tokens of a Protocol Buffer definition against the grammar of the language.
 *
 * <p>Parameters named {@code found} receive the text of the offending token exactly as it appears
 * in the source. Each method documents the key of the {@code diagnostics} resource bundle entry that
 * holds its text.
 *
 * @see ProtobufLexicalErrors
 * @see ProtobufSemanticErrors
 */
public final class ProtobufSyntaxErrors {
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private ProtobufSyntaxErrors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a message stating that the input ended before the current declaration was complete.
     *
     * <p>Bundle key: {@code syntax.unexpected.end.of.input}.
     *
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unexpectedEndOfInput() {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unexpected.end.of.input");
    }

    /**
     * Returns a message stating that a specific token was expected but a different one was found.
     *
     * <p>Bundle key: {@code syntax.expected.token}.
     *
     * @param expected the text of the expected token
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedToken(String expected, String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.token", expected, found);
    }

    /**
     * Returns a message stating that a token cannot appear at its position.
     *
     * <p>Bundle key: {@code syntax.unexpected.token}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unexpectedToken(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unexpected.token", found);
    }

    /**
     * Returns a message stating that either a {@code ,} or a {@code ;} was expected.
     *
     * <p>Bundle key: {@code syntax.expected.separator.or.semicolon}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSeparatorOrSemicolon(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.separator.or.semicolon", found);
    }

    /**
     * Returns a message stating that a token cannot appear at the top level of a file.
     *
     * <p>Bundle key: {@code syntax.unexpected.top.level.token}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unexpectedTopLevelToken(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unexpected.top.level.token", found);
    }

    /**
     * Returns a message stating that a visibility modifier is not followed by a message or an enum.
     *
     * <p>Bundle key: {@code syntax.expected.message.or.enum.after.visibility}.
     *
     * @param modifier the visibility modifier, such as {@code export}
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMessageOrEnumAfterVisibility(String modifier, String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.message.or.enum.after.visibility", modifier, found);
    }

    /**
     * Returns a message stating that a package name is not a valid qualified identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.package.name}.
     *
     * @param name the invalid package name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidPackageName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.package.name", name);
    }

    /**
     * Returns a message stating that a package declaration is not terminated by a semicolon.
     *
     * <p>Bundle key: {@code syntax.expected.semicolon.after.package}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSemicolonAfterPackage(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.semicolon.after.package", found);
    }

    /**
     * Returns a message stating that the {@code syntax} keyword is not followed by {@code =}.
     *
     * <p>Bundle key: {@code syntax.expected.assignment.after.syntax}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedAssignmentAfterSyntax(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.assignment.after.syntax", found);
    }

    /**
     * Returns a message stating that the version of a syntax declaration is not a string literal.
     *
     * <p>Bundle key: {@code syntax.expected.syntax.literal}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSyntaxLiteral(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.syntax.literal", found);
    }

    /**
     * Returns a message stating that a syntax declaration names an unknown version.
     *
     * <p>Bundle key: {@code syntax.unknown.syntax.version}.
     *
     * @param version the unknown version
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unknownSyntaxVersion(String version) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unknown.syntax.version", version);
    }

    /**
     * Returns a message stating that a syntax declaration is not terminated by a semicolon.
     *
     * <p>Bundle key: {@code syntax.expected.semicolon.after.syntax}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSemicolonAfterSyntax(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.semicolon.after.syntax", found);
    }

    /**
     * Returns a message stating that the {@code edition} keyword is not followed by {@code =}.
     *
     * <p>Bundle key: {@code syntax.expected.assignment.after.edition}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedAssignmentAfterEdition(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.assignment.after.edition", found);
    }

    /**
     * Returns a message stating that the value of an edition declaration is not a string literal.
     *
     * <p>Bundle key: {@code syntax.expected.edition.literal}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedEditionLiteral(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.edition.literal", found);
    }

    /**
     * Returns a message stating that an edition declaration names an unknown edition.
     *
     * <p>Bundle key: {@code syntax.unknown.edition}.
     *
     * @param edition the unknown edition
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unknownEdition(String edition) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unknown.edition", edition);
    }

    /**
     * Returns a message stating that an edition declaration is not terminated by a semicolon.
     *
     * <p>Bundle key: {@code syntax.expected.semicolon.after.edition}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSemicolonAfterEdition(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.semicolon.after.edition", found);
    }

    /**
     * Returns a message stating that an option declaration is not terminated by a semicolon.
     *
     * <p>Bundle key: {@code syntax.expected.semicolon.after.option}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedSemicolonAfterOption(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.semicolon.after.option", found);
    }

    /**
     * Returns a message stating that the name of a custom option is not closed by {@code )}.
     *
     * <p>Bundle key: {@code syntax.expected.extension.name.end}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedExtensionNameEnd(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.extension.name.end", found);
    }

    /**
     * Returns a message stating that the name of an option is not followed by {@code =}.
     *
     * <p>Bundle key: {@code syntax.expected.assignment.after.option.name}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedAssignmentAfterOptionName(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.assignment.after.option.name", found);
    }

    /**
     * Returns a message stating that the name of a message is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.message.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMessageName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.message.name", name);
    }

    /**
     * Returns a message stating that the body of a message does not start with an opening brace.
     *
     * <p>Bundle key: {@code syntax.expected.message.body.start}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMessageBodyStart(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.message.body.start", found);
    }

    /**
     * Returns a message stating that the target of an extend declaration is not a valid type name.
     *
     * <p>Bundle key: {@code syntax.invalid.extend.target}.
     *
     * @param name the invalid type name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidExtendTarget(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.extend.target", name);
    }

    /**
     * Returns a message stating that the body of an extend declaration does not start with an opening
     * brace.
     *
     * <p>Bundle key: {@code syntax.expected.extend.body.start}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedExtendBodyStart(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.extend.body.start", found);
    }

    /**
     * Returns a message stating that the key type of a map is not a valid type name.
     *
     * <p>Bundle key: {@code syntax.invalid.map.key.type}.
     *
     * @param type the invalid type name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMapKeyType(String type) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.map.key.type", type);
    }

    /**
     * Returns a message stating that the key and value types of a map are not separated by
     * {@code ,}.
     *
     * <p>Bundle key: {@code syntax.expected.map.type.separator}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMapTypeSeparator(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.map.type.separator", found);
    }

    /**
     * Returns a message stating that the value type of a map is not a valid type name.
     *
     * <p>Bundle key: {@code syntax.invalid.map.value.type}.
     *
     * @param type the invalid type name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMapValueType(String type) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.map.value.type", type);
    }

    /**
     * Returns a message stating that the type parameters of a map are not closed by {@code >}.
     *
     * <p>Bundle key: {@code syntax.expected.map.type.end}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMapTypeEnd(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.map.type.end", found);
    }

    /**
     * Returns a message stating that the name of a field is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.field.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidFieldName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.field.name", name);
    }

    /**
     * Returns a message stating that the name of a field is not followed by {@code =}.
     *
     * <p>Bundle key: {@code syntax.expected.assignment.after.field.name}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedAssignmentAfterFieldName(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.assignment.after.field.name", found);
    }

    /**
     * Returns a message stating that the number of a field is not an integer.
     *
     * <p>Bundle key: {@code syntax.expected.field.number}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedFieldNumber(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.field.number", found);
    }

    /**
     * Returns a message stating that the name of an enum is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.enum.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidEnumName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.enum.name", name);
    }

    /**
     * Returns a message stating that the value of an enum constant is not an integer.
     *
     * <p>Bundle key: {@code syntax.expected.enum.constant.number}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedEnumConstantNumber(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.enum.constant.number", found);
    }

    /**
     * Returns a message stating that an integer was expected in an extensions or reserved
     * declaration.
     *
     * <p>Bundle key: {@code syntax.expected.integer.value}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedIntegerValue(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.integer.value", found);
    }

    /**
     * Returns a message stating that the upper bound of a range is neither an integer nor
     * {@code max}.
     *
     * <p>Bundle key: {@code syntax.expected.range.end}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedRangeEnd(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.range.end", found);
    }

    /**
     * Returns a message stating that a reserved declaration contains something other than a field
     * number, a range or a name.
     *
     * <p>Bundle key: {@code syntax.expected.reserved.value}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedReservedValue(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.reserved.value", found);
    }

    /**
     * Returns a message stating that the name of a service is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.service.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidServiceName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.service.name", name);
    }

    /**
     * Returns a message stating that a token cannot appear in the body of a service.
     *
     * <p>Bundle key: {@code syntax.unexpected.service.token}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unexpectedServiceToken(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unexpected.service.token", found);
    }

    /**
     * Returns a message stating that the name of an rpc method is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.method.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMethodName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.method.name", name);
    }

    /**
     * Returns a message stating that the input type of an rpc method is not followed by
     * {@code returns}.
     *
     * <p>Bundle key: {@code syntax.expected.returns}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedReturns(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.returns", found);
    }

    /**
     * Returns a message stating that a token cannot appear in the body of an rpc method.
     *
     * <p>Bundle key: {@code syntax.unexpected.method.body.token}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unexpectedMethodBodyToken(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unexpected.method.body.token", found);
    }

    /**
     * Returns a message stating that an rpc method is followed by neither a body nor a semicolon.
     *
     * <p>Bundle key: {@code syntax.expected.method.body.or.semicolon}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMethodBodyOrSemicolon(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.method.body.or.semicolon", found);
    }

    /**
     * Returns a message stating that the input or output type of an rpc method is not a valid type
     * name.
     *
     * <p>Bundle key: {@code syntax.invalid.method.type}.
     *
     * @param type the invalid type name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidMethodType(String type) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.method.type", type);
    }

    /**
     * Returns a message stating that the name of a oneof is not a valid identifier.
     *
     * <p>Bundle key: {@code syntax.invalid.oneof.name}.
     *
     * @param name the invalid name
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidOneofName(String name) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.oneof.name", name);
    }

    /**
     * Returns a message stating that an import declaration uses an unknown modifier.
     *
     * <p>Bundle key: {@code syntax.unknown.import.modifier}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage unknownImportModifier(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.unknown.import.modifier", found);
    }

    /**
     * Returns a message stating that the location of an import declaration is not a string
     * literal.
     *
     * <p>Bundle key: {@code syntax.expected.import.location}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedImportLocation(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.import.location", found);
    }

    /**
     * Returns a message stating that a message literal contains something other than a field name
     * where a key was expected.
     *
     * <p>Bundle key: {@code syntax.expected.message.literal.key}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage expectedMessageLiteralKey(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.expected.message.literal.key", found);
    }

    /**
     * Returns a message stating that a token is not a valid option value.
     *
     * <p>Bundle key: {@code syntax.invalid.option.value}.
     *
     * @param found the text of the offending token
     * @return the diagnostic message
     */
    public static ProtobufDiagnosticMessage invalidOptionValue(String found) {
        return ProtobufDiagnosticMessage.of(Kind.SYNTAX, "syntax.invalid.option.value", found);
    }
}
