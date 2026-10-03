package com.github.auties00.daedalus.processor.graph;

import java.util.List;
import java.util.Objects;

/**
 * The result of a path search in a {@link DaedalusConverterGraph}.
 *
 * <p>A search has one of three outcomes:
 * <ul>
 * <li>{@link Found}: a path exists whose converters only require the companion
 *     parameters annotations supplied by the use site.
 * <li>{@link MissingArguments}: no such path exists, but some paths would exist if the
 *     use site supplied additional companion parameters annotations.
 * <li>{@link NotFound}: no path exists at all.
 * </ul>
 *
 * <p>Instances of this interface are immutable and thread-safe.
 */
public sealed interface DaedalusConverterPath {
    /**
     * A path whose converters can all be invoked by the use site.
     *
     * @param arcs the conversion arcs forming the path, in order
     */
    record Found(List<DaedalusConverterArc> arcs) implements DaedalusConverterPath {
        /**
         * Constructs a new found path.
         *
         * @param arcs the conversion arcs forming the path, in order
         * @throws NullPointerException if {@code arcs} is {@code null}
         */
        public Found {
            Objects.requireNonNull(arcs, "arcs cannot be null");
        }
    }

    /**
     * The paths that would be valid if the use site supplied the arguments of the
     * additional parameters of some of their converters.
     *
     * <p>Every candidate is a shortest path in the first mixin tier that contains one,
     * and contains at least one arc whose required companion parameters annotation is
     * not supplied by the use site.
     *
     * @param candidates the candidate paths, each made of conversion arcs in order
     */
    record MissingArguments(List<List<DaedalusConverterArc>> candidates) implements DaedalusConverterPath {
        /**
         * Constructs a new missing arguments result.
         *
         * @param candidates the candidate paths
         * @throws NullPointerException if {@code candidates} is {@code null}
         */
        public MissingArguments {
            Objects.requireNonNull(candidates, "candidates cannot be null");
        }
    }

    /**
     * The absence of any path, even ignoring the arguments required by converters.
     */
    record NotFound() implements DaedalusConverterPath {

    }
}
