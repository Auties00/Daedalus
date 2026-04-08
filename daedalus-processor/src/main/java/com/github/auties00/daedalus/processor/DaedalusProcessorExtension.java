package com.github.auties00.daedalus.processor;

import com.github.auties00.daedalus.processor.graph.DaedalusConverterGraph;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusMixinScopeManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * A service provider interface for data format extensions to the Daedalus annotation processor.
 *
 * <p>Implementations of this interface are discovered at compile time via
 * {@link java.util.ServiceLoader} by the {@code DaedalusProcessor}. Each extension handles
 * a specific data format (e.g. Protocol Buffers, JSON) and can coexist with other extensions
 * in the same compilation unit.
 */
public interface DaedalusProcessorExtension {

    /**
     * Returns the unique name of this extension.
     *
     * @return the extension name
     */
    String name();

    /**
     * Returns the set of fully qualified annotation type names that this extension processes.
     *
     * @return the supported annotation types
     */
    Set<String> supportedAnnotationTypes();

    /**
     * Initializes this extension with the common processing utilities.
     *
     * @param processingEnv the annotation processing environment
     * @param types the common type utilities
     * @param messages the compiler diagnostics utility
     * @param mixinScopeResolver the mixin scope resolver for auto-applying scoped mixins
     */
    void init(ProcessingEnvironment processingEnv, DaedalusTypeManager types, DaedalusLogManager messages, DaedalusMixinScopeManager mixinScopeResolver);

    /**
     * Returns whether the given type is managed by this extension.
     *
     * <p>A managed type is one that this extension processes and generates code for.
     * This method is used by the converter graph to determine path legality.
     *
     * @param type the type to check
     * @return {@code true} if the type is managed by this extension
     */
    boolean isManagedType(TypeMirror type);

    /**
     * Runs format specific validation checks on the annotated elements in the given round.
     *
     * @param roundEnv the current round environment
     */
    void runChecks(RoundEnvironment roundEnv);

    /**
     * Processes annotated types from the round environment into processed objects.
     *
     * <p>During this phase, extensions should also populate the provided converter graph
     * with synthetic entries for their managed types (e.g. encode/decode methods in
     * generated Spec classes).
     *
     * @param roundEnv the current round environment
     * @param converterGraph the shared converter graph
     * @return the processed objects
     */
    SequencedCollection<? extends DaedalusTypeElement> processObjects(
            RoundEnvironment roundEnv,
            DaedalusConverterGraph converterGraph
    );

    /**
     * Generates code for the given processed objects.
     *
     * <p>This method is called after all objects from all extensions have been attributed
     * (i.e. their converters have been resolved via the shared converter graph).
     *
     * @param objects the processed objects to generate code for
     */
    void generateCode(SequencedCollection<? extends DaedalusTypeElement> objects);

    /**
     * Returns the set of I/O types supported by this extension.
     *
     * <p>I/O types are reader and writer types that the framework automatically
     * supplies as additional parameters to {@code @TypeSerializer} and
     * {@code @TypeDeserializer} methods. For example, the protobuf extension supports
     * {@code ProtobufBinaryWriter} as the second parameter of serializer methods and
     * {@code ProtobufBinaryReader} as the first parameter of deserializer methods.
     *
     * <p>Parameters whose types match one of the supported I/O types are excluded
     * from the companion parameters annotation because they are supplied by the
     * generated code, not by a use-site annotation.
     *
     * <p>The default implementation returns an empty set.
     *
     * @return the set of supported I/O types
     */
    default Set<TypeMirror> supportedIOTypes() {
        return Set.of();
    }
}
