package it.auties.protobuf.schema.generation;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import it.auties.protobuf.parser.tree.ProtobufFieldStatement;
import it.auties.protobuf.parser.tree.ProtobufOneofStatement;

import java.util.ArrayList;

/**
 * Generates sealed type hierarchies for proto {@code oneof} declarations.
 *
 * <p>Each oneof becomes a sealed interface with record subtypes, one per
 * oneof member field. Each record subtype holds a single
 * {@code @ProtobufProperty}-annotated value component.
 */
public final class OneofGenerator {
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;

    /**
     * Constructs a oneof generator.
     *
     * @param propertyGenerator the property generator for field annotations
     * @param naming the naming strategy
     */
    public OneofGenerator(PropertyGenerator propertyGenerator, NamingStrategy naming) {
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
    }

    /**
     * Generates a sealed interface with record subtypes for a proto oneof
     * and adds it as a nested type in the parent type declaration.
     *
     * <p>Each oneof field becomes a record implementing the sealed interface.
     * The record's single component is annotated with {@code @ProtobufProperty}
     * carrying the field's index and type.
     *
     * @param parentType the parent type to add the sealed hierarchy into
     * @param oneof the proto oneof statement
     */
    public void generate(TypeDeclaration<?> parentType, ProtobufOneofStatement oneof) {
        var sealedName = naming.toTypeName(oneof.name()) + "Seal";
        var fields = new ArrayList<>(oneof.getDirectChildrenByType(ProtobufFieldStatement.class).toList());

        var sealedInterface = new ClassOrInterfaceDeclaration();
        sealedInterface.setName(sealedName);
        sealedInterface.setInterface(true);
        sealedInterface.addModifier(Modifier.Keyword.SEALED);
        sealedInterface.addAnnotation(new MarkerAnnotationExpr("ProtobufMessage"));

        for (var field : fields) {
            var subtypeName = naming.toTypeName(field.name());
            var javaType = propertyGenerator.resolveJavaType(field);
            var annotation = propertyGenerator.createPropertyAnnotation(field);

            var valueParam = new Parameter(
                    com.github.javaparser.StaticJavaParser.parseType(javaType),
                    "value"
            );
            valueParam.addAnnotation(annotation);

            var components = new NodeList<Parameter>();
            components.add(valueParam);

            var record = new RecordDeclaration(
                    new NodeList<>(),
                    new NodeList<>(),
                    new SimpleName(subtypeName),
                    components,
                    new NodeList<>(),
                    new NodeList<>(new ClassOrInterfaceType(null, sealedName)),
                    new NodeList<>(),
                    null
            );
            record.addModifier(Modifier.Keyword.STATIC);
            record.addAnnotation(new MarkerAnnotationExpr("ProtobufMessage"));

            sealedInterface.addMember(record);
        }

        parentType.addMember(sealedInterface);
    }
}
