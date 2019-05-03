package ru.ifmo.rain.ponomarev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author Pavel Ponomarev (pavponn@gmail.com)
 */
public class WebCrawler implements Crawler {
    private final ConcurrentMap<String, HostBarrier> hostBarrierMap;
    private final ExecutorService downloadersPoll;
    private final ExecutorService extractorsPoll;
    private final Downloader downloader;
    private final int perHost;


    /**
     * Creates a new instance of {@code WebCrawler} class. Initialized with specified {@code downloader},
     * number of {@code downloaders}, {@code extractors} and {@code perHost} limit.
     *
     * @param downloader  specified downloader
     * @param downloaders number of downloaders
     * @param extractors  number of extractors
     * @param perHost     perHost limit
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        extractorsPoll = Executors.newFixedThreadPool(extractors);
        downloadersPoll = Executors.newFixedThreadPool(downloaders);
        hostBarrierMap = new ConcurrentHashMap<>();
        this.downloader = downloader;
        this.perHost = perHost;
    }

    /**
     * Allows usage of the class via command line. Arguments passed to this method should have this format:
     * {@code url [depth [downloaders [extractors [perHost]]]]}
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 5) {
            incorrectArgumentsMessage();
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                incorrectArgumentsMessage();
                return;
            }
        }
        int depth = 1;
        int downloaders = 4;
        int extractors = 4;
        int perHost = 4;
        String url = "";

        try {
            switch (args.length) {
                case 5:
                    perHost = Integer.parseInt(args[4]);
                case 4:
                    extractors = Integer.parseInt(args[3]);
                case 3:
                    downloaders = Integer.parseInt(args[2]);
                case 2:
                    depth = Integer.parseInt(args[1]);
                case 1:
                    url = args[0];
            }
        } catch (NumberFormatException e) {
            System.err.println("Can't convert one of the arguments to integer: " + e.getMessage());
            return;
        }

        if (perHost < 1 || extractors < 1 || downloaders < 1 || depth < 1) {
            System.err.println("Invalid arguments: all integer arguments should be positive.");
            return;
        }

        try (Crawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            Result result = webCrawler.download(url, depth);
            System.out.println(String.format("Pages downloaded: %d \n Errors occurred %d \n Links: \n",
                    result.getDownloaded().size(), result.getErrors().size()));
            for (String link : result.getDownloaded()) {
                System.out.println(link);
            }
        } catch (IOException e) {
            System.err.println("Can't create CachingDownloader: " + e.getMessage());

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result download(final String url, final int depth) {
        final Set<String> linksSet = new ConcurrentSkipListSet<>();
        final ConcurrentMap<String, IOException> exceptions = new ConcurrentHashMap<>();
        Phaser phaser = new Phaser(1);

        linksSet.add(url);
        recursive(url, depth, phaser, linksSet, exceptions);
        phaser.arriveAndAwaitAdvance();

        linksSet.removeAll(exceptions.keySet());
        return new Result(new ArrayList<>(linksSet), exceptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        extractorsPoll.shutdownNow();
        downloadersPoll.shutdownNow();
    }


    private void recursive(final String url, final int stepsLeft, final Phaser phaser,
                           final Set<String> result, final ConcurrentMap<String, IOException> exceptions) {
        Optional<String> optionalHost = getHost(url, exceptions);
        if (optionalHost.isEmpty()) {
            return;
        }
        final String host = optionalHost.get(); // checked above

        HostBarrier hostBarrier = hostBarrierMap.computeIfAbsent(host, h -> new HostBarrier());
        phaser.register();
        hostBarrier.add(() -> {
            try {
                final Document document = downloader.download(url);
                if (stepsLeft == 1) {
                    return;
                }
                phaser.register();
                extractorsPoll.submit(() -> {
                    try {
                        document.extractLinks()
                                .stream()
                                .filter(result::add)
                                .forEach(link -> recursive(link, stepsLeft - 1, phaser, result, exceptions));
                    } catch (IOException e) {
                        //That's okay to ignore it
                    } finally {
                        phaser.arrive();
                    }
                });
            } catch (IOException e) {
                exceptions.put(url, e);
            } finally {
                phaser.arrive();
                hostBarrier.next();
            }
        });
    }

    private Optional<String> getHost(final String url, final Map<String, IOException> exceptions) {
        Optional<String> result = Optional.empty();
        try {
            result = Optional.of(URLUtils.getHost(url));
        } catch (MalformedURLException e) {
            exceptions.put(url, e);
        }
        return result;
    }

    private class HostBarrier {
        private final Queue<Runnable> tasks;
        private int amount;

        HostBarrier() {
            tasks = new ArrayDeque<>();
            amount = 0;
        }

        private synchronized void add(final Runnable task) {
            if (amount < perHost) {
                amount += 1;
                downloadersPoll.submit(task);
            } else {
                tasks.add(task);
            }
        }

        private synchronized void next() {
            final Runnable task = tasks.poll();
            if (task == null) {
                amount -= 1;
            } else {
                downloadersPoll.submit(task);
            }
        }

    }

    private static void incorrectArgumentsMessage() {
        System.out.println("Incorrect arguments passed to WebCrawler, please follow specified format:\n" +
                "url [depth [downloaders [extractors [perHost]]]]");
    }

}
