package com.github.auties00.daedalus.processor.model;

import com.github.auties00.daedalus.processor.util.DaedalusMixinInstanceUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Function;

/**
 * A converter method that transforms a value from one type to another during
 * serialization or deserialization.
 *
 * <p>Converter methods come in three flavors:
 * <ul>
 * <li>{@link Concrete} methods are backed by an actual {@link ExecutableElement}
 *     declared in source code.
 * <li>{@link Synthetic} methods are generated programmatically by the processor
 *     framework and have no corresponding source element.
 * <li>{@link Inline} conversions have no method at all: they are expressions that the
 *     generated code evaluates in place, such as a {@code switch} over the constants of
 *     an enum.
 * </ul>
 *
 * <p>Instances are created via the {@link #of(ExecutableElement, boolean, String)},
 * {@link #of(String, Set, TypeMirror, String, TypeMirror...)} and
 * {@link #inline(String, String, TypeMirror, TypeMirror, String, String, List)} factory
 * methods.
 */
public sealed interface DaedalusMethodElement {

    /**
     * Returns the executable element backing this converter method, if one exists.
     *
     * @return an {@link Optional} containing the executable element for concrete
     *         methods, or an empty {@code Optional} for synthetic methods
     */
    Optional<ExecutableElement> element();

    /**
     * Returns the fully qualified name of the type that owns this converter method.
     *
     * @return the owner type name
     */
    String ownerName();

    /**
     * Returns the set of modifiers applied to this converter method.
     *
     * @return the modifiers
     */
    Set<Modifier> modifiers();

    /**
     * Returns the expression that evaluates to the instance of a mixin on which this
     * converter method is invoked, with the value to convert as its first argument.
     *
     * <p>Only an instance method declared in a mixin has a receiver, which is provided or
     * created by the member of the mixin annotated with
     * {@link com.github.auties00.daedalus.typesystem.annotation.TypeMixin.Instance} or
     * {@link com.github.auties00.daedalus.typesystem.annotation.TypeMixin.Factory}, as
     * described by {@link DaedalusMixinInstanceUtils#getInstanceExpression(TypeElement)}.
     *
     * @return the receiver expression, or an empty {@link Optional} if this method is a
     *         constructor, a static method, or an instance method of the type of the value
     */
    Optional<String> receiver();

    /**
     * Returns the return type of this converter method.
     *
     * @return the return type
     */
    TypeMirror returnType();

    /**
     * Returns the simple name of this converter method.
     *
     * @return the method name
     */
    String name();

    /**
     * Returns whether this converter method uses type parameters that need to be
     * resolved against the actual type arguments at the call site.
     *
     * @return {@code true} if this method is parametrized
     */
    boolean isParametrized();

    /**
     * Returns the parameter types of this converter method.
     *
     * @return the parameter types
     */
    List<TypeMirror> parameters();

    /**
     * Returns a warning message associated with this converter method, or an
     * empty string if no warning is present.
     *
     * @return the warning message
     */
    String warning();

