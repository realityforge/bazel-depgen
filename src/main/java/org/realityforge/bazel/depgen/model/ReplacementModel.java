package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.bazel.depgen.config.ReplacementConfig;

public final class ReplacementModel
{
  @Nonnull
  private final ReplacementConfig _source;
  @Nonnull
  private final String _group;
  @Nonnull
  private final String _id;
  @Nonnull
  private final String _target;

  @Nonnull
  public static ReplacementModel parse( @Nonnull final ReplacementConfig source )
  {
    final String coord = source.getCoord();
    String group = source.getGroup();
    String id = source.getId();
    final String target = source.getTarget();
    if ( null == target )
    {
      throw new InvalidModelException( "The replacement must specify the 'target' property.", source );
    }
    else if ( null != coord && ( null != group || null != id ) )
    {
      throw new InvalidModelException( "The replacement must not specify the 'coord' property if other properties " +
                                       "are present that define the maven coordinates. .i.e. coord must not " +
                                       "be present when any of the following properties are present: group or id.",
                                       source );
    }
    if ( null == coord )
    {
      if ( null == group )
      {
        throw new InvalidModelException( "The replacement must specify the 'group' property unless the 'coord' " +
                                         "shorthand property is used.", source );
      }
      else if ( null == id )
      {
        throw new InvalidModelException( "The replacement must specify the 'id' property unless the 'coord' " +
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

    return new ReplacementModel( source, group, id, target );
  }

  private ReplacementModel( @Nonnull final ReplacementConfig source,
                            @Nonnull final String group,
                            @Nonnull final String id,
                            @Nonnull final String target )
  {
    _source = Objects.requireNonNull( source );
    _group = Objects.requireNonNull( group );
    _id = Objects.requireNonNull( id );
    _target = Objects.requireNonNull( target );
  }

  @Nonnull
  public ReplacementConfig getSource()
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

  @Nonnull
  public String getTarget()
  {
    return _target;
  }
}
