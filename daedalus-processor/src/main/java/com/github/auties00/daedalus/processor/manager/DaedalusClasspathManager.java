package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.processor.exception.DaedalusProcessorClasspathException;
import com.github.auties00.daedalus.processor.util.DaedalusProcessingEnvironmentUtils;
import com.github.auties00.daedalus.typesystem.exception.TypeReflectionException;
import com.github.auties00.daedalus.typesystem.util.ReflectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

/**
 * A resolver for the locations of the current compilation, such as its class path and
 * its module path.
 *
 * <p>The annotation processing API does not expose these locations, but every compiler
 * knows them, so they are read from the internals of the processing environment created by
 * the compiler, as described by {@link CompilationLocations}. That environment is found by
 * {@link DaedalusProcessingEnvironmentUtils#findCompilerProcessingEnvironment(ProcessingEnvironment)}
 * before this resolver is created, as build tools and IDEs often wrap it. The supported
 * compilers are javac, including when it is run by Maven, Gradle, IntelliJ IDEA or kapt,
 * the batch Eclipse compiler, and the compiler of the Eclipse IDE.
 *
 * <p>The locations are resolved once, the first time a location is requested. A resolver
 * created by {@link #unavailable(DaedalusProcessorClasspathException)} throws the exception it was
 * created with instead.
 */
public final class DaedalusClasspathManager {
    /**
     * The processing environment created by the compiler, or {@code null} if the locations
     * of the compilation cannot be read.
     */
    private final ProcessingEnvironment compilerProcessingEnv;

    /**
     * The exception that explains why the locations of the compilation cannot be read, or
     * {@code null} if they can.
     */
    private final DaedalusProcessorClasspathException unavailability;

    /**
     * The locations of the compilation, or {@code null} if they were not resolved yet.
     */
    private CompilationLocations locations;

    /**
     * Constructs a new resolver for the compilation of the given processing environment,
     * which must be the one created by the compiler, not a wrapper of it.
     *
     * @param compilerProcessingEnv the processing environment created by javac or by the
     *        Eclipse compiler
     */
    public DaedalusClasspathManager(ProcessingEnvironment compilerProcessingEnv) {
        this(compilerProcessingEnv, null);
    }

    /**
     * Constructs a new resolver from either a processing environment created by the
     * compiler or the exception that explains why there is none.
     *
     * @param compilerProcessingEnv the processing environment created by the compiler, or
     *        {@code null}
     * @param unavailability the exception thrown by every request, or {@code null}
     */
    private DaedalusClasspathManager(ProcessingEnvironment compilerProcessingEnv, DaedalusProcessorClasspathException unavailability) {
        this.compilerProcessingEnv = compilerProcessingEnv;
        this.unavailability = unavailability;
    }

    /**
     * Returns a resolver that cannot read the locations of the compilation, for example
     * because the processing environment created by the compiler cannot be found.
     *
     * @param unavailability the exception thrown by every request for a location, which
     *        explains why the locations cannot be read
     * @return a resolver whose {@link #getPaths(JavaFileManager.Location)} method always
     *         throws {@code unavailability}
     */
    public static DaedalusClasspathManager unavailable(DaedalusProcessorClasspathException unavailability) {
        return new DaedalusClasspathManager(null, unavailability);
    }

    /**
     * Returns the paths of the given location of the current compilation.
     *
     * <p>For example, {@link StandardLocation#CLASS_PATH} returns the jars and the
     * directories on the class path, and {@link StandardLocation#MODULE_PATH} returns the
     * modules and the directories of modules on the module path.
     *
     * @param location the location
     * @return the paths of the location, or an empty list if the compilation does not use it
     * @throws DaedalusProcessorClasspathException if the locations of the compilation cannot be read
     */
    public List<Path> getPaths(JavaFileManager.Location location) throws DaedalusProcessorClasspathException {
        if (unavailability != null) {
            throw unavailability;
        }

        if (locations == null) {
            locations = CompilationLocations.of(compilerProcessingEnv);
        }
        return locations.getPaths(location);
    }

    /**
     * The locations of a compilation, such as its class output, its class path and its
     * module path, as known by the compiler that runs it.
     *
     * <p>The annotation processing API does not expose these locations, so each subtype
     * reads them from a different part of the internals of a compiler:
     * <ul>
     * <li>{@link FileManagerLocations} reads them from the file manager of javac, which is
     *     kept in the compiler context of its processing environment, in the encapsulated
     *     {@code jdk.compiler} module, or from the file manager of the batch Eclipse
     *     compiler, which build tools invoke, that is exposed by its processing environment;
     * <li>{@link EclipseProjectLocations} computes them from the Java project that is
     *     being built by the compiler of the Eclipse IDE, which has no file manager, but
     *     exposes the project through its processing environment.
     * </ul>
     * The subtype that applies to a compilation is chosen by {@link #of(ProcessingEnvironment)}.
     */
    private abstract static sealed class CompilationLocations {
        /**
         * The name of the module that contains javac.
         */
        private static final String JAVAC_MODULE = "jdk.compiler";

