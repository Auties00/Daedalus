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
    private final List<DaedalusProcessorExtension> extensions;
    private final Set<DaedalusConverterNode> nodes;

    /**
     * Constructs a new converter graph with the given type utilities.
     *
     * @param types      the common type utilities
     * @param extensions the extensions
     */
    public DaedalusConverterGraph(DaedalusTypeManager types, List<DaedalusProcessorExtension> extensions) {
        this.types = types;
        this.extensions = extensions;
        this.nodes = new HashSet<>();
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
     * Links a converter method into the graph as a transformation from one type to another.
     *
     * @param from the source type
     * @param to the target type
     * @param arc the converter method
     */
    public void link(TypeMirror from, TypeMirror to, DaedalusMethodElement arc) {
        var node = new DaedalusConverterNode(from, to, arc);
        nodes.add(node);
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
    public List<DaedalusConverterArc> findPath(TypeMirror from, TypeMirror to, List<TypeElement> mixins) {
        var mixinsSet = Set.copyOf(mixins);
        var visited = new HashSet<String>();
        visited.add(from.toString());
        return findAnyPath(from, to, mixinsSet, visited);
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
        } else if (node.arc().isParametrized()) {
            var returnType = node.arc()
                    .element()
                    .map(element -> types.getReturnType(element, List.of(from)))
                    .orElse(node.arc().returnType());
            if (types.isAssignable(to, returnType, false)) {
                var arc = new DaedalusConverterArc(node.arc(), returnType);
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

            var arc = new DaedalusConverterArc(node.arc(), returnType);
            var result = new LinkedList<>(nested);
            result.addFirst(arc);
            return result;
        } else if (types.isAssignable(to, node.to()) && isPathLegal(node, from, to, mixins)) {
            var arc = new DaedalusConverterArc(node.arc(), node.arc().returnType());
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

            var arc = new DaedalusConverterArc(node.arc(), node.arc().returnType());
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
