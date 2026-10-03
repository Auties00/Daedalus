package com.github.auties00.daedalus.processor.diagnostic;

import com.github.auties00.daedalus.processor.graph.DaedalusConverterArc;
import com.github.auties00.daedalus.processor.model.DaedalusConverterParametersElement;
import com.github.auties00.daedalus.processor.model.DaedalusMethodElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Static factory methods for the diagnostics reported while resolving converters and
 * their additional parameters.
 *
 * <p>Every method returns the text of a diagnostic localized for the
 * {@linkplain Locale#getDefault() default locale}. The text is looked up in the
 * {@code diagnostics} resource bundle of this package using {@link MessageFormat}, and
 * the key of each entry is documented by the method that uses it.
 *
 * <p>This class is thread-safe.
 */
public final class DaedalusConverterErrors {
    /**
     * The base name of the resource bundle containing the text of every diagnostic.
     */
    private static final String BUNDLE_NAME = "com.github.auties00.daedalus.processor.diagnostic.diagnostics";

    /**
     * Prevents the instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private DaedalusConverterErrors() {
        throw new UnsupportedOperationException("DaedalusConverterErrors is a utility class and cannot be instantiated");
    }

    /**
     * Returns the diagnostic reported when no serializer path exists from a type.
     *
     * <p>The text is resolved from the {@code converter.missingSerializer} key.
     *
     * @param from the type that cannot be serialized
     * @return the localized diagnostic
     */
    public static String missingSerializer(TypeMirror from) {
        return localize("converter.missingSerializer", from);
    }

    /**
     * Returns the diagnostic reported when no deserializer path exists to a type.
     *
     * <p>The text is resolved from the {@code converter.missingDeserializer} key.
     *
     * @param to the type that cannot be deserialized
     * @return the localized diagnostic
     */
    public static String missingDeserializer(TypeMirror to) {
        return localize("converter.missingDeserializer", to);
    }

    /**
     * Returns the diagnostic reported when every serializer path from a type goes
     * through a converter whose additional parameters are not supplied by the use site.
     *
     * <p>The text is resolved from the {@code converter.missingSerializerArguments} key,
     * and each candidate path is rendered as described by
     * {@link #missingDeserializerArguments(TypeMirror, TypeMirror, Collection, List, Set)}.
     *
     * @param from the type that cannot be serialized
     * @param missingAnnotations the companion parameters annotations that
     *        would supply the missing arguments
     * @param candidates the paths that would work if the arguments were supplied
     * @param suppliedAnnotations the companion parameters annotations that the
     *        use site already supplies
     * @return the localized diagnostic
     */
    public static String missingSerializerArguments(TypeMirror from, Collection<DaedalusConverterParametersElement> missingAnnotations, List<List<DaedalusConverterArc>> candidates, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        return localize("converter.missingSerializerArguments", from, formatAnnotations(missingAnnotations), formatCandidates(from, candidates, suppliedAnnotations));
    }

    /**
     * Returns the diagnostic reported when every deserializer path to a type goes
     * through a converter whose additional parameters are not supplied by the use site.
     *
     * <p>The text is resolved from the {@code converter.missingDeserializerArguments}
     * key. Each candidate path is appended using the {@code converter.candidatePath}
     * key, and each of its steps that requires a missing annotation is marked using the
     * {@code converter.candidateStepArguments} key.
     *
     * @param from the wire type that every candidate path starts from
     * @param to the type that cannot be deserialized
     * @param missingAnnotations the companion parameters annotations that
     *        would supply the missing arguments
     * @param candidates the paths that would work if the arguments were supplied
     * @param suppliedAnnotations the companion parameters annotations that the
     *        use site already supplies
     * @return the localized diagnostic
     */
    public static String missingDeserializerArguments(TypeMirror from, TypeMirror to, Collection<DaedalusConverterParametersElement> missingAnnotations, List<List<DaedalusConverterArc>> candidates, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        return localize("converter.missingDeserializerArguments", to, formatAnnotations(missingAnnotations), formatCandidates(from, candidates, suppliedAnnotations));
    }

    /**
     * Returns the diagnostic reported when an additional parameter of a converter has a
     * type whose values cannot be supplied by an annotation.
     *
     * <p>The text is resolved from the {@code converter.illegalParameterType} key.
     *
     * @param parameter the illegal parameter
     * @return the localized diagnostic
     */
    public static String illegalParameterType(VariableElement parameter) {
        return localize("converter.illegalParameterType", parameter.asType(), parameter.getSimpleName());
    }

    /**
     * Returns the diagnostic reported when two converters of the same type declare
     * different additional parameters, which cannot be supplied by a single companion
     * parameters annotation.
     *
     * <p>The text is resolved from the {@code converter.mismatchedParameters} key.
     *
     * @param method the converter whose additional parameters do not match
     * @param parameters the additional parameters of {@code method}
     * @param existingMethod the converter that first declared additional parameters
     * @param existingParameters the additional parameters of {@code existingMethod}
     * @param annotation the companion parameters annotation
     * @return the localized diagnostic
     */
    public static String mismatchedParameters(ExecutableElement method, List<? extends VariableElement> parameters, ExecutableElement existingMethod, List<? extends VariableElement> existingParameters, DaedalusConverterParametersElement annotation) {
        return localize("converter.mismatchedParameters", method.getSimpleName(), formatParameters(parameters), existingMethod.getSimpleName(), formatParameters(existingParameters), annotation);
    }

    /**
     * Returns the diagnostic reported when a companion parameters annotation cannot be
     * written.
     *
     * <p>The text is resolved from the {@code converter.annotationGenerationFailed} key.
     *
     * @param annotation the annotation
     * @param cause the error that prevented the annotation from being written
     * @return the localized diagnostic
     */
    public static String annotationGenerationFailed(DaedalusConverterParametersElement annotation, Exception cause) {
        return localize("converter.annotationGenerationFailed", annotation, cause.getMessage());
    }

    /**
     * Returns the diagnostic reported when the holder class that keeps the instance of a
     * mixin created by its member annotated with {@code @TypeMixin.Factory} cannot be
     * written.
     *
     * <p>The text is resolved from the {@code converter.holderGenerationFailed} key.
     *
     * @param holderName the fully qualified name of the holder class
     * @param mixin the fully qualified name of the mixin
     * @param cause the error that prevented the holder class from being written
     * @return the localized diagnostic
     */
    public static String holderGenerationFailed(String holderName, String mixin, Exception cause) {
        return localize("converter.holderGenerationFailed", holderName, mixin, cause.getMessage());
    }

    /**
     * Returns the diagnostic reported when an element is converted by a converter whose
     * annotation declares a warning in its {@code warning} attribute.
     *
     * <p>The warning itself is written by the author of the converter, so it is
     * embedded verbatim in the diagnostic and is not localized.
     *
     * <p>The text is resolved from the {@code converter.declaredWarning} key.
     *
     * @param method the converter that declares the warning
     * @return the localized diagnostic
     */
    public static String declaredWarning(DaedalusMethodElement method) {
        return localize("converter.declaredWarning", method, method.warning());
    }

    /**
     * Renders the given annotations as a list of annotation usages.
     *
     * @param annotations the annotations
     * @return the rendered list
     */
    private static String formatAnnotations(Collection<DaedalusConverterParametersElement> annotations) {
        var result = new StringJoiner(", ");
        for (var annotation : annotations) {
            result.add("@" + annotation);
        }
        return result.toString();
    }

    /**
     * Renders the given parameters as a comma separated list of declarations.
     *
     * @param parameters the parameters
     * @return the rendered list
     */
    private static String formatParameters(List<? extends VariableElement> parameters) {
        var result = new StringJoiner(", ");
        for (var parameter : parameters) {
            result.add(parameter.asType() + " " + parameter.getSimpleName());
        }
        return result.toString();
    }

    /**
     * Renders the given candidate paths, one per line, marking the steps that require an
     * annotation which the use site does not supply.
     *
     * @param from the source type of every path
     * @param candidates the paths
     * @param suppliedAnnotations the annotations that the use site supplies
     * @return the rendered paths
     */
    private static String formatCandidates(TypeMirror from, List<List<DaedalusConverterArc>> candidates, Set<DaedalusConverterParametersElement> suppliedAnnotations) {
        var result = new StringBuilder();
        for (var candidate : candidates) {
            var path = new StringJoiner(" -> ");
            path.add(from.toString());
            for (var arc : candidate) {
                var step = arc.method().toString();
                for (var annotation : arc.parametersAnnotations()) {
                    if (!suppliedAnnotations.contains(annotation)) {
                        step = localize("converter.candidateStepArguments", step, annotation);
                    }
                }
                path.add(step);
                path.add(arc.returnType().toString());
            }
            result.append(localize("converter.candidatePath", path));
        }
        return result.toString();
    }

    /**
     * Formats the bundle entry with the given key for the default locale.
     *
     * @param key the key of the bundle entry
     * @param args the arguments substituted into the entry
     * @return the formatted entry
     */
    private static String localize(String key, Object... args) {
        var locale = Locale.getDefault();
        var pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        return new MessageFormat(pattern, locale).format(args);
    }
}
