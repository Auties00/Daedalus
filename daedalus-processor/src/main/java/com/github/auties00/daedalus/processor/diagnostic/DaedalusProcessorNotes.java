package com.github.auties00.daedalus.processor.diagnostic;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Static factory methods for the informational notes reported by the annotation
 * processor while it runs the phases of its processing pipeline.
 *
 * <p>Every method returns the text of a note localized for the
 * {@linkplain Locale#getDefault() default locale}. The text is looked up in the
 * {@code diagnostics} resource bundle of this package using {@link MessageFormat}, and
 * the key of each entry is documented by the method that uses it.
 *
 * <p>The name of each phase is localized by its own method, such as
 * {@link #commonChecksPhase()}, and is meant to be passed to
 * {@link #phaseStarted(String)} and {@link #phaseFinished(String, long)}.
 *
 * <p>This class is thread-safe.
 */
public final class DaedalusProcessorNotes {
    /**
     * The base name of the resource bundle containing the text of every note.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.processor.diagnostic.diagnostics";

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private DaedalusProcessorNotes() {
        throw new UnsupportedOperationException("DaedalusProcessorNotes is a utility class and cannot be instantiated");
    }

    /**
     * Returns the note reported when a phase of the processing pipeline starts.
     *
     * <p>The text is resolved from the {@code processor.phaseStarted} key.
     *
     * @param phase the localized name of the phase
     * @return the localized note
     */
    public static String phaseStarted(String phase) {
        return localize("processor.phaseStarted", phase);
    }

    /**
     * Returns the note reported when a phase of the processing pipeline finishes.
     *
     * <p>The text is resolved from the {@code processor.phaseFinished} key.
     *
     * @param phase the localized name of the phase
     * @param elapsedMillis the time spent running the phase, in milliseconds
     * @return the localized note
     */
    public static String phaseFinished(String phase, long elapsedMillis) {
        return localize("processor.phaseFinished", phase, elapsedMillis);
    }

    /**
     * Returns the name of the phase that runs the validation checks common to every
     * serialization format and populates the scope of the mixins.
     *
     * <p>The text is resolved from the {@code processor.phase.commonChecks} key.
     *
     * @return the localized name of the phase
     */
    public static String commonChecksPhase() {
        return localize("processor.phase.commonChecks");
    }

    /**
     * Returns the name of the phase that runs the validation checks of each extension.
     *
     * <p>The text is resolved from the {@code processor.phase.extensionChecks} key.
     *
     * @return the localized name of the phase
     */
    public static String extensionChecksPhase() {
        return localize("processor.phase.extensionChecks");
    }

    /**
     * Returns the name of the phase in which each extension processes its annotated
     * types and populates the converter graph.
     *
     * <p>The text is resolved from the {@code processor.phase.objectProcessing} key.
     *
     * @return the localized name of the phase
     */
    public static String objectProcessingPhase() {
        return localize("processor.phase.objectProcessing");
    }

    /**
     * Returns the name of the phase that resolves the converters of every field through
     * the converter graph.
     *
     * <p>The text is resolved from the {@code processor.phase.attribution} key.
     *
     * @return the localized name of the phase
     */
    public static String attributionPhase() {
        return localize("processor.phase.attribution");
    }

    /**
     * Returns the name of the phase that generates the source code of every extension.
     *
     * <p>The text is resolved from the {@code processor.phase.codeGeneration} key.
     *
     * @return the localized name of the phase
     */
    public static String codeGenerationPhase() {
        return localize("processor.phase.codeGeneration");
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
