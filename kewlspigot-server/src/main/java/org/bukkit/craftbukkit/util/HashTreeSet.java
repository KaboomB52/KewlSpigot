package org.bukkit.craftbukkit.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

// MinetickMod start
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
// MinetickMod end

public class HashTreeSet<V> implements Set<V> {

    private LinkedHashSet<V> hash = new LinkedHashSet<V>(); // MinetickMod - HashSet -> LinkedHashSet
    private PriorityQueue<V> tree = new PriorityQueue<V>(); // MinetickMod - TreeSet -> PriorityQueue

    public HashTreeSet() {

    }

    // MinetickMod start
    public boolean checkConsistency() {
        int sizeHash = hash.size();
        int sizeQueue = tree.size();
        if(sizeHash != sizeQueue) {
            if(sizeHash > sizeQueue) {
                tree.clear();
                tree.addAll(hash);
            } else if(sizeHash < sizeQueue) {
                hash.clear();
                hash.addAll(tree);
            }
            return false;
        }
        return true;
    }
    // MinetickMod end

    @Override
    public int size() {
        return hash.size();
    }

    @Override
    public boolean isEmpty() {
        return hash.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return hash.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>() {

            private Iterator<V> it = tree.iterator();
            private V last;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public V next() {
                return last = it.next();
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new IllegalStateException();
                }
                it.remove();
                hash.remove(last);
                last = null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return hash.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return hash.toArray(a);
    }

    @Override
    public boolean add(V e) {
        hash.add(e);
        return tree.add(e);
    }

    @Override
    public boolean remove(Object o) {
        hash.remove(o);
        return tree.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        return hash.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
        tree.addAll(c);
        return hash.addAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
        tree.retainAll(c);
        return hash.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        tree.removeAll(c);
        return hash.removeAll(c);
    }

    @Override
    public void clear() {
        hash.clear();
        tree.clear();
    }

    public V first() {
        return tree.peek(); // MinetickMod
    }

}
