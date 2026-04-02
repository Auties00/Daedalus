package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.github.auties00.daedalus.typesystem.annotation.TypeBuilder.FIELD_NAME;

/**
 * A {@link TypeMixin} that provides default values, serializers, and deserializers for the standard {@link Collection} types in the {@code java.util} package.
 *
 * @see TypeDefaultValue
 * @see TypeMixin
 */
@SuppressWarnings("unused")
@TypeMixin
public final class CollectionMixin {
    private CollectionMixin() {
        throw new UnsupportedOperationException("CollectionMixin is a mixin and cannot be instantiated");
    }

    @TypeDefaultValue
    public static <T> Collection<T> newCollection() {
        return new ArrayList<>();
    }

    @TypeDefaultValue
    public static <T> SequencedCollection<T> newSequencedCollection() {
        return new ArrayList<>();
    }

    @TypeDefaultValue
    public static <T> List<T> newList() {
        return new ArrayList<>();
    }

    @TypeDefaultValue
    public static <T> Set<T> newSet() {
        return new HashSet<>();
    }

    @TypeDefaultValue
    public static <T> Queue<T> newQueue() {
        return new LinkedList<>();
    }

    @TypeDefaultValue
    public static <T> Deque<T> newDeque() {
        return new LinkedList<>();
    }

    @TypeDefaultValue
    public static <T> SequencedSet<T> newSequencedSet() {
        return new LinkedHashSet<>();
    }

    @TypeDefaultValue
    public static <T> ConcurrentHashMap.KeySetView<T, Boolean> newKeySet() {
        return ConcurrentHashMap.newKeySet();
    }

    @TypeBuilder.Mixin(builderMethodName = {"add", FIELD_NAME})
    public static <T> Collection<T> addElement(Collection<T> collection, T value) {
        if (collection == null) {
            var result = new ArrayList<T>();
            result.add(value);
            return result;
        } else {
            try {
                collection.add(value);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<>(collection);
                result.add(value);
                return result;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @TypeBuilder.Mixin(builderMethodName = {"add", FIELD_NAME})
    public static <T> Collection<T> addElements(Collection<T> collection, T... values) {
        if (collection == null) {
            var result = new ArrayList<T>();
            Collections.addAll(result, values);
            return result;
        } else {
            try {
                Collections.addAll(collection, values);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<T>(collection.size() + values.length);
                result.addAll(collection);
                Collections.addAll(result, values);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"add", FIELD_NAME})
    public static <T> Collection<T> addAll(Collection<T> collection, Collection<? extends T> values) {
        if (collection == null) {
            return new ArrayList<>(values);
        } else {
            try {
                collection.addAll(values);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<T>(collection.size() + values.size());
                result.addAll(collection);
                result.addAll(values);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"remove", FIELD_NAME})
    public static <T> Collection<T> removeElement(Collection<T> collection, T value) {
        if (collection == null) {
            return new ArrayList<>();
        } else {
            try {
                collection.remove(value);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<>(collection);
                result.remove(value);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"remove", FIELD_NAME})
    public static <T> Collection<T> removeAll(Collection<T> collection, Collection<? extends T> values) {
        if (collection == null) {
            return new ArrayList<>();
        } else {
            try {
                collection.removeAll(values);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<T>(collection.size());
                for (var element : collection) {
                    if (!values.contains(element)) {
                        result.add(element);
                    }
                }
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"remove", FIELD_NAME, "If"})
    public static <T> Collection<T> removeIf(Collection<T> collection, Predicate<? super T> filter) {
        if (collection == null) {
            return new ArrayList<>();
        } else {
            try {
                collection.removeIf(filter);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<T>(collection.size());
                for (var element : collection) {
                    if (!filter.test(element)) {
                        result.add(element);
                    }
                }
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"retain", FIELD_NAME})
    public static <T> Collection<T> retainAll(Collection<T> collection, Collection<? extends T> values) {
        if (collection == null) {
            return new ArrayList<>();
        } else {
            try {
                collection.retainAll(values);
                return collection;
            } catch (UnsupportedOperationException _) {
                var result = new ArrayList<T>(collection.size());
                for (var element : collection) {
                    if (values.contains(element)) {
                        result.add(element);
                    }
                }
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"clear", FIELD_NAME})
    public static <T> Collection<T> clear(Collection<T> collection) {
        return new ArrayList<>();
    }
}
