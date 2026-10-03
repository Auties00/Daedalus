package com.github.auties00.daedalus.typesystem.exception;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A checked exception thrown when a class, a method or a field cannot be accessed
 * reflectively, for example because the module system denies the access and its
 * encapsulation cannot be bypassed.
 *
 * <p>Instances are created exclusively through the static factory methods of this class,
 * each of which documents the key of the {@code messages} resource bundle entry of this
 * package that holds its text.
 */
public final class TypeReflectionException extends Exception {
    /**
     * Constructs a new {@code TypeReflectionException} with the given message and cause.
     *
     * @param message the localized detail message
     * @param cause the cause of the exception, or {@code null} if the cause is unknown
     */
    private TypeReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a module is not part of
     * the boot layer.
     *
     * <p>Bundle key: {@code reflection.missingModule}.
     *
     * @param moduleName the name of the missing module
     * @return a {@code TypeReflectionException} stating that the module is missing
     */
    public static TypeReflectionException missingModule(String moduleName) {
        return new TypeReflectionException(TypeException.localize("reflection.missingModule", moduleName, Runtime.version(), System.getProperty("java.home")), null);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a package of a module
     * cannot be opened to another module.
     *
     * <p>Bundle key: {@code reflection.inaccessiblePackage}.
     *
     * @param moduleName the name of the module that contains the package
     * @param packageName the name of the package
     * @param targetModule the module that the package should be opened to
     * @param cause the error that prevented the package from being opened
     * @return a {@code TypeReflectionException} stating that the package cannot be opened
     */
    public static TypeReflectionException inaccessiblePackage(String moduleName, String packageName, Module targetModule, Throwable cause) {
        var targetModuleName = targetModule.isNamed() ? targetModule.getName() : "ALL-UNNAMED";
        return new TypeReflectionException(TypeException.localize("reflection.inaccessiblePackage", packageName, moduleName, targetModuleName, describe(cause)), cause);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a reflected object
     * cannot be made accessible, although {@link sun.misc.Unsafe} is available.
     *
     * <p>Bundle key: {@code reflection.inaccessibleMember}.
     *
     * @param member the reflected object
     * @param cause the error that prevented the object from being made accessible
     * @return a {@code TypeReflectionException} stating that the object cannot be accessed
     */
    public static TypeReflectionException inaccessibleMember(AccessibleObject member, Throwable cause) {
        return new TypeReflectionException(TypeException.localize("reflection.inaccessibleMember", member, describe(cause)), cause);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a reflected object
     * cannot be made accessible, because the module system denies the access and
     * {@link sun.misc.Unsafe}, which is used to bypass it, is not available.
     *
     * <p>Bundle key: {@code reflection.unavailableUnsafe}.
     *
     * @param member the reflected object
     * @return a {@code TypeReflectionException} stating that the object cannot be accessed
     */
    public static TypeReflectionException unavailableUnsafe(AccessibleObject member) {
        return new TypeReflectionException(TypeException.localize("reflection.unavailableUnsafe", member, Runtime.version()), null);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a class has no public
     * method with the given name that accepts the given arguments.
     *
     * <p>Bundle key: {@code reflection.missingMethod}.
     *
     * @param type the class that was searched
     * @param name the name of the method
     * @param arguments the arguments that the method should accept
     * @return a {@code TypeReflectionException} stating that the method is missing
     */
    public static TypeReflectionException missingMethod(Class<?> type, String name, Object[] arguments) {
        var argumentTypes = Arrays.stream(arguments)
                .map(argument -> argument == null ? "null" : argument.getClass().getName())
                .collect(Collectors.joining(", "));
        return new TypeReflectionException(TypeException.localize("reflection.missingMethod", name, argumentTypes, type.getName(), describeOrigin(type)), null);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that the invocation of a
     * method failed.
     *
     * <p>Bundle key: {@code reflection.failedInvocation}.
     *
     * @param method the invoked method
     * @param targetType the class of the object the method was invoked on
     * @param cause the error thrown by the method, or the error that prevented its invocation
     * @return a {@code TypeReflectionException} stating that the invocation failed
     */
    public static TypeReflectionException failedInvocation(Method method, Class<?> targetType, Throwable cause) {
        return new TypeReflectionException(TypeException.localize("reflection.failedInvocation", method, targetType.getName(), describe(cause)), cause);
    }

    /**
     * Creates a new {@code TypeReflectionException} indicating that a method returned a
     * value of an unexpected type.
     *
     * <p>Bundle key: {@code reflection.unexpectedReturnType}.
     *
     * @param method the invoked method
     * @param expectedType the expected type of the returned value
     * @param actualType the actual type of the returned value
     * @return a {@code TypeReflectionException} stating that the returned value has an
     *         unexpected type
     */
    public static TypeReflectionException unexpectedReturnType(Method method, Class<?> expectedType, Class<?> actualType) {
        return new TypeReflectionException(TypeException.localize("reflection.unexpectedReturnType", method, expectedType.getName(), actualType.getName(), describeOrigin(method.getDeclaringClass())), null);
    }

    /**
     * Returns a description of the given cause that can be embedded in a message.
     *
     * <p>The message of a {@code TypeReflectionException} is already a complete sentence, so
     * it is returned as is. Any other throwable is described by its class and its message,
     * as the message of many exceptions of the JDK is meaningless without its class.
     *
     * @param cause the cause to describe, or {@code null}
     * @return the description of the cause, or {@code "unknown cause"} if it is {@code null}
     */
    private static String describe(Throwable cause) {
        if (cause == null) {
            return "unknown cause";
        }

        if (cause instanceof TypeReflectionException) {
            return cause.getMessage();
        }

        return cause.toString();
    }

    /**
     * Describes where the given class was loaded from, so that the version of the library
     * that declares it can be identified.
     *
     * @param type the class
     * @return the location of the class, or {@code "an unknown location"} if it is not known
     */
    private static String describeOrigin(Class<?> type) {
        var protectionDomain = type.getProtectionDomain();
        var codeSource = protectionDomain == null ? null : protectionDomain.getCodeSource();
        if (codeSource != null && codeSource.getLocation() != null) {
            return codeSource.getLocation().toString();
        }

        return Objects.requireNonNullElse(type.getModule().getName(), "an unknown location");
    }
}
