package com.github.auties00.daedalus.processor.exception;

import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;

/**
 * A checked exception thrown when the locations of the current compilation, such as its
 * class path and its module path, cannot be read.
 *
 * <p>Instances are created exclusively through the static factory methods of this class,
 * each of which documents the key of the {@code messages} resource bundle entry that holds
 * its text.
 */
public final class DaedalusProcessorClasspathException extends DaedalusProcessorException {
    /**
     * Constructs a new {@code DaedalusProcessorClasspathException} with the given message key.
     *
     * @param key the resource bundle key of the message
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     * @param args the arguments substituted into the message
     */
    private DaedalusProcessorClasspathException(String key, Throwable cause, Object... args) {
        super(key, cause, args);
    }

    /**
     * Creates a new {@code DaedalusProcessorClasspathException} indicating that the processing
     * environment was not created by a supported compiler, and does not wrap one.
     *
     * <p>Bundle key: {@code classpath.unsupported.compiler}.
     *
     * @param environmentType the class of the processing environment
     * @return a {@code DaedalusProcessorClasspathException} stating that the compiler is not supported
     */
    public static DaedalusProcessorClasspathException unsupportedCompiler(Class<?> environmentType) {
        return new DaedalusProcessorClasspathException("classpath.unsupported.compiler", null, environmentType.getName());
    }

    /**
     * Creates a new {@code DaedalusProcessorClasspathException} indicating that the processing
     * environment of the Eclipse compiler exposes neither a file manager nor a Java project.
     *
     * <p>Bundle key: {@code classpath.unsupported.eclipse.compiler}.
     *
     * @param environmentType the class of the processing environment of the Eclipse compiler
     * @return a {@code DaedalusProcessorClasspathException} stating that this variant of the Eclipse
     *         compiler is not supported
     */
    public static DaedalusProcessorClasspathException unsupportedEclipseCompiler(Class<?> environmentType) {
        return new DaedalusProcessorClasspathException("classpath.unsupported.eclipse.compiler", null, environmentType.getName());
    }

    /**
     * Creates a new {@code DaedalusProcessorClasspathException} indicating that the internals of
     * the compiler cannot be accessed.
     *
     * <p>Bundle key: {@code classpath.inaccessible.compiler}.
     *
     * @param environmentType the class of the processing environment of the compiler
     * @param cause the error that prevented the internals from being accessed
     * @return a {@code DaedalusProcessorClasspathException} stating that the compiler cannot be accessed
     */
    public static DaedalusProcessorClasspathException inaccessibleCompiler(Class<?> environmentType, TypeReflectionException cause) {
        return new DaedalusProcessorClasspathException("classpath.inaccessible.compiler", cause, environmentType.getName(), describe(cause));
    }

    /**
     * Creates a new {@code DaedalusProcessorClasspathException} indicating that the file manager of
     * the compilation does not delegate to any file manager that can list its locations.
     *
     * <p>Bundle key: {@code classpath.unlistable.file.manager}.
     *
     * @param fileManagerType the class of the file manager
     * @return a {@code DaedalusProcessorClasspathException} stating that the file manager cannot
     *         list its locations
     */
    public static DaedalusProcessorClasspathException unlistableFileManager(Class<?> fileManagerType) {
        return new DaedalusProcessorClasspathException("classpath.unlistable.file.manager", null, fileManagerType.getName());
    }
}