    /**
     * Returns the Java source expression that invokes this converter method on the
     * given value, passing the given arguments to its additional parameters.
     *
     * <p>An instance method of the type of the value is invoked on the value, a
     * constructor creates a new instance of its owner type from the value, and a static
     * method, or an instance method of a mixin invoked on its {@linkplain #receiver()
     * receiver}, receives the value as its first argument. The arguments follow the value
     * in order: a
     * {@link DaedalusConverterArgument.Value} is rendered as its expression, while a
     * {@link DaedalusConverterArgument.Injected} is rendered by {@code injector}.
     *
     * <p>For example, a static method {@code toNullable} declared in
     * {@code InstantMixin} and invoked with a single value argument produces:
     * <pre>{@code
     * InstantMixin.toNullable(value, java.util.concurrent.TimeUnit.SECONDS)
     * }</pre>
     *
     * @param value the expression that provides the value to convert
     * @param arguments the arguments passed to the additional parameters of this
     *        method, in declaration order
     * @param injector the function that renders the expression of an injected argument
     * @return the invocation expression
     */
    default String invoke(String value, List<DaedalusConverterArgument> arguments, Function<DaedalusConverterArgument.Injected, String> injector) {
        var renderedArguments = new StringJoiner(", ");
        for (var argument : arguments) {
            var expression = switch (argument) {
                case DaedalusConverterArgument.Value valueArgument -> valueArgument.expression();
                case DaedalusConverterArgument.Injected injectedArgument -> injector.apply(injectedArgument);
            };
            renderedArguments.add(expression);
        }

        var constructor = element()
                .map(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .orElse(false);
        if (constructor) {
            return "new %s(%s)".formatted(ownerName(), prependValue(value, renderedArguments));
        }

        var receiver = receiver();
        if (receiver.isPresent()) {
            return "%s.%s(%s)".formatted(receiver.get(), name(), prependValue(value, renderedArguments));
        }

        if (!modifiers().contains(Modifier.STATIC)) {
            return "%s.%s(%s)".formatted(value, name(), renderedArguments);
        }

        return "%s.%s(%s)".formatted(ownerName(), name(), prependValue(value, renderedArguments));
    }

    /**
     * Returns the argument list of an invocation whose first argument is the given value.
     *
     * @param value the expression of the first argument
     * @param arguments the remaining arguments
     * @return the joined argument list
     */
    private static String prependValue(String value, StringJoiner arguments) {
        return arguments.length() == 0 ? value : value + ", " + arguments;
    }

    /**
     * Creates a concrete converter method backed by an executable element.
     *
     * @param element the executable element
     * @param parametrized whether the method uses type parameters
     * @param warning a warning message, or an empty string if none
     * @return a new concrete converter method
     */
    static DaedalusMethodElement of(ExecutableElement element, boolean parametrized, String warning) {
        return new Concrete(element, parametrized, warning);
    }

    /**
     * Creates an inline conversion, which is the expression made of the given prefix,
     * the expression of the value to convert, and the given suffix.
     *
     * <p>The expression of the value appears once in the conversion, so it is evaluated
     * once, as it would be if it were the argument of a method.
     *
     * @param owner the fully qualified name of the type that the conversion belongs to
     * @param name a name that identifies the conversion in diagnostics
     * @param returnType the type of the expression
     * @param parameter the type of the value to convert
     * @param prefix the source code that precedes the value to convert
     * @param suffix the source code that follows the value to convert
     * @param thrownTypes the checked exceptions that the expression may throw
     * @return a new inline conversion
     */
    static DaedalusMethodElement inline(String owner, String name, TypeMirror returnType, TypeMirror parameter, String prefix, String suffix, List<TypeMirror> thrownTypes) {
        return new Inline(owner, name, returnType, parameter, prefix, suffix, thrownTypes);
    }

    /**
     * Creates a synthetic converter method with no backing source element.
     *
     * @param owner the fully qualified name of the owning type
     * @param modifiers the modifiers for the method
     * @param returnType the return type
     * @param name the method name
     * @param parameters the parameter types
     * @return a new synthetic converter method
     */
    static DaedalusMethodElement of(String owner, Set<Modifier> modifiers, TypeMirror returnType, String name, TypeMirror... parameters) {
        return new Synthetic(owner, null, modifiers, returnType, name, parameters);
    }

    /**
     * Creates a synthetic converter method with no backing source element, which is an
     * instance method of a mixin invoked on the given receiver with the value to convert
     * as its first argument.
     *
     * @param owner the fully qualified name of the owning mixin
     * @param receiver the expression that evaluates to the instance of the mixin
     * @param returnType the return type
     * @param name the method name
     * @param parameters the parameter types, the first of which is the type of the value
     * @return a new synthetic converter method
     * @throws NullPointerException if {@code receiver} is {@code null}
     */
    static DaedalusMethodElement ofMixinInstance(String owner, String receiver, TypeMirror returnType, String name, TypeMirror... parameters) {
        return new Synthetic(owner, Objects.requireNonNull(receiver, "receiver cannot be null"), Set.of(Modifier.PUBLIC), returnType, name, parameters);
    }

    /**
     * A concrete converter method backed by an {@link ExecutableElement} declared in source code.
     */
    final class Concrete implements DaedalusMethodElement {
        private final ExecutableElement element;
        private final boolean parametrized;
        private final String warning;
        private final String receiver;

        private Concrete(ExecutableElement element, boolean parametrized, String warning) {
            this.element = element;
            this.parametrized = parametrized;
            this.warning = warning;
            this.receiver = DaedalusMixinInstanceUtils.isInvokedOnMixinInstance(element)
                    ? DaedalusMixinInstanceUtils.getInstanceExpression((TypeElement) element.getEnclosingElement()).orElse(null)
                    : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> receiver() {
            return Optional.ofNullable(receiver);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ExecutableElement> element() {
            return Optional.of(element);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParametrized() {
            return parametrized;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String ownerName() {
            var typeElement = (TypeElement) element.getEnclosingElement();
            return typeElement.getQualifiedName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TypeMirror returnType() {
            return element.getReturnType();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String name() {
            return element.getSimpleName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<TypeMirror> parameters() {
            return element.getParameters()
                    .stream()
                    .map(VariableElement::asType)
                    .toList();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Modifier> modifiers() {
            return element.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String warning() {
            return warning;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Concrete that
                    && Objects.equals(element, that.element);
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }

        @Override
        public String toString() {
            return ownerName() + "#" + name();
        }
    }

    /**
     * A synthetic converter method generated programmatically by the processor framework.
     *
     * <p>Synthetic methods have no backing source element and are never parametrized.
     */
    final class Synthetic implements DaedalusMethodElement {
        private final String ownerName;
        private final String receiver;
        private final Set<Modifier> modifiers;
        private final String name;
        private final TypeMirror returnType;
        private final List<TypeMirror> parameters;

        private Synthetic(String ownerName, String receiver, Set<Modifier> modifiers, TypeMirror returnType, String name, TypeMirror... parameters) {
            this.ownerName = ownerName;
            this.receiver = receiver;
            this.modifiers = modifiers;
            this.name = name;
            this.returnType = returnType;
            this.parameters = Arrays.asList(parameters);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> receiver() {
            return Optional.ofNullable(receiver);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ExecutableElement> element() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParametrized() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String ownerName() {
            return ownerName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TypeMirror returnType() {
            return returnType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String name() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<TypeMirror> parameters() {
            return parameters;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Modifier> modifiers() {
            return modifiers;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String warning() {
            return "";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Synthetic that
                    && Objects.equals(ownerName, that.ownerName)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownerName, name);
        }

        @Override
        public String toString() {
            return ownerName() + "#" + name();
        }
    }

    /**
     * An inline conversion, which the generated code evaluates in place instead of
     * invoking a method.
     *
     * <p>Inline conversions have no backing source element, are static, are never
     * parametrized, and never receive arguments for additional parameters.
     */
    final class Inline implements DaedalusMethodElement {
        private final String ownerName;
        private final String name;
        private final TypeMirror returnType;
        private final List<TypeMirror> parameters;
        private final String prefix;
        private final String suffix;
        private final List<TypeMirror> thrownTypes;

        private Inline(String ownerName, String name, TypeMirror returnType, TypeMirror parameter, String prefix, String suffix, List<TypeMirror> thrownTypes) {
            this.ownerName = ownerName;
            this.name = name;
            this.returnType = returnType;
            this.parameters = List.of(parameter);
            this.prefix = prefix;
            this.suffix = suffix;
            this.thrownTypes = List.copyOf(thrownTypes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ExecutableElement> element() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String ownerName() {
            return ownerName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Modifier> modifiers() {
            return Set.of(Modifier.PUBLIC, Modifier.STATIC);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> receiver() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TypeMirror returnType() {
            return returnType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String name() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParametrized() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<TypeMirror> parameters() {
            return parameters;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String warning() {
            return "";
        }

        /**
         * Returns the checked exceptions that the expression of this conversion may throw.
         *
         * @return the thrown types
         */
        public List<TypeMirror> thrownTypes() {
            return thrownTypes;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns the expression of this conversion applied to the given value. An
         * inline conversion never receives arguments, so {@code arguments} and
         * {@code injector} are ignored.
         */
        @Override
        public String invoke(String value, List<DaedalusConverterArgument> arguments, Function<DaedalusConverterArgument.Injected, String> injector) {
            return prefix + value + suffix;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Inline that
                    && Objects.equals(ownerName, that.ownerName)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownerName, name);
        }

        @Override
        public String toString() {
            return ownerName() + "#" + name();
        }
    }
}
