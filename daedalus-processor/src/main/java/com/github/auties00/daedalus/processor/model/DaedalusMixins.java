package com.github.auties00.daedalus.processor.model;

import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import javax.lang.model.element.TypeElement;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

/**
 * The mixins that apply to a field, each with the narrowest {@linkplain TypeMixin.Scope scope}
 * through which it applies.
 *
 * <p>A mixin referenced explicitly by the field applies through {@link TypeMixin.Scope#MANUAL},
 * and a mixin that applies to the package, the module, or every type through
 * {@link TypeMixin.Scope#PACKAGE}, {@link TypeMixin.Scope#MODULE} or
 * {@link TypeMixin.Scope#GLOBAL}. A mixin imported by another one applies through the scope
 * of the mixin that imports it, and a mixin reachable through more than one scope keeps the
 * narrowest one.
 *
 * <p>The scopes order the mixins by precedence: a conversion only uses the mixins of a
 * broader scope when no conversion exists with the mixins of the narrower ones, so that an
 * explicit mixin always wins over a scoped one. The scopes that are searched, in order, are
 * the {@linkplain #tiers() tiers} of these mixins, and the mixins searched in a tier are
 * {@linkplain #upTo(TypeMixin.Scope) the ones that apply up to its scope}, which is a view
 * that is not copied.
 *
 * <p>Instances of this class are immutable and thread safe, and are created through
 * {@link #empty()}, {@link #of(Collection)} and {@link #builder()}.
 */
public final class DaedalusMixins {
    /**
     * The scopes, from the narrowest to the broadest.
     */
    private static final TypeMixin.Scope[] SCOPES = TypeMixin.Scope.values();

    /**
     * The mixins of a field that has none.
     */
    private static final DaedalusMixins EMPTY = new DaedalusMixins(Map.of(), new EnumMap<>(TypeMixin.Scope.class));

    /**
     * The narrowest scope of each mixin, indexed by its qualified name.
     */
    private final Map<String, TypeMixin.Scope> scopes;

    /**
     * The mixins introduced by each scope, which do not apply through a narrower one.
     */
    private final EnumMap<TypeMixin.Scope, Set<TypeElement>> introduced;

    /**
     * The scopes that are searched, from the narrowest to the broadest.
     */
    private final SequencedSet<TypeMixin.Scope> tiers;

    /**
     * Constructs mixins from the given scopes.
     *
     * @param scopes the narrowest scope of each mixin, indexed by its qualified name
     * @param introduced the mixins introduced by each scope
     */
    private DaedalusMixins(Map<String, TypeMixin.Scope> scopes, EnumMap<TypeMixin.Scope, Set<TypeElement>> introduced) {
        this.scopes = scopes;
        this.introduced = introduced;
        var tiers = new LinkedHashSet<TypeMixin.Scope>();
        tiers.add(TypeMixin.Scope.MANUAL);
        for (var scope : SCOPES) {
            if (introduced.containsKey(scope)) {
                tiers.add(scope);
            }
        }
        this.tiers = Collections.unmodifiableSequencedSet(tiers);
    }

    /**
     * Returns the mixins of a field that has none.
     *
     * @return the empty mixins
     */
    public static DaedalusMixins empty() {
        return EMPTY;
    }

    /**
     * Returns the mixins that apply through {@link TypeMixin.Scope#MANUAL}, because they are
     * referenced explicitly.
     *
     * @param mixins the mixins
     * @return the mixins
     * @throws NullPointerException if {@code mixins} is {@code null}
     */
    public static DaedalusMixins of(Collection<TypeElement> mixins) {
        return builder()
                .addAll(mixins, TypeMixin.Scope.MANUAL)
                .build();
    }

    /**
     * Returns a builder of mixins.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the scopes that a conversion searches, from the narrowest to the broadest.
     *
     * <p>The first tier is always {@link TypeMixin.Scope#MANUAL}, even if no mixin is
     * referenced explicitly, followed by each broader scope that introduces a mixin.
     *
     * @return a non empty, unmodifiable set of scopes
     */
    public SequencedSet<TypeMixin.Scope> tiers() {
        return tiers;
    }

    /**
     * Returns the mixins introduced by the given scope, which do not apply through a narrower
     * one.
     *
     * @param scope the scope
     * @return an unmodifiable set of mixins
     * @throws NullPointerException if {@code scope} is {@code null}
     */
    public Set<TypeElement> introducedIn(TypeMixin.Scope scope) {
        Objects.requireNonNull(scope, "scope cannot be null");
        return introduced.getOrDefault(scope, Set.of());
    }

    /**
     * Returns the mixins that apply through the given scope or through a narrower one.
     *
     * <p>The returned set is a view of these mixins, which is not copied.
     *
     * @param scope the broadest scope
     * @return an unmodifiable view of the mixins
     * @throws NullPointerException if {@code scope} is {@code null}
     */
    public Set<TypeElement> upTo(TypeMixin.Scope scope) {
        Objects.requireNonNull(scope, "scope cannot be null");
        return new ScopeView(scope);
    }

    /**
     * Returns every mixin that applies, whatever its scope.
     *
     * @return an unmodifiable view of the mixins
     */
    public Set<TypeElement> all() {
        return upTo(TypeMixin.Scope.GLOBAL);
    }

