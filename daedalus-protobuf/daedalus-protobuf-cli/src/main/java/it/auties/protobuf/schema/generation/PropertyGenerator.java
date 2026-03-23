package it.auties.protobuf.schema.generation;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.parser.tree.ProtobufFieldStatement;
import it.auties.protobuf.parser.tree.ProtobufModifier;
import it.auties.protobuf.parser.typeReference.ProtobufMapTypeReference;
import it.auties.protobuf.parser.typeReference.ProtobufTypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates {@code @ProtobufProperty} annotation expressions and Java type names
 * for proto field definitions.
 */
public final class PropertyGenerator {
    private final TypeMapper typeMapper;
    private final NamingStrategy naming;

    /**
     * Constructs a property generator with the given type mapper and naming strategy.
     *
     * @param typeMapper the type mapper for resolving Java types
     * @param naming the naming strategy for field names
     */
    public PropertyGenerator(TypeMapper typeMapper, NamingStrategy naming) {
        this.typeMapper = typeMapper;
        this.naming = naming;
    }

    /**
     * Creates the {@code @ProtobufProperty} annotation expression for a proto field.
     *
     * @param field the proto field statement
     * @return the annotation expression
     */
    public NormalAnnotationExpr createPropertyAnnotation(ProtobufFieldStatement field) {
        var pairs = new ArrayList<MemberValuePair>();

        pairs.add(new MemberValuePair("index", new LongLiteralExpr(
                String.valueOf(field.index().value().longValueExact())
        )));

        var protobufTypeName = typeMapper.toProtobufTypeName(field.type());
        pairs.add(new MemberValuePair("type", new FieldAccessExpr(
                new NameExpr("ProtobufType"), protobufTypeName
        )));

        if (field.type() instanceof ProtobufMapTypeReference map) {
            pairs.add(new MemberValuePair("mapKeyType", new FieldAccessExpr(
                    new NameExpr("ProtobufType"), typeMapper.toMapKeyTypeName(map)
            )));
            pairs.add(new MemberValuePair("mapValueType", new FieldAccessExpr(
                    new NameExpr("ProtobufType"), typeMapper.toMapValueTypeName(map)
            )));
        }

        if (field.modifier() == ProtobufModifier.REQUIRED) {
            pairs.add(new MemberValuePair("required", new BooleanLiteralExpr(true)));
        }

        if (isPacked(field)) {
            pairs.add(new MemberValuePair("packed", new BooleanLiteralExpr(true)));
        }

        return new NormalAnnotationExpr(
                new Name("ProtobufProperty"),
                new NodeList<>(pairs)
        );
    }

    /**
     * Returns the Java type string for a proto field, accounting for repeated
     * (wrapping in {@code List<>}) and map types.
     *
     * @param field the proto field statement
     * @return the Java type string
     */
    public String resolveJavaType(ProtobufFieldStatement field) {
        var baseType = typeMapper.toJavaType(field.type(), isRepeated(field));
        if (isRepeated(field) && !(field.type() instanceof ProtobufMapTypeReference)) {
            return "List<" + boxType(baseType) + ">";
        }
        return baseType;
    }

    /**
     * Returns the Java field name for a proto field.
     *
     * @param field the proto field statement
     * @return the Java field name in camelCase
     */
    public String resolveFieldName(ProtobufFieldStatement field) {
        return naming.toFieldName(field.name());
    }

    /**
     * Creates a record component {@link Parameter} for this field,
     * with the {@code @ProtobufProperty} annotation applied.
     *
     * @param field the proto field statement
     * @return the annotated parameter for a record component
     */
    public Parameter createRecordComponent(ProtobufFieldStatement field) {
        var annotation = createPropertyAnnotation(field);
        var javaType = resolveJavaType(field);
        var fieldName = resolveFieldName(field);
        var param = new Parameter(
                parseType(javaType),
                fieldName
        );
        param.addAnnotation(annotation);
        return param;
    }

    /**
     * Returns the list of imports needed for a proto field's Java type.
     *
     * @param field the proto field statement
     * @return the list of fully-qualified class names to import
     */
    public List<String> requiredImports(ProtobufFieldStatement field) {
        var imports = new ArrayList<String>();
        imports.add("it.auties.protobuf.annotation.ProtobufMessage.ProtobufProperty");
        imports.add("it.auties.protobuf.model.ProtobufType");

        if (isRepeated(field) && !(field.type() instanceof ProtobufMapTypeReference)) {
            imports.add("java.util.List");
        }
        if (field.type() instanceof ProtobufMapTypeReference) {
            imports.add("java.util.Map");
        }
        return imports;
    }

    private boolean isRepeated(ProtobufFieldStatement field) {
        return field.modifier() == ProtobufModifier.REPEATED;
    }

    private boolean isPacked(ProtobufFieldStatement field) {
        var packedOption = field.getOption("packed");
        return packedOption.isPresent();
    }

    private String boxType(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            case "byte[]" -> "byte[]";
            default -> type;
        };
    }

    private com.github.javaparser.ast.type.Type parseType(String typeName) {
        if (typeName.startsWith("List<") || typeName.startsWith("Map<")) {
            return new ClassOrInterfaceType(null, typeName);
        }
        return switch (typeName) {
            case "int" -> com.github.javaparser.ast.type.PrimitiveType.intType();
            case "long" -> com.github.javaparser.ast.type.PrimitiveType.longType();
            case "float" -> com.github.javaparser.ast.type.PrimitiveType.floatType();
            case "double" -> com.github.javaparser.ast.type.PrimitiveType.doubleType();
            case "boolean" -> com.github.javaparser.ast.type.PrimitiveType.booleanType();
            case "byte[]" -> com.github.javaparser.StaticJavaParser.parseType("byte[]");
            default -> new ClassOrInterfaceType(null, typeName);
        };
    }
}
