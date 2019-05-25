package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.ExcludeConfig;

public final class GlobalExcludeModel
{
  @Nonnull
  private final ExcludeConfig _source;
  @Nonnull
  private final String _group;
  @Nonnull
  private final String _id;

  @Nonnull
  public static GlobalExcludeModel parse( @Nonnull final ExcludeConfig source )
  {
    final String coord = source.getCoord();
    String group = source.getGroup();
    String id = source.getId();

    if ( null != coord && ( null != group || null != id ) )
    {
      throw new InvalidModelException( "The global exclude must not specify the 'coord' property if other properties " +
                                       "are present that define the maven coordinates. .i.e. coord must not " +
                                       "be present when any of the following properties are present: group or id.",
                                       source );
    }
    if ( null == coord )
    {
      if ( null == group )
      {
        throw new InvalidModelException( "The global exclude must specify the 'group' property unless the 'coord' " +
                                         "shorthand property is used.", source );
      }
      else if ( null == id )
      {
        throw new InvalidModelException( "The global exclude must specify the 'id' property unless the 'coord' " +
                                         "shorthand property is used", source );
      }
    }
    else
    {
      final String[] components = coord.split( ":" );
      if ( components.length != 2 )
      {
        throw new InvalidModelException( "The 'coord' property on the dependency must specify 2 components " +
                                         "separated by the ':' character. The 'coords' must be in the form; " +
                                         "'group:id'.", source );
      }
      else
      {
        group = components[ 0 ];
        id = components[ 1 ];
      }
    }

    return new GlobalExcludeModel( source, group, id );
  }

  private GlobalExcludeModel( @Nonnull final ExcludeConfig source,
                              @Nonnull final String group,
                              @Nonnull final String id )
  {
    _source = Objects.requireNonNull( source );
    _group = Objects.requireNonNull( group );
    _id = Objects.requireNonNull( id );
  }

  @Nonnull
  public ExcludeConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public String getGroup()
  {
    return _group;
  }

  @Nonnull
  public String getId()
  {
    return _id;
  }
}
