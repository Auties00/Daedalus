package com.github.auties00.daedalus.processor.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * A utility for resolving placeholder tokens in builder name segments.
 *
 * <p>Name segments may contain the following placeholders:
 * <ul>
 * <li>{@code $FIELD_NAME} resolves to the field name
 * <li>{@code $CLASS_NAME} resolves to the simple name of the enclosing type
 * <li>{@code $FIELD_TYPE_NAME} resolves to the simple name of the field type
 * </ul>
 *
 * <p>Resolved segments are joined in camelCase: the first segment is lowercased,
 * subsequent segments have their first character uppercased.
 */
public final class DaedalusBuilderNameUtils {

    private DaedalusBuilderNameUtils() {
        throw new UnsupportedOperationException("DaedalusBuilderNameUtils is a utility class and cannot be instantiated");
    }

    /**
     * Resolves setter name segments into a single camelCase method name.
     *
     * <p>If the segments array is empty, the field name is returned as-is.
     *
     * @param segments the name segments, possibly containing placeholders
     * @param fieldName the field name for {@code $FIELD_NAME} resolution
     * @param fieldType the field type for {@code $FIELD_TYPE_NAME} resolution
     * @param enclosingType the enclosing type for {@code $CLASS_NAME} resolution
     * @return the resolved camelCase method name
     */
    public static String resolveSetterName(String[] segments, String fieldName, TypeMirror fieldType, TypeElement enclosingType) {
        if (segments.length == 0) {
            return fieldName;
        }

        var result = new StringBuilder();
        for (var i = 0; i < segments.length; i++) {
            var resolved = resolveSegment(segments[i], fieldName, fieldType, enclosingType);
            if (i == 0) {
                result.append(decapitalize(resolved));
            } else {
                result.append(capitalize(resolved));
            }
        }
        return result.toString();
    }

    /**
     * Resolves a single segment by replacing any placeholder token with its value.
     *
     * @param segment the segment to resolve
     * @param fieldName the field name
     * @param fieldType the field type
     * @param enclosingType the enclosing type
     * @return the resolved segment
     */
    private static String resolveSegment(String segment, String fieldName, TypeMirror fieldType, TypeElement enclosingType) {
        return switch (segment) {
            case TypeBuilder.FIELD_NAME -> fieldName;
            case TypeBuilder.CLASS_NAME -> enclosingType.getSimpleName().toString();
            case TypeBuilder.FIELD_TYPE_NAME -> getSimpleTypeName(fieldType);
            default -> segment;
        };
    }

    /**
     * Returns the simple name of a type mirror.
     *
     * @param type the type mirror
     * @return the simple type name
     */
    private static String getSimpleTypeName(TypeMirror type) {
        if (type instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement element) {
            return element.getSimpleName().toString();
        }
        return type.toString();
    }

    /**
     * Returns the given string with its first character lowercased.
     *
     * @param value the string to decapitalize
     * @return the decapitalized string
     */
    private static String decapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * Returns the given string with its first character uppercased.
     *
     * @param value the string to capitalize
     * @return the capitalized string
     */
    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
