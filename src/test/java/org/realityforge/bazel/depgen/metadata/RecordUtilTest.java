package org.realityforge.bazel.depgen.metadata;

import static org.testng.Assert.*;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gir.io.FileUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Executors;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.DepgenConfigurationException;
import org.realityforge.bazel.depgen.DepgenException;
import org.realityforge.bazel.depgen.record.ApplicationRecord;
import org.testng.annotations.Test;

public final class RecordUtilTest extends AbstractTest {
    @Test
    public void sha256() throws Exception {
        final Path filename = FileUtil.createLocalTempDir().resolve("file.txt");
        Files.write(filename, new byte[] {1, 2, 3});
        assertEquals(
                RecordUtil.sha256(filename.toFile()),
                "039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81");
    }

    @Test
    public void sha256_badFile() throws Exception {
        final Path filename = FileUtil.createLocalTempDir().resolve("file.txt");
        final DepgenException exception =
                expectThrows(DepgenException.class, () -> RecordUtil.sha256(filename.toFile()));
        assertEquals(exception.getMessage(), "Error generating sha256 hash for file " + filename.toFile());
    }

    @Test
    public void readAnnotationProcessors_notAJar() throws Exception {
        final Path path = FileUtil.createLocalTempDir().resolve("file.txt");
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readAnnotationProcessors_jarButNoProcessors() throws Exception {
        final Path path = createTempJarFile();
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readAnnotationProcessors_jarNotReadable() throws Exception {
        // Remove read permission so that attempting to read metadata generates an IOException
        final Path path = createTempJarFile();
        // Remove read permission so that attempting to read metadata generates an IOException
        Files.setPosixFilePermissions(path, new HashSet<>());
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readAnnotationProcessors_jarWithSingleProcessor() throws Exception {
        final Path path = createJarFile(
                "META-INF/services/javax.annotation.processing.Processor", "react4j.processor.ReactProcessor\n");
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, "react4j.processor.ReactProcessor");
    }

    @Test
    public void readAnnotationProcessors_jarWithSingleProcessorAndComments() throws Exception {
        final Path path = createJarFile("META-INF/services/javax.annotation.processing.Processor", """
             # Copyright some megacorp!
            react4j.processor.ReactProcessor
            """);
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, "react4j.processor.ReactProcessor");
    }

    @Test
    public void readAnnotationProcessors_jarWithSingleProcessorAndBlankLines() throws Exception {
        final Path path = createJarFile(
                "META-INF/services/javax.annotation.processing.Processor", "\nreact4j.processor.ReactProcessor\n\n\n");
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, "react4j.processor.ReactProcessor");
    }

    @Test
    public void readAnnotationProcessors_jarWithMultipleProcessors() throws Exception {
        final Path path = createJarFile(
                "META-INF/services/javax.annotation.processing.Processor",
                "react4j.processor.ReactProcessor\narez.processor.ArezProcessor\n");
        final String processors = RecordUtil.readAnnotationProcessors(path.toFile());
        assertEquals(processors, "react4j.processor.ReactProcessor,arez.processor.ArezProcessor");
    }

