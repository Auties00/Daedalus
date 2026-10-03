package com.github.auties00.daedalus.processor.diagnostic;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Static factory methods for the diagnostics reported while validating the elements
 * annotated with the annotations of the type system, such as {@code @TypeSerializer},
 * {@code @TypeDeserializer}, {@code @TypeSize}, {@code @TypeBuilder},
 * {@code @TypeBuilder.Mixin}, {@code @TypeDefaultValue} and {@code @TypeMixin}.
 *
 * <p>Every method returns the text of a diagnostic localized for the
 * {@linkplain Locale#getDefault() default locale}. The text is looked up in the
 * {@code diagnostics} resource bundle of this package using {@link MessageFormat}, and
 * the key of each entry is documented by the method that uses it. Every diagnostic
 * names the offending element, explains why it is rejected and how to fix it.
 *
 * <p>Unless otherwise noted, passing a {@code null} argument to a method of this class
 * will cause a {@link NullPointerException} to be thrown.
 *
 * <p>This class is thread-safe.
 */
public final class DaedalusValidationErrors {
    /**
     * The base name of the resource bundle containing the text of every diagnostic.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.processor.diagnostic.diagnostics";

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private DaedalusValidationErrors() {
        throw new UnsupportedOperationException("DaedalusValidationErrors is a utility class and cannot be instantiated");
    }

    /**
     * Returns the diagnostic reported when an element annotated with an annotation of the
     * type system is private, so the generated code cannot access it.
     *
     * <p>The text is resolved from the {@code validation.annotation.private} key.
     *
     * @param annotation the annotation that the element is annotated with
     * @param element the private element
     * @return the localized diagnostic
     */
    public static String annotatedElementPrivate(Class<? extends Annotation> annotation, Element element) {
        return localize("validation.annotation.private", annotationName(annotation), describe(element));
    }

    /**
     * Returns the diagnostic reported when an element annotated with {@code @TypeMixin}
     * is neither a class nor an interface.
     *
     * <p>The text is resolved from the {@code validation.mixin.illegalKind} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String mixinIllegalKind(Element element) {
        return localize("validation.mixin.illegalKind", describe(element));
    }

    /**
     * Returns the diagnostic reported when a type that is not annotated with
     * {@code @TypeMixin} is listed among the mixins of an element.
     *
     * <p>The text is resolved from the {@code validation.mixin.notMixin} key.
     *
     * @param element the element that lists the type among its mixins
     * @param mixin the type that is not a mixin
     * @return the localized diagnostic
     */
    public static String notMixin(Element element, TypeElement mixin) {
        return localize("validation.mixin.notMixin", mixin.getQualifiedName(), describe(element));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @TypeSerializer} is not a method.
     *
     * <p>The text is resolved from the {@code validation.serializer.notMethod} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String serializerNotMethod(Element element) {
        return localize("validation.serializer.notMethod", describe(element));
    }

    /**
     * Returns the diagnostic reported when a constructor is annotated with
     * {@code @TypeSerializer}.
     *
     * <p>The text is resolved from the {@code validation.serializer.constructor} key.
     *
     * @param constructor the illegal constructor
     * @return the localized diagnostic
     */
    public static String serializerConstructor(ExecutableElement constructor) {
        return localize("validation.serializer.constructor", describe(constructor), describeEnclosing(constructor));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is declared in a type managed by a serialization format.
     *
     * <p>The text is resolved from the {@code validation.serializer.formatManaged} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerInFormatManagedType(ExecutableElement method) {
        return localize("validation.serializer.formatManaged", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is private.
     *
     * <p>The text is resolved from the {@code validation.serializer.private} key.
     *
     * @param method the private method
     * @return the localized diagnostic
     */
    public static String serializerPrivate(ExecutableElement method) {
        return localize("validation.serializer.private", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is declared in a mixin and is not static, but the mixin has
     * no member annotated with {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory}
     * that provides the instance to invoke it on.
     *
     * <p>The text is resolved from the {@code validation.serializer.mixinNotStatic} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerNotStaticInMixin(ExecutableElement method) {
        return localize("validation.serializer.mixinNotStatic", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is static but is not declared in a mixin.
     *
     * <p>The text is resolved from the {@code validation.serializer.staticOutsideMixin}
     * key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerStaticOutsideMixin(ExecutableElement method) {
        return localize("validation.serializer.staticOutsideMixin", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is declared in a mixin but takes no parameters.
     *
     * <p>The text is resolved from the {@code validation.serializer.mixinNoParameters}
     * key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerNoParametersInMixin(ExecutableElement method) {
        return localize("validation.serializer.mixinNoParameters", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is not declared in a mixin but takes mandatory parameters.
     *
     * <p>The text is resolved from the
     * {@code validation.serializer.mandatoryParameters} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerMandatoryParameters(ExecutableElement method) {
        return localize("validation.serializer.mandatoryParameters", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} declares a receiver parameter.
     *
     * <p>The text is resolved from the {@code validation.serializer.receiver} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerReceiverParameter(ExecutableElement method) {
        return localize("validation.serializer.receiver", describe(method), method.getReceiverType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeSerializer} is a variable arity method.
     *
     * <p>The text is resolved from the {@code validation.serializer.varargs} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String serializerVarargs(ExecutableElement method) {
        return localize("validation.serializer.varargs", describe(method));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @TypeDeserializer} is neither a method nor a constructor.
     *
     * <p>The text is resolved from the {@code validation.deserializer.notMethod} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String deserializerNotMethod(Element element) {
        return localize("validation.deserializer.notMethod", describe(element));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is declared in a type managed by a serialization format.
     *
     * <p>The text is resolved from the {@code validation.deserializer.formatManaged} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerInFormatManagedType(ExecutableElement method) {
        return localize("validation.deserializer.formatManaged", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is private.
     *
     * <p>The text is resolved from the {@code validation.deserializer.private} key.
     *
     * @param method the private method
     * @return the localized diagnostic
     */
    public static String deserializerPrivate(ExecutableElement method) {
        return localize("validation.deserializer.private", describe(method));
    }

    /**
     * Returns the diagnostic reported when a constructor of a mixin is annotated with
     * {@code @TypeDeserializer}.
     *
     * <p>The text is resolved from the {@code validation.deserializer.mixinConstructor}
     * key.
     *
     * @param constructor the illegal constructor
     * @return the localized diagnostic
     */
    public static String deserializerConstructorInMixin(ExecutableElement constructor) {
        return localize("validation.deserializer.mixinConstructor", describe(constructor), describeEnclosing(constructor));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is not static.
     *
     * <p>The text is resolved from the {@code validation.deserializer.notStatic} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerNotStatic(ExecutableElement method) {
        return localize("validation.deserializer.notStatic", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is declared in a mixin and is not static, but the mixin has
     * no member annotated with {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory}
     * that provides the instance to invoke it on.
     *
     * <p>The text is resolved from the {@code validation.deserializer.mixinNotStatic} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerNotStaticInMixin(ExecutableElement method) {
        return localize("validation.deserializer.mixinNotStatic", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} takes no parameters.
     *
     * <p>The text is resolved from the {@code validation.deserializer.noParameters} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerNoParameters(ExecutableElement method) {
        return localize("validation.deserializer.noParameters", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is not declared in a mixin and returns a type that is
     * not assignable to its enclosing type.
     *
     * <p>The text is resolved from the {@code validation.deserializer.returnType} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerReturnType(ExecutableElement method) {
        return localize("validation.deserializer.returnType", describe(method), describeEnclosing(method), method.getReturnType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} declares a receiver parameter.
     *
     * <p>The text is resolved from the {@code validation.deserializer.receiver} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerReceiverParameter(ExecutableElement method) {
        return localize("validation.deserializer.receiver", describe(method), method.getReceiverType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDeserializer} is a variable arity method.
     *
     * <p>The text is resolved from the {@code validation.deserializer.varargs} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String deserializerVarargs(ExecutableElement method) {
        return localize("validation.deserializer.varargs", describe(method));
    }

    /**
     * Returns the diagnostic reported when an element annotated with {@code @TypeSize}
     * is not a method.
     *
     * <p>The text is resolved from the {@code validation.size.notMethod} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String sizeNotMethod(Element element) {
        return localize("validation.size.notMethod", describe(element));
    }

    /**
     * Returns the diagnostic reported when a constructor is annotated with
     * {@code @TypeSize}.
     *
     * <p>The text is resolved from the {@code validation.size.constructor} key.
     *
     * @param constructor the illegal constructor
     * @return the localized diagnostic
     */
    public static String sizeConstructor(ExecutableElement constructor) {
        return localize("validation.size.constructor", describe(constructor), describeEnclosing(constructor));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is
     * declared in a type managed by a serialization format.
     *
     * <p>The text is resolved from the {@code validation.size.formatManaged} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeInFormatManagedType(ExecutableElement method) {
        return localize("validation.size.formatManaged", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is
     * private.
     *
     * <p>The text is resolved from the {@code validation.size.private} key.
     *
     * @param method the private method
     * @return the localized diagnostic
     */
    public static String sizePrivate(ExecutableElement method) {
        return localize("validation.size.private", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is
     * declared in a mixin and is not static, but the mixin has no member annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory} that provides the instance
     * to invoke it on.
     *
     * <p>The text is resolved from the {@code validation.size.mixinNotStatic} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeNotStaticInMixin(ExecutableElement method) {
        return localize("validation.size.mixinNotStatic", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is
     * static but is not declared in a mixin.
     *
     * <p>The text is resolved from the {@code validation.size.staticOutsideMixin} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeStaticOutsideMixin(ExecutableElement method) {
        return localize("validation.size.staticOutsideMixin", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is
     * declared in a mixin but takes no parameters.
     *
     * <p>The text is resolved from the {@code validation.size.mixinNoParameters} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeNoParametersInMixin(ExecutableElement method) {
        return localize("validation.size.mixinNoParameters", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} does
     * not return {@code int}.
     *
     * <p>The text is resolved from the {@code validation.size.returnType} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeReturnType(ExecutableElement method) {
        return localize("validation.size.returnType", describe(method), method.getReturnType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize}
     * declares a receiver parameter.
     *
     * <p>The text is resolved from the {@code validation.size.receiver} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeReceiverParameter(ExecutableElement method) {
        return localize("validation.size.receiver", describe(method), method.getReceiverType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeSize} is a
     * variable arity method.
     *
     * <p>The text is resolved from the {@code validation.size.varargs} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String sizeVarargs(ExecutableElement method) {
        return localize("validation.size.varargs", describe(method));
    }

    /**
     * Returns the diagnostic reported when an element annotated with {@code @TypeBuilder}
     * is neither a method nor a constructor.
     *
     * <p>The text is resolved from the {@code validation.builder.notMethod} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String builderNotMethod(Element element) {
        return localize("validation.builder.notMethod", describe(element));
    }

    /**
     * Returns the diagnostic reported when a method or constructor annotated with
     * {@code @TypeBuilder} is private.
     *
     * <p>The text is resolved from the {@code validation.builder.private} key.
     *
     * @param element the private method or constructor
     * @return the localized diagnostic
     */
    public static String builderPrivate(Element element) {
        return localize("validation.builder.private", describe(element));
    }

    /**
     * Returns the diagnostic reported when a method annotated with {@code @TypeBuilder}
     * is neither a constructor nor static.
     *
     * <p>The text is resolved from the {@code validation.builder.notStatic} key.
     *
     * @param element the illegal method
     * @return the localized diagnostic
     */
    public static String builderNotStatic(Element element) {
        return localize("validation.builder.notStatic", describe(element), describeEnclosing(element));
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @TypeBuilder.Mixin} is not a method.
     *
     * <p>The text is resolved from the {@code validation.builderMixin.notMethod} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String builderMixinNotMethod(Element element) {
        return localize("validation.builderMixin.notMethod", describe(element));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeBuilder.Mixin} is private.
     *
     * <p>The text is resolved from the {@code validation.builderMixin.private} key.
     *
     * @param method the private method
     * @return the localized diagnostic
     */
    public static String builderMixinPrivate(ExecutableElement method) {
        return localize("validation.builderMixin.private", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeBuilder.Mixin} is not static.
     *
     * <p>The text is resolved from the {@code validation.builderMixin.notStatic} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String builderMixinNotStatic(ExecutableElement method) {
        return localize("validation.builderMixin.notStatic", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeBuilder.Mixin} takes no parameters.
     *
     * <p>The text is resolved from the {@code validation.builderMixin.noParameters} key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String builderMixinNoParameters(ExecutableElement method) {
        return localize("validation.builderMixin.noParameters", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeBuilder.Mixin} returns a type that is not assignable to the type of its
     * first parameter.
     *
     * <p>The text is resolved from the {@code validation.builderMixin.returnType} key.
     *
     * @param method the illegal method, which must take at least one parameter
     * @return the localized diagnostic
     */
    public static String builderMixinReturnType(ExecutableElement method) {
        return localize("validation.builderMixin.returnType", describe(method), method.getReturnType(), method.getParameters().getFirst().asType());
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDefaultValue} is private.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.methodPrivate} key.
     *
     * @param method the private method
     * @return the localized diagnostic
     */
    public static String defaultValueMethodPrivate(Element method) {
        return localize("validation.defaultValue.methodPrivate", describe(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDefaultValue} is not static.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.methodNotStatic}
     * key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String defaultValueMethodNotStatic(Element method) {
        return localize("validation.defaultValue.methodNotStatic", describe(method), describeEnclosing(method));
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDefaultValue} takes parameters.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.methodParameters}
     * key.
     *
     * @param method the illegal method
     * @return the localized diagnostic
     */
    public static String defaultValueMethodParameters(ExecutableElement method) {
        return localize("validation.defaultValue.methodParameters", describe(method), method.getParameters().size());
    }

    /**
     * Returns the diagnostic reported when a method annotated with
     * {@code @TypeDefaultValue} is not declared in a mixin and returns a type that is not
     * assignable to its enclosing type.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.methodReturnType}
     * key.
     *
     * @param method the illegal method
     * @param enclosingType the type that encloses {@code method}
     * @return the localized diagnostic
     */
    public static String defaultValueMethodReturnType(ExecutableElement method, TypeElement enclosingType) {
        return localize("validation.defaultValue.methodReturnType", describe(method), enclosingType.getQualifiedName(), method.getReturnType());
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @TypeDefaultValue} is private.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.fieldPrivate} key.
     *
     * @param field the private field
     * @return the localized diagnostic
     */
    public static String defaultValueFieldPrivate(Element field) {
        return localize("validation.defaultValue.fieldPrivate", describe(field));
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @TypeDefaultValue} is not static.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.fieldNotStatic}
     * key.
     *
     * @param field the illegal field
     * @return the localized diagnostic
     */
    public static String defaultValueFieldNotStatic(Element field) {
        return localize("validation.defaultValue.fieldNotStatic", describe(field), describeEnclosing(field));
    }

    /**
     * Returns the diagnostic reported when a field annotated with
     * {@code @TypeDefaultValue} is not declared in a mixin and has a type that is not
     * assignable to its enclosing type.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.fieldType} key.
     *
     * @param field the illegal field
     * @param enclosingType the type that encloses {@code field}
     * @return the localized diagnostic
     */
    public static String defaultValueFieldType(Element field, TypeElement enclosingType) {
        return localize("validation.defaultValue.fieldType", describe(field), enclosingType.getQualifiedName(), field.asType());
    }

    /**
     * Returns the diagnostic reported when an element annotated with
     * {@code @TypeDefaultValue} is neither a method, nor a field, nor an enum constant.
     *
     * <p>The text is resolved from the {@code validation.defaultValue.illegalKind} key.
     *
     * @param element the illegal element
     * @return the localized diagnostic
     */
    public static String defaultValueIllegalKind(Element element) {
        return localize("validation.defaultValue.illegalKind", describe(element));
    }

    /**
     * Returns the diagnostic reported when a member annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory} is not declared in a type
     * annotated with {@code @TypeMixin}.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.notInMixin} key.
     *
     * @param member the illegal member
     * @param annotation the annotation of the member
     * @return the localized diagnostic
     */
    public static String mixinInstanceNotInMixin(Element member, Class<? extends Annotation> annotation) {
        return localize("validation.mixinInstance.notInMixin", describe(member), annotationName(annotation), describeEnclosing(member));
    }

    /**
     * Returns the diagnostic reported when a member annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory} is private.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.private} key.
     *
     * @param member the illegal member
     * @param annotation the annotation of the member
     * @return the localized diagnostic
     */
    public static String mixinInstancePrivate(Element member, Class<? extends Annotation> annotation) {
        return localize("validation.mixinInstance.private", describe(member), annotationName(annotation));
    }

    /**
     * Returns the diagnostic reported when a field or a method annotated with
     * {@code @TypeMixin.Instance}, or a method annotated with {@code @TypeMixin.Factory},
     * is not static.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.notStatic} key.
     *
     * @param member the illegal member
     * @param annotation the annotation of the member
     * @return the localized diagnostic
     */
    public static String mixinInstanceNotStatic(Element member, Class<? extends Annotation> annotation) {
        return localize("validation.mixinInstance.notStatic", describe(member), annotationName(annotation), describeEnclosing(member));
    }

    /**
     * Returns the diagnostic reported when a method or a constructor annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory} takes parameters.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.parameters} key.
     *
     * @param member the illegal method or constructor
     * @param annotation the annotation of the member
     * @return the localized diagnostic
     */
    public static String mixinInstanceParameters(ExecutableElement member, Class<? extends Annotation> annotation) {
        return localize("validation.mixinInstance.parameters", describe(member), annotationName(annotation), describeEnclosing(member));
    }

    /**
     * Returns the diagnostic reported when the type of a field annotated with
     * {@code @TypeMixin.Instance}, or the return type of a method annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory}, is not assignable to the
     * mixin that declares it.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.type} key.
     *
     * @param member the illegal field or method
     * @param annotation the annotation of the member
     * @param type the type of the field, or the return type of the method
     * @return the localized diagnostic
     */
    public static String mixinInstanceType(Element member, Class<? extends Annotation> annotation, Object type) {
        return localize("validation.mixinInstance.type", describe(member), annotationName(annotation), type, describeEnclosing(member));
    }

    /**
     * Returns the diagnostic reported when a constructor annotated with
     * {@code @TypeMixin.Factory} belongs to an abstract class, which cannot be
     * instantiated.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.abstractFactory} key.
     *
     * @param constructor the illegal constructor
     * @return the localized diagnostic
     */
    public static String mixinInstanceAbstractFactory(ExecutableElement constructor) {
        return localize("validation.mixinInstance.abstractFactory", describe(constructor), describeEnclosing(constructor));
    }

    /**
     * Returns the diagnostic reported when a mixin has more than one member annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory}.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.duplicate} key.
     *
     * @param member the member reported, which is not the first one
     * @param first the first member of the mixin annotated with either annotation
     * @return the localized diagnostic
     */
    public static String mixinInstanceDuplicate(Element member, Element first) {
        return localize("validation.mixinInstance.duplicate", describe(member), describeEnclosing(member), describe(first));
    }

    /**
     * Returns the warning reported when a mixin has a member annotated with
     * {@code @TypeMixin.Instance} or {@code @TypeMixin.Factory}, but every converter it
     * declares is static, so its instance is never used.
     *
     * <p>The text is resolved from the {@code validation.mixinInstance.unused} key.
     *
     * @param member the unused member
     * @param annotation the annotation of the member
     * @return the localized diagnostic
     */
    public static String mixinInstanceUnused(Element member, Class<? extends Annotation> annotation) {
        return localize("validation.mixinInstance.unused", describe(member), annotationName(annotation), describeEnclosing(member));
    }

    /**
     * Returns the name of the given annotation as it is written in source code, including
     * the names of the types that enclose it, such as {@code TypeBuilder.Mixin}.
     *
     * @param annotation the annotation
     * @return the name of the annotation
     */
    private static String annotationName(Class<? extends Annotation> annotation) {
        String canonicalName = annotation.getCanonicalName();
        String packageName = annotation.getPackageName();
        return packageName.isEmpty() ? canonicalName : canonicalName.substring(packageName.length() + 1);
    }

    /**
     * Renders the given element as it should be named by a diagnostic.
     *
     * <p>A type is rendered as its qualified name. A member of a type is rendered as the
     * qualified name of that type followed by a dot and by the member, which for a
     * method or a constructor includes the types of its parameters. Any other element is
     * rendered as the element that encloses it followed by a dot and by its simple name.
     *
     * @param element the element
     * @return the rendered element
     */
    private static String describe(Element element) {
        if (element instanceof TypeElement type) {
            return type.getQualifiedName().toString();
        }

        if (element instanceof PackageElement packageElement) {
            return packageElement.getQualifiedName().toString();
        }

        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) {
            return element.toString();
        }

        if (enclosing instanceof TypeElement type) {
            return type.getQualifiedName() + "." + element;
        }

        return describe(enclosing) + "." + element.getSimpleName();
    }

    /**
     * Renders the element that directly encloses the given element as it should be named
     * by a diagnostic.
     *
     * @param element the element
     * @return the rendered enclosing element
     * @see #describe(Element)
     */
    private static String describeEnclosing(Element element) {
        Element enclosing = element.getEnclosingElement();
        return enclosing == null ? describe(element) : describe(enclosing);
    }

    /**
     * Formats the bundle entry with the given key for the default locale.
     *
     * @param key the key of the bundle entry
     * @param args the arguments substituted into the entry
     * @return the formatted entry
     */
    private static String localize(String key, Object... args) {
        Locale locale = Locale.getDefault();
        String pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }
}
