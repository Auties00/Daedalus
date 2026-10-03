package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * An arc in the converter graph representing a resolved conversion step with its actual return type.
 *
 * @param method the converter method for this step
 * @param returnType the resolved return type (may differ from the method's declared return type
 *        when generics are involved)
 * @param sizer the size calculation method for the source type of this step, or {@code null}
 *        if none is registered. Only set for arcs that originate from a serializer node with
 *        an associated {@code @TypeSize} method.
 * @param parametersAnnotations the companion parameters annotations that
 *        a use site must supply to invoke {@code method} and {@code sizer}, empty if
 *        neither declares an annotated parameter
 */
public record DaedalusConverterArc(
        DaedalusMethodElement method,
        TypeMirror returnType,
        DaedalusMethodElement sizer,
        List<DaedalusConverterParametersElement> parametersAnnotations
) {

}
