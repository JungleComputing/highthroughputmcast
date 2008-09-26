package mcast.p2p.admin;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class PieceIndexBooleanSet extends AbstractPieceIndexSet
        implements PieceIndexSet {

    private static final int DEFAULT_CAPACITY = 16;

    private boolean[] list;
    private int size;

    PieceIndexBooleanSet() {
        init(DEFAULT_CAPACITY);
    }

    PieceIndexBooleanSet(int capacity) {
        init(capacity);
    }

    private void init(int capacity) {
        list = new boolean[capacity];
        size = 0;
    }

    public void clear() {
        init(DEFAULT_CAPACITY);
    }

    PieceIndexBooleanSet(ReadMessage m) throws IOException {
        size = m.readInt();

        int capacity = m.readInt();
        list = new boolean[capacity];

        m.readArray(list);
    }

    PieceIndexBooleanSet(PieceIndexBooleanSet original) {
        size = original.size;

        list = new boolean[original.list.length];
        System.arraycopy(original.list, 0, list, 0, original.list.length);
    }

    PieceIndexBooleanSet(boolean[] b) {
        this.list = new boolean[b.length];
        System.arraycopy(b, 0, list, 0, b.length);

        size = 0;

        for (int i = 0; i < list.length; i++) {
            if (list[i]) {
                size++;
            }
        }
    }

    PieceIndexBooleanSet(String desc) throws NumberFormatException {
        if (desc == null) {
            throw new IllegalArgumentException("description cannot be null");
        }
        if (desc.length() < 2) {
            throw new IllegalArgumentException("missing enclosing brackets");
        }
        if (desc.charAt(0) != '[') {
            throw new IllegalArgumentException(
            "description must start with a '['");
        }
        if (desc.charAt(desc.length() - 1) != ']') {
            throw new IllegalArgumentException(
            "description must end with a '['");
        }

        init(DEFAULT_CAPACITY);

        StringTokenizer t = new StringTokenizer(desc.substring(1,
                desc.length() - 1), ",");

        while (t.hasMoreElements()) {
            String s = t.nextToken();

            int rangeSeparator = s.indexOf('-');

            if (rangeSeparator < 0) {
                // single element
                int element = Integer.parseInt(s);
                add(element);
            } else {
                // range of elements
                String firstElement = s.substring(0, rangeSeparator);
                int first = Integer.parseInt(firstElement);

                String lastElement = s
                .substring(rangeSeparator + 1, s.length());
                int last = Integer.parseInt(lastElement);

                add(first, last + 1);
            }
        }
    }

    public void writeTo(WriteMessage m) throws IOException {
        m.writeInt(size);
        m.writeInt(list.length);
        m.writeArray(list);
    }

    protected void ensureCapacity(int capacity, boolean exact) {
        if (list.length < capacity) {

            // increase list length

            int newSize = determineSize(capacity, exact);
            boolean[] newList = new boolean[newSize];
            System.arraycopy(list, 0, newList, 0, list.length);
            list = newList;
        }
    }

    protected int determineSize(int capacity, boolean exact) {
        if (exact) {
            return capacity;
        } else {
            // double the size of the current array until the capacity suffices
            int newSize = list.length;

            while (newSize < capacity) {
                newSize <<= 1; // times 2
            }

            return newSize;
        }
    }

    public void add(int index) {
        ensureCapacity(index + 1, false);

        if (!list[index]) {
            // bit was not present yet
            list[index] = true;
            size++;
        }
    }

    public void add(int start, int end) {
        ensureCapacity(end, false);

        for (int i = start; i < end; i++) {
            if (!list[i]) {
                // bit was not present yet
                list[i] = true;
                size++;
            }
        }
    }

    public PieceIndexSet and(PieceIndexSet other) {
        PieceIndexBooleanSet result = new PieceIndexBooleanSet(list.length);

        for (int i = 0; i < list.length; i++) {
            if (list[i] && other.contains(i)) {
                result.list[i] = true;
                result.size++;
            }
        }

        return result;
    }

    public PieceIndexSet or(PieceIndexSet other) {
        PieceIndexBooleanSet result = new PieceIndexBooleanSet(list.length);

        System.arraycopy(list, 0, result.list, 0, list.length);

        result.size = size;

        for (Integer i : other) {
            result.add(i);
        }

        return result;
    }

    public PieceIndexSet not(int lastIndex) {
        int minSize = Math.min(list.length, lastIndex);
        int maxSize = Math.max(list.length, lastIndex);

        PieceIndexBooleanSet result = new PieceIndexBooleanSet(maxSize);

        for (int i = 0; i < minSize; i++) {
            if (!list[i]) {
                result.list[i] = true;
                result.size++;
            }
        }

        for (int i = list.length; i < lastIndex; i++) {
            result.list[i] = true;
        }

        if (lastIndex > list.length) {
            result.size += lastIndex - list.length;
        }

        return result;
    }

    public boolean contains(int index) {
        if (index >= list.length) {
            // index lies outside the current list length, so it's not present
            return false;
        } else {
            return list[index];
        }
    }

    public void init(int offset, int length) {
        ensureCapacity(offset + length, true);
        Arrays.fill(list, offset, offset + length, true);
        size = length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean remove(int index) {
        if (index < list.length && list[index]) {
            list[index] = false;
            size--;
            return true;
        }

        return false;
    }

    protected PieceIndexSet createEmptySet() {
        return new PieceIndexBooleanSet();
    }

    protected PieceIndexSet doRemoveFirst(int amount) {
        int count = 0;
        int end = 0;

        while (count < amount) {
            if (list[end]) {
                count++;
            }
            end++;
        }

        PieceIndexBooleanSet result = new PieceIndexBooleanSet(end);

        System.arraycopy(list, 0, result.list, 0, end);
        result.size = amount;

        for (int i = 0; i < end; i++) {
            list[i] = false;
        }
        size -= amount;

        return result;
    }

    public int size() {
        return size;
    }

    public Iterator<Integer> iterator() {
        return new BooleanSetIterator();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof PieceIndexBooleanSet) {
            PieceIndexBooleanSet other = (PieceIndexBooleanSet) o;

            if (size != other.size) {
                return false;
            }

            int smallest = Math.min(list.length, other.list.length);

            for (int i = 0; i < smallest; i++) {
                if (list[i] != other.list[i]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        String concat = "";

        result.append('[');

        boolean inRange = false;
        int rangeStart = 0;
        int rangeEnd = 0;

        for (int i = 0; i < list.length; i++) {
            if (list[i]) {
                if (inRange) {
                    // increase the current range by one
                    rangeEnd++;

                    if (i == list.length - 1 || list[i + 1] == false) {
                        // last element in the current range
                        result.append(concat);
                        result.append(rangeStart);
                        result.append('-');
                        result.append(rangeEnd);

                        concat = ",";

                        inRange = false;
                    }
                } else if (i < list.length - 1 && list[i + 1] == true) {
                    // start of a new range
                    rangeStart = rangeEnd = i;
                    inRange = true;
                } else {
                    // individual element
                    result.append(concat);
                    result.append(i);

                    concat = ",";
                }
            }
        }

        result.append("]");

        return result.toString();
    }

    public PieceIndexSet deepCopy() {
        return new PieceIndexBooleanSet(this);
    }

    private class BooleanSetIterator implements Iterator<Integer> {

        int cursor;
        boolean movedToNext;

        BooleanSetIterator() {
            cursor = -1;
            movedToNext = false;
        }

        public boolean hasNext() {
            gotoNext();

            return cursor < list.length;
        }

        private void gotoNext() {
            if (!movedToNext) {
                do {
                    cursor++;
                } while (cursor < list.length && !list[cursor]);

                movedToNext = true;
            }
        }

        public Integer next() {
            gotoNext();

            if (cursor >= list.length) {
                throw new NoSuchElementException();
            } else {
                movedToNext = false;
                return new Integer(cursor);
            }
        }

        public void remove() {
            if (cursor < -1) {
                throw new IllegalStateException("next() not called yet");
            } else if (!list[cursor]) {
                throw new IllegalStateException("remove() was already called");
            }

            list[cursor] = false;
            size--;
        }

    }

}
