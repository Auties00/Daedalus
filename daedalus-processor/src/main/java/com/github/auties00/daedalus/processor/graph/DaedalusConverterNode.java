package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import javax.lang.model.type.TypeMirror;

/**
 * A node in the converter graph representing a type transformation.
 *
 * @param from the source type of the transformation
 * @param to the target type of the transformation
 * @param arc the converter method that performs the transformation
 */
record DaedalusConverterNode(
        TypeMirror from,
        TypeMirror to,
        DaedalusMethodElement arc
) {

}
