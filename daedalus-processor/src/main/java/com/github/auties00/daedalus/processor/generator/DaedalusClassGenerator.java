package com.github.auties00.daedalus.processor.generator;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;

/**
 * An abstract base class for code generators that create class files.
 */
public abstract class DaedalusClassGenerator {
    /**
     * The filer used to create generated source files.
     */
    protected final Filer filer;

    /**
     * Constructs a new class generator with the given filer.
     *
     * @param filer the filer for creating source files
     */
    protected DaedalusClassGenerator(Filer filer) {
        this.filer = filer;
    }

    /**
     * Returns the generated class name for the given type element with a suffix appended.
     *
     * <p>For nested types, the names of all enclosing types are prepended.
     * For example, a type {@code Outer.Inner} with suffix {@code "Spec"} produces
     * {@code "OuterInnerSpec"}.
     *
     * @param element the type element
     * @param suffix the suffix to append
     * @return the generated class name
     */
    protected String getGeneratedClassNameWithSuffix(TypeElement element, String suffix) {
        return getGeneratedClassName(element, element.getSimpleName() + suffix);
    }

    /**
     * Returns the generated class name for the given type element with the given class name.
     *
     * <p>For nested types, the names of all enclosing types are prepended.
     *
     * @param element the type element
     * @param className the class name to use
     * @return the generated class name with enclosing type names prepended
     */
    protected String getGeneratedClassName(TypeElement element, String className) {
        var name = new StringBuilder();
        while (element.getEnclosingElement() instanceof TypeElement parent) {
            name.append(parent.getSimpleName());
            element = parent;
        }
        return name + className;
    }
}
