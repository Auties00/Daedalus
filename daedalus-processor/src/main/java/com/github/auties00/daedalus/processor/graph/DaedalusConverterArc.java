package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import javax.lang.model.type.TypeMirror;

/**
 * An arc in the converter graph representing a resolved conversion step with its actual return type.
 *
 * @param method the converter method for this step
 * @param returnType the resolved return type (may differ from the method's declared return type
 *        when generics are involved)
 */
public record DaedalusConverterArc(
        DaedalusMethodElement method,
        TypeMirror returnType
) {

}
