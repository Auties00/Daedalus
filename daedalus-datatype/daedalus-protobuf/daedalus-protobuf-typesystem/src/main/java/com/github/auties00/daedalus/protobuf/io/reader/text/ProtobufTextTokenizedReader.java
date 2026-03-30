// I'm gonna be honest nobody uses prototext, but my intention is to support the entire protobuf spec, so I added support anyway.
// I also kind of wanted to try the simd-json approach of using SIMD to build a lexer.
package com.github.auties00.daedalus.protobuf.io.reader.text;

import com.github.auties00.daedalus.protobuf.exception.ProtobufDeserializationException;
import com.github.auties00.daedalus.protobuf.io.reader.ProtobufTextReader;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

import static com.github.auties00.daedalus.protobuf.io.reader.text.ProtobufTextChars.*;

/**
 * A high-performance text reader for Protocol Buffers text format (textproto)
 * that eagerly tokenizes the entire input using SIMD-accelerated lexing and
 * then walks the resulting token tape to implement the {@link ProtobufTextReader} API.
 *
 * <h2>Overview</h2>
 *
 * <p>The input is processed in 64-byte chunks. Each chunk flows through
 * three phases:
 *
 * <ol>
 *   <li><b>Character Classification (SIMD):</b> Load 64 bytes
 *       using the preferred vector species ({@code SPECIES_PREFERRED}) and
 *       produce per-character-class 64-bit bitmasks via vectorized equality
 *       comparisons. On AVX-512 hardware, a single 64-byte vector load
 *       covers the entire chunk; on AVX2, two 32-byte loads are used; on
 *       128-bit NEON/SSE, four 16-byte loads. The number of loads is
 *       determined at class-init time by {@code VECTORS_PER_CHUNK}.
 *
 *       <p><i>Limitation (Java Vector API):</i> The ideal classification
 *       technique is the double-{@code vpshufb} lookup table used by
 *       simdjson in C/C++, which classifies all bytes in ~4 instructions.
 *       In Java, this would use {@link ByteVector#selectFrom(Vector)},
 *       which since JDK 25 (JDK-8340079) compiles to {@code vpshufb} for
 *       128-bit species. However, the textproto character set causes
 *       nibble-index collisions that produce false positives (e.g. 'z'
 *       falsely classified as STRUCTURAL because it shares nibbles with
 *       '{' and ':'). The collision-free LUT design requires either a
 *       restricted character set (like JSON's 6 structural characters) or
 *       post-LUT fixup comparisons that negate most of the savings. We
 *       therefore use explicit equality comparisons, which produce more
 *       vector ops per chunk but are correct by construction.</p>
 *
 *   <li><b>String and Comment Masking:</b> Compute 64-bit
 *       bitmasks for "inside double-quoted string", "inside single-quoted
 *       string", and "inside comment" regions.
 *
 *       <ul>
 *         <li><i>Escape detection:</i> Uses the simdjson carry-addition
 *             technique to find bytes preceded by an odd number of
 *             consecutive backslashes, entirely without loops.</li>
 *         <li><i>String regions:</i> Prefix XOR (cumulative XOR) on
 *             unescaped quote positions toggles an "in-string" bit at
 *             each quote. This is computed via a cascading shift-XOR
 *             chain (6 shifts + 6 XORs = 12 instructions).
 *
 *             <p><i>Limitation (Java Vector API):</i> The optimal
 *             implementation uses a single {@code PCLMULQDQ} (carry-less
 *             multiply) instruction, reducing the prefix XOR to ~3 cycles.
 *             The Java Vector API does not expose {@code CLMUL}.</p></li>
 *         <li><i>Comment regions:</i> Each {@code #} outside a string
 *             opens a comment that extends to the next newline.</li>
 *       </ul>
 *
 *   <li><b>Token Extraction and Emission:</b> Combine bitmasks
 *       to identify token boundaries, classify each token by examining
 *       which category bitmask it belongs to, and write packed tokens to
 *       the tape in input order via {@code tzcnt}/{@code blsr}.</li>
 * </ol>
 */
public final class ProtobufTextTokenizedReader extends ProtobufTextReader {
    // Each token is encoded as a single {@code long} with the following layout:
    // [offset:32][length:28][kind:4]
    private static final int KIND_BITS    = 4;
    private static final long KIND_MASK   = (1L << KIND_BITS) - 1;
    private static final int LENGTH_BITS  = 28;
    private static final long LENGTH_MASK = (1L << LENGTH_BITS) - 1;

    // Each constant identifies the lexical category of a token in the
    // packed long tape. The kind occupies the lowest 4 bits of the
    // packed token, allowing up to 16 distinct kinds.
    private static final int IDENT         = 0;
    private static final int DSTR          = 1;
    private static final int SSTR          = 2;
    private static final int NUMERIC       = 3;
    private static final int MINUS         = 4;
    private static final int BRACE_OPEN    = 5;
    private static final int BRACE_CLOSE   = 6;
    private static final int ANGLE_OPEN    = 7;
    private static final int ANGLE_CLOSE   = 8;
    private static final int BRACKET_OPEN  = 9;
    private static final int BRACKET_CLOSE = 10;
    private static final int COLON         = 11;
    private static final int SEP           = 12;


    /**
     * Preferred vector species selected at class-init time. Uses the widest
     * SIMD registers available on the current CPU: 512-bit (AVX-512),
     * 256-bit (AVX2), or 128-bit (SSE/NEON). This determines how many
     * vector loads are needed per 64-byte chunk.
     */
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    /** Number of bytes processed per single vector load. */
    private static final int VECTOR_LENGTH = SPECIES.length();

    /**
     * Number of vector loads required to cover one 64-byte chunk.
     * 1 for AVX-512 (64B), 2 for AVX2 (32B), 4 for SSE/NEON (16B).
     */
    private static final int VECTORS_PER_CHUNK = Long.SIZE / VECTOR_LENGTH;

    /**
     * Bitmask with the lower VECTOR_LENGTH bits set. Used to isolate the
     * meaningful bits from {@code VectorMask.toLong()} before shifting
     * them into position in the 64-bit chunk mask.
     */
    private static final long VECTOR_BITMASK = (VECTOR_LENGTH == 64) ? -1L : ((1L << VECTOR_LENGTH) - 1);

    // Character class flags (bit positions in the classification byte).
    // Each flag occupies one bit so a single CHAR_CLASS[byte] lookup can
    // indicate membership in multiple classes simultaneously.
    private static final byte CHAR_WHITESPACE   = 1;
    private static final byte CHAR_STRUCTURAL   = 2;
    private static final byte CHAR_DQUOTE       = 4;
    private static final byte CHAR_SQUOTE       = 8;
    private static final byte CHAR_HASH         = 16;
    private static final byte CHAR_NEWLINE      = 32;
    private static final byte CHAR_BACKSLASH    = 64;
    private static final byte CHAR_DIGIT_OR_DOT = (byte) 128;

    /**
     * Scalar classification table: maps each unsigned byte value (0 to 255) to a
     * bitmask of character classes. Used by the scalar fallback path for
     * tail chunks smaller than 64 bytes, and during token emission to
     * distinguish NUMERIC from IDENT content runs.
     */
    private static final byte[] CHAR_CLASS = new byte[256];