        /**
         * The packages of javac that contain its processing environment, its compiler context
         * and the file manager it installs when compiling with {@code --release}.
         */
        private static final String[] JAVAC_PACKAGES = {
                "com.sun.tools.javac.main",
                "com.sun.tools.javac.processing",
                "com.sun.tools.javac.util"
        };

        /**
         * Returns the locations of the compilation of the given processing environment created
         * by the compiler, read from its internals.
         *
         * <p>The locations are read from javac, as described by {@link #ofJavac(Object)}, or
         * from the Eclipse compiler, as described by {@link #ofEclipse(Object)}.
         *
         * @param compilerProcessingEnv the processing environment created by the compiler
         * @return the locations of the compilation
         * @throws DaedalusProcessorClasspathException if the environment was not created by a supported
         *         compiler, or if its internals cannot be accessed
         */
        static CompilationLocations of(ProcessingEnvironment compilerProcessingEnv) throws DaedalusProcessorClasspathException {
            try {
                if (DaedalusProcessingEnvironmentUtils.isJavacProcessingEnvironment(compilerProcessingEnv)) {
                    return ofJavac(compilerProcessingEnv);
                }

                if (DaedalusProcessingEnvironmentUtils.isEclipseProcessingEnvironment(compilerProcessingEnv)) {
                    return ofEclipse(compilerProcessingEnv);
                }

                throw DaedalusProcessorClasspathException.unsupportedCompiler(compilerProcessingEnv.getClass());
            } catch (TypeReflectionException exception) {
                throw DaedalusProcessorClasspathException.inaccessibleCompiler(compilerProcessingEnv.getClass(), exception);
            }
        }

        /**
         * Returns the locations of a compilation of javac, listed by the file manager in its
         * compiler context.
         *
         * @param javacProcessingEnv the processing environment created by javac
         * @return the locations listed by the file manager of javac
         * @throws TypeReflectionException if the internals of javac cannot be accessed
         * @throws DaedalusProcessorClasspathException if the file manager cannot list its locations
         */
        private static CompilationLocations ofJavac(Object javacProcessingEnv) throws TypeReflectionException, DaedalusProcessorClasspathException {
            ReflectionUtils.openPackages(JAVAC_MODULE, JAVAC_PACKAGES);
            var context = ReflectionUtils.invokePublicMethod(javacProcessingEnv, Object.class, "getContext");
            var fileManager = ReflectionUtils.invokePublicMethod(context, JavaFileManager.class, "get", JavaFileManager.class);
            return FileManagerLocations.of(fileManager);
        }

        /**
         * Returns the locations of a compilation of the Eclipse compiler, listed by its file
         * manager if it is the batch compiler, or from the Java project that is being built
         * if it is the compiler of the Eclipse IDE.
         *
         * @param eclipseProcessingEnv the processing environment created by the Eclipse compiler
         * @return the locations of the compilation
         * @throws TypeReflectionException if the internals of the Eclipse compiler cannot
         *         be accessed
         * @throws DaedalusProcessorClasspathException if the processing environment exposes neither a
         *         file manager nor a Java project, or if the file manager cannot list its
         *         locations
         */
        private static CompilationLocations ofEclipse(Object eclipseProcessingEnv) throws TypeReflectionException, DaedalusProcessorClasspathException {
            var fileManager = ReflectionUtils.invokePublicMethod(eclipseProcessingEnv, JavaFileManager.class, "getFileManager");
            if (fileManager != null) {
                return FileManagerLocations.of(fileManager);
            }

            if (ReflectionUtils.findPublicMethod(eclipseProcessingEnv, "getJavaProject").isPresent()) {
                var javaProject = ReflectionUtils.invokePublicMethod(eclipseProcessingEnv, Object.class, "getJavaProject");
                if (javaProject != null) {
                    return new EclipseProjectLocations(javaProject);
                }
            }

            throw DaedalusProcessorClasspathException.unsupportedEclipseCompiler(eclipseProcessingEnv.getClass());
        }

