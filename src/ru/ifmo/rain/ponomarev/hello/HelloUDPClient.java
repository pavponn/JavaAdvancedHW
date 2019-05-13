package ru.ifmo.rain.ponomarev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavel Ponomarev (pavponn@gmail.com)
 */
public class HelloUDPClient implements HelloClient {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        ExecutorService executorsPoll = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executorsPoll.submit(new Thread(() -> sendRequests(inetSocketAddress, prefix, requests, threadId)));
        }
        executorsPoll.shutdown();
        try {
            executorsPoll.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
            //ignore
        }

    }

    /**
     * Allows usage of the class via command line. Arguments passed to this method should have this format:
     * {@code <host> <port_num> <prefix> <threads_num> <queries_per_thread>}
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            printErrorMessage();
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                printErrorMessage();
                return;
            }
        }
        int port, threadsNum, requestsNum;
        String host, prefix;
        try {
            requestsNum = Integer.parseInt(args[4]);
            threadsNum = Integer.parseInt(args[3]);
            prefix = args[2];
            port = Integer.parseInt(args[1]);
            host = args[0];
        } catch (NumberFormatException e) {
            printErrorMessage();
            return;
        }
        new HelloUDPClient().run(host, port, prefix, threadsNum, requestsNum);
    }

    private void sendRequests(final InetSocketAddress inetSocketAddress, final String prefix, final int requests, final int threadId) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);
            byte[] buffer = new byte[socket.getReceiveBufferSize()];
            DatagramPacket responsePacket = new DatagramPacket(buffer, socket.getReceiveBufferSize());
            for (int requestId = 0; requestId < requests; requestId++) {
                String requestString = String.format("%s%d_%d", prefix, threadId, requestId);
                byte[] dataRequest = requestString.getBytes(StandardCharsets.UTF_8);
                DatagramPacket requestPacket = new DatagramPacket(dataRequest, requestString.length(), inetSocketAddress);
                while (true) {
                    try {
                        socket.send(requestPacket);
                        socket.receive(responsePacket);
                        String responseString = new String(responsePacket.getData(), responsePacket.getOffset(),
                                responsePacket.getLength(), StandardCharsets.UTF_8);
                        if (checkResponse(requestString, responseString)) {
                            System.out.println("Response: " + responseString);
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout expired: " + e.getMessage());
                    } catch (PortUnreachableException e) {
                        System.out.println("Port is unreachable due to some reason: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("Input/output error occurred: " + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Can't open socket or socket can't be bind to the specified local port: "
                    + e.getMessage());
        }
    }

    private boolean checkResponse(final String requestMessage, final String responseMessage) {
        return responseMessage.contains(requestMessage);
    }

    private static void printErrorMessage() {
        System.err.println("Incorrect arguments, please follow specified format:\n" +
                "<host> <port_num> <prefix> <threads_num> <queries_per_thread>");
    }
}
