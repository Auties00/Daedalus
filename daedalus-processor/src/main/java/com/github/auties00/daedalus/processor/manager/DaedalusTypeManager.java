package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility for type manipulation during annotation processing.
 *
 * <p>This class provides thread-safe type comparison, erasure, assignability checking,
 * generic type resolution, and other type-related operations needed by the converter
 * graph and code generators.
 *
 * <p>Some operations in the standard {@link javax.lang.model.util.Types} utility are
 * not thread-safe. This class reimplements critical operations like
 * {@link #isAssignable(TypeMirror, TypeMirror)} to be safe for use in parallel streams.
 *
 * <p>This class is non-final so that format-specific subclasses can extend it with
 * additional type predicates (for example, checking whether a type is a protobuf
 * message or enum).
 */
public class DaedalusTypeManager {

    private final ProcessingEnvironment processingEnv;
    private final TypeMirror booleanType;
    private final TypeMirror byteType;
    private final TypeMirror shortType;
    private final TypeMirror intType;
    private final TypeMirror longType;
    private final TypeMirror charType;
    private final TypeMirror floatType;
    private final TypeMirror doubleType;
    private final TypeMirror wrappedBooleanType;
    private final TypeMirror wrappedByteType;
    private final TypeMirror wrappedShortType;
    private final TypeMirror wrappedIntType;
    private final TypeMirror wrappedLongType;
    private final TypeMirror wrappedCharType;
    private final TypeMirror wrappedFloatType;
    private final TypeMirror wrappedDoubleType;

    /**
     * Constructs a new type utility with the given processing environment.
     *
     * @param processingEnv the annotation processing environment
     */
    public DaedalusTypeManager(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.booleanType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
        this.byteType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.BYTE);
        this.shortType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.SHORT);
        this.intType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.INT);
        this.longType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.LONG);
        this.charType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.CHAR);
        this.floatType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.FLOAT);
        this.doubleType = processingEnv.getTypeUtils().getPrimitiveType(TypeKind.DOUBLE);
        this.wrappedBooleanType = getType(Boolean.class);
        this.wrappedByteType = getType(Byte.class);
        this.wrappedShortType = getType(Short.class);
        this.wrappedIntType = getType(Integer.class);
        this.wrappedLongType = getType(Long.class);
        this.wrappedCharType = getType(Character.class);
        this.wrappedFloatType = getType(Float.class);
        this.wrappedDoubleType = getType(Double.class);
    }

    /**
     * Returns whether the given type mirror represents a mixin type.
     *
     * <p>A type is considered a mixin if its erased form is a declared type
     * whose element is annotated with {@link TypeMixin}.
     *
     * @param mirror the type mirror to check
     * @return {@code true} if the type is a mixin
     */
    public boolean isMixin(TypeMirror mirror) {
        return erase(mirror) instanceof DeclaredType declaredType
               && declaredType.asElement().getAnnotation(TypeMixin.class) != null;
    }

    /**
     * Converts a Java reflection {@link Class} into an AST {@link TypeMirror}.
     *
     * <p>If the given type is a primitive, its corresponding primitive type mirror
     * is returned. If it is an array, an array type mirror is returned. Otherwise,
     * the type is looked up by its canonical name and optionally parameterized with
     * the given type arguments.
     *
     * @param type the Java class to convert, or {@code null}
     * @param params optional type parameter classes for parameterized types
     * @return the corresponding type mirror, or {@code null} if {@code type} is {@code null}
     */
    public TypeMirror getType(Class<?> type, Class<?>... params) {
        if (type == null) {
            return null;
        }

        if (type.isPrimitive()) {
            return getPrimitiveType(TypeKind.valueOf(type.getName().toUpperCase(Locale.ROOT)));
        }

        if (type.isArray()) {
            return processingEnv.getTypeUtils().getArrayType(getType(type.getComponentType()));
        }

        var result = processingEnv.getElementUtils().getTypeElement(type.getCanonicalName());
        if (params.length == 0) {
            return erase(result.asType());
        }

        var typeArgs = Arrays.stream(params)
                .map(this::getType)
                .toArray(TypeMirror[]::new);
        return processingEnv.getTypeUtils().getDeclaredType(result, typeArgs);
    }

    private TypeMirror getPrimitiveType(TypeKind kind) {
        return switch (kind) {
            case BOOLEAN -> booleanType;
            case BYTE -> byteType;
            case SHORT -> shortType;
            case INT -> intType;
            case LONG -> longType;
            case CHAR -> charType;
            case FLOAT -> floatType;
            case DOUBLE -> doubleType;
            default -> throw new IllegalStateException("Unexpected value: " + kind);
        };
    }

    /**
     * Returns whether the given type mirror represents the same type as the given class.
     *
     * <p>Both types are erased before comparison.
     *
     * @param firstType the type mirror to compare
     * @param secondType the class to compare against
     * @return {@code true} if the types are the same after erasure
     */
    public boolean isSameType(TypeMirror firstType, Class<?> secondType) {
        return isSameType(firstType, getType(secondType));
    }

    /**
     * Returns whether two type mirrors represent the same type.
     *
     * <p>Both types are erased before comparison.
     *
     * @param firstType the first type mirror
     * @param secondType the second type mirror
     * @return {@code true} if the types are the same after erasure
     */
    public boolean isSameType(TypeMirror firstType, TypeMirror secondType) {
        return isSameType(firstType, secondType, true);
    }

    /**
     * Returns whether two type mirrors represent the same type, optionally erasing
     * type arguments before comparison.
     *
     * @param firstType the first type mirror
     * @param secondType the second type mirror
     * @param erase whether to erase both types before comparison
     * @return {@code true} if the types are the same
     */
    public boolean isSameType(TypeMirror firstType, TypeMirror secondType, boolean erase) {
        return firstType != null &&
                secondType != null
                && processingEnv.getTypeUtils().isSameType(erase ? erase(firstType) : firstType, erase ? erase(secondType) : secondType);
    }

    /**
     * Returns the erasure of the given type mirror.
     *
     * <p>If the erasure operation returns {@code null}, the original type mirror
     * is returned unchanged.
     *
     * @param typeMirror the type mirror to erase
     * @return the erased type mirror
     */
    public TypeMirror erase(TypeMirror typeMirror) {
        var result = processingEnv.getTypeUtils().erasure(typeMirror);
        return result == null ? typeMirror : result;
    }

    /**
     * Returns whether the right-hand side type is assignable to the left-hand side class.
     *
     * <p>Both types are erased before the assignability check.
     *
     * @param rhs the type to check for assignability
     * @param lhs the target class
     * @return {@code true} if {@code rhs} is assignable to {@code lhs}
     */
    public boolean isAssignable(TypeMirror rhs, Class<?> lhs) {
        return isAssignable(rhs, lhs, true);
    }

    /**
     * Returns whether the right-hand side type is assignable to the left-hand side class,
     * optionally erasing types before comparison.
     *
     * @param rhs the type to check for assignability
     * @param lhs the target class
     * @param erase whether to erase types before the check
     * @return {@code true} if {@code rhs} is assignable to {@code lhs}
     */
    public boolean isAssignable(TypeMirror rhs, Class<?> lhs, boolean erase) {
        return isAssignable(rhs, getType(lhs), erase);
    }

    /**
     * Returns whether the right-hand side type is assignable to the left-hand side type.
     *
     * <p>Both types are erased before the assignability check. This method is a
     * thread-safe reimplementation of
     * {@link javax.lang.model.util.Types#isAssignable(TypeMirror, TypeMirror)}.
     *
     * @param rhs the type to check for assignability
     * @param lhs the target type
     * @return {@code true} if {@code rhs} is assignable to {@code lhs}
     */
    public boolean isAssignable(TypeMirror rhs, TypeMirror lhs) {
        return isAssignable(rhs, lhs, true);
    }

    /**
     * Returns whether the right-hand side type is assignable to the left-hand side type,
     * optionally erasing types before comparison.
     *
     * <p>This method walks the type hierarchy of {@code rhs} breadth-first, checking
     * superclasses and implemented interfaces for a match against {@code lhs}. Primitive
     * types are boxed before the check.
     *
     * @param rhs the type to check for assignability
     * @param lhs the target type
     * @param erase whether to erase types before the check
     * @return {@code true} if {@code rhs} is assignable to {@code lhs}
     */
    public boolean isAssignable(TypeMirror rhs, TypeMirror lhs, boolean erase) {
        lhs = boxOrErase(lhs, erase);
        rhs = boxOrErase(rhs, erase);
        var rhsTypes = new LinkedList<TypeMirror>();
        rhsTypes.add(rhs);
        while (!rhsTypes.isEmpty()) {
            var rhsAncestorType = rhsTypes.removeFirst();
            if (isSameType(rhsAncestorType, lhs, erase)) {
                return true;
            }
            getDirectSuperClass(rhsAncestorType)
                    .ifPresent(rhsTypes::add);
            rhsTypes.addAll(getAllImplementedInterfaces(rhsAncestorType));
        }
        return false;
    }

    private TypeMirror boxOrErase(TypeMirror rhs, boolean erase) {
        return switch (rhs.getKind()) {
            case BOOLEAN -> wrappedBooleanType;
            case BYTE -> wrappedByteType;
            case SHORT -> wrappedShortType;
            case INT -> wrappedIntType;
            case LONG -> wrappedLongType;
            case CHAR -> wrappedCharType;
            case FLOAT -> wrappedFloatType;
            case DOUBLE -> wrappedDoubleType;
            default -> erase ? erase(rhs) : rhs;
        };
    }

    /**
     * Returns the type element for a non-abstract type with a no-argument constructor,
     * if the given type mirror represents such a type.
     *
     * @param type the type mirror to inspect
     * @return an {@link Optional} containing the type element if it is a concrete type
     *         with a no-argument constructor, or an empty {@code Optional} otherwise
     */
    public Optional<TypeElement> getDefaultConstructor(TypeMirror type) {
        if (erase(type) instanceof DeclaredType declaredType
                && declaredType.asElement() instanceof TypeElement typeElement
                && !typeElement.getModifiers().contains(Modifier.ABSTRACT)
                && hasNoArgsConstructor(typeElement)) {
            return Optional.of(typeElement);
        }

        return Optional.empty();
    }

    private boolean hasNoArgsConstructor(TypeElement typeElement) {
        return typeElement.getEnclosedElements()
                .stream()
                .anyMatch(entry -> entry.getKind() == ElementKind.CONSTRUCTOR && ((ExecutableElement) entry).getParameters().isEmpty());
    }

    /**
     * Resolves an array of mirrored type references from an annotation value supplier.
     *
     * <p>Annotation values that reference {@link Class} objects throw a
     * {@link MirroredTypesException} at runtime during annotation processing. This
     * method catches that exception and extracts the type elements from the mirrored
     * type mirrors.
     *
     * @param supplier a supplier that invokes the annotation method returning
     *        {@code Class<?>[]}
     * @return a list of resolved type elements
     */
    public List<TypeElement> getMirroredTypes(Supplier<Class<?>[]> supplier) {
        try {
            return Arrays.stream(supplier.get())
                    .map(mixin -> processingEnv.getElementUtils().getTypeElement(mixin.getName()))
                    .collect(Collectors.toList());
        } catch (MirroredTypesException exception) {
            return exception.getTypeMirrors()
                    .stream()
                    .map(entry -> entry instanceof DeclaredType declaredType
                            && declaredType.asElement() instanceof TypeElement typeElement ? typeElement : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Returns whether any parameter of the given executable element has a parameterized type.
     *
     * @param element the executable element to inspect
     * @return {@code true} if any parameter type involves type variables
     */
    public boolean isParametrized(ExecutableElement element) {
        return element.getParameters()
                .stream()
                .anyMatch(this::isParametrized);
    }

    private boolean isParametrized(VariableElement parameter) {
        return parameter.asType().getKind() == TypeKind.TYPEVAR ||
                parameter.asType() instanceof DeclaredType declaredType
                        && declaredType.asElement() instanceof TypeElement typeElement
                        && isParametrized(typeElement);
    }

    /**
     * Returns whether the given type mirror involves type variables.
     *
     * <p>A type is considered parameterized if it is itself a type variable, or if
     * it is a declared type with any type argument that is parameterized.
     *
     * @param mirror the type mirror to inspect
     * @return {@code true} if the type involves type variables
     */
    public boolean isParametrized(TypeMirror mirror) {
        return mirror.getKind() == TypeKind.TYPEVAR ||
                (mirror instanceof DeclaredType declaredType && declaredType.getTypeArguments()
                        .stream()
                        .anyMatch(this::isParametrized));
    }

    /**
     * Returns whether the given element declares type parameters.
     *
     * @param element the element to inspect
     * @return {@code true} if the element is a type element with type parameters
     */
    public boolean isParametrized(Element element) {
        return element instanceof TypeElement typeElement && typeElement.getTypeParameters()
                .stream()
                .anyMatch(entry -> entry.asType().getKind() == TypeKind.TYPEVAR || isParametrized(entry));
    }

    /**
     * Resolves the return type of a generic method given concrete argument types.
     *
     * <p>This method performs type inference by mapping each method type parameter to
     * the types of the actual arguments, computing the lower common bound for each
     * parameter, and then substituting into the return type.
     *
     * <p>If the return type is not parameterized, it is returned as-is. If the method
     * uses a receiver type or varargs, no inference is attempted (an error will have
     * already been issued by preliminary checks).
     *
     * @param method the executable element whose return type to resolve
     * @param arguments the actual argument types passed to the method
     * @return the resolved return type
     */
    public TypeMirror getReturnType(ExecutableElement method, List<TypeMirror> arguments) {
        var returnType = method.getReturnType();
        if (!isParametrized(returnType)) {
            return returnType;
        }

        if ((method.getReceiverType() != null && method.getReceiverType().getKind() != TypeKind.NONE) || method.isVarArgs()) {
            return returnType;
        }

        Map<String, Stream<TypeMirror>> typeParametersToArguments = HashMap.newHashMap(method.getParameters().size());
        var parametersIterator = method.getParameters().iterator();
        var argumentsIterator = arguments.iterator();
        while (parametersIterator.hasNext() && argumentsIterator.hasNext()) {
            var methodParameterUses = getTypeUses(parametersIterator.next().asType(), argumentsIterator.next());
            for (var entry : methodParameterUses.entrySet()) {
                typeParametersToArguments.merge(
                        entry.getKey(),
                        entry.getValue().stream(),
                        Stream::concat
                );
            }
        }

        Map<String, TypeMirror> typeParametersToLcb = HashMap.newHashMap(method.getParameters().size());
        for (var entry : typeParametersToArguments.entrySet()) {
            typeParametersToLcb.put(entry.getKey(), lowerCommonBound(entry.getValue()));
        }

        return getReturnType(returnType, typeParametersToLcb);
    }

    private TypeMirror getReturnType(TypeMirror type, Map<String, TypeMirror> typeParametersToMirrors) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            var result = typeParametersToMirrors.getOrDefault(type.toString(), type);
            return boxOrErase(result, false);
        }

        if (type instanceof PrimitiveType primitiveType) {
            return boxOrErase(primitiveType, false);
        }

        if (!(type instanceof DeclaredType declaredType)) {
            return type;
        }

        if (!(declaredType.asElement() instanceof TypeElement typeElement)) {
            return type;
        }

        var resultArguments = new TypeMirror[declaredType.getTypeArguments().size()];
        for (var index = 0; index < resultArguments.length; index++) {
            var typeArgument = declaredType.getTypeArguments().get(index);
            resultArguments[index] = getReturnType(typeArgument, typeParametersToMirrors);
        }

        return processingEnv.getTypeUtils()
                .getDeclaredType(typeElement, resultArguments);
    }

    private TypeMirror lowerCommonBound(Stream<TypeMirror> types) {
        var counter = new HashMap<TypeMirror, Integer>();
        types.forEach(type -> {
            for (var implementedInterface : getAllImplementedInterfaces(type)) {
                counter.compute(implementedInterface, (key, value) -> value == null ? 1 : value + 1);
            }
            while (type != null) {
                counter.compute(type, (key, value) -> value == null ? 1 : value + 1);
                type = getDirectSuperClass(type)
                        .orElse(null);
            }
        });
        TypeMirror bestElement = null;
        var bestCount = 0;
        for (var entry : counter.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestElement = entry.getKey();
                bestCount = entry.getValue();
            } else if (entry.getValue() == bestCount) {
                if (isAssignable(entry.getKey(), bestElement)) {
                    bestElement = entry.getKey();
                }
            }
        }
        return bestElement;
    }

    private Map<String, List<TypeMirror>> getTypeUses(TypeMirror parameterType, TypeMirror argumentType) {
        if (parameterType.getKind() == TypeKind.TYPEVAR) {
            return Map.of(parameterType.toString(), List.of(argumentType));
        }

        if (!(parameterType instanceof DeclaredType methodDeclaredParameterType)) {
            return Map.of();
        }

        var uses = new HashMap<String, List<TypeMirror>>();

        var methodParameterTypeParameters = methodDeclaredParameterType.getTypeArguments();
        for (var index = 0; index < methodParameterTypeParameters.size(); index++) {
            var methodParameterTypeParameter = methodParameterTypeParameters.get(index);
            if (methodParameterTypeParameter.getKind() == TypeKind.TYPEVAR) {
                var currentIndex = index;
                var type = getTypeParameter(argumentType, parameterType, index)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine type"));
                uses.compute(methodParameterTypeParameter.toString(), (key, value) -> {
                    if (value == null) {
                        var data = new ArrayList<TypeMirror>();
                        data.add(type);
                        return data;
                    }

                    value.add(type);
                    return value;
                });
            } else if (methodParameterTypeParameter instanceof DeclaredType methodParameterDeclaredTypeParameter) {
                var methodArgumentTypeParameter = methodDeclaredParameterType.getTypeArguments().get(index);
                if (methodArgumentTypeParameter instanceof DeclaredType methodArgumentDeclaredTypeParameter) {
                    uses.putAll(getTypeUses(methodParameterDeclaredTypeParameter, methodArgumentDeclaredTypeParameter));
                }
            }
        }

        return uses;
    }

    /**
     * Returns the concrete type argument at the given index from a parameterized type,
     * resolved against a model type.
     *
     * <p>This method walks the type hierarchy (interfaces and superclasses) to find the
     * declaration that matches the model type, then extracts and resolves the type
     * argument at the specified index.
     *
     * @param concrete the concrete parameterized type to extract from
     * @param model the target type to match against (typically a raw interface or class)
     * @param index the zero-based index of the type argument to extract
     * @return an {@link Optional} containing the resolved type argument, or empty if
     *         resolution fails
     */
    public Optional<TypeMirror> getTypeParameter(TypeMirror concrete, TypeMirror model, int index) {
        if (!(concrete instanceof DeclaredType declaredType)) {
            return Optional.empty();
        }

        if (isSameType(concrete, model) && index < declaredType.getTypeArguments().size()) {
            var collectionTypeArgument = declaredType.getTypeArguments().get(index);
            return getConcreteTypeParameter(collectionTypeArgument, declaredType, index);
        }

        var typeElement = (TypeElement) declaredType.asElement();
        return typeElement.getInterfaces()
                .stream()
                .filter(implemented -> implemented instanceof DeclaredType)
                .map(implemented -> (DeclaredType) implemented)
                .map(implemented -> getTypeParameterByImplement(declaredType, implemented, model, index))
                .flatMap(Optional::stream)
                .findFirst()
                .or(() -> getTypeParameterBySuperClass(declaredType, typeElement, model, index));
    }

    private Optional<TypeMirror> getTypeParameterByImplement(DeclaredType declaredType, DeclaredType implemented, TypeMirror targetType, int index) {
        if (isSameType(implemented, targetType)) {
            var collectionTypeArgument = implemented.getTypeArguments().get(index);
            return getConcreteTypeParameter(collectionTypeArgument, declaredType, index);
        }

        return getTypeParameter(implemented, targetType, index)
                .flatMap(result -> getConcreteTypeParameter(result, declaredType, index));
    }

    private Optional<TypeMirror> getTypeParameterBySuperClass(DeclaredType declaredType, TypeElement typeElement, TypeMirror targetType, int index) {
        if (!(typeElement.getSuperclass() instanceof DeclaredType superDeclaredType)) {
            return Optional.empty();
        }

        return getTypeParameter(superDeclaredType, targetType, index)
                .flatMap(result -> getConcreteTypeParameter(result, superDeclaredType, index))
                .flatMap(result -> getConcreteTypeParameter(result, declaredType, index));
    }

    private Optional<TypeMirror> getConcreteTypeParameter(TypeMirror argumentMirror, DeclaredType previousType, int index) {
        return switch (argumentMirror) {
            case DeclaredType declaredTypeArgument -> Optional.of(declaredTypeArgument);
            case ArrayType arrayType -> Optional.of(arrayType);
            case TypeVariable typeVariableArgument -> getConcreteTypeFromTypeVariable(typeVariableArgument, previousType, index);
            case null, default -> Optional.empty();
        };
    }

    private Optional<TypeMirror> getConcreteTypeFromTypeVariable(TypeVariable typeVariableArgument, DeclaredType previousType, int index) {
        var currentTypeVarName = typeVariableArgument.asElement().getSimpleName();
        var previousTypeArguments = previousType.getTypeArguments();
        var previousElement = (TypeElement) previousType.asElement();
        var previousTypeParameters = previousElement.getTypeParameters();
        for (; index < previousTypeParameters.size() && index < previousTypeArguments.size(); index++) {
            if (previousTypeParameters.get(index).getSimpleName().equals(currentTypeVarName)) {
                return Optional.of(previousTypeArguments.get(index));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all interfaces transitively implemented by the given type, including
     * interfaces implemented by superclasses.
     *
     * @param typeMirror the type mirror to inspect
     * @return a set of all implemented interface type mirrors
     */
    public Set<? extends TypeMirror> getAllImplementedInterfaces(TypeMirror typeMirror) {
        var results = new HashSet<TypeMirror>();
        var types = new LinkedList<TypeElement>();
        if (typeMirror instanceof DeclaredType declaredType
                && declaredType.asElement() instanceof TypeElement typeElement) {
            types.add(typeElement);
        }
        while (!types.isEmpty()) {
            var typeElement = types.removeFirst();
            getDirectSuperClass(typeElement)
                    .ifPresent(types::add);
            for (var interfaceMirror : typeElement.getInterfaces()) {
                results.add(interfaceMirror);
                if (interfaceMirror instanceof DeclaredType declaredType
                        && declaredType.asElement() instanceof TypeElement interfaceElement) {
                    types.add(interfaceElement);
                }
            }
        }
        return results;
    }

    /**
     * Returns the direct superclass of the given type mirror, if one exists.
     *
     * <p>For array types, {@link Object} is returned as the superclass. For declared
     * types, the superclass declared by the type element is returned.
     *
     * @param mirror the type mirror to inspect
     * @return an {@link Optional} containing the superclass type mirror, or empty if
     *         no superclass exists
     */
    public Optional<TypeMirror> getDirectSuperClass(TypeMirror mirror) {
        return switch (mirror) {
            case ArrayType ignored -> Optional.of(getType(Object.class));
            case DeclaredType declaredType
                    when declaredType.asElement() instanceof TypeElement typeElement
                        && typeElement.getSuperclass() != null
                        && typeElement.getSuperclass().getKind() != TypeKind.NONE
                            -> Optional.ofNullable(typeElement.getSuperclass());
            default -> Optional.empty();
        };
    }

    /**
     * Returns the direct superclass of the given type element, if one exists.
     *
     * @param typeElement the type element to inspect
     * @return an {@link Optional} containing the superclass type element, or empty if
     *         no superclass exists
     */
    public Optional<TypeElement> getDirectSuperClass(TypeElement typeElement) {
        var superClassMirror = typeElement.getSuperclass();
        if (superClassMirror == null || superClassMirror.getKind() == TypeKind.NONE) {
            return Optional.empty();
        }

        if (!(superClassMirror instanceof DeclaredType superClassType)) {
            return Optional.empty();
        }

        if (!(superClassType instanceof TypeElement superClassElement)) {
            return Optional.empty();
        }

        return Optional.of(superClassElement);
    }
}
