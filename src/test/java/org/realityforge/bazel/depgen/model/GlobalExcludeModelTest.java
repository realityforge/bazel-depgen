package org.realityforge.bazel.depgen.model;

import org.realityforge.bazel.depgen.AbstractTest;
import org.realityforge.bazel.depgen.config.ExcludeConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GlobalExcludeModelTest
  extends AbstractTest
{
  @Test
  public void parseExcludeWithCoord()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setCoord( "com.example:myapp" );

    final GlobalExcludeModel model = GlobalExcludeModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
  }

  @Test
  public void parseExcludeWith1PartCoord()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setCoord( "com.example" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The 'coords' must be in the form; 'group:id'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseExcludeWith3PartCoord()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setCoord( "com.example:myapp:1.0" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The 'coord' property on the dependency must specify 2 components separated by the ':' character. The 'coords' must be in the form; 'group:id'." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseExcludeWithSpecElements()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setGroup( "com.example" );
    source.setId( "myapp" );

    final GlobalExcludeModel model = GlobalExcludeModel.parse( source );
    assertEquals( model.getSource(), source );
    assertEquals( model.getGroup(), "com.example" );
    assertEquals( model.getId(), "myapp" );
  }

  @Test
  public void parseExcludeMissingGroup()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setId( "myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The global exclude must specify the 'group' property unless the 'coord' shorthand property is used." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseExcludeMissingId()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setGroup( "com.example" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The global exclude must specify the 'id' property unless the 'coord' shorthand property is used" );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseExcludeWithCoordAndGroup()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setCoord( "com.example:myapp" );
    source.setGroup( "com.example" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The global exclude must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group or id." );
    assertEquals( exception.getModel(), source );
  }

  @Test
  public void parseExcludeWithCoordAndId()
  {
    final ExcludeConfig source = new ExcludeConfig();
    source.setCoord( "com.example:myapp" );
    source.setId( "myapp" );

    final InvalidModelException exception =
      expectThrows( InvalidModelException.class, () -> GlobalExcludeModel.parse( source ) );
    assertEquals( exception.getMessage(),
                  "The global exclude must not specify the 'coord' property if other properties are present that define the maven coordinates. .i.e. coord must not be present when any of the following properties are present: group or id." );
    assertEquals( exception.getModel(), source );
  }
}
