package com.github.auties00.daedalus.processor.graph;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A conversion that the path search of {@code DaedalusConverterGraph} can apply to
 * leave a state.
 *
 * @param source the state the conversion is applied to
 * @param arc the conversion
 * @param type the type produced by the conversion
 * @param terminal whether the produced type is assignable to the target type of the
 *        search, which makes this conversion the last one of a path
 * @param annotatedArcs the number of conversions, among those that lead from the initial
 *        state of the search to the type produced by this hop, that require a companion
 *        parameters annotation
 * @param inexactArcs the number of conversions, among those that lead from the initial
 *        state of the search to the type produced by this hop, whose declared source type
 *        is not the type they are applied to, but one of its supertypes, plus one if this
 *        hop is terminal and produces a subtype of the target type instead of the target
 *        type itself
 */
record DaedalusSearchHop(
        DaedalusSearchState source,
        DaedalusConverterArc arc,
        TypeMirror type,
        boolean terminal,
        int annotatedArcs,
        int inexactArcs
) {
    /**
     * The order of precedence of the hops of the same depth.
     *
     * <p>A hop whose path uses more companion parameters annotations comes first, as a
     * use site that supplies the arguments of a converter expects that converter to be
     * used. Among those, a hop whose path applies fewer converters to a subtype of their
     * declared source type, and whose last converter produces exactly the target type,
     * comes first, as the most specific converter of a type is the one that knows how to
     * convert it, and it must be the same converter in both directions. The order is only meaningful when used by a stable sort, which keeps
     * the order in which the converters were linked for hops that are otherwise equal.
     */
    static final Comparator<DaedalusSearchHop> PRECEDENCE = Comparator.comparingInt(DaedalusSearchHop::annotatedArcs)
            .reversed()
            .thenComparingInt(DaedalusSearchHop::inexactArcs);

    /**
     * Returns the conversions that lead from the initial state of the search to the
     * type produced by this hop, in order.
     *
     * @return the conversions forming the path, ending with the conversion of this hop
     */
    List<DaedalusConverterArc> toPath() {
        var path = new ArrayList<DaedalusConverterArc>();
        path.add(arc);
        for (var state = source; state.arc() != null; state = state.previous()) {
            path.add(state.arc());
        }
        return path.reversed();
    }
}
