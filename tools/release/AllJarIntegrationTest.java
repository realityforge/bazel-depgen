package org.realityforge.bazel.depgen.release;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public final class AllJarIntegrationTest {
    private AllJarIntegrationTest() {}

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected path to bazel-depgen-all.jar");
        }
        final Path allJar = resolve(args[0]);
        final Path root = Files.createTempDirectory("bazel-depgen-all-jar-test");
        try {
            final Path repository = root.resolve("repo");
            final Path work = root.resolve("work");
            final Path cache = root.resolve("cache");
            Files.createDirectories(repository);
            Files.createDirectories(work);
            Files.createDirectories(cache);

            installArtifact(repository);
            installDepgenArtifact(repository, allJar);
            run(allJar, work, cache, "init", "--no-generate");

            final Path config = work.resolve("thirdparty").resolve("dependencies.yml");
            final String content = Files.readString(config)
                    .replace(
                            "https://repo.maven.apache.org/maven2/",
                            repository.toUri().toString());
            Files.writeString(config, content, StandardCharsets.UTF_8);

            run(allJar, work, cache, "add", "com.example:demo:1.0");
            run(allJar, work, cache, "generate");

            assertExists(work.resolve("thirdparty").resolve("dependencies.yml"));
            assertExists(work.resolve("thirdparty").resolve("dependencies.bzl"));
            assertExists(work.resolve("thirdparty").resolve("BUILD.bazel"));
        } finally {
            deleteTree(root);
        }
    }

    private static Path resolve(final String path) {
        final Path direct = Path.of(path);
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().normalize();
        }
        final String runfiles = System.getenv("RUNFILES_DIR");
        if (runfiles != null) {
            final Path inRunfiles = Path.of(runfiles).resolve(path);
            if (Files.exists(inRunfiles)) {
                return inRunfiles.toAbsolutePath().normalize();
            }
        }
        throw new IllegalArgumentException("File does not exist: " + path);
    }

    private static void installArtifact(final Path repository) throws Exception {
        final Path directory =
                repository.resolve("com").resolve("example").resolve("demo").resolve("1.0");
        Files.createDirectories(directory);

        writeJar(directory.resolve("demo-1.0.jar"), "demo.txt", "demo");
        writeJar(
                directory.resolve("demo-1.0-sources.jar"),
                "com/example/Demo.java",
                "package com.example; public class Demo {}\n");
        Files.writeString(
                directory.resolve("demo-1.0.pom"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                        + "  <modelVersion>4.0.0</modelVersion>\n"
                        + "  <groupId>com.example</groupId>\n"
                        + "  <artifactId>demo</artifactId>\n"
                        + "  <version>1.0</version>\n"
                        + "  <packaging>jar</packaging>\n"
                        + "</project>\n",
                StandardCharsets.UTF_8);

        writeChecksums(directory.resolve("demo-1.0.jar"));
        writeChecksums(directory.resolve("demo-1.0-sources.jar"));
        writeChecksums(directory.resolve("demo-1.0.pom"));
    }

    private static void installDepgenArtifact(final Path repository, final Path allJar) throws Exception {
        final String version = readDepgenVersion(allJar);
        final Path directory = repository
                .resolve("org")
                .resolve("realityforge")
                .resolve("bazel")
                .resolve("depgen")
                .resolve("bazel-depgen")
                .resolve(version);
        Files.createDirectories(directory);

        final Path allArtifact = directory.resolve("bazel-depgen-" + version + "-all.jar");
        Files.copy(allJar, allArtifact);
        writeJar(
                directory.resolve("bazel-depgen-" + version + "-sources.jar"),
                "org/realityforge/bazel/depgen/Main.java",
                "package org.realityforge.bazel.depgen; public final class Main {}\n");
        Files.writeString(
                directory.resolve("bazel-depgen-" + version + ".pom"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                        + "  <modelVersion>4.0.0</modelVersion>\n"
                        + "  <groupId>org.realityforge.bazel.depgen</groupId>\n"
                        + "  <artifactId>bazel-depgen</artifactId>\n"
                        + "  <version>"
                        + version
                        + "</version>\n"
                        + "  <packaging>jar</packaging>\n"
                        + "</project>\n",
                StandardCharsets.UTF_8);

        writeChecksums(allArtifact);
        writeChecksums(directory.resolve("bazel-depgen-" + version + "-sources.jar"));
        writeChecksums(directory.resolve("bazel-depgen-" + version + ".pom"));
    }

    private static String readDepgenVersion(final Path allJar) throws IOException {
        try (JarFile jar = new JarFile(allJar.toFile())) {
            final JarEntry entry = jar.getJarEntry("org/realityforge/bazel/depgen/config.properties");
            if (entry == null) {
                throw new IOException("Unable to find config.properties in " + allJar);
            }
            final Properties properties = new Properties();
            try (InputStream input = jar.getInputStream(entry)) {
                properties.load(input);
            }
            return properties.getProperty("version");
        }
    }

    private static void writeJar(final Path path, final String entryName, final String content) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            final JarEntry entry = new JarEntry(entryName);
            entry.setTime(0L);
            out.putNextEntry(entry);
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private static void writeChecksums(final Path file) throws IOException, NoSuchAlgorithmException {
        Files.writeString(
                file.resolveSibling(file.getFileName() + ".md5"), digest(file, "MD5") + "\n", StandardCharsets.UTF_8);
        Files.writeString(
                file.resolveSibling(file.getFileName() + ".sha1"),
                digest(file, "SHA-1") + "\n",
                StandardCharsets.UTF_8);
    }

    private static String digest(final Path file, final String algorithm) throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (InputStream input = Files.newInputStream(file)) {
            final byte[] buffer = new byte[8192];
            while (true) {
                final int count = input.read(buffer);
                if (count < 0) {
                    break;
                }
                digest.update(buffer, 0, count);
            }
        }
        return toHex(digest.digest());
    }

    private static String toHex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static void run(final Path allJar, final Path work, final Path cache, final String... command)
            throws Exception {
        final Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        final List<String> args = new ArrayList<>();
        args.add(java.toString());
        args.add("-jar");
        args.add(allJar.toString());
        args.add("--directory");
        args.add(work.toString());
        args.add("--cache-directory");
        args.add(cache.toString());
        for (final String arg : command) {
            args.add(arg);
        }

        final Process process =
                new ProcessBuilder(args).redirectErrorStream(true).start();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            input.transferTo(output);
        }
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + String.join(" ", args) + "\n"
                    + output.toString(StandardCharsets.UTF_8));
        }
    }

    private static void assertExists(final Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Expected file to exist: " + path);
        }
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        final List<Path> paths;
        try (var stream = Files.walk(root)) {
            paths = stream.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList());
        }
        for (final Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
