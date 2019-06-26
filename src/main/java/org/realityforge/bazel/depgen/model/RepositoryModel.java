package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.bazel.depgen.config.RepositoryConfig;

public final class RepositoryModel
{
  @Nullable
  private final RepositoryConfig _source;
  @Nonnull
  private final String _name;
  @Nonnull
  private final String _url;

  @Nonnull
  public static RepositoryModel parse( @Nonnull final RepositoryConfig source )
  {
    final String url = source.getUrl();
    if ( null == url )
    {
      throw new InvalidModelException( "The repository must specify the 'url' property.", source );
    }
    final String name = source.getName();
    final String actualName =
      null != name ?
      name :
      url.replaceAll( "[^a-zA-Z0-9]", "_" ).replaceAll( "_+$", "" ).replaceAll( "_+", "_" );
    return new RepositoryModel( source, actualName, url );
  }

  @Nonnull
  public static RepositoryModel create( @Nonnull final String name, @Nonnull final String url )
  {
    return new RepositoryModel( null, name, url );
  }

  private RepositoryModel( @Nullable final RepositoryConfig source,
                           @Nonnull final String name,
                           @Nonnull final String url )
  {
    _source = source;
    _name = Objects.requireNonNull( name );
    _url = Objects.requireNonNull( url );
  }

  @Nullable
  public RepositoryConfig getSource()
  {
    return _source;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  @Nonnull
  public String getUrl()
  {
    return _url;
  }

  public boolean cacheLookups()
  {
    final Boolean cacheLookups = null != _source ? _source.getCacheLookups() : null;
    return cacheLookups == null ? true : cacheLookups;
  }

  public boolean searchByDefault()
  {
    final Boolean searchByDefault = null != _source ? _source.getSearchByDefault() : null;
    return searchByDefault == null ? true : searchByDefault;
  }
}
