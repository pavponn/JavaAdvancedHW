package ru.ifmo.rain.ponomarev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private final String TAB = "    ";
    private final String COMMA =", ";

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Arguments are incorrect, required: <interface_name> <path_to_interface>");
            return;
        }

        if (args[0] == null || args[1] == null) {
            System.err.println("Arguments can't contain null values");
        }

        Impler implementor = new Implementor();

        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " can not be found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path  (" + args[1] + ") to class file: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error occurred while implementing class " + args[0] + ":" + e.getMessage());
        }

    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Required non null arguments");
        }

        if (!token.isInterface()) {
            throw new ImplerException("First argument requires interface");
        }
        String implementationName = token.getSimpleName() + "Impl";

        try (BufferedWriter writer = Files.newBufferedWriter(getImplementedClassPath(token, root))) {
            writer.write(String.format("%s %n %s{%n %s %n}%n",
                    getClassPackage(token),
                    getClassHeader(token, implementationName),
                    getMethods(token)
                    )
            );
        } catch (UnsupportedEncodingException e) {
            throw new ImplerException("UTF8 is not supported on this machine");
        } catch (IOException e) {
            throw new ImplerException("Error occurred while writing code to file");
        }
    }


    private String getClassPackage(Class<?> token) {
        if (token.getPackage() != null) {
            return String.format("package %s;%n", token.getPackage().getName());
        }
        return "";
    }

    private String getClassHeader(Class<?> token, String className) {
        return String.format("public class %s implements %s", className, token.getSimpleName());
    }

    private String getMethods(Class<?> token) {
        StringBuilder builder = new StringBuilder();
        for (Method method : token.getMethods()) {
            builder.append(getMethod(method));
        }

        return builder.toString();
    }

    private String getMethod(Method method) {
        return String.format("%s%s {%n%s%s%s %n}%n", TAB, getMethodHeader(method), TAB, TAB, getMethodReturn(method));
    }

    private String getMethodHeader(Method method) {
        return String.format("%s %s %s (%s) %s",
                getMethodModifiers(method),
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getMethodArguments(method),
                getMethodExceptions(method)
        );
    }

    private String getMethodModifiers(Method method) {
        return Modifier.toString(method.getModifiers() & (Modifier.methodModifiers() ^ Modifier.ABSTRACT));
    }

    private String getMethodArguments(Method method) {
        Parameter[] parameters = method.getParameters();
        return Arrays.stream(parameters)
                .map(s -> s.getType().getCanonicalName() + " " + s.getName())
                .collect(Collectors.joining(COMMA));
    }

    private String getMethodExceptions(Method method) {
        Class<?>[] methodExceptions = method.getExceptionTypes();
        if (methodExceptions.length == 0) {
            return "";
        }
        return "throws " +
                Arrays.stream(methodExceptions)
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(COMMA));
    }

    private String getMethodReturn(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(boolean.class)) {
            return "return true;";
        } else if (returnType.equals(void.class)) {
            return "";
        } else if (returnType.isPrimitive()) {
            return "return 0;";
        }
        return "return null;";
    }

    private Path getImplementedClassPath(Class<?> token, Path root) throws IOException {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace(".", File.separator) + File.separator);
            Files.createDirectories(root);
        }
        return root.resolve(token.getSimpleName() + "Impl.java");
    }
}

