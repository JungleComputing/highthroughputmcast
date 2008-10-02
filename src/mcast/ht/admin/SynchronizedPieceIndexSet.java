package mcast.ht.admin;

import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Iterator;

public class SynchronizedPieceIndexSet implements PieceIndexSet {

    private PieceIndexSet delegate;

    public SynchronizedPieceIndexSet(PieceIndexSet delegate) {
        this.delegate = delegate;
    }

    public synchronized void init(PieceIndexSet newDelegate) {
        delegate = newDelegate;
    }

    public synchronized void clear() {
        delegate.clear();
    }

    public synchronized void add(int start, int end) {
        delegate.add(start, end);
    }

    public synchronized void add(int index) {
        delegate.add(index);
    }

    public synchronized void addAll(PieceIndexSet indices) {
        delegate.addAll(indices);
    }

    public synchronized PieceIndexSet and(PieceIndexSet other) {
        return delegate.and(other);
    }

    public synchronized boolean contains(int index) {
        return delegate.contains(index);
    }

    public synchronized boolean containsAny(PieceIndexSet indices) {
        return delegate.containsAny(indices);
    }

    public synchronized PieceIndexSet deepCopy() {
        return delegate.deepCopy();
    }

    public synchronized void init(int offset, int amount) {
        delegate.init(offset, amount);
    }

    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    public synchronized Iterator<Integer> iterator() {
        return delegate.iterator();
    }

    public synchronized PieceIndexSet not(int lastIndex) {
        return delegate.not(lastIndex);
    }

    public synchronized PieceIndexSet or(PieceIndexSet other) {
        return delegate.or(other);
    }

    public synchronized boolean remove(int index) {
        return delegate.remove(index);
    }

    public synchronized boolean removeAll(PieceIndexSet indices) {
        return delegate.removeAll(indices);
    }

    public synchronized PieceIndexSet removeFirst(double fraction) {
        return delegate.removeFirst(fraction);
    }

    public synchronized int size() {
        return delegate.size();
    }

    public synchronized void writeTo(WriteMessage m) throws IOException {
        delegate.writeTo(m);
    }

    public synchronized String toString() {
        return delegate.toString();
    }

    public synchronized boolean equals(Object o) {
        return delegate.equals(o);
    }

    public synchronized int hashCode() {
        return delegate.hashCode();
    }

}
