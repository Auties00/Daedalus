package com.github.auties00.daedalus.protobuf.compiler.tree;

/**
 * A comment captured by the lexer and attached to a Protocol Buffer AST node.
 *
 * <p>The text payload is normalized to follow {@code protoc}'s comment-extraction rules:
 * <ul>
 * <li>{@code // foo} becomes {@code foo} (the leading {@code //} and at most one space are stripped)
 * <li>{@code /* foo *}{@code /} becomes {@code foo} (markers stripped, single-line trimmed)
 * <li>Multi-line {@code /* ... *}{@code /} comments have each continuation line stripped of leading
 *     whitespace, an optional {@code *}, and an optional single space, then joined with {@code \n}
 * </ul>
 *
 * <p>The accompanying {@link ProtobufTreeSourceRange} covers the entire comment including the markers,
 * allowing tooling to find the original text in the source file.
 *
 * <p>Comment categorization follows {@code protoc}'s {@code Tokenizer::NextWithComments} algorithm:
 * a comment is {@link Trailing} when it appears on the same line as the previously consumed token,
 * {@link Leading} when its block is immediately adjacent (no blank line) to the next token, and
 * {@link LeadingDetached} otherwise.
 *
 * @see Leading
 * @see Trailing
 * @see LeadingDetached
 */
public sealed abstract class ProtobufCommentStatement
        extends ProtobufStatementImpl
        implements ProtobufStatement
        permits ProtobufCommentStatement.Leading, ProtobufCommentStatement.LeadingDetached, ProtobufCommentStatement.Trailing {
    private final String text;

    /**
     * Constructs a new comment statement with the specified text and source range.
     *
     * @param text the normalized comment text
     * @param range the source range covering the comment, including the comment markers
     */
    ProtobufCommentStatement(String text, ProtobufTreeSourceRange range) {
        super(range);
        this.text = text;
    }

    /**
     * Returns the normalized comment text, without comment markers.
     *
     * @return the comment text
     */
    public String text() {
        return text;
    }

    @Override
    public boolean isAttributed() {
        return true;
    }

    /**
     * A leading comment that is immediately adjacent to the AST node it documents.
     */
    public static final class Leading extends ProtobufCommentStatement {
        /**
         * Constructs a new leading comment statement.
         *
         * @param text the normalized comment text
         * @param range the source range covering the comment
         */
        public Leading(String text, ProtobufTreeSourceRange range) {
            super(text, range);
        }
    }

    /**
     * A trailing comment that follows the AST node on the same line as the node's terminator.
     */
    public static final class Trailing extends ProtobufCommentStatement {
        /**
         * Constructs a new trailing comment statement.
         *
         * @param text the normalized comment text
         * @param range the source range covering the comment
         */
        public Trailing(String text, ProtobufTreeSourceRange range) {
            super(text, range);
        }
    }

    /**
     * A detached leading comment that precedes the AST node but is separated from it by at least
     * one blank line.
     */
    public static final class LeadingDetached extends ProtobufCommentStatement {
        /**
         * Constructs a new detached leading comment statement.
         *
         * @param text the normalized comment text
         * @param range the source range covering the comment
         */
        public LeadingDetached(String text, ProtobufTreeSourceRange range) {
            super(text, range);
        }
    }
}
