package com.github.auties00.daedalus.protobuf.compiler.tree;

import java.util.Objects;

/**
 * Represents a single segment in an option name.
 * <p>
 * Each segment is either a simple field name or an extension name in parentheses.
 * </p>
 *
 * @param name         the segment name
 * @param isExtension  whether this segment is an extension (surrounded by parentheses)
 */
public record ProtobufOptionNameSegment(String name, boolean isExtension) {
    public ProtobufOptionNameSegment {
        Objects.requireNonNull(name, "name cannot be null");
    }

    /**
     * Checks whether this segment has the given name.
     *
     * @param name the name to check for
     * @return true if this segment has the given name, false otherwise
     */
    public boolean hasName(String name) {
        return this.name.equals(name);
    }

    @Override
    public String toString() {
        return isExtension ? "(" + name + ")" : name;
    }
}
