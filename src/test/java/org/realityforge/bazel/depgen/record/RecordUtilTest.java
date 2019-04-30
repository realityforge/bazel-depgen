package org.realityforge.bazel.depgen.record;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RecordUtilTest
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
}
