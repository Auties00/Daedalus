package it.auties.protobuf.builtin;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CollectionMixinTest {

    // Return type checks

    @Test
    void newCollectionReturnsArrayList() {
        var result = CollectionMixin.newCollection();
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void newSequencedCollectionReturnsArrayList() {
        var result = CollectionMixin.newSequencedCollection();
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void newListReturnsArrayList() {
        var result = CollectionMixin.newList();
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void newSetReturnsHashSet() {
        var result = CollectionMixin.newSet();
        assertInstanceOf(HashSet.class, result);
    }

    @Test
    void newQueueReturnsLinkedList() {
        var result = CollectionMixin.newQueue();
        assertInstanceOf(LinkedList.class, result);
    }

    @Test
    void newDequeReturnsLinkedList() {
        var result = CollectionMixin.newDeque();
        assertInstanceOf(LinkedList.class, result);
    }

    @Test
    void newSequencedSetReturnsLinkedHashSet() {
        var result = CollectionMixin.newSequencedSet();
        assertInstanceOf(LinkedHashSet.class, result);
    }

    @Test
    void newKeySetReturnsConcurrentKeySet() {
        var result = CollectionMixin.newKeySet();
        assertInstanceOf(ConcurrentHashMap.KeySetView.class, result);
    }

    // All returned collections are empty

    @Test
    void newCollectionIsEmpty() {
        assertTrue(CollectionMixin.newCollection().isEmpty());
    }

    @Test
    void newSequencedCollectionIsEmpty() {
        assertTrue(CollectionMixin.newSequencedCollection().isEmpty());
    }

    @Test
    void newListIsEmpty() {
        assertTrue(CollectionMixin.newList().isEmpty());
    }

    @Test
    void newSetIsEmpty() {
        assertTrue(CollectionMixin.newSet().isEmpty());
    }

    @Test
    void newQueueIsEmpty() {
        assertTrue(CollectionMixin.newQueue().isEmpty());
    }

    @Test
    void newDequeIsEmpty() {
        assertTrue(CollectionMixin.newDeque().isEmpty());
    }

    @Test
    void newSequencedSetIsEmpty() {
        assertTrue(CollectionMixin.newSequencedSet().isEmpty());
    }

    @Test
    void newKeySetIsEmpty() {
        assertTrue(CollectionMixin.newKeySet().isEmpty());
    }

    // All returned collections are mutable

    @Test
    void newCollectionIsMutable() {
        Collection<String> c = CollectionMixin.newCollection();
        c.add("a");
        assertEquals(1, c.size());
        assertTrue(c.contains("a"));
    }

    @Test
    void newSequencedCollectionIsMutable() {
        SequencedCollection<String> c = CollectionMixin.newSequencedCollection();
        c.add("a");
        assertEquals(1, c.size());
    }

    @Test
    void newListIsMutable() {
        List<String> list = CollectionMixin.newList();
        list.add("a");
        list.add("b");
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    void newSetIsMutable() {
        Set<Integer> set = CollectionMixin.newSet();
        set.add(1);
        set.add(2);
        set.add(1);
        assertEquals(2, set.size());
    }

    @Test
    void newQueueIsMutable() {
        Queue<String> q = CollectionMixin.newQueue();
        q.offer("first");
        q.offer("second");
        assertEquals("first", q.poll());
        assertEquals("second", q.poll());
    }

    @Test
    void newDequeIsMutable() {
        Deque<String> d = CollectionMixin.newDeque();
        d.addFirst("a");
        d.addLast("b");
        assertEquals("a", d.peekFirst());
        assertEquals("b", d.peekLast());
    }

    @Test
    void newSequencedSetIsMutable() {
        SequencedSet<String> s = CollectionMixin.newSequencedSet();
        s.add("a");
        s.add("b");
        assertEquals(2, s.size());
    }

    @Test
    void newKeySetIsMutable() {
        var ks = CollectionMixin.newKeySet();
        ks.add("a");
        ks.add("b");
        assertEquals(2, ks.size());
    }

    // Distinct instances

    @Test
    void newCollectionReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newCollection(), CollectionMixin.newCollection());
    }

    @Test
    void newListReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newList(), CollectionMixin.newList());
    }

    @Test
    void newSetReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newSet(), CollectionMixin.newSet());
    }

    @Test
    void newQueueReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newQueue(), CollectionMixin.newQueue());
    }

    @Test
    void newDequeReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newDeque(), CollectionMixin.newDeque());
    }

    @Test
    void newSequencedSetReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newSequencedSet(), CollectionMixin.newSequencedSet());
    }

    @Test
    void newKeySetReturnsDistinctInstances() {
        assertNotSame(CollectionMixin.newKeySet(), CollectionMixin.newKeySet());
    }

    // SequencedSet preserves insertion order

    @Test
    void newSequencedSetPreservesInsertionOrder() {
        SequencedSet<String> s = CollectionMixin.newSequencedSet();
        s.add("c");
        s.add("a");
        s.add("b");
        var it = s.iterator();
        assertEquals("c", it.next());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
    }
}
