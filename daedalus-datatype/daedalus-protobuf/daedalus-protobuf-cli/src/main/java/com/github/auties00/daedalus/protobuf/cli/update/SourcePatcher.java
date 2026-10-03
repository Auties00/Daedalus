package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOneofStatement;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumConstantStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.cli.generation.NamingStrategy;
import com.github.auties00.daedalus.protobuf.cli.generation.PropertyGenerator;

import java.util.ArrayList;
import java.util.List;

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

        var newFields = propertyGenerator.collectFields(message)
                .stream()
                .filter(field -> !existingIndexes.contains((int) field.index().value()))
                .toList();

        // A oneof is held by a single field of a sealed type, which cannot be patched one member at a time
        for (var field : newFields) {
            if (field.parent() instanceof ProtobufOneofStatement oneof) {
                changes.add("Skipped member '" + field.name() + "' (index " + field.index().value() + ") of oneof '" + oneof.name() + "': add it as a case of the oneof manually, or regenerate the message");
            }
        }
        newFields = newFields.stream()
                .filter(field -> !(field.parent() instanceof ProtobufOneofStatement))
                .toList();

        if (newFields.isEmpty()) {
            return changes;
        }

        var withAccessors = false;
        if (typeDecl instanceof RecordDeclaration record) {
            patchRecordFields(record, newFields, changes, scannedType);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration classOrInterface) {
            if (classOrInterface.isInterface()) {
                patchInterfaceFields(classOrInterface, newFields, changes, scannedType);
            } else {
                withAccessors = hasAccessors(classOrInterface, scannedType);
                patchClassFields(classOrInterface, newFields, changes, withAccessors);
            }
        }

        addMissingImports(scannedType, newFields, withAccessors);
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
                .filter(constant -> !existingIndexes.contains((int) constant.index().value()))
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
            var index = (int) field.index().value();
            changes.add("Added record component '" + fieldName + "' (index " + index + ")");
        }
    }

    /**
     * Adds the declarations of new proto fields to a class.
     *
     * <p>Each field is added as a package private field, which the generated Spec class
     * reads directly, and, if the class declares accessors, together with a public
     * accessor that returns an {@code Optional} for a field that can be unset, exactly
     * like the {@link com.github.auties00.daedalus.protobuf.cli.generation.MessageGenerator}
     * does for a newly generated class.
     *
     * @param classDecl the class declaration to patch
     * @param newFields the proto fields to add
     * @param changes the list that receives a description of each change
     * @param withAccessors whether an accessor is added for each new field
     */
    private void patchClassFields(ClassOrInterfaceDeclaration classDecl, List<? extends ProtobufFieldStatement> newFields,
                                   List<String> changes, boolean withAccessors) {
        for (var field : newFields) {
            var fieldName = propertyGenerator.resolveFieldName(field);
            var index = (int) field.index().value();

            // The LexicalPreservingPrinter does not indent a member whose annotations are attached
            // before it joins the observed tree, so the annotations are attached afterwards
            var fieldDeclaration = propertyGenerator.createField(field);
            var annotations = new ArrayList<>(fieldDeclaration.getAnnotations());
            fieldDeclaration.getAnnotations().clear();
            classDecl.addMember(fieldDeclaration);
            annotations.forEach(fieldDeclaration::addAnnotation);
            if (withAccessors) {
                // The LexicalPreservingPrinter prints a comment that is attached before its node
                // joins the observed tree once for each observer, and does not indent it, so it
                // is attached afterwards with the indentation of the existing members
                var accessor = propertyGenerator.createAccessor(field);
                var javadoc = accessor.getJavadoc().orElse(null);
                accessor.removeComment();
                classDecl.addMember(accessor);
                if (javadoc != null) {
                    accessor.setJavadocComment(resolveMemberIndentation(classDecl), javadoc);
                }
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
            var index = (int) field.index().value();

            var method = interfaceDecl.addMethod(fieldName);
            method.setType(javaType);
            method.removeBody();
            method.addAnnotation(propertyGenerator.createFieldAnnotation(field));

            changes.add("Added method '" + fieldName + "()' (index " + index + ")");
        }
    }

    private void patchJavaEnumConstants(EnumDeclaration enumDecl,
                                         List<? extends ProtobufEnumConstantStatement> newConstants,
                                         List<String> changes) {
        for (var constant : newConstants) {
            var constantName = naming.toEnumConstantName(constant.name());
            var index = (int) constant.index().value();

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
            var index = (int) constant.index().value();

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
            var index = (int) constant.index().value();

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

    /**
     * Returns the indentation of the members of a class, as written in its source.
     *
     * <p>The indentation is the one of the first member of the class that has a position
     * in the source, or four spaces if there is none.
     *
     * @param classDecl the class declaration to inspect
     * @return the indentation of the members of the class
     */
    private String resolveMemberIndentation(ClassOrInterfaceDeclaration classDecl) {
        return classDecl.getMembers()
                .stream()
                .flatMap(member -> member.getBegin().stream())
                .findFirst()
                .map(position -> " ".repeat(Math.max(position.column - 1, 0)))
                .orElse("    ");
    }

    /**
     * Returns whether a class declares accessors for its proto fields.
     *
     * <p>A class declares accessors if one of its methods is annotated with
     * {@code @ProtobufAccessor}, or if one of its methods takes no parameters and is
     * named after one of its existing proto fields, which is how the generator declares
     * them.
     *
     * @param classDecl the class declaration to inspect
     * @param scannedType the scanned type of the class
     * @return {@code true} if the class declares accessors, otherwise {@code false}
     */
    private boolean hasAccessors(ClassOrInterfaceDeclaration classDecl, ScannedType scannedType) {
        var fieldNames = scannedType.existingFieldsByIndex().values();
        for (var member : classDecl.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                if (method.getParameters().isEmpty() && fieldNames.contains(method.getNameAsString())) {
                    return true;
                }
                for (var annotation : method.getAnnotations()) {
                    if (annotation.getNameAsString().contains("ProtobufAccessor")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adds the imports needed by new proto fields, and by their accessors, to the
     * compilation unit of a scanned type.
     *
     * @param scannedType the scanned type whose compilation unit is patched
     * @param newFields the proto fields that were added
     * @param withAccessors whether an accessor was added for each new field
     */
    private void addMissingImports(ScannedType scannedType, List<? extends ProtobufFieldStatement> newFields, boolean withAccessors) {
        var cu = scannedType.compilationUnit();
        for (var field : newFields) {
            for (var importName : propertyGenerator.requiredImports(field)) {
                addImportIfMissing(cu, importName);
            }
            if (withAccessors) {
                for (var importName : propertyGenerator.requiredAccessorImports(field)) {
                    addImportIfMissing(cu, importName);
                }
            }
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
