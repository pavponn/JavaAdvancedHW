package ru.ifmo.rain.ponomarev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class IterativeParallelism implements ScalarIP {

    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException,
            IllegalArgumentException {
        if (list == null || comparator == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return calculateFunction(list, i, x -> x.max(comparator).orElseThrow()).stream().max(comparator).orElseThrow();
    }

    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException,
            IllegalArgumentException {
        if (list == null || comparator == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return maximum(i, list, comparator.reversed());
    }

    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException,
            IllegalArgumentException {
        if (list == null || predicate == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return calculateFunction(list, i, x -> x.anyMatch(predicate)).stream().anyMatch(Boolean::booleanValue);
    }

    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException,
            IllegalArgumentException {
        if (list == null || predicate == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        return !any(i, list, predicate.negate());
    }

    private <T, E> List<E> calculateFunction(List<? extends T> list, int threadsNumber,
                                             Function<Stream<? extends T>, E> function) throws InterruptedException {
        threadsNumber = Math.max(Math.min(threadsNumber, list.size()), 1);
        int elementsPerOneThread = list.size() / threadsNumber;
        int elementsLeft = list.size() % threadsNumber;
        Thread[] threadList = new Thread[threadsNumber];
        List<E> resultList = new ArrayList<>(threadsNumber);

        int leftBorder = 0;
        for (int i = 0; i < threadsNumber; ++i) {
            int rightBorder = leftBorder + elementsPerOneThread + ((elementsLeft > 0) ? 1 : 0);
            --elementsLeft;
            final int finalLeftBorder = leftBorder;
            Thread currentThread = new Thread(() -> resultList.
                    add(function.apply((list.subList(finalLeftBorder, rightBorder)).stream())));
            currentThread.start();
            threadList[i] = currentThread;
            leftBorder = rightBorder;
        }
        for (int i = 0; i < threadsNumber; ++i) {
            threadList[i].join();
        }
        return resultList;
    }
}
