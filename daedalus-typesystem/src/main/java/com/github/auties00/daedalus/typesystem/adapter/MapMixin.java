package com.github.auties00.daedalus.typesystem.adapter;

import com.github.auties00.daedalus.typesystem.annotation.TypeBuilder;
import com.github.auties00.daedalus.typesystem.annotation.TypeDefaultValue;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import static com.github.auties00.daedalus.typesystem.annotation.TypeBuilder.FIELD_NAME;

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

    @TypeBuilder.Mixin(builderMethodName = {"put", FIELD_NAME})
    public static <K, V> Map<K, V> putEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            var result = new HashMap<K, V>();
            result.put(key, value);
            return result;
        } else {
            try {
                map.put(key, value);
                return map;
            } catch (UnsupportedOperationException _) {
                var result = new HashMap<>(map);
                result.put(key, value);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"put", FIELD_NAME})
    public static <K, V> Map<K, V> putAll(Map<K, V> map, Map<? extends K, ? extends V> entries) {
        if (map == null) {
            return new HashMap<>(entries);
        } else {
            try {
                map.putAll(entries);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size() + entries.size());
                result.putAll(map);
                result.putAll(entries);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"put", FIELD_NAME, "IfAbsent"})
    public static <K, V> Map<K, V> putIfAbsent(Map<K, V> map, K key, V value) {
        if (map == null) {
            var result = new HashMap<K, V>();
            result.put(key, value);
            return result;
        } else {
            try {
                map.putIfAbsent(key, value);
                return map;
            } catch (UnsupportedOperationException _) {
                var result = new HashMap<>(map);
                result.putIfAbsent(key, value);
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"remove", FIELD_NAME})
    public static <K, V> Map<K, V> removeEntry(Map<K, V> map, K key) {
        if (map == null) {
            return new HashMap<>();
        } else {
            try {
                map.remove(key);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size());
                map.forEach((k, v) -> {
                    if (!Objects.equals(k, key)) {
                        result.put(k, v);
                    }
                });
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"remove", FIELD_NAME})
    public static <K, V> Map<K, V> removeEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            return new HashMap<>();
        } else {
            try {
                map.remove(key, value);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size());
                map.forEach((k, v) -> {
                    if (!(Objects.equals(k, key) && Objects.equals(v, value))) {
                        result.put(k, v);
                    }
                });
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"replace", FIELD_NAME})
    public static <K, V> Map<K, V> replaceEntry(Map<K, V> map, K key, V value) {
        if (map == null) {
            return new HashMap<>();
        } else {
            try {
                map.replace(key, value);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size());
                map.forEach((k, v) -> result.put(k, Objects.equals(k, key) ? value : v));
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"replace", FIELD_NAME})
    public static <K, V> Map<K, V> replaceEntry(Map<K, V> map, K key, V oldValue, V newValue) {
        if (map == null) {
            return new HashMap<>();
        } else {
            try {
                map.replace(key, oldValue, newValue);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size());
                map.forEach((k, v) -> result.put(k, Objects.equals(k, key) && Objects.equals(v, oldValue) ? newValue : v));
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"replace", FIELD_NAME})
    public static <K, V> Map<K, V> replaceAll(Map<K, V> map, BiFunction<? super K, ? super V, ? extends V> function) {
        if (map == null) {
            return new HashMap<>();
        } else {
            try {
                map.replaceAll(function);
                return map;
            } catch (UnsupportedOperationException _) {
                HashMap<K, V> result = HashMap.newHashMap(map.size());
                map.forEach((k, v) -> result.put(k, function.apply(k, v)));
                return result;
            }
        }
    }

    @TypeBuilder.Mixin(builderMethodName = {"clear", FIELD_NAME})
    public static <K, V> Map<K, V> clear(Map<K, V> map) {
        return new HashMap<>();
    }
}
