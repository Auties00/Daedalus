package com.github.auties00.daedalus.protobuf.io.reader.text;

/**
 * Character constants for the Protocol Buffers text format lexer.
 */
final class ProtobufTextChars {
    // Whitespace
    static final int SPACE = ' ';
    static final int TAB   = '\t';
    static final int CR    = '\r';
    static final int LF    = '\n';

    // Structural delimiters
    static final int LBRACE   = '{';
    static final int RBRACE   = '}';
    static final int LANGLE   = '<';
    static final int RANGLE   = '>';
    static final int LBRACKET = '[';
    static final int RBRACKET = ']';
    static final int COLON    = ':';
    static final int COMMA    = ',';
    static final int SEMICOLON = ';';
    static final int MINUS    = '-';

    // Quotes and comment
    static final int DQUOTE    = '"';
    static final int SQUOTE    = '\'';
    static final int HASH      = '#';
    static final int BACKSLASH = '\\';

    // Content classification
    static final int DOT      = '.';

    // Escape result characters
    static final char ALERT = '\u0007';
    static final char VTAB  = '\u000B';

    private ProtobufTextChars() {
        throw new UnsupportedOperationException("ProtobufTextChars is a utility class and cannot be initialized");
    }
}
