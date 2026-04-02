package com.github.auties00.daedalus.processor.model;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * A converter method that transforms a value from one type to another during
 * serialization or deserialization.
 *
 * <p>Converter methods come in two flavors:
 * <ul>
 * <li>{@link Concrete} methods are backed by an actual {@link ExecutableElement}
 *     declared in source code.
 * <li>{@link Synthetic} methods are generated programmatically by the processor
 *     framework and have no corresponding source element.
 * </ul>
 *
 * <p>Instances are created via the {@link #of(ExecutableElement, boolean, String)}
 * and {@link #of(String, Set, TypeMirror, String, TypeMirror...)} factory methods.
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
        return new Synthetic(owner, modifiers, returnType, name, parameters);
    }

    /**
     * A concrete converter method backed by an {@link ExecutableElement} declared in source code.
     */
    final class Concrete implements DaedalusMethodElement {
        private final ExecutableElement element;
        private final boolean parametrized;
        private final String warning;

        private Concrete(ExecutableElement element, boolean parametrized, String warning) {
            this.element = element;
            this.parametrized = parametrized;
            this.warning = warning;
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
        private final Set<Modifier> modifiers;
        private final String name;
        private final TypeMirror returnType;
        private final List<TypeMirror> parameters;

        private Synthetic(String ownerName, Set<Modifier> modifiers, TypeMirror returnType, String name, TypeMirror... parameters) {
            this.ownerName = ownerName;
            this.modifiers = modifiers;
            this.name = name;
            this.returnType = returnType;
            this.parameters = Arrays.asList(parameters);
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
}
