package com.github.auties00.daedalus.processor.manager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * A utility for printing compiler diagnostics during annotation processing.
 *
 * <p>This class wraps the annotation processing environment's messager to provide
 * convenient methods for emitting warnings, errors, and informational notes
 * during compilation.
 */
public final class DaedalusLogManager {

    /**
     * The annotation processing environment used to obtain the messager.
     */
    private final ProcessingEnvironment processingEnv;

    /**
     * Constructs a new diagnostics utility with the given processing environment.
     *
     * @param processingEnv the annotation processing environment
     */
    public DaedalusLogManager(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Prints a mandatory warning message associated with the given element.
     *
     * <p>If the element is {@code null}, the warning is emitted without an
     * associated program element.
     *
     * @param msg the warning message
     * @param element the element to associate with the warning, or {@code null}
     *        for a general warning
     */
    public void printWarning(String msg, Element element) {
        if (element == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, msg);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, msg, element);
        }
    }

    /**
     * Prints an error message associated with the given element.
     *
     * @param msg the error message
     * @param element the element to associate with the error
     */
    public void printError(String msg, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
    }

    /**
     * Prints an informational note without an associated element.
     *
     * @param msg the note message
     */
    public void printInfo(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    /**
     * Prints an informational note associated with the given element.
     *
     * @param msg the note message
     * @param element the element to associate with the note
     */
    public void printNote(String msg, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg, element);
    }
}
