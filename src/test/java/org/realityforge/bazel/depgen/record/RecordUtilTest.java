package org.realityforge.bazel.depgen.record;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gir.io.FileUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.guiceyloops.server.http.TinyHttpd;
import org.realityforge.guiceyloops.server.http.TinyHttpdFactory;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RecordUtilTest
  extends AbstractTest
{
  @Test
  public void toArtifactKey_dependencyNode()
  {
    assertEquals( RecordUtil.toArtifactKey( new DefaultDependencyNode( new DefaultArtifact( "com.example:mylib:jar:0.98" ) ) ),
                  "com.example:mylib" );
  }

  @Test
  public void artifactToPath()
  {
    assertEquals( RecordUtil.artifactToPath( new DefaultArtifact( "com.example:mylib:jar:0.98" ) ),
                  "com/example/mylib/0.98/mylib-0.98.jar" );
    assertEquals( RecordUtil.artifactToPath( new DefaultArtifact( "com.example:mylib:jar:javadocs:0.98" ) ),
                  "com/example/mylib/0.98/mylib-0.98-javadocs.jar" );
  }

  @Test
  public void sha256()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path filename = FileUtil.createLocalTempDir().resolve( "file.txt" );
      Files.write( filename, new byte[]{ 1, 2, 3 } );
      assertEquals( RecordUtil.sha256( filename.toFile() ),
                    "039058C6F2C0CB492C533B0A4D14EF77CC0F78ABCCCED5287D84A1A2011CFB81" );
    } );
  }

  @Test
  public void sha256_badFile()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path filename = FileUtil.createLocalTempDir().resolve( "file.txt" );
      final IllegalStateException exception =
        expectThrows( IllegalStateException.class, () -> RecordUtil.sha256( filename.toFile() ) );
      assertEquals( exception.getMessage(), "Error generating sha256 hash for file " + filename.toFile() );
    } );
  }

  @Test
  public void deriveUrls_multipleFileURLs()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir1 = FileUtil.createLocalTempDir();
      final Path dir2 = FileUtil.createLocalTempDir();
      final Path dir3 = FileUtil.createLocalTempDir();

      final URI uri = dir1.toUri();

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", uri.toString() ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", dir2.toUri().toString() ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", dir3.toUri().toString() ).build();

      deployTempArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir2, "com.example:myapp:1.0" );

      final List<String> urls =
        RecordUtil.deriveUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                               Arrays.asList( repo1, repo2, repo3 ),
                               Collections.emptyMap() );
      assertEquals( urls.size(), 2 );
      assertTrue( urls.get( 0 ).startsWith( repo1.getUrl() ) );
      assertTrue( urls.get( 1 ).startsWith( repo2.getUrl() ) );
      assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertTrue( urls.get( 1 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
    } );
  }

  @Test
  public void deriveUrls_httpUrls()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir1 = FileUtil.createLocalTempDir();
      final Path dir2 = FileUtil.createLocalTempDir();

      final TinyHttpd server1 = TinyHttpdFactory.createServer();
      final TinyHttpd server2 = TinyHttpdFactory.createServer();
      server1.setHttpHandler( e -> serveFilePath( dir1, e ) );
      server2.setHttpHandler( e -> serveFilePath( dir2, e ) );

      deployTempArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );

      server1.start();
      server2.start();
      try
      {
        final RemoteRepository repo1 = new RemoteRepository.Builder( "http", "default", server1.getBaseURL() ).build();
        final RemoteRepository repo2 = new RemoteRepository.Builder( "http", "default", server2.getBaseURL() ).build();

        final List<String> urls =
          RecordUtil.deriveUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                                 Arrays.asList( repo1, repo2 ),
                                 Collections.emptyMap() );
        assertEquals( urls.size(), 1 );
        assertTrue( urls.get( 0 ).startsWith( repo1.getUrl() ) );
        assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      }
      finally
      {
        server1.stop();
        server2.stop();
      }
    } );
  }

  @Test
  public void deriveUrls_authenticatedHttpUrls()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path settingsFile = FileUtil.getCurrentDirectory().resolve( "settings.xml" );
      final String settingsContent =
        "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
        "  <servers>\n" +
        "    <server>\n" +
        "      <id>my-repo</id>\n" +
        "      <username>root</username>\n" +
        "      <password>secret</password>\n" +
        "    </server>\n" +
        "  </servers>\n" +
        "</settings>\n";
      Files.write( settingsFile, settingsContent.getBytes( StandardCharsets.UTF_8 ) );

      final Path dir = FileUtil.createLocalTempDir();

      final HttpServer server = HttpServer.create( new InetSocketAddress( InetAddress.getLocalHost(), 0 ), 0 );
      server
        .createContext( "/", e -> serveFilePath( dir, e ) )
        .setAuthenticator( new BasicAuthenticator( "MyRealm" )
        {
          @Override
          public boolean checkCredentials( @Nonnull final String username, @Nonnull final String password )
          {
            return username.equals( "root" ) && password.equals( "secret" );
          }
        } );
      server.setExecutor( Executors.newCachedThreadPool() );

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      server.start();
      try
      {
        final InetSocketAddress address1 = server.getAddress();
        final String repositoryUrl =
          "http://" + address1.getAddress().getCanonicalHostName() + ":" + address1.getPort() + "/";

        writeDependencies( dir,
                           "repositories:\n" +
                           "  my-repo: " + repositoryUrl + "\n" );
        final ApplicationRecord record = loadApplicationRecord();

        final List<String> urls =
          RecordUtil.deriveUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                                 record.getNode().getRepositories(),
                                 record.getAuthenticationContexts() );
        assertEquals( urls.size(), 1 );
        assertTrue( urls.get( 0 ).startsWith( repositoryUrl ) );
        assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      }
      finally
      {
        server.stop( 1 );
      }
    } );
  }

  private void serveFilePath( @Nonnull final Path baseDirectory, @Nonnull final HttpExchange httpExchange )
    throws IOException
  {
    final String path = httpExchange.getRequestURI().getPath();
    final Path file = baseDirectory.resolve( path.substring( 1 ) );
    if ( file.toFile().exists() )
    {
      if ( httpExchange.getRequestMethod().equals( "HEAD" ) )
      {
        httpExchange.sendResponseHeaders( 200, -1 );
      }
      else
      {
        final byte[] data = Files.readAllBytes( file );
        httpExchange.sendResponseHeaders( 200, data.length );
        httpExchange.getResponseBody().write( data );
      }
    }
    else
    {
      httpExchange.close();
    }
  }
}
