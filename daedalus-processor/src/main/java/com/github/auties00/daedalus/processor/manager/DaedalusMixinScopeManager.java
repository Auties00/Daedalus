package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.processor.exception.DaedalusProcessorClasspathException;
import com.github.auties00.daedalus.processor.model.DaedalusMixins;
import com.github.auties00.daedalus.processor.diagnostic.DaedalusMixinErrors;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * A resolver that collects {@code @TypeMixin} annotations from each processing round
 * and indexes them by scope for efficient lookup.
 *
 * <p>The resolver also handles {@code @TypeMixin.Import} transitively: each applicable
 * mixin's imports are included in the result set.
 *
 * <p>Global mixins also apply to dependencies compiled separately. Every compilation that
 * declares global mixins lists them in the {@value #INDEX_PATH} resource of its output,
 * which is packaged with its classes. The first time mixins are collected, the index of
 * every entry of the class path and of the module path is read, and each listed mixin
 * becomes a global mixin of the current compilation, as long as it can be read by the
 * module being compiled. The index of the output of the current compilation is read as
 * well, so that an incremental compilation keeps the global mixins of the sources it
 * does not recompile.
 */
public final class DaedalusMixinScopeManager {
    /**
     * The path of the resource that lists the global mixins of a compilation, one
     * canonical name per line.
     */
    public static final String INDEX_PATH = "META-INF/daedalus/mixins";

    private final ProcessingEnvironment processingEnv;
    private final DaedalusTypeManager types;
    private final DaedalusClasspathManager classpath;
    private final DaedalusLogManager logs;
    private final Map<String, Set<TypeElement>> packageMixins;
    private final Map<String, Set<TypeElement>> moduleMixins;
    private final Set<TypeElement> globalMixins;

    /**
     * The global mixins declared by the current compilation, which are written to its index.
     */
    private final Set<TypeElement> localGlobalMixins;

    /**
     * Whether the indexes of the dependencies were read.
     */
    private boolean indexesRead;

    /**
     * Whether the output of the current compilation already contained an index, which
     * must be rewritten even if it becomes empty.
     */
    private boolean outputIndexed;

    /**
     * Constructs a new mixin scope resolver.
     *
     * @param processingEnv the annotation processing environment
     * @param types the common type utilities for mirrored type resolution
     * @param classpath the resolver of the locations of the compilation, used to find the
     *        indexes of the dependencies
     * @param logs the compiler diagnostics utility
     */
    public DaedalusMixinScopeManager(ProcessingEnvironment processingEnv, DaedalusTypeManager types, DaedalusClasspathManager classpath, DaedalusLogManager logs) {
        this.processingEnv = processingEnv;
        this.types = types;
        this.classpath = classpath;
        this.logs = logs;
        this.packageMixins = new HashMap<>();
        this.moduleMixins = new HashMap<>();
        this.globalMixins = new LinkedHashSet<>();
        this.localGlobalMixins = new LinkedHashSet<>();
    }

    /**
     * Scans the given round environment for {@code @TypeMixin} annotations and
     * indexes them by scope.
     *
     * <p>The first time this method is invoked, the global mixins listed by the indexes
     * of the dependencies are collected as well.
     *
     * @param roundEnv the current annotation processing round environment
     */
    public void populate(RoundEnvironment roundEnv) {
        if (!indexesRead) {
            indexesRead = true;
            readIndexes(roundEnv);
        }

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
                case GLOBAL -> {
                    globalMixins.add(typeElement);
                    localGlobalMixins.add(typeElement);
                }
            }
        }
    }

    /**
     * Writes the index of the global mixins declared by the current compilation to its
     * output, so that they also apply to the compilations that depend on it.
     *
     * <p>Nothing is written if the compilation declares no global mixin and its output
     * had no index. This method should be invoked once, in the last processing round.
     */
    public void writeIndex() {
        if (localGlobalMixins.isEmpty() && !outputIndexed) {
            return;
        }

        try {
            var resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", INDEX_PATH);
            try (var writer = new BufferedWriter(new OutputStreamWriter(resource.openOutputStream(), StandardCharsets.UTF_8))) {
                for (var mixin : localGlobalMixins) {
                    writer.write(mixin.getQualifiedName().toString());
                    writer.newLine();
                }
            }
        } catch (IOException exception) {
            logs.printWarning(DaedalusMixinErrors.unwritableIndex(INDEX_PATH, localGlobalMixins, exception), null);
        }
    }

    /**
     * Collects the global mixins listed by the indexes of the output of the current
     * compilation, and of the entries of its class path and module path.
     *
     * <p>If the locations of the compilation cannot be accessed, a warning is printed and
     * only the global mixins declared by the current compilation apply.
     *
     * @param roundEnv the first annotation processing round environment
     */
    private void readIndexes(RoundEnvironment roundEnv) {
        List<Path> outputs;
        List<Path> dependencies;
        try {
            outputs = classpath.getPaths(StandardLocation.CLASS_OUTPUT);
            dependencies = new ArrayList<>(classpath.getPaths(StandardLocation.CLASS_PATH));
            for (var modulePathEntry : classpath.getPaths(StandardLocation.MODULE_PATH)) {
                dependencies.addAll(expandModulePathEntry(modulePathEntry));
            }
        } catch (DaedalusProcessorClasspathException exception) {
            logs.printWarning(DaedalusMixinErrors.unreadableClasspath(exception), null);
            return;
        }

        var readableModules = getReadableModules(roundEnv);
        for (var output : outputs) {
            var names = readIndex(output);
            if (names == null) {
                continue;
            }

            outputIndexed = true;
            for (var name : names) {
                var mixin = resolveIndexedMixin(name, readableModules);
                if (mixin != null) {
                    globalMixins.add(mixin);
                    localGlobalMixins.add(mixin);
                }
            }
        }

        for (var dependency : dependencies) {
            if (outputs.contains(dependency)) {
                continue;
            }

            var names = readIndex(dependency);
            if (names == null) {
                continue;
            }

            for (var name : names) {
                var mixin = resolveIndexedMixin(name, readableModules);
                if (mixin != null) {
                    globalMixins.add(mixin);
                }
            }
        }
    }

    /**
     * Returns the modules contained by an entry of the module path.
     *
     * <p>An entry of the module path is either a module, packaged as a jar or exploded in
     * a directory, or a directory that contains modules.
     *
     * @param entry the entry of the module path
     * @return the modules of the entry
     */
    private List<Path> expandModulePathEntry(Path entry) {
        if (!Files.isDirectory(entry) || Files.exists(entry.resolve("module-info.class"))) {
            return List.of(entry);
        }

        try (var children = Files.list(entry)) {
            return children.sorted().toList();
        } catch (IOException exception) {
            return List.of(entry);
        }
    }

    /**
     * Reads the index of the given entry of a location, which is either a directory or
     * an archive.
     *
     * @param entry the entry
     * @return the canonical names listed by the index, or {@code null} if the entry has no index
     */
    private List<String> readIndex(Path entry) {
        try {
            if (Files.isDirectory(entry)) {
                var index = entry.resolve(INDEX_PATH);
                if (!Files.isRegularFile(index)) {
                    return null;
                }

                try (var input = Files.newInputStream(index)) {
                    return parseIndex(input);
                }
            }

            if (!Files.isRegularFile(entry)) {
                return null;
            }

            try (var archive = new ZipFile(entry.toFile())) {
                var index = archive.getEntry(INDEX_PATH);
                if (index == null) {
                    return null;
                }

                try (var input = archive.getInputStream(index)) {
                    return parseIndex(input);
                }
            }
        } catch (IOException exception) {
            logs.printWarning(DaedalusMixinErrors.unreadableIndex(entry, INDEX_PATH, exception), null);
            return null;
        }
    }

    /**
     * Parses the canonical names listed by an index, ignoring blank lines and lines
     * that start with {@code #}.
     *
     * @param input the content of the index
     * @return the canonical names
     * @throws IOException if the index cannot be read
     */
    private List<String> parseIndex(InputStream input) throws IOException {
        var results = new ArrayList<String>();
        for (var line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
            var name = line.strip();
            if (!name.isEmpty() && !name.startsWith("#")) {
                results.add(name);
            }
        }
        return results;
    }

    /**
     * Resolves a mixin listed by an index.
     *
     * <p>The mixin is ignored if it cannot be found, if it is no longer a global mixin, for
     * example because the index is stale, or if the module being compiled cannot read it,
     * as the generated code could not use it.
     *
     * @param name the canonical name of the mixin
     * @param readableModules the modules that the module being compiled can read, or
     *        {@code null} if every type is readable
     * @return the mixin, or {@code null} if it should be ignored
     */
    private TypeElement resolveIndexedMixin(String name, Set<ModuleElement> readableModules) {
        var elements = processingEnv.getElementUtils();
        var mixin = elements.getTypeElement(name);
        if (mixin == null) {
            mixin = elements.getAllTypeElements(name)
                    .stream()
                    .filter(candidate -> readableModules == null || readableModules.contains(elements.getModuleOf(candidate)))
                    .findFirst()
                    .orElse(null);
        }

        if (mixin == null) {
            return null;
        }

        if (readableModules != null && !readableModules.contains(elements.getModuleOf(mixin))) {
            return null;
        }

        var annotation = mixin.getAnnotation(TypeMixin.class);
        if (annotation == null || annotation.scope() != TypeMixin.Scope.GLOBAL) {
            return null;
        }

        return mixin;
    }

    /**
     * Returns the modules that the module being compiled can read.
     *
     * <p>A named module reads itself, {@code java.base}, the modules it requires, and the
     * modules they require transitively. An automatic module reads every other automatic
     * module. Code in the unnamed module can read every type.
     *
     * @param roundEnv the first annotation processing round environment
     * @return the readable modules, or {@code null} if the code being compiled is not in a
     *         named module
     */
    private Set<ModuleElement> getReadableModules(RoundEnvironment roundEnv) {
        var elements = processingEnv.getElementUtils();
        ModuleElement currentModule = null;
        for (var rootElement : roundEnv.getRootElements()) {
            currentModule = elements.getModuleOf(rootElement);
            if (currentModule != null) {
                break;
            }
        }

        if (currentModule == null || currentModule.isUnnamed()) {
            return null;
        }

        var results = new HashSet<ModuleElement>();
        results.add(currentModule);
        var baseModule = elements.getModuleElement("java.base");
        if (baseModule != null) {
            results.add(baseModule);
        }

        var queue = new ArrayDeque<ModuleElement>();
        for (var directive : currentModule.getDirectives()) {
            if (directive instanceof ModuleElement.RequiresDirective requires && results.add(requires.getDependency())) {
                queue.add(requires.getDependency());
            }
        }

        var automaticModulesAdded = false;
        while (!queue.isEmpty()) {
            var module = queue.poll();
            if (!automaticModulesAdded && elements.isAutomaticModule(module)) {
                automaticModulesAdded = true;
                for (var candidate : elements.getAllModuleElements()) {
                    if (elements.isAutomaticModule(candidate) && results.add(candidate)) {
                        queue.add(candidate);
                    }
                }
            }

            for (var directive : module.getDirectives()) {
                if (directive instanceof ModuleElement.RequiresDirective requires && requires.isTransitive() && results.add(requires.getDependency())) {
                    queue.add(requires.getDependency());
                }
            }
        }

        return results;
    }

    /**
     * Returns the mixins applicable to the given target type.
     *
     * <p>The explicitly referenced mixins apply through {@link TypeMixin.Scope#MANUAL}, and
     * the mixins scoped to the package of the target, to its module, and globally apply
     * through {@link TypeMixin.Scope#PACKAGE}, {@link TypeMixin.Scope#MODULE} and
     * {@link TypeMixin.Scope#GLOBAL}. The mixins transitively imported by a mixin apply
     * through its scope, and a mixin that applies through more than one scope keeps the
     * narrowest one, which decides its precedence: a conversion only uses the mixins of a
     * broader scope when no conversion exists with the narrower ones, so that an explicit
     * mixin always wins over a scoped one.
     *
     * @param target the type element to find applicable mixins for
     * @param explicitMixins the explicitly referenced mixin types
     * @return the applicable mixins, each with the narrowest scope through which it applies
     */
    public DaedalusMixins getApplicableMixins(TypeElement target, Set<TypeElement> explicitMixins) {
        var builder = DaedalusMixins.builder();
        addScope(builder, explicitMixins, TypeMixin.Scope.MANUAL);
        addScope(builder, packageMixins.get(types.getPackageName(target)), TypeMixin.Scope.PACKAGE);
        addScope(builder, moduleMixins.get(types.getModuleName(target)), TypeMixin.Scope.MODULE);
        addScope(builder, globalMixins, TypeMixin.Scope.GLOBAL);
        return builder.build();
    }

    /**
     * Adds the given mixins, and the mixins that they transitively import, to the given
     * builder, through the given scope.
     *
     * @param builder the builder of the mixins
     * @param mixins the mixins of the scope, or {@code null} if the scope has none
     * @param scope the scope through which the mixins apply
     */
    private void addScope(DaedalusMixins.Builder builder, Set<TypeElement> mixins, TypeMixin.Scope scope) {
        if (mixins == null) {
            return;
        }

        var resolved = new LinkedHashSet<>(mixins);
        resolveImports(resolved);
        builder.addAll(resolved, scope);
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
