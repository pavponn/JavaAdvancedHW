package ru.ifmo.rain.Ponomarev.walk;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong format of arguments. To launch, please, follow this format: " +
                    "java Walk <input file> <output file>");
            return;
        }

        Path inputFilePath;
        Path outputFilePath;

        try {
            inputFilePath = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.err.println("Error! Invalid path for input file: " + args[0] + e.getMessage());
            return;
        }

        try {
            outputFilePath = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Error! Invalid path for output file: " + args[1] + e.getMessage());
            return;
        }

        Walker walker = new Walker(inputFilePath, outputFilePath);
        try {
            walker.walk();
        } catch (WalkerException e) {
            System.err.println("Error occurred : " + e.getMessage());
        }

    }
}
