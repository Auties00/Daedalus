package com.github.auties00.daedalus.protobuf.cli.generation;

import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOneofStatement;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import java.util.List;

/**
 * A member of a generated message: either a field, or a {@code oneof} held by a single
 * field whose type is sealed.
 *
 * <p>The members of a message are ordered by {@linkplain #index() index}, which is the
 * order of the parameters of the constructor that the annotation processor expects.
 */
public sealed interface MessageMember {
    /**
     * Returns the index that orders this member among the members of its message.
     *
     * @return the index of a field, or the lowest index of the cases of a oneof
     */
    int index();

    /**
     * A field of a message.
     *
     * @param field the proto field statement
     */
    record Field(ProtobufFieldStatement field) implements MessageMember {
        /**
         * {@inheritDoc}
         *
         * @return the index of the field
         */
        @Override
        public int index() {
            return (int) field.index().value();
        }
    }

    /**
     * A {@code oneof} of a message, held by a field annotated with
     * {@code @ProtobufMessage.OneOfField} whose type is a sealed interface nested in the
     * message.
     *
     * @param statement the proto oneof statement
     * @param index the lowest index of the cases of the oneof
     * @param fieldName the name of the Java field that holds the oneof
     * @param typeName the simple name of the sealed interface that holds the cases
     * @param annotation the {@code @ProtobufMessage.OneOfField} annotation of the field,
     *        which must be cloned before being attached to a declaration
     * @param declaration the sealed interface, together with the records that wrap the
     *        cases that are not held by a message implementing it, which must be cloned
     *        before being attached to a declaration
     * @param imports the fully qualified names of the types that the annotation and the
     *        declaration need to import
     */
    record OneOf(
            ProtobufOneofStatement statement,
            int index,
            String fieldName,
            String typeName,
            NormalAnnotationExpr annotation,
            ClassOrInterfaceDeclaration declaration,
            List<String> imports
    ) implements MessageMember {

    }
}
