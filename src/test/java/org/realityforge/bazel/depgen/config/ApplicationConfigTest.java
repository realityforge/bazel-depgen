package org.realityforge.bazel.depgen.config;

import gir.io.FileUtil;
import java.util.Arrays;
import java.util.Collections;
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
      final ApplicationConfig config = loadApplicationConfig();
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
      final ApplicationConfig config = loadApplicationConfig();
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
  public void parseDependencyWithCoords()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
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
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      final List<String> excludes = artifact.getExcludes();
      assertNotNull( excludes );
      assertEquals( excludes.size(), 2 );

      assertTrue( excludes.contains( "org.realityforge.javax.annotation:javax.annotation" ) );
      assertTrue( excludes.contains( "org.realityforge.braincheck" ) );
    } );
  }

  @Test
  public void parseDependencyWithVisibility()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      final List<String> visibility = artifact.getVisibility();
      assertNotNull( visibility );
      assertEquals( visibility.size(), 2 );

      assertTrue( visibility.contains( "//some/package:__pkg__" ) );
      assertTrue( visibility.contains( "//other/package:__subpackages__" ) );
    } );
  }

  @Test
  public void parseDependencyWithLanguages()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    languages: [J2cl]\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      final List<Language> languages = artifact.getLanguages();
      assertNotNull( languages );
      assertEquals( languages, Collections.singletonList( Language.J2cl ) );
    } );
  }

  @Test
  public void parseWithJ2clConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.arez:arez-core:0.138\n" +
                         "    languages: [J2cl]\n" +
                         "    j2cl:\n" +
                         "      suppress: ['checkDebuggerStatement','other']\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
      final List<Language> languages = artifact.getLanguages();
      assertNotNull( languages );
      assertEquals( languages, Collections.singletonList( Language.J2cl ) );
      final ArtifactJ2clConfig j2cl = artifact.getJ2cl();
      assertNotNull( j2cl );
      assertEquals( j2cl.getSuppress(), Arrays.asList( "checkDebuggerStatement", "other" ) );
    } );
  }

  @Test
  public void parseWithoutJ2clConfig()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.arez:arez-core:0.138\n" +
                         "    languages: [J2cl]\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
      final List<Language> languages = artifact.getLanguages();
      assertNotNull( languages );
      assertEquals( languages, Collections.singletonList( Language.J2cl ) );
      assertNull( artifact.getJ2cl() );
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
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNotNull( artifact.getIncludeOptional() );
      assertTrue( artifact.getIncludeOptional() );
    } );
  }

  @Test
  public void parseDependencyWithExportDeps()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    exportDeps: true\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNotNull( artifact.getExportDeps() );
      assertTrue( artifact.getExportDeps() );
    } );
  }

  @Test
  public void generatesApi()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    generatesApi: true\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );
      final ArtifactConfig artifact = ensureSingleArtifact( config );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNotNull( artifact.getGeneratesApi() );
      assertTrue( artifact.getGeneratesApi() );
    } );
  }

  @Test
  public void parseDependencyWithAlias()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:0.08\n" +
                         "    alias: mighty-gir-core\n" );
      final ApplicationConfig config = loadApplicationConfig();
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:0.08" );
      assertEquals( artifact.getAlias(), "mighty-gir-core" );
    } );
  }

  @Test
  public void parseDependencyWithNature()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:0.08\n" +
                         "    nature: Plugin\n" );
      final ApplicationConfig config = loadApplicationConfig();
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:0.08" );
      assertEquals( artifact.getNature(), Nature.Plugin );
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
                         "  includeSource: false\n" +
                         "  exportDeps: true\n" +
                         "  supportDependencyOmit: true\n" +
                         "  emitDependencyGraph: false\n" +
                         "  workspaceMacroName: workspace_rules\n" +
                         "  targetMacroName: gen_targets\n" +
                         "  namePrefix: magic_\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "  defaultLanguage: J2cl\n" +
                         "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final OptionsConfig options = config.getOptions();
      assertNotNull( options );

      assertEquals( options.getWorkspaceDirectory(), "workspaceDir" );
      assertEquals( options.getExtensionFile(), "workspaceDir/vendor/workspace.bzl" );
      assertEquals( options.getWorkspaceMacroName(), "workspace_rules" );
      assertEquals( options.getTargetMacroName(), "gen_targets" );
      assertEquals( options.getNamePrefix(), "magic_" );
      assertEquals( options.getAliasStrategy(), AliasStrategy.ArtifactId );
      assertEquals( options.getDefaultLanguage(), Language.J2cl );
      assertEquals( options.getFailOnMissingPom(), Boolean.FALSE );
      assertEquals( options.getFailOnInvalidPom(), Boolean.FALSE );
      assertEquals( options.getEmitDependencyGraph(), Boolean.FALSE );
      assertEquals( options.getIncludeSource(), Boolean.FALSE );
      assertEquals( options.getExportDeps(), Boolean.TRUE );
      assertEquals( options.getSupportDependencyOmit(), Boolean.TRUE );
    } );
  }

  @Test
  public void parseDefaultOptions()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "options: {}\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final OptionsConfig options = config.getOptions();
      assertNotNull( options );

      assertNull( options.getWorkspaceDirectory() );
      assertNull( options.getExtensionFile() );
      assertNull( options.getWorkspaceMacroName() );
      assertNull( options.getTargetMacroName() );
      assertNull( options.getNamePrefix() );
      assertNull( options.getAliasStrategy() );
      assertNull( options.getDefaultLanguage() );
      assertNull( options.getFailOnMissingPom() );
      assertNull( options.getFailOnInvalidPom() );
      assertNull( options.getEmitDependencyGraph() );
      assertNull( options.getIncludeSource() );
      assertNull( options.getExportDeps() );
      assertNull( options.getSupportDependencyOmit() );
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
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final List<ReplacementConfig> replacements = config.getReplacements();
      assertNotNull( replacements );

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
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final List<ReplacementConfig> replacements = config.getReplacements();
      assertNotNull( replacements );

      assertEquals( replacements.size(), 1 );
      final ReplacementConfig replacement = replacements.get( 0 );
      assertEquals( replacement.getTarget(), "@com_example//:myapp" );
      assertEquals( replacement.getCoord(), "com.example:myapp" );
      assertNull( replacement.getGroup() );
      assertNull( replacement.getId() );
    } );
  }

  @Test
  public void parseWithNoConfiguration()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      assertNull( config.getOptions() );
      assertNull( config.getRepositories() );
      assertNull( config.getArtifacts() );
      assertNull( config.getReplacements() );
      assertNull( config.getExcludes() );
    } );
  }

  @Test
  public void parseExcludesDefinedUsingGroupAndId()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "excludes:\n" +
                         "  - group: com.example\n" +
                         "    id: myapp\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final List<ExcludeConfig> excludes = config.getExcludes();
      assertNotNull( excludes );

      assertEquals( excludes.size(), 1 );
      final ExcludeConfig exclude = excludes.get( 0 );
      assertEquals( exclude.getGroup(), "com.example" );
      assertEquals( exclude.getId(), "myapp" );
      assertNull( exclude.getCoord() );
    } );
  }

  @Test
  public void parseExcludesDefinedUsingCoord()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "excludes:\n" +
                         "  - coord: com.example:myapp\n" );
      final ApplicationConfig config = loadApplicationConfig();
      assertNotNull( config );

      final List<ExcludeConfig> excludes = config.getExcludes();
      assertNotNull( excludes );

      assertEquals( excludes.size(), 1 );
      final ExcludeConfig exclude = excludes.get( 0 );
      assertEquals( exclude.getCoord(), "com.example:myapp" );
      assertNull( exclude.getGroup() );
      assertNull( exclude.getId() );
    } );
  }

  @Nonnull
  private ArtifactConfig ensureSingleArtifact( @Nonnull final ApplicationConfig config )
  {
    final List<ArtifactConfig> artifacts = config.getArtifacts();
    assertNotNull( artifacts );

    assertEquals( artifacts.size(), 1 );
    final ArtifactConfig artifact = artifacts.get( 0 );
    assertNotNull( artifact );
    return artifact;
  }
}
