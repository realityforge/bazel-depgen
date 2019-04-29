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
    assertTrue( model.failOnMissingPom() );
    assertTrue( model.failOnInvalidPom() );
    assertTrue( model.emitDependencyGraph() );
  }

  @Test
  public void parse()
  {
    final OptionsConfig source = new OptionsConfig();
    source.setWorkspaceDirectory( ".." );
    source.setExtensionFile( "dependencies.bzl" );
    source.setFailOnMissingPom( false );
    source.setFailOnInvalidPom( false );
    source.setEmitDependencyGraph( false );

    final OptionsModel model = OptionsModel.parse( FileUtil.getCurrentDirectory(), source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(),
                  FileUtil.getCurrentDirectory().resolve( ".." ).normalize() );
    assertEquals( model.getExtensionFile(),
                  FileUtil.getCurrentDirectory().resolve( "dependencies.bzl" ) );
    assertFalse( model.failOnMissingPom() );
    assertFalse( model.failOnInvalidPom() );
    assertFalse( model.emitDependencyGraph() );
  }
}
