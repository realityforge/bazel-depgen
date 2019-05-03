package org.realityforge.bazel.depgen.config;

import gir.io.FileUtil;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationConfigTest
  extends AbstractTest
{
  @Test
  public void parseEmpty()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      assertEquals( config.getConfigLocation(), FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
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
      assertEquals( config.getConfigLocation(), FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
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
      writeDependencies( "artifacts:\n" +
                         "  - group: org.realityforge.gir\n" +
                         "    id: gir-core\n" +
                         "    version: 0.08\n" +
                         "    classifier: sources\n" +
                         "    type: jar\n" );
      final ApplicationConfig config = parseDependencies();
      assertEquals( config.getConfigLocation(), FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
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
      assertFalse( artifact.isIncludeOptional() );
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
      writeDependencies( "artifacts:\n" +
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
      assertFalse( artifact.isIncludeOptional() );
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
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNull( artifact.isIncludeOptional() );
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
      writeDependencies( "artifacts:\n" +
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
      assertNull( artifact.isIncludeOptional() );
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
  public void parseDependencyWithIncludeOptional()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    includeOptional: true\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertTrue( artifact.isIncludeOptional() );
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
  public void parseOptions()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "options:\n" +
                         "  workspaceDirectory: workspaceDir\n" +
                         "  failOnMissingPom: false\n" +
                         "  failOnInvalidPom: false\n" +
                         "  emitDependencyGraph: false\n" +
                         "  generateRulesMacroName: workspace_rules\n" +
                         "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );

      final OptionsConfig options = config.getOptions();
      assertNotNull( options );

      assertEquals( options.getWorkspaceDirectory(), "workspaceDir" );
      assertEquals( options.getExtensionFile(), "workspaceDir/vendor/workspace.bzl" );
      assertEquals( options.getGenerateRulesMacroName(), "workspace_rules" );
      assertFalse( options.isFailOnMissingPom() );
      assertFalse( options.isFailOnInvalidPom() );
      assertFalse( options.isEmitDependencyGraph() );
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
      assertEquals( options.getGenerateRulesMacroName(), OptionsConfig.DEFAULT_GENERATE_RULES_MACRO_NAME );
      assertTrue( options.isFailOnMissingPom() );
      assertTrue( options.isFailOnInvalidPom() );
      assertTrue( options.isEmitDependencyGraph() );
    } );
  }

  @Test
  public void parseReplacementsDefinedUsingGroupAndId()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "replacements:\n" +
                         "  - group: com.example\n" +
                         "    id: myapp\n" +
                         "    target: \"@com_example//:myapp\"\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );

      final List<ReplacementConfig> replacements = config.getReplacements();

      assertEquals( replacements.size(), 1 );
      final ReplacementConfig replacement = replacements.get( 0 );
      assertEquals( replacement.getTarget(), "@com_example//:myapp" );
      assertEquals( replacement.getGroup(), "com.example" );
      assertEquals( replacement.getId(), "myapp" );
      assertNull( replacement.getCoord() );
    } );
  }

  @Test
  public void parseReplacementsDefinedUsingCoord()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "replacements:\n" +
                         "  - coord: com.example:myapp\n" +
                         "    target: \"@com_example//:myapp\"\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );

      final List<ReplacementConfig> replacements = config.getReplacements();

      assertEquals( replacements.size(), 1 );
      final ReplacementConfig replacement = replacements.get( 0 );
      assertEquals( replacement.getTarget(), "@com_example//:myapp" );
      assertEquals( replacement.getCoord(), "com.example:myapp" );
      assertNull( replacement.getGroup() );
      assertNull( replacement.getId() );
    } );
  }

  @Nonnull
  private ApplicationConfig parseDependencies()
    throws Exception
  {
    return ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }
}
