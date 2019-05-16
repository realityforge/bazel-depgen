package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
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
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - group: com.example\n" +
                         "    version: 1.0\n" +
                         "    ids: ['myapp']\n" +
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

  @Test
  public void invalidYaml()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "artifacts: 's\n" +
                         "  - group: org.realityforge.gir\n" );

      final String output = runCommand( 3 );
      assertOutputContains( output, "Error: Failed to read dependencies file " );
      assertOutputContains( output, "Cause: while scanning a quoted scalar" );
      assertOutputDoesNotContain( output, "\tat org.yaml.snakeyaml.Yaml.load(" );
    } );
  }

  @Test
  public void invalidYamlInVerboseMode()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "artifacts: 's\n" +
                         "  - group: org.realityforge.gir\n" );

      final String output = runCommand( 3, "--verbose" );
      assertOutputContains( output, "Error: Failed to read dependencies file " );
      assertOutputContains( output, "Cause: while scanning a quoted scalar" );
      assertOutputContains( output, "\tat org.yaml.snakeyaml.Yaml.load(" );
    } );
  }
}