    /**
     * Maps structural ASCII byte values to their corresponding token kind
     * constants. Only entries for the 10 structural characters are
     * meaningful; all others are zero.
     */
    private static final int[] STRUCTURAL_KIND = new int[128];

    // Broadcast vectors for SIMD equality comparisons.
    // Each vector holds VECTOR_LENGTH copies of a single byte value.
    // Comparing an input vector against a broadcast vector produces a
    // VECTOR_LENGTH-bit mask of matching positions.
    private static final ByteVector V_SPACE, V_TAB, V_CR, V_LF;
    private static final ByteVector V_LBRACE, V_RBRACE, V_LANGLE, V_RANGLE;
    private static final ByteVector V_LBRACKET, V_RBRACKET, V_COLON, V_COMMA, V_SEMI, V_MINUS;
    private static final ByteVector V_DQUOTE, V_SQUOTE, V_HASH, V_BACKSLASH;

    static {
        // Populate the scalar classification table
        CHAR_CLASS[SPACE] = CHAR_WHITESPACE;
        CHAR_CLASS[TAB]   = CHAR_WHITESPACE;
        CHAR_CLASS[CR]    = CHAR_WHITESPACE;
        CHAR_CLASS[LF]    = (byte) (CHAR_WHITESPACE | CHAR_NEWLINE);

        CHAR_CLASS[LBRACE]   = CHAR_STRUCTURAL;
        CHAR_CLASS[RBRACE]   = CHAR_STRUCTURAL;
        CHAR_CLASS[LANGLE]   = CHAR_STRUCTURAL;
        CHAR_CLASS[RANGLE]   = CHAR_STRUCTURAL;
        CHAR_CLASS[LBRACKET] = CHAR_STRUCTURAL;
        CHAR_CLASS[RBRACKET] = CHAR_STRUCTURAL;
        CHAR_CLASS[COLON]    = CHAR_STRUCTURAL;
        CHAR_CLASS[COMMA]    = CHAR_STRUCTURAL;
        CHAR_CLASS[SEMICOLON]= CHAR_STRUCTURAL;
        CHAR_CLASS[MINUS]    = CHAR_STRUCTURAL;

        CHAR_CLASS[DQUOTE]    = CHAR_DQUOTE;
        CHAR_CLASS[SQUOTE]    = CHAR_SQUOTE;
        CHAR_CLASS[HASH]      = CHAR_HASH;
        CHAR_CLASS[BACKSLASH] = CHAR_BACKSLASH;

        for (int c = '0'; c <= '9'; c++) {
            CHAR_CLASS[c] = CHAR_DIGIT_OR_DOT;
        }
        CHAR_CLASS[DOT] = CHAR_DIGIT_OR_DOT;

        // Populate the structural-to-kind mapping
        STRUCTURAL_KIND[LBRACE]   = BRACE_OPEN;
        STRUCTURAL_KIND[RBRACE]   = BRACE_CLOSE;
        STRUCTURAL_KIND[LANGLE]   = ANGLE_OPEN;
        STRUCTURAL_KIND[RANGLE]   = ANGLE_CLOSE;
        STRUCTURAL_KIND[LBRACKET] = BRACKET_OPEN;
        STRUCTURAL_KIND[RBRACKET] = BRACKET_CLOSE;
        STRUCTURAL_KIND[COLON]    = COLON;
        STRUCTURAL_KIND[COMMA]    = SEP;
        STRUCTURAL_KIND[SEMICOLON]= SEP;
        STRUCTURAL_KIND[MINUS]    = MINUS;

        // Pre-compute broadcast vectors
        V_SPACE    = ByteVector.broadcast(SPECIES, (byte) SPACE);
        V_TAB      = ByteVector.broadcast(SPECIES, (byte) TAB);
        V_CR       = ByteVector.broadcast(SPECIES, (byte) CR);
        V_LF       = ByteVector.broadcast(SPECIES, (byte) LF);
        V_LBRACE   = ByteVector.broadcast(SPECIES, (byte) LBRACE);
        V_RBRACE   = ByteVector.broadcast(SPECIES, (byte) RBRACE);
        V_LANGLE   = ByteVector.broadcast(SPECIES, (byte) LANGLE);
        V_RANGLE   = ByteVector.broadcast(SPECIES, (byte) RANGLE);
        V_LBRACKET = ByteVector.broadcast(SPECIES, (byte) LBRACKET);
        V_RBRACKET = ByteVector.broadcast(SPECIES, (byte) RBRACKET);
        V_COLON    = ByteVector.broadcast(SPECIES, (byte) COLON);
        V_COMMA    = ByteVector.broadcast(SPECIES, (byte) COMMA);
        V_SEMI     = ByteVector.broadcast(SPECIES, (byte) SEMICOLON);
        V_MINUS    = ByteVector.broadcast(SPECIES, (byte) MINUS);
        V_DQUOTE   = ByteVector.broadcast(SPECIES, (byte) DQUOTE);
        V_SQUOTE   = ByteVector.broadcast(SPECIES, (byte) SQUOTE);
        V_HASH     = ByteVector.broadcast(SPECIES, (byte) HASH);
        V_BACKSLASH= ByteVector.broadcast(SPECIES, (byte) BACKSLASH);
    }

    // Parity masks for escape detection.
    // EVEN_BITS has bits 0, 2, 4, ..., 62 set. ODD_BITS has bits 1, 3, 5, ..., 63 set.
    // Used to split backslash-run starts by position parity during the
    // carry-addition escape detection algorithm.
    private static final long EVEN_BITS = 0x5555_5555_5555_5555L;
    private static final long ODD_BITS = 0xAAAA_AAAA_AAAA_AAAAL;

    private static final ValueLayout.OfByte JAVA_BYTE = ValueLayout.JAVA_BYTE;

    private final MemorySegment input;
    private final int inputOffset;
    private final int inputLimit;
    private final LongBuffer tape;
    private int tapePosition;

    /**
     * Constructs a new reader backed by the specified {@link MemorySegment}.
     *
     * @param input  the memory segment to read
     * @param offset the byte offset at which reading begins
     * @param limit  the byte offset at which reading ends (exclusive)
     */
    public ProtobufTextTokenizedReader(MemorySegment input, int offset, int limit) {
        this.input = input;
        this.inputOffset = offset;
        this.inputLimit = limit;
        this.tape = tokenize();
    }

    /**
     * Constructs a new reader for the entire {@link MemorySegment}.
     *
     * @param input the memory segment to read
     */
    public ProtobufTextTokenizedReader(MemorySegment input) {
        this(input, 0, (int) input.byteSize());
    }

    /**
     * Constructs a new reader backed by the specified byte array range.
     *
     * @param input  the byte array to read
     * @param offset the byte offset at which reading begins
     * @param limit  the byte offset at which reading ends (exclusive)
     */
    public ProtobufTextTokenizedReader(byte[] input, int offset, int limit) {
        this(MemorySegment.ofArray(input), offset, limit);
    }

    /**
     * Constructs a new reader for the entire byte array.
     *
     * @param input the byte array to read
     */
    public ProtobufTextTokenizedReader(byte[] input) {
        this(input, 0, input.length);
    }

