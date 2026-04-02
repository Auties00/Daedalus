package com.github.auties00.daedalus.processor;

import com.github.auties00.daedalus.processor.graph.DaedalusConverterGraph;
import com.github.auties00.daedalus.processor.manager.DaedalusLogManager;
import com.github.auties00.daedalus.processor.manager.DaedalusTypeManager;
import com.github.auties00.daedalus.processor.model.DaedalusConverterElement;
import com.github.auties00.daedalus.processor.type.DaedalusFieldType;
import com.github.auties00.daedalus.processor.model.DaedalusTypeElement;
import com.github.auties00.daedalus.processor.manager.DaedalusValidationManager;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Supplier;

/**
 * The main annotation processor that hosts the SPI extension system for the
 * Daedalus serialization framework.
 *
 * <p>This processor discovers {@link DaedalusProcessorExtension} implementations via
 * {@link ServiceLoader} and orchestrates a five-phase processing pipeline:
 * <ul>
 * <li>Phase 1: Run common type system validation checks
 * <li>Phase 2: Run extension-specific validation checks
 * <li>Phase 3: Process annotated objects (extensions populate converter graphs)
 * <li>Phase 4: Attribute all processed objects (resolve converter paths)
 * <li>Phase 5: Generate code for each extension
 * </ul>
 *
 * <p>The processor aggregates the supported annotation types from all discovered
 * extensions, together with the common type system annotations handled by
 * {@link DaedalusValidationManager}. Extensions are initialized once during {@link #init(ProcessingEnvironment)}
 * and share a common converter graph for cross-format converter resolution.
 *
 * <p>This processor includes IntelliJ IDEA compatibility logic that unwraps the
 * processing environment when running inside the JPS build system.
 */
public class DaedalusProcessor extends AbstractProcessor {

    /**
     * The common type utilities shared across all extensions.
     */
    private DaedalusTypeManager types;

    /**
     * The compiler diagnostics utility.
     */
    private DaedalusLogManager messages;

    /**
     * The common validation checks instance.
     */
    private DaedalusValidationManager validator;

    /**
     * The unified converter graph shared across all extensions.
     */
    private DaedalusConverterGraph converterGraph;

    /**
     * The discovered extension implementations.
     */
    private List<DaedalusProcessorExtension> extensions;

