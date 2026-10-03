package com.github.auties00.daedalus.processor.generator;

import com.palantir.javapoet.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An abstract base class for method generators that create individual methods in classes.
 */
public abstract class DaedalusMethodGenerator {
    private static final ConcurrentMap<String, String> GENERATED_NAMES_CACHE = new ConcurrentHashMap<>();

    /**
     * Converts a type to its generated Spec class name.
     *
     * <p>For example:
     * <ul>
     * <li>{@code com.example.Message} produces {@code com.example.MessageSpec}
     * <li>{@code com.example.Outer.Inner} produces {@code com.example.OuterInnerSpec}
     * </ul>
     *
     * @param typeMirror the type to resolve
     * @return the fully qualified Spec class name, or an empty string if the type is not a declared type
     */
    public static String getSpecByType(TypeMirror typeMirror) {
        return getGeneratedClassByType(typeMirror, "Spec");
    }

    /**
     * Converts a type to the name of the builder class generated for it by its data format.
     *
     * <p>For example:
     * <ul>
     * <li>{@code com.example.Message} produces {@code com.example.MessageBuilder}
     * <li>{@code com.example.Outer.Inner} produces {@code com.example.OuterInnerBuilder}
     * </ul>
     *
     * @param typeMirror the type to resolve
     * @return the fully qualified builder class name, or an empty string if the type is not a declared type
     */
    public static String getBuilderByType(TypeMirror typeMirror) {
        return getGeneratedClassByType(typeMirror, "Builder");
    }

    /**
     * Converts a type to the name of a class generated for it, which is placed in the package
     * of the type and named after the type and its enclosing types, followed by the given
     * suffix.
     *
     * @param typeMirror the type to resolve
     * @param suffix the suffix of the generated class name
     * @return the fully qualified generated class name, or an empty string if the type is not a declared type
     */
    private static String getGeneratedClassByType(TypeMirror typeMirror, String suffix) {
        if (!(typeMirror instanceof DeclaredType declaredType)
                || !(declaredType.asElement() instanceof TypeElement element)) {
            return "";
        }

        return GENERATED_NAMES_CACHE.computeIfAbsent(element.getQualifiedName() + "#" + suffix, _ -> {
            var parent = element.getEnclosingElement();
            String packageName = null;
            var name = new StringBuilder();
            while (parent != null) {
                if (parent instanceof TypeElement typeElement) {
                    name.append(typeElement.getSimpleName());
                } else if (parent instanceof PackageElement packageElement) {
                    packageName = packageElement.getQualifiedName().toString();
                    break;
                }

                parent = parent.getEnclosingElement();
            }

            name.append(declaredType.asElement().getSimpleName());

            var result = new StringBuilder();
            if (packageName != null && !packageName.isEmpty()) {
                result.append(packageName);
                result.append(".");
            }
            result.append(name);
            result.append(suffix);
            return result.toString();
        });
    }

    /**
     * The name of the variable of the {@code catch} block that holds an exception wrapped by
     * the statement returned by {@link #wrapperStatement()}.
     */
    protected static final String WRAPPED_EXCEPTION = "protoWrappedException";

    /**
     * The list of deferred operations to execute after the main instrumentation.
     */
    protected final List<Runnable> deferredOperations;

    /**
     * Constructs a new method generator.
     */
    protected DaedalusMethodGenerator() {
        this.deferredOperations = new ArrayList<>();
    }

    /**
     * Generates the method and adds it to the given class builder.
     *
     * <p>If {@link #shouldInstrument()} returns {@code false}, no method is generated.
     * After the main instrumentation, all deferred operations are executed in rounds
     * until none remain.
     *
     * @param classBuilder the class builder to add the generated method to
     */
    public void generate(TypeSpec.Builder classBuilder) {
        if (!shouldInstrument()) {
            return;
        }

        var parametersTypes = parametersTypes();
        var parametersNames = parametersNames();
        if (parametersTypes.size() != parametersNames.size()) {
            throw new IllegalArgumentException("Parameters mismatch");
        }

        var methodBuilder = MethodSpec.methodBuilder(name());
        methodBuilder.addModifiers(modifiers().toArray(new Modifier[0]));
        methodBuilder.returns(returnType());

        for (var annotation : annotations()) {
            methodBuilder.addAnnotation(annotation);
        }

        for (var i = 0; i < parametersTypes.size(); i++) {
            var paramType = parametersTypes.get(i);
            var paramName = parametersNames.get(i);
            methodBuilder.addParameter(ParameterSpec.builder(paramType, paramName).build());
        }

        instrumentWrapped(methodBuilder, () -> doInstrumentation(classBuilder, methodBuilder));
        methodBuilder.addExceptions(thrownTypes());
        classBuilder.addMethod(methodBuilder.build());

        while (!deferredOperations.isEmpty()) {
            List<Runnable> round = new ArrayList<>(deferredOperations);
            deferredOperations.clear();
            for (var runnable : round) {
                runnable.run();
            }
        }
    }

