package org.realityforge.bazel.depgen;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class VersionTest
  extends AbstractTest
{
  @Test
  public void get()
  {
    // This gets it from property set via system setting
    assertEquals( Version.get(), "" );
  }

  @Test
  public void get_nonStandardSystemProperty()
  {
    System.setProperty( Version.PROPERTY_KEY, "X123" );
    assertEquals( Version.loadVersion(), "X123" );
  }

  @Test
  public void get_fromResource()
  {
    System.getProperties().remove( Version.PROPERTY_KEY );
    assertNotNull( Version.loadVersion() );
  }
}
