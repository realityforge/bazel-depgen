package org.realityforge.bazel.depgen;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DepGenConfigTest
  extends AbstractTest
{
  @Test
  public void getVersion()
  {
    // This gets it from property set via system setting
    assertEquals( DepGenConfig.getVersion(), "1" );
    assertEquals( DepGenConfig.getCoord(), "org.realityforge.bazel.depgen:bazel-depgen:jar:all:1" );
  }

  @Test
  public void getVersion_nonStandardSystemProperty()
  {
    System.setProperty( DepGenConfig.PROPERTY_KEY, "X123" );
    assertEquals( DepGenConfig.getVersion(), "X123" );
  }

  @Test
  public void getVersion_fromResource()
  {
    System.getProperties().remove( DepGenConfig.PROPERTY_KEY );
    assertNotNull( DepGenConfig.getVersion() );
    assertNotNull( DepGenConfig.getGroupId() );
    assertNotNull( DepGenConfig.getArtifactId() );
    assertNotNull( DepGenConfig.getClassifier() );
    assertNotNull( DepGenConfig.getCoord() );
  }
}
