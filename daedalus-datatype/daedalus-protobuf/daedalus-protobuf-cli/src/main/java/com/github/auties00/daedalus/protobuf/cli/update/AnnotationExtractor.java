package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.util.*;

/**
 * Extracts annotation attribute values from JavaParser AST nodes.
 *
 * <p>Provides methods to extract the {@code name} attribute from
 * {@code @ProtobufMessage}, {@code @ProtobufEnum}, and {@code @ProtobufGroup}
 * annotations, and the {@code index} attribute from the typed field annotations,
 * such as {@code @ProtobufMessage.Int32Field}, the legacy {@code @ProtobufProperty}
 * annotation and {@code @ProtobufEnum.Constant} annotations.
 */
public final class AnnotationExtractor {
    /**
     * The simple names of the annotations that declare a protobuf field.
     */
    private static final List<String> FIELD_ANNOTATION_NAMES = List.of(
            "FloatField", "DoubleField", "BoolField",
            "Int32Field", "Sint32Field", "Uint32Field", "Fixed32Field", "Sfixed32Field",
            "Int64Field", "Sint64Field", "Uint64Field", "Fixed64Field", "Sfixed64Field",
            "EnumField", "StringField", "BytesField", "MessageField", "MapField",
            "ProtobufProperty"
    );

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
     * Extracts the set of protobuf field indexes from the members
     * of the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable set of field indexes
     */
    public Set<Integer> extractFieldIndexes(TypeDeclaration<?> typeDecl) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(extractFieldsByIndex(typeDecl).keySet()));
    }

    /**
     * Extracts a map from protobuf field index to the field or method name
     * in the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable map from field index to member name
     */
    public Map<Integer, String> extractFieldsByIndex(TypeDeclaration<?> typeDecl) {
        var fields = new LinkedHashMap<Integer, String>();
        for (var member : typeDecl.getMembers()) {
            if (member instanceof FieldDeclaration fieldDecl) {
                if (!fieldDecl.getVariables().isEmpty()) {
                    for (var index : extractIndexes(fieldDecl.getAnnotations())) {
                        fields.put(index, fieldDecl.getVariable(0).getNameAsString());
                    }
                }
            } else if (member instanceof MethodDeclaration methodDecl) {
                for (var index : extractIndexes(methodDecl.getAnnotations())) {
                    fields.put(index, methodDecl.getNameAsString());
                }
            }
        }
        if (typeDecl instanceof RecordDeclaration record) {
            for (var param : record.getParameters()) {
                for (var index : extractIndexes(param.getAnnotations())) {
                    fields.put(index, param.getNameAsString());
                }
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Returns the indexes declared by the given annotations of a member: the index of a field
     * annotation, or the indexes of the cases of a {@code @ProtobufMessage.OneOfField}
     * annotation.
     *
     * @param annotations the annotations of the member
     * @return the indexes, which are empty if the member is not a field
     */
    private List<Integer> extractIndexes(List<AnnotationExpr> annotations) {
        var result = new ArrayList<Integer>();
        for (var annotation : annotations) {
            if (isFieldAnnotation(annotation)) {
                var index = extractIntAttribute(annotation, "index");
                if (index != null) {
                    result.add(index);
                }
            } else if (isAnnotationNamed(annotation, "OneOfField") && annotation instanceof NormalAnnotationExpr oneOf) {
                for (var pair : oneOf.getPairs()) {
                    var values = pair.getValue() instanceof ArrayInitializerExpr array ? array.getValues() : List.of(pair.getValue());
                    for (var value : values) {
                        if (value instanceof AnnotationExpr oneOfCase) {
                            var index = extractIntAttribute(oneOfCase, "index");
                            if (index != null) {
                                result.add(index);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Extracts the set of {@code @ProtobufEnum.Constant} indexes from the members
     * of the given type declaration.
     *
     * @param typeDecl the type declaration to inspect
     * @return an unmodifiable set of enum constant indexes
     */
    public Set<Integer> extractEnumConstantIndexes(TypeDeclaration<?> typeDecl) {
        var indexes = new LinkedHashSet<Integer>();
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

    /**
     * Returns whether an annotation declares a protobuf field.
     *
     * <p>A field is declared by one of the typed field annotations nested in
     * {@code @ProtobufMessage}, such as {@code @ProtobufMessage.Int32Field}, or by the
     * legacy {@code @ProtobufProperty} annotation of sources generated by earlier
     * versions of the CLI.
     *
     * @param annotation the annotation to inspect
     * @return {@code true} if the annotation declares a protobuf field, otherwise {@code false}
     */
    private boolean isFieldAnnotation(AnnotationExpr annotation) {
        for (var name : FIELD_ANNOTATION_NAMES) {
            if (isAnnotationNamed(annotation, name)) {
                return true;
            }
        }
        return false;
    }

    private Integer extractConstantIndex(FieldDeclaration fieldDecl) {
        for (var annotation : fieldDecl.getAnnotations()) {
            if (isAnnotationNamed(annotation, "Constant")
                    || isAnnotationNamed(annotation, "ProtobufEnum.Constant")) {
                return extractIntAttribute(annotation, "index");
            }
        }
        return null;
    }

    private Integer extractConstantIndexFromEntry(EnumConstantDeclaration entry) {
        for (var annotation : entry.getAnnotations()) {
            if (isAnnotationNamed(annotation, "Constant")
                    || isAnnotationNamed(annotation, "ProtobufEnum.Constant")) {
                return extractIntAttribute(annotation, "index");
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

    private Integer extractIntAttribute(AnnotationExpr annotation, String attribute) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (var pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(attribute)) {
                    return parseIntExpression(pair.getValue());
                }
            }
        } else if (annotation instanceof SingleMemberAnnotationExpr single && attribute.equals("value")) {
            return parseIntExpression(single.getMemberValue());
        }
        return null;
    }

    private Integer parseIntExpression(Expression expr) {
        if (expr instanceof IntegerLiteralExpr intLit) {
            return intLit.asNumber().intValue();
        }
        if (expr instanceof LongLiteralExpr longLit) {
            return (int) longLit.asNumber().longValue();
        }
        if (expr instanceof UnaryExpr unary && unary.getOperator() == UnaryExpr.Operator.MINUS) {
            var inner = parseIntExpression(unary.getExpression());
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
