package org.realityforge.bazel.depgen.util;

import org.realityforge.bazel.depgen.AbstractTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class BazelUtilTest
  extends AbstractTest
{
  @Test
  public void cleanNamePart()
  {
    assertEquals( BazelUtil.cleanNamePart( "com.example:mylib:0.98" ), "com_example_mylib_0_98" );
    assertEquals( BazelUtil.cleanNamePart( "com.example:My-App:22-RC1" ), "com_example_my_app_22_rc1" );
  }
}
