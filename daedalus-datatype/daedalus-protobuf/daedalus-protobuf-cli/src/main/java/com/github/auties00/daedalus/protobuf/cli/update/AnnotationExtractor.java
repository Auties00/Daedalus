package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.util.*;

/**
 * Extracts annotation attribute values from JavaParser AST nodes.
 *
 * <p>Provides methods to extract the {@code name} attribute from
 * {@code @ProtobufMessage}, {@code @ProtobufEnum}, and {@code @ProtobufGroup}
 * annotations, and the {@code index} attribute from {@code @ProtobufProperty}
 * and {@code @ProtobufEnum.Constant} annotations.
 */
public final class AnnotationExtractor {

    /**
     * Constructs an annotation extractor.
     */
    public AnnotationExtractor() {
    }

    /**
     * Returns the protobuf annotation type present on the given type declaration,
     * or {@code null} if none is found.
     *
     * @param typeDecl the type declaration to inspect
     * @return the annotation type, or {@code null}
     */
    public ScannedType.AnnotationType findAnnotationType(TypeDeclaration<?> typeDecl) {
        if (hasAnnotation(typeDecl, "ProtobufMessage")) {
            return ScannedType.AnnotationType.MESSAGE;
        }
        if (hasAnnotation(typeDecl, "ProtobufEnum")) {
            return ScannedType.AnnotationType.ENUM;
        }
        if (hasAnnotation(typeDecl, "ProtobufGroup")) {
            return ScannedType.AnnotationType.GROUP;
        }
        return null;
    }

    /**
     * Extracts the {@code name} attribute value from the protobuf annotation
     * on the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @param annotationType the expected annotation type
     * @return the name attribute value, or {@code null} if not specified or empty
     */
    public String extractProtoName(TypeDeclaration<?> typeDecl, ScannedType.AnnotationType annotationType) {
        var annotationName = switch (annotationType) {
            case MESSAGE -> "ProtobufMessage";
            case ENUM -> "ProtobufEnum";
            case GROUP -> "ProtobufGroup";
        };
        return extractStringAttribute(typeDecl, annotationName, "protoName");
    }

    /**
     * Extracts the set of {@code @ProtobufProperty} field indexes from the members
     * of the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable set of field indexes
     */
    public Set<Long> extractFieldIndexes(TypeDeclaration<?> typeDecl) {
        var indexes = new LinkedHashSet<Long>();
        for (var member : typeDecl.getMembers()) {
            var index = extractPropertyIndex(member);
            if (index != null) {
                indexes.add(index);
            }
        }
        if (typeDecl instanceof RecordDeclaration record) {
            for (var param : record.getParameters()) {
                var index = extractPropertyIndexFromParameter(param);
                if (index != null) {
                    indexes.add(index);
                }
            }
        }
        return Collections.unmodifiableSet(indexes);
    }

