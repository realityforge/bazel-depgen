package org.realityforge.bazel.depgen.config;

import gir.io.FileUtil;
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
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNull( artifact.getIncludeOptional() );
      assertNull( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getCoord() );
      assertNull( artifact.getExcludes() );
      assertNull( artifact.getVisibility() );
      assertNull( artifact.getLanguages() );
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
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNull( artifact.getIncludeOptional() );
      assertNull( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getCoord() );
      assertNull( artifact.getExcludes() );
      assertNull( artifact.getVisibility() );
      assertNull( artifact.getLanguages() );
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
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNull( artifact.getIncludeOptional() );
      assertNull( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
      assertNull( artifact.getGroup() );
      assertNull( artifact.getId() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getExcludes() );
      assertNull( artifact.getVisibility() );
      assertNull( artifact.getLanguages() );
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
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNull( artifact.getIncludeOptional() );
      assertNull( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
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
  public void parseDependencyWithVisibility()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    visibility: ['//some/package:__pkg__', '//other/package:__subpackages__']\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNull( artifact.getIncludeOptional() );
      assertNull( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
      assertNull( artifact.getGroup() );
      assertNull( artifact.getId() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getExcludes() );
      assertNull( artifact.getLanguages() );
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
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      final List<Language> languages = artifact.getLanguages();
      assertNotNull( languages );
      assertEquals( languages, Collections.singletonList( Language.J2cl ) );
    } );
  }

  @Test
  public void parseDependencyWithIncludeOptional()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "artifacts:\n" +
                         "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                         "    includeOptional: true\n" +
                         "    includeSource: false\n" );
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
      assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
      assertNull( artifact.getAlias() );
      assertNull( artifact.getNature() );
      assertNotNull( artifact.getIncludeOptional() );
      assertTrue( artifact.getIncludeOptional() );
      assertNotNull( artifact.getIncludeSource() );
      assertFalse( artifact.getIncludeSource() );
      assertNull( artifact.getExportDeps() );
      assertNull( artifact.getGeneratesApi() );
      assertNull( artifact.getGroup() );
      assertNull( artifact.getId() );
      assertNull( artifact.getVersion() );
      assertNull( artifact.getClassifier() );
      assertNull( artifact.getType() );
      assertNull( artifact.getIds() );
      assertNull( artifact.getExcludes() );
      assertNull( artifact.getVisibility() );
      assertNull( artifact.getLanguages() );
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
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
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
      final ApplicationConfig config = parseDependencies();
      assertNotNull( config );
      final List<ArtifactConfig> artifacts = config.getArtifacts();
      assertNotNull( artifacts );

      assertEquals( artifacts.size(), 1 );
      final ArtifactConfig artifact = artifacts.get( 0 );
      assertNotNull( artifact );
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
      final ApplicationConfig config = parseDependencies();
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
      final ApplicationConfig config = parseDependencies();
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
                         "  emitDependencyGraph: false\n" +
                         "  workspaceMacroName: workspace_rules\n" +
                         "  targetMacroName: gen_targets\n" +
                         "  namePrefix: magic_\n" +
                         "  aliasStrategy: ArtifactId\n" +
                         "  defaultLanguage: J2cl\n" +
                         "  extensionFile: workspaceDir/vendor/workspace.bzl\n" );
      final ApplicationConfig config = parseDependencies();
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
      assertEquals( options.isFailOnMissingPom(), Boolean.FALSE );
      assertEquals( options.isFailOnInvalidPom(), Boolean.FALSE );
      assertEquals( options.isEmitDependencyGraph(), Boolean.FALSE );
      assertEquals( options.isIncludeSource(), Boolean.FALSE );
      assertEquals( options.isExportDeps(), Boolean.TRUE );
    } );
  }

  @Test
  public void parseDefaultOptions()
    throws Exception
  {
    inIsolatedDirectory( () -> {
      writeDependencies( "options: {}\n" );
      final ApplicationConfig config = parseDependencies();
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
      assertNull( options.isFailOnMissingPom() );
      assertNull( options.isFailOnInvalidPom() );
      assertNull( options.isEmitDependencyGraph() );
      assertNull( options.isIncludeSource() );
      assertNull( options.isExportDeps() );
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
      final ApplicationConfig config = parseDependencies();
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
      final ApplicationConfig config = parseDependencies();
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
      final ApplicationConfig config = parseDependencies();
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
      final ApplicationConfig config = parseDependencies();
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
  private ApplicationConfig parseDependencies()
    throws Exception
  {
    return ApplicationConfig.parse( FileUtil.getCurrentDirectory().resolve( "dependencies.yml" ) );
  }
}
