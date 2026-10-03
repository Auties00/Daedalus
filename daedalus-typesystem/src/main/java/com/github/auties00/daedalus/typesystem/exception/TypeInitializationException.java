package com.github.auties00.daedalus.typesystem.exception;

/**
 * An unchecked exception thrown when a class of the Daedalus type system cannot be
 * initialized, because the JVM it runs on is not configured as it requires.
 *
 * <p>It is thrown by the static initializer of the class that cannot be initialized, so the JVM
 * reports it as the cause of an {@link ExceptionInInitializerError} the first time that class
 * is used, and of a {@link NoClassDefFoundError} afterwards. Its message explains what the JVM
 * is missing and how to provide it.
 *
 * <p>Instances are created through the static factory methods of this class, whose messages
 * are localized.
 */
public final class TypeInitializationException extends RuntimeException {
    /**
     * Constructs a new {@code TypeInitializationException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception, or {@code null} if there is none
     */
    private TypeInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns an exception reporting that the internal array of a {@link String} cannot be
     * read, because the {@code java.lang} package of the {@code java.base} module is not open
     * to the Daedalus type system and cannot be opened by it, or because the fields of a
     * string are not the expected ones.
     *
     * <p>The message is the {@code initialization.stringInternalsInaccessible} entry of the
     * bundle of this package, which explains how to open the package. A cause that is a
     * {@link TypeReflectionException} is embedded in it as is, as its message is already a
     * complete explanation.
     *
     * @param cause the exception thrown while opening the package, or while looking up the
     *        fields of a string
     * @return the exception
     */
    public static TypeInitializationException stringInternalsInaccessible(Throwable cause) {
        var reason = cause instanceof TypeReflectionException ? cause.getMessage() : cause + ".";
        return new TypeInitializationException(
                TypeException.localize("initialization.stringInternalsInaccessible", reason),
                cause
        );
    }
}
