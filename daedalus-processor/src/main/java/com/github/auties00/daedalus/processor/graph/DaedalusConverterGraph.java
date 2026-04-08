package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * A unified graph of type converters that resolves multi-step conversion paths between types.
 *
 * <p>The graph is populated with converter methods annotated with {@link TypeSerializer}
 * or {@link TypeDeserializer}. Both serializer and deserializer converters coexist in the
 * same graph, allowing path discovery to mix conversion directions when necessary (e.g.
 * unwrapping an {@code Optional} during deserialization). Cycle detection prevents infinite
 * loops that could otherwise arise from bidirectional converter pairs.
 *
 * <p>Given a source type and target type, the graph finds a path of converter methods that
 * transforms from source to target, including through intermediate types and generic type
 * resolution.
 */
public final class DaedalusConverterGraph {
    private final DaedalusTypeManager types;
    private final SequencedCollection<DaedalusProcessorExtension> extensions;
    private final Set<DaedalusConverterNode> nodes;
    private final SequencedCollection<DaedalusOrphanSizer> orphanedSizers;

    /**
     * Constructs a new converter graph with the given type utilities.
     *
     * @param types      the common type utilities
     * @param extensions the extensions
     */
    public DaedalusConverterGraph(DaedalusTypeManager types, SequencedCollection<DaedalusProcessorExtension> extensions) {
        this.types = types;
        this.extensions = extensions;
        this.nodes = new HashSet<>();
        this.orphanedSizers = new ArrayList<>();
    }

    /**
     * Returns whether the given type is managed by a format extension.
     *
     * <p>Managed types have generated Spec classes with encode/decode methods
     * that are valid conversion endpoints.
     *
     * @param type the type to check
     * @return {@code true} if the type is managed
     */
    private boolean isManagedType(TypeMirror type) {
        return extensions.stream().anyMatch(ext -> ext.isManagedType(type));
    }

    /**
     * Returns the fully qualified name of the Spec class for the given managed type.
     *
     * @param type the managed type
     * @return the Spec class name
     */
    private String getSpecName(TypeMirror type) {
        return DaedalusMethodGenerator.getSpecByType(type);
    }

    /**
     * Links a serializer converter method into the graph as a transformation from
     * a value type to a wire-format type.
     *
     * <p>If a sizer for {@code from} has previously been registered via
     * {@link #attachSizer(TypeMirror, DaedalusMethodElement)} as an orphan, it is
     * consumed and attached to the new serializer node.
     *
     * @param from the source value type
     * @param to the target wire-format type
     * @param arc the converter method
     */
    public void linkSerializer(TypeMirror from, TypeMirror to, DaedalusMethodElement arc) {
        var serializer = new DaedalusConverterNode.Serializer(from, to, arc);
        orphanedSizers.parallelStream()
                .filter(orphan -> types.isSameType(orphan.valueType(), from, false))
                .findAny()
                .ifPresent(orphan -> {
                    serializer.setSizer(orphan.sizer());
                    orphanedSizers.remove(orphan);
                });
        nodes.add(serializer);
    }

    /**
     * Links a deserializer converter method into the graph as a transformation from
     * a wire-format reader type to a value type.
     *
     * @param from the source wire-format type
     * @param to the target value type
     * @param arc the converter method
     */
    public void linkDeserializer(TypeMirror from, TypeMirror to, DaedalusMethodElement arc) {
        nodes.add(new DaedalusConverterNode.Deserializer(from, to, arc));
    }

    /**
     * Attaches a sizer to every existing serializer node whose source type matches
     * {@code valueType}, or stores it as an orphan to be picked up by a future
     * {@link #linkSerializer(TypeMirror, TypeMirror, DaedalusMethodElement)} call
     * if no matching serializer exists yet.
     *
     * <p>The matching scan runs in parallel and mutates serializer nodes in place,
     * which avoids the cost of rebuilding the node set.
     *
     * @param valueType the value type the sizer applies to
     * @param sizer the size calculation method
     */
    public void attachSizer(TypeMirror valueType, DaedalusMethodElement sizer) {
        var matches = nodes.parallelStream()
                .filter(node -> node instanceof DaedalusConverterNode.Serializer serializer
                        && serializer.sizer() == null
                        && types.isSameType(serializer.from(), valueType, false))
                .map(node -> (DaedalusConverterNode.Serializer) node)
                .toList();
        if (matches.isEmpty()) {
            orphanedSizers.add(new DaedalusOrphanSizer(valueType, sizer));
        } else {
            for (var match : matches) {
                match.setSizer(sizer);
            }
        }
    }

    /**
     * Links a converter method into the graph as a generic transformation, used by tests
     * and legacy callers that do not need to distinguish serialization direction.
     *
     * @param from the source type
     * @param to the target type
     * @param arc the converter method
     */
    public void link(TypeMirror from, TypeMirror to, DaedalusMethodElement arc) {
        linkSerializer(from, to, arc);
    }

