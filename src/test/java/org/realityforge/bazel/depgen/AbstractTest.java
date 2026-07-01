package org.realityforge.bazel.depgen;

import static org.testng.Assert.*;

import gir.Gir;
import gir.io.FileUtil;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.metadata.DepgenMetadata;
import org.realityforge.bazel.depgen.model.ApplicationModel;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.realityforge.bazel.depgen.record.ArtifactRecord;
import org.realityforge.bazel.depgen.util.ArtifactUtil;
import org.testng.Assert;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;

public abstract class AbstractTest implements IHookable {
    @NonNull
    protected static <T> T requireNonNull(@Nullable final T value) {
        assertNotNull(value);
        return Objects.requireNonNull(value);
    }

    @Override
    public void run(final IHookCallBack callBack, final ITestResult testResult) {
        System.setProperty(DepGenConfig.PROPERTY_KEY, "1");
        try {
            Gir.go(() -> FileUtil.inTempDir(() -> {
                callBack.runTestMethod(testResult);
            }));
        } catch (final Exception e) {
            assertNull(e);
        } finally {
            System.getProperties().remove(DepGenConfig.PROPERTY_KEY);
        }
    }

    @NonNull
    protected final ApplicationRecord loadApplicationRecord() throws Exception {
        return loadApplicationRecord(FileUtil.createLocalTempDir());
    }

    @NonNull
    protected final ApplicationRecord loadApplicationRecord(@NonNull final Path cacheDir) throws Exception {
        final ApplicationModel model = loadApplicationModel();
        final Resolver resolver = createResolver(model, cacheDir);
        final DependencyNode root = resolveDependencies(resolver, model);
        return ApplicationRecord.build(model, root, resolver.getAuthenticationContexts(), Assert::fail);
    }

    @NonNull
    private Resolver createResolver(@NonNull final ApplicationModel model, @NonNull final Path cacheDir)
            throws Exception {
        final Path settingsFile = FileUtil.getCurrentDirectory().resolve("settings.xml");
        return ResolverUtil.createResolver(
                newEnvironment(),
                cacheDir,
                model,
                SettingsUtil.loadSettings(settingsFile, Logger.getAnonymousLogger()));
    }

    @NonNull
    final Environment newEnvironment() throws IOException {
        return newEnvironment(Logger.getAnonymousLogger());
    }

    @NonNull
    final Environment newEnvironment(@NonNull final TestHandler handler) throws IOException {
        return newEnvironment(createLogger(handler));
    }

    @NonNull
    final Environment newEnvironment(@NonNull final Logger logger) throws IOException {
        final var environment = new Environment(null, FileUtil.getCurrentDirectory(), logger);
        environment.setConfigFile(getDefaultConfigFile());
        environment.setSettingsFile(FileUtil.getCurrentDirectory().resolve("settings.xml"));
        environment.setCacheDir(FileUtil.createLocalTempDir());
        environment.setRepositoryCacheDir(FileUtil.createLocalTempDir());
        return environment;
    }

    @NonNull
    protected final Path getDefaultConfigFile() {
        return FileUtil.getCurrentDirectory().resolve("thirdparty").resolve(ApplicationConfig.FILENAME);
    }

    @NonNull
    final Resolver createResolver(@NonNull final Path localRepositoryDirectory) throws Exception {
        final var remoteRepository = new RemoteRepository.Builder(
                        "local", "default", localRepositoryDirectory.toUri().toString())
                .build();
        return ResolverUtil.createResolver(
                newEnvironment(),
                FileUtil.createLocalTempDir(),
                Collections.singletonList(remoteRepository),
                true,
                true);
    }

    @NonNull
    final DependencyNode resolveDependencies(@NonNull final Resolver resolver, @NonNull final ApplicationModel model)
            throws DependencyResolutionException {
        final DependencyResult result = resolver.resolveDependencies(model, (m, e) -> fail());

        assertTrue(result.getCycles().isEmpty());
        assertTrue(result.getCollectExceptions().isEmpty());
        final DependencyNode root = result.getRoot();
        assertNotNull(root);
        return root;
    }

