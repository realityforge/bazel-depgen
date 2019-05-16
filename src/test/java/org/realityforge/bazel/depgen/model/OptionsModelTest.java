package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import java.nio.file.Path;
import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.AliasStrategy;
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
    assertEquals( model.getWorkspaceDirectory(), FileUtil.getCurrentDirectory() );
    assertEquals( model.getExtensionFile(),
                  FileUtil.getCurrentDirectory().resolve( OptionsConfig.DEFAULT_EXTENSION_FILE ) );
    assertEquals( model.getWorkspaceMacroName(), OptionsConfig.DEFAULT_WORKSPACE_MACRO_NAME );
    assertEquals( model.getTargetMacroName(), OptionsConfig.DEFAULT_TARGET_MACRO_NAME );
    assertEquals( model.getNamePrefix(), OptionsConfig.DEFAULT_NAME_PREFIX );
    assertEquals( model.getAliasStrategy(), OptionsConfig.DEFAULT_ALIAS_STRATEGY );
    assertTrue( model.failOnMissingPom() );
    assertTrue( model.failOnInvalidPom() );
    assertTrue( model.emitDependencyGraph() );
    assertTrue( model.includeSource() );
    assertFalse( model.exportDeps() );
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
    inIsolatedDirectory( () -> {
      final Path dir = FileUtil.createLocalTempDir();
      final Path thirdpartyDir = dir.resolve( "thirdparty" );

      final OptionsConfig source = new OptionsConfig();
      source.setWorkspaceDirectory( ".." );
      source.setExtensionFile( "dependencies.bzl" );
      source.setWorkspaceMacroName( "gen_myprj_dependency_rules" );
      source.setTargetMacroName( "gen_myprj_targets" );
      source.setNamePrefix( "myprj_" );
      source.setAliasStrategy( AliasStrategy.ArtifactId );
      source.setFailOnMissingPom( false );
      source.setFailOnInvalidPom( false );
      source.setEmitDependencyGraph( false );
      source.setIncludeSource( false );
      source.setExportDeps( true );

      final OptionsModel model = OptionsModel.parse( thirdpartyDir, source );
      assertEquals( model.getSource(), source );
      assertEquals( model.getWorkspaceDirectory(), dir.normalize() );
      assertEquals( model.getExtensionFile(), dir.resolve( "dependencies.bzl" ) );
      assertEquals( model.getWorkspaceMacroName(), "gen_myprj_dependency_rules" );
      assertEquals( model.getTargetMacroName(), "gen_myprj_targets" );
      assertEquals( model.getNamePrefix(), "myprj_" );
      assertEquals( model.getAliasStrategy(), AliasStrategy.ArtifactId );
      assertFalse( model.failOnMissingPom() );
      assertFalse( model.failOnInvalidPom() );
      assertFalse( model.includeSource() );
      assertTrue( model.exportDeps() );
    } );
  }
}
