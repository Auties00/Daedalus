package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.auties00.daedalus.protobuf.cli.model.Conflict;
import com.github.auties00.daedalus.protobuf.cli.model.MatchResult;
import com.github.auties00.daedalus.protobuf.cli.model.SourceChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Represents the planned set of changes to apply during an update operation.
 *
 * <p>Contains matched types, their proposed source changes, any conflicts
 * detected, and proto types that could not be matched to existing Java types.
 */
public final class UpdatePlan {
    private final List<PlannedUpdate> updates;
    private final List<String> unmatchedProtoTypes;
    private final List<Conflict> conflicts;

    /**
     * Constructs an empty update plan.
     */
    public UpdatePlan() {
        this.updates = new ArrayList<>();
        this.unmatchedProtoTypes = new ArrayList<>();
        this.conflicts = new ArrayList<>();
    }

    /**
     * Adds a planned update for a matched proto-to-Java type pair.
     *
     * @param matchResult the match result linking proto and Java types
     * @param sourceChange the proposed source code change
     * @param updateConflicts any conflicts detected for this update
     */
    public void addUpdate(MatchResult matchResult, SourceChange sourceChange,
                           SequencedCollection<Conflict> updateConflicts) {
        updates.add(new PlannedUpdate(matchResult, sourceChange, updateConflicts));
        conflicts.addAll(updateConflicts);
    }

    /**
     * Records a proto type that could not be matched to any existing Java type.
     *
     * @param protoFqn the fully-qualified name of the unmatched proto type
     */
    public void addUnmatchedType(String protoFqn) {
        unmatchedProtoTypes.add(protoFqn);
    }

    /**
     * Returns the planned updates.
     *
     * @return an unmodifiable collection of planned updates
     */
    public SequencedCollection<PlannedUpdate> updates() {
        return Collections.unmodifiableSequencedCollection(updates);
    }

    /**
     * Returns the proto types that could not be matched.
     *
     * @return an unmodifiable collection of unmatched proto FQNs
     */
    public SequencedCollection<String> unmatchedProtoTypes() {
        return Collections.unmodifiableSequencedCollection(unmatchedProtoTypes);
    }

    /**
     * Returns all conflicts across all planned updates.
     *
     * @return an unmodifiable collection of conflicts
     */
    public SequencedCollection<Conflict> conflicts() {
        return Collections.unmodifiableSequencedCollection(conflicts);
    }

    /**
     * Returns whether this plan has any changes to apply.
     *
     * @return {@code true} if at least one update has changes
     */
    public boolean hasChanges() {
        return updates.stream().anyMatch(u -> u.sourceChange().hasChanges());
    }

    /**
     * Returns the number of updates with actual changes.
     *
     * @return the count of updates that modify source files
     */
    public int changeCount() {
        return (int) updates.stream().filter(u -> u.sourceChange().hasChanges()).count();
    }

    /**
     * Represents a single planned update linking a match result to its proposed change.
     *
     * @param matchResult the match result
     * @param sourceChange the proposed source change
     * @param conflicts any conflicts for this specific update
     */
    public record PlannedUpdate(
            MatchResult matchResult,
            SourceChange sourceChange,
            SequencedCollection<Conflict> conflicts
    ) {
    }
}