    @NonNull
    protected final ApplicationModel loadApplicationModel() throws Exception {
        return ApplicationModel.load(loadApplicationConfig(), false);
    }

    @NonNull
    protected final ApplicationConfig loadApplicationConfig() throws Exception {
        return ApplicationConfig.load(getDefaultConfigFile());
    }

    protected final void writeBazelrc(@NonNull final Path repositoryCache) throws IOException {
        FileUtil.write(
                ".bazelrc",
                "startup --output_user_root " + Files.createTempDirectory("bazel-depgen") + "\n"
                        + "build --repository_cache "
                        + repositoryCache + "\n" + "build --repo_contents_cache=\n");
    }

    final void writeWorkspace() throws IOException {
        FileUtil.write("WORKSPACE", "");
    }

    final void writeModule() throws IOException {
        FileUtil.write("MODULE.bazel", "module(name = \"test\")\n");
    }

    protected final void writeConfigFile(@NonNull final Path dir, @NonNull final String content) throws Exception {
        deployDepGenArtifactToLocalRepository(dir);
        writeConfigFile("repositories:\n" + "  - name: local\n" + "    url: " + dir.toUri() + "\n" + content);
    }

    protected final void deployDepGenArtifactToLocalRepository(@NonNull final Path dir) throws Exception {
        deployArtifactToLocalRepository(dir, DepGenConfig.getCoord());
    }

    final void deployDepGenArtifactToCacheDir(@NonNull final Path cacheDir) throws Exception {
        deployDepGenArtifactToLocalRepository(cacheDir);
        final String directory = ArtifactUtil.artifactToDirectory(
                DepGenConfig.getGroupId(), DepGenConfig.getArtifactId(), DepGenConfig.getVersion());
        final String artifactPath = ArtifactUtil.artifactToPath(
                DepGenConfig.getGroupId(),
                DepGenConfig.getArtifactId(),
                DepGenConfig.getVersion(),
                DepGenConfig.getClassifier(),
                "jar");
        final Path metaDataFile = cacheDir.resolve(directory).resolve(DepgenMetadata.FILENAME);
        FileUtil.write(
                metaDataFile,
                "all.central.url=https://repo.maven.apache.org/maven2/" + artifactPath + "\n"
                        + "all.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n"
                        + "processors=-\n"
                        + "sources.central.url=https://repo.maven.apache.org/maven2/"
                        + artifactPath + "\n" + "sources.present=true\n"
                        + "sources.sha256=E424B659CF9C9C4ADF4C19A1CACDB13C0CBD78A79070817F433DBC2DADE3C6D4\n");
    }

    protected final void writeConfigFile(@NonNull final String content) throws IOException {
        FileUtil.write(getDefaultConfigFile(), content);
    }

    protected final void assertOutputContains(@NonNull final String output, @NonNull final String text) {
        assertTrue(
                output.contains(text),
                "Expected output\n---\n" + output + "\n---\nto contain text\n---\n" + text + "\n---\n");
    }

    protected final void assertOutputDoesNotContain(@NonNull final String output, @NonNull final String text) {
        assertFalse(
                output.contains(text),
                "Expected output\n---\n" + output + "\n---\nto not contain text\n---\n" + text + "\n---\n");
    }

    @NonNull
    private Path createTempPomFile(
            @NonNull final String group,
            @NonNull final String id,
            @NonNull final String version,
            @NonNull final String type,
            @NonNull final String... dependencies)
            throws IOException {
        final Path pomFile = Files.createTempFile("data", ".pom");
        String pomContents = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                + " http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>"
                + group + "</groupId>\n" + "  <artifactId>"
                + id + "</artifactId>\n" + "  <version>"
                + version + "</version>\n" + "  <packaging>"
                + type + "</packaging>\n";

        pomContents += buildDependenciesSection(dependencies);

        pomContents += "</project>\n";
        Files.write(pomFile, pomContents.getBytes());
        return pomFile;
    }

