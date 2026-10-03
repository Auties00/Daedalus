package com.github.auties00.daedalus.processor.diagnostic;

import com.github.auties00.daedalus.processor.exception.DaedalusProcessorException;

import javax.lang.model.element.TypeElement;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringJoiner;

/**
 * Static factory methods for the diagnostics reported while resolving the scope of
 * mixins, such as the global mixins declared by the dependencies of the compilation.
 *
 * <p>Every method returns the text of a diagnostic localized for the
 * {@linkplain Locale#getDefault() default locale}. The text is looked up in the
 * {@code diagnostics} resource bundle of this package using {@link MessageFormat}, and
 * the key of each entry is documented by the method that uses it.
 *
 * <p>This class is thread-safe.
 */
public final class DaedalusMixinErrors {
    /**
     * The base name of the resource bundle containing the text of every diagnostic.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.processor.diagnostic.diagnostics";

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private DaedalusMixinErrors() {
        throw new UnsupportedOperationException("DaedalusMixinErrors is a utility class and cannot be instantiated");
    }

    /**
     * Returns the diagnostic reported when the class path of the compilation cannot be
     * read, so the global mixins declared by its dependencies cannot be discovered.
     *
     * <p>The diagnostic also warns that the built-in mixins, which support the types of
     * the JDK, are global mixins of a dependency too, so they are only applied where they
     * are referenced explicitly.
     *
     * <p>The text is resolved from the {@code mixin.unreadableClasspath} key.
     *
     * @param cause the error that prevented the class path from being read
     * @return the localized diagnostic
     */
    public static String unreadableClasspath(Exception cause) {
        return localize("mixin.unreadableClasspath", DaedalusProcessorException.describe(cause));
    }

    /**
     * Returns the diagnostic reported when the index of the global mixins of an entry of
     * the class path or of the module path cannot be read.
     *
     * <p>The text is resolved from the {@code mixin.unreadableIndex} key.
     *
     * @param entry the entry whose index cannot be read
     * @param indexPath the path of the index, relative to the entry
     * @param cause the error that prevented the index from being read
     * @return the localized diagnostic
     */
    public static String unreadableIndex(Path entry, String indexPath, Exception cause) {
        return localize("mixin.unreadableIndex", entry, indexPath, DaedalusProcessorException.describe(cause));
    }

    /**
     * Returns the diagnostic reported when the index of the global mixins declared by the
     * compilation cannot be written to its output.
     *
     * <p>The text is resolved from the {@code mixin.unwritableIndex} key.
     *
     * @param indexPath the path of the index, relative to the output
     * @param mixins the global mixins that the index should list
     * @param cause the error that prevented the index from being written
     * @return the localized diagnostic
     */
    public static String unwritableIndex(String indexPath, Collection<TypeElement> mixins, Exception cause) {
        var names = new StringJoiner(", ");
        names.setEmptyValue("none");
        for (var mixin : mixins) {
            names.add(mixin.getQualifiedName());
        }
        return localize("mixin.unwritableIndex", indexPath, names, DaedalusProcessorException.describe(cause));
    }

    /**
     * Resolves the entry with the given key and formats it with the given arguments.
     *
     * @param key the key of the entry
     * @param args the arguments of the entry
     * @return the localized text
     */
    private static String localize(String key, Object... args) {
        var locale = Locale.getDefault();
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }
}