    @Test
    public void readJsAssets_notAJar() throws Exception {
        final Path path = FileUtil.createLocalTempDir().resolve("file.txt");
        final String processors = RecordUtil.readJsAssets(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readJsAssets_jarButNoJsAssets() throws Exception {
        final Path path = createTempJarFile();
        final String processors = RecordUtil.readJsAssets(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readJsAssets_jarNotReadable() throws Exception {
        // Remove read permission so that attempting to read metadata generates an IOException
        final Path path = createTempJarFile();
        // Remove read permission so that attempting to read metadata generates an IOException
        Files.setPosixFilePermissions(path, new HashSet<>());
        final String processors = RecordUtil.readJsAssets(path.toFile());
        assertEquals(processors, DepgenMetadata.SENTINEL);
    }

    @Test
    public void readJsAssets_jarSingleJsAsset() throws Exception {
        final Path path = createJarFile(outputStream -> createJarEntry(outputStream, "com/biz/MyFile.js", ""));
        final String processors = RecordUtil.readJsAssets(path.toFile());
        assertEquals(processors, "com/biz/MyFile.js");
    }

    @Test
    public void readJsAssets_jarMultipleAssets() throws Exception {
        final Path path = createJarFile(outputStream -> {
            createJarEntry(outputStream, "com/biz/MyFile1.js", "");
            createJarEntry(outputStream, "com/biz/MyOtherFile.js", "");
            createJarEntry(outputStream, "com/biz/MyBlah.js", "");
            createJarEntry(outputStream, "com/biz/public/NotIncludedAsNestedInPublic.js", "");
            createJarEntry(outputStream, "com/biz/TheClass.native.js", "");
            createJarEntry(outputStream, "com/public/biz/NotIncludedAsNestedDeeplyInPublic.js", "");
        });
        final String processors = RecordUtil.readJsAssets(path.toFile());
        assertEquals(processors, "com/biz/MyBlah.js,com/biz/MyFile1.js,com/biz/MyOtherFile.js");
    }

    @Test
    public void lookupArtifactInRepository_file_url() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        final URI uri = dir.toUri();

        final var repo = new RemoteRepository.Builder("dir1", "default", uri.toString()).build();

        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        final String url = requireNonNull(RecordUtil.lookupArtifactInRepository(
                new DefaultArtifact("com.example:myapp:jar:1.0"), repo, Collections.emptyMap()));
        assertTrue(url.startsWith(repo.getUrl()));
        assertTrue(url.endsWith("com/example/myapp/1.0/myapp-1.0.jar"));
    }

    @Test
    public void lookupArtifactInRepository_file_url_missing() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        final URI uri = dir.toUri();

        final var repo = new RemoteRepository.Builder("dir1", "default", uri.toString()).build();

        final String url = RecordUtil.lookupArtifactInRepository(
                new DefaultArtifact("com.example:myapp:jar:1.0"), repo, Collections.emptyMap());
        assertNull(url);
    }

    @Test
    public void lookupArtifactInRepository_unknown_protocol() {
        final var repo = new RemoteRepository.Builder("dir1", "default", "ftp://example.com").build();

        final DepgenConfigurationException exception = expectThrows(
                DepgenConfigurationException.class,
                () -> RecordUtil.lookupArtifactInRepository(
                        new DefaultArtifact("com.example:myapp:jar:1.0"), repo, Collections.emptyMap()));
        assertEquals(
                exception.getMessage(),
                "Unsupported repository protocol for com.example:myapp:jar:1.0 with url"
                        + " ftp://example.com/com/example/myapp/1.0/myapp-1.0.jar.");
    }

    @Test
    public void lookupArtifactInRepository_http_url() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        final TinyHttpd server = TinyHttpdFactory.createServer();
        server.setHttpHandler(e -> serveFilePath(dir, e));

        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        server.start();
        try {
            final var repo = new RemoteRepository.Builder("http", "default", server.getBaseURL()).build();

            final String url = requireNonNull(RecordUtil.lookupArtifactInRepository(
                    new DefaultArtifact("com.example:myapp:jar:1.0"), repo, Collections.emptyMap()));
            assertTrue(url.startsWith(repo.getUrl()));
            assertTrue(url.endsWith("com/example/myapp/1.0/myapp-1.0.jar"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void lookupArtifactInRepository_http_url_missing() throws Exception {
        final Path dir = FileUtil.createLocalTempDir();

        final TinyHttpd server1 = TinyHttpdFactory.createServer();
        server1.setHttpHandler(e -> serveFilePath(dir, e));

        server1.start();
        try {
            final var repo = new RemoteRepository.Builder("http", "default", server1.getBaseURL()).build();

            final String url = RecordUtil.lookupArtifactInRepository(
                    new DefaultArtifact("com.example:myapp:jar:1.0"), repo, Collections.emptyMap());
            assertNull(url);
        } finally {
            server1.stop();
        }
    }

    @Test
    public void lookupArtifactInRepository_authenticated_http_url() throws Exception {
        final String username = "root";
        final String password = "secret";
        emitSettings("my-repo", username, password);

        final Path dir = FileUtil.createLocalTempDir();
        deployDepGenArtifactToLocalRepository(dir);

        final HttpServer server = serveDirectoryWithBasicAuth(dir, username, password);

        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        server.start();
        try {
            final String repositoryUrl = toUrl(server);

            writeConfigFile("repositories:\n" + "  - name: my-repo\n" + "    url: " + repositoryUrl + "\n");
            final ApplicationRecord record = loadApplicationRecord();

            final String url = requireNonNull(RecordUtil.lookupArtifactInRepository(
                    new DefaultArtifact("com.example:myapp:jar:1.0"),
                    record.getNode().getRepositories().get(0),
                    record.getAuthenticationContexts()));
            assertTrue(url.startsWith(repositoryUrl));
            assertTrue(url.endsWith("com/example/myapp/1.0/myapp-1.0.jar"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void lookupArtifactInRepository_authenticated_http_url_with_creds_in_url() throws Exception {
        final String username = "root";
        final String password = "secret";

        final Path dir = FileUtil.createLocalTempDir();
        deployDepGenArtifactToLocalRepository(dir);

        final HttpServer server = serveDirectoryWithBasicAuth(dir, username, password);

        deployTempArtifactToLocalRepository(dir, "com.example:myapp:1.0");

        server.start();
        try {
            final InetSocketAddress address = server.getAddress();
            final String repositoryUrl = "http://" + username
                    + ":"
                    + password
                    + "@"
                    + address.getAddress().getCanonicalHostName()
                    + ":"
                    + address.getPort()
                    + "/";
            final String repositoryUrlSansAuth =
                    "http://" + address.getAddress().getCanonicalHostName() + ":" + address.getPort() + "/";

            writeConfigFile("repositories:\n" + "  - name: my-repo\n" + "    url: " + repositoryUrl + "\n");
            final ApplicationRecord record = loadApplicationRecord();

            final String url = requireNonNull(RecordUtil.lookupArtifactInRepository(
                    new DefaultArtifact("com.example:myapp:jar:1.0"),
                    record.getNode().getRepositories().get(0),
                    record.getAuthenticationContexts()));
            assertTrue(url.startsWith(repositoryUrlSansAuth));
            assertTrue(url.endsWith("com/example/myapp/1.0/myapp-1.0.jar"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void lookupArtifactInRepository_authenticated_http_url_missing() throws Exception {
        final String username = "root";
        final String password = "secret";
        emitSettings("my-repo", username, password);

        final Path dir = FileUtil.createLocalTempDir();

        final HttpServer server = serveDirectoryWithBasicAuth(dir, username, password);

        server.start();
        try {
            final String repositoryUrl = toUrl(server);

            writeConfigFile("repositories:\n" + "  - name: my-repo\n" + "    url: " + repositoryUrl + "\n");
            deployDepGenArtifactToLocalRepository(dir);

            final ApplicationRecord record = loadApplicationRecord();

            final String url = RecordUtil.lookupArtifactInRepository(
                    new DefaultArtifact("com.example:myapp:jar:1.0"),
                    record.getNode().getRepositories().get(0),
                    record.getAuthenticationContexts());
            assertNull(url);
        } finally {
            server.stop(0);
        }
    }

    @SuppressWarnings("SameParameterValue")
    @NonNull
    private HttpServer serveDirectoryWithBasicAuth(
            @NonNull final Path dir, @NonNull final String username, @NonNull final String password)
            throws IOException {
        final var server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
        server.createContext("/", e -> serveFilePath(dir, e)).setAuthenticator(new BasicAuthenticator("MyRealm") {
            @Override
            public boolean checkCredentials(
                    @NonNull final String suppliedUsername, @NonNull final String suppliedPassword) {
                return username.equals(suppliedUsername) && password.equals(suppliedPassword);
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    @NonNull
    private String toUrl(@NonNull final HttpServer server) {
        final InetSocketAddress address = server.getAddress();
        return "http://" + address.getAddress().getCanonicalHostName() + ":" + address.getPort() + "/";
    }

    @SuppressWarnings("SameParameterValue")
    private void emitSettings(final String serverId, final String username, final String password) throws IOException {
        final String settingsContent = "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" + "  <servers>\n"
                + "    <server>\n"
                + "      <id>"
                + serverId + "</id>\n" + "      <username>"
                + username + "</username>\n" + "      <password>"
                + password + "</password>\n" + "    </server>\n"
                + "  </servers>\n"
                + "</settings>\n";
        final Path settingsFile = FileUtil.getCurrentDirectory().resolve("settings.xml");
        Files.writeString(settingsFile, settingsContent);
    }

    private void serveFilePath(@NonNull final Path baseDirectory, @NonNull final HttpExchange httpExchange)
            throws IOException {
        final String path = httpExchange.getRequestURI().getPath();
        final Path file = baseDirectory.resolve(path.substring(1));
        if (file.toFile().exists()) {
            if (httpExchange.getRequestMethod().equals("HEAD")) {
                httpExchange.sendResponseHeaders(200, -1);
            } else {
                final byte[] data = Files.readAllBytes(file);
                httpExchange.sendResponseHeaders(200, data.length);
                httpExchange.getResponseBody().write(data);
                httpExchange.close();
            }
        } else {
            httpExchange.sendResponseHeaders(404, -1);
            httpExchange.close();
        }
    }
}
