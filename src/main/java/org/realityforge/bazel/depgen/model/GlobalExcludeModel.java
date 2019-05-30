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
    if ( null == coord )
    {
      throw new InvalidModelException( "The global exclude must specify the 'coord' property.", source );
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
        return new GlobalExcludeModel( source, components[ 0 ], components[ 1 ] );
      }
    }
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