    /**
     * Finds a conversion path from one type to another, considering only converters
     * that are accessible through the given mixins.
     *
     * @param from the source type
     * @param to the target type
     * @param mixins the mixins that provide accessible converters
     * @return the list of conversion arcs forming the path, or an empty list if no path exists
     */
    public List<DaedalusConverterArc> findPath(TypeMirror from, TypeMirror to, Set<TypeElement> mixins) {
        var visited = new HashSet<String>();
        visited.add(from.toString());
        return findAnyPath(from, to, mixins, visited);
    }

    private List<DaedalusConverterArc> findAnyPath(TypeMirror from, TypeMirror to, Set<TypeElement> mixins, Set<String> visited) {
        return nodes.parallelStream()
                .map(node -> findSubPath(node, to, mixins, from, visited))
                .filter(entry -> !entry.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    private List<DaedalusConverterArc> findSubPath(DaedalusConverterNode node, TypeMirror to, Set<TypeElement> mixins, TypeMirror from, Set<String> visited) {
        if (!types.isAssignable(from, node.from())) {
            return List.of();
        }

        var sizer = switch (node) {
            case DaedalusConverterNode.Serializer serializer -> serializer.sizer();
            case DaedalusConverterNode.Deserializer ignored -> null;
        };
        if (node.arc().isParametrized()) {
            var returnType = node.arc()
                    .element()
                    .map(element -> types.getReturnType(element, List.of(from)))
                    .orElse(node.arc().returnType());
            if (types.isAssignable(to, returnType, false)) {
                var arc = new DaedalusConverterArc(node.arc(), returnType, sizer);
                return List.of(arc);
            }

            var length = countTypeArguments(returnType);
            if (length > countTypeArguments(to) && length > countTypeArguments(from)) {
                return List.of();
            }

            var returnKey = returnType.toString();
            if (visited.contains(returnKey)) {
                return List.of();
            }

            var newVisited = new HashSet<>(visited);
            newVisited.add(returnKey);
            var nested = findAnyPath(returnType, to, mixins, newVisited);
            if (nested.isEmpty() || !isPathLegal(node, from, to, mixins)) {
                return List.of();
            }

            var arc = new DaedalusConverterArc(node.arc(), returnType, sizer);
            var result = new LinkedList<>(nested);
            result.addFirst(arc);
            return result;
        } else if (types.isAssignable(to, node.to()) && isPathLegal(node, from, to, mixins)) {
            var arc = new DaedalusConverterArc(node.arc(), node.arc().returnType(), sizer);
            return List.of(arc);
        } else if (isPathLegal(node, from, node.to(), mixins)) {
            var toKey = node.to().toString();
            if (visited.contains(toKey)) {
                return List.of();
            }

            var newVisited = new HashSet<>(visited);
            newVisited.add(toKey);
            var nested = findAnyPath(node.to(), to, mixins, newVisited);
            if (nested.isEmpty()) {
                return List.of();
            }

            var arc = new DaedalusConverterArc(node.arc(), node.arc().returnType(), sizer);
            var result = new LinkedList<>(nested);
            result.addFirst(arc);
            return result;
        } else {
            return List.of();
        }
    }

    private boolean isPathLegal(DaedalusConverterNode node, TypeMirror from, TypeMirror to, Set<TypeElement> mixins) {
        return isPathLegalThroughMixin(node, mixins)
                || isPathLegalThroughInterpretedObject(node, from)
                || isPathLegalThroughInterpretedObject(node, to)
                || isPathLegalThroughObject(node, from)
                || isPathLegalThroughObject(node, to);
    }

    private boolean isPathLegalThroughMixin(DaedalusConverterNode node, Set<TypeElement> mixins) {
        return mixins.stream()
                .anyMatch(mixin -> Objects.equals(node.arc().ownerName(), types.erase(mixin.asType()).toString()));
    }

    private boolean isPathLegalThroughInterpretedObject(DaedalusConverterNode node, TypeMirror type) {
        return type instanceof DeclaredType firstDeclaredType
                && firstDeclaredType.asElement() instanceof TypeElement firstTypeElement
                && firstTypeElement.getQualifiedName().contentEquals(node.arc().ownerName());
    }

    private boolean isPathLegalThroughObject(DaedalusConverterNode node, TypeMirror type) {
        return isManagedType(type)
                && Objects.equals(getSpecName(type), node.arc().ownerName());
    }

    private int countTypeArguments(TypeMirror type) {
        var counter = 0;
        var queue = new LinkedList<TypeMirror>();
        queue.add(type);
        while (!queue.isEmpty()) {
            type = queue.pop();
            if (type.getKind() != TypeKind.TYPEVAR && type instanceof DeclaredType declaredType) {
                var args = declaredType.getTypeArguments();
                counter += args.size();
                queue.addAll(args);
            }
        }
        return counter;
    }
}
