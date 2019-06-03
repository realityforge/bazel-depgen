package org.realityforge.bazel.depgen.util;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArtifactUtilTest
  extends AbstractTest
{
  @Test
  public void artifactToPath()
  {
    assertEquals( ArtifactUtil.artifactToPath( new DefaultArtifact( "com.example:mylib:jar:0.98" ) ),
                  "com/example/mylib/0.98/mylib-0.98.jar" );
    assertEquals( ArtifactUtil.artifactToPath( new DefaultArtifact( "com.example:mylib:jar:javadocs:0.98" ) ),
                  "com/example/mylib/0.98/mylib-0.98-javadocs.jar" );
  }

  @Test
  public void artifactToLocalFilename()
  {
    assertEquals( ArtifactUtil.artifactToLocalFilename( new DefaultArtifact( "com.example:mylib:jar:0.98" ) ),
                  "mylib-0.98.jar" );
    assertEquals( ArtifactUtil.artifactToLocalFilename( new DefaultArtifact( "com.example:mylib:jar:javadocs:0.98" ) ),
                  "mylib-0.98-javadocs.jar" );
  }
}
