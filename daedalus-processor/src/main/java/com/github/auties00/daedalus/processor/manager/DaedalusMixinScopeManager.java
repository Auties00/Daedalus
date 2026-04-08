package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
 * A resolver that collects {@code @TypeMixin} annotations from each processing round
 * and indexes them by scope for efficient lookup.
 *
 * <p>The resolver also handles {@code @TypeMixin.Import} transitively: each applicable
 * mixin's imports are included in the result set.
 */
public final class DaedalusMixinScopeManager {

    private final DaedalusTypeManager types;
    private final Map<String, Set<TypeElement>> packageMixins;
    private final Map<String, Set<TypeElement>> moduleMixins;
    private final Set<TypeElement> globalMixins;

    /**
     * Constructs a new mixin scope resolver.
     *
     * @param types the common type utilities for mirrored type resolution
     */
    public DaedalusMixinScopeManager(DaedalusTypeManager types) {
        this.types = types;
        this.packageMixins = new HashMap<>();
        this.moduleMixins = new HashMap<>();
        this.globalMixins = new LinkedHashSet<>();
    }

    /**
     * Scans the given round environment for {@code @TypeMixin} annotations and
     * indexes them by scope.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void populate(RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(TypeMixin.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }

            var annotation = typeElement.getAnnotation(TypeMixin.class);
            if (annotation == null) {
                continue;
            }

            switch (annotation.scope()) {
                case MANUAL -> {
                }
                case PACKAGE -> {
                    var packageName = types.getPackageName(typeElement);
                    packageMixins.computeIfAbsent(packageName, _ -> new LinkedHashSet<>()).add(typeElement);
                }
                case MODULE -> {
                    var moduleName = types.getModuleName(typeElement);
                    moduleMixins.computeIfAbsent(moduleName, _ -> new LinkedHashSet<>()).add(typeElement);
                }
                case GLOBAL -> globalMixins.add(typeElement);
            }
        }
    }

    /**
     * Returns the set of all applicable mixins for the given target type.
     *
     * @param target the type element to find applicable mixins for
     * @param explicitMixins the explicitly referenced mixin types
     * @return the full set of applicable mixin type elements
     */
    public Set<TypeElement> getApplicableMixins(TypeElement target, Set<TypeElement> explicitMixins) {
        Set<TypeElement> result = new LinkedHashSet<>(explicitMixins);

        var targetPackage = types.getPackageName(target);
        var packageScoped = packageMixins.get(targetPackage);
        if (packageScoped != null) {
            result.addAll(packageScoped);
        }

        var targetModule = types.getModuleName(target);
        var moduleScoped = moduleMixins.get(targetModule);
        if (moduleScoped != null) {
            result.addAll(moduleScoped);
        }

        result.addAll(globalMixins);

        resolveImports(result);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Transitively resolves {@code @TypeMixin.Import} annotations on the given
     * set of mixins, adding all imported mixin types to the set.
     *
     * @param mixins the mixin set to expand with imports (modified in place)
     */
    private void resolveImports(Set<TypeElement> mixins) {
        var queue = new ArrayDeque<>(mixins);
        while (!queue.isEmpty()) {
            var mixin = queue.poll();
            var annotation = mixin.getAnnotation(TypeMixin.class);
            if (annotation == null) {
                continue;
            }

            for (var importAnnotation : annotation.imports()) {
                var imported = types.getMirroredTypes(importAnnotation::value);
                for (var importedMixin : imported) {
                    if (mixins.add(importedMixin)) {
                        queue.add(importedMixin);
                    }
                }
            }
        }
    }
}
