package com.github.auties00.daedalus.protobuf.cli.tui.onboarding;

import com.github.auties00.daedalus.protobuf.cli.model.EnumConstantMode;
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
                    @ProtobufMessage(name = "example.Person")
                    public record Person(
                        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                        String name,
                        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
                        int age
                    ) {
                    }""");
            case CLASS -> highlightJava("""
                    @ProtobufMessage(name = "example.Person")
                    public final class Person {
                        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                        private final String name;
                        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
                        private final int age;

                        public Person(String name, int age) { ... }

                        @ProtobufAccessor(index = 1)
                        public String name() { return name; }
                        @ProtobufAccessor(index = 2)
                        public int age() { return age; }
                    }""");
            case INTERFACE -> highlightJava("""
                    @ProtobufMessage(name = "example.Person")
                    public interface Person {
                        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                        String name();
                        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
                        int age();
                    }""");
            case SEALED_CLASS -> highlightJava("""
                    @ProtobufMessage(name = "example.Person")
                    public sealed abstract class Person {
                        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                        private final String name;
                        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
                        private final int age;

                        @ProtobufMessage
                        static final class DefaultPerson extends Person { ... }
                    }""");
            case SEALED_INTERFACE -> highlightJava("""
                    @ProtobufMessage(name = "example.Person")
                    public sealed interface Person {
                        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                        String name();
                        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
                        int age();

                        @ProtobufMessage
                        record DefaultPerson(String name, int age) implements Person {}
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
            case SEALED_INTERFACE -> highlightJava("""
                    @ProtobufEnum(name = "example.Status")
                    public sealed interface Status {
                        int index();
                        record Unknown() implements Status {
                            public int index() { return 0; }
                        }
                        record Active() implements Status {
                            public int index() { return 1; }
                        }
                        Status UNKNOWN = new Unknown();
                        Status ACTIVE = new Active();
                    }""");
            case SEALED_ABSTRACT_CLASS -> highlightJava("""
                    @ProtobufEnum(name = "example.Status")
                    public sealed abstract class Status {
                        private final int index;
                        protected Status(int index) { this.index = index; }
                        static final class UnknownImpl extends Status {
                            UnknownImpl() { super(0); }
                        }
                        static final Status UNKNOWN = new UnknownImpl();
                    }""");
        };
    }

    /**
     * Returns a syntax-highlighted code preview for the given enum constant mode.
     *
     * @param mode the enum constant mode to preview
     * @return the highlighted code string
     */
    public static String forEnumConstantMode(EnumConstantMode mode) {
        return switch (mode) {
            case CONSTANT_ANNOTATION -> highlightJava("""
                    @ProtobufEnum.Constant(index = 0)
                    UNKNOWN,
                    @ProtobufEnum.Constant(index = 1)
                    ACTIVE""");
            case SERIALIZER_DESERIALIZER -> highlightJava("""
                    UNKNOWN(0), ACTIVE(1), INACTIVE(2);

                    private final int index;
                    Status(int index) { this.index = index; }

                    @ProtobufSerializer
                    int index() { return index; }

                    @ProtobufDeserializer
                    static Status of(int index) { ... }""");
        };
    }

    /**
     * Returns a syntax-highlighted code preview for builder generation.
     *
     * @param enabled whether builders are enabled
     * @return the highlighted code string
     */
    public static String forBuilders(boolean enabled) {
        if (enabled) {
            return highlightJava("""
                    @ProtobufBuilder
                    public Person(
                        @ProtobufBuilder.PropertyParameter(index = 1) String name,
                        @ProtobufBuilder.PropertyParameter(index = 2) int age
                    ) {
                        this.name = name;
                        this.age = age;
                    }

                    // Auto-generated: PersonBuilder.name("...").age(25).build()""");
        }
        return Theme.MUTED_TEXT.render("  No builder will be generated.\n  Construct instances directly.");
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
                    private Map<Long, ProtobufUnknownValue> unknownFields;

                    @ProtobufUnknownFields.Setter
                    public void setUnknownField(long index, ProtobufUnknownValue value) {
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
                 "ProtobufUnknownValue", "Person", "Status", "PersonBuilder" -> true;
            default -> false;
        };
    }
}
