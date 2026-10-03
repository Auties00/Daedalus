package com.github.auties00.daedalus.protobuf.compiler.tree;

/**
 * Represents a half-open source range covering a Protocol Buffer AST node.
 *
 * <p>{@code start} points at the first character of the first token of the construct, and {@code end}
 * points at the column immediately after the last character of the last token.
 *
 * @param start the inclusive start position
 * @param end the exclusive end position
 */
public record ProtobufTreeSourceRange(ProtobufTreeSourcePosition start, ProtobufTreeSourcePosition end) {
    /**
     * A sentinel range used when no source range is available, such as for synthetic nodes
     * created outside the parser.
     */
    public static final ProtobufTreeSourceRange UNKNOWN = new ProtobufTreeSourceRange(ProtobufTreeSourcePosition.UNKNOWN, ProtobufTreeSourcePosition.UNKNOWN);
}
