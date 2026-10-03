package com.github.auties00.daedalus.protobuf.compiler.expression;

import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOptionDefinition;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOptionName;

import java.util.Objects;

/**
 * Represents an option key-value pair expression in the Protocol Buffer AST.
 * <p>
 * Options are metadata annotations that configure behavior of Protocol Buffer elements.
 * They appear in square brackets and consist of a name and a value:
 * </p>
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Field options
 * optional string name = 1 [deprecated = true, default = "unknown"];
 *
 * // File-level option statement
 * option java_package = "com.example.proto";
 *
 * // Custom extension option
 * option (my.custom.option) = {
 *   field: "value"
 * };
 * }</pre>
 *
 * <p>This is a class rather than a record because the analyzer attaches the
 * resolved {@link ProtobufOptionDefinition} after parsing, via
 * {@link #setDefinition(ProtobufOptionDefinition)}. The definition is used by
 * the descriptor codegen to encode unknown custom-extension entries on the
 * matching {@code *Options} message.
 *
 * @see ProtobufOptionName
 * @see ProtobufExpression
 */
public final class ProtobufOptionExpression implements ProtobufExpression {
    private final ProtobufOptionName name;
    private final ProtobufExpression value;
    private ProtobufOptionDefinition definition;

    /**
     * Constructs a new option expression with the given name and value. The
     * resolved definition starts as {@code null} and is filled in by the
     * analyzer once the option is bound to a descriptor field.
     *
     * @param name  the option name
     * @param value the option value
     */
    public ProtobufOptionExpression(ProtobufOptionName name, ProtobufExpression value) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Returns the option name.
     *
     * @return the option name; never {@code null}
     */
    public ProtobufOptionName name() {
        return name;
    }

    /**
     * Returns the option value expression.
     *
     * @return the option value; never {@code null}
     */
    public ProtobufExpression value() {
        return value;
    }

    /**
     * Returns the resolved option definition, if any.
     *
     * <p>The analyzer fills this in by walking the option name segments and
     * binding them to the matching {@code *Options} field in the descriptor
     * model. Until that happens (or for an unresolved option), the value is
     * {@code null}.
     *
     * @return the resolved definition, or {@code null} if not resolved yet
     */
    public ProtobufOptionDefinition definition() {
        return definition;
    }

    /**
     * Returns whether this option has been resolved to a definition.
     *
     * @return {@code true} if {@link #definition()} would return non-null
     */
    public boolean hasDefinition() {
        return definition != null;
    }

    /**
     * Attaches the resolved option definition computed by the analyzer.
     *
     * @param definition the resolved definition
     */
    public void setDefinition(ProtobufOptionDefinition definition) {
        this.definition = definition;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ProtobufOptionExpression that
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        var nameOrPlaceholder = Objects.requireNonNullElse(this.name, "[missing]");
        builder.append(nameOrPlaceholder);

        builder.append(" ");
        builder.append("=");
        builder.append(" ");

        var valueOrPlaceholder = Objects.requireNonNullElse(this.value, "[missing]");
        builder.append(valueOrPlaceholder);

        return builder.toString();
    }
}
