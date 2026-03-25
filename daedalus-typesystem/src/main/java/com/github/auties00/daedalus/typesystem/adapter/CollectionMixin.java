package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
}
