package org.realityforge.bazel.depgen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.ArtifactConfig;
import org.realityforge.bazel.depgen.config.Language;
import org.realityforge.bazel.depgen.config.Nature;

public final class ArtifactModel
{
  @Nonnull
  private final ArtifactConfig _source;
  @Nonnull
  private final String _group;
  @Nonnull
  private final String _id;
  @Nullable
  private final String _type;
  @Nullable
  private final String _classifier;
  @Nullable
  private final String _version;
  @Nonnull
  private final List<ExcludeModel> _excludes;
  @Nonnull
  private final List<String> _visibility;

  @Nonnull
  public static List<ArtifactModel> parse( @Nonnull final ArtifactConfig source )
  {
    final String coord = source.getCoord();
    String group = source.getGroup();
    String id = source.getId();
    String version = source.getVersion();
    String classifier = source.getClassifier();
    String type = source.getType();
    List<String> ids = source.getIds();
    final List<String> excludes = source.getExcludes();
    if ( null != coord &&
         ( null != group || null != id || null != version || null != classifier || null != type || null != ids ) )
    {
      throw new InvalidModelException( "The dependency must not specify the 'coord' property if other properties " +
                                       "are present that define the maven coordinates. .i.e. coord must not " +
                                       "be present when any of the following properties are present: group, " +
                                       "id, version, classifier, type or ids.", source );
    }
    else if ( null != id && null != ids )
    {
      throw new InvalidModelException( "The dependency must not specify both the 'id' property and the " +
                                       "'ids' property.", source );
    }
    if ( null == coord )
    {
      if ( null == group )
      {
        throw new InvalidModelException( "The dependency must specify the 'group' property unless the 'coord' " +
                                         "shorthand property is used.", source );
      }
      else if ( null == id && null == ids )
      {
        throw new InvalidModelException( "The dependency must specify either the 'id' property or the " +
                                         "'ids' property.", source );
      }
      else if ( null == version && null != type )
      {
        throw new InvalidModelException( "The dependency must specify either the 'version' property if the " +
                                         "'type' property is specified.", source );
      }
      else if ( null == version && null != classifier )
      {
        throw new InvalidModelException( "The dependency must specify either the 'version' property if the " +
                                         "'classifier' property is specified.", source );
      }
    }
    else
    {
      final String[] components = coord.split( ":" );
      if ( components.length < 2 || components.length > 5 )
      {
        throw new InvalidModelException( "The 'coord' property on the dependency must specify 2-5 components " +
                                         "separated by the ':' character. The 'coords' must be in one of the " +
                                         "forms; 'group:id', 'group:id:version', 'group:id:type:version' or " +
                                         "'group:id:type:classifier:version'.", source );
      }
      else if ( 2 == components.length )
      {
        group = components[ 0 ];
        id = components[ 1 ];
      }
      else if ( 3 == components.length )
      {
        group = components[ 0 ];
        id = components[ 1 ];
        version = components[ 2 ];
      }
      else if ( 4 == components.length )
      {
        group = components[ 0 ];
        id = components[ 1 ];
        type = components[ 2 ];
        version = components[ 3 ];
      }
      else
      {
        group = components[ 0 ];
        id = components[ 1 ];
        type = components[ 2 ];
        classifier = components[ 3 ];
        version = components[ 4 ];
      }
    }
    if ( Nature.Plugin == source.getNature() && null != source.getLanguages() )
    {
      throw new InvalidModelException( "The dependency must not specify the 'languages' property if the " +
                                       "'nature' property is specified as `Plugin`.", source );
    }

    if ( null == ids )
    {
      ids = Collections.singletonList( id );
    }

    final String agroup = group;
    final String aversion = version;
    final String aclassifier = classifier;
    final String atype = type;
    final List<ExcludeModel> aexcludes =
      null == excludes ?
      Collections.emptyList() :
      excludes
        .stream()
        .map( ExcludeModel::parse )
        .collect( Collectors.toList() );
    final List<String> visibility = source.getVisibility();
    final List<String> avisibility =
      null == visibility ?
      Collections.emptyList() :
      Collections.unmodifiableList( new ArrayList<>( visibility ) );

    return
      ids
        .stream()
        .map( aid -> new ArtifactModel( source, agroup, aid, atype, aclassifier, aversion, aexcludes, avisibility ) )
        .collect( Collectors.toList() );
  }

  public ArtifactModel( @Nonnull final ArtifactConfig source,
                        @Nonnull final String group,
                        @Nonnull final String id,
                        @Nullable final String type,
                        @Nullable final String classifier,
                        @Nullable final String version,
                        @Nonnull final List<ExcludeModel> excludes,
                        @Nonnull final List<String> visibility )
  {
    _source = Objects.requireNonNull( source );
    _group = Objects.requireNonNull( group );
    _id = Objects.requireNonNull( id );
    _type = type;
    _classifier = classifier;
    _version = version;
    _excludes = Objects.requireNonNull( excludes );
    _visibility = Objects.requireNonNull( visibility );
  }

  @Nonnull
  public ArtifactConfig getSource()
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
  public String getType()
  {
    return null == _type ? "jar" : _type;
  }

  @Nullable
  public String getClassifier()
  {
    return _classifier;
  }

  @Nullable
  public String getVersion()
  {
    return _version;
  }

  public boolean isVersioned()
  {
    return null != _version;
  }

  @Nullable
  public String getAlias()
  {
    return _source.getAlias();
  }

  @Nonnull
  public Nature getNature()
  {
    final Nature nature = _source.getNature();
    return null == nature ? Nature.Auto : nature;
  }

  @Nonnull
  public List<Language> getLanguages( @Nonnull final Language defaultLanguage )
  {
    final List<Language> languages = _source.getLanguages();
    return null == languages ? Collections.singletonList( defaultLanguage ) : languages;
  }

  public boolean includeOptional()
  {
    final Boolean includeOptional = _source.getIncludeOptional();
    return null == includeOptional ? false : includeOptional;
  }

  public boolean includeSource( final boolean defaultIncludeSource )
  {
    final Boolean includeSource = _source.getIncludeSource();
    return null == includeSource ? defaultIncludeSource : includeSource;
  }

  public boolean exportDeps( final boolean defaultExportDeps )
  {
    final Boolean exportDeps = _source.getExportDeps();
    return null == exportDeps ? defaultExportDeps : exportDeps;
  }

  @Nonnull
  public List<ExcludeModel> getExcludes()
  {
    return _excludes;
  }

  @Nonnull
  public List<String> getVisibility()
  {
    return _visibility;
  }

  @Nonnull
  public String toCoord()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append( getGroup() );
    sb.append( ':' );
    sb.append( getId() );
    final String version = getVersion();
    if ( null != version )
    {
      sb.append( ':' );
      sb.append( getType() );
      final String classifier = getClassifier();
      if ( null != classifier )
      {
        sb.append( ':' );
        sb.append( classifier );
      }
      sb.append( ':' );
      sb.append( version );
    }

    return sb.toString();
  }
}
