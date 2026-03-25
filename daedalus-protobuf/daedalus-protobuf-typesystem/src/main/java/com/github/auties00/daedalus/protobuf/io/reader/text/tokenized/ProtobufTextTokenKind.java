package com.github.auties00.daedalus.protobuf.io.reader.text.tokenized;

/**
 * Token kind constants produced by {@link ProtobufTextTokenizedReader}.
 *
 * <p>Each constant identifies the lexical category of a token in the
 * packed {@code long} tape. The kind occupies the lowest 4 bits of the
 * packed token, allowing up to 16 distinct kinds.
 *
 * <p>IDENT covers field names, enum values, and boolean/special literals
 * (true, false, inf, nan). DSTR and SSTR are double and single quoted
 * strings respectively. NUMERIC covers integer and floating-point
 * literals; the sign is emitted as a separate MINUS token and merged
 * by the consumer. The remaining kinds are single-byte structural
 * characters.
 */
public final class ProtobufTextTokenKind {
    public static final int IDENT         = 0;
    public static final int DSTR          = 1;
    public static final int SSTR          = 2;
    public static final int NUMERIC       = 3;
    public static final int MINUS         = 4;
    public static final int BRACE_OPEN    = 5;
    public static final int BRACE_CLOSE   = 6;
    public static final int ANGLE_OPEN    = 7;
    public static final int ANGLE_CLOSE   = 8;
    public static final int BRACKET_OPEN  = 9;
    public static final int BRACKET_CLOSE = 10;
    public static final int COLON         = 11;
    public static final int SEP           = 12;

    private ProtobufTextTokenKind() {
        throw new UnsupportedOperationException("ProtobufTextTokenKind is a utility class and cannot be initialized");
    }
}