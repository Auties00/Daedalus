package com.github.auties00.daedalus.typesystem.util;

import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;
import sun.misc.Unsafe;

import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Static utility methods for accessing classes that are not visible to the caller, or that
 * are encapsulated by the module system, such as the internals of a compiler or of the JDK.
 *
 * <p>The classes of a compiler, of a build tool or of an IDE are usually loaded by a class
 * loader that the annotation processor cannot see, so their methods are looked up by name
 * on the class of an object with {@link #findPublicMethod(Object, String, Object...)} and
 * invoked with {@link #invokePublicMethod(Object, Class, String, Object...)}.
 *
 * <p>Some classes are also encapsulated by the module system, for example the internals of
 * javac in the {@code jdk.compiler} module, or the internal array of a {@link String} in
 * the {@code java.base} module, which {@link CharsetUtils} reads. No supported API can open a
 * package of another module, or make a member of an encapsulated package accessible, so
 * {@link #openPackages(String, String...)} and {@link #makeAccessible(AccessibleObject)}
 * use {@link Unsafe} when nothing else works: the first time this happens, the Java
 * runtime prints a warning about the use of a terminally deprecated method of
 * {@link Unsafe}.
 *
 * @implNote
 * Bypassing the encapsulation only works as long as the memory access methods of
 * {@link Unsafe} are allowed, which is the default up to and including Java 26. Every
 * method of this class throws {@link TypeReflectionException} if it cannot bypass the
 * encapsulation, so that callers can degrade gracefully. It should be replaced by a
 * supported API as soon as the JDK provides one.
 */
public final class ReflectionUtils {
    private ReflectionUtils() {
        throw new UnsupportedOperationException("ReflectionUtils is a utility class and cannot be instantiated");
    }

    /**
     * Opens the given packages of the given module of the boot layer to the module of
     * this class.
     *
     * <p>Packages that are already open, for example because the Java runtime was started
     * with {@code --add-opens}, are left untouched, and {@link Unsafe} is only used if at
     * least one package is not open yet.
     *
     * @param moduleName the name of the module that contains the packages
     * @param packageNames the names of the packages to open
     * @throws TypeReflectionException if the module is not part of the boot layer, or
     *         if a package cannot be opened
     */
    public static void openPackages(String moduleName, String... packageNames) throws TypeReflectionException {
        var targetModule = ModuleLayer.boot()
                .findModule(moduleName)
                .orElseThrow(() -> TypeReflectionException.missingModule(moduleName));
        var module = ReflectionUtils.class.getModule();
        Method addOpensMethod = null;
        for (var packageName : packageNames) {
            if (targetModule.isOpen(packageName, module)) {
                continue;
            }

            try {
                if (addOpensMethod == null) {
                    addOpensMethod = makeAccessible(Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class));
                }
                addOpensMethod.invoke(targetModule, packageName, module);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | TypeReflectionException exception) {
                throw TypeReflectionException.inaccessiblePackage(moduleName, packageName, module, exception);
            }
        }
    }

    /**
     * Makes the given reflected object accessible, bypassing the access checks of the
     * module system if necessary.
     *
     * <p>{@link AccessibleObject#trySetAccessible()} is tried first, and {@link Unsafe}
     * is only used if it fails.
     *
     * @param <T> the type of the reflected object
     * @param object the reflected object
     * @return the given object, which is now accessible
     * @throws TypeReflectionException if the object cannot be made accessible
     */
    public static <T extends AccessibleObject> T makeAccessible(T object) throws TypeReflectionException {
        if (!object.trySetAccessible()) {
            UnsafeAccess.overrideAccessChecks(object);
        }
        return object;
    }

    /**
     * Returns the value of the given field of the given object, bypassing the access
     * checks of the module system if necessary.
     *
     * @param owner the object that owns the field
     * @param field the field
     * @return the value of the field
     * @throws TypeReflectionException if the field cannot be made accessible
     */
    public static Object readField(Object owner, Field field) throws TypeReflectionException {
        try {
            return makeAccessible(field).get(owner);
        } catch (IllegalAccessException exception) {
            throw TypeReflectionException.inaccessibleMember(field, exception);
        }
    }

    /**
     * Returns whether the given class is, or extends, the class with the given name.
     *
     * <p>The class is compared by name, so that it can be checked even if the named
     * class is not visible to the caller.
     *
     * @param type the class
     * @param className the binary name of the superclass
     * @return {@code true} if the class is, or extends, the named class
     */
    public static boolean isSubclassOf(Class<?> type, String className) {
        for (var current = type; current != null; current = current.getSuperclass()) {
            if (current.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a public method of the class of the given object, including the methods it
     * inherits, that accepts the given arguments.
     *
     * <p>A primitive parameter accepts an argument of its wrapper type, and a reference
     * parameter accepts {@code null}. If more than one method is applicable, any of them
     * is returned.
     *
     * @param target the object
     * @param name the name of the method
     * @param arguments the arguments of the method
     * @return the method, or an empty {@link Optional} if the class of the object has no
     *         such method
     */
    public static Optional<Method> findPublicMethod(Object target, String name, Object... arguments) {
        for (var method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && isApplicable(method, arguments)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the given arguments can be passed to the given method.
     *
     * @param method the method
     * @param arguments the arguments
     * @return {@code true} if every argument can be passed to its parameter
     */
    private static boolean isApplicable(Method method, Object[] arguments) {
        var parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != arguments.length) {
            return false;
        }

        for (var index = 0; index < parameterTypes.length; index++) {
            var parameterType = parameterTypes[index];
            var argument = arguments[index];
            if (argument == null ? parameterType.isPrimitive() : !MethodType.methodType(parameterType).wrap().returnType().isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Invokes a public method of the class of the given object, including the methods it
     * inherits, that accepts the given arguments.
     *
     * <p>The method is found as described by {@link #findPublicMethod(Object, String, Object...)},
     * and made accessible as described by {@link #makeAccessible(AccessibleObject)}, as a
     * public method declared by a class that is not public cannot be invoked otherwise.
     *
     * @param <T> the type of the returned value
     * @param target the object
     * @param returnType the type of the returned value, which is the wrapper type for a
     *        method that returns a primitive
     * @param name the name of the method
     * @param arguments the arguments of the method
     * @return the value returned by the method, possibly {@code null}
     * @throws TypeReflectionException if the class of the object has no such method,
     *         if the method cannot be invoked or throws an exception, or if it returns a
     *         value that is not an instance of {@code returnType}
     */
    public static <T> T invokePublicMethod(Object target, Class<T> returnType, String name, Object... arguments) throws TypeReflectionException {
        var method = findPublicMethod(target, name, arguments)
                .orElseThrow(() -> TypeReflectionException.missingMethod(target.getClass(), name, arguments));
        Object result;
        try {
            result = makeAccessible(method).invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw TypeReflectionException.failedInvocation(method, target.getClass(), exception.getCause());
        } catch (IllegalAccessException exception) {
            throw TypeReflectionException.failedInvocation(method, target.getClass(), exception);
        }

        if (result != null && !returnType.isInstance(result)) {
            throw TypeReflectionException.unexpectedReturnType(method, returnType, result.getClass());
        }

        return returnType.cast(result);
    }

    /**
     * The lazily initialized access to {@link Unsafe}.
     *
     * <p>This class is only initialized the first time the access checks must be
     * bypassed, so that no warning is printed by the Java runtime if every package is
     * already open.
     */
    @SuppressWarnings("removal")
    private static final class UnsafeAccess {
        /**
         * The instance of {@link Unsafe}, or {@code null} if it cannot be accessed.
         */
        private static final Unsafe UNSAFE = findUnsafe();

        private UnsafeAccess() {
            throw new UnsupportedOperationException("UnsafeAccess is a utility class and cannot be instantiated");
        }

        /**
         * Sets the field of the given object that overrides the access checks.
         *
         * <p>The field is hidden from reflection, so its offset is computed from
         * {@link AccessibleObjectPlaceholder}, which declares the same fields in the
         * same order.
         *
         * @param object the reflected object
         * @throws TypeReflectionException if {@link Unsafe} is not available, or if
         *         the field cannot be set, for example because the memory access methods
         *         of {@link Unsafe} are denied
         */
        private static void overrideAccessChecks(AccessibleObject object) throws TypeReflectionException {
            if (UNSAFE == null) {
                throw TypeReflectionException.unavailableUnsafe(object);
            }

            try {
                var overrideOffset = UNSAFE.objectFieldOffset(AccessibleObjectPlaceholder.class.getDeclaredField("override"));
                UNSAFE.putBooleanVolatile(object, overrideOffset, true);
            } catch (Throwable throwable) {
                throw TypeReflectionException.inaccessibleMember(object, throwable);
            }
        }

        /**
         * Returns the instance of {@link Unsafe}.
         *
         * @return the instance, or {@code null} if it cannot be accessed
         */
        private static Unsafe findUnsafe() {
            try {
                var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                return (Unsafe) unsafeField.get(null);
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    /**
     * A class that declares the same instance fields of {@link AccessibleObject}, in the
     * same order, so that the offset of its {@code override} field is the offset of the
     * field of {@link AccessibleObject}.
     */
    @SuppressWarnings("unused")
    private static final class AccessibleObjectPlaceholder {
        /**
         * Mirrors the field of {@link AccessibleObject} that overrides the access checks.
         */
        boolean override;

        /**
         * Mirrors the cache of the access checks of {@link AccessibleObject}.
         */
        Object accessCheckCache;
    }
}