        /**
         * Returns the paths of the given location.
         *
         * @param location the location
         * @return the paths of the location, or an empty list if the compilation does not use it
         */
        abstract List<Path> getPaths(JavaFileManager.Location location);

        /**
         * The locations of a compilation that are listed by its file manager, as is the case for
         * javac and for the batch Eclipse compiler.
         *
         * <p>Only a {@link StandardJavaFileManager} can list the paths of a location, so the file
         * manager of the compilation is unwrapped, if the compiler or a build tool delegates it,
         * until every standard file manager it delegates to is found. For example, javac delegates
         * the platform locations and the other locations to two different file managers when
         * compiling with {@code --release}. The paths of a location are the union of the paths
         * listed by each of them.
         */
        private static final class FileManagerLocations extends CompilationLocations {
            /**
             * The standard file managers that the file manager of the compilation is, or delegates to.
             */
            private final List<StandardJavaFileManager> fileManagers;

            /**
             * Constructs the locations listed by the given standard file managers.
             *
             * @param fileManagers the standard file managers
             */
            private FileManagerLocations(List<StandardJavaFileManager> fileManagers) {
                this.fileManagers = fileManagers;
            }

            /**
             * Returns the locations listed by the given file manager of a compilation.
             *
             * @param fileManager the file manager of the compilation
             * @return the locations listed by the file manager
             * @throws TypeReflectionException if a file manager that the given one delegates
             *         to cannot be read
             * @throws DaedalusProcessorClasspathException if the file manager does not delegate to any
             *         standard file manager
             */
            private static FileManagerLocations of(JavaFileManager fileManager) throws TypeReflectionException, DaedalusProcessorClasspathException {
                var fileManagers = new ArrayList<StandardJavaFileManager>();
                collectStandardFileManagers(fileManager, fileManagers, Collections.newSetFromMap(new IdentityHashMap<>()));
                if (fileManagers.isEmpty()) {
                    throw DaedalusProcessorClasspathException.unlistableFileManager(fileManager.getClass());
                }

                return new FileManagerLocations(List.copyOf(fileManagers));
            }

            /**
             * Collects the standard file managers that the given file manager is, or delegates to.
             *
             * <p>A standard file manager is collected without being inspected further. Any other
             * file manager, such as a {@link ForwardingJavaFileManager} or the file manager that
             * javac installs when compiling with {@code --release}, is inspected for fields whose
             * value is a file manager.
             *
             * @param fileManager the file manager to inspect, or {@code null}
             * @param results the collected standard file managers
             * @param visited the file managers that were already inspected, to avoid cycles
             * @throws TypeReflectionException if a field of a file manager cannot be read
             */
            private static void collectStandardFileManagers(JavaFileManager fileManager, List<StandardJavaFileManager> results, Set<Object> visited) throws TypeReflectionException {
                if (fileManager == null || !visited.add(fileManager)) {
                    return;
                }

                if (fileManager instanceof StandardJavaFileManager standardFileManager) {
                    results.add(standardFileManager);
                    return;
                }

                for (Class<?> type = fileManager.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                    for (var field : type.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && JavaFileManager.class.isAssignableFrom(field.getType())) {
                            collectStandardFileManagers((JavaFileManager) ReflectionUtils.readField(fileManager, field), results, visited);
                        }
                    }
                }
            }

            @Override
            List<Path> getPaths(JavaFileManager.Location location) {
                var results = new LinkedHashSet<Path>();
                for (var fileManager : fileManagers) {
                    if (!fileManager.hasLocation(location)) {
                        continue;
                    }

                    var paths = fileManager.getLocationAsPaths(location);
                    if (paths == null) {
                        continue;
                    }

                    for (var path : paths) {
                        results.add(path);
                    }
                }
                return List.copyOf(results);
            }
        }

        /**
         * The locations of a compilation of the Eclipse IDE, computed from the resolved class path
         * of the Java project that is being built, as the compiler of the Eclipse IDE has no file
         * manager.
         *
         * <p>The locations are computed as the compiler of the Eclipse IDE does:
         * <ul>
         * <li>the class output is made of the default output folder of the project and of the
         *     output folders of its source folders;
         * <li>a library is on the module path if it has the {@code module} attribute, and on the
         *     class path otherwise;
         * <li>a required project contributes its output folders, and the libraries and the
         *     projects that it exports, recursively.
         * </ul>
         *
         * <p>The paths of a Java project are relative to the workspace, unless they point outside
         * of it, so they are resolved against the workspace first. The classes of the Eclipse IDE
         * are not visible to the processor, so they are accessed reflectively.
         */
        private static final class EclipseProjectLocations extends CompilationLocations {
            /**
             * The kind of a class path entry that is a library, as defined by
             * {@code IClasspathEntry.CPE_LIBRARY}.
             */
            private static final int LIBRARY_ENTRY = 1;

