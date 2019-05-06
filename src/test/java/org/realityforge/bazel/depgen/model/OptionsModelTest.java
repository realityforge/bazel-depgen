package org.realityforge.bazel.depgen.model;

import gir.io.FileUtil;
import org.realityforge.bazel.depgen.AbstractTest;
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
    assertEquals( model.getGenerateRulesMacroName(), OptionsConfig.DEFAULT_GENERATE_RULES_MACRO_NAME );
    assertEquals( model.getNamePrefix(), OptionsConfig.DEFAULT_NAME_PREFIX );
    assertTrue( model.failOnMissingPom() );
    assertTrue( model.failOnInvalidPom() );
    assertTrue( model.emitDependencyGraph() );
    assertTrue( model.includeSource() );
  }

  @Test
  public void parse()
  {
    final OptionsConfig source = new OptionsConfig();
    source.setWorkspaceDirectory( ".." );
    source.setExtensionFile( "dependencies.bzl" );
    source.setGenerateRulesMacroName( "gen_myprj_dependency_rules" );
    source.setNamePrefix( "myprj_" );
    source.setFailOnMissingPom( false );
    source.setFailOnInvalidPom( false );
    source.setEmitDependencyGraph( false );
    source.setIncludeSource( false );

    final OptionsModel model = OptionsModel.parse( FileUtil.getCurrentDirectory(), source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(),
                  FileUtil.getCurrentDirectory().resolve( ".." ).normalize() );
    assertEquals( model.getExtensionFile(),
                  FileUtil.getCurrentDirectory().resolve( "dependencies.bzl" ) );
    assertEquals( model.getGenerateRulesMacroName(), "gen_myprj_dependency_rules" );
    assertEquals( model.getNamePrefix(), "myprj_" );
    assertFalse( model.failOnMissingPom() );
    assertFalse( model.failOnInvalidPom() );
    assertFalse( model.includeSource() );
  }
}
