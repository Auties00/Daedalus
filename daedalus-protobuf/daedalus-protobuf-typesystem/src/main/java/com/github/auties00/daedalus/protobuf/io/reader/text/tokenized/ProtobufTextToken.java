package com.github.auties00.daedalus.protobuf.io.reader.text.tokenized;

/**
 * Packs and unpacks tokenizer output into a compact {@code long} representation.
 *
 * <p>Each token is encoded as a single {@code long} with the following layout:
 * <pre>
 *   [offset:32][length:28][kind:4]
 * </pre>
 */
public final class ProtobufTextToken {
    static final int KIND_BITS    = 4;
    static final long KIND_MASK   = (1L << KIND_BITS) - 1;
    static final int LENGTH_BITS  = 28;
    static final long LENGTH_MASK = (1L << LENGTH_BITS) - 1;

    private ProtobufTextToken() {
        throw new UnsupportedOperationException("ProtobufTextToken is a utility class and cannot be initialized");
    }

    /**
     * Packs an offset, length, and kind into a single {@code long} token.
     *
     * @param offset the byte offset in the input
     * @param length the byte length of the token
     * @param kind   the token kind (one of the {@link ProtobufTextTokenKind} constants)
     * @return the packed token
     */
    public static long pack(int offset, int length, int kind) {
        return ((long) offset << 32)
                | ((long) (length & (int) LENGTH_MASK) << KIND_BITS)
                | (kind & (int) KIND_MASK);
    }

    /**
     * Extracts the byte offset from a packed token.
     *
     * @param token the packed token
     * @return the byte offset in the input
     */
    public static int offset(long token) {
        return (int) (token >>> 32);
    }

    /**
     * Extracts the byte length from a packed token.
     *
     * @param token the packed token
     * @return the byte length of the token
     */
    public static int length(long token) {
        return (int) ((token >>> KIND_BITS) & LENGTH_MASK);
    }

    /**
     * Extracts the token kind from a packed token.
     *
     * @param token the packed token
     * @return one of the {@link ProtobufTextTokenKind} constants
     */
    public static int kind(long token) {
        return (int) (token & KIND_MASK);
    }
}