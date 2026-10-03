package com.github.auties00.daedalus.processor.util;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;

/**
 * Static utility methods for the instance of a mixin on which its instance converters are
 * invoked.
 *
 * <p>A converter of a mixin, that is a method annotated with {@code @TypeSerializer},
 * {@code @TypeDeserializer} or {@code @TypeSize} declared in a type annotated with
 * {@link TypeMixin}, is either static or an instance method. An instance converter is
 * invoked on the instance of the mixin, which is provided by its member annotated with
 * {@link TypeMixin.Instance}, or created once by its member annotated with
 * {@link TypeMixin.Factory} and kept by a generated holder class. In both cases, the value
 * to convert is the first parameter of the converter, as it is for a static one.
 */
public final class DaedalusMixinInstanceUtils {
    /**
     * The suffix of the name of the holder class generated for a mixin whose instance is
     * created by its member annotated with {@link TypeMixin.Factory}.
     */
    private static final String HOLDER_SUFFIX = "InstanceHolder";

    /**
     * The name of the static field of a holder class that keeps the lazily created
     * instance of its mixin.
     */
    public static final String HOLDER_FIELD = "INSTANCE";

    private DaedalusMixinInstanceUtils() {
        throw new UnsupportedOperationException("DaedalusMixinInstanceUtils is a utility class and cannot be instantiated");
    }

    /**
     * Returns whether the given type is annotated with {@link TypeMixin}.
     *
     * @param type the type
     * @return {@code true} if the type is a mixin
     */
    public static boolean isMixin(TypeElement type) {
        return type.getAnnotation(TypeMixin.class) != null;
    }

    /**
     * Returns whether the given converter is an instance method of a mixin, which is
     * invoked on the instance of the mixin with the value to convert as its first
     * parameter.
     *
     * @param method the converter
     * @return {@code true} if the converter is a non-static method declared in a mixin
     */
    public static boolean isInvokedOnMixinInstance(ExecutableElement method) {
        return method.getKind() == ElementKind.METHOD
                && !method.getModifiers().contains(Modifier.STATIC)
                && method.getEnclosingElement() instanceof TypeElement owner
                && isMixin(owner);
    }

    /**
     * Returns whether the given converter receives the value to convert as its first
     * parameter, which is the case for a constructor, a static method, and an instance
     * method of a mixin, but not for an instance method of the type of the value.
     *
     * @param method the converter
     * @return {@code true} if the value to convert is the first parameter of the converter
     */
    public static boolean receivesValueAsFirstParameter(ExecutableElement method) {
        return method.getKind() == ElementKind.CONSTRUCTOR
                || method.getModifiers().contains(Modifier.STATIC)
                || isInvokedOnMixinInstance(method);
    }

    /**
     * Returns the members of the given mixin annotated with {@link TypeMixin.Instance} or
     * {@link TypeMixin.Factory}, in declaration order.
     *
     * @param mixin the mixin
     * @return the members that provide or create the instance of the mixin
     */
    public static List<Element> findInstanceAccessors(TypeElement mixin) {
        return mixin.getEnclosedElements()
                .stream()
                .filter(DaedalusMixinInstanceUtils::isInstanceAccessor)
                .map(element -> (Element) element)
                .toList();
    }

    /**
     * Returns whether the given element is annotated with {@link TypeMixin.Instance} or
     * {@link TypeMixin.Factory}.
     *
     * @param element the element
     * @return {@code true} if the element provides or creates the instance of its mixin
     */
    public static boolean isInstanceAccessor(Element element) {
        return element.getAnnotation(TypeMixin.Instance.class) != null
                || element.getAnnotation(TypeMixin.Factory.class) != null;
    }

    /**
     * Returns the expression that evaluates to the instance of the given mixin on which its
     * instance converters are invoked.
     *
     * <p>The expression reads the field, or invokes the method, annotated with
     * {@link TypeMixin.Instance}, or reads the lazy constant of the holder class generated
     * for the member annotated with {@link TypeMixin.Factory}. If the mixin has more than
     * one such member, the first one is used, as the mixin is reported as illegal.
     *
     * @param mixin the mixin
     * @return the expression, or an empty {@link Optional} if the mixin has no member that
     *         provides or creates its instance
     */
    public static Optional<String> getInstanceExpression(TypeElement mixin) {
        var accessors = findInstanceAccessors(mixin);
        if (accessors.isEmpty()) {
            return Optional.empty();
        }

        var accessor = accessors.getFirst();
        if (accessor.getAnnotation(TypeMixin.Factory.class) != null) {
            return Optional.of(getHolderQualifiedName(mixin) + "." + HOLDER_FIELD + ".get()");
        }

        var owner = mixin.getQualifiedName() + "." + accessor.getSimpleName();
        return Optional.of(accessor.getKind() == ElementKind.FIELD ? owner : owner + "()");
    }

    /**
     * Returns the simple name of the holder class generated for the given mixin, which is
     * the names of its enclosing types and its own name, followed by
     * {@value #HOLDER_SUFFIX}.
     *
     * <p>For example, the holder of {@code com.example.Outer.InnerMixin} is
     * {@code OuterInnerMixinInstanceHolder}.
     *
     * @param mixin the mixin
     * @return the simple name of the holder class
     */
    public static String getHolderSimpleName(TypeElement mixin) {
        var name = new StringBuilder();
        for (Element current = mixin; current instanceof TypeElement type; current = type.getEnclosingElement()) {
            name.insert(0, type.getSimpleName());
        }
        return name.append(HOLDER_SUFFIX).toString();
    }

    /**
     * Returns the fully qualified name of the holder class generated for the given mixin,
     * which is declared in the package of the mixin.
     *
     * @param mixin the mixin
     * @return the fully qualified name of the holder class
     */
    public static String getHolderQualifiedName(TypeElement mixin) {
        var packageName = getPackageName(mixin);
        var simpleName = getHolderSimpleName(mixin);
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }

    /**
     * Returns the name of the package that contains the given type.
     *
     * @param type the type
     * @return the name of the package, or an empty string for the unnamed package
     */
    public static String getPackageName(TypeElement type) {
        Element current = type;
        while (!(current instanceof PackageElement packageElement)) {
            current = current.getEnclosingElement();
        }
        return packageElement.getQualifiedName().toString();
    }
}
