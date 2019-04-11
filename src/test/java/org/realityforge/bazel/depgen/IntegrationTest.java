package org.realityforge.bazel.depgen;

import java.nio.file.Path;
import org.testng.annotations.Test;

public class IntegrationTest
  extends AbstractTest
{
  @Test
  public void validDependencySpec()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil2.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n" +
                         "  local: " + dir.toUri() + "\n" +
                         "artifacts:\n" +
                         "  - group: com.example\n" +
                         "    version: 1.0\n" +
                         "    ids: ['myapp']\n" +
                         "    classifier:\n" +
                         "    type: jar\n" +
                         "    excludes: ['org.realityforge.javax.annotation:javax.annotation']\n" );
      final String output = runCommand();
      assertOutputContains( output, "" );
    } );
  }

  @Test
  public void invalidDependencySpec()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n" +
                         "  central: http://repo1.maven.org/maven2\n" +
                         "artifacts:\n" +
                         "  - group: org.realityforge.gir\n" );

      final String output = runCommand( 5 );
      assertOutputContains( output, "The dependency must specify either the 'id' property or the 'ids' property.\n" +
                                    "--- Invalid Config ---\n" +
                                    "group: org.realityforge.gir\n" +
                                    "--- End Config ---" );
    } );
  }
}
