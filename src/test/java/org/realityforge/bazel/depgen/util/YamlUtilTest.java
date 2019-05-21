package org.realityforge.bazel.depgen.util;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Language;
import org.realityforge.bazel.depgen.config.OptionsConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class YamlUtilTest
  extends AbstractTest
{
  @Test
  public void asYamlString()
  {
    assertEquals( YamlUtil.asYamlString( new ApplicationConfig() ), "{}\n" );

    // With a few settings
    final ApplicationConfig object = new ApplicationConfig();
    final OptionsConfig options = new OptionsConfig();
    options.setDefaultLanguage( Language.J2cl );
    object.setOptions( options );
    assertEquals( YamlUtil.asYamlString( object ), "options:\n  defaultLanguage: J2cl\n" );
  }
}