            /**
             * The kind of a class path entry that is a required project, as defined by
             * {@code IClasspathEntry.CPE_PROJECT}.
             */
            private static final int PROJECT_ENTRY = 2;

            /**
             * The kind of a class path entry that is a source folder, as defined by
             * {@code IClasspathEntry.CPE_SOURCE}.
             */
            private static final int SOURCE_ENTRY = 3;

            /**
             * The name of the attribute of a class path entry that places it on the module path,
             * as defined by {@code IClasspathAttribute.MODULE}.
             */
            private static final String MODULE_ATTRIBUTE = "module";

            /**
             * The {@code IWorkspaceRoot} that contains the projects.
             */
            private final Object workspaceRoot;

            /**
             * The {@code IJavaModel} that contains the Java projects.
             */
            private final Object javaModel;

            /**
             * The names of the projects whose locations were already added, to avoid cycles.
             */
            private final Set<String> visitedProjects;

            private final Set<Path> classOutput;
            private final Set<Path> classPath;
            private final Set<Path> modulePath;

            /**
             * Constructs the locations of the given Java project.
             *
             * @param javaProject the {@code IJavaProject} that is being built
             * @throws TypeReflectionException if the class path of the project cannot be resolved
             */
            private EclipseProjectLocations(Object javaProject) throws TypeReflectionException {
                var project = ReflectionUtils.invokePublicMethod(javaProject, Object.class, "getProject");
                var workspace = ReflectionUtils.invokePublicMethod(project, Object.class, "getWorkspace");
                this.workspaceRoot = ReflectionUtils.invokePublicMethod(workspace, Object.class, "getRoot");
                this.javaModel = ReflectionUtils.invokePublicMethod(javaProject, Object.class, "getJavaModel");
                this.visitedProjects = new HashSet<>();
                this.classOutput = new LinkedHashSet<>();
                this.classPath = new LinkedHashSet<>();
                this.modulePath = new LinkedHashSet<>();
                visitedProjects.add(ReflectionUtils.invokePublicMethod(javaProject, String.class, "getElementName"));
                addOutputFolders(javaProject, classOutput);
                addDependencies(javaProject, false);
            }

            @Override
            List<Path> getPaths(JavaFileManager.Location location) {
                if (location == StandardLocation.CLASS_OUTPUT) {
                    return List.copyOf(classOutput);
                }

                if (location == StandardLocation.CLASS_PATH) {
                    return List.copyOf(classPath);
                }

                if (location == StandardLocation.MODULE_PATH) {
                    return List.copyOf(modulePath);
                }

                return List.of();
            }

            /**
             * Adds the output folders of the given Java project.
             *
             * @param javaProject the {@code IJavaProject}
             * @param results the paths to add to
             * @throws TypeReflectionException if the project cannot be read
             */
            private void addOutputFolders(Object javaProject, Set<Path> results) throws TypeReflectionException {
                addWorkspacePath(results, ReflectionUtils.invokePublicMethod(javaProject, Object.class, "getOutputLocation"));
                for (var entry : getResolvedClasspath(javaProject)) {
                    if (getEntryKind(entry) == SOURCE_ENTRY) {
                        addWorkspacePath(results, ReflectionUtils.invokePublicMethod(entry, Object.class, "getOutputLocation"));
                    }
                }
            }

            /**
             * Adds the libraries and the required projects of the given Java project to the class
             * path or to the module path.
             *
             * @param javaProject the {@code IJavaProject}
             * @param exportedOnly whether only the entries exported by the project are added, as
             *        is the case for a required project
             * @throws TypeReflectionException if the project cannot be read
             */
            private void addDependencies(Object javaProject, boolean exportedOnly) throws TypeReflectionException {
                for (var entry : getResolvedClasspath(javaProject)) {
                    if (exportedOnly && !ReflectionUtils.invokePublicMethod(entry, Boolean.class, "isExported")) {
                        continue;
                    }

                    var target = isOnModulePath(entry) ? modulePath : classPath;
                    switch (getEntryKind(entry)) {
                        case LIBRARY_ENTRY -> addWorkspacePath(target, ReflectionUtils.invokePublicMethod(entry, Object.class, "getPath"));
                        case PROJECT_ENTRY -> addRequiredProject(entry, target);
                        default -> {}
                    }
                }
            }

