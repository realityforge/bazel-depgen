package org.realityforge.bazel.depgen.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JavadocJarBuilder {
    private static final long STABLE_TIME = 0L;

    private JavadocJarBuilder() {}

    public static void main(final String[] args) throws Exception {
        Path output = null;
        final List<Path> sourceJars = new ArrayList<>();
        final List<Path> classpath = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output":
                    output = Path.of(args[++i]);
                    break;
                case "--source-jar":
                    sourceJars.add(Path.of(args[++i]));
                    break;
                case "--classpath":
                    classpath.add(Path.of(args[++i]));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (output == null) {
            throw new IllegalArgumentException("Missing --output");
        }
        build(output, sourceJars, classpath);
    }

    private static void build(final Path output, final List<Path> sourceJars, final List<Path> classpath)
            throws Exception {
        final Path work = Files.createTempDirectory("bazel-depgen-javadoc");
        final Path sources = work.resolve("sources");
        final Path docs = work.resolve("docs");
        Files.createDirectories(sources);
        Files.createDirectories(docs);
        try {
            for (final Path sourceJar : sourceJars) {
                extractSources(sourceJar, sources);
            }
            final List<Path> sourceFiles = collectSourceFiles(sources);
            runJavadoc(docs, sourceFiles, classpath);
            writeJar(output, docs);
        } finally {
            deleteTree(work);
        }
    }

    private static void extractSources(final Path sourceJar, final Path output) throws IOException {
        try (JarFile jar = new JarFile(sourceJar.toFile())) {
            final List<? extends JarEntry> entries = Collections.list(jar.entries());
            entries.sort((a, b) -> a.getName().compareTo(b.getName()));
            for (final JarEntry entry : entries) {
                if (!entry.isDirectory() && entry.getName().endsWith(".java")) {
                    final Path target = output.resolve(entry.getName());
                    Files.createDirectories(target.getParent());
                    final byte[] content = jar.getInputStream(entry).readAllBytes();
                    if (Files.exists(target)) {
                        final byte[] existing = Files.readAllBytes(target);
                        if (!java.util.Arrays.equals(existing, content)) {
                            throw new IOException("Duplicate non-identical source entry: " + entry.getName());
                        }
                    } else {
                        Files.write(target, content);
                    }
                }
            }
        }
    }

    private static List<Path> collectSourceFiles(final Path sources) throws IOException {
        try (Stream<Path> stream = Files.walk(sources)) {
            final List<Path> files = stream.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                throw new IOException("No Java source files found for Javadocs");
            }
            return files;
        }
    }

    private static void runJavadoc(final Path docs, final List<Path> sourceFiles, final List<Path> classpath)
            throws Exception {
        final Path argsFile = Files.createTempFile("bazel-depgen-javadoc", ".args");
        final List<String> argLines = new ArrayList<>();
        argLines.add("-quiet");
        argLines.add("-d");
        argLines.add(docs.toString());
        argLines.add("-encoding");
        argLines.add("UTF-8");
        argLines.add("-charset");
        argLines.add("UTF-8");
        argLines.add("-docencoding");
        argLines.add("UTF-8");
        if (!classpath.isEmpty()) {
            argLines.add("-classpath");
            argLines.add(String.join(
                    System.getProperty("path.separator"),
                    classpath.stream().map(Path::toString).collect(Collectors.toList())));
        }
        sourceFiles.stream().map(Path::toString).forEach(argLines::add);
        Files.write(argsFile, argLines, StandardCharsets.UTF_8);

        final Path javadoc = Path.of(System.getProperty("java.home"), "bin", "javadoc");
        final Process process = new ProcessBuilder(javadoc.toString(), "@" + argsFile)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();
        final int exit = process.waitFor();
        Files.deleteIfExists(argsFile);
        if (exit != 0) {
            throw new IOException("javadoc exited with status " + exit);
        }
    }

    private static void writeJar(final Path output, final Path docs) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(output), new Manifest())) {
            final List<Path> files;
            try (Stream<Path> stream = Files.walk(docs)) {
                files = stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
            }
            for (final Path file : files) {
                final String name = docs.relativize(file).toString().replace('\\', '/');
                final JarEntry entry = new JarEntry(name);
                entry.setTime(STABLE_TIME);
                out.putNextEntry(entry);
                Files.copy(file, out);
                out.closeEntry();
            }
        }
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        final List<Path> paths;
        try (Stream<Path> stream = Files.walk(root)) {
            paths = stream.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList());
        }
        for (final Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
