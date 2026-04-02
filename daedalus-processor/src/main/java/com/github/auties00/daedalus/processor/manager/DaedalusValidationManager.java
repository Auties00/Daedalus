package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeDeserializer;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;
import com.github.auties00.daedalus.typesystem.annotation.TypeSerializer;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * An abstract base for validation checks performed during annotation processing.
 *
 * <p>This class provides common validation logic for the type system annotations
 * ({@link TypeSerializer}, {@link TypeDeserializer}, {@link TypeMixin},
 * {@link TypeDefaultValue}, and {@link TypeBuilder}). Format-specific subclasses
 * extend this class and implement the abstract predicates to integrate with their
 * own managed type annotations.
 *
 * <p>Subclasses must implement {@link #isFormatManagedType(TypeMirror)} to provide format-specific type classification.
 */
public abstract class DaedalusValidationManager {

    /**
     * The type utility used for type comparisons and queries.
     */
    protected final DaedalusTypeManager types;

    /**
     * The diagnostics utility used to report errors and warnings.
     */
    protected final DaedalusLogManager messages;

    /**
     * Constructs a new checks instance with the given type and message utilities.
     *
     * @param types      the type utility
     * @param messages   the diagnostics utility
     */
    public DaedalusValidationManager(DaedalusTypeManager types, DaedalusLogManager messages) {
        this.types = types;
        this.messages = messages;
    }

    /**
     * Returns whether the given type is a format-managed type.
     *
     * <p>Format-managed types are types that are directly handled by the serialization
     * format (for example, protobuf messages or protobuf enums). Serializers and
     * deserializers annotated with type system annotations cannot be declared inside
     * such types.
     *
     * @param type the type mirror to check
     * @return {@code true} if the type is managed by the format
     */
    protected abstract boolean isFormatManagedType(TypeMirror type);

    /**
     * Runs all common validation checks against the given round environment.
     *
     * <p>This method invokes checks for mixins, serializers, deserializers, builders,
     * and default values. Subclasses should call this method from their own
     * {@code runChecks} method, in addition to any format-specific checks.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void runCommonChecks(RoundEnvironment roundEnv) {
        checkMixins(roundEnv);
        checkSerializers(roundEnv);
        checkDeserializers(roundEnv);
        checkBuilders(roundEnv);
        checkDefaultValues(roundEnv);
    }

    /**
     * Returns the set of fully qualified annotation type names for the common
     * type system annotations that this checks class handles.
     *
     * <p>Subclasses should include these in the set returned by their processor's
     * {@code getSupportedAnnotationTypes} method.
     *
     * @return a set of fully qualified annotation class names
     */
    public Set<String> supportedAnnotationTypes() {
        return Set.of(
                TypeMixin.class.getCanonicalName(),
                TypeSerializer.class.getCanonicalName(),
                TypeDeserializer.class.getCanonicalName(),
                TypeBuilder.class.getCanonicalName(),
                TypeDefaultValue.class.getCanonicalName()
        );
    }

    /**
     * Validates all elements annotated with {@link TypeMixin} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkMixins(RoundEnvironment roundEnv) {
        checkAnnotation(
                roundEnv,
                TypeMixin.class,
                "Illegal annotation: only classes and interfaces can be annotated with @TypeMixin",
                ElementKind.CLASS,
                ElementKind.INTERFACE
        );
    }

    /**
     * Validates all elements annotated with {@link TypeSerializer} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkSerializers(RoundEnvironment roundEnv) {
        var serializers = roundEnv.getElementsAnnotatedWith(TypeSerializer.class);
        for (var serializer : serializers) {
            checkSerializer(serializer);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeSerializer}.
     *
     * <p>A valid serializer must be a non-constructor executable element with at
     * least package-private visibility. It must not be declared inside a
     * format-managed type. If it is inside a mixin, it must be static and accept
     * exactly one parameter; otherwise it must be non-static and accept no
     * parameters. It must not have a receiver type or be varargs.
     *
     * @param element the element to validate
     */
    protected void checkSerializer(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError("Invalid delegate: only methods can be annotated with @TypeSerializer", element);
            return;
        }

        if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            messages.printError("Invalid delegate: constructors cannot be annotated with @TypeSerializer", element);
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        if (isFormatManagedType(enclosingType)) {
            messages.printError("Illegal method: a method annotated with @TypeSerializer cannot be inside a format-managed type", executableElement);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError("Weak visibility: a method annotated with @TypeSerializer must have at least package-private visibility", executableElement);
            return;
        }

        var inMixin = types.isMixin(enclosingType);
        if (executableElement.getModifiers().contains(Modifier.STATIC) != inMixin) {
            var message = inMixin ? "Illegal method: a method annotated with @TypeSerializer in a mixin must be static" : "Illegal method: a method annotated with @TypeSerializer must not be static";
            messages.printError(message, executableElement);
            return;
        }

        if (executableElement.getParameters().size() != (inMixin ? 1 : 0)) {
            var message = inMixin ? "Illegal method: a method annotated with @TypeSerializer in a mixin must take exactly one parameter" : "Illegal method: a method annotated with @TypeSerializer must take no parameters";
            messages.printError(message, executableElement);
            return;
        }

        var receiverType = executableElement.getReceiverType();
        if (receiverType.getKind() != TypeKind.NONE) {
            messages.printError("Illegal method: a method annotated with @TypeSerializer cannot have a receiver type", executableElement);
            return;
        }

        if (executableElement.isVarArgs()) {
            messages.printError("Illegal method: a method annotated with @TypeSerializer cannot be varargs", executableElement);
        }
    }

    /**
     * Validates all elements annotated with {@link TypeDeserializer} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkDeserializers(RoundEnvironment roundEnv) {
        var deserializers = roundEnv.getElementsAnnotatedWith(TypeDeserializer.class);
        for (var deserializer : deserializers) {
            checkDeserializer(deserializer);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeDeserializer}.
     *
     * <p>A valid deserializer must be a static executable element with at least
     * package-private visibility. It must not be declared inside a format-managed
     * type. It must accept exactly one parameter. If it is inside a mixin, it
     * cannot be a constructor and must return a type assignable to its enclosing
     * type. It must not have a receiver type or be varargs.
     *
     * @param element the element to validate
     */
    protected void checkDeserializer(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError("Invalid delegate: only methods can be annotated with @TypeDeserializer", element);
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        if (isFormatManagedType(enclosingType)) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer cannot be inside a format-managed type", executableElement);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError("Weak visibility: a method annotated with @TypeDeserializer must have at least package-private visibility", executableElement);
            return;
        }

        if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer must be static", executableElement);
            return;
        }

        var inMixin = types.isMixin(enclosingType);
        if (executableElement.getKind() == ElementKind.CONSTRUCTOR && inMixin) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer in a mixin cannot be a constructor", executableElement);
            return;
        }

        if (executableElement.getParameters().size() != 1) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer must take exactly one parameter", executableElement);
            return;
        }

        if (inMixin && !types.isAssignable(executableElement.getReturnType(), enclosingType)) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer must return a type assignable to its parent or be in a mixin", executableElement);
            return;
        }

        var receiverType = executableElement.getReceiverType();
        if (receiverType.getKind() != TypeKind.NONE) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer cannot have a receiver type", executableElement);
            return;
        }

        if (executableElement.isVarArgs()) {
            messages.printError("Illegal method: a method annotated with @TypeDeserializer cannot be varargs", executableElement);
        }
    }

    /**
     * Validates all elements annotated with {@link TypeBuilder} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkBuilders(RoundEnvironment roundEnv) {
        var builders = roundEnv.getElementsAnnotatedWith(TypeBuilder.class);
        for (var builder : builders) {
            checkBuilder(builder);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeBuilder}.
     *
     * <p>A valid builder must be an executable element (method or constructor) with
     * at least package-private visibility. If it is a method (not a constructor), it
     * must be static.
     *
     * @param element the element to validate
     */
    protected void checkBuilder(Element element) {
        if (!(element instanceof ExecutableElement)) {
            messages.printError("Invalid delegate: only methods can be annotated with @TypeBuilder", element);
            return;
        }

        if (element.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError("Weak visibility: a method annotated with @TypeBuilder must have at least package-private visibility", element);
            return;
        }

        if (element.getKind() != ElementKind.CONSTRUCTOR && !element.getModifiers().contains(Modifier.STATIC)) {
            messages.printError("Illegal method: a method annotated with @TypeBuilder must be a constructor or static", element);
        }
    }

    /**
     * Validates all elements annotated with {@link TypeDefaultValue} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkDefaultValues(RoundEnvironment roundEnv) {
        var defaultValues = roundEnv.getElementsAnnotatedWith(TypeDefaultValue.class);
        for (var defaultValue : defaultValues) {
            checkDefaultValue(defaultValue);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeDefaultValue}.
     *
     * <p>A valid default value must be either a static method or an enum constant.
     * If it is a method, it must have at least package-private visibility and must
     * return a type assignable to its enclosing type (unless the enclosing type is
     * a mixin).
     *
     * @param element the element to validate
     */
    protected void checkDefaultValue(Element element) {
        switch (element.getKind()) {
            case METHOD -> {
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    messages.printError("Weak visibility: a method annotated with @TypeDefaultValue must have at least package-private visibility", element);
                    return;
                }

                if (!element.getModifiers().contains(Modifier.STATIC)) {
                    messages.printError("Illegal method: a method annotated with @TypeDefaultValue must be static", element);
                    return;
                }

                var enclosingElement = getEnclosingTypeElement(element);
                if (!types.isMixin(enclosingElement.asType()) && !types.isAssignable(((ExecutableElement) element).getReturnType(), enclosingElement.asType())) {
                    messages.printError("Illegal method: a method annotated with @TypeDefaultValue must return a type assignable to its parent or be in a mixin", element);
                }
            }

            case ENUM_CONSTANT -> {
                // All uses are fine
            }

            default -> messages.printError("Invalid delegate: only methods and enum constants can be annotated with @TypeDefaultValue", element);
        }
    }

    /**
     * Validates that a list of mixin type elements are all valid mixin types.
     *
     * @param property the property element that references the mixins
     * @param mixins the list of type elements to validate as mixins
     */
    protected void checkMixins(Element property, List<TypeElement> mixins) {
        for (var mixin : mixins) {
            if (!types.isMixin(mixin.asType())) {
                messages.printError("Illegal argument: %s is not a valid mixin".formatted(mixin.getSimpleName()), property);
            }
        }
    }

    /**
     * Validates that all elements annotated with the given annotation have one of the
     * expected element kinds, and have at least package-private visibility.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotationClass the annotation class to check
     * @param error the error message to report for invalid element kinds
     * @param elementKind the allowed element kinds
     */
    protected void checkAnnotation(RoundEnvironment roundEnv, Class<? extends Annotation> annotationClass, String error, ElementKind... elementKind) {
        var kinds = Set.of(elementKind);
        for (var element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                messages.printError("Weak visibility: a method annotated with @" + annotationClass.getSimpleName() + " must have at least package-private visibility", element);
                return;
            } else if (!kinds.contains(element.getKind())) {
                messages.printError(error, element);
            }
        }
    }

    /**
     * Validates that all elements annotated with the given annotation are enclosed by
     * a type annotated with one of the required annotations.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotation the annotation class to check
     * @param error the error message to report for invalid enclosing types
     * @param requiredAnnotations the required annotations on the enclosing type
     */
    @SafeVarargs
    protected final void checkEnclosing(RoundEnvironment roundEnv, Class<? extends Annotation> annotation, String error, Class<? extends Annotation>... requiredAnnotations) {
        roundEnv.getElementsAnnotatedWith(annotation)
                .stream()
                .filter(property -> {
                    var enclosingTypeElement = getEnclosingTypeElement(property);
                    return Arrays.stream(requiredAnnotations)
                            .noneMatch(type -> enclosingTypeElement.getAnnotation(type) != null);
                })
                .forEach(property -> messages.printError(error, property));
    }

    /**
     * Returns the nearest enclosing type element for the given element.
     *
     * <p>If the element is itself a type element, it is returned directly. Otherwise,
     * this method walks up the enclosing element chain until a type element is found.
     *
     * @param element the element whose enclosing type to find
     * @return the nearest enclosing type element
     */
    protected TypeElement getEnclosingTypeElement(Element element) {
        Objects.requireNonNull(element);
        if (element instanceof TypeElement typeElement) {
            return typeElement;
        }

        return getEnclosingTypeElement(element.getEnclosingElement());
    }
}
