package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.processor.diagnostic.DaedalusValidationErrors;
import com.github.auties00.daedalus.processor.util.DaedalusMixinInstanceUtils;
import com.github.auties00.daedalus.typesystem.annotation.*;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

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
        checkMixinInstances(roundEnv);
        checkSerializers(roundEnv);
        checkDeserializers(roundEnv);
        checkSizes(roundEnv);
        checkBuilders(roundEnv);
        checkBuilderMixins(roundEnv);
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
                TypeMixin.Instance.class.getCanonicalName(),
                TypeMixin.Factory.class.getCanonicalName(),
                TypeSerializer.class.getCanonicalName(),
                TypeDeserializer.class.getCanonicalName(),
                TypeSize.class.getCanonicalName(),
                TypeBuilder.class.getCanonicalName(),
                TypeBuilder.Mixin.class.getCanonicalName(),
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
                DaedalusValidationErrors::mixinIllegalKind,
                ElementKind.CLASS,
                ElementKind.INTERFACE
        );
    }

    /**
     * Validates the members that provide or create the instance of a mixin in the given
     * round, which are the elements annotated with {@link TypeMixin.Instance} or
     * {@link TypeMixin.Factory}, and the number of such members of every mixin.
     *
     * <p>A mixin can have at most one such member. A mixin whose converters are all
     * static never uses its instance, so such a member is reported with a warning.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkMixinInstances(RoundEnvironment roundEnv) {
        for (var member : roundEnv.getElementsAnnotatedWith(TypeMixin.Instance.class)) {
            checkMixinInstance(member, TypeMixin.Instance.class);
        }

        for (var member : roundEnv.getElementsAnnotatedWith(TypeMixin.Factory.class)) {
            checkMixinInstance(member, TypeMixin.Factory.class);
        }

        for (var mixin : roundEnv.getElementsAnnotatedWith(TypeMixin.class)) {
            if (!(mixin instanceof TypeElement mixinElement)) {
                continue;
            }

            var accessors = DaedalusMixinInstanceUtils.findInstanceAccessors(mixinElement);
            if (accessors.isEmpty()) {
                continue;
            }

            var first = accessors.getFirst();
            for (var duplicate : accessors.subList(1, accessors.size())) {
                messages.printError(DaedalusValidationErrors.mixinInstanceDuplicate(duplicate, first), duplicate);
            }

            var usesInstance = mixinElement.getEnclosedElements()
                    .stream()
                    .anyMatch(enclosed -> enclosed instanceof ExecutableElement method
                            && DaedalusMixinInstanceUtils.isInvokedOnMixinInstance(method)
                            && (method.getAnnotation(TypeSerializer.class) != null
                            || method.getAnnotation(TypeDeserializer.class) != null
                            || method.getAnnotation(TypeSize.class) != null));
            if (!usesInstance) {
                var annotation = first.getAnnotation(TypeMixin.Factory.class) != null ? TypeMixin.Factory.class : TypeMixin.Instance.class;
                messages.printWarning(DaedalusValidationErrors.mixinInstanceUnused(first, annotation), first);
            }
        }
    }

    /**
     * Validates a single member annotated with {@link TypeMixin.Instance} or
     * {@link TypeMixin.Factory}.
     *
     * <p>The member must be declared in a mixin and must not be private. A field annotated
     * with {@link TypeMixin.Instance} must be static, and its type must be assignable to
     * the mixin. A method must be static, take no parameters, and return a type assignable
     * to the mixin. A constructor annotated with {@link TypeMixin.Factory} must take no
     * parameters and must not belong to an abstract class.
     *
     * @param member the member to validate
     * @param annotation the annotation of the member
     */
    protected void checkMixinInstance(Element member, Class<? extends Annotation> annotation) {
        if (!(member.getEnclosingElement() instanceof TypeElement mixin) || !DaedalusMixinInstanceUtils.isMixin(mixin)) {
            messages.printError(DaedalusValidationErrors.mixinInstanceNotInMixin(member, annotation), member);
            return;
        }

        if (member.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.mixinInstancePrivate(member, annotation), member);
            return;
        }

        if (member.getKind() == ElementKind.CONSTRUCTOR) {
            var constructor = (ExecutableElement) member;
            if (!constructor.getParameters().isEmpty()) {
                messages.printError(DaedalusValidationErrors.mixinInstanceParameters(constructor, annotation), member);
                return;
            }

            if (mixin.getModifiers().contains(Modifier.ABSTRACT)) {
                messages.printError(DaedalusValidationErrors.mixinInstanceAbstractFactory(constructor), member);
            }
            return;
        }

        if (!member.getModifiers().contains(Modifier.STATIC)) {
            messages.printError(DaedalusValidationErrors.mixinInstanceNotStatic(member, annotation), member);
            return;
        }

        TypeMirror type;
        if (member instanceof ExecutableElement method) {
            if (!method.getParameters().isEmpty()) {
                messages.printError(DaedalusValidationErrors.mixinInstanceParameters(method, annotation), member);
                return;
            }

            type = method.getReturnType();
        } else {
            type = member.asType();
        }

        if (!types.isAssignable(type, mixin.asType())) {
            messages.printError(DaedalusValidationErrors.mixinInstanceType(member, annotation, type), member);
        }
    }

    /**
     * Returns whether the given mixin has a member annotated with {@link TypeMixin.Instance}
     * or {@link TypeMixin.Factory}, which provides the instance that its non-static
     * converters are invoked on.
     *
     * @param enclosingType the type that declares a converter
     * @return {@code true} if the type is a declared type that has such a member
     */
    private boolean hasMixinInstance(TypeMirror enclosingType) {
        return enclosingType instanceof DeclaredType declaredType
                && declaredType.asElement() instanceof TypeElement mixin
                && !DaedalusMixinInstanceUtils.findInstanceAccessors(mixin).isEmpty();
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
     * format-managed type. If it is inside a mixin, it must accept at least one
     * parameter, and it must be static unless the mixin has a member annotated with
     * {@link TypeMixin.Instance} or {@link TypeMixin.Factory}; otherwise it must be
     * non-static. It must not have a receiver type or be varargs.
     *
     * @param element the element to validate
     */
    protected void checkSerializer(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError(DaedalusValidationErrors.serializerNotMethod(element), element);
            return;
        }

        if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            messages.printError(DaedalusValidationErrors.serializerConstructor(executableElement), element);
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        if (isFormatManagedType(enclosingType)) {
            messages.printError(DaedalusValidationErrors.serializerInFormatManagedType(executableElement), executableElement);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.serializerPrivate(executableElement), executableElement);
            return;
        }

        var inMixin = types.isMixin(enclosingType);
        var isStatic = executableElement.getModifiers().contains(Modifier.STATIC);
        if (inMixin ? !isStatic && !hasMixinInstance(enclosingType) : isStatic) {
            String message = inMixin ? DaedalusValidationErrors.serializerNotStaticInMixin(executableElement) : DaedalusValidationErrors.serializerStaticOutsideMixin(executableElement);
            messages.printError(message, executableElement);
            return;
        }

        if (executableElement.getParameters().size() < (inMixin ? 1 : 0)) {
            String message = inMixin ? DaedalusValidationErrors.serializerNoParametersInMixin(executableElement) : DaedalusValidationErrors.serializerMandatoryParameters(executableElement);
            messages.printError(message, executableElement);
            return;
        }

        var receiverType = executableElement.getReceiverType();
        if (receiverType.getKind() != TypeKind.NONE) {
            messages.printError(DaedalusValidationErrors.serializerReceiverParameter(executableElement), executableElement);
            return;
        }

        if (executableElement.isVarArgs()) {
            messages.printError(DaedalusValidationErrors.serializerVarargs(executableElement), executableElement);
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
     * <p>A valid deserializer must be an executable element with at least
     * package-private visibility. It must not be declared inside a format-managed
     * type. It must accept at least one parameter. If it is inside a mixin, it cannot
     * be a constructor, and it must be static unless the mixin has a member annotated
     * with {@link TypeMixin.Instance} or {@link TypeMixin.Factory}; otherwise it must be
     * static or a constructor, and must return a type assignable to its enclosing type.
     * It must not have a receiver type or be varargs.
     *
     * @param element the element to validate
     */
    protected void checkDeserializer(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError(DaedalusValidationErrors.deserializerNotMethod(element), element);
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        if (isFormatManagedType(enclosingType)) {
            messages.printError(DaedalusValidationErrors.deserializerInFormatManagedType(executableElement), executableElement);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.deserializerPrivate(executableElement), executableElement);
            return;
        }

        var inMixin = types.isMixin(enclosingType);
        if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            if (inMixin) {
                messages.printError(DaedalusValidationErrors.deserializerConstructorInMixin(executableElement), executableElement);
                return;
            }
        } else if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
            if (!inMixin) {
                messages.printError(DaedalusValidationErrors.deserializerNotStatic(executableElement), executableElement);
                return;
            }

            if (!hasMixinInstance(enclosingType)) {
                messages.printError(DaedalusValidationErrors.deserializerNotStaticInMixin(executableElement), executableElement);
                return;
            }
        }

        if (executableElement.getParameters().isEmpty()) {
            messages.printError(DaedalusValidationErrors.deserializerNoParameters(executableElement), executableElement);
            return;
        }

        if (!inMixin && !types.isAssignable(executableElement.getReturnType(), enclosingType)) {
            messages.printError(DaedalusValidationErrors.deserializerReturnType(executableElement), executableElement);
            return;
        }

        var receiverType = executableElement.getReceiverType();
        if (receiverType.getKind() != TypeKind.NONE) {
            messages.printError(DaedalusValidationErrors.deserializerReceiverParameter(executableElement), executableElement);
            return;
        }

        if (executableElement.isVarArgs()) {
            messages.printError(DaedalusValidationErrors.deserializerVarargs(executableElement), executableElement);
        }
    }

    /**
     * Validates all elements annotated with {@link TypeSize} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkSizes(RoundEnvironment roundEnv) {
        var sizes = roundEnv.getElementsAnnotatedWith(TypeSize.class);
        for (var size : sizes) {
            checkSize(size);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeSize}.
     *
     * <p>A valid size method must be a non-constructor executable element with at
     * least package-private visibility. It must not be declared inside a
     * format-managed type. If it is inside a mixin, it must accept at least one
     * parameter, and it must be static unless the mixin has a member annotated with
     * {@link TypeMixin.Instance} or {@link TypeMixin.Factory}; otherwise it must be
     * non-static. It must return {@code int}, and must not have a receiver type or be
     * varargs.
     *
     * @param element the element to validate
     */
    protected void checkSize(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError(DaedalusValidationErrors.sizeNotMethod(element), element);
            return;
        }

        if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            messages.printError(DaedalusValidationErrors.sizeConstructor(executableElement), element);
            return;
        }

        var enclosingType = executableElement.getEnclosingElement().asType();
        if (isFormatManagedType(enclosingType)) {
            messages.printError(DaedalusValidationErrors.sizeInFormatManagedType(executableElement), executableElement);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.sizePrivate(executableElement), executableElement);
            return;
        }

        var inMixin = types.isMixin(enclosingType);
        var isStatic = executableElement.getModifiers().contains(Modifier.STATIC);
        if (inMixin ? !isStatic && !hasMixinInstance(enclosingType) : isStatic) {
            String message = inMixin
                    ? DaedalusValidationErrors.sizeNotStaticInMixin(executableElement)
                    : DaedalusValidationErrors.sizeStaticOutsideMixin(executableElement);
            messages.printError(message, executableElement);
            return;
        }

        if (inMixin && executableElement.getParameters().isEmpty()) {
            messages.printError(DaedalusValidationErrors.sizeNoParametersInMixin(executableElement), executableElement);
            return;
        }

        if (executableElement.getReturnType().getKind() != TypeKind.INT) {
            messages.printError(DaedalusValidationErrors.sizeReturnType(executableElement), executableElement);
            return;
        }

        var receiverType = executableElement.getReceiverType();
        if (receiverType.getKind() != TypeKind.NONE) {
            messages.printError(DaedalusValidationErrors.sizeReceiverParameter(executableElement), executableElement);
            return;
        }

        if (executableElement.isVarArgs()) {
            messages.printError(DaedalusValidationErrors.sizeVarargs(executableElement), executableElement);
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
            messages.printError(DaedalusValidationErrors.builderNotMethod(element), element);
            return;
        }

        if (element.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.builderPrivate(element), element);
            return;
        }

        if (element.getKind() != ElementKind.CONSTRUCTOR && !element.getModifiers().contains(Modifier.STATIC)) {
            messages.printError(DaedalusValidationErrors.builderNotStatic(element), element);
        }
    }

    /**
     * Validates all elements annotated with {@link TypeBuilder.Mixin} in the given round.
     *
     * @param roundEnv the current annotation processing round environment
     */
    protected void checkBuilderMixins(RoundEnvironment roundEnv) {
        var mixins = roundEnv.getElementsAnnotatedWith(TypeBuilder.Mixin.class);
        for (var mixin : mixins) {
            checkBuilderMixin(mixin);
        }
    }

    /**
     * Validates a single element annotated with {@link TypeBuilder.Mixin}.
     *
     * <p>A valid builder mixin must be a static method with at least package-private
     * visibility. It must have at least one parameter, and its return type must be
     * assignable to the type of its first parameter.
     *
     * @param element the element to validate
     */
    protected void checkBuilderMixin(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            messages.printError(DaedalusValidationErrors.builderMixinNotMethod(element), element);
            return;
        }

        if (executableElement.getModifiers().contains(Modifier.PRIVATE)) {
            messages.printError(DaedalusValidationErrors.builderMixinPrivate(executableElement), executableElement);
            return;
        }

        if (!executableElement.getModifiers().contains(Modifier.STATIC)) {
            messages.printError(DaedalusValidationErrors.builderMixinNotStatic(executableElement), executableElement);
            return;
        }

        if (executableElement.getParameters().isEmpty()) {
            messages.printError(DaedalusValidationErrors.builderMixinNoParameters(executableElement), executableElement);
            return;
        }

        var firstParamType = executableElement.getParameters().getFirst().asType();
        if (!types.isAssignable(executableElement.getReturnType(), firstParamType)) {
            messages.printError(DaedalusValidationErrors.builderMixinReturnType(executableElement), executableElement);
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
                    messages.printError(DaedalusValidationErrors.defaultValueMethodPrivate(element), element);
                    return;
                }

                if (!element.getModifiers().contains(Modifier.STATIC)) {
                    messages.printError(DaedalusValidationErrors.defaultValueMethodNotStatic(element), element);
                    return;
                }

                if (!((ExecutableElement) element).getParameters().isEmpty()) {
                    messages.printError(DaedalusValidationErrors.defaultValueMethodParameters((ExecutableElement) element), element);
                    return;
                }

                var enclosingElement = types.getEnclosingTypeElement(element);
                if (!types.isMixin(enclosingElement.asType()) && !types.isAssignable(((ExecutableElement) element).getReturnType(), enclosingElement.asType())) {
                    messages.printError(DaedalusValidationErrors.defaultValueMethodReturnType((ExecutableElement) element, enclosingElement), element);
                }
            }

            case ENUM_CONSTANT -> {
                // Enum constants are always valid default values
            }

            case FIELD -> {
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    messages.printError(DaedalusValidationErrors.defaultValueFieldPrivate(element), element);
                    return;
                }

                if (!element.getModifiers().contains(Modifier.STATIC)) {
                    messages.printError(DaedalusValidationErrors.defaultValueFieldNotStatic(element), element);
                    return;
                }

                var enclosingElement = types.getEnclosingTypeElement(element);
                if (!types.isMixin(enclosingElement.asType()) && !types.isAssignable(element.asType(), enclosingElement.asType())) {
                    messages.printError(DaedalusValidationErrors.defaultValueFieldType(element, enclosingElement), element);
                }
            }

            default -> messages.printError(DaedalusValidationErrors.defaultValueIllegalKind(element), element);
        }
    }

    /**
     * Validates that a list of mixin type elements are all valid mixin types.
     *
     * @param property the property element that references the mixins
     * @param mixins the list of type elements to validate as mixins
     */
    protected void checkMixins(Element property, Set<TypeElement> mixins) {
        for (var mixin : mixins) {
            if (!types.isMixin(mixin.asType())) {
                messages.printError(DaedalusValidationErrors.notMixin(property, mixin), property);
            }
        }
    }

    /**
     * Validates that all elements annotated with the given annotation have one of the
     * expected element kinds, and have at least package-private visibility.
     *
     * <p>This method behaves as
     * {@link #checkAnnotation(RoundEnvironment, Class, Function, ElementKind...)} with a
     * function that always returns {@code error}.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotationClass the annotation class to check
     * @param error the localized error message to report for invalid element kinds
     * @param elementKind the allowed element kinds
     */
    protected void checkAnnotation(RoundEnvironment roundEnv, Class<? extends Annotation> annotationClass, String error, ElementKind... elementKind) {
        checkAnnotation(roundEnv, annotationClass, element -> error, elementKind);
    }

    /**
     * Validates that all elements annotated with the given annotation have one of the
     * expected element kinds, and have at least package-private visibility.
     *
     * <p>A private element is reported using
     * {@link DaedalusValidationErrors#annotatedElementPrivate(Class, Element)}, and stops
     * the validation of the remaining elements. An element whose kind is not allowed is
     * reported using the message that {@code error} returns for it.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotationClass the annotation class to check
     * @param error the function that returns the localized error message to report
     *        for an element whose kind is not allowed
     * @param elementKind the allowed element kinds
     */
    protected void checkAnnotation(RoundEnvironment roundEnv, Class<? extends Annotation> annotationClass, Function<? super Element, String> error, ElementKind... elementKind) {
        var kinds = Set.of(elementKind);
        for (var element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                messages.printError(DaedalusValidationErrors.annotatedElementPrivate(annotationClass, element), element);
                return;
            } else if (!kinds.contains(element.getKind())) {
                messages.printError(error.apply(element), element);
            }
        }
    }

    /**
     * Validates that all elements annotated with the given annotation are enclosed by
     * a type annotated with one of the required annotations.
     *
     * <p>This method behaves as
     * {@link #checkEnclosing(RoundEnvironment, Class, Function, Class...)} with a function
     * that always returns {@code error}.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotation the annotation class to check
     * @param error the localized error message to report for invalid enclosing types
     * @param requiredAnnotations the required annotations on the enclosing type
     */
    @SafeVarargs
    protected final void checkEnclosing(RoundEnvironment roundEnv, Class<? extends Annotation> annotation, String error, Class<? extends Annotation>... requiredAnnotations) {
        checkEnclosing(roundEnv, annotation, element -> error, requiredAnnotations);
    }

    /**
     * Validates that all elements annotated with the given annotation are enclosed by
     * a type annotated with one of the required annotations.
     *
     * @param roundEnv the current annotation processing round environment
     * @param annotation the annotation class to check
     * @param error the function that returns the localized error message to report
     *        for an element whose enclosing type is not annotated with any of the
     *        required annotations
     * @param requiredAnnotations the required annotations on the enclosing type
     */
    @SafeVarargs
    protected final void checkEnclosing(RoundEnvironment roundEnv, Class<? extends Annotation> annotation, Function<? super Element, String> error, Class<? extends Annotation>... requiredAnnotations) {
        roundEnv.getElementsAnnotatedWith(annotation)
                .stream()
                .filter(property -> {
                    var enclosingTypeElement = types.getEnclosingTypeElement(property);
                    return Arrays.stream(requiredAnnotations)
                            .noneMatch(type -> enclosingTypeElement.getAnnotation(type) != null);
                })
                .forEach(property -> messages.printError(error.apply(property), property));
    }
}