    /**
     * Returns whether this method should be generated.
     *
     * @return {@code true} if the method should be generated
     */
    public abstract boolean shouldInstrument();

    /**
     * Performs the main instrumentation by adding statements to the method builder.
     *
     * @param classBuilder the class builder (for adding helper methods or inner classes)
     * @param methodBuilder the method builder to add statements to
     */
    protected abstract void doInstrumentation(TypeSpec.Builder classBuilder, MethodSpec.Builder methodBuilder);

    /**
     * Returns the modifiers for the generated method.
     *
     * @return the method modifiers
     */
    protected abstract List<Modifier> modifiers();

    /**
     * Returns the return type of the generated method.
     *
     * @return the return type
     */
    protected abstract TypeName returnType();

    /**
     * Returns the name of the generated method.
     *
     * @return the method name
     */
    protected abstract String name();

    /**
     * Returns the parameter types of the generated method.
     *
     * @return the parameter types
     */
    protected abstract List<TypeName> parametersTypes();

    /**
     * Returns the parameter names of the generated method.
     *
     * @return the parameter names
     */
    protected abstract List<String> parametersNames();

    /**
     * Returns the annotations to apply to the generated method.
     *
     * <p>The default implementation returns an empty list. Subclasses may override
     * to attach method-level annotations (for example, {@code @TypeSerializer}).
     *
     * @return the list of annotation specs to apply
     */
    public abstract List<AnnotationSpec> annotations();

    /**
     * Returns the checked exceptions that the generated method declares in its {@code throws}
     * clause.
     *
     * @implSpec
     * The default implementation returns an empty list. Subclasses whose generated method
     * invokes methods that throw checked exceptions, including the converters applied to its
     * fields, must override this method.
     *
     * @return the checked exceptions thrown by the generated method
     */
    protected List<TypeName> thrownTypes() {
        return List.of();
    }

    /**
     * Returns the statement that rethrows a checked exception caught while running the code
     * of the generated method, wrapped in one of the exceptions that the method declares.
     *
     * <p>When this method returns a statement, the code of the generated method is enclosed in
     * a {@code try} block, whose {@code catch} block catches any {@link Exception} as
     * {@value #WRAPPED_EXCEPTION} and runs the statement, so that the method declares a fixed
     * set of exceptions whatever the converters it invokes throw. The statement must rethrow
     * unchecked exceptions, and the exceptions that the method declares, unchanged.
     *
     * @implSpec
     * The default implementation returns an empty {@link Optional}, so the code is not
     * enclosed in a {@code try} block.
     *
     * @return the statement, or an empty {@link Optional} if no exception is wrapped
     */
    protected Optional<CodeBlock> wrapperStatement() {
        return Optional.empty();
    }

    /**
     * Runs the given instrumentation, which adds the code of a method to the given builder,
     * enclosing it in a {@code try} block that wraps the checked exceptions it throws if
     * {@link #wrapperStatement()} returns a statement.
     *
     * <p>This method is used for the main method of this generator, and can be used for the
     * helper methods that it adds to the class.
     *
     * @param methodBuilder the builder of the method
     * @param instrumentation the instrumentation that adds the code of the method
     */
    protected final void instrumentWrapped(MethodSpec.Builder methodBuilder, Runnable instrumentation) {
        var wrapper = wrapperStatement();
        if (wrapper.isEmpty()) {
            instrumentation.run();
            return;
        }

        methodBuilder.beginControlFlow("try");
        instrumentation.run();
        methodBuilder.nextControlFlow("catch ($T $L)", Exception.class, WRAPPED_EXCEPTION);
        methodBuilder.addStatement(wrapper.get());
        methodBuilder.endControlFlow();
    }

    /**
     * Returns a code expression that accesses the given element on the given object.
     *
     * <p>For fields, returns {@code "object.fieldName"}. For methods, returns
     * {@code "object.methodName()"}.
     *
     * @param object the object expression
     * @param accessor the element to access (field or method)
     * @return the accessor expression
     */
    protected String getAccessorCall(String object, Element accessor) {
        return switch (accessor) {
            case ExecutableElement executableElement -> "%s.%s()".formatted(object, executableElement.getSimpleName());
            case VariableElement variableElement -> "%s.%s".formatted(object, variableElement.getSimpleName());
            default -> throw new IllegalStateException("Unexpected value: " + accessor);
        };
    }

    /**
     * Returns the fully qualified name of the given type.
     *
     * @param type the type mirror
     * @return the fully qualified name
     */
    protected String getQualifiedName(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return type.toString();
        }

        if (!(declaredType.asElement() instanceof TypeElement typeElement)) {
            return declaredType.toString();
        }

        return typeElement.getQualifiedName().toString();
    }

    /**
     * Returns the simple name of the given type.
     *
     * @param type the type mirror
     * @return the simple name
     */
    protected String getSimpleName(TypeMirror type) {
        var parts = getQualifiedName(type).split("\\.");
        return parts[parts.length - 1].replaceAll("\\$", ".");
    }
}
