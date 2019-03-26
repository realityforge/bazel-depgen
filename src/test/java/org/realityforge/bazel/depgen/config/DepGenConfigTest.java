package org.realityforge.bazel.depgen.config;

import gir.io.FileUtil;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.AbstractDepGenTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DepGenConfigTest
  extends AbstractDepGenTest
{
  @Test
  public void parseEmpty()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      assertNull( parseDependencies() );
    } );
  }

  @Test
  public void parseRepositories()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies(
        "repositories:\n  central: http://repo1.maven.org/maven2\n  example: https://example.com/repo\n" );
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      final Map<String, String> repositories = config.getRepositories();
      assertNotNull( repositories );
      assertNull( config.getArtifacts() );

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
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      assertNull( config.getRepositories() );
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
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      assertNull( config.getRepositories() );
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
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      assertNull( config.getRepositories() );
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
      final DepGenConfig config = parseDependencies();
      assertNotNull( config );
      assertNull( config.getRepositories() );
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

  @Nullable
  private DepGenConfig parseDependencies()
    throws Exception
  {
    return DepGenConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }
}
