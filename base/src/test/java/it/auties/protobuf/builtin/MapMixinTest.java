package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MapMixinTest {

    // Return type checks

    @Test
    void newMapReturnsHashMap() {
        assertInstanceOf(HashMap.class, MapMixin.newMap());
    }

    @Test
    void newConcurrentMapReturnsConcurrentHashMap() {
        assertInstanceOf(ConcurrentHashMap.class, MapMixin.newConcurrentMap());
    }

    @Test
    void newSequencedMapReturnsLinkedHashMap() {
        assertInstanceOf(LinkedHashMap.class, MapMixin.newSequencedMap());
    }

    @Test
    void newNavigableMapReturnsTreeMap() {
        assertInstanceOf(TreeMap.class, MapMixin.newNavigableMap());
    }

    @Test
    void newSortedMapReturnsTreeMap() {
        assertInstanceOf(TreeMap.class, MapMixin.newSortedMap());
    }

    // All returned maps are empty

    @Test
    void newMapIsEmpty() {
        assertTrue(MapMixin.newMap().isEmpty());
    }

    @Test
    void newConcurrentMapIsEmpty() {
        assertTrue(MapMixin.newConcurrentMap().isEmpty());
    }

    @Test
    void newSequencedMapIsEmpty() {
        assertTrue(MapMixin.newSequencedMap().isEmpty());
    }

    @Test
    void newNavigableMapIsEmpty() {
        assertTrue(MapMixin.newNavigableMap().isEmpty());
    }

    @Test
    void newSortedMapIsEmpty() {
        assertTrue(MapMixin.newSortedMap().isEmpty());
    }

    // All returned maps are mutable

    @Test
    void newMapIsMutable() {
        Map<String, Integer> map = MapMixin.newMap();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    void newConcurrentMapIsMutable() {
        var map = MapMixin.newConcurrentMap();
        map.put("a", 1);
        assertEquals(1, map.size());
    }

    @Test
    void newSequencedMapIsMutable() {
        SequencedMap<String, Integer> map = MapMixin.newSequencedMap();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
    }

    @Test
    void newNavigableMapIsMutable() {
        NavigableMap<String, Integer> map = MapMixin.newNavigableMap();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
    }

    @Test
    void newSortedMapIsMutable() {
        SortedMap<String, Integer> map = MapMixin.newSortedMap();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
    }

    // Distinct instances

    @Test
    void newMapReturnsDistinctInstances() {
        assertNotSame(MapMixin.newMap(), MapMixin.newMap());
    }

    @Test
    void newConcurrentMapReturnsDistinctInstances() {
        assertNotSame(MapMixin.newConcurrentMap(), MapMixin.newConcurrentMap());
    }

    @Test
    void newSequencedMapReturnsDistinctInstances() {
        assertNotSame(MapMixin.newSequencedMap(), MapMixin.newSequencedMap());
    }

    @Test
    void newNavigableMapReturnsDistinctInstances() {
        assertNotSame(MapMixin.newNavigableMap(), MapMixin.newNavigableMap());
    }

    @Test
    void newSortedMapReturnsDistinctInstances() {
        assertNotSame(MapMixin.newSortedMap(), MapMixin.newSortedMap());
    }

    // SequencedMap preserves insertion order

    @Test
    void newSequencedMapPreservesInsertionOrder() {
        SequencedMap<String, Integer> map = MapMixin.newSequencedMap();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        var keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("c", "a", "b"), keys);
    }

    // NavigableMap sorts by natural order

    @Test
    void newNavigableMapSortsByNaturalOrder() {
        NavigableMap<String, Integer> map = MapMixin.newNavigableMap();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        var keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("a", "b", "c"), keys);
    }

    @Test
    void newNavigableMapSupportsNavigationOperations() {
        NavigableMap<Integer, String> map = MapMixin.newNavigableMap();
        map.put(1, "one");
        map.put(3, "three");
        map.put(5, "five");
        assertEquals(1, map.firstKey());
        assertEquals(5, map.lastKey());
        assertEquals(3, map.floorKey(4));
        assertEquals(5, map.ceilingKey(4));
    }

    // SortedMap sorts by natural order

    @Test
    void newSortedMapSortsByNaturalOrder() {
        SortedMap<String, Integer> map = MapMixin.newSortedMap();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    // addUnknownField

    @Test
    void addUnknownFieldPutsEntry() {
        Map<Long, Object> map = new HashMap<>();
        MapMixin.addUnknownField(map, 1L, "value1");
        assertEquals(1, map.size());
        assertEquals("value1", map.get(1L));
    }

    @Test
    void addUnknownFieldPutsMultipleEntries() {
        Map<Long, Object> map = new HashMap<>();
        MapMixin.addUnknownField(map, 1L, "value1");
        MapMixin.addUnknownField(map, 2L, 42);
        MapMixin.addUnknownField(map, 3L, true);
        assertEquals(3, map.size());
        assertEquals("value1", map.get(1L));
        assertEquals(42, map.get(2L));
        assertEquals(true, map.get(3L));
    }

    @Test
    void addUnknownFieldOverwritesDuplicateIndex() {
        Map<Long, Object> map = new HashMap<>();
        MapMixin.addUnknownField(map, 1L, "old");
        MapMixin.addUnknownField(map, 1L, "new");
        assertEquals(1, map.size());
        assertEquals("new", map.get(1L));
    }

    @Test
    void addUnknownFieldWithNullValue() {
        Map<Long, Object> map = new HashMap<>();
        MapMixin.addUnknownField(map, 1L, null);
        assertEquals(1, map.size());
        assertNull(map.get(1L));
    }

    @Test
    void addUnknownFieldWithLargeIndex() {
        Map<Long, Object> map = new HashMap<>();
        MapMixin.addUnknownField(map, Long.MAX_VALUE, "max");
        assertEquals("max", map.get(Long.MAX_VALUE));
    }
}
