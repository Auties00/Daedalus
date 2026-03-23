package it.auties.protobuf.schema.update;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import it.auties.protobuf.parser.tree.ProtobufEnumConstantStatement;
import it.auties.protobuf.parser.tree.ProtobufEnumStatement;
import it.auties.protobuf.parser.tree.ProtobufFieldStatement;
import it.auties.protobuf.parser.tree.ProtobufMessageStatement;
import it.auties.protobuf.parser.tree.ProtobufModifier;
import it.auties.protobuf.schema.generation.NamingStrategy;
import it.auties.protobuf.schema.generation.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Applies additive patches to existing Java source files based on proto schema changes.
 *
 * <p>Uses JavaParser's {@link LexicalPreservingPrinter} to preserve all existing
 * formatting, comments, and user-written code. Only adds new fields, methods,
 * and enum constants that are missing from the Java source but present in the
 * proto schema.
 *
 * <p>This patcher never removes or modifies existing code without explicit
 * user confirmation.
 */
public final class SourcePatcher {
    private final PropertyGenerator propertyGenerator;
    private final NamingStrategy naming;

    /**
     * Constructs a source patcher with the given property generator and naming strategy.
     *
     * @param propertyGenerator the property generator for creating field annotations
     * @param naming the naming strategy for converting proto names to Java names
     */
    public SourcePatcher(PropertyGenerator propertyGenerator, NamingStrategy naming) {
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
    }

    /**
     * Patches a message type by adding new fields from the proto definition
     * that are not yet present in the Java source.
     *
     * @param scannedType the scanned Java type to patch
     * @param message the proto message definition
     * @return the list of descriptions of changes made
     */
    public List<String> patchMessage(ScannedType scannedType, ProtobufMessageStatement message) {
        var changes = new ArrayList<String>();
        var typeDecl = scannedType.typeDeclaration();
        var existingIndexes = scannedType.existingFieldIndexes();

        var newFields = message.getDirectChildrenByType(ProtobufFieldStatement.class)
                .filter(field -> !existingIndexes.contains(field.index().value().longValueExact()))
                .toList();

        if (newFields.isEmpty()) {
            return changes;
        }

        if (typeDecl instanceof RecordDeclaration record) {
            patchRecordFields(record, newFields, changes, scannedType);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration classOrInterface) {
            if (classOrInterface.isInterface()) {
                patchInterfaceFields(classOrInterface, newFields, changes, scannedType);
            } else {
                patchClassFields(classOrInterface, newFields, changes, scannedType);
            }
        }

        addMissingImports(scannedType, newFields);
        return changes;
    }

    /**
     * Patches an enum type by adding new constants from the proto definition
     * that are not yet present in the Java source.
     *
     * @param scannedType the scanned Java type to patch
     * @param enumStmt the proto enum definition
     * @return the list of descriptions of changes made
     */
    public List<String> patchEnum(ScannedType scannedType, ProtobufEnumStatement enumStmt) {
        var changes = new ArrayList<String>();
        var typeDecl = scannedType.typeDeclaration();
        var existingIndexes = scannedType.existingEnumConstantIndexes();

        var newConstants = enumStmt.getDirectChildrenByType(ProtobufEnumConstantStatement.class)
                .filter(constant -> !existingIndexes.contains((long) constant.index().value().longValueExact()))
                .toList();

        if (newConstants.isEmpty()) {
            return changes;
        }

        if (typeDecl instanceof EnumDeclaration enumDecl) {
            patchJavaEnumConstants(enumDecl, newConstants, changes);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration classDecl) {
            patchClassEnumConstants(classDecl, newConstants, changes, scannedType);
        } else if (typeDecl instanceof RecordDeclaration recordDecl) {
            patchRecordEnumConstants(recordDecl, newConstants, changes, scannedType);
        }

        return changes;
    }

    /**
     * Returns the updated source text for a scanned type after patching.
     *
     * <p>Uses {@link LexicalPreservingPrinter} to produce output that preserves
     * the original formatting.
     *
     * @param scannedType the scanned type whose compilation unit was modified
     * @return the updated source text
     */
    public String getUpdatedSource(ScannedType scannedType) {
        return LexicalPreservingPrinter.print(scannedType.compilationUnit());
    }

    private void patchRecordFields(RecordDeclaration record, List<? extends ProtobufFieldStatement> newFields,
                                    List<String> changes, ScannedType scannedType) {
        for (var field : newFields) {
            var component = propertyGenerator.createRecordComponent(field);
            record.getParameters().add(component);
            var fieldName = propertyGenerator.resolveFieldName(field);
            var index = field.index().value().longValueExact();
            changes.add("Added record component '" + fieldName + "' (index " + index + ")");
        }
    }