    /**
     * Extracts a map from {@code @ProtobufProperty} index to the field or method name
     * in the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable map from field index to member name
     */
    public Map<Long, String> extractFieldsByIndex(TypeDeclaration<?> typeDecl) {
        var fields = new LinkedHashMap<Long, String>();
        for (var member : typeDecl.getMembers()) {
            if (member instanceof FieldDeclaration fieldDecl) {
                var index = extractPropertyIndex(fieldDecl);
                if (index != null && !fieldDecl.getVariables().isEmpty()) {
                    fields.put(index, fieldDecl.getVariable(0).getNameAsString());
                }
            } else if (member instanceof MethodDeclaration methodDecl) {
                var index = extractPropertyIndex(methodDecl);
                if (index != null) {
                    fields.put(index, methodDecl.getNameAsString());
                }
            }
        }
        if (typeDecl instanceof RecordDeclaration record) {
            for (var param : record.getParameters()) {
                var index = extractPropertyIndexFromParameter(param);
                if (index != null) {
                    fields.put(index, param.getNameAsString());
                }
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Extracts the set of {@code @ProtobufEnum.Constant} indexes from the members
     * of the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable set of enum constant indexes
     */
    public Set<Long> extractEnumConstantIndexes(TypeDeclaration<?> typeDecl) {
        var indexes = new LinkedHashSet<Long>();
        for (var member : typeDecl.getMembers()) {
            if (member instanceof FieldDeclaration fieldDecl) {
                var index = extractConstantIndex(fieldDecl);
                if (index != null) {
                    indexes.add(index);
                }
            }
        }
        if (typeDecl instanceof EnumDeclaration enumDecl) {
            for (var entry : enumDecl.getEntries()) {
                var index = extractConstantIndexFromEntry(entry);
                if (index != null) {
                    indexes.add(index);
                }
            }
        }
        return Collections.unmodifiableSet(indexes);
    }

    private Long extractPropertyIndex(BodyDeclaration<?> member) {
        for (var annotation : member.getAnnotations()) {
            if (isAnnotationNamed(annotation, "ProtobufProperty")) {
                return extractLongAttribute(annotation, "index");
            }
        }
        return null;
    }

    private Long extractPropertyIndexFromParameter(Parameter param) {
        for (var annotation : param.getAnnotations()) {
            if (isAnnotationNamed(annotation, "ProtobufProperty")) {
                return extractLongAttribute(annotation, "index");
            }
        }
        return null;
    }

    private Long extractConstantIndex(FieldDeclaration fieldDecl) {
        for (var annotation : fieldDecl.getAnnotations()) {
            if (isAnnotationNamed(annotation, "Constant")
                    || isAnnotationNamed(annotation, "ProtobufEnum.Constant")) {
                return extractLongAttribute(annotation, "index");
            }
        }
        return null;
    }

    private Long extractConstantIndexFromEntry(EnumConstantDeclaration entry) {
        for (var annotation : entry.getAnnotations()) {
            if (isAnnotationNamed(annotation, "Constant")
                    || isAnnotationNamed(annotation, "ProtobufEnum.Constant")) {
                return extractLongAttribute(annotation, "index");
            }
        }
        return null;
    }

    private String extractStringAttribute(TypeDeclaration<?> typeDecl, String annotationName, String attribute) {
        for (var annotation : typeDecl.getAnnotations()) {
            if (isAnnotationNamed(annotation, annotationName)) {
                if (annotation instanceof NormalAnnotationExpr normal) {
                    for (var pair : normal.getPairs()) {
                        if (pair.getNameAsString().equals(attribute)) {
                            var value = pair.getValue();
                            if (value instanceof StringLiteralExpr stringLit) {
                                var str = stringLit.getValue();
                                return str.isEmpty() ? null : str;
                            }
                        }
                    }
                } else if (annotation instanceof SingleMemberAnnotationExpr single && attribute.equals("value")) {
                    if (single.getMemberValue() instanceof StringLiteralExpr stringLit) {
                        var str = stringLit.getValue();
                        return str.isEmpty() ? null : str;
                    }
                }
                return null;
            }
        }
        return null;
    }

    private Long extractLongAttribute(AnnotationExpr annotation, String attribute) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (var pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(attribute)) {
                    return parseLongExpression(pair.getValue());
                }
            }
        } else if (annotation instanceof SingleMemberAnnotationExpr single && attribute.equals("value")) {
            return parseLongExpression(single.getMemberValue());
        }
        return null;
    }

    private Long parseLongExpression(Expression expr) {
        if (expr instanceof IntegerLiteralExpr intLit) {
            return (long) intLit.asNumber().intValue();
        }
        if (expr instanceof LongLiteralExpr longLit) {
            return longLit.asNumber().longValue();
        }
        if (expr instanceof UnaryExpr unary && unary.getOperator() == UnaryExpr.Operator.MINUS) {
            var inner = parseLongExpression(unary.getExpression());
            return inner != null ? -inner : null;
        }
        return null;
    }

    private boolean hasAnnotation(TypeDeclaration<?> typeDecl, String name) {
        for (var annotation : typeDecl.getAnnotations()) {
            if (isAnnotationNamed(annotation, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnnotationNamed(AnnotationExpr annotation, String name) {
        var annotationName = annotation.getNameAsString();
        return annotationName.equals(name)
                || annotationName.endsWith("." + name);
    }
}
