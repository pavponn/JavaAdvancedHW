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

    public void set(int index, T value) {
        synchronized (list) {
            list.set(index, value);
        }

        synchronized (this) {
            ++size;
            if (size == list.size()) {
                notify();
            }
        }
    }

    synchronized List<T> getList() throws InterruptedException {
        while (size < list.size()) {
            wait();
        }
        return list;
    }

}