    /**
     * Constructs a new reader backed by the specified {@link ByteBuffer}.
     *
     * <p>The buffer's contents from {@link ByteBuffer#position() position}
     * to {@link ByteBuffer#limit() limit} are read. The buffer's position
     * is not modified.
     *
     * @param input the byte buffer to read
     */
    public ProtobufTextTokenizedReader(ByteBuffer input) {
        this(MemorySegment.ofBuffer(input), input.position(), input.limit());
    }

    /**
     * Constructs a new reader for the UTF-8 encoding of the specified
     * {@link CharSequence}.
     *
     * <p>The character sequence is encoded to UTF-8 before tokenization.
     * Token offsets are byte offsets into the UTF-8 encoding, which are
     * identical to character offsets for ASCII-only input.
     *
     * @param input the character sequence to read
     */
    public ProtobufTextTokenizedReader(CharSequence input) {
        // TODO: Would be cool to have some way to not have to UTF-8 encode the CharSequence
        //       Problem is strings are UTF-16 encoded internally in Java and MemorySegment.ofBuffer doens't support Buffers that don't have a backing array(e.g. CharBuffer)
        this(input.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not
     *         {@code &#123;} or {@code <}
     */
    @Override
    public void readStartObject() {
        var kind = tokenKind(next());
        if (kind != BRACE_OPEN && kind != ANGLE_OPEN) {
            throw new ProtobufDeserializationException("Expected '{' or '<' to start object");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not
     *         {@code &#125;} or {@code >}
     */
    @Override
    public void readEndObject() {
        var kind = tokenKind(next());
        if (kind != BRACE_CLOSE && kind != ANGLE_CLOSE) {
            throw new ProtobufDeserializationException("Expected '}' or '>' to end object");
        }
        skipOptionalSeparator();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not {@code [}
     */
    @Override
    public void readStartArray() {
        if (tokenKind(next()) != BRACKET_OPEN) {
            throw new ProtobufDeserializationException("Expected '[' to start array");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ProtobufDeserializationException if the next token is not {@code ]}
     */
    @Override
    public void readEndArray() {
        if (tokenKind(next()) != BRACKET_CLOSE) {
            throw new ProtobufDeserializationException("Expected ']' to end array");
        }
        skipOptionalSeparator();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code false} if the tape is exhausted or the next token
     * is not an identifier. If an identifier is found, the optional colon
     * separator after the field name is also consumed.
     */
    @Override
    public boolean readPropertyName() {
        if (!hasRemaining() || peekKind() != IDENT) {
            return false;
        }
        var token = next();
        propertyName = tokenText(token);
        if (hasRemaining() && peekKind() == COLON) {
            next();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished() {
        return !hasRemaining();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles scalar values, signed numerics, adjacent string
     * concatenation, and nested objects or arrays at arbitrary depth.
     */
    @Override
    public void skipUnknownProperty() {
        if (!hasRemaining()) {
            return;
        }

        switch (peekKind()) {
            case BRACE_OPEN -> skipBlock(BRACE_OPEN, BRACE_CLOSE);
            case ANGLE_OPEN -> skipBlock(ANGLE_OPEN, ANGLE_CLOSE);
            case BRACKET_OPEN -> skipBlock(BRACKET_OPEN, BRACKET_CLOSE);
            case MINUS -> {
                next();
                next();
                skipOptionalSeparator();
            }
            case DSTR, SSTR -> {
                do {
                    next();
                } while (hasRemaining() && (peekKind() == DSTR || peekKind() == SSTR));
                skipOptionalSeparator();
            }
            default -> {
                next();
                skipOptionalSeparator();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloatProperty() {
        var negative = consumeOptionalMinus();
        var token = next();
        var kind = tokenKind(token);
        float value;
        if (kind == IDENT) {
            var text = tokenText(token);
            value = switch (text) {
                case "inf", "Inf", "infinity", "Infinity" -> Float.POSITIVE_INFINITY;
                case "nan", "NaN" -> Float.NaN;
                default -> throw new ProtobufDeserializationException("Expected float value");
            };
        } else if (kind == NUMERIC) {
            value = parseFloat(token);
        } else {
            throw new ProtobufDeserializationException("Expected float value");
        }
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    // TODO: Improve performance
    private float parseFloat(long token) {
        return Float.parseFloat(tokenText(token));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDoubleProperty() {
        var negative = consumeOptionalMinus();
        var token = next();
        var kind = tokenKind(token);
        double value;
        if (kind == IDENT) {
            var text = tokenText(token);
            value = switch (text) {
                case "inf", "Inf", "infinity", "Infinity" -> Double.POSITIVE_INFINITY;
                case "nan", "NaN" -> Double.NaN;
                default -> throw new ProtobufDeserializationException("Expected double value");
            };
        } else if (kind == NUMERIC) {
            value = parseDouble(token);
        } else {
            throw new ProtobufDeserializationException("Expected double value");
        }
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    // TODO: Improve performance
    private double parseDouble(long token) {
        return Double.parseDouble(tokenText(token));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSInt32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readFixed32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readSFixed32Property() {
        return (int) readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readUInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSInt64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readFixed64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readSFixed64Property() {
        return readRawInteger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts identifier literals ({@code true}, {@code false} and
     * common variants) as well as numeric values ({@code 0} for false,
     * non-zero for true).
     *
     * @throws ProtobufDeserializationException if the next token is not a
     *         valid boolean representation
     */
    @Override
    public boolean readBoolProperty() {
        var token = next();
        var kind = tokenKind(token);
        boolean result;
        if (kind == IDENT) {
            var text = tokenText(token);
            result = switch (text) {
                case "true", "True", "TRUE", "t" -> true;
                case "false", "False", "FALSE", "f" -> false;
                default -> throw new ProtobufDeserializationException(
                        "Expected boolean value, got: " + text);
            };
        } else if (kind == NUMERIC) {
            result = !tokenText(token).equals("0");
        } else {
            throw new ProtobufDeserializationException("Expected boolean value");
        }
        skipOptionalSeparator();
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles C-style escape sequences and adjacent string concatenation
     * (consecutive string tokens are merged into one value).
     *
     * @throws ProtobufDeserializationException if the next token is not a string
     */
    @Override
    public String readStringProperty() {
        var token = next();
        var kind = tokenKind(token);
        if (kind != DSTR && kind != SSTR) {
            throw new ProtobufDeserializationException("Expected string value");
        }
        var sb = new StringBuilder();
        unescapeStringInto(sb, token);
        while (hasRemaining() && (peekKind() == DSTR || peekKind() == SSTR)) {
            unescapeStringInto(sb, next());
        }
        skipOptionalSeparator();
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads one or more adjacent string tokens and decodes their
     * content as raw bytes with C-style escape sequences (octal, hex).
     *
     * @throws ProtobufDeserializationException if the next token is not a string
     */
    @Override
    public byte[] readBytesProperty() {
        var token = next();
        var kind = tokenKind(token);
        if (kind != DSTR && kind != SSTR) {
            throw new ProtobufDeserializationException("Expected string value for bytes field");
        }
        var out = new ByteArrayOutputStream();
        unescapeBytesInto(out, token);
        while (hasRemaining() && (peekKind() == DSTR || peekKind() == SSTR)) {
            unescapeBytesInto(out, next());
        }
        skipOptionalSeparator();
        return out.toByteArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation is a no-op since all data is in memory.
     */
    @Override
    public void close() {
    }

    private boolean hasRemaining() {
        return tapePosition < tape.limit();
    }

    private long next() {
        return tape.get(tapePosition++);
    }

    private int peekKind() {
        return tokenKind(tape.get(tapePosition));
    }

    private String tokenText(long token) {
        var offset = tokenOffset(token);
        var length = tokenLength(token);
        var bytes = new byte[length];
        MemorySegment.copy(input, JAVA_BYTE, offset, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void skipOptionalSeparator() {
        if (hasRemaining() && peekKind() == SEP) {
            next();
        }
    }

    private void skipBlock(int openKind, int closeKind) {
        next();
        var depth = 1;
        while (depth > 0 && hasRemaining()) {
            var kind = tokenKind(next());
            if (kind == openKind) {
                depth++;
            } else if (kind == closeKind) {
                depth--;
            }
        }
        skipOptionalSeparator();
    }

    private boolean consumeOptionalMinus() {
        if (hasRemaining() && peekKind() == MINUS) {
            next();
            return true;
        }
        return false;
    }

    private long readRawInteger() {
        var negative = consumeOptionalMinus();
        var token = next();
        if (tokenKind(token) != NUMERIC) {
            throw new ProtobufDeserializationException("Expected numeric value");
        }
        var value = parseInteger(tokenText(token));
        skipOptionalSeparator();
        return negative ? -value : value;
    }

    // TODO: Improve performance
    private static long parseInteger(String text) {
        if (text.length() > 2 && (text.startsWith("0x") || text.startsWith("0X"))) {
            return Long.parseUnsignedLong(text.substring(2), 16);
        }
        if (text.length() > 1 && text.charAt(0) == '0' && text.charAt(1) >= '0' && text.charAt(1) <= '7') {
            return Long.parseUnsignedLong(text, 8);
        }
        return Long.parseLong(text);
    }

    private void unescapeStringInto(StringBuilder sb, long token) {
        var offset = tokenOffset(token) + 1;
        var length = tokenLength(token) - 2;
        var end = offset + length;
        var pos = offset;
        while (pos < end) {
            var b = input.get(JAVA_BYTE, pos) & 0xFF;
            if (b != '\\') {
                if (b < 0x80) {
                    sb.append((char) b);
                    pos++;
                } else {
                    int codePoint;
                    int seqLen;
                    if ((b & 0xE0) == 0xC0) {
                        seqLen = 2;
                        codePoint = b & 0x1F;
                    } else if ((b & 0xF0) == 0xE0) {
                        seqLen = 3;
                        codePoint = b & 0x0F;
                    } else {
                        seqLen = 4;
                        codePoint = b & 0x07;
                    }
                    for (var j = 1; j < seqLen && (pos + j) < end; j++) {
                        codePoint = (codePoint << 6) | (input.get(JAVA_BYTE, pos + j) & 0x3F);
                    }
                    sb.appendCodePoint(codePoint);
                    pos += seqLen;
                }
                continue;
            }
            pos++;
            if (pos >= end) break;
            pos = unescapeCharInto(sb, pos, end);
        }
    }

    private int unescapeCharInto(StringBuilder sb, int pos, int end) {
        var esc = input.get(JAVA_BYTE, pos) & 0xFF;
        pos++;
        switch (esc) {
            case 'a' -> sb.append(ALERT);
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'v' -> sb.append(VTAB);
            case '\\' -> sb.append('\\');
            case '\'' -> sb.append('\'');
            case '"' -> sb.append('"');
            case '?' -> sb.append('?');
            case 'x', 'X' -> {
                var hex = 0;
                for (var d = 0; d < 2 && pos < end; d++) {
                    var digit = hexDigit(input.get(JAVA_BYTE, pos) & 0xFF);
                    if (digit < 0) break;
                    hex = (hex << 4) | digit;
                    pos++;
                }
                sb.append((char) hex);
            }
            case 'u' -> {
                var cp = 0;
                for (var d = 0; d < 4 && pos < end; d++) {
                    cp = (cp << 4) | hexDigit(input.get(JAVA_BYTE, pos) & 0xFF);
                    pos++;
                }
                sb.append((char) cp);
            }
            case 'U' -> {
                var cp = 0;
                for (var d = 0; d < 8 && pos < end; d++) {
                    cp = (cp << 4) | hexDigit(input.get(JAVA_BYTE, pos) & 0xFF);
                    pos++;
                }
                sb.appendCodePoint(cp);
            }
            default -> {
                if (esc >= '0' && esc <= '7') {
                    var oct = esc - '0';
                    for (var d = 1; d < 3 && pos < end; d++) {
                        var digit = input.get(JAVA_BYTE, pos) & 0xFF;
                        if (digit < '0' || digit > '7') break;
                        oct = (oct << 3) | (digit - '0');
                        pos++;
                    }
                    sb.append((char) oct);
                } else {
                    sb.append('\\');
                    sb.append((char) esc);
                }
            }
        }
        return pos;
    }

    private void unescapeBytesInto(ByteArrayOutputStream out, long token) {
        var offset = tokenOffset(token) + 1;
        var length = tokenLength(token) - 2;
        var end = offset + length;
        var pos = offset;
        while (pos < end) {
            var b = input.get(JAVA_BYTE, pos) & 0xFF;
            if (b != '\\') {
                out.write(b);
                pos++;
                continue;
            }
            pos++;
            if (pos >= end) break;
            pos = unescapeByteInto(out, pos, end);
        }
    }

    private int unescapeByteInto(ByteArrayOutputStream out, int pos, int end) {
        var esc = input.get(JAVA_BYTE, pos) & 0xFF;
        pos++;
        switch (esc) {
            case 'a' -> out.write(0x07);
            case 'b' -> out.write(0x08);
            case 'f' -> out.write(0x0C);
            case 'n' -> out.write(0x0A);
            case 'r' -> out.write(0x0D);
            case 't' -> out.write(0x09);
            case 'v' -> out.write(0x0B);
            case '\\' -> out.write('\\');
            case '\'' -> out.write('\'');
            case '"' -> out.write('"');
            case '?' -> out.write('?');
            case 'x', 'X' -> {
                var hex = 0;
                for (var d = 0; d < 2 && pos < end; d++) {
                    var digit = hexDigit(input.get(JAVA_BYTE, pos) & 0xFF);
                    if (digit < 0) break;
                    hex = (hex << 4) | digit;
                    pos++;
                }
                out.write(hex);
            }
            default -> {
                if (esc >= '0' && esc <= '7') {
                    var oct = esc - '0';
                    for (var d = 1; d < 3 && pos < end; d++) {
                        var digit = input.get(JAVA_BYTE, pos) & 0xFF;
                        if (digit < '0' || digit > '7') break;
                        oct = (oct << 3) | (digit - '0');
                        pos++;
                    }
                    out.write(oct);
                } else {
                    out.write('\\');
                    out.write(esc);
                }
            }
        }
        return pos;
    }

    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    /**
     * Tokenizes the input and returns a read-only {@link LongBuffer}
     * containing packed tokens. Each token is a {@code long} produced by
     * {@link #packToken(int, int, int)}.
     *
     * <p>The buffer's position is 0 and its limit equals the token count.
     * The backing array is overallocated (up to {@code inputLength} entries)
     * and the buffer's limit hides the unused tail. This single-pass design
     * avoids recomputing SIMD bitmasks twice, which would be the dominant
     * cost of a two-pass (count-then-emit) approach.
     *
     * @return a read-only LongBuffer of packed tokens in input order
     */
    private LongBuffer tokenize() {
        var length = inputLimit - inputOffset;
        // Worst case: every input byte is a token (e.g., "{}{}{}{}").
        // TODO: Benchmark whether a two pass approach is superior
        var tapeArray = new long[Math.max(length, 1)];
        var count = tokenizeSinglePass(tapeArray);
        return LongBuffer.wrap(tapeArray, 0, count)
                .asReadOnlyBuffer();
    }

    /**
     * Processes all input chunks and writes tokens into the pre-allocated
     * tape. Returns the number of tokens written.
     *
     * <p>State is carried across chunks via a {@link ParseState} object
     * that holds the three carry bits (dquote open, squote open, in-comment)
     * plus the pending cross-chunk string information.
     */
    private int tokenizeSinglePass(long[] tape) {
        var length = inputLimit - inputOffset;
        var fullChunks = length >>> 6; // length / 64
        var pos = inputOffset;
        var tapePos = 0;
        var state = new ParseState();

        // Full 64-byte chunks: SIMD path
        for (var i = 0; i < fullChunks; i++) {
            tapePos = processChunkSIMD(pos, state, tape, tapePos);
            pos += 64;
        }

        // Tail chunk (< 64 bytes): scalar fallback
        var remaining = inputLimit - pos;
        if (remaining > 0) {
            tapePos = processChunkScalar(pos, remaining, state, tape, tapePos);
        }

        // Handle unclosed content run at end of input.
        if (state.pendingContentStart >= 0) {
            var len = inputLimit - state.pendingContentStart;
            tape[tapePos++] = packToken(state.pendingContentStart, len, state.pendingContentKind);
        }

        // Handle unclosed string at end of input.
        // If a string was opened but never closed (malformed input or string
        // extending to end of file), emit it with length to end of input.
        if (state.pendingStringStart >= 0) {
            var len = inputLimit - state.pendingStringStart;
            tape[tapePos++] = packToken(state.pendingStringStart, len, state.pendingStringKind);
        }

        return tapePos;
    }

    // SIMD character classification (64 bytes via N vector loads)

    /**
     * Classifies all 64 bytes in a chunk using vectorized equality
     * comparisons and produces per-character-class 64-bit bitmasks.
     *
     * <p>The chunk is loaded using {@code VECTORS_PER_CHUNK} vector loads
     * of {@code VECTOR_LENGTH} bytes each. On AVX-512, this is a single
     * 64-byte load. On AVX2, two 32-byte loads. On 128-bit NEON/SSE,
     * four 16-byte loads. Each load's comparison results are shifted into
     * the correct bit positions of the 64-bit chunk mask.
     *
     * <p>Each character class is detected by comparing the input vector
     * against broadcast vectors of the target byte. Multiple comparisons
     * for the same class are ORed together.
     */
    private int processChunkSIMD(int chunkStart, ParseState state,
                                 long[] tape, int tapePos) {
        long whitespace = 0, structural = 0, dquote = 0, squote = 0;
        long hash = 0, newline = 0, backslash = 0;

        for (int v = 0; v < VECTORS_PER_CHUNK; v++) {
            var offset = chunkStart + v * VECTOR_LENGTH;
            var vec = ByteVector.fromMemorySegment(SPECIES, input, offset, ByteOrder.nativeOrder());
            var shift = v * VECTOR_LENGTH;

            // Whitespace: SP | HT | CR | LF
            var wsMask = vec.compare(VectorOperators.EQ, V_SPACE)
                    .or(vec.compare(VectorOperators.EQ, V_TAB))
                    .or(vec.compare(VectorOperators.EQ, V_CR))
                    .or(vec.compare(VectorOperators.EQ, V_LF))
                    .toLong();
            whitespace |= (wsMask & VECTOR_BITMASK) << shift;

            // Newline (subset of whitespace, needed for comment detection)
            var nlMask = vec.compare(VectorOperators.EQ, V_LF).toLong();
            newline |= (nlMask & VECTOR_BITMASK) << shift;

            // Structural characters: { } < > [ ] : , ; -
            // Minus is treated as structural so that "-42" tokenizes as
            // MINUS + NUMERIC("42"); the consumer merges them if needed.
            var stMask = vec.compare(VectorOperators.EQ, V_LBRACE)
                    .or(vec.compare(VectorOperators.EQ, V_RBRACE))
                    .or(vec.compare(VectorOperators.EQ, V_LANGLE))
                    .or(vec.compare(VectorOperators.EQ, V_RANGLE))
                    .or(vec.compare(VectorOperators.EQ, V_LBRACKET))
                    .or(vec.compare(VectorOperators.EQ, V_RBRACKET))
                    .or(vec.compare(VectorOperators.EQ, V_COLON))
                    .or(vec.compare(VectorOperators.EQ, V_COMMA))
                    .or(vec.compare(VectorOperators.EQ, V_SEMI))
                    .or(vec.compare(VectorOperators.EQ, V_MINUS))
                    .toLong();
            structural |= (stMask & VECTOR_BITMASK) << shift;

            // Single-character classes: one comparison each
            dquote    |= (vec.compare(VectorOperators.EQ, V_DQUOTE).toLong()    & VECTOR_BITMASK) << shift;
            squote    |= (vec.compare(VectorOperators.EQ, V_SQUOTE).toLong()    & VECTOR_BITMASK) << shift;
            hash      |= (vec.compare(VectorOperators.EQ, V_HASH).toLong()      & VECTOR_BITMASK) << shift;
            backslash |= (vec.compare(VectorOperators.EQ, V_BACKSLASH).toLong() & VECTOR_BITMASK) << shift;
        }

        // Delegate to shared masking and extraction logic
        return processMaskingAndExtraction(
                whitespace, structural, dquote, squote, hash, newline, backslash,
                -1L, // validMask: all 64 bits active
                64,  // chunkLen
                chunkStart,
                state, tape, tapePos);
    }

    // Scalar classification fallback (for tail < 64 bytes)

    /**
     * Classifies the tail of the input (fewer than 64 bytes) using the
     * scalar {@link #CHAR_CLASS} lookup table. Produces the same bitmasks
     * as the SIMD path, then delegates to the shared masking and extraction logic.
     *
     * <p>Since the tail is at most 63 bytes and runs once per tokenization,
     * the performance impact is negligible.
     */
    private int processChunkScalar(int chunkStart, int chunkLen,
                                   ParseState state,
                                   long[] tape, int tapePos) {
        long whitespace = 0, structural = 0, dquote = 0, squote = 0;
        long hash = 0, newline = 0, backslash = 0;

        for (var i = 0; i < chunkLen; i++) {
            var cls = CHAR_CLASS[input.get(JAVA_BYTE, chunkStart + i) & 0xFF];
            var bit = 1L << i;
            if ((cls & CHAR_WHITESPACE)   != 0) whitespace  |= bit;
            if ((cls & CHAR_STRUCTURAL)   != 0) structural  |= bit;
            if ((cls & CHAR_DQUOTE)       != 0) dquote      |= bit;
            if ((cls & CHAR_SQUOTE)       != 0) squote      |= bit;
            if ((cls & CHAR_HASH)         != 0) hash        |= bit;
            if ((cls & CHAR_NEWLINE)      != 0) newline     |= bit;
            if ((cls & CHAR_BACKSLASH)    != 0) backslash   |= bit;
        }

        // validMask has exactly chunkLen bits set (bits 0..chunkLen-1).
        var validMask = (chunkLen == 64) ? -1L : ((1L << chunkLen) - 1);

        return processMaskingAndExtraction(
                whitespace, structural, dquote, squote, hash, newline, backslash,
                validMask, chunkLen, chunkStart,
                state, tape, tapePos);
    }

    // String/comment masking + token extraction

    /**
     * Shared logic for string/comment region detection and token boundary
     * extraction and emission. Called by both the SIMD and scalar
     * classification paths.
     *
     * @param whitespace  64-bit mask of whitespace byte positions
     * @param structural  64-bit mask of structural character positions
     * @param dquote      64-bit mask of double-quote positions
     * @param squote      64-bit mask of single-quote positions
     * @param hash        64-bit mask of '#' positions
     * @param newline     64-bit mask of '\n' positions
     * @param backslash   64-bit mask of '\\' positions
     * @param validMask   bits set for valid positions (all 1s for 64-byte
     *                    chunks, lower chunkLen bits for tail chunks)
     * @param chunkLen    number of valid bytes in this chunk (≤ 64)
     * @param chunkStart  absolute byte offset of this chunk in the input
     * @param state       mutable cross-chunk state (carries + pending string)
     * @param tape        output tape array
     * @param tapePos     current write position in the tape
     * @return the new tapePos after emitting tokens from this chunk
     */
    private int processMaskingAndExtraction(long whitespace, long structural,
                                            long dquote, long squote,
                                            long hash, long newline, long backslash,
                                            long validMask, int chunkLen, int chunkStart,
                                            ParseState state,
                                            long[] tape, int tapePos) {

        // Escape detection with cross-chunk carry
        //
        // A byte at position i is "escaped" if it is preceded by an
        // odd-length run of consecutive backslashes. Computed entirely
        // with bitwise arithmetic using the simdjson carry-addition technique.
        //
        // Cross-chunk carry: if the previous chunk ended with an odd-length
        // backslash run, the first byte of this chunk may be escaped or
        // may extend that run.
        var escaped = computeEscaped(backslash) & validMask;

        if (state.carryEscaped != 0) {
            if ((backslash & 1) == 0) {
                // Position 0 is not a backslash, so it IS escaped
                // (follows an odd-length run from the previous chunk)
                escaped |= 1L;
            } else {
                // Position 0 is a backslash, extending the previous chunk's run.
                // The carry-addition computed the run starting at position 0
                // as a standalone run. Since the carry adds an odd prefix from
                // the previous chunk, the parity of the result at the first
                // position after the run flips.
                var runExtension = Long.numberOfTrailingZeros(~backslash);
                if (runExtension < chunkLen) {
                    escaped ^= (1L << runExtension);
                }
            }
        }

        // Compute carry for next chunk: 1 if this chunk ends with an
        // odd-length backslash run (accounting for carry from previous chunk)
        long nextCarryEscaped = 0;
        if (chunkLen > 0 && ((backslash >>> (chunkLen - 1)) & 1) != 0) {
            // Count consecutive backslashes ending at position chunkLen-1
            var runLen = Long.numberOfLeadingZeros(~backslash << (64 - chunkLen));
            // If the run extends all the way to position 0 and we had a carry,
            // the total run includes the previous chunk's odd-length prefix
            var touchesBit0 = (runLen >= chunkLen);
            if (touchesBit0 && state.carryEscaped != 0) {
                nextCarryEscaped = ((runLen + 1) & 1);
            } else {
                nextCarryEscaped = (runLen & 1);
            }
        }

        // String region detection with cross-quote filtering
        //
        // The two quote types (double and single) must be aware of each other:
        // a single-quote inside a double-quoted string is a literal byte, not
        // a string delimiter, and vice versa.
        //
        // To handle this correctly, we scan all unescaped quotes left-to-right
        // and track which quote type is currently "open." Quotes of the other
        // type inside an open string are suppressed. This produces filtered
        // quote bitmasks where cross-type quotes have been removed.
        //
        // The scan is O(number of quotes), which is typically very small.
        var unescapedDquote = dquote & ~escaped;
        var unescapedSquote = squote & ~escaped;

        var effectiveDquote = 0L;
        var effectiveSquote = 0L;
        var allQuotes = unescapedDquote | unescapedSquote;

        // Determine initial quote state from previous chunk's carry
        // 0 = not inside any string, 1 = inside dquote, 2 = inside squote
        int insideType;
        if (state.carryDquoteOpen != 0) {
            insideType = 1;
        } else if (state.carrySquoteOpen != 0) {
            insideType = 2;
        } else {
            insideType = 0;
        }

        var bits = allQuotes;
        while (bits != 0) {
            var pos = Long.numberOfTrailingZeros(bits);
            var bit = 1L << pos;
            var isDq = (unescapedDquote & bit) != 0;

            if (insideType == 0) {
                // Not inside any string; this quote opens a new one
                insideType = isDq ? 1 : 2;
                if (isDq) {
                    effectiveDquote |= bit;
                } else {
                    effectiveSquote |= bit;
                }
            } else if ((insideType == 1 && isDq) || (insideType == 2 && !isDq)) {
                // Same type as the open string; this is the closing quote
                insideType = 0;
                if (isDq) {
                    effectiveDquote |= bit;
                } else {
                    effectiveSquote |= bit;
                }
            }
            // else: different type inside an open string, skip (literal byte)

            bits &= bits - 1;
        }

        // Compute string regions from the filtered quote masks
        var inDquote = prefixXor(effectiveDquote);
        if (state.carryDquoteOpen != 0) {
            inDquote ^= validMask;
        }
        inDquote &= validMask;

        var inSquote = prefixXor(effectiveSquote);
        if (state.carrySquoteOpen != 0) {
            inSquote ^= validMask;
        }
        inSquote &= validMask;

        // Derive next carry from the scan's final state (more accurate than
        // bitcount, since cross-type quotes have been filtered out)
        var nextCarryDquote = (insideType == 1) ? 1L : 0L;
        var nextCarrySquote = (insideType == 2) ? 1L : 0L;

        var inString = (inDquote | inSquote) & validMask;

        var hashOutsideString = hash & ~inString;
        var inComment = computeCommentRegion(hashOutsideString, newline, validMask);
        if (state.carryInComment != 0) {
            var firstNewline = Long.numberOfTrailingZeros(newline);
            if (firstNewline < chunkLen) {
                inComment |= maskBelow(firstNewline + 1);
            } else {
                inComment = validMask;
            }
        }
        var nextCarryComment = (chunkLen > 0)
                ? ((inComment & ~newline) >>> (chunkLen - 1)) & 1
                : state.carryInComment;

        var active = ~(inString | inComment | whitespace) & validMask;
        var activeStructural = structural & active;

        // String starts: the rising edge (0 to 1 transition) of inDquote or inSquote.
        // When carry is set, bit 0 was already inside a string from the
        // previous chunk, so it's NOT a new string start.
        var dstrStarts = inDquote & ~((inDquote << 1) | (state.carryDquoteOpen != 0 ? 1L : 0));
        var sstrStarts = inSquote & ~((inSquote << 1) | (state.carrySquoteOpen != 0 ? 1L : 0));

        // Suppress false string starts in cross-chunk continuation region.
        // When carry is set, bytes before the closing quote are part of the
        // pending string from the previous chunk, not new starts.
        if (state.carryDquoteOpen != 0 && effectiveDquote != 0) {
            var closePos = Long.numberOfTrailingZeros(effectiveDquote);
            dstrStarts &= maskAtAndAbove(closePos + 1);
        }
        if (state.carrySquoteOpen != 0 && effectiveSquote != 0) {
            var closePos = Long.numberOfTrailingZeros(effectiveSquote);
            sstrStarts &= maskAtAndAbove(closePos + 1);
        }

        // Content runs: contiguous sequences of active non-structural,
        // non-quote bytes (identifiers, numeric literals)
        var content = active & ~activeStructural & ~dquote & ~squote;
        var runStarts = content & ~(content << 1); // rising edges

        // If a content run is pending from the previous chunk, suppress
        // the false start at position 0 (it's a continuation, not a new run).
        if (state.pendingContentStart >= 0 && (content & 1) != 0) {
            runStarts &= ~1L;
        }

        // Cross-chunk pending handling

        // Handle pending content run from previous chunk
        tapePos = handlePendingContent(state, chunkStart, content, chunkLen, tape, tapePos);

        // Handle pending string from previous chunk
        tapePos = handlePendingString(state, chunkStart, chunkLen,
                effectiveDquote, effectiveSquote, tape, tapePos);

        // Unified in-order token emission
        //
        // Combine all token-start bitmasks and iterate in bit order (lowest
        // bit first = leftmost byte first). For each set bit, test which
        // category it belongs to and emit the corresponding token.
        var allStarts = activeStructural | dstrStarts | sstrStarts | runStarts;
        while (allStarts != 0) {
            var pos = Long.numberOfTrailingZeros(allStarts);
            var bit = 1L << pos;
            var absPos = chunkStart + pos;

            if ((activeStructural & bit) != 0) {
                // Structural token (single byte)
                var b = input.get(JAVA_BYTE, absPos) & 0xFF;
                tape[tapePos++] = packToken(absPos, 1, STRUCTURAL_KIND[b]);

            } else if ((dstrStarts & bit) != 0) {
                // Double-quoted string
                tapePos = emitString(pos, inDquote, chunkStart, chunkLen,
                        DSTR, state, tape, tapePos);

            } else if ((sstrStarts & bit) != 0) {
                // Single-quoted string
                tapePos = emitString(pos, inSquote, chunkStart, chunkLen,
                        SSTR, state, tape, tapePos);

            } else {
                // Content run (identifier or numeric literal).
                // Compute run length: count consecutive set bits in content.
                var runLen = Long.numberOfTrailingZeros(~(content >>> pos));
                if (pos + runLen > chunkLen) {
                    runLen = chunkLen - pos;
                }
                // Classify by first byte: digits and '.' start NUMERIC runs
                var firstByte = input.get(JAVA_BYTE, absPos) & 0xFF;
                var kind = (CHAR_CLASS[firstByte] & CHAR_DIGIT_OR_DOT) != 0
                        ? NUMERIC
                        : IDENT;

                if (pos + runLen >= chunkLen) {
                    // Run extends to end of chunk, may continue in next chunk
                    state.pendingContentStart = absPos;
                    state.pendingContentKind = kind;
                } else {
                    tape[tapePos++] = packToken(absPos, runLen, kind);
                }
            }

            allStarts &= allStarts - 1; // clear lowest set bit (blsr)
        }

        // Update cross-chunk carry state
        state.carryDquoteOpen = nextCarryDquote;
        state.carrySquoteOpen = nextCarrySquote;
        state.carryInComment = nextCarryComment;
        state.carryEscaped = nextCarryEscaped;

        return tapePos;
    }

    /**
     * Emits a string token starting at position {@code pos} within the
     * current chunk. If the string does not close within this chunk, it is
     * recorded as a pending string in the parse state and will be emitted
     * when the closing quote is found in a subsequent chunk.
     *
     * @param pos          position of the opening quote within the chunk (0-based)
     * @param inStringMask bitmask of positions inside this string type
     * @param chunkStart   absolute byte offset of the chunk
     * @param chunkLen     number of valid bytes in the chunk
     * @param kind         token kind (DSTR or SSTR)
     * @param state        parse state for recording pending strings
     * @param tape         output tape
     * @param tapePos      current write position in tape
     * @return updated tapePos
     */
    private int emitString(int pos, long inStringMask, int chunkStart, int chunkLen,
                           int kind, ParseState state, long[] tape, int tapePos) {
        // If opening quote is at the last byte, string definitely extends past chunk
        if (pos + 1 >= chunkLen) {
            state.pendingStringStart = chunkStart + pos;
            state.pendingStringKind = kind;
            return tapePos;
        }

        // Look at in-string bits after the opening quote. Count consecutive
        // set bits to find where inStringMask drops to 0 (the closing quote).
        var afterPos = inStringMask >>> (pos + 1);
        var inStringCount = Long.numberOfTrailingZeros(~afterPos);
        var closePos = pos + 1 + inStringCount; // position of the closing quote

        if (closePos < chunkLen) {
            // String closed within this chunk, emit it (includes both quotes)
            var length = closePos - pos + 1;
            tape[tapePos++] = packToken(chunkStart + pos, length, kind);
        } else {
            // String extends past chunk boundary, defer to pending
            state.pendingStringStart = chunkStart + pos;
            state.pendingStringKind = kind;
        }
        return tapePos;
    }

    /**
     * Handles a pending cross-chunk string. If the previous chunk left a
     * string open, this method looks for the closing quote in the current
     * chunk and emits the complete string token spanning from the original
     * opening quote to the closing quote.
     */
    private int handlePendingString(ParseState state, int chunkStart, int chunkLen,
                                    long unescapedDquote, long unescapedSquote,
                                    long[] tape, int tapePos) {
        if (state.pendingStringStart < 0) {
            return tapePos;
        }

        var isDquote = (state.pendingStringKind == DSTR);
        var quotes = isDquote ? unescapedDquote : unescapedSquote;

        if (quotes == 0) {
            // No closing quote in this chunk, string continues
            return tapePos;
        }

        // Found the closing quote: emit the complete string token
        var closePos = Long.numberOfTrailingZeros(quotes);
        var absClosePos = chunkStart + closePos;
        var length = absClosePos - state.pendingStringStart + 1;
        tape[tapePos++] = packToken(state.pendingStringStart, length, state.pendingStringKind);

        state.pendingStringStart = -1;
        return tapePos;
    }

    /**
     * Handles a pending cross-chunk content run (identifier or numeric literal).
     * If the previous chunk ended mid-run, this method finds where the run ends
     * in the current chunk and emits the complete token.
     */
    private int handlePendingContent(ParseState state, int chunkStart,
                                     long content, int chunkLen,
                                     long[] tape, int tapePos) {
        if (state.pendingContentStart < 0) {
            return tapePos;
        }

        if ((content & 1) == 0) {
            // Content does not continue at position 0, run ended at previous chunk boundary
            var totalLength = chunkStart - state.pendingContentStart;
            tape[tapePos++] = packToken(state.pendingContentStart, totalLength, state.pendingContentKind);
            state.pendingContentStart = -1;
            return tapePos;
        }

        // Content continues from position 0. Find where it ends.
        var extension = Long.numberOfTrailingZeros(~content);
        if (extension >= chunkLen) {
            // Entire chunk is content, run continues to next chunk
            return tapePos;
        }

        // Content ended within this chunk
        var totalLength = (chunkStart + extension) - state.pendingContentStart;
        tape[tapePos++] = packToken(state.pendingContentStart, totalLength, state.pendingContentKind);
        state.pendingContentStart = -1;
        return tapePos;
    }

    /**
     * Returns a mask with bits 0..(n-1) set. Safe for n >= 64 (returns -1L)
     * and n <= 0 (returns 0L). Avoids Java's shift-wrap where {@code 1L << 64}
     * silently becomes {@code 1L << 0}.
     */
    private static long maskBelow(int n) {
        if (n >= 64) return -1L;
        if (n <= 0) return 0L;
        return (1L << n) - 1;
    }

    /**
     * Returns a mask with bits n..63 set. Safe for n >= 64 (returns 0L)
     * and n <= 0 (returns -1L). Avoids Java's shift-wrap.
     */
    private static long maskAtAndAbove(int n) {
        if (n >= 64) return 0L;
        if (n <= 0) return -1L;
        return -(1L << n);
    }

    /**
     * Computes the escaped-byte bitmask using the simdjson carry-addition
     * technique. A byte at position {@code i} is escaped if it is preceded
     * by an odd-length run of consecutive backslashes.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Find run starts: backslash positions not preceded by a backslash.</li>
     *   <li>Split starts by absolute position parity (even vs. odd).</li>
     *   <li>Add the backslash bitmask to each group. Carry propagates
     *       through consecutive backslash bits and exits at the first
     *       non-backslash position.</li>
     *   <li>Filter carry-outs by opposite parity to select only odd-length
     *       runs (which produce escaped bytes).</li>
     * </ol>
     *
     * <p>Note: this method computes escapes within a single chunk. Cross-chunk
     * escape carry (a backslash run straddling a chunk boundary) is handled
     * by the caller in {@code processMaskingAndExtraction} via {@code state.carryEscaped}.
     *
     * @param backslash 64-bit mask of backslash positions
     * @return 64-bit mask of escaped byte positions
     */
    private static long computeEscaped(long backslash) {
        if (backslash == 0) {
            return 0;
        }

        var starts = backslash & ~(backslash << 1);
        var evenStarts = starts & EVEN_BITS;
        var oddStarts  = starts & ODD_BITS;

        var evenCarries = (evenStarts + backslash) & ~backslash;
        var oddCarries  = (oddStarts  + backslash) & ~backslash;

        var evenEscaped = evenCarries & ODD_BITS;
        var oddEscaped  = oddCarries  & EVEN_BITS;

        return evenEscaped | oddEscaped;
    }

    /**
     * Computes prefix XOR (cumulative XOR) of a 64-bit value. Each set bit
     * toggles all subsequent bits. Equivalent to carry-less multiplication
     * by all-ones ({@code PCLMULQDQ(x, 0xFF...F)}), but implemented as a
     * cascading shift-XOR chain because the Java Vector API does not expose
     * {@code PCLMULQDQ}.
     *
     * <p>Cost: 6 shifts + 6 XORs = 12 instructions, ~12 cycles.
     * A {@code PCLMULQDQ} would be ~3 cycles.
     *
     * @param bits input bitmask
     * @return prefix XOR of the input
     */
    private static long prefixXor(long bits) {
        bits ^= bits <<  1;
        bits ^= bits <<  2;
        bits ^= bits <<  4;
        bits ^= bits <<  8;
        bits ^= bits << 16;
        bits ^= bits << 32;
        return bits;
    }

    /**
     * Computes comment regions: for each {@code #} position, fills all
     * bits from the {@code #} to the next {@code \n} (or end of chunk).
     *
     * <p>Uses a per-hash loop because comments are typically sparse (0 to 2
     * per chunk in real textproto).
     *
     * @param hashes    positions of '#' characters outside strings
     * @param newlines  positions of '\n' characters
     * @param validMask valid byte positions in this chunk
     * @return bitmask of positions inside comments
     */
    private static long computeCommentRegion(long hashes, long newlines, long validMask) {
        var result = 0L;
        var bits = hashes;
        while (bits != 0) {
            var hashPos = Long.numberOfTrailingZeros(bits);
            // Find next newline after this hash. maskAtAndAbove guards
            // against the shift-wrap when hashPos=63 (Java masks long
            // shifts by 63, so 1L << 64 wraps to 1L << 0 = 1).
            var newlinesAfter = newlines & maskAtAndAbove(hashPos + 1);
            var newlinePos = Long.numberOfTrailingZeros(newlinesAfter);
            long regionEnd;
            if (newlinePos < 64) {
                regionEnd = maskBelow(newlinePos + 1);
            } else {
                regionEnd = validMask;
            }
            var regionStart = (1L << hashPos) - 1;
            result |= (regionEnd & ~regionStart);
            bits &= bits - 1;
        }
        return result;
    }

    /**
     * Packs an offset, length, and kind into a single {@code long} token.
     *
     * @param offset the byte offset in the input
     * @param length the byte length of the token
     * @param kind   the token kind
     * @return the packed token
     */
    private static long packToken(int offset, int length, int kind) {
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
    private static int tokenOffset(long token) {
        return (int) (token >>> 32);
    }

    /**
     * Extracts the byte length from a packed token.
     *
     * @param token the packed token
     * @return the byte length of the token
     */
    private static int tokenLength(long token) {
        return (int) ((token >>> KIND_BITS) & LENGTH_MASK);
    }

    /**
     * Extracts the token kind from a packed token.
     *
     * @param token the packed token
     * @return the token kind
     */
    private static int tokenKind(long token) {
        return (int) (token & KIND_MASK);
    }

    /**
     * Mutable state carried across 64-byte chunk boundaries.
     *
     * <p>The four carry bits track whether we are currently inside a
     * double-quoted string, single-quoted string, comment, or preceded
     * by an odd-length backslash run at the boundary between chunks.
     *
     * <p>The pending string fields handle strings that span chunk
     * boundaries. When a string opens in one chunk but does not close
     * within it, its absolute start offset and kind are recorded here.
     * The token is emitted only when the closing quote is found in a
     * subsequent chunk, with a length spanning the full string.
     */
    private static final class ParseState {
        long carryDquoteOpen;
        long carrySquoteOpen;
        long carryInComment;

        /** 1 if the previous chunk ended with an odd-length backslash run (the next byte is escaped). */
        long carryEscaped;

        /** Absolute offset of a pending string's opening quote, or -1. */
        int pendingStringStart = -1;

        /** Token kind (DSTR or SSTR) of the pending string. Only meaningful when {@code pendingStringStart >= 0}. */
        int pendingStringKind;

        /** Absolute offset of a pending content run's first byte, or -1. */
        int pendingContentStart = -1;

        /** Token kind (IDENT or NUMERIC) of the pending content run. Only meaningful when {@code pendingContentStart >= 0}. */
        int pendingContentKind;
    }
}