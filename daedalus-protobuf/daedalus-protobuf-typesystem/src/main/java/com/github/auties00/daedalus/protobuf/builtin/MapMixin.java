package com.github.auties00.daedalus.protobuf.builtin;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufDefaultValue;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.protobuf.annotation.ProtobufMixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
@ProtobufMixin
public final class MapMixin {
    @ProtobufDefaultValue
    public static <K, V> Map<K, V> newMap() {
        return new HashMap<>();
    }

    @ProtobufDefaultValue
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    @ProtobufDefaultValue
    public static <K, V> SequencedMap<K, V> newSequencedMap() {
        return new LinkedHashMap<>();
    }

    @ProtobufDefaultValue
    public static <K extends Comparable<? extends K>, V> NavigableMap<K, V> newNavigableMap() {
        return new TreeMap<>();
    }

    @ProtobufDefaultValue
    public static <K extends Comparable<? extends K>, V> SortedMap<K, V> newSortedMap() {
        return new TreeMap<>();
    }

    @ProtobufMessage.UnknownFields.Setter
    public static void addUnknownField(Map<Long, Object> map, long index, Object value) {
        map.put(index, value);
    }
}
