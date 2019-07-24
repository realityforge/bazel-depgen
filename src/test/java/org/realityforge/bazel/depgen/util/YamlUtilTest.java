package org.realityforge.bazel.depgen.util;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ApplicationConfig;
import org.realityforge.bazel.depgen.config.Nature;
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
    options.setNature( Nature.J2cl );
    object.setOptions( options );
    assertEquals( YamlUtil.asYamlString( object ), "options:\n  nature: J2cl\n" );
  }
}
