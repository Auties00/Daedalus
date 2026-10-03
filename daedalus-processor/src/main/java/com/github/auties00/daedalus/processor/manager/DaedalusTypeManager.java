package com.github.auties00.daedalus.processor.manager;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
 * <p>The compiler model itself is not thread-safe either: javac loads a class lazily
 * the first time one of its properties is queried, including the kind, the supertypes
 * and the type arguments of its declared type, and several of its type relations mutate
 * shared caches. For this reason, every class and method is resolved exactly once while
 * holding a lock: its class is loaded, the lazily computed state that the type factories
 * rely on is initialized, and an immutable snapshot of its erasure, type parameters,
 * supertypes and annotations is recorded. Erasure, type equality, subtyping,
 * assignability and type substitution are then computed from these snapshots following
 * the rules of the Java Language Specification, and are never delegated to javac. The
 * only compiler services invoked concurrently are the factories of javac that create
 * array, wildcard and declared types, which only allocate once the involved classes are
 * resolved. The factories of any other compiler, such as the Eclipse compiler, which
 * caches the types it creates in shared tables, are invoked while holding the lock.
 *
 * <p>This class is non-final so that format-specific subclasses can extend it with
 * additional type predicates (for example, checking whether a type is a protobuf
 * message or enum).
 */
public class DaedalusTypeManager {
    /**
     * The maximum nesting of a subtyping or containment check, which bounds the
     * recursion caused by expansive generic types such as
     * {@code class C<T> implements I<C<C<T>>>}.
     */
    private static final int MAX_RELATION_DEPTH = 64;

    /**
     * The name of the implementation of {@link javax.lang.model.util.Types} of javac.
     */
    private static final String JAVAC_TYPES = "com.sun.tools.javac.model.JavacTypes";

    protected final ProcessingEnvironment processingEnv;
    protected final TypeMirror booleanType;
    protected final TypeMirror byteType;
    protected final TypeMirror shortType;
    protected final TypeMirror intType;
    protected final TypeMirror longType;
    protected final TypeMirror charType;
    protected final TypeMirror floatType;
    protected final TypeMirror doubleType;
    protected final TypeMirror wrappedBooleanType;
    protected final TypeMirror wrappedByteType;
    protected final TypeMirror wrappedShortType;
    protected final TypeMirror wrappedIntType;
    protected final TypeMirror wrappedLongType;
    protected final TypeMirror wrappedCharType;
    protected final TypeMirror wrappedFloatType;
    protected final TypeMirror wrappedDoubleType;
    protected final TypeMirror byteArrayType;
    protected final TypeMirror stringType;

    /**
     * The erasure of {@link Object}, the supertype of every reference type.
     */
    protected final TypeMirror objectType;

    /**
     * The erasure of {@link Cloneable}, a supertype of every array type.
     */
    private final TypeMirror cloneableType;

    /**
     * The erasure of {@link Serializable}, a supertype of every array type.
     */
    private final TypeMirror serializableType;

    /**
     * The lock held while javac is queried to resolve a class or a method.
     */
    private final Object resolutionLock = new Object();

    /**
     * Whether the factories that create array, wildcard and declared types can be
     * invoked concurrently without holding the resolution lock.
     */
    private final boolean concurrentFactories;

    /**
     * The resolved classes, keyed by their type element.
     */
    private final ConcurrentMap<TypeElement, ClassInfo> classes = new ConcurrentHashMap<>();

    /**
     * The resolved methods, keyed by their executable element.
     */
    private final ConcurrentMap<ExecutableElement, MethodInfo> methods = new ConcurrentHashMap<>();

    /**
     * Constructs a new type utility with the given processing environment.
     *
     * @param processingEnv the annotation processing environment
     */
    public DaedalusTypeManager(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.concurrentFactories = processingEnv.getTypeUtils().getClass().getName().equals(JAVAC_TYPES);

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

        this.byteArrayType = getType(byte[].class);
        this.stringType = getType(String.class);

        this.objectType = getType(Object.class);
        this.cloneableType = getType(Cloneable.class);
        this.serializableType = getType(Serializable.class);
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
        return hasAnnotation(mirror, TypeMixin.class);
    }

    /**
     * Returns whether the erasure of the given type is a declared type annotated
     * with the given annotation.
     *
     * <p>Like {@link Element#getAnnotation(Class)}, an annotation that is
     * meta annotated with {@link Inherited} is also found on the superclasses of
     * a class. Unlike that method, this one is thread-safe, as the annotations of
     * a class are read once when it is resolved.
     *
     * @param mirror the type mirror to check
     * @param annotation the annotation type to look for
     * @return {@code true} if the annotation is present
     */
    public boolean hasAnnotation(TypeMirror mirror, Class<? extends Annotation> annotation) {
        var classType = asClassType(erase(mirror));
        if (classType == null) {
            return false;
        }

        var annotationName = annotation.getCanonicalName();
        var inherited = annotation.isAnnotationPresent(Inherited.class);
        var info = getClassInfo((TypeElement) classType.asElement());
        while (true) {
            if (info.annotations().contains(annotationName)) {
                return true;
            }

            // Only annotations on superclasses are inherited, never those on interfaces
            var superclass = asClassType(info.superclass());
            if (!inherited || superclass == null) {
                return false;
            }

            info = getClassInfo((TypeElement) superclass.asElement());
        }
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
            return createArrayType(getType(type.getComponentType()));
        }

        TypeElement result;
        synchronized (resolutionLock) {
            result = processingEnv.getElementUtils().getTypeElement(type.getCanonicalName());
        }
        if (params.length == 0) {
            return erase(result.asType());
        }

