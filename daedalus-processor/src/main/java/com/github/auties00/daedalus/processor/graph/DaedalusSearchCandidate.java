package com.github.auties00.daedalus.processor.graph;

/**
 * A converter that the path search of {@code DaedalusConverterGraph} tries to apply
 * to a state.
 *
 * <p>Candidates are created for every state of a depth of the search and every
 * converter of the graph, so that they can be evaluated in parallel.
 *
 * @param state the state the converter is applied to
 * @param node the converter to apply
 */
record DaedalusSearchCandidate(
        DaedalusSearchState state,
        DaedalusConverterNode node
) {

}
