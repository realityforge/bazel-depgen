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
  }

  @Test
  public void parse()
  {
    final OptionsConfig source = new OptionsConfig();
    source.setWorkspaceDirectory( ".." );
    source.setExtensionFile( "workspace.bzl" );
    source.setFailOnMissingPom( false );
    source.setFailOnInvalidPom( false );

    final OptionsModel model = OptionsModel.parse( FileUtil.getCurrentDirectory(), source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(),
                  FileUtil.getCurrentDirectory().resolve( ".." ).normalize() );
    assertEquals( model.getExtensionFile(),
                  FileUtil.getCurrentDirectory().resolve( "workspace.bzl" ) );
    assertFalse( model.failOnMissingPom() );
    assertFalse( model.failOnInvalidPom() );
  }
}
