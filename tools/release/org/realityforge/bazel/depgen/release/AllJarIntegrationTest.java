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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class AllJarIntegrationTest {
    private AllJarIntegrationTest() {}

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected path to bazel-depgen-all.jar");
        }
        final var allJar = resolve(args[0]);
        final var root = Files.createTempDirectory("bazel-depgen-all-jar-test");
        try {
            final var repository = root.resolve("repo");
            final var work = root.resolve("work");
            final var cache = root.resolve("cache");
            Files.createDirectories(repository);
            Files.createDirectories(work);
            Files.createDirectories(cache);

            installArtifact(repository);
            installDepgenArtifact(repository, allJar);
            run(allJar, work, cache, "init", "--no-generate");

            final var config = work.resolve("thirdparty").resolve("dependencies.yml");
            final var content = Files.readString(config)
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
        final var direct = Path.of(path);
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().normalize();
        }
        final String runfiles = System.getenv("RUNFILES_DIR");
        if (runfiles != null) {
            final var inRunfiles = Path.of(runfiles).resolve(path);
            if (Files.exists(inRunfiles)) {
                return inRunfiles.toAbsolutePath().normalize();
            }
        }
        throw new IllegalArgumentException("File does not exist: " + path);
    }

    private static void installArtifact(final Path repository) throws Exception {
        final var directory =
                repository.resolve("com").resolve("example").resolve("demo").resolve("1.0");
        Files.createDirectories(directory);

        writeJar(directory.resolve("demo-1.0.jar"), "demo.txt", "demo");
        writeJar(
                directory.resolve("demo-1.0-sources.jar"),
                "com/example/Demo.java",
                "package com.example; public class Demo {}\n");
        Files.writeString(directory.resolve("demo-1.0.pom"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>1.0</version>
              <packaging>jar</packaging>
            </project>
            """, StandardCharsets.UTF_8);

        writeChecksums(directory.resolve("demo-1.0.jar"));
        writeChecksums(directory.resolve("demo-1.0-sources.jar"));
        writeChecksums(directory.resolve("demo-1.0.pom"));
    }

    private static void installDepgenArtifact(final Path repository, final Path allJar) throws Exception {
        final var version = readDepgenVersion(allJar);
        final var directory = repository
                .resolve("org")
                .resolve("realityforge")
                .resolve("bazel")
                .resolve("depgen")
                .resolve("bazel-depgen")
                .resolve(version);
        Files.createDirectories(directory);

        final var allArtifact = directory.resolve("bazel-depgen-" + version + "-all.jar");
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
            final var properties = new Properties();
            try (InputStream input = jar.getInputStream(entry)) {
                properties.load(input);
            }
            final String version = properties.getProperty("version");
            if (version == null) {
                throw new IOException("Unable to find version in config.properties in " + allJar);
            }
            return version;
        }
    }

    private static void writeJar(final Path path, final String entryName, final String content) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            final var entry = new JarEntry(entryName);
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
        final var digest = MessageDigest.getInstance(algorithm);
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
        final var sb = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static void run(final Path allJar, final Path work, final Path cache, final String... command)
            throws IOException, InterruptedException {
        final var java = javaTool("java");
        final var args = new ArrayList<String>();
        args.add(java.toString());
        args.add("-jar");
        args.add(allJar.toString());
        args.add("--directory");
        args.add(work.toString());
        args.add("--cache-directory");
        args.add(cache.toString());
        args.addAll(Arrays.asList(command));

        final var process = new ProcessBuilder(args).redirectErrorStream(true).start();
        final var output = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            input.transferTo(output);
        }
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + String.join(" ", args) + "\n"
                    + output.toString(StandardCharsets.UTF_8));
        }
    }

    private static Path javaTool(final String name) throws IOException {
        final var javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new IOException("java.home system property is not set");
        }
        return Path.of(javaHome, "bin", name);
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
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        for (final Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