    /**
     * Initializes this processor by discovering extensions via {@link ServiceLoader}
     * and creating the shared type utilities, diagnostics, checks, and converter graphs.
     *
     * <p>The processing environment is unwrapped if running inside IntelliJ IDEA's
     * JPS build system to ensure compatibility with compiler internals.
     *
     * @param wrapperProcessingEnv the annotation processing environment, possibly
     *        wrapped by IntelliJ's JPS API
     */
    @Override
    public synchronized void init(ProcessingEnvironment wrapperProcessingEnv) {
        var unwrappedProcessingEnv = unwrapProcessingEnv(wrapperProcessingEnv);
        super.init(unwrappedProcessingEnv);
        this.types = new DaedalusTypeManager(processingEnv);
        this.messages = new DaedalusLogManager(processingEnv);
        this.extensions = ServiceLoader.load(DaedalusProcessorExtension.class, getClass().getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        this.validator = new DaedalusValidationManager(types, messages) {
            @Override
            protected boolean isFormatManagedType(TypeMirror type) {
                return extensions.stream().anyMatch(ext -> ext.isManagedType(type));
            }
        };
        this.converterGraph = new DaedalusConverterGraph(types, extensions);
        for (var extension : extensions) {
            extension.init(processingEnv, types, messages);
        }
    }

    /**
     * Unwraps the processing environment from IntelliJ IDEA's JPS API wrapper.
     *
     * <p>When running inside IntelliJ's JPS build system, the processing environment
     * is wrapped by {@code org.jetbrains.jps.javac.APIWrappers}. This method attempts
     * to reflectively unwrap it so that compiler internals can be accessed directly.
     * If unwrapping fails (e.g. when not running in IntelliJ), the original
     * environment is returned unchanged.
     *
     * @param wrapper the potentially wrapped processing environment
     * @return the unwrapped processing environment, or the original if unwrapping fails
     */
    private ProcessingEnvironment unwrapProcessingEnv(ProcessingEnvironment wrapper) {
        try {
            var apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            var unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            return (ProcessingEnvironment) unwrapMethod.invoke(null, ProcessingEnvironment.class, wrapper);
        } catch (ReflectiveOperationException exception) {
            return wrapper;
        }
    }

    /**
     * Returns the set of fully qualified annotation type names supported by this
     * processor.
     *
     * <p>The returned set is the union of the common type system annotations
     * (from {@link DaedalusValidationManager#supportedAnnotationTypes()}) and the annotation types
     * supported by each discovered extension.
     *
     * @return the aggregated set of supported annotation types
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> result = new HashSet<>(validator.supportedAnnotationTypes());
        for (var extension : extensions) {
            result.addAll(extension.supportedAnnotationTypes());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns the latest source version supported by this processor.
     *
     * @return {@link SourceVersion#latestSupported()}
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Processes the annotated elements through the five-phase pipeline.
     *
     * <p>The phases are executed in order:
     * <ol>
     * <li>Common validation checks (serializer, deserializer, mixin, builder, and
     *     default value annotations)
     * <li>Extension-specific validation checks
     * <li>Object processing (each extension populates the shared converter graphs)
     * <li>Attribution (resolve unattributed converter elements into attributed ones
     *     using the converter graphs)
     * <li>Code generation (each extension generates its Spec classes and related code)
     * </ol>
     *
     * @param annotations the annotation types requested to be processed
     * @param roundEnv the environment for information about the current and prior round
     * @return {@code true} to indicate that the annotations are claimed by this processor
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        runWithTimer("common checks", () -> validator.runCommonChecks(roundEnv));
        runWithTimer("extension checks", () -> {
            for (var extension : extensions) {
                extension.runChecks(roundEnv);
            }
        });
        var objectsByExtension = runWithTimer("object processing", () -> {
            Map<DaedalusProcessorExtension, List<? extends DaedalusTypeElement>> result = new LinkedHashMap<>();
            for (var extension : extensions) {
                var objects = extension.processObjects(roundEnv, converterGraph);
                result.put(extension, objects);
            }
            return result;
        });
        runWithTimer("attribution", () -> {
            for (var objects : objectsByExtension.values()) {
                attributeObjects(objects);
            }
        });
        runWithTimer("code generation", () -> {
            for (var entry : objectsByExtension.entrySet()) {
                entry.getKey().generateCode(entry.getValue());
            }
        });
        return true;
    }

    /**
     * Attributes all converter elements for the properties of the given processed objects.
     *
     * <p>This method iterates over every property of every processed object and resolves
     * its unattributed converter elements into attributed ones using the shared converter
     * graphs.
     *
     * @param objects the processed objects whose properties should be attributed
     */
    private void attributeObjects(List<? extends DaedalusTypeElement> objects) {
        for (var object : objects) {
            for (var propertyType : object.properties()) {
                attributeConverter(propertyType.type());
            }
        }
    }

    /**
     * Resolves the converter elements of a single property type by replacing
     * unattributed elements with attributed ones discovered through the converter graphs.
     *
     * <p>All existing converters on the property are collected and resolved. The
     * property's converter list is then cleared and repopulated with only the
     * attributed results.
     *
     * @param type the property type whose converters should be attributed
     */
    private void attributeConverter(DaedalusFieldType type) {
        List<DaedalusConverterElement.Attributed> attributed = new ArrayList<>();
        for (var entry : type.converters()) {
            switch (entry) {
                case DaedalusConverterElement.Attributed attributedElement -> attributed.add(attributedElement);
                case DaedalusConverterElement.Unattributed unattributedElement -> attributed.addAll(attributeConverter(unattributedElement));
            }
        }

        type.clearConverters();

        for (var entry : attributed) {
            type.addConverter(entry);
        }
    }

    /**
     * Resolves a single unattributed converter element into a list of attributed
     * converter elements by finding a conversion path through the unified converter graph.
     *
     * <p>If no path is found, a compilation error is reported against the invoking element.
     * The error message is tailored to the converter direction (serializer or deserializer).
     *
     * <p>Any warnings associated with converter methods along the resolved path
     * are emitted as compiler warnings.
     *
     * @param unattributedElement the unattributed converter element to resolve
     * @return the list of attributed converter elements forming the conversion chain
     */
    private List<DaedalusConverterElement.Attributed> attributeConverter(DaedalusConverterElement.Unattributed unattributedElement) {
        var from = unattributedElement.from();
        List<DaedalusConverterElement.Attributed> results = new ArrayList<>();
        var methodPath = converterGraph.findPath(from, unattributedElement.to(), unattributedElement.mixins());
        if (methodPath.isEmpty()) {
            var errorMessage = switch (unattributedElement.type()) {
                case SERIALIZER -> "Missing converter: cannot find a serializer from %s to %s".formatted(from, unattributedElement.targetDescription());
                case DESERIALIZER -> "Missing converter: cannot find a deserializer from %s to %s".formatted(unattributedElement.targetDescription(), unattributedElement.to());
            };
            messages.printError(errorMessage, unattributedElement.invoker());
        } else {
            for (var element : methodPath) {
                var warning = element.method().warning();
                if (!warning.isEmpty()) {
                    messages.printWarning(warning, unattributedElement.invoker());
                }
                var attributed = switch (unattributedElement.type()) {
                    case SERIALIZER -> new DaedalusConverterElement.Attributed.Serializer(element.method(), from, element.returnType());
                    case DESERIALIZER -> new DaedalusConverterElement.Attributed.Deserializer(element.method(), from, element.returnType());
                };
                results.add(attributed);
                from = element.returnType();
            }
        }
        return results;
    }

    /**
     * Runs the given supplier with timing diagnostics, printing informational
     * notes before and after execution.
     *
     * @param <T> the return type of the supplier
     * @param name the name of the phase being timed
     * @param supplier the supplier to execute
     * @return the result produced by the supplier
     */
    @SuppressWarnings("SameParameterValue")
    private <T> T runWithTimer(String name, Supplier<T> supplier) {
        var start = System.currentTimeMillis();
        messages.printInfo("Running %s...".formatted(name));
        var result = supplier.get();
        messages.printInfo("Finished %s(%dms)".formatted(name, System.currentTimeMillis() - start));
        return result;
    }

    /**
     * Runs the given runnable with timing diagnostics, printing informational
     * notes before and after execution.
     *
     * @param name the name of the phase being timed
     * @param runnable the runnable to execute
     */
    private void runWithTimer(String name, Runnable runnable) {
        var start = System.currentTimeMillis();
        messages.printInfo("Running %s...".formatted(name));
        runnable.run();
        messages.printInfo("Finished %s(%dms)".formatted(name, System.currentTimeMillis() - start));
    }
}
