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
