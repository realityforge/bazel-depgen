package org.realityforge.bazel.depgen;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

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
      final String output = runCommand( "generate" );
      assertEquals( output, "" );
    } );
  }

  @Test
  public void hash()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" +
                         "    excludes: ['org.realityforge.javax.annotation:javax.annotation']\n" );
      final String output = runCommand( "hash" );
      assertEquals( output, "Content SHA256: A8060A486659CC1397477EFE6C28F83F65DC9CBB19182978AFB69746EC3D99F2\n" );
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

      final String output = runCommand( 5, "generate" );
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

      final String output = runCommand( 3, "generate" );
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

      final String output = runCommand( 3, "--verbose", "generate" );
      assertOutputContains( output, "Error: Failed to read dependencies file " );
      assertOutputContains( output, "Cause: while scanning a quoted scalar" );
      assertOutputContains( output, "\tat org.yaml.snakeyaml.Yaml.load(" );
    } );
  }

  @Test
  public void noOutputInNormalCase()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      writeDependencies( "" );

      final String output = runCommand( "generate" );
      assertEquals( output, "" );
    } );
  }

  @Test
  public void printGraph()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();

      deployTempArtifactToLocalRepository( dir, "com.example:myapp:1.0" );

      writeWorkspace();
      writeDependencies( dir,
                         "artifacts:\n" +
                         "  - coord: com.example:myapp:1.0\n" );

      final String output = runCommand( "print-graph" );
      assertEquals( output,
                    "Dependency Graph:\n" +
                    "\\- com.example:myapp:jar:1.0 [compile]\n" );
    } );
  }

  @Test
  public void missingDefaultSettingsIsFine()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      runCommand( "generate" );
    } );
  }

  @Test
  public void invalidDefaultSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2" ).toFile().mkdir() );
      FileUtil.write( ".m2/settings.xml", "JHSGDJHDS()*&(&Y*&" );

      final String output = runCommand( 4, "generate" );
      assertOutputContains( output, "Error: Problem loading settings from " );
    } );
  }

  @Test
  public void defaultSettingsIsDirectory()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );
      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2/settings.xml" ).toFile().mkdirs() );

      final String output = runCommand( 4, "generate" );
      assertOutputContains( output, "Error: Problem loading settings from " );
    } );
  }

  @Test
  public void validDefaultSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      assertTrue( FileUtil.getCurrentDirectory().resolve( ".m2" ).toFile().mkdir() );
      FileUtil.write( ".m2/settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final String output = runCommand( "generate" );
      assertOutputContains( output, "" );
    } );
  }

  @Test
  public void validSpecifiedSettings()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeWorkspace();
      // Need to declare repositories otherwise we never even try to load settings
      writeDependencies( "repositories:\n  central: http://repo1.maven.org/maven2\n" );

      FileUtil.write( "some_settings.xml",
                      "<settings xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                      "  <servers>\n" +
                      "    <server>\n" +
                      "      <id>my-repo</id>\n" +
                      "      <username>root</username>\n" +
                      "      <password>secret</password>\n" +
                      "    </server>\n" +
                      "  </servers>\n" +
                      "</settings>\n" );

      final String output = runCommand( "--settings-file", "some_settings.xml", "generate" );
      assertOutputContains( output, "" );
    } );
  }
}
