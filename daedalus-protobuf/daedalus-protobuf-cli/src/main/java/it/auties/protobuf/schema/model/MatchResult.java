package it.auties.protobuf.schema.model;

import it.auties.protobuf.schema.update.ScannedType;

import java.util.List;

/**
 * Represents the result of matching a proto type definition to an existing Java type.
 *
 * <p>Contains the matched Java type, the confidence score, and the reasons
 * for the match. Higher confidence scores indicate stronger matches.
 *
 * @param scannedType the matched Java type from the source scan
 * @param protoFqn the fully-qualified proto name that was matched against
 * @param confidence the match confidence score, between {@code 0.0} and {@code 1.0}
 * @param reasons the list of reasons contributing to the match score
 */
public record MatchResult(
        ScannedType scannedType,
        String protoFqn,
        double confidence,
        List<String> reasons
) implements Comparable<MatchResult> {

    /**
     * The minimum confidence threshold for an auto-accepted match.
     */
    public static final double AUTO_ACCEPT_THRESHOLD = 0.8;

    /**
     * Compares this match result to another by descending confidence.
     *
     * @param other the other match result to compare to
     * @return a negative value if this result has higher confidence
     */
    @Override
    public int compareTo(MatchResult other) {
        return Double.compare(other.confidence, this.confidence);
    }
}
