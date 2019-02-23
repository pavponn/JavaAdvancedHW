package ru.ifmo.rain.Ponomarev.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walker {
    private final Path inputFile;
    private final Path outputFile;

    public Walker(final Path inputFile, final Path outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public void walk() throws WalkerException {
        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                String filePath;
                while ((filePath = reader.readLine()) != null) {
                    try {
                        Hasher hasher = new Hasher();
                        int hash = hasher.hashFNV(Paths.get(filePath));
                        writer.write(String.format("%08x %s", hash, filePath));
                        writer.newLine();
                    } catch (InvalidPathException e) {
                        writer.write("00000000 " + filePath);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                throw new WalkerException("Error while opening output file or processing input/output file: "
                        + e.getMessage());
            }
        } catch (IOException e) {
            throw new WalkerException("Error while opening input file: " + e.getMessage());
        }
    }
}
