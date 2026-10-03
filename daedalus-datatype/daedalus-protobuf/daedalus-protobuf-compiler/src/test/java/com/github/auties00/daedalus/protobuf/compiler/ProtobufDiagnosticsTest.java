package com.github.auties00.daedalus.protobuf.compiler;

import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufPrimitiveTypeReference;
import com.github.auties00.daedalus.protobuf.compiler.exception.*;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufTreeSourcePosition;
import com.github.auties00.daedalus.protobuf.model.ProtobufType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ProtobufDiagnosticsTest {
    private static List<ProtobufDiagnostic> diagnosticsOf(String proto) {
        var exception = assertThrows(ProtobufParserException.class, () -> ProtobufParser.parseOnly(proto));
        return exception.diagnostics();
    }

    private static List<ProtobufDiagnosticMessage.Kind> kindsOf(List<ProtobufDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(diagnostic -> diagnostic.message().kind())
                .toList();
    }

    @Nested
    class RecoveryTests {
        @Test
        public void testIndependentSyntaxErrorsAreAllReported() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                    }
                    message B {
                      string y 2;
                    }
                    """);
            assertEquals(List.of(ProtobufDiagnosticMessage.Kind.SYNTAX, ProtobufDiagnosticMessage.Kind.SYNTAX), kindsOf(diagnostics));
            assertEquals(3, diagnostics.get(0).range().start().line());
            assertEquals(6, diagnostics.get(1).range().start().line());
        }

        @Test
        public void testMissingSemicolonBeforeClosingBraceDoesNotCascade() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = 1
                    }
                    message B {
                      int32 y = 1;
                    }
                    """);
            assertEquals(1, diagnostics.size());
            assertEquals("syntax.expected.token", diagnostics.getFirst().message().key());
        }

        @Test
        public void testMissingSemicolonBeforeKeywordDoesNotCascade() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = 1
                      message Nested {
                        int32 y = 1;
                      }
                    }
                    """);
            assertEquals(1, diagnostics.size());
        }

        @Test
        public void testMissingSemicolonResumesAtNextLine() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message User {
                      string name = 1
                      int32 age = ;
                    }
                    """);
            assertEquals(2, diagnostics.size());
            assertEquals(new ProtobufTreeSourcePosition(3, 18), diagnostics.get(0).range().start());
            assertEquals(4, diagnostics.get(1).range().start().line());
        }

        @Test
        public void testMalformedTopLevelTokenDoesNotLoop() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    }
                    reserved 1;
                    message A {
                      int32 x = 1;
                    }
                    """);
            assertEquals(2, diagnostics.size());
            assertTrue(diagnostics.stream().allMatch(diagnostic -> diagnostic.message().key().equals("syntax.unexpected.top.level.token")));
        }

        @Test
        public void testBrokenMessageKeepsValidSiblings() throws IOException {
            var diagnostics = new ProtobufDiagnosticator();
            var document = ProtobufParser.doParse(null, new StringReader("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                      int32 y = 2;
                    }
                    message 1B {
                      int32 z = 1;
                    }
                    message C {
                      int32 w = 1;
                    }
                    """), diagnostics);
            assertTrue(diagnostics.hasErrors());
            var messages = document.getDirectChildrenByType(ProtobufMessageStatement.class)
                    .map(ProtobufMessageStatement::name)
                    .toList();
            assertEquals(List.of("A", "C"), messages);
            var a = document.getDirectChildByNameAndType("A", ProtobufMessageStatement.class).orElseThrow();
            assertEquals(1, a.children().size());
        }

        @Test
        public void testUnexpectedEndOfInputIsReportedOnce() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      message B {
                        int32 x = 1;
                    """);
            assertEquals(1, diagnostics.size());
            assertEquals("syntax.unexpected.end.of.input", diagnostics.getFirst().message().key());
        }

        @Test
        public void testUnterminatedStringAndSyntaxErrorAreBothReported() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    option java_package = "abc
                    ;
                    message A {
                      int32 x = ;
                    }
                    """);
            assertEquals(List.of(ProtobufDiagnosticMessage.Kind.LEXICAL, ProtobufDiagnosticMessage.Kind.SYNTAX), kindsOf(diagnostics));
        }
    }

    @Nested
    class SemanticTests {
        @Test
        public void testSemanticErrorsInDifferentMessagesAreAllReported() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = 1;
                      int32 y = 1;
                    }
                    message B {
                      string s = 1;
                      string s = 2;
                    }
                    """);
            assertEquals(List.of("semantic.duplicate.field.index", "semantic.duplicate.field.name"),
                    diagnostics.stream().map(diagnostic -> diagnostic.message().key()).toList());
        }

        @Test
        public void testUnresolvedTypeWithDefaultValueIsReportedOnce() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto2";
                    message A {
                      optional Missing x = 1 [default = 5];
                    }
                    """);
            assertEquals(1, diagnostics.size());
            assertEquals("semantic.unresolved.field.type.nested", diagnostics.getFirst().message().key());
        }

        @Test
        public void testUnresolvedMapTypeIsReportedOnce() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      map<string, Missing> x = 1;
                    }
                    """);
            assertEquals(1, diagnostics.size());
        }

        @Test
        public void testUnresolvedMethodTypesAreReported() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message Request {}
                    service S {
                      rpc Fetch (Request) returns (MissingResponse);
                      rpc Push (stream MissingRequest) returns (Request);
                    }
                    """);
            assertEquals(List.of("semantic.unresolved.method.type", "semantic.unresolved.method.type"),
                    diagnostics.stream().map(diagnostic -> diagnostic.message().key()).toList());
            assertEquals(4, diagnostics.get(0).range().start().line());
            assertEquals(5, diagnostics.get(1).range().start().line());
        }

        @Test
        public void testSyntaxErrorsSkipSemanticAnalysis() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                      Missing y = 2;
                    }
                    """);
            assertEquals(List.of(ProtobufDiagnosticMessage.Kind.SYNTAX), kindsOf(diagnostics));
        }

        @Test
        public void testImportersOfBrokenFilesAreNotAnalyzed(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("b.proto"), """
                    syntax = "proto3";
                    message B {
                      int32 x = ;
                    }
                    """);
            Files.writeString(tempDir.resolve("a.proto"), """
                    syntax = "proto3";
                    import "b.proto";
                    message A {
                      B b = 1;
                      Missing m = 2;
                    }
                    """);
            Files.writeString(tempDir.resolve("c.proto"), """
                    syntax = "proto3";
                    message C {
                      Missing m = 1;
                    }
                    """);
            var exception = assertThrows(ProtobufParserException.class, () -> ProtobufParser.parse(tempDir));
            var files = exception.diagnostics()
                    .stream()
                    .map(diagnostic -> diagnostic.location().getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
            assertEquals(Set.of("b.proto", "c.proto"), files);
            assertEquals(2, exception.diagnostics().size());
        }
    }

    @Nested
    class PositionTests {
        @Test
        public void testPositionPointsAtOffendingToken() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                    }
                    """);
            assertEquals(1, diagnostics.size());
            assertEquals(new ProtobufTreeSourcePosition(3, 13), diagnostics.getFirst().range().start());
        }

        @Test
        public void testConcatenatedStringStartsAtFirstFragment() throws IOException {
            var lexer = new ProtobufLexer(new StringReader("\"a\"\n  \"b\""));
            assertEquals("ab", lexer.nextRawToken());
            assertEquals(new ProtobufTreeSourcePosition(1, 1), lexer.tokenRange().start());
            assertEquals(new ProtobufTreeSourcePosition(2, 6), lexer.tokenRange().end());
        }

        @Test
        public void testRenderedFormat() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                    }
                    """);
            var rendered = diagnostics.getFirst().format(Locale.ROOT);
            assertTrue(rendered.startsWith("<input>:3:13: syntax error: Expected a field number but found ';'"), rendered);
        }

        @Test
        public void testPercentInTokenDoesNotBreakFormatting() {
            var exception = assertThrows(ProtobufParserException.class, () -> ProtobufParser.parseOnly("message %s { }"));
            assertTrue(exception.getMessage().contains("Invalid message name '%'"), exception.getMessage());
        }
    }

    @Nested
    class ContextTests {
        private static List<String> headlinesOf(String proto) {
            return diagnosticsOf(proto)
                    .stream()
                    .map(diagnostic -> diagnostic.message().format(Locale.ROOT).split("\n", 2)[0])
                    .toList();
        }

        @Test
        public void testDuplicateFieldNumberNamesTheOtherField() {
            var headlines = headlinesOf("""
                    syntax = "proto3";
                    message A {
                      int32 first = 1;
                      int32 second = 1;
                    }
                    """);
            assertEquals(List.of("Field number 1 of 'second' is already used by field 'first'"), headlines);
        }

        @Test
        public void testReservedFieldNumberIsReportedAtTheField() {
            var diagnostics = diagnosticsOf("""
                    syntax = "proto3";
                    message A {
                      reserved 2 to 5;
                      int32 value = 3;
                    }
                    """);
            assertEquals(1, diagnostics.size());
            assertEquals(4, diagnostics.getFirst().range().start().line());
            assertEquals("Field 'value' uses field number 3, which is reserved", diagnostics.getFirst().message().format(Locale.ROOT).split("\n", 2)[0]);
        }

        @Test
        public void testOverlappingRangesAreNamed() {
            var headlines = headlinesOf("""
                    syntax = "proto2";
                    message A {
                      reserved 10 to 20;
                      reserved 15;
                      extensions 100 to max;
                      extensions 150 to 160;
                    }
                    """);
            assertEquals(List.of(
                    "Reserved numbers 15 overlap the numbers 10 to 20 that are already reserved",
                    "Extension range 150 to 160 overlaps the extension range 100 to max of this message"
            ), headlines);
        }

        @Test
        public void testDuplicateEnumValueListsTheConstants() {
            var headlines = headlinesOf("""
                    syntax = "proto3";
                    enum Status {
                      UNKNOWN = 0;
                      ACTIVE = 1;
                      ENABLED = 1;
                    }
                    """);
            assertEquals(List.of("Value 1 is used by more than one constant of enum 'Status': ACTIVE, ENABLED"), headlines);
        }

        @Test
        public void testExtensionOutsideRangesListsTheRanges() {
            var headlines = headlinesOf("""
                    syntax = "proto2";
                    message A {
                      extensions 100 to 199, 500;
                    }
                    message B {
                      optional int32 x = 1;
                    }
                    extend A {
                      optional int32 a = 300;
                    }
                    extend B {
                      optional int32 b = 100;
                    }
                    """);
            assertEquals(List.of(
                    "Extension field 'a' uses number 300, which is outside of the extension ranges of 'A'",
                    "Extension field 'b' extends 'B', which declares no extension ranges"
            ), headlines);
        }

        @Test
        public void testTypeMismatchQuotesTheValue() {
            var headlines = headlinesOf("""
                    syntax = "proto2";
                    message A {
                      optional int32 retries = 1 [default = "three"];
                    }
                    """);
            assertEquals(List.of("The default value \"three\" of field 'retries' is not a valid int32 value"), headlines);
        }

        @Test
        public void testDiagnosticsAreSeparatedByAnEmptyLine() {
            var exception = assertThrows(ProtobufParserException.class, () -> ProtobufParser.parseOnly("""
                    syntax = "proto3";
                    message A {
                      int32 x = ;
                      int32 y = ;
                    }
                    """));
            var rendered = exception.format(Locale.ROOT);
            var first = exception.diagnostics().get(0).format(Locale.ROOT);
            var second = exception.diagnostics().get(1).format(Locale.ROOT);
            assertEquals(first + "\n\n" + second + "\n\n2 errors", rendered);
        }
    }

    @Nested
    class LimitTests {
        @Test
        public void testNoErrorCap() {
            var proto = "foo;\n".repeat(500);
            var diagnostics = diagnosticsOf(proto);
            assertEquals(500, diagnostics.size());
            assertEquals(500, diagnostics.getLast().range().start().line());
        }
    }

    @Nested
    class LocalizationTests {
        private static final String BUNDLE_NAME = "com.github.auties00.daedalus.protobuf.compiler.exception.diagnostics";
        private static final List<Class<?>> FACTORIES = List.of(ProtobufLexicalErrors.class, ProtobufSyntaxErrors.class, ProtobufSemanticErrors.class);

        private static Object dummyArgument(Class<?> type, boolean nullStrings) {
            if (type == String.class) {
                return nullStrings ? null : "x";
            } else if (type == int.class) {
                return 1;
            } else if (type == long.class) {
                return 1L;
            } else if (type == ProtobufType.class) {
                return ProtobufType.INT32;
            } else if (type == ProtobufTypeReference.class) {
                return new ProtobufPrimitiveTypeReference(ProtobufType.INT32);
            } else {
                throw new AssertionError("Unsupported factory parameter type: " + type);
            }
        }

        private static List<ProtobufDiagnosticMessage> allMessages() throws InvocationTargetException, IllegalAccessException {
            var results = new ArrayList<ProtobufDiagnosticMessage>();
            for (var factory : FACTORIES) {
                for (var method : factory.getDeclaredMethods()) {
                    if (!Modifier.isPublic(method.getModifiers()) || method.getReturnType() != ProtobufDiagnosticMessage.class) {
                        continue;
                    }
                    results.add(invoke(method, false));
                    results.add(invoke(method, true));
                }
            }
            return results;
        }

        private static ProtobufDiagnosticMessage invoke(Method method, boolean nullStrings) throws InvocationTargetException, IllegalAccessException {
            var parameters = method.getParameterTypes();
            var args = new Object[parameters.length];
            for (var i = 0; i < parameters.length; i++) {
                args[i] = dummyArgument(parameters[i], nullStrings);
            }
            return (ProtobufDiagnosticMessage) method.invoke(null, args);
        }

        @Test
        public void testEveryFactoryKeyExistsAndFormats() throws InvocationTargetException, IllegalAccessException {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
            for (var message : allMessages()) {
                assertTrue(bundle.containsKey(message.key()), "Missing bundle key: " + message.key());
                assertDoesNotThrow(() -> message.format(Locale.ROOT), message.key());
            }
        }

        @Test
        public void testEveryBundleKeyIsUsed() throws InvocationTargetException, IllegalAccessException {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
            var used = allMessages().stream()
                    .map(ProtobufDiagnosticMessage::key)
                    .collect(Collectors.toSet());
            for (var key : bundle.keySet()) {
                if (key.startsWith("diagnostic.")) {
                    continue;
                }
                var baseKey = key.endsWith(".help") ? key.substring(0, key.length() - ".help".length()) : key;
                assertTrue(used.contains(baseKey), "Unused bundle key: " + key);
            }
        }

        @Test
        public void testEveryMessageHasHelp() {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
            for (var key : bundle.keySet()) {
                if (key.startsWith("diagnostic.") || key.endsWith(".help")) {
                    continue;
                }
                assertTrue(bundle.containsKey(key + ".help"), "Missing help for bundle key: " + key);
            }
        }

        @Test
        public void testHeadlinesDoNotEndWithPeriod() {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
            for (var key : bundle.keySet()) {
                if (key.startsWith("diagnostic.") || key.endsWith(".help")) {
                    continue;
                }
                var headline = bundle.getString(key).split("\n", 2)[0];
                assertFalse(headline.endsWith("."), "Headline ends with a period in bundle key: " + key);
            }
        }

        @Test
        public void testBundleQuotesAreBalanced() {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
            for (var key : bundle.keySet()) {
                var value = bundle.getString(key)
                        .replace("''", "")
                        .replace("'{'", "")
                        .replace("'}'", "");
                assertFalse(value.contains("'"), "Unescaped apostrophe in bundle key: " + key);
            }
        }

        @Test
        public void testBracesAreRendered() {
            var text = ProtobufSyntaxErrors.expectedMessageBodyStart("x").format(Locale.ROOT);
            assertTrue(text.startsWith("Expected '{' to start the body of the message but found 'x'"), text);
            assertTrue(text.contains("message Person {"), text);
        }

        @Test
        public void testMissingLocaleFallsBackToRootBundle() {
            var message = ProtobufSemanticErrors.duplicateFieldIndex("x", 1000, "y");
            assertEquals(message.format(Locale.ROOT), message.format(Locale.JAPANESE));
            assertTrue(message.format(Locale.ROOT).startsWith("Field number 1000 of 'x' is already used by field 'y'"), message.format(Locale.ROOT));
        }

        @Test
        public void testMessagesAreOnlyCreatedByFactories() throws IOException {
            var root = Path.of("src/main/java");
            var factories = Set.of("ProtobufLexicalErrors.java", "ProtobufSyntaxErrors.java", "ProtobufSemanticErrors.java", "ProtobufDiagnosticMessage.java");
            try (var files = Files.walk(root)) {
                var offenders = files.filter(file -> file.toString().endsWith(".java"))
                        .filter(file -> !factories.contains(file.getFileName().toString()))
                        .filter(file -> {
                            try {
                                var source = Files.readString(file);
                                return source.contains("new ProtobufDiagnosticMessage(") || source.contains("ProtobufDiagnosticMessage.of(");
                            } catch (IOException exception) {
                                throw new AssertionError(exception);
                            }
                        })
                        .toList();
                assertEquals(List.of(), offenders);
            }
        }
    }
}