    private void patchClassFields(ClassOrInterfaceDeclaration classDecl, List<? extends ProtobufFieldStatement> newFields,
                                   List<String> changes, ScannedType scannedType) {
        for (var field : newFields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            var index = field.index().value().longValueExact();

            // Add field declaration
            var fieldDeclaration = classDecl.addField(javaType, fieldName,
                    Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
            fieldDeclaration.getVariable(0).getParentNode().ifPresent(parent -> {
                if (parent instanceof FieldDeclaration fd) {
                    fd.addAnnotation(propertyGenerator.createPropertyAnnotation(field));
                }
            });

            // Add accessor method if the class uses @ProtobufAccessor
            if (hasAccessorAnnotations(classDecl)) {
                var accessor = classDecl.addMethod(fieldName, Modifier.Keyword.PUBLIC);
                accessor.setType(javaType);
                accessor.addAnnotation(new NormalAnnotationExpr(
                        new Name("ProtobufAccessor"),
                        new NodeList<>(new MemberValuePair("index",
                                new LongLiteralExpr(String.valueOf(index))))
                ));
                accessor.setBody(new BlockStmt().addStatement(
                        new ReturnStmt(new NameExpr(fieldName))
                ));
            }

            changes.add("Added field '" + fieldName + "' (index " + index + ")");
        }
    }

    private void patchInterfaceFields(ClassOrInterfaceDeclaration interfaceDecl,
                                       List<? extends ProtobufFieldStatement> newFields,
                                       List<String> changes, ScannedType scannedType) {
        for (var field : newFields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var javaType = propertyGenerator.resolveJavaType(field);
            var index = field.index().value().longValueExact();

            var method = interfaceDecl.addMethod(fieldName);
            method.setType(javaType);
            method.removeBody();
            method.addAnnotation(propertyGenerator.createPropertyAnnotation(field));

            changes.add("Added method '" + fieldName + "()' (index " + index + ")");
        }
    }

    private void patchJavaEnumConstants(EnumDeclaration enumDecl,
                                         List<? extends ProtobufEnumConstantStatement> newConstants,
                                         List<String> changes) {
        for (var constant : newConstants) {
            var constantName = naming.toEnumConstantName(constant.name());
            var index = constant.index().value().longValueExact();

            var entry = new EnumConstantDeclaration(constantName);
            entry.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index",
                            new IntegerLiteralExpr(String.valueOf(index))))
            ));
            enumDecl.getEntries().add(entry);

            changes.add("Added enum constant '" + constantName + "' (index " + index + ")");
        }
    }

    private void patchClassEnumConstants(ClassOrInterfaceDeclaration classDecl,
                                          List<? extends ProtobufEnumConstantStatement> newConstants,
                                          List<String> changes, ScannedType scannedType) {
        var typeName = classDecl.getNameAsString();
        for (var constant : newConstants) {
            var constantName = naming.toEnumConstantName(constant.name());
            var index = constant.index().value().longValueExact();

            var fieldDecl = classDecl.addFieldWithInitializer(
                    typeName, constantName,
                    new ObjectCreationExpr(null,
                            new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new IntegerLiteralExpr(String.valueOf(index)))),
                    Modifier.Keyword.STATIC, Modifier.Keyword.FINAL
            );
            fieldDecl.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index",
                            new IntegerLiteralExpr(String.valueOf(index))))
            ));

            changes.add("Added static constant '" + constantName + "' (index " + index + ")");
        }
    }

    private void patchRecordEnumConstants(RecordDeclaration recordDecl,
                                           List<? extends ProtobufEnumConstantStatement> newConstants,
                                           List<String> changes, ScannedType scannedType) {
        var typeName = recordDecl.getNameAsString();
        for (var constant : newConstants) {
            var constantName = naming.toEnumConstantName(constant.name());
            var index = constant.index().value().longValueExact();

            var fieldDecl = new FieldDeclaration();
            fieldDecl.addModifier(Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            fieldDecl.addVariable(new VariableDeclarator(
                    new ClassOrInterfaceType(null, typeName),
                    constantName,
                    new ObjectCreationExpr(null,
                            new ClassOrInterfaceType(null, typeName),
                            new NodeList<>(new IntegerLiteralExpr(String.valueOf(index))))
            ));
            fieldDecl.addAnnotation(new NormalAnnotationExpr(
                    new Name("ProtobufEnum.Constant"),
                    new NodeList<>(new MemberValuePair("index",
                            new IntegerLiteralExpr(String.valueOf(index))))
            ));
            recordDecl.addMember(fieldDecl);

            changes.add("Added static constant '" + constantName + "' (index " + index + ")");
        }
    }

    private boolean hasAccessorAnnotations(ClassOrInterfaceDeclaration classDecl) {
        for (var member : classDecl.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                for (var annotation : method.getAnnotations()) {
                    if (annotation.getNameAsString().contains("ProtobufAccessor")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addMissingImports(ScannedType scannedType, List<? extends ProtobufFieldStatement> newFields) {
        var cu = scannedType.compilationUnit();
        var hasMap = false;
        var hasList = false;

        for (var field : newFields) {
            if (field.type() instanceof it.auties.protobuf.parser.typeReference.ProtobufMapTypeReference) {
                hasMap = true;
            }
            if (field.modifier() == ProtobufModifier.REPEATED
                    && !(field.type() instanceof it.auties.protobuf.parser.typeReference.ProtobufMapTypeReference)) {
                hasList = true;
            }
        }

        addImportIfMissing(cu, "it.auties.protobuf.annotation.ProtobufMessage.ProtobufProperty");
        addImportIfMissing(cu, "it.auties.protobuf.model.ProtobufType");
        if (hasMap) {
            addImportIfMissing(cu, "java.util.Map");
        }
        if (hasList) {
            addImportIfMissing(cu, "java.util.List");
        }
    }

    private void addImportIfMissing(com.github.javaparser.ast.CompilationUnit cu, String importName) {
        var alreadyImported = cu.getImports().stream()
                .anyMatch(imp -> imp.getNameAsString().equals(importName));
        if (!alreadyImported) {
            cu.addImport(importName);
        }
    }
}
