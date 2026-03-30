package com.github.auties00.daedalus.protobuf.cli.model;

/**
 * Represents a conflict detected during the update process.
 *
 * <p>Conflicts arise when a proto schema change cannot be applied
 * cleanly to existing Java source code, such as index collisions
 * or type changes.
 *
 * @param type the kind of conflict
 * @param protoFqn the fully-qualified proto type name where the conflict occurred
 * @param fieldName the name of the field involved, or {@code null} for type-level conflicts
 * @param fieldIndex the field index involved, or {@code -1} for non-index conflicts
 * @param message a human-readable description of the conflict
 */
public record Conflict(
        Type type,
        String protoFqn,
        String fieldName,
        long fieldIndex,
        String message
) {

    /**
     * The kind of conflict detected during the update process.
     */
    public enum Type {
        /**
         * A proto field index is already used by a different field name in the Java source.
         */
        INDEX_COLLISION,

        /**
         * A proto field changed its type but the existing Java field has the old type.
         */
        TYPE_CHANGE,

        /**
         * A proto field name conflicts with an existing user-written method or field.
         */
        NAME_COLLISION
    }
}