        var typeArgs = Arrays.stream(params)
                .map(this::getType)
                .toArray(TypeMirror[]::new);
        return createDeclaredType(result, processingEnv.getTypeUtils().getNoType(TypeKind.NONE), typeArgs);
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
            case VOID -> processingEnv.getTypeUtils().getNoType(TypeKind.VOID);
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
     * <p>This method is a thread-safe reimplementation of
     * {@link javax.lang.model.util.Types#isSameType(TypeMirror, TypeMirror)}, which
     * follows the same rules: two declared types are the same when they share the
     * same element, the same enclosing type and the same type arguments, two type
     * variables are the same when they are declared by the same type parameter,
     * and a wildcard is never the same type as any type, not even itself. Two
     * wildcards used as type arguments are the same when they have the same kind
     * and bound, where the unbounded wildcard is the same as {@code ? extends Object}.
     *
     * @param firstType the first type mirror
     * @param secondType the second type mirror
     * @param erase whether to erase both types before comparison
     * @return {@code true} if the types are the same
     */
    public boolean isSameType(TypeMirror firstType, TypeMirror secondType, boolean erase) {
        if (firstType == null || secondType == null) {
            return false;
        }

        if (erase) {
            return isSameTypeUnchecked(erase(firstType), erase(secondType));
        }

        if (firstType instanceof WildcardType || secondType instanceof WildcardType) {
            return false;
        }

        return isSameTypeUnchecked(firstType, secondType);
    }

    /**
     * Returns whether two type mirrors represent the same type, without the
     * top level wildcard rule of {@link #isSameType(TypeMirror, TypeMirror, boolean)}.
     *
     * @param firstType the first type mirror
     * @param secondType the second type mirror
     * @return {@code true} if the types are the same
     */
    private boolean isSameTypeUnchecked(TypeMirror firstType, TypeMirror secondType) {
        if (firstType == secondType) {
            return true;
        }

        return switch (firstType) {
            case IntersectionType firstIntersectionType -> secondType instanceof IntersectionType secondIntersectionType
                    && isSameBounds(firstIntersectionType.getBounds(), secondIntersectionType.getBounds());
            case UnionType _ -> false;
            case DeclaredType firstDeclaredType -> {
                var firstClassType = asClassType(firstDeclaredType);
                var secondClassType = asClassType(secondType);
                yield firstClassType != null
                        && secondClassType != null
                        && firstClassType.asElement().equals(secondClassType.asElement())
                        && isSameEnclosingType(firstClassType.getEnclosingType(), secondClassType.getEnclosingType())
                        && isSameTypeArguments(firstClassType.getTypeArguments(), secondClassType.getTypeArguments());
            }
            case ArrayType firstArrayType -> secondType instanceof ArrayType secondArrayType
                    && isSameTypeUnchecked(firstArrayType.getComponentType(), secondArrayType.getComponentType());
            case TypeVariable firstTypeVariable -> secondType instanceof TypeVariable secondTypeVariable
                    && firstTypeVariable.asElement().equals(secondTypeVariable.asElement());
            case WildcardType firstWildcardType -> secondType instanceof WildcardType secondWildcardType
                    && isSameWildcard(firstWildcardType, secondWildcardType);
            case PrimitiveType _, NoType _, NullType _ -> isKindOnlyType(secondType)
                    && firstType.getKind() == secondType.getKind();
            case null, default -> false;
        };
    }

    /**
     * Returns whether the given type is a primitive type, a pseudo type such as
     * {@code void}, or the null type, which are fully identified by their kind.
     *
     * <p>The kind of any other type is never queried to compare it, as javac loads
     * the class of a declared type when its kind is queried.
     *
     * @param type the type
     * @return {@code true} if the type is identified by its kind
     */
    private boolean isKindOnlyType(TypeMirror type) {
        return type instanceof PrimitiveType || type instanceof NoType || type instanceof NullType;
    }

    /**
     * Returns whether the given type is the null type.
     *
     * <p>The kind of the type is checked in addition to its interface, as some compilers
     * implement both {@link NoType} and {@link NullType} with the same class: for example,
     * the {@code void} type of the Eclipse compiler is an instance of {@link NullType}.
     *
     * @param type the type
     * @return {@code true} if the type is the null type
     */
    private boolean isNullType(TypeMirror type) {
        return type instanceof NullType && type.getKind() == TypeKind.NULL;
    }

    /**
     * Returns whether two enclosing types, as returned by
     * {@link DeclaredType#getEnclosingType()}, are the same.
     *
     * @param firstType the first enclosing type
     * @param secondType the second enclosing type
     * @return {@code true} if both types have no enclosing type, or if they are the same
     */
    private boolean isSameEnclosingType(TypeMirror firstType, TypeMirror secondType) {
        if (firstType instanceof DeclaredType || secondType instanceof DeclaredType) {
            return isSameTypeUnchecked(firstType, secondType);
        }

        return true;
    }

    /**
     * Returns whether two lists of type arguments are pairwise the same.
     *
     * <p>A wildcard argument is only the same as another wildcard argument, as
     * a wildcard never contains a type that is not a wildcard in both directions.
     *
     * @param firstArguments the first type arguments
     * @param secondArguments the second type arguments
     * @return {@code true} if both lists have the same size and the same arguments
     */
    private boolean isSameTypeArguments(List<? extends TypeMirror> firstArguments, List<? extends TypeMirror> secondArguments) {
        if (firstArguments.size() != secondArguments.size()) {
            return false;
        }

        for (var index = 0; index < firstArguments.size(); index++) {
            if (!isSameTypeUnchecked(firstArguments.get(index), secondArguments.get(index))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether two wildcards have the same kind and the same bound.
     *
     * <p>The unbounded wildcard is treated as {@code ? extends Object}.
     *
     * @param firstWildcard the first wildcard
     * @param secondWildcard the second wildcard
     * @return {@code true} if the wildcards are the same
     */
    private boolean isSameWildcard(WildcardType firstWildcard, WildcardType secondWildcard) {
        var firstSuperBound = firstWildcard.getSuperBound();
        var secondSuperBound = secondWildcard.getSuperBound();
        if (firstSuperBound != null || secondSuperBound != null) {
            return firstSuperBound != null
                    && secondSuperBound != null
                    && isSameTypeUnchecked(firstSuperBound, secondSuperBound);
        }

        return isSameTypeUnchecked(getUpperBound(firstWildcard), getUpperBound(secondWildcard));
    }

    /**
     * Returns whether two lists of intersection bounds contain the same types,
     * regardless of their order.
     *
     * @param firstBounds the first bounds
     * @param secondBounds the second bounds
     * @return {@code true} if every bound of each list is the same as a bound of the other
     */
    private boolean isSameBounds(List<? extends TypeMirror> firstBounds, List<? extends TypeMirror> secondBounds) {
        return firstBounds.size() == secondBounds.size()
                && firstBounds.stream().allMatch(firstBound -> secondBounds.stream().anyMatch(secondBound -> isSameTypeUnchecked(firstBound, secondBound)))
                && secondBounds.stream().allMatch(secondBound -> firstBounds.stream().anyMatch(firstBound -> isSameTypeUnchecked(firstBound, secondBound)));
    }

    /**
     * Returns the erasure of the given type mirror.
     *
     * <p>This method is a thread-safe reimplementation of
     * {@link javax.lang.model.util.Types#erasure(TypeMirror)}, which follows the
     * rules of the Java Language Specification: a declared type is erased to its
     * raw type, an array type to the array of the erasure of its component, a
     * type variable to the erasure of its leftmost bound, a wildcard to the
     * erasure of its upper bound, and an intersection type to the erasure of its
     * first bound. Every other type, including a type whose class could not be
     * loaded, is its own erasure.
     *
     * @param typeMirror the type mirror to erase
     * @return the erased type mirror
     */
    public TypeMirror erase(TypeMirror typeMirror) {
        return switch (typeMirror) {
            case IntersectionType intersectionType -> erase(intersectionType.getBounds().getFirst());
            case UnionType _ -> typeMirror;
            case DeclaredType declaredType -> {
                var classType = asClassType(declaredType);
                yield classType == null ? typeMirror : getClassInfo((TypeElement) classType.asElement()).erasure();
            }
            case ArrayType arrayType -> {
                var componentType = arrayType.getComponentType();
                var erasedComponentType = erase(componentType);
                yield erasedComponentType == componentType ? arrayType : createArrayType(erasedComponentType);
            }
            case TypeVariable typeVariable -> erase(typeVariable.getUpperBound());
            case WildcardType wildcardType -> erase(getUpperBound(wildcardType));
            case null, default -> typeMirror;
        };
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
     * <p>Primitive types are boxed before the check, so a primitive type and its
     * wrapper are assignable to each other, while primitive widening, such as from
     * {@code int} to {@code long}, is not applied. The boxed types are then checked
     * for subtyping as defined by the Java Language Specification: supertypes are
     * computed by substituting the type arguments of {@code rhs} into the declared
     * supertypes of its class, type arguments are compared by containment, so that
     * {@code List<String>} is assignable to {@code List<? extends CharSequence>},
     * array types are covariant and assignable to {@link Object}, {@link Cloneable}
     * and {@link Serializable}, and a raw type is assignable to any parameterization
     * of its class through an unchecked conversion.
     *
     * @param rhs the type to check for assignability
     * @param lhs the target type
     * @param erase whether to erase types before the check
     * @return {@code true} if {@code rhs} is assignable to {@code lhs}
     */
    public boolean isAssignable(TypeMirror rhs, TypeMirror lhs, boolean erase) {
        lhs = boxOrErase(lhs, erase);
        rhs = boxOrErase(rhs, erase);
        return isSubtype(rhs, lhs, true, 0);
    }

    private TypeMirror boxOrErase(TypeMirror rhs, boolean erase) {
        if (!(rhs instanceof PrimitiveType)) {
            return erase ? erase(rhs) : rhs;
        }

        return switch (rhs.getKind()) {
            case BOOLEAN -> wrappedBooleanType;
            case BYTE -> wrappedByteType;
            case SHORT -> wrappedShortType;
            case INT -> wrappedIntType;
            case LONG -> wrappedLongType;
            case CHAR -> wrappedCharType;
            case FLOAT -> wrappedFloatType;
            case DOUBLE -> wrappedDoubleType;
            default -> rhs;
        };
    }

    /**
     * Returns whether the given subtype is a subtype of the given supertype.
     *
     * @param subtype the candidate subtype
     * @param supertype the candidate supertype
     * @param unchecked whether a raw type is a subtype of a parameterization of its
     *        class, as allowed by an unchecked conversion
     * @param depth the nesting of this check, used to stop expansive recursions
     * @return {@code true} if {@code subtype} is a subtype of {@code supertype}
     */
    private boolean isSubtype(TypeMirror subtype, TypeMirror supertype, boolean unchecked, int depth) {
        if (depth > MAX_RELATION_DEPTH) {
            return false;
        }

        if (isSameTypeUnchecked(subtype, supertype)) {
            return true;
        }

        if (supertype instanceof IntersectionType intersectionType) {
            return intersectionType.getBounds()
                    .stream()
                    .allMatch(bound -> isSubtype(subtype, bound, unchecked, depth + 1));
        }

        if (isNullType(subtype)) {
            return isReference(supertype);
        }

        return switch (subtype) {
            case IntersectionType intersectionType -> intersectionType.getBounds()
                    .stream()
                    .anyMatch(bound -> isSubtype(bound, supertype, unchecked, depth + 1));
            case UnionType _ -> false;
            case TypeVariable typeVariable -> isSubtype(typeVariable.getUpperBound(), supertype, unchecked, depth + 1);
            case WildcardType wildcardType -> isSubtype(getUpperBound(wildcardType), supertype, unchecked, depth + 1);
            case ArrayType arraySubtype -> isArraySubtype(arraySubtype, supertype, unchecked, depth);
            case DeclaredType declaredSubtype -> {
                var classSubtype = asClassType(declaredSubtype);
                var classSupertype = asClassType(supertype);
                yield classSubtype != null
                        && classSupertype != null
                        && isClassSubtype(classSubtype, classSupertype, unchecked, depth);
            }
            case null, default -> false;
        };
    }

    /**
     * Returns whether an array type is a subtype of the given supertype.
     *
     * <p>Every array type is a subtype of {@link Object}, {@link Cloneable} and
     * {@link Serializable}. Arrays of primitives are only subtypes of arrays of the
     * same primitive, while arrays of references are covariant.
     *
     * @param subtype the candidate array subtype
     * @param supertype the candidate supertype
     * @param unchecked whether an unchecked conversion is allowed
     * @param depth the nesting of this check
     * @return {@code true} if {@code subtype} is a subtype of {@code supertype}
     */
    private boolean isArraySubtype(ArrayType subtype, TypeMirror supertype, boolean unchecked, int depth) {
        if (supertype instanceof ArrayType arraySupertype) {
            var componentSubtype = subtype.getComponentType();
            var componentSupertype = arraySupertype.getComponentType();
            if (componentSubtype instanceof PrimitiveType || componentSupertype instanceof PrimitiveType) {
                return componentSubtype instanceof PrimitiveType
                        && componentSupertype instanceof PrimitiveType
                        && componentSubtype.getKind() == componentSupertype.getKind();
            }

            return isSubtype(componentSubtype, componentSupertype, unchecked, depth + 1);
        }

        var classSupertype = asClassType(supertype);
        return classSupertype != null
                && (isSameTypeUnchecked(classSupertype, objectType)
                || isSameTypeUnchecked(classSupertype, cloneableType)
                || isSameTypeUnchecked(classSupertype, serializableType));
    }

    /**
     * Returns whether a class type is a subtype of another class type.
     *
     * @param subtype the candidate subtype
     * @param supertype the candidate supertype
     * @param unchecked whether an unchecked conversion is allowed
     * @param depth the nesting of this check
     * @return {@code true} if {@code subtype} is a subtype of {@code supertype}
     */
    private boolean isClassSubtype(DeclaredType subtype, DeclaredType supertype, boolean unchecked, int depth) {
        // Every reference type, including interfaces, is a subtype of Object
        if (isSameTypeUnchecked(erase(supertype), objectType)) {
            return true;
        }

        var matchingSupertype = asSuper(subtype, (TypeElement) supertype.asElement());
        if (matchingSupertype == null) {
            return false;
        }

        var supertypeArguments = supertype.getTypeArguments();
        if (supertypeArguments.isEmpty()) {
            return true;
        }

        var matchingArguments = matchingSupertype.getTypeArguments();
        if (matchingArguments.size() != supertypeArguments.size()) {
            return unchecked && matchingArguments.isEmpty();
        }

        var enclosingSupertype = asClassType(supertype.getEnclosingType());
        if (enclosingSupertype != null
                && !enclosingSupertype.getTypeArguments().isEmpty()
                && !isSubtype(matchingSupertype.getEnclosingType(), enclosingSupertype, unchecked, depth + 1)) {
            return false;
        }

        for (var index = 0; index < supertypeArguments.size(); index++) {
            if (!containsTypeArgument(supertypeArguments.get(index), matchingArguments.get(index), depth + 1)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether the given type argument contains the given argument, as
     * defined by the containment relation of the Java Language Specification.
     *
     * <p>A type argument that is not a wildcard only contains the same type.
     * The wildcard {@code ? extends U} contains every argument whose upper bound is
     * a subtype of {@code U}, and the wildcard {@code ? super L} contains every
     * argument whose lower bound is a supertype of {@code L}.
     *
     * @param typeArgument the type argument that should contain the argument
     * @param argument the contained argument
     * @param depth the nesting of this check
     * @return {@code true} if {@code typeArgument} contains {@code argument}
     */
    private boolean containsTypeArgument(TypeMirror typeArgument, TypeMirror argument, int depth) {
        if (!(typeArgument instanceof WildcardType wildcardArgument)) {
            return isSameTypeUnchecked(typeArgument, argument);
        }

        var superBound = wildcardArgument.getSuperBound();
        if (superBound != null) {
            var argumentLowerBound = argument instanceof WildcardType wildcardType ? wildcardType.getSuperBound() : argument;
            return argumentLowerBound != null
                    && isSubtype(superBound, argumentLowerBound, false, depth + 1);
        }

        var extendsBound = getUpperBound(wildcardArgument);
        if (isSameTypeUnchecked(extendsBound, objectType)) {
            return true;
        }

        var argumentUpperBound = argument instanceof WildcardType wildcardType ? getUpperBound(wildcardType) : argument;
        return isSubtype(argumentUpperBound, extendsBound, false, depth + 1);
    }

    /**
     * Returns the supertype of the given class type whose element is the given
     * type element, with the type arguments of the given type substituted into it.
     *
     * <p>If the given type is raw, the returned supertype is raw as well.
     *
     * @param type the class type whose supertypes are searched
     * @param element the element of the supertype to find
     * @return the matching supertype, or {@code null} if the element is not a supertype
     */
    private DeclaredType asSuper(DeclaredType type, TypeElement element) {
        var queue = new ArrayDeque<DeclaredType>();
        queue.add(type);
        var visited = new HashSet<Element>();
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            var currentElement = current.asElement();
            if (currentElement.equals(element)) {
                return current;
            }

            if (visited.add(currentElement)) {
                queue.addAll(getDirectSupertypes(current));
            }
        }

        return null;
    }

    /**
     * Returns the direct superclass and superinterfaces of the given class type,
     * with its type arguments substituted into them.
     *
     * <p>The supertypes of a raw type are the erasures of the declared supertypes
     * of its class.
     *
     * @param type the class type
     * @return the direct supertypes of the given type
     */
    private List<DeclaredType> getDirectSupertypes(DeclaredType type) {
        var info = getClassInfo((TypeElement) type.asElement());
        var raw = isRaw(type);
        var bindings = raw ? Map.<Element, TypeMirror>of() : getTypeBindings(type);
        var results = new ArrayList<DeclaredType>(info.interfaces().size() + 1);
        addDirectSupertype(results, info.superclass(), raw, bindings);
        for (var implemented : info.interfaces()) {
            addDirectSupertype(results, implemented, raw, bindings);
        }
        return results;
    }

    /**
     * Adds the given declared supertype to the given list, after erasing it or
     * substituting the given bindings into it.
     *
     * <p>Supertypes that are not class types, such as the missing superclass of
     * {@link Object} or of an interface, and supertypes whose class could not be
     * loaded are ignored.
     *
     * @param results the list of supertypes to add to
     * @param supertype the declared supertype
     * @param raw whether the subtype is raw
     * @param bindings the type arguments of the subtype, keyed by their type parameter
     */
    private void addDirectSupertype(List<DeclaredType> results, TypeMirror supertype, boolean raw, Map<Element, TypeMirror> bindings) {
        var classSupertype = asClassType(supertype);
        if (classSupertype == null) {
            return;
        }

        var result = asClassType(raw ? erase(classSupertype) : substitute(classSupertype, bindings));
        if (result != null) {
            results.add(result);
        }
    }

    /**
     * Returns whether the given class type is raw, that is whether it omits the
     * type arguments of a generic class, or is a member of a raw type.
     *
     * @param type the class type
     * @return {@code true} if the type is raw
     */
    private boolean isRaw(DeclaredType type) {
        if (type.getTypeArguments().isEmpty() && !getClassInfo((TypeElement) type.asElement()).typeParameters().isEmpty()) {
            return true;
        }

        var enclosingType = asClassType(type.getEnclosingType());
        return enclosingType != null && isRaw(enclosingType);
    }

    /**
     * Returns the type arguments of the given class type, and of its enclosing
     * types, keyed by the type parameter they are bound to.
     *
     * @param type the class type
     * @return the bindings of the type parameters in scope for the given type
     */
    private Map<Element, TypeMirror> getTypeBindings(DeclaredType type) {
        var bindings = new HashMap<Element, TypeMirror>();
        var current = type;
        while (current != null) {
            var parameters = getClassInfo((TypeElement) current.asElement()).typeParameters();
            var arguments = current.getTypeArguments();
            if (parameters.size() == arguments.size()) {
                for (var index = 0; index < parameters.size(); index++) {
                    bindings.put(((TypeVariable) parameters.get(index)).asElement(), arguments.get(index));
                }
            }
            current = asClassType(current.getEnclosingType());
        }
        return bindings;
    }

    /**
     * Substitutes the type variables bound by the given bindings in the given type.
     *
     * <p>A type variable bound to a wildcard is replaced by the wildcard where a
     * type argument is expected, and by the bound of the wildcard everywhere else.
     * Intersection and union types are never substituted, as they only appear as
     * the bounds of type variables and in catch clauses.
     *
     * @param type the type to substitute into
     * @param bindings the type arguments, keyed by the type parameter they are bound to
     * @return the substituted type, or {@code type} itself if nothing was substituted
     */
    private TypeMirror substitute(TypeMirror type, Map<Element, TypeMirror> bindings) {
        if (bindings.isEmpty()) {
            return type;
        }

        return switch (type) {
            case IntersectionType _, UnionType _ -> type;
            case TypeVariable typeVariable -> bindings.getOrDefault(typeVariable.asElement(), type);
            case DeclaredType declaredType -> {
                var classType = asClassType(declaredType);
                if (classType == null) {
                    yield type;
                }

                var arguments = classType.getTypeArguments();
                var substitutedArguments = new TypeMirror[arguments.size()];
                var changed = false;
                for (var index = 0; index < substitutedArguments.length; index++) {
                    var argument = arguments.get(index);
                    substitutedArguments[index] = substitute(argument, bindings);
                    changed |= substitutedArguments[index] != argument;
                }

                var enclosingType = classType.getEnclosingType();
                var substitutedEnclosingType = asClassType(enclosingType) != null
                        ? substitute(enclosingType, bindings)
                        : enclosingType;
                if (!changed && substitutedEnclosingType == enclosingType) {
                    yield type;
                }

                yield createDeclaredType((TypeElement) classType.asElement(), substitutedEnclosingType, substitutedArguments);
            }
            case ArrayType arrayType -> {
                var componentType = arrayType.getComponentType();
                var substitutedComponentType = substitute(componentType, bindings);
                if (substitutedComponentType == componentType) {
                    yield type;
                }

                yield createArrayType(toTypeBound(substitutedComponentType, true));
            }
            case WildcardType wildcardType -> {
                var extendsBound = wildcardType.getExtendsBound();
                var superBound = wildcardType.getSuperBound();
                var substitutedExtendsBound = extendsBound == null ? null : substitute(extendsBound, bindings);
                var substitutedSuperBound = superBound == null ? null : substitute(superBound, bindings);
                if (substitutedExtendsBound == extendsBound && substitutedSuperBound == superBound) {
                    yield type;
                }

                yield createWildcardType(
                        substitutedExtendsBound == null ? null : toTypeBound(substitutedExtendsBound, true),
                        substitutedSuperBound == null ? null : toTypeBound(substitutedSuperBound, false)
                );
            }
            case null, default -> type;
        };
    }

    /**
     * Returns the given type if it is not a wildcard, or the requested bound of the
     * wildcard otherwise, for positions where a wildcard is not allowed.
     *
     * @param type the type
     * @param upper whether the upper bound, rather than the lower bound, is requested
     * @return a type that is not a wildcard
     */
    private TypeMirror toTypeBound(TypeMirror type, boolean upper) {
        if (!(type instanceof WildcardType wildcardType)) {
            return type;
        }

        if (upper) {
            return getUpperBound(wildcardType);
        }

        var superBound = wildcardType.getSuperBound();
        return superBound != null ? superBound : getUpperBound(wildcardType);
    }

    /**
     * Returns the upper bound of the given wildcard, which is {@link Object} for
     * the unbounded wildcard and for wildcards with a lower bound.
     *
     * @param wildcardType the wildcard
     * @return the upper bound of the wildcard
     */
    private TypeMirror getUpperBound(WildcardType wildcardType) {
        var extendsBound = wildcardType.getExtendsBound();
        return extendsBound != null ? extendsBound : objectType;
    }

    /**
     * Creates a declared type of the given element with the given enclosing type
     * and type arguments.
     *
     * <p>javac lazily computes whether a containing type is parameterized, so a
     * parameterized enclosing type is always recreated before being passed to the
     * factory: the new instance is only visible to the current thread. The factory is
     * invoked while holding the resolution lock unless it is the one of javac.
     *
     * @param element the element of the type to create
     * @param enclosingType the enclosing type, or a type of kind {@link TypeKind#NONE}
     * @param typeArguments the type arguments
     * @return the created declared type, or the erasure of the element if the
     *         requested type is malformed
     */
    private TypeMirror createDeclaredType(TypeElement element, TypeMirror enclosingType, TypeMirror[] typeArguments) {
        var info = getClassInfo(element);
        try {
            var enclosingClassType = asClassType(enclosingType);
            if (enclosingClassType != null && !enclosingClassType.getTypeArguments().isEmpty()) {
                var containingType = createDeclaredType(
                        (TypeElement) enclosingClassType.asElement(),
                        enclosingClassType.getEnclosingType(),
                        enclosingClassType.getTypeArguments().toArray(TypeMirror[]::new)
                );
                if (containingType instanceof DeclaredType containingDeclaredType) {
                    if (concurrentFactories) {
                        return processingEnv.getTypeUtils().getDeclaredType(containingDeclaredType, element, typeArguments);
                    }

                    synchronized (resolutionLock) {
                        return processingEnv.getTypeUtils().getDeclaredType(containingDeclaredType, element, typeArguments);
                    }
                }
            }

            if (concurrentFactories) {
                return processingEnv.getTypeUtils().getDeclaredType(element, typeArguments);
            }

            synchronized (resolutionLock) {
                return processingEnv.getTypeUtils().getDeclaredType(element, typeArguments);
            }
        } catch (IllegalArgumentException exception) {
            return info.erasure();
        }
    }

    /**
     * Creates an array type with the given component type.
     *
     * @param componentType the component type
     * @return the created array type
     */
    private ArrayType createArrayType(TypeMirror componentType) {
        if (concurrentFactories) {
            return processingEnv.getTypeUtils().getArrayType(componentType);
        }

        synchronized (resolutionLock) {
            return processingEnv.getTypeUtils().getArrayType(componentType);
        }
    }

    /**
     * Creates a wildcard type with the given bounds.
     *
     * @param extendsBound the upper bound, or {@code null} if none
     * @param superBound the lower bound, or {@code null} if none
     * @return the created wildcard type
     */
    private WildcardType createWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        if (concurrentFactories) {
            return processingEnv.getTypeUtils().getWildcardType(extendsBound, superBound);
        }

        synchronized (resolutionLock) {
            return processingEnv.getTypeUtils().getWildcardType(extendsBound, superBound);
        }
    }

    /**
     * Returns the given type as a declared type of a class that was loaded
     * successfully, or {@code null} if it is any other type.
     *
     * <p>A declared type cannot be told apart from an error type, nor from an
     * intersection or union type, by testing its interfaces: javac models all of
     * them with the same class, which implements both {@link DeclaredType} and
     * {@link ErrorType}. The kind of a declared type is only queried once its
     * class is resolved, as javac loads the class when its kind is queried.
     *
     * @param type the type
     * @return the class type, or {@code null} if the type is not a class type
     */
    private DeclaredType asClassType(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)
                || type instanceof IntersectionType
                || type instanceof UnionType
                || !(declaredType.asElement() instanceof TypeElement element)) {
            return null;
        }

        var info = getClassInfo(element);
        if (info.erroneous() || declaredType.getKind() != TypeKind.DECLARED) {
            return null;
        }

        return declaredType;
    }

    /**
     * Returns whether the given type is a reference type, that is a type that
     * {@code null} is assignable to.
     *
     * @param type the type
     * @return {@code true} if the type is a reference type
     */
    private boolean isReference(TypeMirror type) {
        if (isNullType(type)) {
            return true;
        }

        return switch (type) {
            case IntersectionType _, UnionType _, ArrayType _, TypeVariable _ -> true;
            case DeclaredType declaredType -> asClassType(declaredType) != null;
            case null, default -> false;
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
        if (asClassType(erase(type)) instanceof DeclaredType declaredType
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
     * <p>Every type element is looked up again by its qualified name, so that the
     * returned elements are the ones returned by {@link Elements#getTypeElement(CharSequence)}.
     * Some compilers do not guarantee it otherwise: for example, the Eclipse compiler
     * binds the classes referenced by the default value of an annotation declared in a
     * dependency to elements that have no annotations and that are not equal to the
     * ones found by name.
     *
     * @param supplier a supplier that invokes the annotation method returning
     *        {@code Class<?>[]}
     * @return a set of resolved type elements
     */
    public Set<TypeElement> getMirroredTypes(Supplier<Class<?>[]> supplier) {
        try {
            var values = supplier.get();
            synchronized (resolutionLock) {
                return Arrays.stream(values)
                        .map(mixin -> processingEnv.getElementUtils().getTypeElement(mixin.getName()))
                        .collect(Collectors.toSet());
            }
        } catch (MirroredTypesException exception) {
            synchronized (resolutionLock) {
                return exception.getTypeMirrors()
                        .stream()
                        .map(entry -> entry instanceof DeclaredType declaredType
                                && declaredType.asElement() instanceof TypeElement typeElement ? getCanonicalTypeElement(typeElement) : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }
    }

    /**
     * Returns the type element found by the qualified name of the given type element.
     *
     * @param typeElement the type element
     * @return the type element found by name, or {@code typeElement} if it has no
     *         qualified name or cannot be found by name, for example because it is a
     *         local class or because its name is ambiguous across modules
     */
    private TypeElement getCanonicalTypeElement(TypeElement typeElement) {
        var qualifiedName = typeElement.getQualifiedName();
        if (qualifiedName.isEmpty()) {
            return typeElement;
        }

        var result = processingEnv.getElementUtils().getTypeElement(qualifiedName);
        return result != null ? result : typeElement;
    }

    /**
     * Returns whether any parameter of the given executable element has a parameterized type.
     *
     * @param element the executable element to inspect
     * @return {@code true} if any parameter type involves type variables
     */
    public boolean isParametrized(ExecutableElement element) {
        return getMethodInfo(element).parameterTypes()
                .stream()
                .anyMatch(this::isParametrizedParameter);
    }

    /**
     * Returns whether the given parameter type is a type variable, or a class
     * type whose class declares type parameters.
     *
     * @param parameterType the parameter type to inspect
     * @return {@code true} if the parameter type is parameterized
     */
    private boolean isParametrizedParameter(TypeMirror parameterType) {
        return parameterType instanceof TypeVariable
                || asClassType(parameterType) instanceof DeclaredType declaredType
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
        return mirror instanceof TypeVariable
                || (asClassType(mirror) instanceof DeclaredType declaredType && declaredType.getTypeArguments()
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
        return element instanceof TypeElement typeElement
                && !getClassInfo(typeElement).typeParameters().isEmpty();
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
        var info = getMethodInfo(method);
        var returnType = info.returnType();
        if (!isParametrized(returnType)) {
            return returnType;
        }

        if (info.receiver() || info.varArgs()) {
            return returnType;
        }

        var typeParametersToLcb = inferTypeBindings(method, arguments);
        return substituteTypeVariables(returnType, typeParametersToLcb);
    }

    /**
     * Infers type variable bindings for a method by unifying each parameter type
     * with the corresponding actual argument type.
     *
     * <p>A parameter type that is a declared type is unified with the supertype of
     * the argument type that has the same class, so that a {@code Collection<T>}
     * parameter binds {@code T} to {@code String} for a {@code List<String>}
     * argument. A type variable bound to a wildcard argument is bound to the bound
     * of the wildcard. When the same type variable appears in multiple parameters,
     * the lower common bound of the candidate bindings is used.
     *
     * @param method the executable element whose type parameters to infer
     * @param arguments the actual argument types
     * @return a map of type variable names to inferred type bindings
     */
    public Map<String, TypeMirror> inferTypeBindings(ExecutableElement method, List<TypeMirror> arguments) {
        var parameterTypes = getMethodInfo(method).parameterTypes();
        Map<String, List<TypeMirror>> typeParametersToArguments = HashMap.newHashMap(parameterTypes.size());
        var parametersIterator = parameterTypes.iterator();
        var argumentsIterator = arguments.iterator();
        while (parametersIterator.hasNext() && argumentsIterator.hasNext()) {
            collectTypeUses(parametersIterator.next(), argumentsIterator.next(), typeParametersToArguments);
        }

        Map<String, TypeMirror> typeParametersToLcb = HashMap.newHashMap(typeParametersToArguments.size());
        for (var entry : typeParametersToArguments.entrySet()) {
            typeParametersToLcb.put(entry.getKey(), lowerCommonBound(entry.getValue().stream()));
        }
        return typeParametersToLcb;
    }

    /**
     * Substitutes type variables in the given type with concrete bindings.
     *
     * <p>Handles declared types, type variables, wildcard types, array types,
     * and primitive types. Unresolved type variables are returned unchanged.
     * A primitive type is boxed, unless it is the component of an array type.
     *
     * @param type the type mirror to substitute into
     * @param typeParametersToMirrors a map of type variable names to their bindings
     * @return the substituted type mirror
     */
    public TypeMirror substituteTypeVariables(TypeMirror type, Map<String, TypeMirror> typeParametersToMirrors) {
        return switch (type) {
            case TypeVariable _ -> {
                var result = typeParametersToMirrors.getOrDefault(type.toString(), type);
                yield boxOrErase(result, false);
            }
            case PrimitiveType primitiveType -> boxOrErase(primitiveType, false);
            case ArrayType arrayType -> {
                var componentType = arrayType.getComponentType();
                var substitutedComponentType = componentType instanceof PrimitiveType
                        ? componentType
                        : toTypeBound(substituteTypeVariables(componentType, typeParametersToMirrors), true);
                yield createArrayType(substitutedComponentType);
            }
            case WildcardType wildcardType -> {
                var extendsBound = wildcardType.getExtendsBound();
                var superBound = wildcardType.getSuperBound();
                var substitutedExtends = extendsBound == null ? null : toTypeBound(substituteTypeVariables(extendsBound, typeParametersToMirrors), true);
                var substitutedSuper = superBound == null ? null : toTypeBound(substituteTypeVariables(superBound, typeParametersToMirrors), false);
                yield createWildcardType(substitutedExtends, substitutedSuper);
            }
            case IntersectionType _, UnionType _ -> type;
            case DeclaredType declaredType -> {
                var classType = asClassType(declaredType);
                if (classType == null) {
                    yield type;
                }

                var typeArguments = classType.getTypeArguments();
                var resultArguments = new TypeMirror[typeArguments.size()];
                for (var index = 0; index < resultArguments.length; index++) {
                    var typeArgument = typeArguments.get(index);
                    resultArguments[index] = substituteTypeVariables(typeArgument, typeParametersToMirrors);
                }

                yield createDeclaredType((TypeElement) classType.asElement(), classType.getEnclosingType(), resultArguments);
            }
            case null, default -> type;
        };
    }

    private TypeMirror lowerCommonBound(Stream<TypeMirror> types) {
        var counter = new HashMap<TypeMirror, Integer>();
        types.forEach(type -> {
            for (var implementedInterface : getAllImplementedInterfaces(type)) {
                counter.compute(implementedInterface, (_, value) -> value == null ? 1 : value + 1);
            }
            while (type != null) {
                counter.compute(type, (_, value) -> value == null ? 1 : value + 1);
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

    /**
     * Collects the argument types bound to each type variable of a parameter type
     * by unifying it with an argument type.
     *
     * @param parameterType the declared parameter type
     * @param argumentType the actual argument type
     * @param uses the collected argument types, keyed by the name of the type variable
     */
    private void collectTypeUses(TypeMirror parameterType, TypeMirror argumentType, Map<String, List<TypeMirror>> uses) {
        switch (parameterType) {
            case TypeVariable _ -> uses.computeIfAbsent(parameterType.toString(), _ -> new ArrayList<>())
                    .add(toTypeBound(argumentType, true));
            case ArrayType parameterArrayType -> {
                if (argumentType instanceof ArrayType argumentArrayType) {
                    collectTypeUses(parameterArrayType.getComponentType(), argumentArrayType.getComponentType(), uses);
                }
            }
            case WildcardType parameterWildcardType -> {
                var superBound = parameterWildcardType.getSuperBound();
                if (superBound != null) {
                    collectTypeUses(superBound, toTypeBound(argumentType, false), uses);
                } else if (parameterWildcardType.getExtendsBound() != null) {
                    collectTypeUses(parameterWildcardType.getExtendsBound(), toTypeBound(argumentType, true), uses);
                }
            }
            case IntersectionType _, UnionType _ -> {
            }
            case DeclaredType parameterDeclaredType -> {
                var parameterClassType = asClassType(parameterDeclaredType);
                var argumentClassType = asClassType(argumentType);
                if (parameterClassType == null || argumentClassType == null) {
                    return;
                }

                var supertype = asSuper(argumentClassType, (TypeElement) parameterClassType.asElement());
                if (supertype == null) {
                    return;
                }

                var parameterArguments = parameterClassType.getTypeArguments();
                var supertypeArguments = supertype.getTypeArguments();
                if (parameterArguments.size() != supertypeArguments.size()) {
                    return;
                }

                for (var index = 0; index < parameterArguments.size(); index++) {
                    collectTypeUses(parameterArguments.get(index), supertypeArguments.get(index), uses);
                }
            }
            case null, default -> {
            }
        }
    }

    /**
     * Returns the concrete type argument at the given index from a parameterized type,
     * resolved against a model type.
     *
     * <p>This method finds the supertype of the concrete type whose class is the class
     * of the model type, with the type arguments of the concrete type substituted into
     * it, and returns its type argument at the specified index.
     *
     * @param concrete the concrete parameterized type to extract from
     * @param model the target type to match against (typically a raw interface or class)
     * @param index the zero-based index of the type argument to extract
     * @return an {@link Optional} containing the resolved type argument, or empty if
     *         resolution fails or the type argument is not a declared or array type
     */
    public Optional<TypeMirror> getTypeParameter(TypeMirror concrete, TypeMirror model, int index) {
        var concreteType = asClassType(concrete);
        var modelType = asClassType(erase(model));
        if (concreteType == null || modelType == null) {
            return Optional.empty();
        }

        var supertype = asSuper(concreteType, (TypeElement) modelType.asElement());
        if (supertype == null) {
            return Optional.empty();
        }

        var typeArguments = supertype.getTypeArguments();
        if (index >= typeArguments.size()) {
            return Optional.empty();
        }

        var typeArgument = typeArguments.get(index);
        if (typeArgument instanceof ArrayType || asClassType(typeArgument) != null) {
            return Optional.of(typeArgument);
        }

        return Optional.empty();
    }

    /**
     * Returns all interfaces transitively implemented by the given type, including
     * interfaces implemented by superclasses.
     *
     * <p>The interfaces are returned as declared by each class, without substituting
     * the type arguments of the given type.
     *
     * @param typeMirror the type mirror to inspect
     * @return a set of all implemented interface type mirrors
     */
    public Set<? extends TypeMirror> getAllImplementedInterfaces(TypeMirror typeMirror) {
        var results = new HashSet<TypeMirror>();
        var types = new LinkedList<TypeElement>();
        if (asClassType(typeMirror) instanceof DeclaredType declaredType
                && declaredType.asElement() instanceof TypeElement typeElement) {
            types.add(typeElement);
        }
        while (!types.isEmpty()) {
            var typeElement = types.removeFirst();
            getDirectSuperClass(typeElement)
                    .ifPresent(types::add);
            for (var interfaceMirror : getClassInfo(typeElement).interfaces()) {
                results.add(interfaceMirror);
                if (asClassType(interfaceMirror) instanceof DeclaredType declaredType
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
        if (mirror instanceof ArrayType) {
            return Optional.of(objectType);
        }

        var classType = asClassType(mirror);
        if (classType != null && getClassInfo((TypeElement) classType.asElement()).superclass() instanceof DeclaredType superclass) {
            return Optional.of(superclass);
        }

        return Optional.empty();
    }

    /**
     * Returns the direct superclass of the given type element, if one exists.
     *
     * @param typeElement the type element to inspect
     * @return an {@link Optional} containing the superclass type element, or empty if
     *         no superclass exists
     */
    public Optional<TypeElement> getDirectSuperClass(TypeElement typeElement) {
        var superClassType = asClassType(getClassInfo(typeElement).superclass());
        if (superClassType == null) {
            return Optional.empty();
        }

        if (!(superClassType.asElement() instanceof TypeElement superClassElement)) {
            return Optional.empty();
        }

        return Optional.of(superClassElement);
    }

    /**
     * Returns the nearest enclosing type element for the given element.
     *
     * <p>If the element is itself a type element, it is returned directly. Otherwise,
     * this method walks up the enclosing element chain until a type element is found.
     *
     * @param element the element whose enclosing type to find
     * @return the nearest enclosing type element
     */
    public TypeElement getEnclosingTypeElement(Element element) {
        Objects.requireNonNull(element);
        if (element instanceof TypeElement typeElement) {
            return typeElement;
        }
        return getEnclosingTypeElement(element.getEnclosingElement());
    }

    /**
     * Returns the name of the package that owns the input type element
     *
     * @param typeElement the non-null type element
     * @return a package name
     */
    public String getPackageName(TypeElement typeElement) {
        Objects.requireNonNull(typeElement);
        var pkg = processingEnv.getElementUtils().getPackageOf(typeElement);
        return pkg == null || pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString();
    }

    /**
     * Returns the name of the module that owns the input type element
     *
     * @param typeElement the non-null type element
     * @return a package name
     */
    String getModuleName(TypeElement typeElement) {
        Objects.requireNonNull(typeElement);
        var module = processingEnv.getElementUtils().getModuleOf(typeElement);
        return module == null || module.isUnnamed() ? "" : module.getQualifiedName().toString();
    }

    /**
     * Resolves the classes referenced by the given type, so that the type can later
     * be used concurrently without waiting for a class to be resolved.
     *
     * <p>Every operation of this class resolves the classes it needs on demand, so
     * calling this method is never required for correctness: it only moves the cost
     * of resolution out of parallel code.
     *
     * @param type the type to resolve
     */
    public void resolve(TypeMirror type) {
        synchronized (resolutionLock) {
            resolveType(type);
        }
    }

    /**
     * Resolves the given method and the classes referenced by its signature, so
     * that it can later be used concurrently without waiting for a class to be
     * resolved.
     *
     * @param method the method to resolve
     */
    public void resolve(ExecutableElement method) {
        getMethodInfo(method);
    }

    /**
     * Returns the snapshot of the given class, resolving it if necessary.
     *
     * @param element the class
     * @return the snapshot of the class
     */
    private ClassInfo getClassInfo(TypeElement element) {
        var result = classes.get(element);
        if (result != null) {
            return result;
        }

        synchronized (resolutionLock) {
            return resolveClass(element);
        }
    }

    /**
     * Returns the snapshot of the given method, resolving it if necessary.
     *
     * @param method the method
     * @return the snapshot of the method
     */
    private MethodInfo getMethodInfo(ExecutableElement method) {
        var result = methods.get(method);
        if (result != null) {
            return result;
        }

        synchronized (resolutionLock) {
            return resolveMethod(method);
        }
    }

    /**
     * Loads the given class and records its snapshot, then resolves every class
     * referenced by its type parameters, supertypes and enclosing type.
     *
     * <p>The snapshot is published before the referenced classes are resolved, so
     * that cycles, like the one between {@link Enum} and its type parameter, end.
     * This is safe as readers resolve every class they need through
     * {@link #getClassInfo(TypeElement)}, and wait for this lock to be released if
     * that class is not resolved yet. A class that cannot be loaded is recorded
     * as erroneous, with no type parameters and no supertypes. This method must
     * only be invoked while holding the resolution lock.
     *
     * @param element the class to resolve
     * @return the snapshot of the class
     */
    private ClassInfo resolveClass(TypeElement element) {
        var existing = classes.get(element);
        if (existing != null) {
            return existing;
        }

        var declaredType = (DeclaredType) element.asType();
        if (declaredType.getKind() != TypeKind.DECLARED) {
            var erroneous = new ClassInfo(true, declaredType, List.of(), declaredType.getEnclosingType(), processingEnv.getTypeUtils().getNoType(TypeKind.NONE), List.of(), Set.of());
            classes.put(element, erroneous);
            return erroneous;
        }

        var typeParameters = List.copyOf(declaredType.getTypeArguments());
        var erasure = (DeclaredType) processingEnv.getTypeUtils().erasure(declaredType);
        var annotations = element.getAnnotationMirrors()
                .stream()
                .map(annotation -> ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString())
                .collect(Collectors.toUnmodifiableSet());
        var info = new ClassInfo(
                false,
                erasure,
                typeParameters,
                declaredType.getEnclosingType(),
                element.getSuperclass(),
                List.copyOf(element.getInterfaces()),
                annotations
        );

        // The factory that creates parameterized types lazily computes whether the
        // enclosing type of the class is parameterized: compute it now
        if (!typeParameters.isEmpty()) {
            try {
                processingEnv.getTypeUtils().getDeclaredType(element, typeParameters.toArray(TypeMirror[]::new));
            } catch (IllegalArgumentException _) {
                // The enclosing type is parameterized: the factory that takes the containing type is used instead
            }
        }

        classes.put(element, info);

        for (var typeParameter : typeParameters) {
            resolveType(((TypeVariable) typeParameter).getUpperBound());
        }
        resolveType(info.enclosingType());
        resolveType(info.superclass());
        for (var implemented : info.interfaces()) {
            resolveType(implemented);
        }
        return info;
    }

    /**
     * Records the snapshot of the given method, then resolves every class
     * referenced by its signature.
     *
     * <p>This method must only be invoked while holding the resolution lock.
     *
     * @param method the method to resolve
     * @return the snapshot of the method
     */
    private MethodInfo resolveMethod(ExecutableElement method) {
        var existing = methods.get(method);
        if (existing != null) {
            return existing;
        }

        if (method.getEnclosingElement() instanceof TypeElement owner) {
            resolveClass(owner);
        }

        var parameterTypes = method.getParameters()
                .stream()
                .map(VariableElement::asType)
                .toList();
        var receiverType = method.getReceiverType();
        var info = new MethodInfo(
                method.getReturnType(),
                parameterTypes,
                receiverType != null && receiverType.getKind() != TypeKind.NONE,
                method.isVarArgs()
        );
        methods.put(method, info);

        resolveType(info.returnType());
        for (var parameterType : parameterTypes) {
            resolveType(parameterType);
        }
        for (var typeParameter : method.getTypeParameters()) {
            resolveType(((TypeVariable) typeParameter.asType()).getUpperBound());
        }
        return info;
    }

    /**
     * Resolves every class referenced by the given type.
     *
     * <p>A type variable is resolved through the class or method that declares
     * it, which resolves its bounds exactly once. This method must only be
     * invoked while holding the resolution lock.
     *
     * @param type the type to resolve
     */
    private void resolveType(TypeMirror type) {
        switch (type) {
            case IntersectionType intersectionType -> {
                for (var bound : intersectionType.getBounds()) {
                    resolveType(bound);
                }
            }
            case UnionType unionType -> {
                for (var alternative : unionType.getAlternatives()) {
                    resolveType(alternative);
                }
            }
            case DeclaredType declaredType -> {
                if (!(declaredType.asElement() instanceof TypeElement element) || resolveClass(element).erroneous()) {
                    return;
                }

                for (var typeArgument : declaredType.getTypeArguments()) {
                    resolveType(typeArgument);
                }
                resolveType(declaredType.getEnclosingType());
            }
            case ArrayType arrayType -> resolveType(arrayType.getComponentType());
            case TypeVariable typeVariable -> {
                if (typeVariable.asElement() instanceof TypeParameterElement typeParameter) {
                    switch (typeParameter.getGenericElement()) {
                        case TypeElement typeElement -> resolveClass(typeElement);
                        case ExecutableElement executableElement -> resolveMethod(executableElement);
                        default -> {
                        }
                    }
                }
            }
            case WildcardType wildcardType -> {
                if (wildcardType.getExtendsBound() != null) {
                    resolveType(wildcardType.getExtendsBound());
                }
                if (wildcardType.getSuperBound() != null) {
                    resolveType(wildcardType.getSuperBound());
                }
            }
            case null, default -> {
            }
        }
    }

    /**
     * An immutable snapshot of a resolved class.
     *
     * @param erroneous whether the class could not be loaded
     * @param erasure the raw type of the class
     * @param typeParameters the type variables declared by the class
     * @param enclosingType the enclosing type of the declared type of the class, or a
     *        type of kind {@link TypeKind#NONE} if the class is not an inner class
     * @param superclass the declared superclass, or a type of kind {@link TypeKind#NONE}
     *        for {@link Object} and interfaces
     * @param interfaces the declared superinterfaces
     * @param annotations the canonical names of the annotations declared on the class
     */
    private record ClassInfo(
            boolean erroneous,
            DeclaredType erasure,
            List<? extends TypeMirror> typeParameters,
            TypeMirror enclosingType,
            TypeMirror superclass,
            List<? extends TypeMirror> interfaces,
            Set<String> annotations
    ) {

    }

    /**
     * An immutable snapshot of a resolved method.
     *
     * @param returnType the declared return type
     * @param parameterTypes the declared parameter types
     * @param receiver whether the method declares an explicit receiver parameter
     * @param varArgs whether the method accepts a variable number of arguments
     */
    private record MethodInfo(
            TypeMirror returnType,
            List<TypeMirror> parameterTypes,
            boolean receiver,
            boolean varArgs
    ) {

    }
}
