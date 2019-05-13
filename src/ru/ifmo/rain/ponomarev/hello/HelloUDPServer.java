package ru.ifmo.rain.ponomarev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * @author Pavel Ponomarev (pavponn@gmail.com)
 */
public class HelloUDPServer implements HelloServer {
    private ExecutorService workers;
    private ExecutorService receiver;
    private DatagramSocket socket;
    private int requestBufferSize;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port, int threads) {
        try {
            openSocket(port);
        } catch (SocketException e) {
            System.err.println("Socket can't be open or bind to specified port: " + e.getMessage());
            return;
        }
        workers = Executors.newFixedThreadPool(threads);
        receiver = Executors.newSingleThreadExecutor();

        receiver.submit(() -> {
            try {
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    byte[] data = new byte[requestBufferSize];
                    DatagramPacket packet = new DatagramPacket(data, requestBufferSize);
                    socket.receive(packet);
                    workers.submit(() -> process(packet));
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout expired: " + e.getMessage());
            } catch (PortUnreachableException e) {
                System.out.println("Port is unreachable due to some reason: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Input/output error occurred: " + e.getMessage());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        workers.shutdownNow();
        receiver.shutdownNow();
        socket.close();
        try {
            workers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            //Ignored
        }
    }

    /**
     * Allows usage of the class via command line. Arguments passed to this method should have this format:
     * {@code <host> <port_num> <prefix> <threads_num> <queries_per_thread>}
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            printErrorMessage();
            return;
        }
        int port, threadsNum;
        try {
            port = Integer.parseInt(args[0]);
            threadsNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Can't convert passed arguments to integers : " + e.getMessage());
            return;
        }
        new HelloUDPServer().start(port, threadsNum);
    }

    private void openSocket(int port) throws SocketException {
        socket = new DatagramSocket(port);
        requestBufferSize = socket.getReceiveBufferSize();
    }

    private void process(DatagramPacket packet) {
        String requestString = new String(packet.getData(), packet.getOffset(),
                packet.getLength(), StandardCharsets.UTF_8);
        String responseString = "Hello, " + requestString;
        try {
            byte[] buffer = responseString.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(buffer, 0, buffer.length, packet.getSocketAddress()));
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout expired: " + e.getMessage());
        } catch (PortUnreachableException e) {
            System.out.println("Port is unreachable due to some reason: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Input/output error occurred: " + e.getMessage());
        }
    }

    private static void printErrorMessage() {
        System.err.println("Incorrect arguments, please follow specified format:\n" +
                "<port_num> <threads_num>");
    }
}
