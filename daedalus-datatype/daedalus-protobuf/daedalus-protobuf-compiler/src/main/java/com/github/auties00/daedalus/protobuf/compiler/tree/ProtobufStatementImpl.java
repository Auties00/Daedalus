package com.github.auties00.daedalus.protobuf.compiler.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Abstract base class for all Protocol Buffer statement implementations in the AST.
 *
 * <p>This sealed class provides common functionality for all statement types, including source
 * range tracking, attached comments, and the parent-child relationship. It exists to provide
 * package-private mutation methods (notably {@code setRange}, {@code addComment} and
 * {@code setParent}) that cannot be exposed through the public {@link ProtobufStatement} interface.
 *
 * <p>The {@link ProtobufTreeSourceRange} associated with a statement is mutable through the package-private
 * {@link #setRange(ProtobufTreeSourceRange)} setter because the parser knows the start position when the
 * statement is constructed but only learns the end position once the terminator has been consumed.
 *
 * @see ProtobufStatement
 * @see ProtobufStatementWithBodyImpl
 * @see ProtobufTree
 */
sealed abstract class ProtobufStatementImpl
        implements ProtobufStatement
        permits ProtobufCommentStatement, ProtobufEditionStatement, ProtobufEmptyStatement, ProtobufEnumConstantStatement, ProtobufExtensionsStatement, ProtobufFieldStatement, ProtobufGroupStatement, ProtobufImportStatement, ProtobufOneofStatement, ProtobufOptionStatement, ProtobufPackageStatement, ProtobufReservedStatement, ProtobufStatementWithBodyImpl, ProtobufSyntaxStatement {
    private ProtobufTreeSourceRange range;
    private final List<ProtobufCommentStatement> comments;
    ProtobufTree.WithBody<?> parent;

    /**
     * Constructs a new statement implementation with the specified source range.
     *
     * @param range the source range covering this statement
     */
    ProtobufStatementImpl(ProtobufTreeSourceRange range) {
        this.range = range;
        this.comments = new ArrayList<>();
    }

    /**
     * Returns the source range covering this statement.
     *
     * @return the source range
     */
    @Override
    public ProtobufTreeSourceRange range() {
        return range;
    }

    /**
     * Updates the source range covering this statement.
     *
     * <p>The parser uses this method to fill in the end position once it has consumed the
     * statement's terminator.
     *
     * @param range the source range to set
     */
    @Override
    public void setRange(ProtobufTreeSourceRange range) {
        this.range = range;
    }

    /**
     * Returns the comments attached to this statement in insertion order.
     *
     * @return an unmodifiable view of the attached comments
     */
    @Override
    public SequencedCollection<ProtobufCommentStatement> comments() {
        return Collections.unmodifiableList(comments);
    }

    /**
     * Appends a comment to this statement.
     *
     * <p>The parser uses this method to attach leading, trailing, and detached comments captured
     * by the lexer to the statement they belong to.
     *
     * @param comment the comment to append
     */
    @Override
    public void addComment(ProtobufCommentStatement comment) {
        comments.add(comment);
    }

    /**
     * Returns the parent node in the AST.
     *
     * <p>All statements have a parent that contains them, except for the root document.
     * The parent is always a node with a body (container node).
     *
     * @return the parent node, or {@code null} if this is a root node
     */
    @Override
    public ProtobufTree.WithBody<?> parent() {
        return parent;
    }

    /**
     * Checks whether this statement has a parent node.
     *
     * @return {@code true} if a parent is present, {@code false} otherwise
     */
    @Override
    public boolean hasParent() {
        return parent != null;
    }

    /**
     * Sets the parent node for this statement.
     *
     * <p>This method is package-private to prevent external code from breaking AST invariants.
     * It is called automatically when statements are added to or removed from container nodes.
     *
     * @param parent the parent node to set, or {@code null} to clear the parent
     */
    void setParent(ProtobufTree.WithBody<?> parent) {
        this.parent = parent;
    }
}
