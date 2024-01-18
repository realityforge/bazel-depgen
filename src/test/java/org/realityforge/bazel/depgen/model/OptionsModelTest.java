package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.NameStrategy;
import org.realityforge.bazel.depgen.config.GlobalJavaConfig;
import org.realityforge.bazel.depgen.config.Nature;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class OptionsModelTest
  extends AbstractTest
{
  @Test
  public void parseWithDefaults()
  {
    final OptionsConfig source = new OptionsConfig();

    final OptionsModel model = OptionsModel.parse( FileUtil.getCurrentDirectory(), source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(),
                  FileUtil.getCurrentDirectory().resolve( ".." ).toAbsolutePath().normalize() );
    assertEquals( model.getExtensionFile(),
                  FileUtil.getCurrentDirectory().resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) );
    assertEquals( model.getWorkspaceMacroName(), OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME );
    assertEquals( model.getTargetMacroName(), OptionsConfig.DEFAULT_TARGET_MACRO_NAME );
    assertEquals( model.getNamePrefix(), OptionsConfig.DEFAULT_NAME_PREFIX );
    assertEquals( model.getNameStrategy(), OptionsConfig.DEFAULT_NAME_STRATEGY );
    assertEquals( model.getDefaultNature(), OptionsConfig.DEFAULT_NATURE );
    assertTrue( model.failOnMissingPom() );
    assertTrue( model.failOnInvalidPom() );
    assertTrue( model.emitDependencyGraph() );
    assertTrue( model.includeSource() );
    assertFalse( model.includeExternalAnnotations() );
    assertFalse( model.exportDeps() );
    assertFalse( model.supportDependencyOmit() );
    assertTrue( model.verifyConfigSha256() );
  }

  @Test
  public void parseWithNamePrefixSpecified()
  {
    final OptionsConfig source = new OptionsConfig();
    source.setNamePrefix( "myprj" );

    final OptionsModel model = OptionsModel.parse( FileUtil.getCurrentDirectory(), source );
    assertEquals( model.getWorkspaceMacroName(), "myprj_" + OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME );
    assertEquals( model.getTargetMacroName(), "myprj_" + OptionsConfig.DEFAULT_TARGET_MACRO_NAME );
    assertEquals( model.getNamePrefix(), "myprj_" );
  }

  @Test
  public void parse()
    throws Exception
  {
    final Path dir = FileUtil.createLocalTempDir();
    final Path thirdpartyDir = dir.resolve( "thirdparty" );

    final OptionsConfig source = new OptionsConfig();
    source.setWorkspaceDirectory( ".." );
    source.setExtensionFile( "dependencies.bzl" );
    source.setWorkspaceMacroName( "gen_myprj_dependency_rules" );
    source.setTargetMacroName( "gen_myprj_targets" );
    source.setNamePrefix( "myprj_" );
    source.setNameStrategy( NameStrategy.ArtifactId );
    source.setDefaultNature( Nature.J2cl );
    source.setFailOnMissingPom( false );
    source.setFailOnInvalidPom( false );
    source.setEmitDependencyGraph( false );
    source.setIncludeSource( false );
    source.setIncludeExternalAnnotations( true );
    source.setSupportDependencyOmit( true );
    source.setVerifyConfigSha256( false );
    final GlobalJavaConfig java = new GlobalJavaConfig();
    java.setExportDeps( true );
    source.setJava( java );

    final OptionsModel model = OptionsModel.parse( thirdpartyDir, source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(), dir.normalize() );
    assertEquals( model.getExtensionFile(), thirdpartyDir.resolve( "dependencies.bzl" ) );
    assertEquals( model.getWorkspaceMacroName(), "gen_myprj_dependency_rules" );
    assertEquals( model.getTargetMacroName(), "gen_myprj_targets" );
    assertEquals( model.getNamePrefix(), "myprj_" );
    assertEquals( model.getNameStrategy(), NameStrategy.ArtifactId );
    assertEquals( model.getDefaultNature(), Nature.J2cl );
    assertFalse( model.failOnMissingPom() );
    assertFalse( model.failOnInvalidPom() );
    assertFalse( model.includeSource() );
    assertTrue( model.includeExternalAnnotations() );
    assertTrue( model.exportDeps() );
    assertTrue( model.supportDependencyOmit() );
    assertFalse( model.verifyConfigSha256() );
  }
}
