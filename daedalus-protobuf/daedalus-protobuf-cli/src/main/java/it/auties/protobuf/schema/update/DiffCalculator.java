package it.auties.protobuf.schema.update;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Computes unified diffs between original and updated Java source files.
 *
 * <p>Uses java-diff-utils to produce standard unified diff output with
 * configurable context lines, suitable for display in the TUI diff viewer.
 */
public final class DiffCalculator {
    private final int contextLines;

    /**
     * Constructs a diff calculator with the specified number of context lines.
     *
     * @param contextLines the number of unchanged lines to show around each change
     */
    public DiffCalculator(int contextLines) {
        this.contextLines = contextLines;
    }

    /**
     * Constructs a diff calculator with the default of 3 context lines.
     */
    public DiffCalculator() {
        this(3);
    }

    /**
     * Computes a unified diff between the original and updated source text.
     *
     * @param filePath the file path to display in the diff header
     * @param originalSource the original source text
     * @param updatedSource the updated source text
     * @return the list of unified diff lines, or an empty list if there are no changes
     */
    public List<String> computeDiff(Path filePath, String originalSource, String updatedSource) {
        if (originalSource.equals(updatedSource)) {
            return List.of();
        }

        var originalLines = Arrays.asList(originalSource.split("\n", -1));
        var updatedLines = Arrays.asList(updatedSource.split("\n", -1));

        var patch = DiffUtils.diff(originalLines, updatedLines);
        var fileName = filePath.getFileName().toString();

        return UnifiedDiffUtils.generateUnifiedDiff(
                "a/" + fileName,
                "b/" + fileName,
                originalLines,
                patch,
                contextLines
        );
    }
}
