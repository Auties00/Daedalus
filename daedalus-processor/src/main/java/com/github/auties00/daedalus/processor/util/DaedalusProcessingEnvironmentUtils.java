package com.github.auties00.daedalus.processor.util;

import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;
import com.github.auties00.daedalus.typesystem.util.ReflectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Static utility methods for finding the processing environment created by the compiler,
 * which build tools and IDEs often wrap before passing it to an annotation processor.
 *
 * <p>A wrapper delegates to the environment it wraps, so the processor can use it for
 * everything the annotation processing API exposes, and build tools rely on their wrappers
 * to track the files that the processor generates. The environment created by the compiler
 * is only needed to read what the API does not expose, such as the locations of the
 * compilation, from the internals of the compiler.
 *
 * <p>The supported compilers are javac, including when it is run by Maven, Gradle, IntelliJ
 * IDEA or kapt, the batch Eclipse compiler, and the compiler of the Eclipse IDE.
 */
public final class DaedalusProcessingEnvironmentUtils {
    /**
     * The name of the processing environment implementation of javac.
     */
    private static final String JAVAC_PROCESSING_ENVIRONMENT = "com.sun.tools.javac.processing.JavacProcessingEnvironment";

    /**
     * The name of the base class of the processing environment implementations of the
     * Eclipse compiler, both the batch one and the one of the Eclipse IDE.
     */
    private static final String ECLIPSE_PROCESSING_ENVIRONMENT = "org.eclipse.jdt.internal.compiler.apt.dispatch.BaseProcessingEnvImpl";

    private DaedalusProcessingEnvironmentUtils() {
        throw new UnsupportedOperationException("DaedalusProcessingEnvironmentUtils is a utility class and cannot be instantiated");
    }

    /**
     * Finds the processing environment created by the compiler, unwrapping the given one if
     * a build tool or an IDE wrapped it.
     *
     * <p>Wrappers keep the environment they delegate to in one of their fields, for example
     * Gradle's incremental processing and Kotlin's kapt, or in the invocation handler of a
     * proxy, for example IntelliJ's build system. Every field of the wrapper whose value is a
     * processing environment or a proxy is followed, until the one created by the compiler is
     * found. The given environment is returned as is if it was created by the compiler.
     *
     * @param processingEnv the annotation processing environment, possibly wrapped by a build
     *        tool or an IDE
     * @return the processing environment created by javac or by the Eclipse compiler, or an
     *         empty {@link Optional} if the given one neither is nor wraps one of them
     * @throws TypeReflectionException if a field of a wrapper cannot be read
     */
    public static Optional<ProcessingEnvironment> findCompilerProcessingEnvironment(ProcessingEnvironment processingEnv) throws TypeReflectionException {
        return Optional.ofNullable(findCompilerProcessingEnvironment(processingEnv, Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    /**
     * Returns whether the given processing environment was created by javac.
     *
     * @param processingEnv the processing environment
     * @return {@code true} if the class of the environment is, or extends, the processing
     *         environment implementation of javac
     */
    public static boolean isJavacProcessingEnvironment(ProcessingEnvironment processingEnv) {
        return ReflectionUtils.isSubclassOf(processingEnv.getClass(), JAVAC_PROCESSING_ENVIRONMENT);
    }

    /**
     * Returns whether the given processing environment was created by the Eclipse compiler,
     * either the batch one or the one of the Eclipse IDE.
     *
     * @param processingEnv the processing environment
     * @return {@code true} if the class of the environment extends the base class of the
     *         processing environment implementations of the Eclipse compiler
     */
    public static boolean isEclipseProcessingEnvironment(ProcessingEnvironment processingEnv) {
        return ReflectionUtils.isSubclassOf(processingEnv.getClass(), ECLIPSE_PROCESSING_ENVIRONMENT);
    }

    /**
     * Finds the processing environment created by the compiler, starting from the given
     * object, which is a processing environment or the invocation handler of a proxy.
     *
     * @param candidate the object to inspect, or {@code null}
     * @param visited the objects that were already inspected, to avoid cycles
     * @return the processing environment created by the compiler, or {@code null} if it
     *         cannot be found
     * @throws TypeReflectionException if a field of a wrapper cannot be read
     */
    private static ProcessingEnvironment findCompilerProcessingEnvironment(Object candidate, Set<Object> visited) throws TypeReflectionException {
        if (candidate == null || !visited.add(candidate)) {
            return null;
        }

        if (candidate instanceof ProcessingEnvironment processingEnv
                && (isJavacProcessingEnvironment(processingEnv) || isEclipseProcessingEnvironment(processingEnv))) {
            return processingEnv;
        }

        var candidateType = candidate.getClass();
        if (Proxy.isProxyClass(candidateType)) {
            return findCompilerProcessingEnvironment(Proxy.getInvocationHandler(candidate), visited);
        }

        for (Class<?> type = candidateType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }

                var value = ReflectionUtils.readField(candidate, field);
                if (value instanceof ProcessingEnvironment || (value != null && Proxy.isProxyClass(value.getClass()))) {
                    var result = findCompilerProcessingEnvironment(value, visited);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }
}
