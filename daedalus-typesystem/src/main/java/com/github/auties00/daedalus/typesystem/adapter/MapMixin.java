package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link TypeMixin} that provides default values for the standard {@link Map} types in the {@code java.util} and {@code java.util.concurrent} packages.
 */
@SuppressWarnings("unused")
@TypeMixin
public final class MapMixin {
    private MapMixin() {
        throw new UnsupportedOperationException("MapMixin is a mixin and cannot be instantiated");
    }

    @TypeDefaultValue
    public static <K, V> Map<K, V> newMap() {
        return new HashMap<>();
    }

    @TypeDefaultValue
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    @TypeDefaultValue
    public static <K, V> SequencedMap<K, V> newSequencedMap() {
        return new LinkedHashMap<>();
    }

    @TypeDefaultValue
    public static <K extends Comparable<? extends K>, V> NavigableMap<K, V> newNavigableMap() {
        return new TreeMap<>();
    }

    @TypeDefaultValue
    public static <K extends Comparable<? extends K>, V> SortedMap<K, V> newSortedMap() {
        return new TreeMap<>();
    }
}
