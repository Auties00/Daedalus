package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.protobuf.cli.model.EnumForm;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;
import com.github.auties00.daedalus.cli.tui.Theme;

/**
 * Generates syntax-highlighted code previews for each configuration option.
 *
 * <p>Uses a canonical {@code Person} message (with {@code name} and {@code age}
 * fields) and a canonical {@code Status} enum to demonstrate how each type form
 * and enum form looks in generated Java code.
 */
public final class CodePreview {

    private CodePreview() {
    }

    /**
     * Returns a syntax-highlighted code preview for the given message type form.
     *
     * @param form the type form to preview
     * @return the highlighted code string
     */
    public static String forTypeForm(TypeForm form) {
        return switch (form) {
            case RECORD -> highlightJava("""
                    @ProtobufMessage(protoName = "example.Person")
                    public record Person(
                        @ProtobufMessage.StringField(index = 1)
                        String name,
                        @ProtobufMessage.Int32Field(index = 2)
                        Integer age
                    ) {
                    }""");
            case CLASS -> highlightJava("""
                    @ProtobufMessage(protoName = "example.Person")
                    public final class Person {
                        @ProtobufMessage.StringField(index = 1)
                        final String name;
                        @ProtobufMessage.Int32Field(index = 2)
                        final Integer age;

                        public Person(String name, Integer age) { ... }

                        public Optional<String> name() { ... }
                        public OptionalInt age() { ... }
                    }""");
            case INTERFACE -> highlightJava("""
                    @ProtobufMessage(protoName = "example.Person")
                    public interface Person {
                        @ProtobufMessage.StringField(index = 1)
                        String name();
                        @ProtobufMessage.Int32Field(index = 2)
                        Integer age();
                    }""");
        };
    }

    /**
     * Returns a syntax-highlighted code preview for the given enum form.
     *
     * @param form the enum form to preview
     * @return the highlighted code string
     */
    public static String forEnumForm(EnumForm form) {
        return switch (form) {
            case JAVA_ENUM -> highlightJava("""
                    @ProtobufEnum(name = "example.Status")
                    public enum Status {
                        @ProtobufEnum.Constant(index = 0)
                        UNKNOWN,
                        @ProtobufEnum.Constant(index = 1)
                        ACTIVE,
                        @ProtobufEnum.Constant(index = 2)
                        INACTIVE
                    }""");
            case CLASS -> highlightJava("""
                    @ProtobufEnum(name = "example.Status")
                    public final class Status {
                        @ProtobufEnum.Constant(index = 0)
                        static final Status UNKNOWN = new Status(0);
                        @ProtobufEnum.Constant(index = 1)
                        static final Status ACTIVE = new Status(1);

                        private final int index;
                        Status(int index) { this.index = index; }
                    }""");
            case RECORD -> highlightJava("""
                    @ProtobufEnum(name = "example.Status")
                    public record Status(int index) {
                        @ProtobufEnum.Constant(index = 0)
                        static final Status UNKNOWN = new Status(0);
                        @ProtobufEnum.Constant(index = 1)
                        static final Status ACTIVE = new Status(1);
                    }""");
        };
    }

    /**
     * Returns a syntax-highlighted code preview for unknown fields support.
     *
     * @param enabled whether unknown fields are enabled
     * @return the highlighted code string
     */
    public static String forUnknownFields(boolean enabled) {
        if (enabled) {
            return highlightJava("""
                    @ProtobufUnknownFields
                    private Map<Integer, ProtobufUnknownValue> unknownFields;

                    @ProtobufUnknownFields.Setter
                    public void setUnknownField(int index, ProtobufUnknownValue value) {
                        ...
                    }""");
        }
        return Theme.MUTED_TEXT.render("  Unknown fields will be silently discarded\n  during deserialization.");
    }

    /**
     * Applies basic syntax highlighting to a Java code snippet.
     *
     * <p>Highlights annotations in amber, Java keywords in purple,
     * type names in blue, and string literals in green.
     *
     * @param code the raw Java code
     * @return the syntax-highlighted code
     */
    static String highlightJava(String code) {
        var sb = new StringBuilder();
        var lines = code.split("\n", -1);
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(highlightLine(line));
        }
        return sb.toString();
    }

    private static String highlightLine(String line) {
        var trimmed = line.stripLeading();
        var indent = line.substring(0, line.length() - trimmed.length());
        var indentStr = Theme.CODE_PLAIN.render(indent);

        if (trimmed.startsWith("@")) {
            return indentStr + Theme.CODE_ANNOTATION.render(trimmed);
        }

        if (trimmed.startsWith("//")) {
            return indentStr + Theme.MUTED_TEXT.render(trimmed);
        }

        var result = new StringBuilder();
        result.append(indentStr);

        var words = trimmed.split("(?<=\\s)|(?=\\s)");
        for (var word : words) {
            if (word.isBlank()) {
                result.append(word);
            } else if (isJavaKeyword(word)) {
                result.append(Theme.CODE_KEYWORD.render(word));
            } else if (isTypeName(word)) {
                result.append(Theme.CODE_TYPE.render(word));
            } else if (word.startsWith("\"") || word.endsWith("\"")) {
                result.append(Theme.CODE_STRING.render(word));
            } else {
                result.append(Theme.CODE_PLAIN.render(word));
            }
        }

        return result.toString();
    }

    private static boolean isJavaKeyword(String word) {
        return switch (word) {
            case "public", "private", "protected", "static", "final", "abstract",
                 "sealed", "class", "interface", "record", "enum", "extends",
                 "implements", "return", "new", "void", "int", "long", "boolean",
                 "float", "double", "byte", "this", "super", "if", "null" -> true;
            default -> false;
        };
    }

    private static boolean isTypeName(String word) {
        return switch (word) {
            case "String", "Map", "List", "Integer", "Long", "Boolean", "Float",
                 "Double", "ProtobufType", "ProtobufType.STRING", "ProtobufType.INT32",
                 "ProtobufUnknownValue", "Person", "Status" -> true;
            default -> false;
        };
    }
}