    /**
     * Returns the narrowest scope through which the given mixin applies.
     *
     * @param mixin the mixin
     * @return the scope of the mixin, or an empty optional if it does not apply
     * @throws NullPointerException if {@code mixin} is {@code null}
     */
    public Optional<TypeMixin.Scope> scopeOf(TypeElement mixin) {
        return Optional.ofNullable(scopes.get(mixin.getQualifiedName().toString()));
    }

    /**
     * Returns whether the mixin with the given qualified name applies through the given scope
     * or through a narrower one.
     *
     * <p>This method runs in constant time, so that it can be checked for every converter
     * that a conversion considers.
     *
     * @param qualifiedName the qualified name of the mixin
     * @param scope the broadest scope
     * @return {@code true} if the mixin applies up to the scope
     */
    public boolean appliesUpTo(String qualifiedName, TypeMixin.Scope scope) {
        var mixinScope = scopes.get(qualifiedName);
        return mixinScope != null && mixinScope.compareTo(scope) <= 0;
    }

    /**
     * Returns whether no mixin applies.
     *
     * @return {@code true} if no mixin applies through any scope
     */
    public boolean isEmpty() {
        return scopes.isEmpty();
    }

    /**
     * Compares these mixins to the given object.
     *
     * @param other the object to compare to
     * @return {@code true} if the object are mixins whose mixins apply through the same scopes
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof DaedalusMixins that && introduced.equals(that.introduced);
    }

    /**
     * Returns the hash code of these mixins.
     *
     * @return the hash code of the mixins introduced by each scope
     */
    @Override
    public int hashCode() {
        return introduced.hashCode();
    }

    /**
     * Returns a string representation of these mixins.
     *
     * @return a string that lists the mixins introduced by each scope
     */
    @Override
    public String toString() {
        return "DaedalusMixins" + introduced;
    }

    /**
     * A view of the mixins that apply up to a scope, which iterates the mixins introduced by
     * each scope, from the narrowest one.
     */
    private final class ScopeView extends AbstractSet<TypeElement> {
        /**
         * The broadest scope of the view.
         */
        private final TypeMixin.Scope scope;

        /**
         * Constructs a view of the mixins that apply up to the given scope.
         *
         * @param scope the broadest scope
         */
        private ScopeView(TypeMixin.Scope scope) {
            this.scope = scope;
        }

        @Override
        public boolean contains(Object object) {
            return object instanceof TypeElement mixin && appliesUpTo(mixin.getQualifiedName().toString(), scope);
        }

        @Override
        public int size() {
            var size = 0;
            for (var entry : introduced.entrySet()) {
                if (entry.getKey().compareTo(scope) <= 0) {
                    size += entry.getValue().size();
                }
            }
            return size;
        }

        @Override
        public Iterator<TypeElement> iterator() {
            return new Iterator<>() {
                private int nextScope = 0;
                private Iterator<TypeElement> current = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!current.hasNext() && nextScope <= scope.ordinal()) {
                        current = introducedIn(SCOPES[nextScope++]).iterator();
                    }
                    return current.hasNext();
                }

                @Override
                public TypeElement next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current.next();
                }
            };
        }
    }

    /**
     * A builder of mixins, which keeps the narrowest scope of a mixin added more than once.
     */
    public static final class Builder {
        /**
         * The narrowest scope of each mixin, in the order the mixins were added.
         */
        private final Map<TypeElement, TypeMixin.Scope> mixins;

        /**
         * Constructs an empty builder.
         */
        private Builder() {
            this.mixins = new LinkedHashMap<>();
        }

        /**
         * Adds a mixin that applies through the given scope.
         *
         * @param mixin the mixin
         * @param scope the scope through which it applies
         * @return this builder
         * @throws NullPointerException if {@code mixin} or {@code scope} is {@code null}
         */
        public Builder add(TypeElement mixin, TypeMixin.Scope scope) {
            Objects.requireNonNull(mixin, "mixin cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            mixins.merge(mixin, scope, (previous, next) -> previous.compareTo(next) <= 0 ? previous : next);
            return this;
        }

        /**
         * Adds mixins that apply through the given scope.
         *
         * @param mixins the mixins
         * @param scope the scope through which they apply
         * @return this builder
         * @throws NullPointerException if {@code mixins} or {@code scope} is {@code null}
         */
        public Builder addAll(Collection<TypeElement> mixins, TypeMixin.Scope scope) {
            for (var mixin : mixins) {
                add(mixin, scope);
            }
            return this;
        }

        /**
         * Builds the mixins.
         *
         * @return the mixins, which are {@linkplain #empty() empty} if none was added
         */
        public DaedalusMixins build() {
            if (mixins.isEmpty()) {
                return EMPTY;
            }

            var scopes = new LinkedHashMap<String, TypeMixin.Scope>();
            var introduced = new EnumMap<TypeMixin.Scope, Set<TypeElement>>(TypeMixin.Scope.class);
            for (var entry : mixins.entrySet()) {
                scopes.put(entry.getKey().getQualifiedName().toString(), entry.getValue());
                introduced.computeIfAbsent(entry.getValue(), _ -> new LinkedHashSet<>()).add(entry.getKey());
            }
            introduced.replaceAll((_, value) -> Collections.unmodifiableSet(value));
            return new DaedalusMixins(Collections.unmodifiableMap(scopes), introduced);
        }
    }
}