            /**
             * Adds the output folders and the exported entries of the project required by the
             * given class path entry, unless it was already added, or it is missing or closed.
             *
             * @param entry the {@code IClasspathEntry} of the required project
             * @param target the class path or the module path, depending on the entry
             * @throws TypeReflectionException if the project cannot be read
             */
            private void addRequiredProject(Object entry, Set<Path> target) throws TypeReflectionException {
                var path = ReflectionUtils.invokePublicMethod(entry, Object.class, "getPath");
                var projectName = ReflectionUtils.invokePublicMethod(path, String.class, "lastSegment");
                if (projectName == null || !visitedProjects.add(projectName)) {
                    return;
                }

                var requiredProject = ReflectionUtils.invokePublicMethod(javaModel, Object.class, "getJavaProject", projectName);
                if (!ReflectionUtils.invokePublicMethod(requiredProject, Boolean.class, "exists")
                        || !ReflectionUtils.invokePublicMethod(requiredProject, Boolean.class, "isOpen")) {
                    return;
                }

                addOutputFolders(requiredProject, target);
                addDependencies(requiredProject, true);
            }

            /**
             * Returns the resolved class path of the given Java project, where variables and
             * containers are replaced by the entries they stand for.
             *
             * @param javaProject the {@code IJavaProject}
             * @return the {@code IClasspathEntry} instances of the resolved class path
             * @throws TypeReflectionException if the class path cannot be resolved
             */
            private Object[] getResolvedClasspath(Object javaProject) throws TypeReflectionException {
                return ReflectionUtils.invokePublicMethod(javaProject, Object[].class, "getResolvedClasspath", true);
            }

            /**
             * Returns the kind of the given class path entry.
             *
             * @param entry the {@code IClasspathEntry}
             * @return the kind of the entry
             * @throws TypeReflectionException if the entry cannot be read
             */
            private int getEntryKind(Object entry) throws TypeReflectionException {
                return ReflectionUtils.invokePublicMethod(entry, Integer.class, "getEntryKind");
            }

            /**
             * Returns whether the given class path entry is on the module path.
             *
             * @param entry the {@code IClasspathEntry}
             * @return {@code true} if the entry has the {@code module} attribute set to {@code true}
             * @throws TypeReflectionException if the entry cannot be read
             */
            private boolean isOnModulePath(Object entry) throws TypeReflectionException {
                for (var attribute : ReflectionUtils.invokePublicMethod(entry, Object[].class, "getExtraAttributes")) {
                    if (MODULE_ATTRIBUTE.equals(ReflectionUtils.invokePublicMethod(attribute, String.class, "getName"))
                            && "true".equals(ReflectionUtils.invokePublicMethod(attribute, String.class, "getValue"))) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * Resolves the given path of a Java project to a path of the file system, and adds it.
             *
             * <p>A path whose first segment is the name of a project of the workspace is resolved
             * against the location of that project, even if the resource it points to does not
             * exist yet, as is the case for an output folder before the first build. Any other
             * path is a path of the file system. A path that cannot be resolved, for example
             * because it points to a virtual resource, is not added.
             *
             * @param results the paths to add to
             * @param path the {@code IPath}, or {@code null}
             * @throws TypeReflectionException if the workspace cannot be read
             */
            private void addWorkspacePath(Set<Path> results, Object path) throws TypeReflectionException {
                if (path == null) {
                    return;
                }

                Object location = path;
                var resource = ReflectionUtils.invokePublicMethod(workspaceRoot, Object.class, "findMember", path);
                if (resource != null) {
                    location = ReflectionUtils.invokePublicMethod(resource, Object.class, "getLocation");
                } else if (ReflectionUtils.invokePublicMethod(path, String.class, "getDevice") == null) {
                    int segmentCount = ReflectionUtils.invokePublicMethod(path, Integer.class, "segmentCount");
                    if (segmentCount > 0) {
                        var projectName = ReflectionUtils.invokePublicMethod(path, String.class, "segment", 0);
                        var project = ReflectionUtils.invokePublicMethod(workspaceRoot, Object.class, "getProject", projectName);
                        if (ReflectionUtils.invokePublicMethod(project, Boolean.class, "exists")) {
                            var container = segmentCount == 1 ? project : ReflectionUtils.invokePublicMethod(workspaceRoot, Object.class, "getFolder", path);
                            location = ReflectionUtils.invokePublicMethod(container, Object.class, "getLocation");
                        }
                    }
                }

                if (location != null) {
                    results.add(ReflectionUtils.invokePublicMethod(location, File.class, "toFile").toPath());
                }
            }
        }
    }
}
