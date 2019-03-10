package ru.ifmo.rain.ponomarev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * This class implements {@link JarImpler} interface.
 *
 * @author Ponomarev Pavel (pavponn@gmail.com)
 */
public class Implementor implements JarImpler {

    /**
     * Constant String field which represents four whitespaces
     */
    private final String TAB = "    ";
    /**
     * Constant String field which represents concatenation of comma and one whitespace
     */
    private final String COMMA = ", ";

    /**
     * Entry point of program, can be used to choose whether .java or .jar file should be generated. If arguments don't
     * satisfy one of the following options, method prints error message.
     * <ul>
     * <li> <tt>interfaceName rootPath</tt> runs {@link #implement(Class, Path)} with these arguments</li>
     * <li><tt> -jar interfaceName jarPath</tt> runs {@link #implementJar(Class, Path)} with these arguments</li>
     * </ul>
     *
     * @param args arguments for program running
     */
    public static void main(String[] args) {
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3 && args[0] != null && args[1] != null && args[2] != null) {
                if (args[0].equals("-jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    incorrectArgumentsMessage();
                }
            } else if (args.length == 2 && args[0] != null && args[1] != null) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                incorrectArgumentsMessage();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " can not be found: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path  (" + args[1] + ") to class file: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error occurred while implementing class " + args[0] + ":" + e.getMessage());
        }

    }

    /**
     * Constructs default instance of {@link Implementor}.
     */
    Implementor() { }

    /**
     * @throws ImplerException when:
     *                         <ul>
     *                         <li>Some of the arguments are null </li>
     *                         <li>Some directories or files can't be created</li>
     *                         <li> {@link JavaCompiler} fails to compile implemented class</li>
     *                         <li>Error occurred while implementing specific class</li>
     *                         <li>I/O error occurred during implementation</li>
     *                         </ul>
     *                         Uses {@link #implement(Class, Path)} to generate <tt>.java</tt> files which will be packed in <tt>.jar</tt> file.
     *                         During implementation creates a temporary folder to store <tt>.java</tt> and <tt>.class</tt> files.
     * @see #implement(Class, Path)
     * @see JarOutputStream
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Required non null arguments");
        }
        createDirectory(jarFile);
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), ".temp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temp dir for generated java file: " + e.getMessage());
        }

        tempDir.toFile().deleteOnExit();
        implement(token, tempDir);
        compileJavaFiles(tempDir, token);

        Manifest manifest = getManifest("Pavel Ponomarev");

        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            outputStream.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            Files.copy(getPathToCreatedFile(token, tempDir, ".class"), outputStream);
        } catch (IOException e) {
            throw new ImplerException("Can't write jar file: " + e.getMessage());
        }

    }

    /**
     * @throws ImplerException when:
     *                         <ul>
     *                         <li>Some of the arguments are null </li>
     *                         <li>Some directories or files can't be created</li>
     *                         <li> {@link JavaCompiler} fails to compile implemented class</li>
     *                         <li>Error occurred while implementing specific class</li>
     *                         <li>I/O error occurred during implementation</li>
     *                         </ul>
     *                         Uses {@link BufferedWriter} to create and to write <tt>.java</tt> file.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Required non null arguments");
        }

        if (!token.isInterface()) {
            throw new ImplerException("First argument requires interface");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                createPathToFile(token, root, ".java"))) {
            writer.write(String.format("%s %n %s{%n %s %n}%n",
                    getClassPackage(token),
                    getClassHeader(token, getImplClassName(token)),
                    getMethods(token)
                    )
            );
        } catch (UnsupportedEncodingException e) {
            throw new ImplerException("UTF8 is not supported on this machine");
        } catch (IOException e) {
            throw new ImplerException("Error occurred while writing code to file");
        }
    }

    /**
     * Returns a string representation of package declaration for specified interface.
     *
     * @param token {@link Class} an interface
     * @return a string representation of package declaration.
     */
    private String getClassPackage(Class<?> token) {
        return String.format("package %s;%n", getPackageName(token));
    }

    /**
     * Returns a string representation of header of class which implements specific interface.
     *
     * @param token     {@link Class} an interface
     * @param className name of class
     * @return a header of class which implements specific interface
     */
    private String getClassHeader(Class<?> token, String className) {
        return String.format("public class %s implements %s", className, token.getSimpleName());
    }

    /**
     * Return a string representation of methods of specific interface. Methods implementations are default.
     * Uses {@link Implementor#getMethod(Method)} to create it.
     *
     * @param token {@link Class} an interface
     * @return a string representation of methods of interface <tt>token</tt>.
     */
    private String getMethods(Class<?> token) {
        StringBuilder builder = new StringBuilder();
        for (Method method : token.getMethods()) {
            builder.append(getMethod(method));
        }

        return builder.toString();
    }

    /**
     * Return a string representation of specific method. String produced by this method can be used as a default
     * method implementation. To create method representation uses:
     * <ul>
     * <li>{@link Implementor#getMethodHeader(Method)}</li>
     * <li>{@link Implementor#getMethodReturn(Method)}</li>
     * </ul>
     *
     * @param method {@link Method} a method
     * @return a string representation of specific <tt>method</tt> (default implementation).
     */
    private String getMethod(Method method) {
        return String.format("%s%s {%n%s%s%s %n}%n", TAB, getMethodHeader(method), TAB, TAB, getMethodReturn(method));
    }

    /**
     * Returns a string representation of header of specific method. To make header uses:
     * <ul>
     * <li>{@link Implementor#getMethodModifiers(Method)} </li>
     * <li> {@link Method#getReturnType()} </li>
     * <li>{@link Method#getName()} </li>
     * <li>{@link Implementor#getMethodArguments(Method)} </li>
     * <li>{@link Implementor#getMethodExceptions(Method)} </li>
     * </ul>
     *
     * @param method {@link Method} a method
     * @return a string representation of <tt>method</tt> header.
     */
    private String getMethodHeader(Method method) {
        return String.format("%s %s %s (%s) %s",
                getMethodModifiers(method),
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getMethodArguments(method),
                getMethodExceptions(method)
        );
    }

    /**
     * Returns a string representation modifiers of specific method except of {@link Modifier#ABSTRACT}.
     *
     * @param method {@link Method} a method
     * @return a string which represents <tt>method</tt>'s modifiers.
     */
    private String getMethodModifiers(Method method) {
        return Modifier.toString(method.getModifiers() & (Modifier.methodModifiers() ^ Modifier.ABSTRACT));
    }

    /**
     * Returns a string representation of arguments of specific method.
     *
     * @param method {@link Method} a method
     * @return a string concatenation of all  <tt>method</tt>'s arguments with their types and names.
     */
    private String getMethodArguments(Method method) {
        Parameter[] parameters = method.getParameters();
        return Arrays.stream(parameters)
                .map(s -> s.getType().getCanonicalName() + " " + s.getName())
                .collect(Collectors.joining(COMMA));
    }

    /**
     * Returns a string part of specific method header which contains exceptions.
     *
     * @param method {@link Method} a method
     * @return concatenation of "throws " and  <tt>method</tt> exception classes joined by ", ".
     * If  <tt>method</tt> doesn't throw anything returns empty string.
     */
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

    /**
     * Returns a string which represents default return statement in code for specific method.
     *
     * @param method {@link Method} a method
     * @return one of the following options:
     * <ul>
     * <li>"if <tt>method</tt> return type is {@link Void} </li>
     * <li>"return true;" if <tt>method</tt> return type is {@link Boolean} </li>
     * <li>"return 0;" if <tt>method</tt> return type is {@link javax.lang.model.type.PrimitiveType} </li>
     * <li>"return null;" otherwise </li>
     * </ul>
     */
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

    /**
     * Creates all necessary parent directories up to file.
     *
     * @param path {@link Path} path to be created.
     * @throws ImplerException if {@link IOException} occurs in {@link Files#createDirectories(Path, FileAttribute[])}
     */
    private void createDirectory(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Can't create directories for jar file: " + e.getMessage());
            }
        }
    }

    /**
     * Gets path to specific created file.
     *
     * @param token  {@link Class} interface which is implemented
     * @param root   {@link Path} root directory
     * @param suffix file suffix
     * @return full {@link Path} path to specific file.
     */
    private Path getPathToCreatedFile(Class<?> token, Path root, String suffix) {
        return root.resolve(getPackageName(token).replace('.', File.separatorChar))
                .resolve(getImplClassName(token) + suffix);
    }

    /**
     * Creates path to specific file. Uses {@link Files#createDirectories(Path, FileAttribute[])} to create necessary
     * directories.
     *
     * @param token  {@link Class} interface which is implemented
     * @param root   {@link Path} root directory
     * @param suffix file suffix
     * @return {@link Path} path to created file
     * @throws IOException when error occurs while creating directory.
     */
    private Path createPathToFile(Class<?> token, Path root, String suffix) throws IOException {
        root = root.resolve(getPackageName(token).replace(".", File.separator) + File.separator);
        Files.createDirectories(root);
        return root.resolve(getImplClassName(token) + suffix);
    }

    /**
     * Compiles java file which is implementation of specific interface. Uses {@link JavaCompiler} to compile files.
     *
     * @param dir   {@link Path} root directory of <tt>token</tt> implementation file.
     * @param token {@link Class} interface which is implemented.
     * @throws ImplerException when java files can't be compiled.
     */
    private void compileJavaFiles(Path dir, Class<?> token) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String classPath = dir.toString() + File.pathSeparator +
                token.getProtectionDomain().getCodeSource().getLocation().getPath() + File.pathSeparator +
                System.getProperty("java.class.path");
        String[] arguments =
                {"-cp", classPath, "-encoding", "UTF8",
                        getPathToCreatedFile(token, dir, ".java").toString()};
        if (compiler.run(System.in, System.out, System.err, arguments) != 0) {
            throw new ImplerException("Can't compile files");
        }
    }

    /**
     * Gets a package name.
     *
     * @param token {@link Class} an interface to get a package for.
     * @return package of <code>token</code>, if <code>token</code> has no package returns "".
     */

    private String getPackageName(Class<?> token) {
        if (token.getPackage() == null)
            return "";
        return token.getPackage().getName();
    }

    /**
     * Gets a name of class which implements specific interface.
     *
     * @param token {@link Class} an interface to get a name for.
     * @return concatenation of {@link Class#getSimpleName()} and "Impl".
     */
    private String getImplClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns standard {@link Manifest} instance for <tt>.jar</tt> file with specified vendor.
     *
     * @param vendor name of implementation vendor
     * @return {@link Manifest} instance with specified vendor.
     */
    private Manifest getManifest(String vendor) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, vendor);
        return manifest;
    }

    /**
     * Prints error message.
     *
     * @see Implementor#main(String[])
     */
    private static void incorrectArgumentsMessage() {
        System.err.println("Arguments are incorrect, please follow one of these formats: \n" +
                "<interface_name> <path> \n" +
                "-jar <interface_name> <file_name.jar>");
    }

}

