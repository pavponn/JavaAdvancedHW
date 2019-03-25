package ru.ifmo.rain.ponomarev.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConcurrentList<T> {
    private final List<T> list;
    private int size;

    ConcurrentList(int capacity) {
        list = new ArrayList<>(Collections.nCopies(capacity, null));
        size = 0;
    }

    public synchronized void set(int index, T value) {
        list.set(index, value);
        ++size;
        if (size == list.size()) {
            notify();
        }
    }

    public synchronized List<T> getList() throws InterruptedException {
        while (size < list.size()) {
            wait();
        }
        return list;
    }

}
