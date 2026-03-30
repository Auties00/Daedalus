package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufIntegerRangeExpression;
import com.github.auties00.daedalus.protobuf.compiler.expression.ProtobufLiteralExpression;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufGroupStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufModifier;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufReservedStatement;
import com.github.auties00.daedalus.protobuf.cli.config.SchemaConfig;
import com.github.auties00.daedalus.protobuf.cli.model.TypeForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Java source files for proto2 group definitions.
 *
 * <p>Groups use {@code @ProtobufGroup} instead of {@code @ProtobufMessage}
 * but otherwise follow the same type form patterns.
 */
public final class GroupGenerator {
    private final SchemaConfig config;
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;

    /**
     * Constructs a group generator with the given config and property generator.
     *
     * @param config the schema configuration
     * @param propertyGenerator the property generator for field annotations
     * @param naming the naming strategy
     */
    public GroupGenerator(SchemaConfig config, PropertyGenerator propertyGenerator, NamingStrategy naming) {
        this.config = config;
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
    }

    /**
     * Generates a Java {@link CompilationUnit} for a proto group.
     *
     * @param group the proto group statement
     * @param javaPackage the target Java package
     * @param protoFqn the fully-qualified proto name
     * @return the generated compilation unit
     */
    public CompilationUnit generate(ProtobufGroupStatement group, String javaPackage, String protoFqn) {
        var cu = new CompilationUnit();
        if (javaPackage != null && !javaPackage.isEmpty()) {
            cu.setPackageDeclaration(javaPackage);
        }

        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufGroup");
        cu.addImport("com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage.ProtobufProperty");
        cu.addImport("com.github.auties00.daedalus.protobuf.model.ProtobufType");

        var typeName = naming.toTypeName(group.name());
        var fields = new ArrayList<>(group.getDirectChildrenByType(ProtobufFieldStatement.class).toList());

        for (var field : fields) {
            if (field.type() instanceof com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference) {
                cu.addImport("java.util.Map");
            }
            if (field.modifier() == ProtobufModifier.REPEATED
                    && !(field.type() instanceof com.github.auties00.daedalus.protobuf.compiler.typeReference.ProtobufMapTypeReference)) {
                cu.addImport("java.util.List");
            }
        }

        var typeForm = resolveTypeForm(protoFqn);
        switch (typeForm) {
            case RECORD -> generateRecord(cu, group, typeName, protoFqn, fields);
            default -> generateRecord(cu, group, typeName, protoFqn, fields);
        }

        return cu;
    }

    private void generateRecord(CompilationUnit cu, ProtobufGroupStatement group,
                                 String typeName, String protoFqn,
                                 List<? extends ProtobufFieldStatement> fields) {
        var components = new NodeList<Parameter>();
        for (var field : fields) {
            components.add(propertyGenerator.createRecordComponent(field));
        }

        var record = new RecordDeclaration(
                new NodeList<>(Modifier.publicModifier()),
                new NodeList<>(),
                new SimpleName(typeName),
                components,
                new NodeList<>(),
                new NodeList<>(),
                new NodeList<>(),
                null
        );

        record.addAnnotation(createGroupAnnotation(protoFqn, group));
        cu.addType(record);
    }

    private AnnotationExpr createGroupAnnotation(String protoFqn, ProtobufGroupStatement group) {
        var pairs = new ArrayList<MemberValuePair>();

        if (protoFqn != null && !protoFqn.isEmpty()) {
            pairs.add(new MemberValuePair("protoName", new StringLiteralExpr(protoFqn)));
        }

        var reservedNames = new ArrayList<String>();
        var reservedIndexes = new ArrayList<Long>();
        var reservedRanges = new ArrayList<long[]>();

        group.getDirectChildrenByType(ProtobufReservedStatement.class).forEach(reserved ->
                reserved.expressions().forEach(expr -> {
                    if (expr instanceof ProtobufLiteralExpression literal) {
                        reservedNames.add(literal.value());
                    } else if (expr instanceof ProtobufIntegerExpression intExpr) {
                        reservedIndexes.add(intExpr.value().value().longValueExact());
                    } else if (expr instanceof ProtobufIntegerRangeExpression rangeExpr) {
                        var range = rangeExpr.value();
                        var lower = range.lowerBound().value().longValueExact();
                        var upper = range.upperBound()
                                .map(u -> u.value().longValueExact())
                                .orElse(536_870_911L);
                        reservedRanges.add(new long[]{lower, upper});
                    }
                })
        );

        if (!reservedNames.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var name : reservedNames) {
                values.add(new StringLiteralExpr(name));
            }
            pairs.add(new MemberValuePair("reservedNames", new ArrayInitializerExpr(values)));
        }

        if (!reservedIndexes.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var idx : reservedIndexes) {
                values.add(new IntegerLiteralExpr(String.valueOf(idx)));
            }
            pairs.add(new MemberValuePair("reservedIndexes", new ArrayInitializerExpr(values)));
        }

        if (!reservedRanges.isEmpty()) {
            var values = new NodeList<Expression>();
            for (var range : reservedRanges) {
                values.add(new NormalAnnotationExpr(
                        new Name("ProtobufReservedRange"),
                        new NodeList<>(
                                new MemberValuePair("min", new LongLiteralExpr(String.valueOf(range[0]))),
                                new MemberValuePair("max", new LongLiteralExpr(String.valueOf(range[1])))
                        )
                ));
            }
            pairs.add(new MemberValuePair("reservedRanges", new ArrayInitializerExpr(values)));
        }

        if (pairs.isEmpty()) {
            return new MarkerAnnotationExpr("ProtobufGroup");
        }

        return new NormalAnnotationExpr(new Name("ProtobufGroup"), new NodeList<>(pairs));
    }

    private TypeForm resolveTypeForm(String protoFqn) {
        if (protoFqn != null) {
            var override = config.typeOverrides().get(protoFqn);
            if (override != null && override.typeForm() != null) {
                return override.typeForm();
            }
        }
        return config.defaults().typeForm();
    }
}
