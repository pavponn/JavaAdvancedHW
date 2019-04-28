package ru.ifmo.rain.ponomarev.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ConcurrentList class for {@link ParallelMapperImpl}.
 *
 * @author Pavel Ponomarev (pavponn@gmail.com)
 */
class ConcurrentList<T> {
    private final List<T> list;
    private int size;

    /**
     * Allocates <code>ConcurrentList</code> object with specified initial capacity.
     * @param capacity initial list capacity
     */
    ConcurrentList(int capacity) {
        list = new ArrayList<>(Collections.nCopies(capacity, null));
        size = 0;
    }

    /**
     * Sets element in specified <code>index</code> to specified <code>value</code>. Synchronized method.
     * @param index index of element to be set
     * @param value value to be set
     */
    public synchronized void set(int index, T value) {
        list.set(index, value);
        ++size;
        if (size == list.size()) {
            notify();
        }
    }

    /**
     * Returns the list representation of this <code>ConcurrentList</code>.
     * @return list with all elements set.
     * @throws InterruptedException if any thread interrupted the current thread before or
     * while the current thread was waiting. The <em>interrupted status</em> of the
     * current thread is cleared when this exception is thrown.
     */
    public synchronized List<T> getList() throws InterruptedException {
        while (size < list.size()) {
            wait();
        }
        return list;
    }

}
