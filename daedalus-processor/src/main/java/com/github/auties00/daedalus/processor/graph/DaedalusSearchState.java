package com.github.auties00.daedalus.processor.graph;

import javax.lang.model.type.TypeMirror;

/**
 * An intermediate type reached by the path search of {@code DaedalusConverterGraph}.
 *
 * <p>Each state links back to the state it was reached from, and to the conversion
 * that reached it, so that the path leading to it can be rebuilt once the target type
 * is reached. The initial state, which holds the source type, has neither.
 *
 * @param type the type reached by this state
 * @param previous the state this state was reached from, or {@code null} for the
 *        initial state
 * @param arc the conversion that reached this state from {@code previous}, or
 *        {@code null} for the initial state
 * @param annotatedArcs the number of conversions of the path leading to this state that
 *        require a companion parameters annotation
 * @param inexactArcs the number of conversions of the path leading to this state whose
 *        declared source type is a supertype of the type they are applied to
 */
record DaedalusSearchState(
        TypeMirror type,
        DaedalusSearchState previous,
        DaedalusConverterArc arc,
        int annotatedArcs,
        int inexactArcs
) {

}
