package ru.ifmo.rain.ponomarev.mapper;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Scalar iterative parallelism class.
 *
 * @author Pavel Ponomarev (pavponn@gmail.com)
 */
public class IterativeParallelism implements ScalarIP {
    private ParallelMapper mapper;

    /**
     * Default constructor. Allocates <code>IterativeMapper </code> object and
     * sets <code>mapper</code> to <code>null</code>.
     */
    public  IterativeParallelism() {
        mapper = null;
    }

    /**
     * Allocates <code>IterativeMapper</code> object and sets <code>mapper</code> to specified mapper.
     *
     * @param mapper mapper which is set as object's <code>mapper</code>.
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns maximum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return maximum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException,
            IllegalArgumentException {
        if (values == null || comparator == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return calculateFunction(values, threads, x -> x.max(comparator).orElseThrow()).stream().max(comparator).orElseThrow();
    }

    /**
     * Returns minimum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return minimum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException,
            IllegalArgumentException {
        if (values == null || comparator == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException, IllegalArgumentException {
        if (values == null || predicate == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return calculateFunction(values, threads, x -> x.anyMatch(predicate)).stream().anyMatch(Boolean::booleanValue);
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether all values satisfy predicate or {@code true}, if no values are given.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException, IllegalArgumentException {
        if (values == null || predicate == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return !any(threads, values, predicate.negate());
    }

    private <T, E> List<E> calculateFunction(List<? extends T> list, int threadsNumber,
                                             Function<Stream<? extends T>, E> function) throws InterruptedException {

        threadsNumber = Math.max(Math.min(threadsNumber, list.size()), 1);

        int elementsPerOneThread = list.size() / threadsNumber;
        int elementsLeft = list.size() % threadsNumber;

        Thread[] threadList = new Thread[threadsNumber];
        List<Stream<? extends T>> subTasks = new ArrayList<>();

        int leftBorder = 0;
        for (int i = 0; i < threadsNumber; ++i) {
            int rightBorder = leftBorder + elementsPerOneThread + ((elementsLeft > 0) ? 1 : 0);
            --elementsLeft;
            final int finalLeftBorder = leftBorder;
            subTasks.add((list.subList(finalLeftBorder, rightBorder).stream()));
            leftBorder = rightBorder;
        }
        if (mapper != null) {
            List<E> resultList;
            resultList = mapper.map(function, subTasks);
            return resultList;
        } else {
            List<E> resultList = new ArrayList<>();
            for (int i = 0; i < threadsNumber; ++i) {
                final int index = i;
                Thread currentThread = new Thread(() -> resultList.add(function.apply(subTasks.get(index))));
                currentThread.start();
                threadList[index] = currentThread;
            }
            for (int i = 0; i < threadsNumber; ++i) {
                threadList[i].join();
            }
            return resultList;
        }
    }
}
