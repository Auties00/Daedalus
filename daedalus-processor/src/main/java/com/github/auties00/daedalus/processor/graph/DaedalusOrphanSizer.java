package com.github.auties00.daedalus.processor.graph;

import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;

import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * A pending sizer registration that has not yet been matched to a serializer node.
 *
 * <p>An orphan is created when {@code DaedalusConverterGraph#attachSizer} is called
 * before any serializer for {@code valueType} has been linked into the graph. The
 * orphan stays in the graph's pending queue until a matching
 * {@code linkSerializer} call consumes it and attaches the {@link #sizer} to the
 * newly added serializer node.
 *
 * @param valueType the value type the sizer applies to
 * @param sizer the size calculation method
 * @param parametersAnnotations the companion parameters annotations
 *        required to invoke {@code sizer}
 */
record DaedalusOrphanSizer(
        TypeMirror valueType,
        DaedalusMethodElement sizer,
        List<DaedalusConverterParametersElement> parametersAnnotations
) {

}
