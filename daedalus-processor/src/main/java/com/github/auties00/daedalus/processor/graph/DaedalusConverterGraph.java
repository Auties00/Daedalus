package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.DaedalusProcessorExtension;
import com.github.auties00.daedalus.processor.generator.DaedalusMethodGenerator;
import com.github.auties00.daedalus.processor.manager.DaedalusConverterParametersManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;
import com.github.auties00.daedalus.typesystem.exception.TypeDeserializationException;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A unified graph of type converters that resolves multi-step conversion paths between types.
 *
 * <p>The graph is populated with converter methods annotated with {@link TypeSerializer}
 * or {@link TypeDeserializer}. Both serializer and deserializer converters coexist in the
 * same graph, allowing path discovery to mix conversion directions when necessary (e.g.
 * unwrapping an {@code Optional} during deserialization). Paths are found with a breadth
 * first search that expands every intermediate type at most once, which prevents infinite
 * loops that could otherwise arise from bidirectional converter pairs, and returns a path
 * with the fewest conversions.
 *
 * <p>Given a source type and target type, the graph finds a path of converter methods that
 * transforms from source to target, including through intermediate types and generic type
 * resolution.
 *
 * <p>A converter that declares additional parameters can only be invoked by a use site
 * that supplies their arguments through the companion parameters annotation of the
 * converter, as described by {@link DaedalusConverterParametersManager}. Such a converter
 * is only part of a path when the use site supplies that annotation. When no path exists
 * under this constraint, the search is repeated without it, so that the paths that would
 * exist if the use site supplied the missing annotations can be reported.
 *
 * <p>An enum that no format manages, such as a JDK enum, is converted from and to the
 * {@code String} that contains the name of its constants. These converters are linked the
 * first time a path from or to the enum is searched. They never rely on reflection: a
 * constant is serialized with {@link Enum#name()}, and deserialized with a {@code switch}
 * over the constants that the enum declares at compile time, as
 * {@link Enum#valueOf(Class, String)} looks them up reflectively.
 */
public final class DaedalusConverterGraph {
    /**
     * The name of the pattern variable that the deserializer of an unmanaged enum binds to
     * a name that matches none of its constants.
     */
    private static final String UNKNOWN_CONSTANT_VARIABLE = "daedalusUnknownConstant";

    private final DaedalusTypeManager types;
    private final DaedalusConverterParametersManager parameters;
    private final SequencedCollection<DaedalusProcessorExtension> extensions;
    private final Set<DaedalusConverterNode> nodes;
    private final SequencedCollection<DaedalusOrphanSizer> orphanedSizers;

    /**
     * The qualified names of the unmanaged enums whose converters are linked.
     */
    private final Set<String> linkedEnums;

    /**
     * The converters linked for unmanaged enums, which are a fallback: they are only part
     * of a path when the last mixin tier is searched, so that any mixin that converts an
     * enum takes precedence over them. The set is only modified before a search starts.
     */
    private final Set<DaedalusMethodElement> fallbackConverters;

    /**
     * Constructs a new converter graph with the given type utilities.
     *
     * @param types      the common type utilities
     * @param parameters the manager of the additional parameters of converters
     * @param extensions the extensions
     */
    public DaedalusConverterGraph(DaedalusTypeManager types, DaedalusConverterParametersManager parameters, SequencedCollection<DaedalusProcessorExtension> extensions) {
        this.types = types;
        this.parameters = parameters;
        this.extensions = extensions;
        this.nodes = new LinkedHashSet<>();
        this.orphanedSizers = new ArrayList<>();
        this.linkedEnums = new HashSet<>();
        this.fallbackConverters = new HashSet<>();
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
        resolve(from, to, arc);
        var serializer = new DaedalusConverterNode.Serializer(from, to, arc, getParametersAnnotations(arc));
        orphanedSizers.parallelStream()
                .filter(orphan -> types.isSameType(orphan.valueType(), from, false))
                .findAny()
                .ifPresent(orphan -> {
                    serializer.setSizer(orphan.sizer(), orphan.parametersAnnotations());
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
        resolve(from, to, arc);
        nodes.add(new DaedalusConverterNode.Deserializer(from, to, arc, getParametersAnnotations(arc)));
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
        types.resolve(valueType);
        sizer.element().ifPresent(types::resolve);
        var sizerParametersAnnotations = getParametersAnnotations(sizer);
        var matches = nodes.parallelStream()
                .filter(node -> node instanceof DaedalusConverterNode.Serializer serializer
                        && serializer.sizer() == null
                        && types.isSameType(serializer.from(), valueType, false))
                .map(node -> (DaedalusConverterNode.Serializer) node)
                .toList();
        if (matches.isEmpty()) {
            orphanedSizers.add(new DaedalusOrphanSizer(valueType, sizer, sizerParametersAnnotations));
        } else {
            for (var match : matches) {
                match.setSizer(sizer, sizerParametersAnnotations);
            }
        }
    }

    /**
     * Returns the companion parameters annotations required to
     * invoke the given converter method.
     *
     * <p>The names are computed when a converter is linked, as the path search runs in
     * parallel and must not query javac.
     *
     * @param method the converter method
     * @return the annotation names, empty if the method declares no annotated parameter
     */
    private List<DaedalusConverterParametersElement> getParametersAnnotations(DaedalusMethodElement method) {
        return parameters.getRequiredAnnotation(method)
                .map(List::of)
                .orElse(List.of());
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
     * Finds the shortest conversion path from one type to another, considering only
     * converters that are accessible through the given mixins.
     *
     * <p>The mixin tiers are searched in order, and a tier is only searched if no path
     * exists in the previous ones, so that an explicitly referenced mixin always takes
     * precedence over a scoped one. Within a tier, the path with the fewest conversions
     * is returned. Paths of the same length are ordered by the number of their converters
     * that use a companion parameters annotation supplied by the use site, then by the
     * number of their converters that are applied to a subtype of their declared source
     * type, and then by the order in which their converters were linked, so the result is
     * deterministic.
     *
     * <p>A converter that requires a companion parameters annotation which is not
     * contained in {@code suppliedAnnotations} is never part of a found path. If no path
     * exists under this constraint, the tiers are searched again while ignoring it, and
     * the shortest paths of the first tier that contains any are returned as the
     * candidates of a {@link DaedalusConverterPath.MissingArguments} result.
     *
     * @param from the source type
     * @param to the target type
     * @param mixins the mixins that provide accessible converters, whose tiers are searched
     *        in order of decreasing precedence
     * @param suppliedAnnotations the companion parameters annotations
     *        supplied by the use site
     * @return the result of the search
     */
    public DaedalusConverterPath findPath(TypeMirror from, TypeMirror to, DaedalusMixins mixins, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        linkUnmanagedEnums(from);
        linkUnmanagedEnums(to);
        types.resolve(from);
        types.resolve(to);
        for (var mixin : mixins.all()) {
            types.resolve(mixin.asType());
        }

        var lastTier = mixins.tiers().getLast();
        for (var tier : mixins.tiers()) {
            var paths = findShortestPaths(from, to, mixins, tier, tier == lastTier, suppliedAnnotations);
            if (!paths.isEmpty()) {
                return new DaedalusConverterPath.Found(paths.getFirst());
            }
        }

        for (var tier : mixins.tiers()) {
            var paths = findShortestPaths(from, to, mixins, tier, tier == lastTier, null);
            if (!paths.isEmpty()) {
                return new DaedalusConverterPath.MissingArguments(paths);
            }
        }

        return new DaedalusConverterPath.NotFound();
    }

    /**
     * Links the converters of every unmanaged enum that is the given type or one of its
     * type arguments, at any depth, unless they are already linked.
     *
     * <p>The serializer of an enum is its {@link Enum#name()} method, and its deserializer
     * is an inline {@code switch} over the names of the constants the enum declares,
     * which returns {@code null} for a {@code null} name and throws a
     * {@code TypeDeserializationException} for an unknown one. An enum is unmanaged if no
     * format extension manages it, as the extension that manages an enum provides its
     * own converters.
     *
     * @param type the type to inspect
     */
    private void linkUnmanagedEnums(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return;
        }

        for (var typeArgument : declaredType.getTypeArguments()) {
            linkUnmanagedEnums(typeArgument);
        }

        if (!(declaredType.asElement() instanceof TypeElement element)
                || element.getKind() != ElementKind.ENUM
                || isManagedType(type)) {
            return;
        }

        var enumName = element.getQualifiedName().toString();
        if (!linkedEnums.add(enumName)) {
            return;
        }

        var enumType = types.erase(type);
        var stringType = types.getType(String.class);
        var serializer = DaedalusMethodElement.of(enumName, Set.of(Modifier.PUBLIC), stringType, "name", enumType);
        fallbackConverters.add(serializer);
        linkSerializer(enumType, stringType, serializer);

        var suffix = new StringBuilder(") { case null -> null; ");
        for (var constant : element.getEnclosedElements()) {
            if (constant.getKind() == ElementKind.ENUM_CONSTANT) {
                suffix.append("case \"%s\" -> %s.%s; ".formatted(constant.getSimpleName(), enumName, constant.getSimpleName()));
            }
        }
        suffix.append("case java.lang.String %s -> throw %s.malformedValue(%s.class, %s, null); }".formatted(
                UNKNOWN_CONSTANT_VARIABLE,
                TypeDeserializationException.class.getName(),
                enumName,
                UNKNOWN_CONSTANT_VARIABLE
        ));
        var deserializer = DaedalusMethodElement.inline(
                enumName,
                "ofName",
                enumType,
                stringType,
                "switch (",
                suffix.toString(),
                List.of(types.getType(TypeDeserializationException.class))
        );
        fallbackConverters.add(deserializer);
        linkDeserializer(stringType, enumType, deserializer);
    }

    /**
     * Resolves the classes referenced by a converter before it is linked, so that
     * the parallel path search never waits for the type manager to resolve them.
     *
     * @param from the source type of the converter
     * @param to the target type of the converter
     * @param arc the converter method
     */
    private void resolve(TypeMirror from, TypeMirror to, DaedalusMethodElement arc) {
        types.resolve(from);
        types.resolve(to);
        arc.element().ifPresent(types::resolve);
    }

    /**
     * Finds the shortest conversion paths from one type to another with a breadth first
     * search, considering only converters that are accessible through the given mixins.
     *
     * <p>A search state is an intermediate type, identified by its string form, and is
     * expanded at most once. Every hop that can leave the states of the current depth is
     * computed in parallel, and the hops are then processed in the order of their source
     * state and of their converter, so the first depth that contains a hop that reaches
     * the target type contains the shortest paths, and the first of them always breaks
     * ties in the same way.
     *
     * <p>Before they are processed, the hops of a depth are ordered by
     * {@link DaedalusSearchHop#PRECEDENCE}: a path that uses more of the companion
     * parameters annotations supplied by the use site comes first, and then a path whose
     * converters match the exact types they are applied to. This order also decides which
     * path reaches an intermediate type first.
     *
     * @param from the source type
     * @param to the target type
     * @param mixins the mixins that provide accessible converters
     * @param tier the broadest scope of the mixins that are searched
     * @param lastTier whether the tier is the last one, which contains every applicable
     *        mixin
     * @param suppliedAnnotations the companion parameters annotations
     *        supplied by the use site, or {@code null} to accept converters
     *        regardless of the annotations they require
     * @return the shortest paths, each made of conversion arcs in order, in the order of
     *         precedence, or an empty list if no path exists
     */
    private List<List<DaedalusConverterArc>> findShortestPaths(TypeMirror from, TypeMirror to, DaedalusMixins mixins, TypeMixin.Scope tier, boolean lastTier, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        var visited = new HashSet<String>();
        visited.add(from.toString());
        var frontier = List.of(new DaedalusSearchState(from, null, null, 0, 0));
        while (!frontier.isEmpty()) {
            var candidates = new ArrayList<DaedalusSearchCandidate>(frontier.size() * nodes.size());
            for (var state : frontier) {
                for (var node : nodes) {
                    candidates.add(new DaedalusSearchCandidate(state, node));
                }
            }

            var hops = candidates.parallelStream()
                    .map(candidate -> findHop(candidate.state(), candidate.node(), to, mixins, tier, lastTier, suppliedAnnotations))
                    .filter(Objects::nonNull)
                    .sorted(DaedalusSearchHop.PRECEDENCE)
                    .collect(Collectors.toCollection(ArrayList::new));

            var paths = new ArrayList<List<DaedalusConverterArc>>();
            for (var hop : hops) {
                if (hop.terminal()) {
                    paths.add(hop.toPath());
                }
            }
            if (!paths.isEmpty()) {
                return paths;
            }

            var nextFrontier = new ArrayList<DaedalusSearchState>();
            for (var hop : hops) {
                if (visited.add(hop.type().toString())) {
                    nextFrontier.add(new DaedalusSearchState(hop.type(), hop.source(), hop.arc(), hop.annotatedArcs(), hop.inexactArcs()));
                }
            }
            frontier = nextFrontier;
        }
        return List.of();
    }

    /**
     * Returns the hop that the given converter allows from the given state, or
     * {@code null} if the converter cannot be applied.
     *
     * <p>A converter can be applied when the type of the state is assignable to its
     * parameter. A hop is terminal when the converter returns a type that is assignable
     * to the target type: the return type of a parametrized converter is inferred from
     * the type of the state. A hop through a converter must be legal, as defined by
     * {@link #isPathLegal(DaedalusConverterNode, TypeMirror, TypeMirror, DaedalusMixins, TypeMixin.Scope)}. The
     * legality of a terminal hop through a parametrized converter is only checked when
     * searching a tier that precedes the last one, while the last tier accepts it
     * unconditionally. A parametrized converter whose return type has more type
     * arguments than both the target type and the type of the state cannot lead to
     * the target type, as it only nests types further, so it is never applied to an
     * intermediate hop. A converter that requires a companion parameters annotation not
     * contained in {@code suppliedAnnotations} cannot be applied, unless
     * {@code suppliedAnnotations} is {@code null}.
     *
     * @param state the state to leave
     * @param node the converter to apply
     * @param to the target type
     * @param mixins the mixins that provide accessible converters
     * @param tier the broadest scope of the mixins that are searched
     * @param lastTier whether the tier is the last one
     * @param suppliedAnnotations the companion parameters annotations supplied by the use
     *        site, or {@code null} to apply the converter regardless of the annotations it
     *        requires
     * @return the hop, or {@code null} if the converter cannot be applied
     */
    private DaedalusSearchHop findHop(DaedalusSearchState state, DaedalusConverterNode node, TypeMirror to, DaedalusMixins mixins, TypeMixin.Scope tier, boolean lastTier, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        if (suppliedAnnotations != null && !suppliedAnnotations.containsAll(node.parametersAnnotations())) {
            return null;
        }

        if (!lastTier && fallbackConverters.contains(node.arc())) {
            return null;
        }

        var from = state.type();
        if (!types.isAssignable(from, node.from())) {
            return null;
        }

        var annotatedArcs = state.annotatedArcs() + (node.parametersAnnotations().isEmpty() ? 0 : 1);
        var inexactArcs = state.inexactArcs() + (types.isSameType(from, node.from(), true) ? 0 : 1);

        var sizer = switch (node) {
            case DaedalusConverterNode.Serializer serializer -> serializer.sizer();
            case DaedalusConverterNode.Deserializer ignored -> null;
        };
        if (node.arc().isParametrized()) {
            var returnType = node.arc()
                    .element()
                    .map(element -> types.getReturnType(element, List.of(from)))
                    .orElse(node.arc().returnType());
            var arc = new DaedalusConverterArc(node.arc(), returnType, sizer, node.parametersAnnotations());
            if (types.isAssignable(returnType, to, false) && (lastTier || isPathLegal(node, from, to, mixins, tier))) {
                return new DaedalusSearchHop(state, arc, returnType, true, annotatedArcs, inexactArcs + getInexactResult(returnType, to));
            }

            var length = countTypeArguments(returnType);
            if (length > countTypeArguments(to) && length > countTypeArguments(from)) {
                return null;
            }

            if (!isPathLegal(node, from, to, mixins, tier)) {
                return null;
            }

            return new DaedalusSearchHop(state, arc, returnType, false, annotatedArcs, inexactArcs);
        }

        var arc = new DaedalusConverterArc(node.arc(), node.arc().returnType(), sizer, node.parametersAnnotations());
        if (types.isAssignable(node.to(), to) && isPathLegal(node, from, to, mixins, tier)) {
            return new DaedalusSearchHop(state, arc, node.to(), true, annotatedArcs, inexactArcs + getInexactResult(node.to(), to));
        }

        if (isPathLegal(node, from, node.to(), mixins, tier)) {
            return new DaedalusSearchHop(state, arc, node.to(), false, annotatedArcs, inexactArcs);
        }

        return null;
    }

    /**
     * Returns whether the last conversion of a path produces a subtype of the target type
     * instead of the target type itself, as a count to add to the inexact conversions of
     * the path.
     *
     * <p>A converter that produces exactly the target type is the one declared for it,
     * so it takes precedence over a converter declared for one of its subtypes: for
     * example, a {@code java.util.Date} must be deserialized by its own converter, and not
     * by the one of {@code java.sql.Date}, although the latter also produces a
     * {@code java.util.Date}.
     *
     * @param produced the type produced by the last conversion
     * @param target the target type of the path
     * @return {@code 0} if {@code produced} is the target type, or {@code 1} otherwise
     */
    private int getInexactResult(TypeMirror produced, TypeMirror target) {
        return types.isSameType(produced, target, true) ? 0 : 1;
    }

    private boolean isPathLegal(DaedalusConverterNode node, TypeMirror from, TypeMirror to, DaedalusMixins mixins, TypeMixin.Scope tier) {
        return mixins.appliesUpTo(node.arc().ownerName(), tier)
                || isPathLegalThroughInterpretedObject(node, from)
                || isPathLegalThroughInterpretedObject(node, to)
                || isPathLegalThroughObject(node, from)
                || isPathLegalThroughObject(node, to);
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
            // Never query the kind of a declared type: javac would load its class, which is not thread-safe
            if (type instanceof DeclaredType declaredType) {
                var args = declaredType.getTypeArguments();
                counter += args.size();
                queue.addAll(args);
            }
        }
        return counter;
    }
}
