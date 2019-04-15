package ru.ifmo.rain.ponomarev.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import static java.util.Arrays.stream;

public class ParallelMapperImpl implements ParallelMapper {
    private Thread[] workers;
    private final Queue<Runnable> queue;
    private final static int QUEUE_MAX_SIZE = 1_000_117;

    /**
     * Allocates <code>ParallelMapper</code> object and sets <code>mapper</code> to specified mapper.
     * @param threads number of threads to be used.
     * @throws IllegalArgumentException if <code>threads</code> are less then 1.
     */
    public ParallelMapperImpl(int threads) throws IllegalArgumentException {
        if (threads < 1) {
            throw new IllegalArgumentException("Thread number can't less than 1");
        }
        queue = new ArrayDeque<>();
        workers = new Thread[threads];
        for (int i = 0; i < threads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        calculate();
                    }
                } catch (InterruptedException e) {
                    //Ignore
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
            workers[i] = thread;
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        final ConcurrentList<R> results = new ConcurrentList<>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            final int finalIndex = i;
            add(() -> results.set(finalIndex, function.apply(list.get(finalIndex))));
        }
        return results.getList();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        stream(workers).forEach(Thread::interrupt);
        waitThreads();
    }

    private void add(Runnable newTask) throws InterruptedException {
        synchronized (queue) {
            while (queue.size() == QUEUE_MAX_SIZE) {
                queue.wait();
            }
            queue.add(newTask);
            queue.notify();
        }
    }

    private void calculate() throws InterruptedException {
        Runnable task;
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }
            task = queue.poll();
            queue.notify();
        }
        task.run();
    }

    private boolean waitThreads() {
        boolean noException = true;
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                noException = false;
            }
        }
        return noException;
    }
}
