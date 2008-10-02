package mcast.ht.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class WeightedSet<T> {

    private LinkedList<WeightedObject<T>> list;
    private int totalWeight;

    public WeightedSet() {
        list = new LinkedList<WeightedObject<T>>();
        totalWeight = 0;
    }

    public void add(T object, int weight) {
        Defense.checkNotNegative(weight, "weight");

        list.add(new WeightedObject<T>(object, weight));
        totalWeight += weight;
    }

    public Object remove(Random random) {
        Defense.checkNotNull(random, "random");

        if (isEmpty()) {
            return null;
        }

        int selected = random.nextInt(totalWeight);

        int sumWeight = 0;
        ListIterator<WeightedObject<T>> listIt = list.listIterator();
        WeightedObject<T> result = null;

        do {
            result = listIt.next();
            sumWeight += result.weight;
        } while (selected >= sumWeight);

        listIt.remove();
        totalWeight -= result.weight;

        return result.object;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public Iterator<T> iterator() {
        ArrayList<T> objects = new ArrayList<T>(list.size());

        for (WeightedObject<T> o: list) {
            objects.add(o.object);
        }

        return objects.iterator();
    }

    private class WeightedObject<X> {

        X object;
        int weight;

        WeightedObject(X object, int weight) {
            this.object = object;
            this.weight = weight;
        }
    }

}
