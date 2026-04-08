package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import javax.lang.model.type.TypeMirror;

/**
 * An arc in the converter graph representing a resolved conversion step with its actual return type.
 *
 * @param method the converter method for this step
 * @param returnType the resolved return type (may differ from the method's declared return type
 *        when generics are involved)
 * @param sizer the size calculation method for the source type of this step, or {@code null}
 *        if none is registered. Only set for arcs that originate from a serializer node with
 *        an associated {@code @TypeSize} method.
 */
public record DaedalusConverterArc(
        DaedalusMethodElement method,
        TypeMirror returnType,
        DaedalusMethodElement sizer
) {

}
