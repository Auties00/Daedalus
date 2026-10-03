package com.github.auties00.daedalus.protobuf.compiler.tree;

/**
 * Represents a single position in a Protocol Buffer source file.
 *
 * @param line the 1-indexed line number, or {@code 0} when the position is unknown
 * @param column the 1-indexed column number, or {@code 0} when the position is unknown
 */
public record ProtobufTreeSourcePosition(int line, int column) {
    /**
     * A sentinel position used when no source position is available, such as for synthetic nodes
     * created outside the parser.
     */
    public static final ProtobufTreeSourcePosition UNKNOWN = new ProtobufTreeSourcePosition(0, 0);
}