    @SuppressWarnings("StringConcatenationInLoop")
    @NonNull
    private String buildDependenciesSection(@NonNull final String[] dependencies) throws IOException {
        if (0 != dependencies.length) {
            String pomContents = "  <dependencies>\n";
            for (final String dependency : dependencies) {
                final String[] components = dependency.split(":");
                assert components.length >= 3 && components.length <= 7;
                final String dependencyGroup = components[0];
                final String dependencyId = components[1];
                final String dependencyType = components.length > 3 ? components[2] : null;
                final String dependencyClassifier = components.length > 4 ? components[3] : null;
                final String dependencyVersion =
                        components.length == 3 ? components[2] : components.length == 4 ? components[3] : components[4];
                final String dependencyScope = components.length == 6 ? components[5] : null;
                final boolean optional = components.length == 7 && "optional".equals(components[6]);

                pomContents += "    <dependency>\n";
                pomContents += "      <groupId>" + dependencyGroup + "</groupId>\n";
                pomContents += "      <artifactId>" + dependencyId + "</artifactId>\n";
                pomContents += "      <version>" + dependencyVersion + "</version>\n";
                if (null != dependencyType) {
                    pomContents += "      <type>" + dependencyType + "</type>\n";
                }
                if (null != dependencyClassifier && !"".equals(dependencyClassifier)) {
                    pomContents += "      <classifier>" + dependencyClassifier + "</classifier>\n";
                }
                if (null != dependencyScope) {
                    pomContents += "      <scope>" + dependencyScope + "</scope>\n";
                    if (org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals(dependencyScope)) {
                        pomContents += "      <systemPath>" + createTempJarFile() + "</systemPath>\n";
                    }
                }
                if (optional) {
                    pomContents += "      <optional>true</optional>\n";
                }
                pomContents += "    </dependency>\n";
            }
            pomContents += "  </dependencies>\n";
            return pomContents;
        } else {
            return "";
        }
    }

    protected final void deployArtifactToLocalRepository(
            @NonNull final Path localRepository, @NonNull final String coords, @NonNull final String... dependencies)
            throws Exception {
        final var sourcesArtifact = new SubArtifact(new DefaultArtifact(coords), "sources", "jar");
        deployTempArtifactToLocalRepository(localRepository, sourcesArtifact.toString());
        deployTempArtifactToLocalRepository(localRepository, coords, dependencies);
    }

    protected final void deployTempArtifactToLocalRepository(
            @NonNull final Path localRepository, @NonNull final String coords, @NonNull final String... dependencies)
            throws Exception {
        deployTempArtifactToLocalRepository(localRepository, coords, createTempJarFile(), dependencies);
    }

    protected final void deployTempArtifactToLocalRepository(
            @NonNull final Path localRepository,
            @NonNull final String coords,
            @NonNull final Path file,
            @NonNull final String... dependencies)
            throws Exception {
        final var artifact = new DefaultArtifact(coords);
        final Path pomFile = createTempPomFile(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getExtension(),
                dependencies);
        deployTempArtifactToLocalRepository(localRepository, coords, file, pomFile);
    }

    private void deployTempArtifactToLocalRepository(
            @NonNull final Path localRepository,
            @NonNull final String coords,
            @NonNull final Path file,
            @NonNull final Path pomFile)
            throws Exception {
        final var artifact = new DefaultArtifact(coords);
        copyArtifactToLocalRepository(localRepository, artifact, file);
        copyArtifactToLocalRepository(localRepository, new SubArtifact(artifact, "", "pom"), pomFile);
    }

    private void copyArtifactToLocalRepository(
            @NonNull final Path localRepository, @NonNull final Artifact artifact, @NonNull final Path file)
            throws IOException {
        final Path artifactFile = localRepository.resolve(ArtifactUtil.artifactToPath(artifact));
        Files.createDirectories(artifactFile.getParent());
        Files.copy(file, artifactFile, StandardCopyOption.REPLACE_EXISTING);
        writeChecksums(artifactFile);
    }

