package com.github.auties00.daedalus.processor.generator;

import com.palantir.javapoet.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An abstract base class for method generators that create individual methods in Spec
 * classes using JavaPoet.
 *
 * <p>Provides method signature construction, Spec class name resolution, deferred
 * operations, and type utilities. Subclasses implement the abstract hooks to define
 * the method's name, parameters, return type, modifiers, and body.
 */
public abstract class DaedalusMethodGenerator {
    private static final ConcurrentMap<String, String> SPECS_CACHE = new ConcurrentHashMap<>();

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
        if (!(typeMirror instanceof DeclaredType declaredType)
                || !(declaredType.asElement() instanceof TypeElement element)) {
            return "";
        }

        return SPECS_CACHE.computeIfAbsent(element.getQualifiedName().toString(), _ -> {
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
            if (packageName != null) {
                result.append(packageName);
                result.append(".");
            }
            result.append(name);
            result.append("Spec");
            return result.toString();
        });
    }

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

        for (var i = 0; i < parametersTypes.size(); i++) {
            var paramType = parametersTypes.get(i);
            var paramName = parametersNames.get(i);
            methodBuilder.addParameter(ParameterSpec.builder(paramType, paramName).build());
        }

        doInstrumentation(classBuilder, methodBuilder);
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
