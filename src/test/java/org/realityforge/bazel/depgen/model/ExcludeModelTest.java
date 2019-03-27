package org.realityforge.bazel.depgen.model;

import org.realityforge.bazel.depgen.AbstractDepGenTest;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ExcludeModelTest
  extends AbstractDepGenTest
{
  @Test
  public void createWithId()
  {
    final String group = ValueUtil.randomString().replace( "-", "_" );
    final String id = ValueUtil.randomString().replace( "-", "_" );
    final ExcludeModel model = ExcludeModel.parse( group + ":" + id );

    assertEquals( model.getGroup(), group );
    assertTrue( model.hasId() );
    assertEquals( model.getId(), id );
  }

  @Test
  public void createWithoutId()
  {
    final String group = ValueUtil.randomString().replace( "-", "_" );
    final ExcludeModel model = ExcludeModel.parse( group );

    assertEquals( model.getGroup(), group );
    assertFalse( model.hasId() );
    assertNull( model.getId() );
  }
}
