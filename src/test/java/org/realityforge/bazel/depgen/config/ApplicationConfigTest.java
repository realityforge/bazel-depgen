package org.realityforge.bazel.depgen.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    writeConfigFile( "" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    assertEquals( config.getConfigLocation(), getDefaultConfigFile() );
  }

  @Test
  public void parseRepositories()
    throws Exception
  {
    writeConfigFile(
      "repositories:\n" +
      "  - name: central\n" +
      "    url: http://repo1.maven.org/maven2\n" +
      "  - url: https://example.com/repo\n" +
      "    cacheLookups: false\n" +
      "    searchByDefault: false\n" +
      "    checksumPolicy: warn\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    assertEquals( config.getConfigLocation(), getDefaultConfigFile() );
    final List<RepositoryConfig> repositories = config.getRepositories();
    assertNotNull( repositories );

    assertEquals( repositories.size(), 2 );
    final RepositoryConfig repository1 = repositories.get( 0 );
    assertEquals( repository1.getName(), "central" );
    assertEquals( repository1.getUrl(), "http://repo1.maven.org/maven2" );
    assertNull( repository1.getCacheLookups() );
    assertNull( repository1.getSearchByDefault() );
    assertNull( repository1.getChecksumPolicy() );
    final RepositoryConfig repository2 = repositories.get( 1 );
    assertNull( repository2.getName() );
    assertEquals( repository2.getUrl(), "https://example.com/repo" );
    assertEquals( repository2.getCacheLookups(), Boolean.FALSE );
    assertEquals( repository2.getSearchByDefault(), Boolean.FALSE );
    assertEquals( repository2.getChecksumPolicy(), ChecksumPolicy.warn );
  }

  @Test
  public void parseDependencyWithCoords()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
  }

  @Test
  public void parseDependencyWithExcludes()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
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
  }

  @Test
  public void parseDependencyWithRepositories()
    throws Exception
  {
    writeConfigFile( "repositories:\n" +
                     "  - name: central\n" +
                     "    url: https://repo1.maven.org/maven2\n" +
                     "  - name: example\n" +
                     "    url: https://repo1.example.com/maven2\n" +
                     "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                     "    repositories: ['example']\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
    assertEquals( artifact.getRepositories(), Collections.singletonList( "example" ) );
  }

  @Test
  public void parseDependencyWithVisibility()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
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
  }

  @Test
  public void parseDependencyWithNatures()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                     "    natures: [J2cl]\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
    final List<Nature> natures = artifact.getNatures();
    assertNotNull( natures );
    assertEquals( natures, Collections.singletonList( Nature.J2cl ) );
  }

  @Test
  public void parseWithJ2clConfig()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.arez:arez-core:0.138\n" +
                     "    natures: [J2cl]\n" +
                     "    j2cl:\n" +
                     "      suppress: ['checkDebuggerStatement','other']\n" +
                     "      mode: Library\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
    final List<Nature> natures = artifact.getNatures();
    assertNotNull( natures );
    assertEquals( natures, Collections.singletonList( Nature.J2cl ) );
    final J2clConfig j2cl = artifact.getJ2cl();
    assertNotNull( j2cl );
    assertEquals( j2cl.getSuppress(), Arrays.asList( "checkDebuggerStatement", "other" ) );
    assertEquals( j2cl.getMode(), J2clMode.Library );
  }

  @Test
  public void parseWithoutJ2clConfig()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.arez:arez-core:0.138\n" +
                     "    natures: [J2cl]\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
    final List<Nature> natures = artifact.getNatures();
    assertNotNull( natures );
    assertEquals( natures, Collections.singletonList( Nature.J2cl ) );
    assertNull( artifact.getJ2cl() );
  }

  @Test
  public void artifactWithNameStrategy()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.arez:arez-core:0.138\n" +
                     "    nameStrategy: ArtifactId\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
    assertEquals( artifact.getNameStrategy(), NameStrategy.ArtifactId );
  }

  @Test
  public void artifactWithoutNameStrategy()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.arez:arez-core:0.138\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.arez:arez-core:0.138" );
    assertNull( artifact.getNameStrategy() );
  }

  @Test
  public void parseDependencyWithIncludeOptional()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                     "    includeOptional: true\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
    assertNotNull( artifact.getIncludeOptional() );
    assertTrue( artifact.getIncludeOptional() );
  }

  @Test
  public void parseDependencyWithExportDeps()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                     "    java:\n" +
                     "      exportDeps: true\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
    final JavaConfig java = artifact.getJava();
    assertNotNull( java );
    assertNotNull( java.getExportDeps() );
    assertTrue( java.getExportDeps() );
  }

  @Test
  public void generatesApi()
    throws Exception
  {
    writeConfigFile( "artifacts:\n" +
                     "  - coord: org.realityforge.gir:gir-core:jar:sources:0.08\n" +
                     "    plugin:\n" +
                     "      generatesApi: false\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );
    final ArtifactConfig artifact = ensureSingleArtifact( config );
    assertEquals( artifact.getCoord(), "org.realityforge.gir:gir-core:jar:sources:0.08" );
    final PluginConfig plugin = artifact.getPlugin();
    assertNotNull( plugin );
    final Boolean generatesApi = plugin.getGeneratesApi();
    assertNotNull( generatesApi );
    assertFalse( generatesApi );
  }

  @Test
  public void parseOptions()
    throws Exception
  {
    writeConfigFile( "options:\n" +
                     "  workspaceDirectory: workspaceDir\n" +
                     "  failOnMissingPom: false\n" +
                     "  failOnInvalidPom: false\n" +
                     "  includeSource: false\n" +
                     "  includeExternalAnnotations: true\n" +
                     "  verifyConfigSha256: false\n" +
                     "  supportDependencyOmit: true\n" +
                     "  emitDependencyGraph: false\n" +
                     "  workspaceMacroName: workspace_rules\n" +
                     "  targetMacroName: gen_targets\n" +
                     "  namePrefix: magic_\n" +
                     "  nameStrategy: ArtifactId\n" +
                     "  defaultNature: J2cl\n" +
                     "  extensionFile: workspaceDir/vendor/workspace.bzl\n" +
                     "  java:\n" +
                     "    exportDeps: true\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );

    final OptionsConfig options = config.getOptions();
    assertNotNull( options );

    assertEquals( options.getWorkspaceDirectory(), "workspaceDir" );
    assertEquals( options.getExtensionFile(), "workspaceDir/vendor/workspace.bzl" );
    assertEquals( options.getWorkspaceMacroName(), "workspace_rules" );
    assertEquals( options.getTargetMacroName(), "gen_targets" );
    assertEquals( options.getNamePrefix(), "magic_" );
    assertEquals( options.getNameStrategy(), NameStrategy.ArtifactId );
    assertEquals( options.getDefaultNature(), Nature.J2cl );
    assertEquals( options.getFailOnMissingPom(), Boolean.FALSE );
    assertEquals( options.getFailOnInvalidPom(), Boolean.FALSE );
    assertEquals( options.getEmitDependencyGraph(), Boolean.FALSE );
    assertEquals( options.getIncludeSource(), Boolean.FALSE );
    assertEquals( options.getIncludeExternalAnnotations(), Boolean.TRUE );
    assertEquals( options.getSupportDependencyOmit(), Boolean.TRUE );
    assertEquals( options.getVerifyConfigSha256(), Boolean.FALSE );
    final GlobalJavaConfig java = options.getJava();
    assertNotNull( java );
    assertEquals( java.getExportDeps(), Boolean.TRUE );
  }

  @Test
  public void parseDefaultOptions()
    throws Exception
  {
    writeConfigFile( "options: {}\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );

    final OptionsConfig options = config.getOptions();
    assertNotNull( options );

    assertNull( options.getWorkspaceDirectory() );
    assertNull( options.getExtensionFile() );
    assertNull( options.getWorkspaceMacroName() );
    assertNull( options.getTargetMacroName() );
    assertNull( options.getNamePrefix() );
    assertNull( options.getNameStrategy() );
    assertNull( options.getDefaultNature() );
    assertNull( options.getFailOnMissingPom() );
    assertNull( options.getFailOnInvalidPom() );
    assertNull( options.getEmitDependencyGraph() );
    assertNull( options.getIncludeSource() );
    assertNull( options.getIncludeExternalAnnotations() );
    assertNull( options.getSupportDependencyOmit() );
    assertNull( options.getVerifyConfigSha256() );
    assertNull( options.getJava() );
  }

  @Test
  public void parseReplacementsDefined()
    throws Exception
  {
    writeConfigFile( "replacements:\n" +
                     "  - coord: com.example:myapp\n" +
                     "    targets:\n" +
                     "      - target: \"@com_example//:myapp\"\n" +
                     "        nature: Java\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );

    final List<ReplacementConfig> replacements = config.getReplacements();
    assertNotNull( replacements );

    assertEquals( replacements.size(), 1 );
    final ReplacementConfig replacement = replacements.get( 0 );
    assertEquals( replacement.getCoord(), "com.example:myapp" );
    final List<ReplacementTargetConfig> targets = replacement.getTargets();
    assertNotNull( targets );
    final ReplacementTargetConfig target = targets.get( 0 );
    assertEquals( target.getTarget(), "@com_example//:myapp" );
    assertEquals( target.getNature(), Nature.Java );
  }

  @Test
  public void parseWithNoConfiguration()
    throws Exception
  {
    writeConfigFile( "" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );

    assertNull( config.getOptions() );
    assertNull( config.getRepositories() );
    assertNull( config.getArtifacts() );
    assertNull( config.getReplacements() );
    assertNull( config.getExcludes() );
  }

  @Test
  public void parseExcludesDefinedUsingCoord()
    throws Exception
  {
    writeConfigFile( "excludes:\n" +
                     "  - coord: com.example:myapp\n" );
    final ApplicationConfig config = loadApplicationConfig();
    assertNotNull( config );

    final List<ExcludeConfig> excludes = config.getExcludes();
    assertNotNull( excludes );

    assertEquals( excludes.size(), 1 );
    final ExcludeConfig exclude = excludes.get( 0 );
    assertEquals( exclude.getCoord(), "com.example:myapp" );
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