    private void writeChecksums(@NonNull final Path file) throws IOException {
        final byte[] contents = Files.readAllBytes(file);
        writeChecksum(file, contents, "MD5", ".md5");
        writeChecksum(file, contents, "SHA-1", ".sha1");
    }

    private void writeChecksum(
            @NonNull final Path file,
            final byte[] contents,
            @NonNull final String algorithm,
            @NonNull final String suffix)
            throws IOException {
        try {
            final byte[] digest = MessageDigest.getInstance(algorithm).digest(contents);
            Files.write(
                    file.resolveSibling(file.getFileName() + suffix),
                    HexFormat.of().formatHex(digest).getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest algorithm " + algorithm, e);
        }
    }

    @NonNull
    protected final String loadPropertiesContent(@NonNull final Path file) throws IOException {
        return loadAsString(file).replaceAll("^#[^\n]*\n", "");
    }

    @NonNull
    final String loadAsString(@NonNull final Path file, @Nullable final String sha256, @Nullable final String uri)
            throws IOException {
        return cleanContent(loadAsString(file), sha256, uri);
    }

    @NonNull
    final String loadAsString(@NonNull final Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
    }

    @NonNull
    protected final Path createTempJarFile() throws IOException {
        return createJarFile("data.txt", "Hi");
    }

    @NonNull
    protected final Path createJarFile(@NonNull final String filename, @NonNull final String contents)
            throws IOException {
        return createJarFile(outputStream -> createJarEntry(outputStream, filename, contents));
    }

    @NonNull
    protected final Path createJarFile(@NonNull final JarFileAction action) throws IOException {
        final Path jarFile = Files.createTempFile(FileUtil.getCurrentDirectory(), "data", ".jar");
        try (final var out = new FileOutputStream(jarFile.toFile())) {
            try (final var outputStream = new JarOutputStream(out)) {
                action.accept(outputStream);
            }
        }
        return jarFile;
    }

    protected final void createJarEntry(
            @NonNull final JarOutputStream outputStream, @NonNull final String filename, @NonNull final String contents)
            throws IOException {
        final var entry = new JarEntry(filename);
        entry.setCreationTime(FileTime.fromMillis(0));
        entry.setTime(0);
        entry.setComment(null);
        outputStream.putNextEntry(entry);
        outputStream.write(contents.getBytes(StandardCharsets.UTF_8));
        outputStream.closeEntry();
    }

    @NonNull
    final Logger createLogger(@NonNull final TestHandler handler) {
        final Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        return logger;
    }

    @NonNull
    protected final String asCleanString(
            @NonNull final ByteArrayOutputStream outputStream,
            @Nullable final String sha256,
            @Nullable final String uri) {
        return cleanContent(asString(outputStream), sha256, uri);
    }

    @NonNull
    protected final String cleanContent(
            @NonNull final String input, @Nullable final String sha256, @Nullable final String uri) {
        String content = input;
        if (null != sha256) {
            content = content.replace(sha256, "MYSHA");
        }
        if (null != uri) {
            content = content.replace(uri, "MYURI/");
        }
        return content;
    }

    @NonNull
    protected final String asString(@NonNull final ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.US_ASCII);
    }

    protected final void assertNonSystemArtifactList(
            @NonNull final ApplicationRecord record, @NonNull final String expected) {
        assertEquals(
                record.getArtifacts().stream()
                        .filter(a -> !record.getSource()
                                .isSystemArtifact(
                                        a.getArtifact().getGroupId(),
                                        a.getArtifact().getArtifactId()))
                        .map(ArtifactRecord::getKey)
                        .collect(Collectors.joining(",")),
                expected);
    }

    protected final void assertNonSystemArtifactCount(
            @NonNull final ApplicationRecord record, final int expectedCount) {
        assertEquals(
                record.getArtifacts().size(),
                expectedCount + record.getSource().getSystemArtifacts().size());
    }
}
