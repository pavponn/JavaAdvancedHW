package ru.ifmo.rain.ponomarev.arrayset;

import java.util.*;


public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> list;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        list = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<? extends T> collection) {
        Objects.requireNonNull(collection);
        list = new ArrayList<>(new TreeSet<>(collection));
        comparator = null;
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(collection);
        this.comparator = comparator;
        NavigableSet<T> localTreeSet = new TreeSet<>(comparator);
        localTreeSet.addAll(collection);
        list = new ArrayList<>(localTreeSet);
    }


    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.list = list;
        this.comparator = comparator;
        if ((list instanceof DescendingList)) {
            ((DescendingList) list).reverseList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object object) {
        return Collections.binarySearch(list, (T) object, comparator) >= 0;
    }


    @Override
    public T lower(T e) {
        int i = findIndexFromTail(e, false);
        return checkIfInBounds(i) ? list.get(i) : null;
    }

    @Override
    public T floor(T e) {
        int i = findIndexFromTail(e, true);
        return checkIfInBounds(i) ? list.get(i) : null;
    }

    @Override
    public T ceiling(T e) {
        int i = findIndexFromHead(e, true);
        return checkIfInBounds(i) ? list.get(i) : null;
    }

    @Override
    public T higher(T e) {
        int i = findIndexFromHead(e, false);
        return checkIfInBounds(i) ? list.get(i) : null;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new DescendingList<>(list), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int leftIndex = findIndexFromHead(fromElement, fromInclusive);
        int rightIndex = findIndexFromTail(toElement, toInclusive);
        if (leftIndex == -1 || rightIndex == -1 || leftIndex > rightIndex) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
        return new ArraySet<>(list.subList(leftIndex, rightIndex + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (size() == 0) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (size() == 0) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (!list.isEmpty()) {
            return list.get(0);
        }
        throw new NoSuchElementException();
    }

    @Override
    public T last() {
        if (!list.isEmpty()) {
            return list.get(size() - 1);
        }
        throw new NoSuchElementException();
    }

    @Override
    public int size() {
        return list.size();
    }

    private boolean checkIfInBounds(int index) {
        return (index >= 0) && (index < size());
    }

    private int findIndexFromHead(T ofElement, boolean inclusive) {
        return inclusive ? findIndex(ofElement, 0, 0) : findIndex(ofElement, 1, 0);
    }

    private int findIndexFromTail(T ofElement, boolean inclusive) {
        return inclusive ? findIndex(ofElement, 0, -1) : findIndex(ofElement, -1, -1);
    }

    private int findIndex(T OfElement, int ExistsOffset, int nonExistsOffset) {
        Objects.requireNonNull(OfElement);
        int i = Collections.binarySearch(list, OfElement, comparator);
        if (i < 0) {
            i ^= 0xFFFFFFFF;
            return checkIfInBounds(i + nonExistsOffset) ? i + nonExistsOffset : -1;
        }
        return checkIfInBounds(i + ExistsOffset) ? i + ExistsOffset : -1;
    }
}