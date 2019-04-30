package org.realityforge.bazel.depgen.record;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
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
}
