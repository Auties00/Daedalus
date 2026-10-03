package com.github.auties00.daedalus.protobuf.cli.generation;

/**
 * Converts proto names (snake_case, PascalCase, UPPER_SNAKE_CASE) to Java naming conventions.
 */
public final class NamingStrategy {
    /**
     * Constructs a naming strategy with default conventions.
     */
    public NamingStrategy() {
    }

    /**
     * Converts a proto message or enum name to a Java type name (PascalCase).
     *
     * <p>Proto type names are already PascalCase by convention, so this typically
     * returns the name unchanged. Underscores are removed and the following
     * character is capitalized.
     *
     * @param protoName the proto type name
     * @return the Java type name in PascalCase
     */
    public String toTypeName(String protoName) {
        if (protoName == null || protoName.isEmpty()) {
            return protoName;
        }
        return toPascalCase(protoName);
    }

    /**
     * Converts a proto field name (snake_case) to a Java field/method name (camelCase).
     *
     * @param protoFieldName the proto field name
     * @return the Java field name in camelCase
     */
    public String toFieldName(String protoFieldName) {
        if (protoFieldName == null || protoFieldName.isEmpty()) {
            return protoFieldName;
        }
        var pascal = toPascalCase(protoFieldName);
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /**
     * Converts a proto enum constant name to a Java enum constant name (UPPER_SNAKE_CASE).
     *
     * <p>Proto enum constants are already UPPER_SNAKE_CASE by convention.
     *
     * @param protoConstantName the proto enum constant name
     * @return the Java enum constant name in UPPER_SNAKE_CASE
     */
    public String toEnumConstantName(String protoConstantName) {
        return protoConstantName;
    }

    /**
     * Converts a Java name to the {@code snake_case} name that the annotation processor
     * derives from it when a field annotation does not declare a {@code name}.
     *
     * <p>An underscore is inserted before an uppercase letter that follows a lowercase
     * letter or a digit, and before the last letter of a run of uppercase letters that is
     * followed by a lowercase letter, so that {@code URLValue} becomes {@code url_value}.
     *
     * @param javaName the Java name
     * @return the {@code snake_case} name
     */
    public String toSnakeCase(String javaName) {
        var result = new StringBuilder(javaName.length() + 4);
        for (var i = 0; i < javaName.length(); i++) {
            var current = javaName.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                var previous = javaName.charAt(i - 1);
                var nextIsLower = i + 1 < javaName.length() && Character.isLowerCase(javaName.charAt(i + 1));
                if (Character.isLowerCase(previous) || Character.isDigit(previous) || (Character.isUpperCase(previous) && nextIsLower)) {
                    result.append('_');
                }
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

    private String toPascalCase(String input) {
        var result = new StringBuilder(input.length());
        var capitalizeNext = true;
        for (var i = 0; i < input.length(); i++) {
            var c = input.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
