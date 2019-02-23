package ru.ifmo.rain.ponomarev.arrayset;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;


public class DescendingList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> list;
    private boolean isReversed;

    public DescendingList() {
        list = Collections.emptyList();
    }

    public DescendingList(List<T> list) {
        this.list = list;
    }

    public DescendingList(List<T> list, boolean isReversed) {
        this.list = list;
        this.isReversed = isReversed;
    }

    public void reverseList() {
        isReversed = !isReversed;
    }

    @Override
    public T get(int index) {
        return !isReversed ? list.get(index) : list.get(size() - index - 1);
    }

    public int size() {
        return list.size();
    }
}