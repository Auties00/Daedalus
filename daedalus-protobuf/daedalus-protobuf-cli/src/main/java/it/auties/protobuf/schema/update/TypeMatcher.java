package it.auties.protobuf.schema.update;

import it.auties.protobuf.schema.model.MatchResult;

import java.util.*;

/**
 * Matches proto type definitions to existing Java types using multi-signal scoring.
 *
 * <p>Scores candidates on multiple dimensions:
 * <ul>
 * <li>Exact annotation match: {@code @ProtobufMessage(name = "pkg.Type")} matches
 *     proto FQN exactly (confidence = 1.0)
 * <li>Simple name equality: Java class name matches proto type name (0.7)
 * <li>Normalized name similarity: Levenshtein distance below threshold (0.5)
 * <li>Field index overlap: ratio of shared indexes between Java and proto (0.4)
 * <li>Package correlation: Java package contains proto package segments (0.2)
 * </ul>
 */
public final class TypeMatcher {

    /**
     * Constructs a type matcher.
     */
    public TypeMatcher() {
    }

    /**
     * Finds matching Java types for a given proto type definition.
     *
     * @param protoFqn the fully-qualified proto name to match
     * @param protoSimpleName the simple name of the proto type
     * @param protoFieldIndexes the set of field indexes defined in the proto type
     * @param protoPackage the proto package name, or {@code null}
     * @param candidates the list of scanned Java types to match against
     * @param expectedAnnotationType the expected annotation type (MESSAGE, ENUM, or GROUP)
     * @return a sorted list of match results, highest confidence first
     */
    public List<MatchResult> findMatches(String protoFqn, String protoSimpleName,
                                          Set<Long> protoFieldIndexes, String protoPackage,
                                          List<ScannedType> candidates,
                                          ScannedType.AnnotationType expectedAnnotationType) {
        var results = new ArrayList<MatchResult>();
        for (var candidate : candidates) {
            if (candidate.annotationType() != expectedAnnotationType) {
                continue;
            }

            var score = scoreCandidate(protoFqn, protoSimpleName, protoFieldIndexes,
                    protoPackage, candidate);
            if (score.confidence() > 0.0) {
                results.add(score);
            }
        }
        Collections.sort(results);
        return results;
    }

    /**
     * Finds the best match for a proto type, returning it only if it meets the
     * auto-accept threshold and is significantly above the next best match.
     *
     * @param protoFqn the fully-qualified proto name to match
     * @param protoSimpleName the simple name of the proto type
     * @param protoFieldIndexes the set of field indexes defined in the proto type
     * @param protoPackage the proto package name, or {@code null}
     * @param candidates the list of scanned Java types to match against
     * @param expectedAnnotationType the expected annotation type
     * @return the best match if it meets the threshold, or {@code null}
     */
    public MatchResult findBestMatch(String protoFqn, String protoSimpleName,
                                      Set<Long> protoFieldIndexes, String protoPackage,
                                      List<ScannedType> candidates,
                                      ScannedType.AnnotationType expectedAnnotationType) {
        var matches = findMatches(protoFqn, protoSimpleName, protoFieldIndexes,
                protoPackage, candidates, expectedAnnotationType);
        if (matches.isEmpty()) {
            return null;
        }

        var best = matches.getFirst();
        if (best.confidence() < MatchResult.AUTO_ACCEPT_THRESHOLD) {
            return null;
        }

        if (matches.size() > 1) {
            var second = matches.get(1);
            if (best.confidence() - second.confidence() < 0.15) {
                return null;
            }
        }

        return best;
    }

    private MatchResult scoreCandidate(String protoFqn, String protoSimpleName,
                                        Set<Long> protoFieldIndexes, String protoPackage,
                                        ScannedType candidate) {
        var reasons = new ArrayList<String>();

        // Exact annotation match (confidence = 1.0)
        if (candidate.protoFqn() != null && candidate.protoFqn().equals(protoFqn)) {
            reasons.add("exact annotation name match");
            return new MatchResult(candidate, protoFqn, 1.0, reasons);
        }

        var score = 0.0;

        // Simple name equality (0.7)
        if (candidate.simpleTypeName().equalsIgnoreCase(protoSimpleName)) {
            score += 0.7;
            reasons.add("name match: " + candidate.simpleTypeName());
        } else {
            var distance = levenshteinDistance(
                    candidate.simpleTypeName().toLowerCase(),
                    protoSimpleName.toLowerCase()
            );
            if (distance <= 3 && distance < Math.max(candidate.simpleTypeName().length(),
                    protoSimpleName.length()) / 2) {
                var similarity = 1.0 - (double) distance / Math.max(
                        candidate.simpleTypeName().length(), protoSimpleName.length());
                score += 0.5 * similarity;
                reasons.add("similar name (distance=" + distance + ")");
            }
        }

        // Field index overlap (0.4)
        if (!protoFieldIndexes.isEmpty()) {
            var existingIndexes = candidate.existingFieldIndexes();
            if (!existingIndexes.isEmpty()) {
                var intersection = new LinkedHashSet<>(existingIndexes);
                intersection.retainAll(protoFieldIndexes);
                var overlap = (double) intersection.size() / protoFieldIndexes.size();
                score += 0.4 * overlap;
                if (!intersection.isEmpty()) {
                    reasons.add(intersection.size() + "/" + protoFieldIndexes.size() + " fields match");
                }
            }
        }

        // Package correlation (0.2)
        if (protoPackage != null && !protoPackage.isEmpty()) {
            var protoSegments = protoPackage.split("\\.");
            var javaPackage = candidate.javaPackage();
            if (!javaPackage.isEmpty()) {
                var matchingSegments = 0;
                for (var segment : protoSegments) {
                    if (javaPackage.contains(segment)) {
                        matchingSegments++;
                    }
                }
                if (matchingSegments > 0) {
                    var packageScore = (double) matchingSegments / protoSegments.length;
                    score += 0.2 * packageScore;
                    reasons.add("package correlation");
                }
            }
        }

        return new MatchResult(candidate, protoFqn, Math.min(score, 1.0), reasons);
    }

    private int levenshteinDistance(String a, String b) {
        var dp = new int[a.length() + 1][b.length() + 1];
        for (var i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (var j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (var i = 1; i <= a.length(); i++) {
            for (var j = 1; j <= b.length(); j++) {
                var cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}
