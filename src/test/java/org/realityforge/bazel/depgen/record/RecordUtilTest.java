package org.realityforge.bazel.depgen.record;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.FileUtil2;
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
      final Path filename = FileUtil2.createLocalTempDir().resolve( "file.txt" );
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
      final Path filename = FileUtil2.createLocalTempDir().resolve( "file.txt" );
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
      final Path dir1 = FileUtil2.createLocalTempDir();
      final Path dir2 = FileUtil2.createLocalTempDir();
      final Path dir3 = FileUtil2.createLocalTempDir();

      final URI uri = dir1.toUri();

      final RemoteRepository repo1 = new RemoteRepository.Builder( "dir1", "default", uri.toString() ).build();
      final RemoteRepository repo2 = new RemoteRepository.Builder( "dir2", "default", dir2.toUri().toString() ).build();
      final RemoteRepository repo3 = new RemoteRepository.Builder( "dir3", "default", dir3.toUri().toString() ).build();

      deployTempArtifactToLocalRepository( dir1, "com.example:myapp:1.0" );
      deployTempArtifactToLocalRepository( dir2, "com.example:myapp:1.0" );

      final List<String> urls =
        RecordUtil.deriveUrls( new DefaultArtifact( "com.example:myapp:jar:1.0" ),
                               Arrays.asList( repo1, repo2, repo3 ) );
      assertEquals( urls.size(), 2 );
      assertTrue( urls.get( 0 ).startsWith( repo1.getUrl() ) );
      assertTrue( urls.get( 1 ).startsWith( repo2.getUrl() ) );
      assertTrue( urls.get( 0 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
      assertTrue( urls.get( 1 ).endsWith( "com/example/myapp/1.0/myapp-1.0.jar" ) );
    } );
  }
}
