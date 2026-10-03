package com.github.auties00.daedalus.protobuf.cli.update;

import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufOneofStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumConstantStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufEnumStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufFieldStatement;
import com.github.auties00.daedalus.protobuf.compiler.tree.ProtobufMessageStatement;
import com.github.auties00.daedalus.protobuf.cli.generation.NamingStrategy;
import com.github.auties00.daedalus.protobuf.cli.generation.PropertyGenerator;
import com.github.auties00.daedalus.protobuf.cli.model.Conflict;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects conflicts between proto schema definitions and existing Java source code.
 *
 * <p>Identifies situations that require user attention:
 * <ul>
 * <li><strong>Index collision</strong>: a proto field index is already used
 *     by a different field name in the Java source
 * <li><strong>Type change</strong>: a proto field changed its type but the
 *     existing Java field has the old type (detected by name match with
 *     different index)
 * <li><strong>Name collision</strong>: a proto field name conflicts with
 *     an existing user-written method or field
 * </ul>
 */
public final class ConflictResolver {
    /**
     * The property generator used to collect the fields of a message, including the
     * members of its {@code oneof} declarations.
     */
    private final PropertyGenerator propertyGenerator;

    private final NamingStrategy naming;

    /**
     * Constructs a conflict resolver with the given property generator and naming strategy.
     *
     * @param propertyGenerator the property generator used to collect the fields of a message
     * @param naming the naming strategy for converting proto names to Java names
     */
    public ConflictResolver(PropertyGenerator propertyGenerator, NamingStrategy naming) {
        this.propertyGenerator = propertyGenerator;
        this.naming = naming;
    }

    /**
     * Detects conflicts between a proto message definition and an existing Java type.
     *
     * @param scannedType the existing Java type
     * @param message the proto message definition
     * @param protoFqn the fully-qualified proto name
     * @return the list of conflicts detected
     */
    public List<Conflict> detectMessageConflicts(ScannedType scannedType,
                                                  ProtobufMessageStatement message,
                                                  String protoFqn) {
        var conflicts = new ArrayList<Conflict>();
        var existingByIndex = scannedType.existingFieldsByIndex();

        propertyGenerator.collectFields(message).forEach(field -> {
            var protoIndex = (int) field.index().value();
            // The member of a oneof is held by the single field of the oneof
            var protoFieldName = field.parent() instanceof ProtobufOneofStatement oneof
                    ? naming.toFieldName(oneof.name())
                    : naming.toFieldName(field.name());

            // Check for index collision: same index, different name
            if (existingByIndex.containsKey(protoIndex)) {
                var existingName = existingByIndex.get(protoIndex);
                if (!existingName.equals(protoFieldName)) {
                    conflicts.add(new Conflict(
                            Conflict.Type.INDEX_COLLISION,
                            protoFqn,
                            protoFieldName,
                            protoIndex,
                            "Index " + protoIndex + " is already used by '" + existingName
                                    + "' but proto defines '" + protoFieldName + "'"
                    ));
                }
            }

            // Check for name collision: same name, different index
            for (var entry : existingByIndex.entrySet()) {
                if (entry.getValue().equals(protoFieldName) && entry.getKey() != protoIndex) {
                    conflicts.add(new Conflict(
                            Conflict.Type.NAME_COLLISION,
                            protoFqn,
                            protoFieldName,
                            protoIndex,
                            "Field '" + protoFieldName + "' exists at index "
                                    + entry.getKey() + " but proto defines it at index " + protoIndex
                    ));
                }
            }
        });

        return conflicts;
    }

    /**
     * Detects conflicts between a proto enum definition and an existing Java type.
     *
     * @param scannedType the existing Java type
     * @param enumStmt the proto enum definition
     * @param protoFqn the fully-qualified proto name
     * @return the list of conflicts detected
     */
    public List<Conflict> detectEnumConflicts(ScannedType scannedType,
                                               ProtobufEnumStatement enumStmt,
                                               String protoFqn) {
        var conflicts = new ArrayList<Conflict>();
        var existingIndexes = scannedType.existingEnumConstantIndexes();

        enumStmt.getDirectChildrenByType(ProtobufEnumConstantStatement.class).forEach(constant -> {
            var protoIndex = (int) constant.index().value();
            var protoName = naming.toEnumConstantName(constant.name());

            // For enums, index collision is less likely to be a problem since
            // we skip existing indexes, but we still detect if the same index
            // maps to a different constant name
            if (existingIndexes.contains(protoIndex)) {
                // Check if it's actually a different name at the same index
                var existingByIndex = scannedType.existingFieldsByIndex();
                if (existingByIndex.containsKey(protoIndex)) {
                    var existingName = existingByIndex.get(protoIndex);
                    if (!existingName.equals(protoName)) {
                        conflicts.add(new Conflict(
                                Conflict.Type.INDEX_COLLISION,
                                protoFqn,
                                protoName,
                                protoIndex,
                                "Enum index " + protoIndex + " is already used by '"
                                        + existingName + "' but proto defines '" + protoName + "'"
                        ));
                    }
                }
            }
        });

        return conflicts;
    }
}
