package org.realityforge.bazel.depgen.util;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class HashUtilTest
{
  @Test
  public void sha256()
  {
    assertEquals( HashUtil.sha256( new byte[]{ 1, 2, 3, 4 } ),
                  "9F64A747E1B97F131FABB6B447296C9B6F0201E79FB3C5356E6C77E89B6A806A" );
    assertEquals( HashUtil.sha256( new byte[]{ 1, 2, 3, 4 }, new byte[]{ 5, 6 } ),
                  "7192385C3C0605DE55BB9476CE1D90748190ECB32A8EED7F5207B30CF6A1FE89" );
  }
}
