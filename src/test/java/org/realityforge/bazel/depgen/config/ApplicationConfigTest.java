package org.realityforge.bazel.depgen.config;

import gir.io.FileUtil;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractDepGenTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationConfigTest
  extends AbstractDepGenTest
{
  @Test
  public void parseEmpty()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      assertNotNull( parseDependencies() );
    } );
  }

  @Test
  public void parseRepositories()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "repositories:\n  central: http://repo1.maven.org/maven2\n  example: https://example.com/repo\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final Map<String, String> repositories = config.getRepositories();
      assertNotNull( repositories );

      assertEquals( repositories.size(), 2 );
      assertEquals( repositories.get( "example" ), "https://example.com/repo" );
      assertEquals( repositories.get( "central" ), "http://repo1.maven.org/maven2" );
    } );
  }

  @Test
  public void parseExpandedDependencyWithAllComponents()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "artifacts:\n" +
        "  - group: org.realityforge.gir\n" +
        "    id: gir-core\n" +
        "    version: 0.08\n" +
        "    classifier: sources\n" +
        "    type: jar\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getGroup(), "org.realityforge.gir" );
      assertEquals( artifact.getId(), "gir-core" );
      assertEquals( artifact.getVersion(), "0.08" );
      assertEquals( artifact.getClassifier(), "sources" );
      assertEquals( artifact.getType(), "jar" );
      assertNull( artifact.getIds() );
      assertNull( artifact.getCoord() );
      assertNull( artifact.getExcludes() );
    } );
  }

  @Test
  public void parseExpandedDependencyWithMinimalComponents()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "artifacts:\n" +
        "  - group: org.realityforge.gir\n" +
        "    id: gir-core\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getGroup(), "org.realityforge.gir" );
      assertEquals( artifact.getId(), "gir-core" );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getCoord() );
      assertNull( artifact.getExcludes() );
    } );
  }

  @Test
  public void parseDependencyWithCoords()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "artifacts:\n" +
        "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNull( artifact.getGroup() );
      assertNull( artifact.getId() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getExcludes() );
    } );
  }

  @Test
  public void parseDependencyWithExcludes()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "artifacts:\n" +
        "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
        "    excludes: ['org.realityforge.javax.annotation:javax.annotation', 'org.realityforge.braincheck']\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNull( artifact.getGroup() );
      assertNull( artifact.getId() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      final List<String> excludes = artifact.getExcludes();
      assertNotNull( excludes );
      assertEquals( excludes.size(), 2 );

      assertTrue( excludes.contains( "org.realityforge.javax.annotation:javax.annotation" ) );
      assertTrue( excludes.contains( "org.realityforge.braincheck" ) );
    } );
  }

  @Test
  public void parseOptions()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "options:\n" +
        "  workspaceDirectory: workspaceDir\n" +
        "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );

      final OptionsConfig options = config.getOptions();
      assertNotNull( options );

      assertEquals( options.getWorkspaceDirectory(), "workspaceDir" );
      assertEquals( options.getExtensionFile(), "workspaceDir/vendor/workspace.bzl" );
    } );
  }

  @Test
  public void parseDefaultOptions()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );

      final OptionsConfig options = config.getOptions();
      assertNotNull( options );

      assertEquals( options.getWorkspaceDirectory(), OptionsConfig.DEFAULT_WORKSPACE_DIR );
      assertEquals( options.getExtensionFile(), OptionsConfig.DEFAULT_EXTENSION_FILE );
    } );
  }

  @Nonnull
  private ApplicationConfig parseDependencies()
    throws Exception
  {
    return ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }
}