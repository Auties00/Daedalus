package com.github.auties00.daedalus.protobuf.compiler.tree;

import com.github.auties00.daedalus.protobuf.model.ProtobufVersion;

import java.util.Objects;

/**
 * Represents an edition declaration statement in the Protocol Buffer AST.
 * <p>
 * The edition statement specifies which edition of the Protocol Buffer language is used in the
 * file. It must be the first non comment, non empty line in a .proto file, and is mutually
 * exclusive with a syntax statement.
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * edition = "2023";
 * edition = "2024";
 * }</pre>
 * <p>
 * Editions replace the classic {@code syntax} keyword and introduce a configurable feature
 * system that unifies proto2 and proto3 semantics.
 * </p>
 *
 * @see ProtobufVersion
 * @see ProtobufSyntaxStatement
 * @see ProtobufDocumentChild
 */
public final class ProtobufEditionStatement
        extends ProtobufStatementImpl
        implements ProtobufStatement,
                   ProtobufDocumentChild {
    private ProtobufVersion version;

    /**
     * Constructs a new edition statement at the specified line number.
     *
     * @param line the line number in the source file
     */
    public ProtobufEditionStatement(int line) {
        super(line);
    }

    /**
     * Returns the Protocol Buffer edition specified by this statement.
     *
     * @return the version, or null if not yet set
     */
    public ProtobufVersion version() {
        return version;
    }

    /**
     * Checks whether this statement has a version assigned.
     *
     * @return true if a version is present, false otherwise
     */
    public boolean hasVersion() {
        return version != null;
    }

    /**
     * Sets the Protocol Buffer edition for this statement.
     *
     * @param version the version to set
     */
    public void setVersion(ProtobufVersion version) {
        this.version = version;
    }

    @Override
    public String toString() {
        var version = Objects.requireNonNullElse(this.version, "[missing]");
        return "edition = \"" + version + "\";";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof ProtobufEditionStatement that
                              && Objects.equals(this.version(), that.version());
    }

    @Override
    public boolean isAttributed() {
        return hasVersion();
    }
}