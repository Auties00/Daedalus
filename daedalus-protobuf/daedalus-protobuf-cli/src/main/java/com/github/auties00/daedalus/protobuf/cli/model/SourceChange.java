package com.github.auties00.daedalus.protobuf.cli.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a proposed modification to a Java source file.
 *
 * <p>Contains the file path, the original and updated source text,
 * and a human-readable description of what changed.
 *
 * @param filePath the path to the Java source file
 * @param originalSource the original source code before modifications
 * @param updatedSource the updated source code after modifications
 * @param description a summary of the changes made
 * @param unifiedDiff the unified diff lines between original and updated source
 */
public record SourceChange(
        Path filePath,
        String originalSource,
        String updatedSource,
        String description,
        List<String> unifiedDiff
) {

    /**
     * Returns whether this change actually modifies the source.
     *
     * @return {@code true} if the updated source differs from the original
     */
    public boolean hasChanges() {
        return !originalSource.equals(updatedSource);
    }
}
