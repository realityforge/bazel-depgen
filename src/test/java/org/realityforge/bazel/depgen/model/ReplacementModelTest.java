package org.realityforge.bazel.depgen.model;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ReplacementConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplacementModelTest
  extends AbstractTest
{
  @Test
  public void parseReplacementWithCoord()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setCoord( "com.example:myapp" );
    source.setTarget( "@com.example//:myapp" );

    final ReplacementModel model = ReplacementModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getTarget(), "@com.example//:myapp" );
  }

  @Test
  public void parseReplacementWith1PartCoord()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setCoord( "com.example" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The 'coords' must be in the form; 'group:id'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementWith3PartCoord()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setCoord( "com.example:myapp:1.0" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The 'coords' must be in the form; 'group:id'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementWithSpecElements()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );
    source.setTarget( "@com.example//:myapp" );

    final ReplacementModel model = ReplacementModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
    assertEquals( model.getTarget(), "@com.example//:myapp" );
  }

  @Test
  public void parseReplacementMissingGroup()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setId( "myapp" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The replacement must specify the 'group' property unless the 'coord' shorthand property is used." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementMissingId()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setGroup( "com.example" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The replacement must specify the 'id' property unless the 'coord' shorthand property is used" );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementMissingTarget()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(), "The replacement must specify the 'target' property." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementWithCoordAndGroup()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setCoord( "com.example:myapp" );
    source.setGroup( "com.example" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The replacement must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group or id." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseReplacementWithCoordAndId()
  {
    final ReplacementConfig source = new ReplacementConfig();
    source.setCoord( "com.example:myapp" );
    source.setId( "myapp" );
    source.setTarget( "@com.example//:myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> ReplacementModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The replacement must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group or id." );
    assertEquals( exception.getModel(), source );
  }
}
