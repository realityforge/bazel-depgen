package org.realityforge.bazel.depgen.config;

import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class NatureTest
  extends AbstractTest
{
  @Test
  public void suffix()
  {
    assertEquals( Nature.Java.suffix( true, Nature.Java ), "" );
    assertEquals( Nature.Java.suffix( false, Nature.Java ), "" );
    assertEquals( Nature.Java.suffix( true, Nature.Plugin ), "-java" );
    assertEquals( Nature.Java.suffix( false, Nature.Plugin ), "" );
    assertEquals( Nature.Plugin.suffix( true, Nature.Plugin ), "" );
    assertEquals( Nature.Plugin.suffix( false, Nature.Plugin ), "" );
    assertEquals( Nature.Plugin.suffix( true, Nature.Java ), "-plugin" );
    assertEquals( Nature.Plugin.suffix( false, Nature.Java ), "" );
    assertEquals( Nature.J2cl.suffix( true, Nature.J2cl ), "-j2cl" );
    assertEquals( Nature.J2cl.suffix( false, Nature.J2cl ), "-j2cl" );
    assertEquals( Nature.J2cl.suffix( true, Nature.Java ), "-j2cl" );
    assertEquals( Nature.J2cl.suffix( false, Nature.Java ), "-j2cl" );
  }
}
