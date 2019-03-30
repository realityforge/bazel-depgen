package org.realityforge.bazel.depgen.model;

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

    final OptionsModel model = OptionsModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(), OptionsConfig.DEFAULT_WORKSPACE_DIR );
    assertEquals( model.getExtensionFile(), OptionsConfig.DEFAULT_EXTENSION_FILE );
    assertTrue( model.failOnInvalidPom() );
  }

  @Test
  public void parse()
  {
    final OptionsConfig source = new OptionsConfig();
    source.setWorkspaceDirectory( ".." );
    source.setExtensionFile( "workspace.bzl" );
    source.setFailOnInvalidPom( false );

    final OptionsModel model = OptionsModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getWorkspaceDirectory(), ".." );
    assertEquals( model.getExtensionFile(), "workspace.bzl" );
    assertFalse( model.failOnInvalidPom() );
  }
}
