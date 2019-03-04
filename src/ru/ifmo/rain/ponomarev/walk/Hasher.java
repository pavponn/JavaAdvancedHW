package ru.ifmo.rain.ponomarev.walk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Hasher {

    public int hashFNV(Path filePath) {
        int hash = 0x811c9dc5;
        try (BufferedInputStream reader = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] bytes = new byte[1024];
            int count;
            while ((count = reader.read(bytes)) != -1) {
                for (int i = 0; i < count; ++i) {
                    hash = (hash * 0x01000193) ^ (bytes[i] & 0xff);
                }
            }
        } catch (IOException e) {
            return 0;
        }
        return hash;
    }
}
