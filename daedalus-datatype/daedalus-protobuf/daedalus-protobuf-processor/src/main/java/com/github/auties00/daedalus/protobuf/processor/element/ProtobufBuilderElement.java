package com.github.auties00.daedalus.protobuf.processor.element;

import com.github.auties00.daedalus.processor.model.DaedalusBuilderElement;
import com.github.auties00.daedalus.typesystem.model.ClassModifier;

import javax.lang.model.element.ExecutableElement;
import java.util.SequencedCollection;

/**
 * A protobuf builder element that carries the full set of attributes from
 * the {@link com.github.auties00.daedalus.typesystem.annotation.TypeBuilder @TypeBuilder}
 * annotation, including class modifiers, setter naming segments, and the
 * terminal build method name.
 *
 * <p>Each instance corresponds to a single annotated constructor or factory
 * method on a {@code @ProtobufMessage} type and provides all metadata needed
 * to generate the builder class.
 *
 * @param name the builder class name, derived from
 *        {@code TypeBuilder.name()}
 * @param modifiers the class modifiers for the generated builder, derived from
 *        {@code TypeBuilder.modifiers()}
 * @param buildMethodName the terminal build method name, derived from
 *        {@code TypeBuilder.buildMethodName()}
 * @param settersMethodNameSegments the setter name segments, derived from
 *        {@code TypeBuilder.settersMethodName()}
 * @param parameters the builder parameters
 * @param delegate the annotated method or constructor that the builder delegates to
 */
public record ProtobufBuilderElement(
        String name,
        ClassModifier[] modifiers,
        String buildMethodName,
        String[] settersMethodNameSegments,
        SequencedCollection<ProtobufBuilderParameterElement> parameters,
        ExecutableElement delegate
) implements DaedalusBuilderElement {

}
